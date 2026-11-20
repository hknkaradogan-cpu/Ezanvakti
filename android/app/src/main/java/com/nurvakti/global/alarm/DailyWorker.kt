package com.nurvakti.global.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nurvakti.global.data.SettingsStore
import com.nurvakti.global.location.Geo
import com.nurvakti.global.prayer.Times
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Her 6 saatte bir: seyahat modunda konumu günceller, çevrimiçi kaynak seçiliyse veriyi tazeler ve alarmları yeniden kurar. */
class DailyWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val store = SettingsStore(ctx)
        val s = store.get()
        if (s.travelMode && ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val loc = LocationServices.getFusedLocationProviderClient(ctx).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                if (loc != null && Geo.distanceKm(loc.latitude, loc.longitude, s.place.lat, s.place.lng) > 15) {
                    val name = Geo.reverse(ctx, loc.latitude, loc.longitude)
                    store.update { it.copy(place = it.place.copy(name = name.first, region = name.second, lat = loc.latitude, lng = loc.longitude,
                        tz = ZoneId.systemDefault().id, country = name.third ?: it.place.country, gps = true)) }
                }
            }
        }
        val s2 = store.get()
        if (s2.source == "ALADHAN") Times.fetchAlAdhan(ctx, s2)
        AlarmScheduler.schedule(ctx, store.get())
        return Result.success()
    }
    companion object {
        fun ensure(ctx: Context) {
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("nur_daily", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DailyWorker>(6, TimeUnit.HOURS).build())
        }
    }
}
