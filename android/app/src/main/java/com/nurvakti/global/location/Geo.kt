package com.nurvakti.global.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nurvakti.global.data.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    fun distanceKm(a: Double, b: Double, c: Double, d: Double): Double {
        val r = 6371.0; val dp = Math.toRadians(c - a); val dl = Math.toRadians(d - b)
        val x = sin(dp / 2) * sin(dp / 2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(x))
    }

    /** Konum izni verilmişse cihazın anlık konumu (konum bilgisi hiçbir sunucuya gönderilmez). */
    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): android.location.Location? = runCatching {
        val c = LocationServices.getFusedLocationProviderClient(ctx)
        c.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() ?: c.lastLocation.await()
    }.getOrNull()

    /** Cihazın kendi Geocoder hizmetiyle şehir adı (ad, bölge, ülke kodu). */
    suspend fun reverse(ctx: Context, lat: Double, lng: Double): Triple<String, String, String?> = withContext(Dispatchers.IO) {
        val fallback = Triple("%.3f, %.3f".format(lat, lng), "", null as String?)
        if (!Geocoder.isPresent()) return@withContext fallback
        val g = Geocoder(ctx, Locale.getDefault())
        val list = runCatching {
            if (Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine<List<android.location.Address>> { cont -> g.getFromLocation(lat, lng, 1) { cont.resume(it) } }
            else @Suppress("DEPRECATION") g.getFromLocation(lat, lng, 1)
        }.getOrNull()
        val a = list?.firstOrNull() ?: return@withContext fallback
        val name = a.subAdminArea ?: a.locality ?: a.adminArea ?: fallback.first
        Triple(name, listOfNotNull(a.adminArea, a.countryName).distinct().joinToString(", "), a.countryCode)
    }

    fun norm(s: String): String = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        .replace('ı', 'i')

    fun search(all: List<Place>, q: String, limit: Int = 60): List<Place> {
        if (q.isBlank()) return all.filter { it.region.isEmpty() }.take(limit)
        val n = norm(q)
        return all.asSequence().filter { norm(it.name).contains(n) || norm(it.region).startsWith(n) }
            .sortedBy { if (norm(it.name).startsWith(n)) 0 else 1 }.take(limit).toList()
    }

    /** Ülkenin saat dilimleri arasından boylama en uygun olanı (çok saat dilimli ülkeler için). */
    fun zoneFor(country: String, lng: Double, tzList: List<List<String>>): String {
        val ids = android.icu.util.TimeZone.getAvailableIDs(country).toList().ifEmpty { tzList.map { it[0] } }
        if (ids.isEmpty()) return java.util.TimeZone.getDefault().id
        if (ids.size == 1) return ids[0]
        val now = System.currentTimeMillis()
        val target = lng / 15.0 * 3600_000
        return ids.minByOrNull { abs(java.util.TimeZone.getTimeZone(it).getOffset(now) - target) } ?: ids[0]
    }

    /** İsteğe bağlı çevrimiçi arama: OpenStreetMap Nominatim (ücretsiz, anahtarsız). Yalnızca kullanıcının yazdığı metin gönderilir. */
    suspend fun nominatim(q: String): List<Place> = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=15&addressdetails=1&accept-language=tr&q=" + URLEncoder.encode(q, "UTF-8")
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "NurVaktiGlobal/1.0 (Android; https://github.com/hknkaradogan-cpu/Ezanvakti)"); connectTimeout = 15000; readTimeout = 15000 }
        val arr = Json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonArray
        arr.map { e -> val o = e.jsonObject
            val addr = o["address"]?.jsonObject
            val cc = addr?.get("country_code")?.jsonPrimitive?.content?.uppercase() ?: ""
            val disp = o["display_name"]!!.jsonPrimitive.content
            Place(disp.substringBefore(","), disp.substringAfter(", ", ""), o["lat"]!!.jsonPrimitive.content.toDouble(), o["lon"]!!.jsonPrimitive.content.toDouble(), cc)
        }
    }
}
