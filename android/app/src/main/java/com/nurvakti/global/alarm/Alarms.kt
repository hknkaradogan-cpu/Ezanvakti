package com.nurvakti.global.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.nurvakti.global.MainActivity
import com.nurvakti.global.data.AlertMode
import com.nurvakti.global.data.Settings
import com.nurvakti.global.data.SettingsStore
import com.nurvakti.global.prayer.Times
import java.time.ZonedDateTime

object Channels {
    const val EZAN = "ezan_v1"          // ezan sesi servis tarafından çalınır; kanal sessiz ama yüksek öncelikli
    const val NOTIFY = "vakit_ses_v1"
    const val VIBRATE = "vakit_titresim_v1"
    const val SILENT = "vakit_sessiz_v1"
    const val PRE = "vakit_oncesi_v1"
    const val DOWNLOAD = "indirme_v1"
    fun create(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        nm.createNotificationChannel(NotificationChannel(EZAN, "Ezan", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); description = "Vakit girdiğinde çalan ezan" })
        nm.createNotificationChannel(NotificationChannel(NOTIFY, "Vakit bildirimi (sesli)", NotificationManager.IMPORTANCE_HIGH).apply { setSound(sound, attrs) })
        nm.createNotificationChannel(NotificationChannel(VIBRATE, "Vakit bildirimi (titreşim)", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(true); vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600) })
        nm.createNotificationChannel(NotificationChannel(SILENT, "Vakit bildirimi (sessiz)", NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null); enableVibration(false) })
        nm.createNotificationChannel(NotificationChannel(PRE, "Vakit öncesi hatırlatma", NotificationManager.IMPORTANCE_HIGH).apply { setSound(sound, attrs) })
        nm.createNotificationChannel(NotificationChannel(DOWNLOAD, "İndirmeler", NotificationManager.IMPORTANCE_LOW))
    }
}

val PRAYER_TR = mapOf("imsak" to "İmsak", "fajr" to "Sabah", "sunrise" to "Güneş", "dhuhr" to "Öğle", "asr" to "İkindi", "maghrib" to "Akşam", "isha" to "Yatsı")

/** AlarmManager ile gerçek yerel alarmlar. Önümüzdeki 48 saatin tüm vakit/ön-bildirim alarmları kurulur;
 *  her alarm çaldığında ve günlük işçide yeniden hesaplanır. Aynı istek kodu kullanıldığı için alarm çoğalmaz. */
object AlarmScheduler {
    const val ACTION = "com.nurvakti.global.ALARM"
    private const val MAX_SLOTS = 40

    fun canExact(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    suspend fun reschedule(ctx: Context) {
        val s = SettingsStore(ctx).get()
        schedule(ctx, s)
    }

    data class Ev(val at: ZonedDateTime, val prayer: String, val pre: Int)

    fun upcoming(ctx: Context, s: Settings, now: ZonedDateTime = Times.nowZ(s)): List<Ev> {
        val res = ArrayList<Ev>()
        for (i in 0..2) {
            val d = Times.day(ctx, s, now.toLocalDate().plusDays(i.toLong()))
            for ((k, a) in s.alerts) {
                val t = d.times[k] ?: continue
                if (a.mode != AlertMode.OFF) res += Ev(t, k, 0)
                if (a.preMinutes > 0) res += Ev(t.minusMinutes(a.preMinutes.toLong()), k, a.preMinutes)
            }
        }
        return res.filter { it.at.isAfter(now) }.sortedBy { it.at }.take(MAX_SLOTS)
    }

    fun schedule(ctx: Context, s: Settings) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        // eski alarmları iptal et
        for (i in 0 until MAX_SLOTS) pi(ctx, i, null)?.let { am.cancel(it) }
        val evs = upcoming(ctx, s)
        evs.forEachIndexed { i, ev ->
            val intent = Intent(ctx, AlarmReceiver::class.java).setAction(ACTION)
                .putExtra("prayer", ev.prayer).putExtra("pre", ev.pre).putExtra("at", ev.at.toInstant().toEpochMilli())
            val p = PendingIntent.getBroadcast(ctx, 1000 + i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val t = ev.at.toInstant().toEpochMilli()
            try {
                if (canExact(ctx)) {
                    if (ev.pre == 0) {
                        val show = PendingIntent.getActivity(ctx, 9, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                        am.setAlarmClock(AlarmManager.AlarmClockInfo(t, show), p)
                    } else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
                } else {
                    // Kesin alarm izni yoksa: Doze'da da çalışan ama birkaç dakika gecikebilen alarm (kullanıcıya bildirilir)
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
            }
        }
        ctx.getSharedPreferences("alarm_state", Context.MODE_PRIVATE).edit()
            .putString("scheduled", evs.joinToString("\n") { "${it.at} ${it.prayer} pre=${it.pre}" }).apply()
    }

    private fun pi(ctx: Context, i: Int, unused: Any?): PendingIntent? =
        PendingIntent.getBroadcast(ctx, 1000 + i, Intent(ctx, AlarmReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

    fun scheduledDebug(ctx: Context): String = ctx.getSharedPreferences("alarm_state", Context.MODE_PRIVATE).getString("scheduled", "") ?: ""

    /** Aynı vaktin ikinci kez çalmasını engeller (ör. yeniden kurulum sırasında çift tetiklenme). */
    fun firstFire(ctx: Context, key: String): Boolean {
        val sp = ctx.getSharedPreferences("alarm_state", Context.MODE_PRIVATE)
        val fired = sp.getStringSet("fired", emptySet())!!.toMutableSet()
        if (key in fired) return false
        if (fired.size > 60) fired.clear()
        fired += key
        sp.edit().putStringSet("fired", fired).apply()
        return true
    }
}
