package com.nurvakti.global.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.nurvakti.global.MainActivity
import com.nurvakti.global.R
import com.nurvakti.global.alarm.AlarmScheduler
import com.nurvakti.global.alarm.goAsync
import com.nurvakti.global.data.Content
import com.nurvakti.global.data.Settings
import com.nurvakti.global.data.SettingsStore
import com.nurvakti.global.prayer.Hijri
import com.nurvakti.global.prayer.Times
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ana ekran widget'ları: canlı geri sayım (Chronometer) + günün vakitleri. Vakit girdiğinde ve gece yarısı AlarmManager ile yenilenir. */
object Widgets {
    private val KEYS = listOf("imsak", "sunrise", "dhuhr", "asr", "maghrib", "isha")
    private val NAMES = mapOf("imsak" to "İmsak", "fajr" to "Sabah", "sunrise" to "Güneş", "dhuhr" to "Öğle", "asr" to "İkindi", "maghrib" to "Akşam", "isha" to "Yatsı")
    const val REFRESH = "com.nurvakti.global.WIDGET_REFRESH"

    private fun tr(c: Content, s: Settings, t: String) = if (s.lang == "tr") t else c.i18n[t]?.get(s.lang) ?: t
    private fun loc(lang: String) = when (lang) { "en" -> Locale.ENGLISH; "ar" -> Locale("ar"); else -> Locale("tr", "TR") }

    fun build(ctx: Context, s: Settings, large: Boolean): RemoteViews {
        val c = Content(ctx)
        val v = RemoteViews(ctx.packageName, if (large) R.layout.widget_large else R.layout.widget_small)
        val now = Times.nowZ(s)
        val (nk, at) = Times.next(ctx, s, now)
        val next = "%02d:%02d".format(at.hour, at.minute)
        val leftMs = java.time.Duration.between(now, at).toMillis().coerceAtLeast(0)
        v.setChronometer(R.id.w_count, SystemClock.elapsedRealtime() + leftMs, null, true)
        v.setChronometerCountDown(R.id.w_count, true)
        v.setTextViewText(R.id.w_city, s.place.name)
        if (large) {
            v.setTextViewText(R.id.w_next, "${tr(c, s, "Sonraki vakit")}: ${tr(c, s, NAMES.getValue(nk))} $next")
            val h = Hijri.of(now.toLocalDate(), s.hijriAdjust)
            v.setTextViewText(R.id.w_date, now.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM, EEE", loc(s.lang))) + " · " + h.format(s.lang))
            val d = Times.day(ctx, s, now.toLocalDate())
            val cur = Times.current(ctx, s, now)
            val rows = mapOf("imsak" to Triple(R.id.r_imsak, R.id.n_imsak, R.id.t_imsak), "sunrise" to Triple(R.id.r_sunrise, R.id.n_sunrise, R.id.t_sunrise),
                "dhuhr" to Triple(R.id.r_dhuhr, R.id.n_dhuhr, R.id.t_dhuhr), "asr" to Triple(R.id.r_asr, R.id.n_asr, R.id.t_asr),
                "maghrib" to Triple(R.id.r_maghrib, R.id.n_maghrib, R.id.t_maghrib), "isha" to Triple(R.id.r_isha, R.id.n_isha, R.id.t_isha))
            for (k in KEYS) {
                val (r, n, t) = rows.getValue(k)
                v.setTextViewText(n, tr(c, s, NAMES.getValue(k)))
                v.setTextViewText(t, d.hhmm(k))
                val active = k == cur || (k == "imsak" && cur == "fajr")
                v.setInt(r, "setBackgroundResource", if (active) R.drawable.widget_row_active else 0)
            }
        } else {
            v.setTextViewText(R.id.w_next, "${tr(c, s, NAMES.getValue(nk))} $next")
        }
        val open = PendingIntent.getActivity(ctx, 7, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        v.setOnClickPendingIntent(R.id.w_root, open)
        return v
    }

    /** Ayarlar'dan widget ekleme isteği. Başlatıcı destekliyorsa onay penceresi açılır; desteklemiyorsa false döner. */
    fun requestPin(ctx: Context, large: Boolean): Boolean {
        val m = AppWidgetManager.getInstance(ctx)
        if (!m.isRequestPinAppWidgetSupported) return false
        val cn = ComponentName(ctx, if (large) LargeWidget::class.java else SmallWidget::class.java)
        return runCatching { m.requestPinAppWidget(cn, null, null) }.getOrDefault(false)
    }

    fun refresh(ctx: Context, s: Settings) {
        val m = AppWidgetManager.getInstance(ctx)
        val small = m.getAppWidgetIds(ComponentName(ctx, SmallWidget::class.java))
        val large = m.getAppWidgetIds(ComponentName(ctx, LargeWidget::class.java))
        if (small.isEmpty() && large.isEmpty()) return
        if (small.isNotEmpty()) runCatching { m.updateAppWidget(small, build(ctx, s, false)) }
        if (large.isNotEmpty()) runCatching { m.updateAppWidget(large, build(ctx, s, true)) }
        scheduleNext(ctx, s)
    }

    /** Bir sonraki vakit (veya gece yarısı) geldiğinde widget'ı yenile */
    private fun scheduleNext(ctx: Context, s: Settings) {
        val now = Times.nowZ(s)
        val (_, at) = Times.next(ctx, s, now)
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        val t = minOf(at, midnight).toInstant().toEpochMilli() + 2000
        val pi = PendingIntent.getBroadcast(ctx, 77, Intent(ctx, WidgetRefreshReceiver::class.java).setAction(REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = ctx.getSystemService(AlarmManager::class.java)
        runCatching { if (AlarmScheduler.canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC, t, pi) else am.setAndAllowWhileIdle(AlarmManager.RTC, t, pi) }
    }
}

open class BaseWidget(private val large: Boolean) : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, m: AppWidgetManager, ids: IntArray) = goAsync {
        val s = SettingsStore(ctx).get()
        m.updateAppWidget(ids, Widgets.build(ctx, s, large))
        Widgets.refresh(ctx, s)
    }
}
class SmallWidget : BaseWidget(false)
class LargeWidget : BaseWidget(true)

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = goAsync { Widgets.refresh(ctx, SettingsStore(ctx).get()) }
}
