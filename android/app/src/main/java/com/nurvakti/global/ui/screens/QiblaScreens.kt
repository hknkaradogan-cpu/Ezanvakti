package com.nurvakti.global.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.SensorManager
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nurvakti.global.prayer.PrayerCalc
import com.nurvakti.global.qibla.Compass
import com.nurvakti.global.qibla.Heading
import com.nurvakti.global.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("SetJavaScriptEnabled")
@Composable fun OsmMap(mode: String, lat: Double, lng: Double, items: List<Triple<String, Double, Double>> = emptyList(), modifier: Modifier = Modifier) {
    val payload = JSONObject().put("mode", mode).put("lat", lat).put("lng", lng)
        .put("items", JSONArray().apply { items.forEach { put(JSONObject().put("name", it.first).put("lat", it.second).put("lng", it.third)) } }).toString()
    AndroidView(factory = { c ->
        WebView(c).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() { override fun onPageFinished(v: WebView, u: String?) { v.evaluateJavascript("show($payload)", null) } }
            loadUrl("file:///android_asset/map/map.html")
        }
    }, update = { it.evaluateJavascript("if(window.show)show($payload)", null) }, modifier = modifier)
}

@Composable fun QiblaScreen(vm: AppVm) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    val qibla = PrayerCalc.qibla(s.place.lat, s.place.lng)
    val dist = PrayerCalc.distanceKm(s.place.lat, s.place.lng)
    val hasSensor = remember { Compass.available(ctx) }
    var tab by remember { mutableIntStateOf(if (hasSensor) 0 else 1) }
    val flow = remember(s.place.lat, s.place.lng, hasSensor) { if (hasSensor) Compass.headings(ctx, s.place.lat, s.place.lng) else kotlinx.coroutines.flow.emptyFlow() }
    val heading by flow.collectAsState(initial = null as Heading?)
    Column(Modifier.fillMaxSize()) {
        Bar(t("Kıble"), actions = { IconButton({ vm.useGps() }) { Icon(Icons.Filled.MyLocation, t("Konumu yenile")) } })
        TabRow(tab) { Tab(tab == 0, { tab = 0 }, text = { Text(t("Pusula")) }, enabled = hasSensor); Tab(tab == 1, { tab = 1 }, text = { Text(t("Harita")) }) }
        if (tab == 1) {
            Text("${t("Kıble açısı")}: ${"%.1f".format(qibla)}° · ${t("Kâbe'ye mesafe")}: ${"%,.0f".format(dist)} km", Modifier.padding(12.dp))
            Text(t("Yeşil çizgi, bulunduğunuz yerden Kâbe'ye en kısa (büyük daire) yoldur. Haritadaki bir bina veya yol ile hizalanarak yön bulabilirsiniz. Harita için internet gerekir."),
                Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall)
            OsmMap("qibla", s.place.lat, s.place.lng, modifier = Modifier.fillMaxSize().padding(top = 8.dp))
            return@Column
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val h = heading
            val diff = h?.let { ((qibla - it.trueDeg + 540) % 360) - 180 }
            val aligned = diff != null && abs(diff) < 3
            Text(if (h == null) t("Sensör bekleniyor…") else if (aligned) t("Kıbleye yöneldiniz ✓") else if (diff!! > 0) "${t("Sağa dönün")} ${"%.0f".format(abs(diff))}°" else "${t("Sola dönün")} ${"%.0f".format(abs(diff))}°",
                style = MaterialTheme.typography.titleLarge, color = if (aligned) Brand.Emerald else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val ring = MaterialTheme.colorScheme.outline; val prim = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(300.dp)) {
                val r = size.minDimension / 2; val c = Offset(size.width / 2, size.height / 2)
                rotate(-(h?.trueDeg ?: 0f), c) {
                    drawCircle(ring, r - 4, c, style = Stroke(6f))
                    for (i in 0 until 72) { val a = Math.toRadians(i * 5.0); val l = if (i % 18 == 0) 30f else if (i % 2 == 0) 16f else 8f
                        drawLine(ring, Offset(c.x + (r - 8) * sin(a).toFloat(), c.y - (r - 8) * cos(a).toFloat()), Offset(c.x + (r - 8 - l) * sin(a).toFloat(), c.y - (r - 8 - l) * cos(a).toFloat()), 3f) }
                    // Kuzey işareti
                    drawCircle(Color(0xFFD32F2F), 10f, Offset(c.x, c.y - r + 46))
                    // Kıble oku
                    val qa = Math.toRadians(qibla)
                    val tip = Offset(c.x + (r - 40) * sin(qa).toFloat(), c.y - (r - 40) * cos(qa).toFloat())
                    drawLine(if (aligned) Brand.Emerald else Brand.Gold, c, tip, 14f)
                    drawCircle(if (aligned) Brand.Emerald else Brand.Gold, 22f, tip)
                }
                drawLine(prim, Offset(c.x, c.y - r - 4), Offset(c.x, c.y - r + 26), 8f) // telefonun üst yönü
                drawCircle(prim, 10f, c)
            }
            Spacer(Modifier.height(12.dp))
            Text("${t("Kıble açısı")}: ${"%.1f".format(qibla)}° (${t("gerçek kuzeyden")})", style = MaterialTheme.typography.titleMedium)
            Text("${t("Kâbe'ye mesafe")}: ${"%,.0f".format(dist)} km")
            h?.let { Text("${t("Telefon yönü")}: ${"%.0f".format(it.trueDeg)}° · ${t("Manyetik sapma")}: ${"%.1f".format(it.declination)}°", style = MaterialTheme.typography.bodySmall) }
            Text("${t("Konum")}: ${s.place.name}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            if (h != null && (h.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW)) SectionCard(t("Pusula kalibrasyonu gerekli")) {
                Text(t("Sensör doğruluğu düşük. Telefonu havada 8 rakamı çizer gibi birkaç kez yavaşça hareket ettirin. Mıknatıs, hoparlör, metal masa ve araç içinden uzak durun."))
            }
            if (h != null && !h.pitchOk) Text(t("Daha doğru sonuç için telefonu yere paralel tutun."), color = MaterialTheme.colorScheme.error)
            SectionCard(t("Nasıl kullanılır?")) {
                Text(t("1. Telefonu düz (yere paralel) tutun.\n2. Altın renkli ok telefonun üst kenarındaki çizgiyle hizalanana kadar kendi etrafınızda dönün.\n3. Hizalandığında ok yeşile döner.\nPusula sensörü yoksa veya güvenilmezse 'Harita' sekmesini kullanın."))
            }
        }
    }
}

// ---------------------------------------------------------------- CAMİLER (OpenStreetMap / Overpass)
data class Mosque(val name: String, val lat: Double, val lng: Double, val km: Double)

@Composable fun CamilerScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    var list by remember { mutableStateOf<List<Mosque>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var radius by remember { mutableIntStateOf(3000) }
    LaunchedEffect(s.place.lat, s.place.lng, radius) {
        list = null; err = null
        runCatching { withContext(Dispatchers.IO) { overpass(s.place.lat, s.place.lng, radius) } }
            .onSuccess { list = it }.onFailure { err = it.message ?: "Hata" }
    }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Yakındaki camiler"), back)
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000, 3000, 10000).forEach { r -> FilterChip(radius == r, { radius = r }, label = { Text("${r / 1000} km") }) }
        }
        when {
            err != null -> Text("${t("Cami verisi alınamadı")}: $err\n${t("İnternet bağlantınızı kontrol edin.")}", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
            list == null -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
            else -> {
                val l = list!!
                OsmMap("mosque", s.place.lat, s.place.lng, l.map { Triple(it.name, it.lat, it.lng) }, Modifier.fillMaxWidth().height(260.dp))
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (l.isEmpty()) item { Text(t("Bu yarıçapta OpenStreetMap'te kayıtlı cami bulunamadı. Yarıçapı büyütün.")) }
                    items(l) { m -> SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(m.name, style = MaterialTheme.typography.titleMedium); Text("%.2f km".format(m.km)) }
                            IconButton({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${m.lat},${m.lng}?q=${m.lat},${m.lng}(${URLEncoder.encode(m.name, "UTF-8")})"))
                                .let { Intent.createChooser(it, m.name) }) }) { Icon(Icons.Filled.Directions, t("Yol tarifi")) }
                        }
                    } }
                    item { SourceLine("© OpenStreetMap katkıcıları (ODbL) · Overpass API") }
                }
            }
        }
    }
}

