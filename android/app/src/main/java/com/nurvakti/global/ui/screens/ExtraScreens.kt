package com.nurvakti.global.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nurvakti.global.data.CustomZikir
import com.nurvakti.global.data.FastLog
import com.nurvakti.global.data.Kaza
import com.nurvakti.global.data.PrayerLog
import com.nurvakti.global.data.ZikirSession
import com.nurvakti.global.download.Downloads
import com.nurvakti.global.prayer.Hijri
import com.nurvakti.global.prayer.PrayerCalc
import com.nurvakti.global.prayer.Times
import com.nurvakti.global.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------- İNDİRME MERKEZİ
@Composable fun DownloadsScreen(vm: AppVm, back: () -> Unit) {
    val ctx = LocalContext.current
    val list by vm.dao.downloads().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var used by remember { mutableLongStateOf(0) }
    LaunchedEffect(list.count { it.status == Downloads.DONE }) { used = withContext(Dispatchers.IO) { Downloads.usedBytes(ctx) } }
    Column(Modifier.fillMaxSize()) {
        Bar(t("İndirme merkezi"), back)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionCard(t("Depolama")) {
                KeyValue(t("İndirilenlerin boyutu"), fmtBytes(used), true); KeyValue(t("Telefonda boş alan"), fmtBytes(Downloads.freeBytes(ctx)))
                Text(t("Sure indirmek için Kur'an > Hafızlar > Sureler; ezan indirmek için Ezan ayarları bölümünü kullanın. İndirilen dosyalar internet yokken çalınır."), style = MaterialTheme.typography.bodySmall)
            } }
            if (list.isEmpty()) item { Text(t("Henüz indirme yok.")) }
            items(list, key = { it.id }) { d ->
                SectionCard {
                    Text(d.title, style = MaterialTheme.typography.titleMedium)
                    val pct = if (d.total > 0) d.done.toFloat() / d.total else 0f
                    if (d.status != Downloads.DONE) LinearProgressIndicator({ pct }, Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    Text("${t(statusTr(d.status))} · ${fmtBytes(d.done)}${if (d.total > 0) " / " + fmtBytes(d.total) else ""}", style = MaterialTheme.typography.bodySmall)
                    d.error?.let { Text("${t("Hata")}: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (d.status) {
                            Downloads.RUNNING, Downloads.QUEUED -> { TextButton({ scope.launch { Downloads.pause(ctx, d.id) } }) { Text(t("Duraklat")) }
                                TextButton({ scope.launch { Downloads.cancel(ctx, d.id) } }) { Text(t("İptal")) } }
                            Downloads.PAUSED, Downloads.FAILED -> { TextButton({ scope.launch { Downloads.resume(ctx, d.id) } }) { Text(t("Devam et")) }
                                TextButton({ scope.launch { Downloads.cancel(ctx, d.id) } }) { Text(t("İptal")) } }
                            else -> TextButton({ scope.launch { Downloads.delete(ctx, d.id) } }) { Icon(Icons.Filled.Delete, null); Text(t("Dosyayı sil")) }
                        }
                    }
                }
            }
        }
    }
}
private fun statusTr(s: String) = mapOf(Downloads.QUEUED to "Sırada", Downloads.RUNNING to "İndiriliyor", Downloads.PAUSED to "Duraklatıldı", Downloads.DONE to "İndirildi", Downloads.FAILED to "Başarısız")[s] ?: s

