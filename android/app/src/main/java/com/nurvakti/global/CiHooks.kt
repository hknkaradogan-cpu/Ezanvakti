package com.nurvakti.global

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nurvakti.global.alarm.AlarmReceiver
import com.nurvakti.global.alarm.AlarmScheduler
import com.nurvakti.global.data.AlertMode
import com.nurvakti.global.data.PrayerAlert
import com.nurvakti.global.ui.AppVm

/** Yalnızca debug derlemesinde: CI emülatör testlerinin uygulamayı otomatik sürmesi için komutlar. */
object CiHooks {
    fun run(ctx: Context, vm: AppVm, cmd: String) {
        if (!BuildConfig.DEBUG) return
        when {
            cmd == "onboard" -> vm.update { it.copy(onboarded = true) }
            cmd.startsWith("lang:") -> vm.update { it.copy(lang = cmd.removePrefix("lang:")) }
            cmd.startsWith("theme:") -> vm.update { it.copy(theme = cmd.removePrefix("theme:")) }
            cmd.startsWith("dl:") -> vm.downloadSurah(vm.settings.value?.reciter ?: "alafasy", cmd.removePrefix("dl:").toInt())
            cmd.startsWith("dlezan:") -> vm.downloadEzan(cmd.removePrefix("dlezan:"))
            cmd.startsWith("playr:") -> { val (rid, n) = cmd.removePrefix("playr:").split(":"); vm.content.reciters.firstOrNull { it.id == rid }?.let { vm.player.playSurahs(it, listOf(n.toInt()), n.toInt()) } }
            cmd.startsWith("place:") -> { val a = cmd.removePrefix("place:").split(","); vm.setPlace(com.nurvakti.global.data.Place(a[4], "", a[0].toDouble(), a[1].toDouble(), a[2])) }
            cmd.startsWith("play:") -> { val n = cmd.removePrefix("play:").toInt(); val r = vm.content.reciters.first { it.id == "alafasy" }; vm.player.playSurahs(r, listOf(n), n) }
            cmd.startsWith("alarm") -> {
                // Gerçek AlarmManager alarmı: N saniye sonra "Öğle" vakti girmiş gibi tetiklenir (tam ezan, gömülü klasik ses)
                val parts = cmd.removePrefix("alarm").split(":")
                val sec = parts[0].toIntOrNull() ?: 60
                vm.update { it.copy(alerts = it.alerts + ("dhuhr" to PrayerAlert(AlertMode.FULL, parts.getOrNull(1) ?: "klasik"))) }
                val at = System.currentTimeMillis() + sec * 1000L
                val i = Intent(ctx, AlarmReceiver::class.java).setAction(AlarmScheduler.ACTION).putExtra("prayer", "dhuhr").putExtra("pre", 0).putExtra("at", at)
                val p = PendingIntent.getBroadcast(ctx, 990, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val show = PendingIntent.getActivity(ctx, 991, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                ctx.getSystemService(AlarmManager::class.java).setAlarmClock(AlarmManager.AlarmClockInfo(at, show), p)
            }
        }
    }
}