private fun overpass(lat: Double, lng: Double, r: Int): List<Mosque> {
    val q = "[out:json][timeout:25];(node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$r,$lat,$lng);way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$r,$lat,$lng););out center 80;"
    val c = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; connectTimeout = 20000; readTimeout = 30000
        setRequestProperty("User-Agent", "NurVaktiGlobal/1.0"); setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    }
    c.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
    if (c.responseCode != 200) throw Exception("Overpass HTTP ${c.responseCode}")
    val o = Json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonObject
    return (o["elements"] as JsonArray).mapNotNull { e ->
        val eo = e.jsonObject
        val la = eo["lat"]?.jsonPrimitive?.double ?: eo["center"]?.jsonObject?.get("lat")?.jsonPrimitive?.double ?: return@mapNotNull null
        val lo = eo["lon"]?.jsonPrimitive?.double ?: eo["center"]?.jsonObject?.get("lon")?.jsonPrimitive?.double ?: return@mapNotNull null
        val tags = eo["tags"] as? JsonObject
        val name = tags?.get("name:tr")?.jsonPrimitive?.content ?: tags?.get("name")?.jsonPrimitive?.content ?: "Cami / Mescit"
        Mosque(name, la, lo, PrayerCalc.distanceKm(lat, lng, la, lo))
    }.sortedBy { it.km }
}
