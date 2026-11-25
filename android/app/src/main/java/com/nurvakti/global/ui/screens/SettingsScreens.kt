package com.nurvakti.global.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nurvakti.global.alarm.AdhanService
import com.nurvakti.global.alarm.AlarmScheduler
import com.nurvakti.global.data.AlertMode
import com.nurvakti.global.data.Place
import com.nurvakti.global.data.PrayerAlert
import com.nurvakti.global.data.SavedPlaceRow
import com.nurvakti.global.data.SettingsJson
import com.nurvakti.global.location.Geo
import com.nurvakti.global.prayer.PrayerCalc
import com.nurvakti.global.prayer.Times
import com.nurvakti.global.ui.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonElement

@Composable fun <T> Choice(label: String, options: List<Pair<T, String>>, value: T, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(options.firstOrNull { it.first == value }?.second ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(open, { open = false }) { options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { open = false; onPick(v) }) } }
    }
}

@Composable fun SwitchRow(label: String, checked: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyLarge); sub?.let { Text(it, style = MaterialTheme.typography.bodySmall) } }
        Switch(checked, onChange)
    }
}

// ---------------------------------------------------------------- GENEL AYARLAR
@Composable fun SettingsScreen(vm: AppVm, back: () -> Unit, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val d = vm.dao
            val obj = buildJsonObject {
                put("app", "NAMAZ VAKTİ: EZAN VE KIBLE"); put("exported", java.time.ZonedDateTime.now().toString())
                put("settings", SettingsJson.encodeToJsonElement(com.nurvakti.global.data.Settings.serializer(), s))
                put("prayerLog", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.PrayerLog.serializer()), d.allLogs()))
                put("kaza", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.Kaza.serializer()), d.allKaza()))
                put("fasts", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.FastLog.serializer()), d.allFasts()))
                put("zikir", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.ZikirSession.serializer()), d.allZikir()))
                put("bookmarks", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.Bookmark.serializer()), d.allBookmarks()))
                put("favorites", SettingsJson.encodeToJsonElement(ListSerializer(com.nurvakti.global.data.Favorite.serializer()), d.allFavs()))
                put("places", SettingsJson.encodeToJsonElement(ListSerializer(SavedPlaceRow.serializer()), d.allPlaces()))
            }
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(obj.toString().toByteArray()) }
            vm.toast("Veriler dışa aktarıldı")
        }
    }
    val pinSent = t("Widget isteği gönderildi. Pencere açılmazsa aşağıdaki yönergeye bakın.")
    val pinHelp = t("Widget eklemek için ana ekrana uzun basın, 'Widget'lar' bölümünden Namaz Vakti'ni seçin.")
    val pinWidget = { large: Boolean ->
        vm.toast(if (com.nurvakti.global.widget.Widgets.requestPin(ctx, large)) pinSent else pinHelp)
    }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Ayarlar"), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(t("Genel")) {
                Choice(t("Dil"), listOf("tr" to "Türkçe", "en" to "English", "ar" to "العربية"), s.lang) { v -> vm.update { it.copy(lang = v) } }
                Choice(t("Tema"), listOf("SYSTEM" to t("Otomatik"), "LIGHT" to t("Açık"), "DARK" to t("Koyu")), s.theme) { v -> vm.update { it.copy(theme = v) } }
                Text(t("Diğer diller (Almanca, Fransızca, İspanyolca, Urduca, Endonezce, Malayca, Farsça, Rusça) sonraki sürümlerde eklenecek."), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Konum")) {
                KeyValue(s.place.name, s.place.tz)
                Button({ go("location") }, Modifier.fillMaxWidth()) { Text(t("Konumu değiştir")) }
            }
            SectionCard(t("Ezan ve bildirimler")) { Button({ go("ezan") }, Modifier.fillMaxWidth()) { Text(t("Ezan ayarlarını aç")) }
                OutlinedButton({ go("battery") }, Modifier.fillMaxWidth()) { Text(t("Pil ve alarm rehberi")) } }
            SectionCard(t("Ana ekran widget'ı")) {
                Text(t("Sonraki vakit ve canlı geri sayım ya da günün tüm vakitleri ana ekranınızda görünür."), style = MaterialTheme.typography.bodySmall)
                Button({ pinWidget(false) }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Widgets, null); Spacer(Modifier.width(8.dp)); Text(t("Geri sayım widget'ı ekle")) }
                OutlinedButton({ pinWidget(true) }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Widgets, null); Spacer(Modifier.width(8.dp)); Text(t("Günün vakitleri widget'ı ekle")) }
                Text(t("Widget penceresi açılmazsa (Xiaomi/Redmi/POCO gibi telefonlarda sık görülür): Uygulama izinleri → Diğer izinler → 'Ana ekrana kısayol oluştur' iznini verin, sonra tekrar deneyin. Ya da ana ekrana uzun basıp 'Widget'lar' bölümünden Namaz Vakti'ni seçin."), style = MaterialTheme.typography.bodySmall)
                TextButton({ ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text(t("Uygulama izinlerini aç")) }
            }
            SectionCard(t("Hesaplama yöntemi")) {
                SwitchRow(t("Ülkeye göre otomatik seç"), s.autoMethod, "${t("Şu an")}: ${PrayerCalc.method(Times.methodId(s)).name}") { v -> vm.update { it.copy(autoMethod = v) } }
                if (!s.autoMethod) Choice(t("Yöntem"), PrayerCalc.METHODS.map { it.id to it.name }, s.method) { v -> vm.update { it.copy(method = v) } }
                Choice(t("İkindi hesabı"), listOf("STANDARD" to t("Standart (Şafii, Mâlikî, Hanbelî)"), "HANAFI" to t("Hanefi")), s.asr) { v -> vm.update { it.copy(asr = v) } }
                Choice(t("Yüksek enlem kuralı"), listOf("ANGLE" to t("Açı tabanlı (önerilen)"), "MIDDLE" to t("Gecenin yarısı"), "SEVENTH" to t("Gecenin yedide biri"), "NONE" to t("Yok")), s.highLat) { v -> vm.update { it.copy(highLat = v) } }
                Choice(t("Veri kaynağı"), listOf("LOCAL" to t("Cihazda hesapla (internetsiz)"), "ALADHAN" to t("AlAdhan API (çevrimiçi)")), s.source) { v -> vm.update { it.copy(source = v) }; if (v == "ALADHAN") vm.fetchOnline() }
                if (s.source == "ALADHAN") OutlinedButton({ vm.fetchOnline() }, Modifier.fillMaxWidth()) { Text(t("AlAdhan verisini şimdi güncelle")) }
                Text(t("Diyanet'in resmî 'Awqat Salah' API'si kurumdan başvuru ile alınan kullanıcı adı ve şifre gerektirir; bu sürümde kullanılmamaktadır. Diyanet yöntemi cihazda hesaplanır ve gösterilen kaynak her zaman yazılır."), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Vakit düzeltmeleri (dakika)")) {
                listOf("imsak", "sunrise", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
                    val v = s.offsets[k] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prayerName(k), Modifier.weight(1f))
                        IconButton({ vm.update { it.copy(offsets = it.offsets + (k to v - 1)) } }) { Icon(Icons.Filled.Remove, "-1") }
                        Text(if (v > 0) "+$v" else "$v", Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                        IconButton({ vm.update { it.copy(offsets = it.offsets + (k to v + 1)) } }) { Icon(Icons.Filled.Add, "+1") }
                    }
                }
            }
            SectionCard(t("Hicri takvim")) {
                Choice(t("Hicri tarih düzeltmesi"), (-2..2).map { it to (if (it > 0) "+$it" else "$it") + " " + t("gün") }, s.hijriAdjust) { v -> vm.update { it.copy(hijriAdjust = v) } }
                Text(t("Hicri tarih Ümmü'l-Kurâ takvimiyle hesaplanır. Ülkenizdeki resmî ilanla 1-2 gün fark varsa buradan düzeltebilirsiniz. Türkiye'de dini günler Diyanet'in yayımladığı listeden gösterilir."), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Kur'an")) {
                Choice(t("Meal"), listOf("tr" to "Türkçe (Diyanet)", "en" to "English (Saheeh Intl.)", "none" to t("Gösterme")), s.translation) { v -> vm.update { it.copy(translation = v) } }
                SwitchRow(t("Latin okunuşu göster"), s.showTranslit) { v -> vm.update { it.copy(showTranslit = v) } }
                Text("${t("Yazı boyutu")}: ${s.quranFont}")
                Slider(s.quranFont.toFloat(), { v -> vm.update { it.copy(quranFont = v.toInt()) } }, valueRange = 16f..48f)
            }
            SectionCard(t("Gizlilik ve verilerim")) {
                Text(t("Namaz takibi, zikir, yer imleri ve ayarlar yalnızca bu telefonda saklanır. Uygulamada reklam, hesap veya analiz aracı yoktur."))
                OutlinedButton({ exportLauncher.launch("nurvakti-verilerim.json") }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Upload, null); Text(t("Verilerimi dışa aktar (JSON)")) }
                OutlinedButton({ confirmDelete = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.DeleteForever, null); Text(t("Tüm verilerimi sil")) }
                TextButton({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(com.nurvakti.global.BuildConfig.WEB_BASE + "gizlilik.html"))) }) { Text(t("Gizlilik politikası")) }
            }
            TextButton({ go("about") }) { Text(t("Hakkında ve lisanslar")) }
        }
    }
    if (confirmDelete) AlertDialog({ confirmDelete = false }, title = { Text(t("Tüm veriler silinsin mi?")) },
        text = { Text(t("Namaz takibi, zikir geçmişi, yer imleri, indirilen sesler ve ayarlar kalıcı olarak silinir.")) },
        confirmButton = { TextButton({ confirmDelete = false; scope.launch {
            vm.store.clear(); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.nurvakti.global.data.AppDb.get(ctx).clearAllTables(); ctx.filesDir.resolve("media").deleteRecursively(); ctx.cacheDir.deleteRecursively() }
            vm.toast("Tüm veriler silindi") } }) { Text(t("Sil")) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(t("Vazgeç")) } })
}

