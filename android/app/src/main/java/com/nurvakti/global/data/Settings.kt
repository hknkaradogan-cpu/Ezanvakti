package com.nurvakti.global.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Her vakit için ezan/bildirim modu */
enum class AlertMode { FULL, SHORT, NOTIFY, VIBRATE, SILENT, OFF }

@Serializable data class PrayerAlert(
    val mode: AlertMode = AlertMode.FULL,
    val sound: String = "klasik",          // ezan katalog kimliği veya "custom"
    val customUri: String? = null,         // kullanıcının seçtiği ses dosyası (content://)
    val preMinutes: Int = 0,               // 0 = kapalı, 5/10/15/30
)

@Serializable data class SavedPlace(
    val name: String = "İstanbul", val region: String = "Türkiye", val lat: Double = 41.0082, val lng: Double = 28.9784,
    val tz: String = "Europe/Istanbul", val country: String = "TR", val gps: Boolean = false,
)

@Serializable data class Settings(
    val onboarded: Boolean = false,
    val place: SavedPlace = SavedPlace(),
    val travelMode: Boolean = false,
    val method: String = "DIYANET",
    val autoMethod: Boolean = true,
    val asr: String = "STANDARD",          // STANDARD (Şafii, Mâlikî, Hanbelî) | HANAFI
    val highLat: String = "ANGLE",         // NONE | MIDDLE | SEVENTH | ANGLE
    val offsets: Map<String, Int> = emptyMap(),
    val source: String = "LOCAL",          // LOCAL (cihazda hesap) | ALADHAN (çevrimiçi)
    val alerts: Map<String, PrayerAlert> = mapOf(
        "imsak" to PrayerAlert(mode = AlertMode.OFF), "fajr" to PrayerAlert(), "sunrise" to PrayerAlert(mode = AlertMode.OFF),
        "dhuhr" to PrayerAlert(), "asr" to PrayerAlert(), "maghrib" to PrayerAlert(), "isha" to PrayerAlert(),
    ),
    val theme: String = "SYSTEM",          // SYSTEM | LIGHT | DARK
    val lang: String = "tr",
    val quranFont: Int = 26,
    val translation: String = "tr",        // tr | en | none
    val showTranslit: Boolean = false,
    val reciter: String = "alafasy",
    val hijriAdjust: Int = 0,
    val lastSurah: Int = 1, val lastAyah: Int = 1,
    val zikirGoal: Int = 100,
    val prayerGoal: Int = 5,
    val manualGold: Double = 0.0, val manualSilver: Double = 0.0, val currency: String = "TRY",
)

private val Context.ds by preferencesDataStore("nur_settings")
private val KEY = stringPreferencesKey("settings_json")
val SettingsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class SettingsStore(private val ctx: Context) {
    val flow: Flow<Settings> = ctx.ds.data.map { p ->
        p[KEY]?.let { runCatching { SettingsJson.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings()
    }
    suspend fun get(): Settings = flow.first()
    suspend fun update(f: (Settings) -> Settings) {
        ctx.ds.edit { p ->
            val cur = p[KEY]?.let { runCatching { SettingsJson.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings()
            p[KEY] = SettingsJson.encodeToString(Settings.serializer(), f(cur))
        }
    }
    suspend fun clear() { ctx.ds.edit { it.clear() } }
}
