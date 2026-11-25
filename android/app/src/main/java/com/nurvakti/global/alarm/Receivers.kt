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
            if (pre == AlarmScheduler.CHECK) {
                val date = Instant.ofEpochMilli(at - s.checkMinutes * 60_000L).atZone(com.nurvakti.global.prayer.Times.zoneOf(s)).toLocalDate().toString()
                val dao = com.nurvakti.global.data.AppDb.get(ctx).dao()
                val done = dao.logsNow(date, date).any { it.prayer == prayer && it.done }
                if (!done) checkNotify(ctx, prayer, name, date)
            } else if (pre == AlarmScheduler.HADIS) {
                val c = com.nurvakti.global.data.Content(ctx)
                val slot = (at / 3_600_000L)
                val en = s.lang == "en"
                // "Hadis ve ayet" modunda saat sırasıyla bir hadis, bir ayet
                if (s.hadisContent == "BOTH" && slot % 2 == 1L || s.hadisContent == "AYET") {
                    val a = c.ayet[((slot / (if (s.hadisContent == "BOTH") 2 else 1)) % c.ayet.size).toInt()]
                    bigNotify(ctx, Channels.HADIS, 400, if (en) "Verse of the Quran" else if (s.lang == "ar") "آية" else "Ayet-i Kerime",
                        (if (en) a.en else a.tr) + "\n— " + a.name + " " + a.ref, a.ar)
                } else {
                    val h = c.hadis[((slot / (if (s.hadisContent == "BOTH") 2 else 1)) % c.hadis.size).toInt()]
                    bigNotify(ctx, Channels.HADIS, 400, if (en) "Hadith" else if (s.lang == "ar") "حديث" else "Hadis-i Şerif", (if (en) h.en else h.tr) + "\n— " + h.src)
                }
            } else if (pre > 0) {
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

fun bigNotify(ctx: Context, channel: String, id: Int, title: String, text: String, arabic: String? = null) {
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val n = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_stat_nur).setContentTitle(title).setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(if (arabic != null) arabic + "\n\n" + text else text)).setContentIntent(open).setAutoCancel(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setPriority(NotificationCompat.PRIORITY_HIGH).setShowWhen(true).build()
    ctx.getSystemService(NotificationManager::class.java).notify(id, n)
}

/** "Öğle namazını kıldın mı?" — altında "Kıldım ✓" düğmesi: dokununca kaydedilir ve bildirim kapanır. */
fun checkNotify(ctx: Context, prayer: String, name: String, date: String) {
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val id = 500 + PRAYER_TR.keys.indexOf(prayer)
    fun act(done: Boolean) = PendingIntent.getBroadcast(ctx, id * 10 + if (done) 1 else 2,
        Intent(ctx, PrayerActionReceiver::class.java).putExtra("prayer", prayer).putExtra("date", date).putExtra("done", done).putExtra("nid", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).putExtra("open", "tracker"), PendingIntent.FLAG_IMMUTABLE)
    val n = NotificationCompat.Builder(ctx, Channels.CHECK).setSmallIcon(R.drawable.ic_stat_nur)
        .setContentTitle("$name namazını kıldın mı?").setContentText("Namaz takibine işlemek için dokun.")
        .addAction(0, "Kıldım ✓", act(true)).addAction(0, "Henüz değil", act(false))
        .setContentIntent(open).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
    ctx.getSystemService(NotificationManager::class.java).notify(id, n)
}

class PrayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = goAsync {
        val prayer = intent.getStringExtra("prayer") ?: return@goAsync
        val date = intent.getStringExtra("date") ?: return@goAsync
        if (intent.getBooleanExtra("done", false))
            com.nurvakti.global.data.AppDb.get(ctx).dao().setLog(com.nurvakti.global.data.PrayerLog(date, prayer, true))
        ctx.getSystemService(NotificationManager::class.java).cancel(intent.getIntExtra("nid", 0))
    }
}
