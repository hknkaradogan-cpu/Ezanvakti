package com.nurvakti.global

import com.nurvakti.global.prayer.PrayerCalc
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class PrayerCalcTest {
    private fun res(n: String) = JSONObject(javaClass.classLoader!!.getResource(n).readText())
    private fun toMin(s: String) = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    /** Diyanet'in yayımladığı resmî vakitlerle karşılaştırma (±1 dk) */
    @Test fun diyanetOfficialTimes() {
        val j = res("diyanet_reference.json"); val tz = j.getDouble("tz"); val cases = j.getJSONArray("cases")
        val keys = listOf("imsak", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        var worst = 0
        for (i in 0 until cases.length()) {
            val c = cases.getJSONArray(i); val (y, m, d) = c.getString(3).split("-").map { it.toInt() }
            val t = PrayerCalc.computeMinutes(y, m, d, c.getDouble(1), c.getDouble(2), tz, PrayerCalc.Opt("DIYANET", "STANDARD", "NONE"))
            c.getString(4).split(" ").forEachIndexed { k, v ->
                val diff = abs(t.getValue(keys[k]).roundToInt() - toMin(v)); worst = maxOf(worst, diff)
                assertTrue("${c.getString(0)} ${c.getString(3)} ${keys[k]}: hesap ${PrayerCalc.fmt(t.getValue(keys[k]))} resmî $v", diff <= 1)
            }
        }
        println("Diyanet en büyük sapma: $worst dk")
    }

    /** Kotlin motoru ile web (JS) motorunun aynı sonucu verdiğini doğrular (CI'da node ile üretilen golden dosya). */
    @Test fun matchesJsEngine() {
        val url = javaClass.classLoader!!.getResource("golden_js.json") ?: return
        val arr = org.json.JSONArray(url.readText())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val (y, m, d) = o.getString("date").split("-").map { it.toInt() }
            val t = PrayerCalc.computeMinutes(y, m, d, o.getDouble("lat"), o.getDouble("lng"), o.getDouble("tz"),
                PrayerCalc.Opt(o.getString("method"), o.getString("asr"), o.getString("highLat")))
            val exp = o.getJSONObject("times")
            for (k in PrayerCalc.PRAYERS) assertEquals("${o.getString("name")} $k", exp.getString(k), PrayerCalc.fmt(t.getValue(k)))
        }
    }

    @Test fun hanafiAsrIsLater() {
        val std = PrayerCalc.computeMinutes(2026, 11, 20, 41.0082, 28.9784, 3.0, PrayerCalc.Opt("DIYANET", "STANDARD"))
        val han = PrayerCalc.computeMinutes(2026, 11, 20, 41.0082, 28.9784, 3.0, PrayerCalc.Opt("DIYANET", "HANAFI"))
        val diff = han.getValue("asr") - std.getValue("asr")
        assertTrue("Hanefi ikindi farkı $diff dk", diff in 30.0..80.0)
    }

    @Test fun qiblaIstanbul() {
        val q = PrayerCalc.qibla(41.0082, 28.9784)
        assertEquals(151.6, q, 0.5)
        assertEquals(2400.0, PrayerCalc.distanceKm(41.0082, 28.9784), 60.0)
    }

    @Test fun highLatitudeNoNaN() {
        val t = PrayerCalc.computeMinutes(2026, 6, 21, 59.9139, 10.7522, 2.0, PrayerCalc.Opt("MWL", "STANDARD", "ANGLE"))
        PrayerCalc.PRAYERS.forEach { assertTrue("$it NaN", !t.getValue(it).isNaN()) }
    }
}
