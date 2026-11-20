package com.nurvakti.global.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nurvakti.global.MainActivity
import com.nurvakti.global.R
import com.nurvakti.global.data.AlertMode
import com.nurvakti.global.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    val pr = goAsync()
    scope.launch { try { block() } finally { pr.finish() } }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = goAsync {
        val prayer = intent.getStringExtra("prayer") ?: return@goAsync
        val pre = intent.getIntExtra("pre", 0)
        val at = intent.getLongExtra("at", System.currentTimeMillis())
        val s = SettingsStore(ctx).get()
        // Geç tetiklenen (ör. 20 dk'dan fazla gecikmiş) eski alarmı çalma
        val late = System.currentTimeMillis() - at > 20 * 60_000
        if (!late && AlarmScheduler.firstFire(ctx, "$prayer-$pre-$at")) {
            val a = s.alerts[prayer]
            val name = PRAYER_TR[prayer] ?: prayer
            val time = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5)
            if (pre > 0) {
                notify(ctx, Channels.PRE, 200 + prayer.hashCode() % 50, "$name vaktine $pre dakika", "${s.place.name} · $name $time")
            } else if (a != null) when (a.mode) {
                AlertMode.FULL, AlertMode.SHORT -> runCatching { AdhanService.start(ctx, prayer, a.mode == AlertMode.SHORT, a.sound, a.customUri) }
                    .onFailure { notify(ctx, Channels.NOTIFY, 300, "$name vakti girdi", "Ezan başlatılamadı (sistem kısıtlaması). Pil ayarlarını kontrol edin.") }
                AlertMode.NOTIFY -> notify(ctx, Channels.NOTIFY, 300, "$name vakti girdi", "${s.place.name} · $time")
                AlertMode.VIBRATE -> notify(ctx, Channels.VIBRATE, 300, "$name vakti girdi", "${s.place.name} · $time")
                AlertMode.SILENT -> notify(ctx, Channels.SILENT, 300, "$name vakti girdi", "${s.place.name} · $time")
                AlertMode.OFF -> Unit
            }
        }
        AlarmScheduler.schedule(ctx, s)
    }
}

fun notify(ctx: Context, channel: String, id: Int, title: String, text: String) {
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val n = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_stat_nur).setContentTitle(title).setContentText(text)
        .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
    ctx.getSystemService(NotificationManager::class.java).notify(id, n)
}

/** Telefon açılışı, saat/saat dilimi değişikliği, uygulama güncellemesi ve kesin alarm izni değişikliğinde alarmları yeniden kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = goAsync {
        val store = SettingsStore(ctx)
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            // GPS/seyahat modunda konum saat dilimi cihazınkini izler
            store.update { s -> if (s.place.gps || s.travelMode) s.copy(place = s.place.copy(tz = ZoneId.systemDefault().id)) else s }
        }
        AlarmScheduler.reschedule(ctx)
        DailyWorker.ensure(ctx)
    }
}