// ---------------------------------------------------------------- DUALAR
@Composable fun DualarScreen(vm: AppVm, back: () -> Unit) {
    val lg = lang()
    val favs by vm.favorites.collectAsState()
    var cat by remember { mutableStateOf("fav_all") }
    val data = vm.content.duas
    val items = data.items.filter { if (cat == "fav_all") true else if (cat == "fav") "dua:${it.id}" in favs else it.cat == cat }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Dualar"), back)
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(cat == "fav_all", { cat = "fav_all" }, { Text(t("Tümü")) }) }
            item { FilterChip(cat == "fav", { cat = "fav" }, { Text("★ " + t("Favoriler")) }) }
            items(data.cats) { c -> FilterChip(cat == c.id, { cat = c.id }, { Text(if (lg == "tr") c.tr else c.en) }) }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { d -> SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton({ vm.toggleFav("dua", d.id.toString()) }) { Icon(if ("dua:${d.id}" in favs) Icons.Filled.Star else Icons.Filled.StarBorder, t("Favori")) }
                }
                ArabicText(d.ar, 24)
                Text(d.okunus, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp)); Text(d.tr)
                Spacer(Modifier.height(4.dp)); Text(d.en, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SourceLine(d.src)
            } }
        }
    }
}

// ---------------------------------------------------------------- ZİKİRMATİK
fun vibrate(ctx: Context, ms: Long) {
    val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
    runCatching { v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

@Composable fun ZikirScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val custom by vm.dao.customZikir().collectAsState(emptyList())
    val history by vm.dao.zikirHistory().collectAsState(emptyList())
    val total by vm.dao.zikirTotal().collectAsState(0L)
    val today by vm.dao.zikirToday(vm.today()).collectAsState(0L)
    val names = vm.content.zikir.map { it.okunus } + custom.map { it.name }
    var name by remember { mutableStateOf(names.first()) }
    var target by remember { mutableIntStateOf(33) }
    var count by remember { mutableIntStateOf(0) }
    var vib by remember { mutableStateOf(true) }
    var addOpen by remember { mutableStateOf(false) }
    fun save() { if (count > 0) { val c = count; scope.launch { vm.dao.addZikir(ZikirSession(0, name, c, vm.today(), System.currentTimeMillis())) }; count = 0 } }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Zikirmatik"), { save(); back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Choice(t("Zikir"), names.map { it to it }, name) { save(); name = it; vm.content.zikir.firstOrNull { z -> z.okunus == it }?.let { z -> target = z.count } ?: custom.firstOrNull { c -> c.name == it }?.let { c -> target = c.target } }
            vm.content.zikir.firstOrNull { it.okunus == name }?.let { z -> ArabicText(z.ar, 26); Text(z.tr, textAlign = TextAlign.Center); SourceLine(z.src) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(33, 99, 100, 1000, 0)) { v -> FilterChip(target == v, { target = v }, { Text(if (v == 0) t("Sınırsız") else "$v") }) }
            }
            Box(Modifier.size(260.dp).clip(CircleShape).background(heroBrush).clickable {
                count++
                if (vib) vibrate(ctx, if (target > 0 && count % target == 0) 400 else 25)
                if (target > 0 && count == target) vm.toast("Hedefe ulaşıldı: $target")
            }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", fontSize = 72.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (target > 0) "/ $target" else "∞", color = Brand.GoldLight, fontSize = 20.sp)
                    if (target > 0 && count >= target) Text("${t("Tur")}: ${count / target}", color = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ count = 0 }) { Icon(Icons.Filled.Refresh, null); Text(t("Sıfırla")) }
                Button({ save(); vm.toast("Kaydedildi") }) { Icon(Icons.Filled.Save, null); Text(t("Kaydet")) }
                OutlinedButton({ addOpen = true }) { Icon(Icons.Filled.Add, null); Text(t("Zikir ekle")) }
            }
            SwitchRow(t("Titreşim"), vib) { vib = it }
            SectionCard(t("İstatistik")) {
                KeyValue(t("Bugün"), "$today"); KeyValue(t("Toplam zikir"), "$total", true)
                Text("${t("Günlük hedef")}: ${s.zikirGoal}")
                LinearProgressIndicator({ (today.toFloat() / s.zikirGoal.coerceAtLeast(1)).coerceAtMost(1f) }, Modifier.fillMaxWidth())
                Slider(s.zikirGoal.toFloat(), { v -> vm.update { it.copy(zikirGoal = v.toInt()) } }, valueRange = 33f..5000f)
            }
            if (custom.isNotEmpty()) SectionCard(t("Kişisel zikirlerim")) { custom.forEach { c -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${c.name} (${c.target})", Modifier.weight(1f)); IconButton({ scope.launch { vm.dao.delCustomZikir(c.id) } }) { Icon(Icons.Filled.Delete, t("Sil")) } } } }
            SectionCard(t("Geçmiş kayıtlar")) {
                if (history.isEmpty()) Text(t("Henüz kayıt yok"))
                history.take(30).forEach { h -> KeyValue("${h.date} · ${h.name}", "${h.count}") }
            }
        }
    }
    if (addOpen) {
        var n by remember { mutableStateOf("") }; var tg by remember { mutableStateOf("100") }
        AlertDialog({ addOpen = false }, title = { Text(t("Kişisel zikir ekle")) }, text = { Column {
            OutlinedTextField(n, { n = it }, label = { Text(t("Zikir adı")) }, singleLine = true)
            OutlinedTextField(tg, { tg = it }, label = { Text(t("Hedef sayı")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } },
            confirmButton = { TextButton({ if (n.isNotBlank()) scope.launch { vm.dao.addCustomZikir(CustomZikir(0, n.trim(), tg.toIntOrNull() ?: 100)) }; addOpen = false }) { Text(t("Ekle")) } },
            dismissButton = { TextButton({ addOpen = false }) { Text(t("Vazgeç")) } })
    }
}

// ---------------------------------------------------------------- ESMA
@Composable fun EsmaScreen(vm: AppVm, back: () -> Unit) {
    val lg = lang(); val favs by vm.favorites.collectAsState()
    var onlyFav by remember { mutableStateOf(false) }
    val today = vm.content.esmaOfDay()
    Column(Modifier.fillMaxSize()) {
        Bar(t("Esmâü'l-Hüsnâ"), back) { IconButton({ onlyFav = !onlyFav }) { Icon(if (onlyFav) Icons.Filled.Star else Icons.Filled.StarBorder, t("Favoriler")) } }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionCard(t("Günün esması")) { ArabicText(today.ar, 34); Text("${today.n}. ${today.okunus}", style = MaterialTheme.typography.titleMedium); Text(if (lg == "tr") today.tr else today.en) } }
            item { Text(t("\"Allah\" lafza-i celâli bütün bu güzel isimleri kendinde toplayan özel isimdir. Liste: Tirmizî, Deavât 82 rivayeti."), style = MaterialTheme.typography.bodySmall) }
            items(vm.content.esma.filter { !onlyFav || "esma:${it.n}" in favs }) { e -> SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${e.n}", Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) { Text(e.okunus, style = MaterialTheme.typography.titleMedium); Text(if (lg == "tr") e.tr else e.en, style = MaterialTheme.typography.bodyMedium)
                        if (lg == "tr") Text(e.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(e.ar, fontSize = 24.sp)
                    IconButton({ vm.toggleFav("esma", e.n.toString()) }) { Icon(if ("esma:${e.n}" in favs) Icons.Filled.Star else Icons.Filled.StarBorder, t("Favori")) }
                }
            } }
        }
    }
}

