package com.nurvakti.global.prayer

import android.content.Context
import com.nurvakti.global.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class DayTimes(val date: LocalDate, val zone: ZoneId, val times: Map<String, ZonedDateTime>, val source: String) {
    fun at(k: String): ZonedDateTime = times.getValue(k)
    fun hhmm(k: String): String = times[k]?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--"
}

/** Vakit hesabı: varsayılan olarak cihaz üzerinde (internetsiz). İstenirse AlAdhan API'den aylık veri alınır ve önbelleğe yazılır. */
object Times {
    private val json = Json { ignoreUnknownKeys = true }

    fun zoneOf(s: Settings): ZoneId = runCatching { ZoneId.of(s.place.tz) }.getOrDefault(ZoneId.systemDefault())
    fun methodId(s: Settings) = if (s.autoMethod) PrayerCalc.defaultMethodFor(s.place.country) else s.method

    fun sourceLabel(s: Settings, online: Boolean): String {
        val m = PrayerCalc.method(methodId(s))
        return if (online) "AlAdhan API (api.aladhan.com) · ${m.name}" else "Cihaz üzerinde hesaplama · ${m.name}"
    }

    fun local(s: Settings, date: LocalDate): DayTimes {
        val zone = zoneOf(s)
        val tzH = zone.rules.getOffset(date.atTime(12, 0).atZone(zone).toInstant()).totalSeconds / 3600.0
        val mins = PrayerCalc.computeMinutes(date.year, date.monthValue, date.dayOfMonth, s.place.lat, s.place.lng, tzH,
            PrayerCalc.Opt(methodId(s), s.asr, s.highLat, 0.0, s.offsets))
        val base = date.atStartOfDay(zone)
        val times = PrayerCalc.PRAYERS.associateWith { k ->
            val m = mins.getValue(k)
            if (m.isNaN()) base.plusHours(12) else date.atStartOfDay().plusMinutes(Math.round(m)).atZone(zone)
        }
        return DayTimes(date, zone, times, sourceLabel(s, false))
    }

    /** Ayarlarda çevrimiçi kaynak seçildiyse önbellekteki AlAdhan verisi; yoksa cihaz hesabı. */
    fun day(ctx: Context, s: Settings, date: LocalDate): DayTimes {
        if (s.source == "ALADHAN") cached(ctx, s, date)?.let { return it }
        return local(s, date)
    }

    fun range(ctx: Context, s: Settings, from: LocalDate, days: Int) = (0 until days).map { day(ctx, s, from.plusDays(it.toLong())) }

    /** Sıradaki vakit (imsak hariç 6 vakit: sabah, güneş, öğle, ikindi, akşam, yatsı). */
    fun next(ctx: Context, s: Settings, now: ZonedDateTime = ZonedDateTime.now(zoneOf(s))): Pair<String, ZonedDateTime> {
        val keys = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        for (i in 0..1) {
            val d = day(ctx, s, now.toLocalDate().plusDays(i.toLong()))
            keys.firstOrNull { d.at(it).isAfter(now) }?.let { return it to d.at(it) }
        }
        val d = day(ctx, s, now.toLocalDate().plusDays(1)); return "fajr" to d.at("fajr")
    }
    fun current(ctx: Context, s: Settings, now: ZonedDateTime = ZonedDateTime.now(zoneOf(s))): String? {
        val d = day(ctx, s, now.toLocalDate())
        return listOf("isha", "maghrib", "asr", "dhuhr", "sunrise", "fajr").firstOrNull { !d.at(it).isAfter(now) }
    }

    // ---------------- AlAdhan (çevrimiçi) ----------------
    private fun cacheFile(ctx: Context, s: Settings, y: Int, m: Int): File {
        val key = "%.3f_%.3f_%s_%s_%d_%02d".format(s.place.lat, s.place.lng, methodId(s), s.asr, y, m)
        return File(ctx.cacheDir, "aladhan/$key.json")
    }
    private fun cached(ctx: Context, s: Settings, date: LocalDate): DayTimes? {
        val f = cacheFile(ctx, s, date.year, date.monthValue); if (!f.exists()) return null
        return runCatching {
            val arr = json.parseToJsonElement(f.readText()).jsonObject["data"]!!.jsonArray
            val o = arr[date.dayOfMonth - 1].jsonObject
            val t = o["timings"]!!.jsonObject
            val zone = ZoneId.of(o["meta"]!!.jsonObject["timezone"]!!.jsonPrimitive.content)
            fun p(k: String): ZonedDateTime {
                val v = t[k]!!.jsonPrimitive.content.substring(0, 5).split(":")
                val base = date.atTime(v[0].toInt(), v[1].toInt()).atZone(zone)
                return base.plusMinutes((s.offsets[mapKey(k)] ?: 0).toLong())
            }
            DayTimes(date, zone, mapOf("imsak" to p("Fajr"), "fajr" to p("Fajr"), "sunrise" to p("Sunrise"), "dhuhr" to p("Dhuhr"),
                "asr" to p("Asr"), "maghrib" to p("Maghrib"), "isha" to p("Isha")), sourceLabel(s, true))
        }.getOrNull()
    }
    private fun mapKey(k: String) = mapOf("Fajr" to "fajr", "Sunrise" to "sunrise", "Dhuhr" to "dhuhr", "Asr" to "asr", "Maghrib" to "maghrib", "Isha" to "isha")[k] ?: k

    /** Bu ayın ve gelecek ayın verisini indirir. Hata olursa mesaj döner; uygulama cihaz hesabıyla devam eder. */
    suspend fun fetchAlAdhan(ctx: Context, s: Settings): String? = withContext(Dispatchers.IO) {
        val now = LocalDate.now()
        val m = PrayerCalc.method(methodId(s))
        for (d in listOf(now, now.plusMonths(1))) {
            val url = "https://api.aladhan.com/v1/calendar/${d.year}/${d.monthValue}?latitude=${s.place.lat}&longitude=${s.place.lng}" +
                "&method=${m.aladhanId}&school=${if (s.asr == "HANAFI") 1 else 0}&timezonestring=${s.place.tz}"
            try {
                val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 20000; setRequestProperty("User-Agent", "NurVaktiGlobal/1.0") }
                if (c.responseCode != 200) return@withContext "AlAdhan HTTP ${c.responseCode}"
                val body = c.inputStream.bufferedReader().readText()
                val o = json.parseToJsonElement(body) as JsonObject
                if (o["code"]?.jsonPrimitive?.content != "200") return@withContext "AlAdhan: beklenmeyen yanıt"
                cacheFile(ctx, s, d.year, d.monthValue).apply { parentFile?.mkdirs(); writeText(body) }
            } catch (e: Exception) { return@withContext "AlAdhan'a ulaşılamadı: ${e.message}" }
        }
        null
    }

    fun nowZ(s: Settings): ZonedDateTime = ZonedDateTime.ofInstant(Instant.now(), zoneOf(s))
}
