package com.nurvakti.global.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Ortak sözlük (web/data/i18n.json): anahtar Türkçe metnin kendisidir; çeviri yoksa Türkçe gösterilir. */
class Strings(val lang: String, private val dict: Map<String, Map<String, String>>) {
    fun t(tr: String): String = if (lang == "tr") tr else dict[tr]?.get(lang) ?: tr
}
val LocalStrings = staticCompositionLocalOf { Strings("tr", emptyMap()) }
@Composable fun t(tr: String): String = LocalStrings.current.t(tr)
@Composable fun lang(): String = LocalStrings.current.lang

val PRAYER_KEYS = listOf("imsak", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
val PRAYER_NAMES = mapOf("imsak" to "İmsak", "fajr" to "Sabah", "sunrise" to "Güneş", "dhuhr" to "Öğle", "asr" to "İkindi", "maghrib" to "Akşam", "isha" to "Yatsı")
@Composable fun prayerName(k: String) = t(PRAYER_NAMES[k] ?: k)