// ---------------------------------------------------------------- RAMAZAN
@Composable fun RamazanScreen(vm: AppVm, back: () -> Unit, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val now by vm.now.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val fasts by vm.dao.fasts().collectAsState(emptyList())
    val todayD = now.toLocalDate()
    val h = Hijri.of(todayD, s.hijriAdjust)
    val inRamadan = h.month == 9
    val start = remember(todayD, s.hijriAdjust) { if (inRamadan) todayD.minusDays((h.day - 1).toLong()) else Hijri.nextRamadanStart(todayD, s.hijriAdjust) }
    val (eid, eidName) = remember(todayD, s.hijriAdjust) { Hijri.nextEid(todayD, s.hijriAdjust) }
    val imsakiye = remember(start, s) { vm.days(start, 30) }
    val td = remember(todayD, s) { Times.day(ctx, s, todayD) }
    fun cd(z: ZonedDateTime): String { val d = Duration.between(now, z); if (d.isNegative) return "—"; val x = d.seconds; return if (x >= 86400) "${x / 86400} ${"gün"} ${x / 3600 % 24} sa" else "%02d:%02d:%02d".format(x / 3600, x / 60 % 60, x % 60) }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Ramazan"), back)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(heroBrush).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (inRamadan) {
                    Text("${t("Ramazan'ın")} ${h.day}. ${t("günü")}", color = Brand.GoldLight, style = MaterialTheme.typography.titleMedium)
                    val iftar = td.at("maghrib"); val sahur = td.at("imsak")
                    val target = if (now.isBefore(sahur)) sahur else iftar
                    Text(if (target == sahur) t("Sahura (imsaka) kalan") else t("İftara kalan"), color = Color.White)
                    Text(cd(if (now.isAfter(iftar)) Times.day(ctx, s, todayD.plusDays(1)).at("imsak") else target), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Text("${t("İmsak")} ${td.hhmm("imsak")} · ${t("İftar")} ${td.hhmm("maghrib")}", color = Color.White)
                } else {
                    Text(t("Ramazan'a kalan"), color = Color.White)
                    Text("${Duration.between(todayD.atStartOfDay(), start.atStartOfDay()).toDays()} ${t("gün")}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Text(start.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale(s.lang))), color = Brand.GoldLight)
                }
                Spacer(Modifier.height(8.dp))
                Text("${t(eidName)}: ${eid.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale(s.lang)))} · ${Duration.between(todayD.atStartOfDay(), eid.atStartOfDay()).toDays()} ${t("gün")}", color = Color.White)
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ go("dualar") }, Modifier.weight(1f)) { Text(t("Ramazan duaları")) }
                OutlinedButton({ go("takvim") }, Modifier.weight(1f)) { Text(t("Kadir Gecesi")) } } }
            item { SectionCard(t("Oruç takibi")) {
                val st = fasts.firstOrNull { it.date == todayD.toString() }?.status
                Text("${t("Bugün")}: ${todayD}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(st == "FASTED", { scope.launch { vm.dao.setFast(FastLog(todayD.toString(), "FASTED")) } }, { Text(t("Tuttum")) })
                    FilterChip(st == "MISSED", { scope.launch { vm.dao.setFast(FastLog(todayD.toString(), "MISSED")) } }, { Text(t("Tutamadım (kaza)")) })
                    if (st != null) TextButton({ scope.launch { vm.dao.delFast(todayD.toString()) } }) { Text(t("Temizle")) }
                }
                KeyValue(t("Tutulan oruç"), "${fasts.count { it.status == "FASTED" }}", true)
                KeyValue(t("Kaza orucu borcu"), "${fasts.count { it.status == "MISSED" }}", true)
            } }
            item { Text("${t("İmsakiye")} · ${s.place.name}", style = MaterialTheme.typography.titleLarge) }
            item { Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp)) {
                listOf("Gün", "Tarih", "İmsak", "Güneş", "Öğle", "İkindi", "İftar", "Yatsı").forEach { Text(t(it), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
            items(imsakiye.size) { i -> val d = imsakiye[i]
                Row(Modifier.fillMaxWidth().background(if (d.date == todayD) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("${i + 1}", Modifier.weight(1f), fontSize = 12.sp); Text(d.date.format(DateTimeFormatter.ofPattern("dd.MM")), Modifier.weight(1f), fontSize = 12.sp)
                    listOf("imsak", "sunrise", "dhuhr", "asr", "maghrib", "isha").forEach { k -> Text(d.hhmm(k), Modifier.weight(1f), fontSize = 12.sp) }
                } }
            item { Text(t("İmsakiye, Hicri takvime göre hesaplanan Ramazan başlangıcından itibaren 30 gün gösterir; resmî ilanla 1 gün fark olabilir."), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ---------------------------------------------------------------- DİNİ TAKVİM
@Composable fun TakvimScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val lg = lang()
    val official = remember { runCatching {
        val o = Json.parseToJsonElement(vm.ctx.assets.open("content/dini_gunler_tr.json").bufferedReader().readText()).jsonObject
        o["days"]!!.jsonArray.map { val a = it.jsonArray; Triple(LocalDate.parse(a[0].jsonPrimitive.content), a[2].jsonPrimitive.content, a[3].jsonPrimitive.content == "true") }
    }.getOrDefault(emptyList()) }
    val useOfficial = s.place.country == "TR" && official.any { it.first.year == year }
    val days = remember(year, s.hijriAdjust, useOfficial) {
        if (useOfficial) official.filter { it.first.year == year }.map { Triple(it.first, it.second, it.third) }
        else Hijri.religiousDays(year, s.hijriAdjust).map { Triple(it.date, if (lg == "en") it.en else if (lg == "ar") it.ar else it.tr, it.night) }
    }
    val fridays = remember(year) { generateSequence(LocalDate.of(year, 1, 1).with(java.time.temporal.TemporalAdjusters.firstInMonth(java.time.DayOfWeek.FRIDAY))) { it.plusWeeks(1) }.takeWhile { it.year == year }.toList() }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Dini takvim"), back)
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ year-- }) { Icon(Icons.Filled.ChevronLeft, null) }
            Text("$year", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            IconButton({ year++ }) { Icon(Icons.Filled.ChevronRight, null) }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(if (useOfficial) t("Kaynak: Diyanet İşleri Başkanlığı dini günler takvimi") else t("Ümmü'l-Kurâ Hicri takvimine göre hesaplanmıştır. Ülkenizin resmî ilanıyla 1-2 gün fark olabilir (Ayarlar > Hicri düzeltme)."),
                style = MaterialTheme.typography.bodySmall) }
            items(days) { (d, name, night) -> SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t(name), style = MaterialTheme.typography.titleMedium)
                        Text(d.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale(lg))) + if (useOfficial) "" else " · " + Hijri.of(if (night) d.plusDays(1) else d, s.hijriAdjust).format(lg), style = MaterialTheme.typography.bodySmall)
                        if (night) Text(t("Bu gecenin (akşamın) ihyası"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    val left = Duration.between(LocalDate.now().atStartOfDay(), d.atStartOfDay()).toDays()
                    if (left >= 0) Text("$left ${t("gün")}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            } }
            item { SectionCard(t("Cuma günleri") + " ($year)") {
                Text(fridays.joinToString("  ") { it.format(DateTimeFormatter.ofPattern("dd.MM")) }, style = MaterialTheme.typography.bodyMedium) } }
        }
    }
}

// ---------------------------------------------------------------- NAMAZ TAKİBİ
@Composable fun TrackerScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val scope = rememberCoroutineScope()
    var day by remember { mutableStateOf(LocalDate.now()) }
    val yearStart = LocalDate.now().withDayOfYear(1)
    val logs by vm.dao.logs(yearStart.minusYears(1).toString(), LocalDate.now().plusDays(1).toString()).collectAsState(emptyList())
    val kaza by vm.dao.kaza().collectAsState(emptyList())
    fun done(d: LocalDate, p: String) = logs.any { it.date == d.toString() && it.prayer == p && it.done }
    fun stat(from: LocalDate): Pair<Int, Int> { val days = (Duration.between(from.atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay()).toDays()).toInt()
        val c = logs.count { it.done && it.date >= from.toString() && it.date <= LocalDate.now().toString() }; return c to days * 5 }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Namaz takibi"), back)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ day = day.minusDays(1) }) { Icon(Icons.Filled.ChevronLeft, null) }
                    Text(day.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale(s.lang))), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton({ if (day < LocalDate.now()) day = day.plusDays(1) }) { Icon(Icons.Filled.ChevronRight, null) }
                }
                PrayerCalc.FIVE.forEach { p -> val d = done(day, p)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { scope.launch { vm.dao.setLog(PrayerLog(day.toString(), p, !d)) } }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (d) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (d) Brand.Emerald else MaterialTheme.colorScheme.outline, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp)); Text(prayerName(p), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(t(if (d) "Kılındı" else "Kılınmadı"), color = if (d) Brand.Emerald else MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                val n = PrayerCalc.FIVE.count { done(day, it) }
                Text("${t("Günlük hedef")}: $n / ${s.prayerGoal}", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator({ n / s.prayerGoal.coerceAtLeast(1).toFloat() }, Modifier.fillMaxWidth())
            } }
            item { SectionCard(t("İstatistik")) {
                listOf("Bu hafta" to LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong()), "Bu ay" to LocalDate.now().withDayOfMonth(1), "Bu yıl" to yearStart).forEach { (l, f) ->
                    val (c, tot) = stat(f); KeyValue(t(l), "$c / $tot  (%${if (tot > 0) c * 100 / tot else 0})") }
                Text(t("Son 7 gün"), fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (6 downTo 0).forEach { i -> val d = LocalDate.now().minusDays(i.toLong()); val c = PrayerCalc.FIVE.count { done(d, it) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(26.dp).height((8 + c * 14).dp).clip(RoundedCornerShape(6.dp)).background(if (c == 5) Brand.Emerald else Brand.Gold))
                            Text(d.format(DateTimeFormatter.ofPattern("dd")), fontSize = 11.sp) } }
                }
            } }
            item { SectionCard(t("Kaza namazı sayacı")) {
                PrayerCalc.FIVE.plus("vitr").forEach { p -> val c = kaza.firstOrNull { it.prayer == p }?.count ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (p == "vitr") t("Vitir") else prayerName(p), Modifier.weight(1f))
                        IconButton({ scope.launch { vm.dao.setKaza(Kaza(p, (c - 1).coerceAtLeast(0))) } }) { Icon(Icons.Filled.Remove, t("Kıldım (azalt)")) }
                        Text("$c", Modifier.width(48.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        IconButton({ scope.launch { vm.dao.setKaza(Kaza(p, c + 1)) } }) { Icon(Icons.Filled.Add, t("Borç ekle")) }
                    } }
                Text(t("Kaza borcunuzu (+) ile ekleyin, kıldıkça (−) ile azaltın. Veriler yalnızca telefonunuzda saklanır."), style = MaterialTheme.typography.bodySmall)
            } }
            item { SectionCard(t("Kişisel hedef")) {
                Choice(t("Günlük namaz hedefi"), (1..5).map { it to "$it" }, s.prayerGoal) { v -> vm.update { it.copy(prayerGoal = v) } } } }
        }
    }
}

