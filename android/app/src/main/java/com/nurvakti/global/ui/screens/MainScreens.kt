package com.nurvakti.global.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nurvakti.global.R
import com.nurvakti.global.alarm.AlarmScheduler
import com.nurvakti.global.data.AlertMode
import com.nurvakti.global.prayer.DayTimes
import com.nurvakti.global.prayer.Hijri
import com.nurvakti.global.prayer.Times
import com.nurvakti.global.ui.*
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun locale(lang: String): Locale = when (lang) { "en" -> Locale.ENGLISH; "ar" -> Locale("ar"); else -> Locale("tr", "TR") }

@Composable fun modeIcon(m: AlertMode): ImageVector = when (m) {
    AlertMode.FULL -> Icons.Filled.VolumeUp; AlertMode.SHORT -> Icons.Filled.Campaign; AlertMode.NOTIFY -> Icons.Filled.NotificationsActive
    AlertMode.VIBRATE -> Icons.Filled.Vibration; AlertMode.SILENT -> Icons.Filled.NotificationsNone; AlertMode.OFF -> Icons.Filled.NotificationsOff
}
val MODE_TR = mapOf(AlertMode.FULL to "Tam ezan", AlertMode.SHORT to "Kısa ezan", AlertMode.NOTIFY to "Normal bildirim",
    AlertMode.VIBRATE to "Titreşim", AlertMode.SILENT to "Sessiz", AlertMode.OFF to "Kapalı")