// ---------------------------------------------------------------- KONUM
@Composable fun LocationScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var country by remember { mutableStateOf(s.place.country) }
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<Place>>(emptyList()) }
    var lat by remember { mutableStateOf("") }; var lng by remember { mutableStateOf("") }
    val saved by vm.dao.places().collectAsState(emptyList())
    val places = remember(country) { vm.content.places(country) }
    val results = remember(q, places) { Geo.search(places, q) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> if (r.values.any { it }) vm.useGps() else vm.toast("Konum izni verilmedi") }
    val countryName = { cc: String -> java.util.Locale("", cc).getDisplayCountry(java.util.Locale(s.lang)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Bar(t("Konum"), back) }
        item { SectionCard(t("Geçerli konum")) {
            Text("${s.place.name}${if (s.place.region.isNotEmpty()) ", " + s.place.region else ""}", style = MaterialTheme.typography.titleMedium)
            Text("${"%.4f".format(s.place.lat)}, ${"%.4f".format(s.place.lng)} · ${s.place.tz}", style = MaterialTheme.typography.bodySmall)
            Button({ perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(8.dp)); Text(t("GPS ile otomatik bul")) }
            SwitchRow(t("Seyahat modu"), s.travelMode, t("Konumunuz 15 km'den fazla değişirse vakitler ve alarmlar otomatik güncellenir.")) { v ->
                if (v && ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                vm.update { it.copy(travelMode = v) } }
            OutlinedButton({ scope.launch { vm.dao.addPlace(SavedPlaceRow(0, s.place.name, s.place.region, s.place.lat, s.place.lng, s.place.tz, s.place.country)); vm.toast("Konum kaydedildi") } },
                Modifier.fillMaxWidth()) { Icon(Icons.Filled.BookmarkAdd, null); Text(t("Bu konumu kaydet")) }
        } }
        if (saved.isNotEmpty()) item { SectionCard(t("Kayıtlı konumlar")) {
            saved.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${p.name} · ${p.tz}", Modifier.weight(1f).clickable { vm.update { it.copy(place = com.nurvakti.global.data.SavedPlace(p.name, p.region, p.lat, p.lng, p.tz, p.country, false)) } })
                IconButton({ scope.launch { vm.dao.delPlace(p.id) } }) { Icon(Icons.Filled.Delete, t("Sil")) } } }
        } }
        item { SectionCard(t("Ülke, il ve ilçe seç")) {
            Choice(t("Ülke"), vm.content.countries.map { it.c to countryName(it.c) }.sortedBy { it.second }, country) { country = it; q = "" }
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(t("Şehir / ilçe ara")) }, leadingIcon = { Icon(Icons.Filled.Search, null) })
            Text(t("Liste internetsiz çalışır (il/eyalet ve ilçe/şehirler)."), style = MaterialTheme.typography.bodySmall)
        } }
        items(results) { p -> SectionCard(onClick = { vm.setPlace(p); back() }) {
            Text(p.name, style = MaterialTheme.typography.titleMedium); Text(if (p.region.isEmpty()) t("İl / eyalet") else p.region, style = MaterialTheme.typography.bodySmall) } }
        item { SectionCard(t("Çevrimiçi arama (OpenStreetMap)")) {
            Text(t("Listede bulamazsanız yazdığınız metin OpenStreetMap Nominatim'e gönderilerek aranır."), style = MaterialTheme.typography.bodySmall)
            Button({ scope.launch { online = runCatching { Geo.nominatim(q) }.getOrElse { vm.toast("Arama başarısız: ${it.message}"); emptyList() } } }, enabled = q.length >= 2) { Text(t("Çevrimiçi ara")) }
            online.forEach { p -> Text("${p.name} — ${p.region}", Modifier.fillMaxWidth().clickable { vm.setPlace(p); back() }.padding(vertical = 8.dp)) }
        } }
        item { SectionCard(t("Koordinatla konum")) {
            OutlinedTextField(lat, { lat = it }, label = { Text(t("Enlem (ör. 41.0082)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(lng, { lng = it }, label = { Text(t("Boylam (ör. 28.9784)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            Button({
                val la = lat.replace(',', '.').toDoubleOrNull(); val lo = lng.replace(',', '.').toDoubleOrNull()
                if (la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0) vm.toast("Geçersiz koordinat")
                else { vm.setPlace(Place("%.4f, %.4f".format(la, lo), "", la, lo, country)); back() }
            }) { Text(t("Uygula")) }
        } }
    }
}

// ---------------------------------------------------------------- EZAN AYARLARI
@SuppressLint("BatteryLife")
@Composable fun EzanSettingsScreen(vm: AppVm, back: () -> Unit, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    val downloads by vm.dao.downloads().collectAsState(emptyList())
    var pickFor by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val k = pickFor
        if (uri != null && k != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.update { it.copy(alerts = it.alerts + (k to (it.alerts[k] ?: PrayerAlert()).copy(sound = "custom", customUri = uri.toString()))) }
        }
    }
    val notifOk = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val exactOk = AlarmScheduler.canExact(ctx)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Bar(t("Ezan ayarları"), back) }
        item { SectionCard(t("İzin durumu")) {
            KeyValue(t("Bildirim izni"), if (notifOk) "✓" else "✗", true)
            if (!notifOk) Button({ notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text(t("Bildirim izni ver")) }
            KeyValue(t("Kesin alarm izni"), if (exactOk) "✓" else "✗", true)
            if (!exactOk && Build.VERSION.SDK_INT >= 31) {
                Text(t("İzin olmadan ezan birkaç dakika gecikebilir."), style = MaterialTheme.typography.bodySmall)
                Button({ ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) }) { Text(t("Kesin alarm iznini aç")) }
            }
            OutlinedButton({ go("battery") }, Modifier.fillMaxWidth()) { Text(t("Pil optimizasyonu rehberi")) }
            OutlinedButton({ AdhanService.stop(ctx) }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Stop, null); Text(t("Çalan ezanı durdur")) }
        } }
        items(listOf("fajr", "dhuhr", "asr", "maghrib", "isha", "imsak", "sunrise")) { k ->
            val a = s.alerts[k] ?: PrayerAlert(mode = AlertMode.OFF)
            fun set(n: PrayerAlert) = vm.update { it.copy(alerts = it.alerts + (k to n)) }
            SectionCard(prayerName(k) + if (k == "fajr") " (${t("sabah ezanı")})" else "") {
                Choice(t("Uyarı türü"), AlertMode.entries.map { it to t(MODE_TR.getValue(it)) }, a.mode) { set(a.copy(mode = it)) }
                if (a.mode == AlertMode.FULL) {
                    Choice(t("Ezan sesi"), vm.content.ezans.map { it.id to t(it.tr) } + ("custom" to t("Telefondan dosya seç…")), a.sound) {
                        if (it == "custom") { pickFor = k; picker.launch(arrayOf("audio/*")) } else set(a.copy(sound = it)) }
                    if (a.sound == "custom") Text(t("Seçilen dosya") + ": " + (a.customUri?.let { Uri.parse(it).lastPathSegment } ?: "-"), style = MaterialTheme.typography.bodySmall)
                }
                Choice(t("Vakit öncesi hatırlatma"), listOf(0, 5, 10, 15, 30).map { it to if (it == 0) t("Kapalı") else "$it ${t("dakika önce")}" }, a.preMinutes) { set(a.copy(preMinutes = it)) }
                if (a.mode == AlertMode.FULL || a.mode == AlertMode.SHORT)
                    TextButton({ AdhanService.preview(ctx, if (a.mode == AlertMode.SHORT) "kisa" else a.sound, a.customUri) }) { Icon(Icons.Filled.PlayArrow, null); Text(t("Önizle")) }
            }
        }
        item { SectionCard(t("Hatırlatmalar")) {
            Choice(t("Namazdan sonra \"kıldın mı?\" sorusu"), listOf(0, 10, 15, 30, 45).map { it to if (it == 0) t("Kapalı") else "$it ${t("dakika sonra")}" }, s.checkMinutes) { v -> vm.update { it.copy(checkMinutes = v) } }
            Text(t("Bildirimdeki \"Kıldım ✓\" düğmesine basınca namaz takibine işlenir ve bildirim kapanır. Zaten işaretlediyseniz soru gelmez."), style = MaterialTheme.typography.bodySmall)
            Choice(t("Hadis bildirimi"), listOf("OFF" to t("Kapalı"), "DAILY" to t("Günde bir"), "HOURLY" to t("Saatte bir (08:00–22:00)")), s.hadisMode) { v -> vm.update { it.copy(hadisMode = v) } }
            if (s.hadisMode == "DAILY") Choice(t("Saat"), (5..23).map { it to "%02d:00".format(it) }, s.hadisHour) { v -> vm.update { it.copy(hadisHour = v) } }
            if (s.hadisMode != "OFF") Choice(t("İçerik"), listOf("BOTH" to t("Hadis ve ayet (sırayla)"), "HADIS" to t("Yalnızca hadis"), "AYET" to t("Yalnızca ayet")), s.hadisContent) { v -> vm.update { it.copy(hadisContent = v) } }
            Text(t("Kilit ekranında tam metni görmek için telefonun bildirim ayarında \"Kilit ekranında tüm içeriği göster\" açık olmalı."), style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ ctx.startActivity(Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                .putExtra(AndroidSettings.EXTRA_CHANNEL_ID, com.nurvakti.global.alarm.Channels.HADIS)) }, Modifier.fillMaxWidth()) { Text(t("Kilit ekranı bildirim ayarını aç")) }
        } }
        item { Text(t("Ezan sesleri"), style = MaterialTheme.typography.titleLarge) }
        items(vm.content.ezans) { e ->
            val f = vm.content.ezanFile(e)
            val dl = downloads.firstOrNull { it.id == "ezan:${e.id}" }
            SectionCard {
                Text(t(e.tr), style = MaterialTheme.typography.titleMedium)
                Text("${e.author} · ${e.license}", style = MaterialTheme.typography.bodySmall)
                Text(e.changes, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        e.bundled -> AssistChip({}, { Text(t("Uygulamada hazır (internetsiz)")) })
                        f.exists() -> AssistChip({}, { Text(t("İndirildi (internetsiz)")) })
                        !e.available -> AssistChip({}, { Text(t("Sunucuda henüz yok")) })
                        dl != null && dl.status != "FAILED" -> AssistChip({}, { Text("${t("İndiriliyor")} ${if (dl.total > 0) dl.done * 100 / dl.total else 0}%") })
                        else -> FilledTonalButton({ vm.downloadEzan(e.id) }) { Icon(Icons.Filled.Download, null); Text(t("İndir")) }
                    }
                    if (e.bundled || f.exists()) TextButton({ AdhanService.preview(ctx, e.id, null) }) { Icon(Icons.Filled.PlayArrow, null); Text(t("Dinle")) }
                }
                if (dl?.status == "FAILED") Text("${t("İndirme hatası")}: ${dl.error}", color = MaterialTheme.colorScheme.error)
            }
        }
        item { SectionCard(t("Test")) {
            Text(t("Planlanan alarmlar"), fontWeight = FontWeight.SemiBold)
            Text(AlarmScheduler.scheduledDebug(ctx).lines().take(8).joinToString("\n").ifEmpty { "-" }, style = MaterialTheme.typography.bodySmall)
        } }
    }
}