// ---------------------------------------------------------------- ZEKÂT
@Composable fun ZekatScreen(vm: AppVm, back: () -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    var gold by remember { mutableStateOf("") }; var silver by remember { mutableStateOf("") }; var cash by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }; var trade by remember { mutableStateOf("") }; var debt by remember { mutableStateOf("") }
    var gPrice by remember { mutableStateOf(if (s.manualGold > 0) s.manualGold.toString() else "") }
    var sPrice by remember { mutableStateOf(if (s.manualSilver > 0) s.manualSilver.toString() else "") }
    var cur by remember { mutableStateOf(s.currency) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun d(x: String) = x.replace(',', '.').toDoubleOrNull() ?: 0.0
    Column(Modifier.fillMaxSize()) {
        Bar(t("Zekât hesaplama"), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard(t("Güncel fiyatlar (gram)")) {
                Choice(t("Para birimi"), listOf("TRY", "USD", "EUR", "GBP", "SAR", "AED").map { it to it }, cur) { cur = it }
                Button({ loading = true; scope.launch {
                    val r = runCatching { withContext(Dispatchers.IO) { fetchMetal(cur) } }
                    r.onSuccess { (g, sv, src) -> gPrice = "%.2f".format(g).replace(',', '.'); sPrice = "%.2f".format(sv).replace(',', '.'); info = src
                        vm.update { it.copy(manualGold = g, manualSilver = sv, currency = cur) } }
                        .onFailure { info = "Fiyat alınamadı (${it.message}). Lütfen fiyatları elle girin." }
                    loading = false } }, enabled = !loading) { Text(t("Güncel fiyatı al")) }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                info?.let { Text(t(it), style = MaterialTheme.typography.bodySmall) }
                NumField(t("1 gram altın fiyatı") + " ($cur)", gPrice) { gPrice = it }
                NumField(t("1 gram gümüş fiyatı") + " ($cur)", sPrice) { sPrice = it }
            }
            SectionCard(t("Varlıklarınız")) {
                NumField(t("Altın (gram, 24 ayar karşılığı)"), gold) { gold = it }
                NumField(t("Gümüş (gram)"), silver) { silver = it }
                NumField(t("Nakit") + " ($cur)", cash) { cash = it }
                NumField(t("Banka birikimi") + " ($cur)", bank) { bank = it }
                NumField(t("Ticari mal (satış değeri)") + " ($cur)", trade) { trade = it }
                NumField(t("Borçlar") + " ($cur)", debt) { debt = it }
            }
            val gp = d(gPrice); val sp = d(sPrice)
            val total = d(gold) * gp + d(silver) * sp + d(cash) + d(bank) + d(trade) - d(debt)
            val nisab = 80.18 * gp
            SectionCard(t("Sonuç")) {
                KeyValue(t("Net zekâta tabi varlık"), "%,.2f %s".format(total, cur), true)
                KeyValue(t("Nisap (80,18 g altın)"), if (gp > 0) "%,.2f %s".format(nisab, cur) else t("altın fiyatı girin"))
                if (gp > 0) {
                    if (total >= nisab) KeyValue(t("Verilecek zekât (%2,5)"), "%,.2f %s".format(total * 0.025, cur), true)
                    else Text(t("Varlığınız nisap miktarının altında; zekât düşmez."), color = MaterialTheme.colorScheme.primary)
                }
                Text(t("Zekât, nisap miktarı mala bir kameri yıl (Hicri yıl) boyunca sahip olunması hâlinde verilir. Nisap Diyanet İşleri Başkanlığı'nın esas aldığı 80,18 gram altındır. Ayrıntılı durumlar için bir din görevlisine danışınız."), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable private fun NumField(label: String, v: String, on: (String) -> Unit) =
    OutlinedTextField(v, on, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

/** Altın/gümüş ons fiyatı (USD): api.gold-api.com (ücretsiz, anahtarsız). Kur: open.er-api.com (ücretsiz, anahtarsız). */
private fun fetchMetal(cur: String): Triple<Double, Double, String> {
    fun get(u: String): String { val c = (URL(u).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 15000; setRequestProperty("User-Agent", "NurVaktiGlobal/1.0") }
        if (c.responseCode != 200) throw Exception("HTTP ${c.responseCode}"); return c.inputStream.bufferedReader().readText() }
    val xau = Json.parseToJsonElement(get("https://api.gold-api.com/price/XAU")).jsonObject["price"]!!.jsonPrimitive.double
    val xag = Json.parseToJsonElement(get("https://api.gold-api.com/price/XAG")).jsonObject["price"]!!.jsonPrimitive.double
    val rate = if (cur == "USD") 1.0 else Json.parseToJsonElement(get("https://open.er-api.com/v6/latest/USD")).jsonObject["rates"]!!.jsonObject[cur]!!.jsonPrimitive.double
    val oz = 31.1034768
    return Triple(xau / oz * rate, xag / oz * rate, "Kaynak: gold-api.com (ons fiyatı) ve open.er-api.com (döviz kuru) · ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
}

// ---------------------------------------------------------------- NAMAZ REHBERİ
@Composable fun RehberScreen(vm: AppVm, back: () -> Unit) {
    val lg = lang(); var open by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Namaz rehberi"), back)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(t("Hanefi mezhebi esas alınmış, Şafii mezhebindeki farklar ayrıca belirtilmiştir. Kaynak: Diyanet İşleri Başkanlığı, İlmihal."), style = MaterialTheme.typography.bodySmall) }
            items(vm.content.rehber) { r -> SectionCard(if (lg == "tr") r.tr else r.en, onClick = { open = if (open == r.id) null else r.id }) {
                r.rekat?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                if (open == r.id) {
                    if (lg != "tr") Text(r.en_summary)
                    r.steps.forEachIndexed { i, st -> Text("${i + 1}. $st", Modifier.padding(vertical = 3.dp)) }
                    r.farz?.let { Spacer(Modifier.height(6.dp)); Text(it, style = MaterialTheme.typography.bodyMedium) }
                    r.safii?.let { Spacer(Modifier.height(6.dp)); Text("${t("Şafii")}: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                } else Text(t("Ayrıntılar için dokunun"), style = MaterialTheme.typography.bodySmall)
            } }
        }
    }
}

// ---------------------------------------------------------------- HAKKINDA
@Composable fun AboutScreen(vm: AppVm, back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Bar(t("Hakkında ve lisanslar"), back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionCard("NAMAZ VAKTİ: EZAN VE KIBLE") {
                Text("Namaz Vakti: Ezan ve Kıble · ${com.nurvakti.global.BuildConfig.VERSION_NAME}"); Text(t("Her vakitte, her yerde, huzurla."))
                Text(t("Reklamsız, hesap gerektirmeyen, verileri yalnızca telefonda saklayan namaz vakti uygulaması."), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Veri kaynakları")) {
                Text("• " + t("Namaz vakitleri: cihaz üzerinde astronomik hesaplama (16 yöntem); isteğe bağlı AlAdhan API (api.aladhan.com)."))
                Text("• " + t("Kur'an metni: Kral Fahd Kur'an Basım Kompleksi Uthmani metni, quran-json 3.1.2 paketi (CC BY 4.0)."))
                Text("• " + t("Türkçe meal: Diyanet İşleri Başkanlığı (tanzil.net); İngilizce: Saheeh International; Latin okunuş: tanzil.net."))
                Text("• " + t("Kur'an sesleri: AlQuran Cloud CDN (cdn.islamic.network) ve mp3quran.net — kayıtların telif hakları sahiplerine aittir; ücretsiz dağıtım için yayımlanmıştır."))
                Text("• " + t("Konum listesi: countries-states-cities-database (ODbL 1.0). Harita ve camiler: © OpenStreetMap katkıcıları (ODbL), Overpass API, Nominatim."))
                Text("• " + t("Altın/gümüş: gold-api.com; döviz: open.er-api.com."))
            }
            SectionCard(t("Ezan kayıtları ve lisansları")) {
                vm.content.ezans.forEach { e -> Text("• ${t(e.tr)} — ${e.author}, ${e.license}\n  ${e.source}", style = MaterialTheme.typography.bodySmall) }
            }
            SectionCard(t("Dini içerik")) {
                Text(t("Hadis ve dualar Kütüb-i Sitte'deki (Buhârî, Müslim, Ebû Dâvûd, Tirmizî, Nesâî, İbn Mâce) rivayetlerden, kaynak numarasıyla birlikte verilmiştir. Kur'an'dan alınan dualar doğrudan Kur'an metninden gösterilir. Esmâü'l-Hüsnâ: Tirmizî, Deavât 82."), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(t("Açık kaynak yazılım")) { Text("Jetpack Compose, Media3 ExoPlayer, Room, WorkManager, DataStore, Kotlinx Serialization (Apache 2.0) · Leaflet (BSD-2)", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
