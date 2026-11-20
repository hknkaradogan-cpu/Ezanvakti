package com.nurvakti.global.prayer

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** web/js/praytimes.js dosyasının birebir Kotlin karşılığı. İkisi de shared/tests altındaki test verisiyle doğrulanır. */
data class Method(
    val id: String, val name: String, val fajr: Double, val isha: Double? = null, val ishaMin: Double? = null,
    val maghribAngle: Double? = null, val tune: Map<String, Double> = emptyMap(), val aladhanId: Int,
)

object PrayerCalc {
    val PRAYERS = listOf("imsak", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
    val FIVE = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")
    val METHODS: List<Method> = listOf(
        Method("DIYANET", "Diyanet İşleri Başkanlığı (Türkiye)", 18.0, 17.0,
            tune = mapOf("sunrise" to -7.0, "dhuhr" to 5.0, "asr" to 4.6, "maghrib" to 8.1, "isha" to 1.5, "fajr" to 0.0), aladhanId = 13),
        Method("MWL", "Muslim World League", 18.0, 17.0, aladhanId = 3),
        Method("ISNA", "ISNA (Kuzey Amerika)", 15.0, 15.0, aladhanId = 2),
        Method("EGYPT", "Egyptian General Authority", 19.5, 17.5, aladhanId = 5),
        Method("MAKKAH", "Umm Al-Qura (Mekke)", 18.5, ishaMin = 90.0, aladhanId = 4),
        Method("KARACHI", "University of Islamic Sciences, Karachi", 18.0, 18.0, aladhanId = 1),
        Method("TEHRAN", "Institute of Geophysics, Tehran", 17.7, 14.0, maghribAngle = 4.5, aladhanId = 7),
        Method("JAKIM", "JAKIM (Malezya)", 20.0, 18.0, aladhanId = 17),
        Method("SINGAPORE", "MUIS (Singapur)", 20.0, 18.0, aladhanId = 11),
        Method("KEMENAG", "Kemenag (Endonezya)", 20.0, 18.0, aladhanId = 20),
        Method("KUWAIT", "Kuwait", 18.0, 17.5, aladhanId = 9),
        Method("QATAR", "Qatar", 18.0, ishaMin = 90.0, aladhanId = 10),
        Method("DUBAI", "Dubai (BAE)", 18.2, 18.2, aladhanId = 16),
        Method("GULF", "Gulf Region", 19.5, ishaMin = 90.0, aladhanId = 8),
        Method("FRANCE", "UOIF (Fransa)", 12.0, 12.0, aladhanId = 12),
        Method("RUSSIA", "Rusya Müslümanları Dini İdaresi", 16.0, 15.0, aladhanId = 14),
    )
    fun method(id: String) = METHODS.firstOrNull { it.id == id } ?: METHODS[1]

    /** Ülke koduna göre varsayılan yöntem */
    fun defaultMethodFor(cc: String): String = when (cc) {
        "TR", "AZ", "CY", "BA", "MK", "XK", "AL", "BG" -> "DIYANET"
        "US", "CA", "MX" -> "ISNA"
        "SA", "YE" -> "MAKKAH"
        "EG", "SD", "LY", "SY", "LB", "IQ", "JO", "PS" -> "EGYPT"
        "PK", "IN", "BD", "AF" -> "KARACHI"
        "IR" -> "TEHRAN"
        "MY", "BN" -> "JAKIM"
        "SG" -> "SINGAPORE"
        "ID" -> "KEMENAG"
        "KW" -> "KUWAIT"
        "QA" -> "QATAR"
        "AE" -> "DUBAI"
        "BH", "OM" -> "GULF"
        "FR" -> "FRANCE"
        "RU", "KZ", "UZ", "TJ", "KG", "TM" -> "RUSSIA"
        else -> "MWL"
    }

    private const val D2R = Math.PI / 180.0
    private const val R2D = 180.0 / Math.PI
    private fun dsin(d: Double) = sin(d * D2R)
    private fun dcos(d: Double) = cos(d * D2R)
    private fun dtan(d: Double) = tan(d * D2R)
    private fun dasin(x: Double) = R2D * asin(x)
    private fun dacos(x: Double) = R2D * acos(x)
    private fun datan2(y: Double, x: Double) = R2D * atan2(y, x)
    private fun dacot(x: Double) = R2D * atan(1.0 / x)
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }

    fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0; var m = m0
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0); val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val dd = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * dd, 360.0)
        val q = fix(280.459 + 0.98564736 * dd, 360.0)
        val l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * dd
        val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqt = q / 15.0 - fix(ra, 24.0)
        val decl = dasin(dsin(e) * dsin(l))
        return decl to eqt
    }

    data class Opt(
        val method: String = "MWL", val asr: String = "STANDARD", val highLat: String = "ANGLE",
        val elevation: Double = 0.0, val offsets: Map<String, Int> = emptyMap(),
    )

    /** Gün başından itibaren dakika cinsinden vakitler. tz saat cinsinden (ör. 3.0). */
    fun computeMinutes(y: Int, m: Int, d: Int, lat: Double, lng: Double, tz: Double, opt: Opt): Map<String, Double> {
        val method = method(opt.method)
        val asrFactor = if (opt.asr == "HANAFI") 2.0 else 1.0
        val jDate = julian(y, m, d) - lng / (15.0 * 24.0)
        val riseAngle = 0.833 + 0.0347 * sqrt(max(0.0, opt.elevation))

        fun midDay(t: Double): Double { val (_, eqt) = sunPosition(jDate + t); return fix(12 - eqt, 24.0) }
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Pair<Double, Boolean> {
            val (decl, _) = sunPosition(jDate + t)
            val noon = midDay(t)
            val v = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            val tt = dacos(max(-1.0, min(1.0, v))) / 15.0
            return (noon + if (ccw) -tt else tt) to (v < -1 || v > 1)
        }
        fun asrTime(factor: Double, t: Double): Double {
            val (decl, _) = sunPosition(jDate + t)
            val angle = -dacot(factor + dtan(abs(lat - decl)))
            return angleTime(angle, t, false).first
        }

        var t = mutableMapOf("fajr" to 5.0, "sunrise" to 6.0, "dhuhr" to 12.0, "asr" to 13.0, "sunset" to 18.0, "maghrib" to 18.0, "isha" to 18.0)
        var invFajr = false; var invIsha = false
        repeat(2) {
            fun p(k: String) = t.getValue(k) / 24.0
            val f = angleTime(method.fajr, p("fajr"), true)
            val sr = angleTime(riseAngle, p("sunrise"), true)
            val ss = angleTime(riseAngle, p("sunset"), false)
            val mg = method.maghribAngle?.let { angleTime(it, p("maghrib"), false) } ?: ss
            val isA = method.isha?.let { angleTime(it, p("isha"), false) } ?: (Double.NaN to false)
            t = mutableMapOf("fajr" to f.first, "sunrise" to sr.first, "dhuhr" to midDay(p("dhuhr")), "asr" to asrTime(asrFactor, p("asr")),
                "sunset" to ss.first, "maghrib" to mg.first, "isha" to isA.first)
            invFajr = f.second; invIsha = isA.second
        }
        for (k in t.keys.toList()) t[k] = t.getValue(k) + tz - lng / 15.0
        method.ishaMin?.let { t["isha"] = t.getValue("maghrib") + it / 60.0 }

        if (opt.highLat != "NONE") {
            val night = fix(t.getValue("sunrise") - t.getValue("sunset"), 24.0)
            fun portion(a: Double) = when (opt.highLat) { "MIDDLE" -> 0.5; "SEVENTH" -> 1.0 / 7.0; else -> a / 60.0 }
            val fp = portion(method.fajr) * night
            val fajr = t.getValue("fajr")
            if (invFajr || fajr.isNaN() || fix(t.getValue("sunrise") - fajr, 24.0) > fp) t["fajr"] = t.getValue("sunrise") - fp
            if (method.ishaMin == null && method.isha != null) {
                val ip = portion(method.isha) * night
                val isha = t.getValue("isha")
                if (invIsha || isha.isNaN() || fix(isha - t.getValue("sunset"), 24.0) > ip) t["isha"] = t.getValue("sunset") + ip
            }
        }
        val tune = method.tune.toMutableMap()
        opt.offsets.forEach { (k, v) -> tune[k] = (tune[k] ?: 0.0) + v }
        return mapOf(
            "imsak" to t.getValue("fajr") * 60 + (tune["imsak"] ?: tune["fajr"] ?: 0.0),
            "fajr" to t.getValue("fajr") * 60 + (tune["fajr"] ?: 0.0),
            "sunrise" to t.getValue("sunrise") * 60 + (tune["sunrise"] ?: 0.0),
            "dhuhr" to t.getValue("dhuhr") * 60 + (tune["dhuhr"] ?: 0.0),
            "asr" to t.getValue("asr") * 60 + (tune["asr"] ?: 0.0),
            "maghrib" to t.getValue("maghrib") * 60 + (tune["maghrib"] ?: 0.0),
            "isha" to t.getValue("isha") * 60 + (tune["isha"] ?: 0.0),
        )
    }

    fun fmt(min: Double): String {
        if (min.isNaN() || min.isInfinite()) return "--:--"
        var m = Math.round(min).toInt(); m = ((m % 1440) + 1440) % 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }

    // ---------- Kıble ----------
    const val KAABA_LAT = 21.4224779
    const val KAABA_LNG = 39.8262136
    fun qibla(lat: Double, lng: Double): Double {
        val dl = (KAABA_LNG - lng) * D2R; val p1 = lat * D2R; val p2 = KAABA_LAT * D2R
        val b = atan2(sin(dl), cos(p1) * tan(p2) - sin(p1) * cos(dl))
        return (b * R2D + 360) % 360
    }
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double = KAABA_LAT, lng2: Double = KAABA_LNG): Double {
        val r = 6371.0088; val dp = (lat2 - lat1) * D2R; val dl = (lng2 - lng1) * D2R
        val a = sin(dp / 2).let { it * it } + cos(lat1 * D2R) * cos(lat2 * D2R) * sin(dl / 2).let { it * it }
        return 2 * r * asin(sqrt(a))
    }
}