// ---------------------------------------------------------------- ONBOARDING
@Composable fun Onboarding(vm: AppVm) {
    val ctx = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) vm.useGps { step = 2 } else step = 1.also { vm.toast("Konum izni verilmedi. Şehri elle seçebilirsiniz.") }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step = 3 }
    Column(Modifier.fillMaxSize().background(heroBrush).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Image(painterResource(R.drawable.ic_logo), null, Modifier.size(110.dp))
        Text("NAMAZ VAKTİ", color = Brand.GoldLight, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(t("Her vakitte, her yerde, huzurla."), color = Color.White.copy(alpha = .9f))
        Spacer(Modifier.height(28.dp))
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (step) {
                    0 -> {
                        Text(t("Dil / Language / اللغة"), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("tr" to "Türkçe", "en" to "English", "ar" to "العربية").forEach { (c, n) -> FilterChip(selected = vm.settings.value?.lang == c, onClick = { vm.update { it.copy(lang = c) } }, label = { Text(n) }) }
                        }
                        Text(t("Konum izni"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Namaz vakitlerini ve kıble yönünü bulunduğunuz yere göre hesaplamak için konumunuz kullanılır. Konumunuz yalnızca telefonunuzda işlenir; hiçbir sunucuya gönderilmez."))
                        Button({ locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Konumumu kullan")) }
                        OutlinedButton({ step = 1 }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Şehri elle seçeceğim")) }
                    }
                    1 -> {
                        Text(t("Şehir seçimi"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Konumu daha sonra Ayarlar > Konum bölümünden değiştirebilirsiniz. Şimdilik İstanbul seçili."))
                        Button({ step = 2 }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Devam")) }
                    }
                    2 -> {
                        Text(t("Ezan bildirimleri"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Vakit girdiğinde ezan çalabilmek ve bildirim gösterebilmek için bildirim izni gerekir."))
                        Button({ if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else step = 3 }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Bildirimlere izin ver")) }
                    }
                    else -> {
                        Text(t("Tam zamanında ezan"), style = MaterialTheme.typography.titleMedium)
                        Text(t("Android 12 ve sonrasında ezanın dakikası dakikasına çalması için 'Alarmlar ve hatırlatıcılar' izni gerekir. Bazı üreticiler (Xiaomi, Huawei, Oppo, Samsung vb.) pil tasarrufu nedeniyle uygulamaları durdurabilir; Ayarlar > Pil rehberinde ne yapmanız gerektiği anlatılır."))
                        if (!AlarmScheduler.canExact(ctx) && Build.VERSION.SDK_INT >= 31)
                            OutlinedButton({ ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Kesin alarm iznini aç")) }
                        Button({ vm.update { it.copy(onboarded = true) } }, Modifier.fillMaxWidth().height(52.dp)) { Text(t("Başla")) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- HOME
@Composable fun HomeScreen(vm: AppVm, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val now by vm.now.collectAsState()
    val today by vm.today.collectAsState()
    val ctx = LocalContext.current
    val lg = lang()
    val (nextKey, nextAt) = remember(now.minute, today, s) { Times.next(ctx, s, now) }
    val curKey = remember(now.minute, today, s) { Times.current(ctx, s, now) }
    val left = Duration.between(now, nextAt).coerceAtLeast(Duration.ZERO)
    val hijri = remember(now.toLocalDate(), s.hijriAdjust) { Hijri.of(now.toLocalDate(), s.hijriAdjust) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(heroBrush).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_logo), null, Modifier.size(42.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NAMAZ VAKTİ · EZAN VE KIBLE", color = Brand.GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Row(Modifier.clickable { go("location") }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" ${s.place.name}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            if (s.travelMode) Text("  ✈", color = Brand.GoldLight)
                        }
                    }
                    IconButton({ go("settings") }) { Icon(Icons.Filled.Settings, t("Ayarlar"), tint = Color.White) }
                }
                Spacer(Modifier.height(4.dp))
                Text(now.toLocalDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale(lg))), color = Color.White.copy(.92f))
                Text(hijri.format(lg), color = Brand.GoldLight)
                Spacer(Modifier.height(18.dp))
                Text("${t("Sonraki vakit")}: ${prayerName(nextKey)} · ${"%02d:%02d".format(nextAt.hour, nextAt.minute)}", color = Color.White.copy(.92f), style = MaterialTheme.typography.titleMedium)
                val sec = left.seconds
                Text("%02d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60), color = Color.White,
                    style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(if (curKey != null) "${t("Şu anki vakit")}: ${prayerName(curKey)}" else t("Gece (yatsı vakti devam ediyor)"), color = Brand.GoldLight, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        item { today?.let { TodayList(it, s.alerts.mapValues { e -> e.value.mode }, curKey) { go("ezan") } } }
        item {
            Button({ go("yasin") }, Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Gold, contentColor = Brand.Navy)) {
                Icon(Icons.Filled.Headphones, null); Spacer(Modifier.width(10.dp)); Text(t("YASİN SURESİ DİNLE"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        item {
            val quick = listOf(Triple(Icons.Filled.Headphones, "Kur'an dinle", "reciters"), Triple(Icons.AutoMirrored.Filled.MenuBook, "Yasin dinle", "yasin"),
                Triple(Icons.Filled.Explore, "Kıble bul", "qibla"), Triple(Icons.Filled.TouchApp, "Zikirmatik", "zikir"),
                Triple(Icons.Filled.NightsStay, "Ramazan", "ramazan"), Triple(Icons.Filled.CheckCircle, "Namaz takibi", "tracker"))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quick.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (ic, l, r) -> Tile(ic, t(l), Modifier.weight(1f)) { go(r) } } }
                }
            }
        }
        item {
            val a = vm.content.ayetOfDay()
            SectionCard(t("Günün ayeti"), onClick = { go("reader/${a.s}?a=${a.a}") }) {
                ArabicText(a.ar, 22); Spacer(Modifier.height(6.dp)); Text(if (lg == "tr") a.tr else a.en); SourceLine("${a.name} ${a.ref}")
            }
        }
        item { val h = vm.content.hadisOfDay(); SectionCard(t("Günün hadisi")) { Text(if (lg == "tr") h.tr else h.en); SourceLine(h.src) } }
        item { val d = vm.content.duaOfDay(); SectionCard(t("Günün duası"), onClick = { go("dualar") }) {
            Text(d.title, fontWeight = FontWeight.SemiBold); ArabicText(d.ar, 20); Text(d.okunus, style = MaterialTheme.typography.bodyMedium); Text(if (lg == "tr") d.tr else d.en); SourceLine(d.src) } }
        item { val e = vm.content.esmaOfDay(); SectionCard(t("Günün esması"), onClick = { go("esma") }) {
            ArabicText(e.ar, 30); Text("${e.okunus} — ${if (lg == "tr") e.tr else e.en}"); SourceLine("Tirmizî, Deavât 82") } }
        item { val z = vm.content.zikirOfDay(); SectionCard(t("Günün zikir önerisi"), onClick = { go("zikir") }) {
            ArabicText(z.ar, 24); Text("${z.okunus} × ${z.count}"); Text(if (lg == "tr") z.tr else z.en); SourceLine(z.src) } }
        item { Text("${t("Veri kaynağı")}: ${today?.source ?: ""} · ${t(if (s.asr == "HANAFI") "Hanefi ikindi" else "Şafii/Standart ikindi")}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun TodayList(d: DayTimes, modes: Map<String, AlertMode>, cur: String?, onAlert: () -> Unit) {
    SectionCard {
        listOf("imsak", "sunrise", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
            val active = k == cur || (k == "imsak" && cur == "fajr")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (k == "imsak") "${prayerName("imsak")} / ${prayerName("fajr")}" else prayerName(k), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                val mk = if (k == "imsak") "fajr" else k
                IconButton(onAlert, Modifier.size(36.dp)) { Icon(modeIcon(modes[mk] ?: AlertMode.OFF), null, tint = MaterialTheme.colorScheme.primary) }
                Text(d.hhmm(k), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------------------------------------------------- TIMES (günlük/haftalık/aylık/yıllık)
@Composable fun TimesScreen(vm: AppVm, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    var tab by remember { mutableIntStateOf(0) }
    var month by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    val lg = lang()
    Column(Modifier.fillMaxSize()) {
        Bar(t("Namaz vakitleri"), actions = { IconButton({ go("settings") }) { Icon(Icons.Filled.Tune, t("Hesaplama ayarları")) } })
        TabRow(tab) { listOf("Günlük", "Haftalık", "Aylık", "Yıllık").forEachIndexed { i, l -> Tab(tab == i, { tab = i }, text = { Text(t(l), maxLines = 1, softWrap = false, fontSize = 13.sp) }) } }
        val days: List<DayTimes> = remember(tab, month, year, s) {
            when (tab) {
                0 -> vm.days(LocalDate.now(), 1)
                1 -> vm.days(LocalDate.now(), 7)
                2 -> vm.days(month, month.lengthOfMonth())
                else -> vm.days(LocalDate.of(year, 1, 1), LocalDate.of(year, 1, 1).lengthOfYear())
            }
        }
        if (tab == 2) Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, null) }
            Text(month.month.getDisplayName(TextStyle.FULL, locale(lg)) + " " + month.year, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            IconButton({ month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, null) }
        }
        if (tab == 3) Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ year -= 1 }) { Icon(Icons.Filled.ChevronLeft, null) }
            Text("$year", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            IconButton({ year += 1 }) { Icon(Icons.Filled.ChevronRight, null) }
        }
        if (tab == 0) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                days.firstOrNull()?.let { d -> SectionCard(d.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale(lg)))) {
                    PRAYER_KEYS.forEach { k -> KeyValue(prayerName(k), d.hhmm(k), bold = true) } } }
                SectionCard(t("Hesaplama")) {
                    KeyValue(t("Konum"), "${s.place.name} (${"%.4f".format(s.place.lat)}, ${"%.4f".format(s.place.lng)})")
                    KeyValue(t("Saat dilimi"), s.place.tz)
                    KeyValue(t("Yöntem"), com.nurvakti.global.prayer.PrayerCalc.method(Times.methodId(s)).name)
                    KeyValue(t("İkindi"), t(if (s.asr == "HANAFI") "Hanefi (gölge 2 misli)" else "Standart / Şafii (gölge 1 misli)"))
                    KeyValue(t("Kaynak"), days.firstOrNull()?.source ?: "")
                    if (Times.methodId(s) == "DIYANET") Text(t("Not: Diyanet yöntemi (18°/17° ve temkin süreleri) cihazda hesaplanır ve Diyanet'in yayımladığı resmî vakitlerle test edilmiştir (±1 dk). Resmî Diyanet API'si kullanılmamaktadır; bu API kurumdan alınan kullanıcı adı/şifre gerektirir."),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(t("Tarih"), Modifier.weight(1.4f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                PRAYER_KEYS.filter { it != "fajr" }.forEach { Text(prayerName(it), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center) }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(days) { d ->
                    val isToday = d.date == LocalDate.now()
                    Row(Modifier.fillMaxWidth().background(if (isToday) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).padding(horizontal = 8.dp, vertical = 8.dp)) {
                        Text(d.date.format(DateTimeFormatter.ofPattern("dd.MM EEE", locale(lg))), Modifier.weight(1.4f), fontSize = 12.sp)
                        PRAYER_KEYS.filter { it != "fajr" }.forEach { Text(d.hhmm(it), Modifier.weight(1f), fontSize = 13.sp, textAlign = TextAlign.Center) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(.3f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- MENU
@Composable fun MenuScreen(go: (String) -> Unit) {
    val items = listOf(
        Triple(Icons.Filled.Headphones, "Hafızlar ve dinleme", "reciters"), Triple(Icons.AutoMirrored.Filled.MenuBook, "Yasin-i Şerif", "yasin"),
        Triple(Icons.Filled.Bookmark, "Yer imleri", "bookmarks"), Triple(Icons.Filled.VolumeUp, "Ezan ayarları", "ezan"),
        Triple(Icons.Filled.Download, "İndirme merkezi", "downloads"), Triple(Icons.Filled.FavoriteBorder, "Dualar", "dualar"),
        Triple(Icons.Filled.TouchApp, "Zikirmatik", "zikir"), Triple(Icons.Filled.AutoAwesome, "Esmâü'l-Hüsnâ", "esma"),
        Triple(Icons.Filled.NightsStay, "Ramazan", "ramazan"), Triple(Icons.Filled.CalendarMonth, "Dini takvim", "takvim"),
        Triple(Icons.Filled.CheckCircle, "Namaz takibi", "tracker"), Triple(Icons.Filled.Calculate, "Zekât hesapla", "zekat"),
        Triple(Icons.Filled.Mosque, "Yakındaki camiler", "camiler"), Triple(Icons.Filled.School, "Namaz rehberi", "rehber"),
        Triple(Icons.Filled.LocationOn, "Konum", "location"), Triple(Icons.Filled.Settings, "Ayarlar", "settings"),
        Triple(Icons.Filled.BatteryAlert, "Pil ve alarm rehberi", "battery"), Triple(Icons.Filled.Info, "Hakkında ve lisanslar", "about"),
    )
    Column(Modifier.fillMaxSize()) {
        Bar(t("Menü"))
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { (ic, l, r) -> Tile(ic, t(l)) { go(r) } }
        }
    }
}