// ---------------------------------------------------------------- PİL REHBERİ
@SuppressLint("BatteryLife")
@Composable fun BatteryScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val pm = ctx.getSystemService(PowerManager::class.java)
    val ignoring = pm.isIgnoringBatteryOptimizations(ctx.packageName)
    Column(Modifier.fillMaxSize()) {
        Bar(t("Pil ve alarm rehberi"), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(t("Pil optimizasyonu")) {
                KeyValue(t("Pil kısıtlaması kapalı mı?"), if (ignoring) "✓" else "✗", true)
                Text(t("Ezanın her zaman çalması için uygulamayı pil optimizasyonundan çıkarmanız önerilir. Uygulama arka planda sürekli çalışmaz; yalnızca vakit girdiğinde sistem alarmıyla uyanır."))
                if (!ignoring) Button({ runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) } }) { Text(t("Kısıtlamayı kaldır")) }
                OutlinedButton({ ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }) { Text(t("Uygulama ayarlarını aç")) }
            }
            SectionCard(t("Üretici kısıtlamaları")) {
                Text(t("• Xiaomi / Redmi / POCO (MIUI, HyperOS): Ayarlar > Uygulamalar > Namaz Vakti > Otomatik başlatma: AÇIK; Pil tasarrufu: Kısıtlama yok.\n• Huawei / Honor: Ayarlar > Pil > Uygulama başlatma > Namaz Vakti: Manuel yönet, üç seçeneği de açın.\n• Samsung: Ayarlar > Pil > Arka plan kullanım sınırları > 'Hiç uyumayan uygulamalar' listesine ekleyin.\n• Oppo / Realme / OnePlus (ColorOS): Ayarlar > Pil > Namaz Vakti > Arka plan etkinliğine izin ver; Otomatik başlatma: AÇIK.\n• Vivo: Ayarlar > Pil > Arka planda yüksek güç tüketimi: izin ver.\nBu ayarlar yapılmazsa bazı telefonlarda ezan gecikebilir veya hiç çalmayabilir."))
                Text(t("Daha fazla bilgi: dontkillmyapp.com"), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Ses")) { Text(t("Ezan, telefonun ALARM ses seviyesini kullanır. Sessiz modda da çalması için alarm sesini açık tutun. Bildirimdeki 'Durdur' düğmesiyle ezanı istediğiniz an kesebilirsiniz.")) }
        }
    }
}
