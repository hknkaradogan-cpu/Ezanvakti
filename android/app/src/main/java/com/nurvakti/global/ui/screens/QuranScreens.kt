package com.nurvakti.global.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.nurvakti.global.data.Bookmark
import com.nurvakti.global.location.Geo
import com.nurvakti.global.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------- SURE LİSTESİ + ARAMA
@Composable fun QuranList(vm: AppVm, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Triple<Int, Int, String>>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lg = lang()
    val surahs = vm.content.surahs
    val filtered = remember(q) { if (q.isBlank()) surahs else surahs.filter { Geo.norm(it.tr).contains(Geo.norm(q)) || Geo.norm(it.tl).contains(Geo.norm(q)) || it.ar.contains(q) || it.n.toString() == q.trim() } }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Kur'an-ı Kerim"), actions = {
            IconButton({ go("bookmarks") }) { Icon(Icons.Filled.Bookmarks, t("Yer imleri")) }
            IconButton({ go("reciters") }) { Icon(Icons.Filled.Headphones, t("Hafızlar")) }
        })
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
            placeholder = { Text(t("Sure adı veya ayet içinde kelime ara")) }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (q.length >= 3) TextButton({ searching = true; scope.launch {
                val qq = Geo.norm(q)
                hits = withContext(Dispatchers.Default) {
                    val tr = vm.content.quran(if (lg == "tr") "tr" else "en"); val ar = vm.content.quran("ar")
                    val res = ArrayList<Triple<Int, Int, String>>()
                    for (si in tr.indices) for (ai in tr[si].indices) {
                        if (Geo.norm(tr[si][ai]).contains(qq) || ar[si][ai].contains(q)) { res += Triple(si + 1, ai + 1, tr[si][ai]); if (res.size >= 200) break }
                    }
                    res
                }; searching = false } }) { Text(t("Ayet ara")) } })
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                SectionCard(t("Kaldığın yerden devam"), onClick = { go("reader/${s.lastSurah}?a=${s.lastAyah}") }) {
                    Text("${surahs[s.lastSurah - 1].tr} · ${t("Ayet")} ${s.lastAyah}", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (searching) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (hits.isNotEmpty()) {
                item { Text("${hits.size} ${t("ayet bulundu")}", style = MaterialTheme.typography.titleMedium) }
                items(hits) { (si, ai, txt) -> SectionCard(onClick = { go("reader/$si?a=$ai") }) {
                    Text("${surahs[si - 1].tr} $si:$ai", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(txt, maxLines = 3) } }
                item { TextButton({ hits = emptyList() }) { Text(t("Aramayı temizle")) } }
            }
            items(filtered) { su ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable { go("reader/${su.n}") }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("${su.n}", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (lg == "tr") su.tr else su.tl, style = MaterialTheme.typography.titleMedium)
                        Text("${su.en} · ${su.ayahs} ${t("ayet")} · ${t(if (su.type == "meccan") "Mekkî" else "Medenî")}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(su.ar, fontSize = 22.sp)
                }
            }
            item { SourceLine(t("Arapça metin: Kral Fahd Kur'an Basım Kompleksi Uthmani metni (QuranEnc). Türkçe meal: Diyanet İşleri Başkanlığı (tanzil.net). İngilizce: Saheeh International.")) }
        }
    }
}

// ---------------------------------------------------------------- OKUYUCU (+ YASİN)
@Composable fun ReaderScreen(vm: AppVm, surah: Int, startAyah: Int, back: () -> Unit, go: (String) -> Unit, yasin: Boolean = false) {
    val s = vm.settings.collectAsState().value ?: return
    val su = vm.content.surahs[surah - 1]
    val ar = remember { vm.content.quran("ar")[surah - 1] }
    val tr = remember(s.translation) { if (s.translation == "none") null else vm.content.quran(s.translation)[surah - 1] }
    val tl = remember(s.showTranslit, yasin) { if (s.showTranslit || yasin) vm.content.quran("translit")[surah - 1] else null }
    val bms by vm.dao.bookmarks().collectAsState(emptyList())
    val ps by vm.player.state.collectAsState()
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (startAyah - 1).coerceAtLeast(0) + 1)
    val scope = rememberCoroutineScope()
    var reciterMenu by remember { mutableStateOf(false) }
    val reciter = vm.content.reciters.firstOrNull { it.id == s.reciter } ?: vm.content.reciters.first()
    val isThis = ps.surah == surah && ps.reciter == reciter.id
    LaunchedEffect(list.firstVisibleItemIndex) {
        val a = (list.firstVisibleItemIndex).coerceIn(1, ar.size)
        if (a != s.lastAyah || surah != s.lastSurah) vm.update { it.copy(lastSurah = surah, lastAyah = a) }
    }
    Column(Modifier.fillMaxSize()) {
        Bar(if (yasin) t("Yasin-i Şerif") else "${su.n}. ${su.tr}", back) {
            IconButton({ vm.update { it.copy(quranFont = (it.quranFont - 2).coerceAtLeast(16)) } }) { Icon(Icons.Filled.TextDecrease, t("Yazı küçült")) }
            IconButton({ vm.update { it.copy(quranFont = (it.quranFont + 2).coerceAtMost(48)) } }) { Icon(Icons.Filled.TextIncrease, t("Yazı büyüt")) }
            IconButton({ vm.update { it.copy(theme = if (it.theme == "DARK") "LIGHT" else "DARK") } }) { Icon(Icons.Filled.DarkMode, t("Gece modu")) }
        }
        // Oynatıcı şeridi
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton({ if (isThis) vm.player.toggle() else vm.player.playSurahs(reciter, if (yasin) listOf(36) else (surah..114).toList(), surah) },
                        Modifier.size(52.dp)) { Icon(if (isThis && ps.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, t("Dinle")) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).clickable { reciterMenu = true }) {
                        Text(reciter.tr, fontWeight = FontWeight.SemiBold)
                        Text(if (surah in reciter.surahs) t("Hafızı değiştirmek için dokunun") else t("Bu hafızın bu sure kaydı yok"), style = MaterialTheme.typography.bodySmall)
                        DropdownMenu(reciterMenu, { reciterMenu = false }) {
                            vm.content.reciters.forEach { r -> DropdownMenuItem(text = { Text("${r.tr} (${r.surahs.size}/114)") }, enabled = surah in r.surahs,
                                onClick = { reciterMenu = false; vm.update { it.copy(reciter = r.id) } }) }
                        }
                    }
                    IconButton({ vm.player.cycleRepeat() }) { Icon(when (ps.repeat) { Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne; Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat; else -> Icons.Filled.Repeat }, t("Tekrar"),
                        tint = if (ps.repeat == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                    IconButton({ vm.downloadSurah(reciter.id, surah); vm.toast("İndirme başladı — İndirme merkezinden takip edebilirsiniz") }) { Icon(Icons.Filled.Download, t("İndir")) }
                    IconButton({ go("player") }) { Icon(Icons.Filled.OpenInFull, t("Oynatıcı")) }
                }
                if (isThis && ps.duration > 0) {
                    Slider(ps.position.toFloat(), { vm.player.seek(it.toLong()) }, valueRange = 0f..ps.duration.toFloat())
                    Text("${fmtDuration(ps.position)} / ${fmtDuration(ps.duration)}", style = MaterialTheme.typography.bodySmall)
                }
                ps.error?.let { if (isThis) Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(su.ar, fontSize = 30.sp); Text("${su.tr} · ${su.ayahs} ${t("ayet")}", style = MaterialTheme.typography.titleMedium)
                    if (surah != 1 && surah != 9) ArabicText("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", s.quranFont, Modifier.padding(top = 8.dp))
                    if (yasin) Text(t("Okunuş: Latin harfli transliterasyon (tanzil.net). Meal: Diyanet İşleri Başkanlığı."), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            itemsIndexed(ar) { i, txt ->
                val a = i + 1
                val bm = bms.any { it.surah == surah && it.ayah == a && it.kind == "bookmark" }
                val fav = bms.any { it.surah == surah && it.ayah == a && it.kind == "fav" }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$surah:$a", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton({ scope.launch { if (bm) vm.dao.delBookmark(surah, a, "bookmark") else vm.dao.addBookmark(Bookmark(0, surah, a, "bookmark", System.currentTimeMillis())) } }) {
                            Icon(if (bm) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, t("Yer imi")) }
                        IconButton({ scope.launch { if (fav) vm.dao.delBookmark(surah, a, "fav") else vm.dao.addBookmark(Bookmark(0, surah, a, "fav", System.currentTimeMillis())) } }) {
                            Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, t("Favori")) }
                    }
                    ArabicText(txt, s.quranFont)
                    tl?.let { Spacer(Modifier.height(6.dp)); Text(it[i], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                    tr?.let { Spacer(Modifier.height(6.dp)); Text(it[i], style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- HAFIZLAR
@Composable fun RecitersScreen(vm: AppVm, back: () -> Unit, go: (String) -> Unit) {
    val s = vm.settings.collectAsState().value ?: return
    val favs by vm.favorites.collectAsState()
    var open by remember { mutableStateOf<String?>(null) }
    val ps by vm.player.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Bar(t("Kur'an hafızları"), back)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(t("Kayıtlar AlQuran Cloud CDN (cdn.islamic.network) üzerinden yayınlanır. Her hafız için yalnızca sunucuda gerçekten bulunan sureler listelenir; eksik sure varsa sayı belirtilir."), style = MaterialTheme.typography.bodySmall) }
            items(vm.content.reciters) { r ->
                val isFav = "reciter:${r.id}" in favs
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.tr, style = MaterialTheme.typography.titleMedium); Text(r.info, style = MaterialTheme.typography.bodySmall)
                            Text("${r.surahs.size}/114 ${t("sure")}" + if (!r.verified) " · ${t("sunucu doğrulaması bekliyor")}" else "", style = MaterialTheme.typography.bodySmall,
                                color = if (r.complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                        IconButton({ vm.toggleFav("reciter", r.id) }) { Icon(if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder, t("Favori hafız")) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton({ vm.update { it.copy(reciter = r.id) } }, enabled = s.reciter != r.id) { Text(if (s.reciter == r.id) t("Seçili") else t("Seç")) }
                        OutlinedButton({ vm.player.playSurahs(r, listOf(1), 1) }) { Icon(Icons.Filled.PlayArrow, null); Text(t("Örnek: Fâtiha")) }
                        TextButton({ open = if (open == r.id) null else r.id }) { Text(t("Sureler")) }
                    }
                    if (open == r.id) {
                        OutlinedButton({ vm.downloadAllSurahs(r.id) }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.Download, null); Text(t("Tüm sureleri indir")) }
                        r.surahs.forEach { n ->
                            val su = vm.content.surahs[n - 1]
                            val local = vm.content.quranFile(r.id, n).exists()
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("$n. ${su.tr}", Modifier.weight(1f))
                                if (local) Icon(Icons.Filled.OfflinePin, t("İndirildi"), tint = MaterialTheme.colorScheme.primary)
                                IconButton({ vm.player.playSurahs(r, r.surahs, n) }) { Icon(if (ps.surah == n && ps.reciter == r.id && ps.playing) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow, t("Dinle")) }
                                IconButton({ vm.downloadSurah(r.id, n) }, enabled = !local) { Icon(Icons.Filled.Download, t("İndir")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- OYNATICI
@Composable fun PlayerScreen(vm: AppVm, back: () -> Unit) {
    val ps by vm.player.state.collectAsState()
    LaunchedEffect(Unit) { vm.player.connect() }
    var titles by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(ps.count, ps.index) { titles = vm.player.titles() }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Kur'an oynatıcı"), back)
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(180.dp).clip(RoundedCornerShape(40.dp)).background(heroBrush), contentAlignment = Alignment.Center) {
                Text("﷽", fontSize = 40.sp, color = Brand.GoldLight) }
            Spacer(Modifier.height(16.dp))
            Text(ps.title.ifEmpty { t("Çalan bir kayıt yok") }, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(ps.subtitle, style = MaterialTheme.typography.bodyMedium)
            if (ps.buffering) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            ps.error?.let { Text(t(it), color = MaterialTheme.colorScheme.error) }
            Slider(ps.position.toFloat(), { vm.player.seek(it.toLong()) }, valueRange = 0f..(ps.duration.coerceAtLeast(1)).toFloat())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(fmtDuration(ps.position)); Text(fmtDuration(ps.duration)) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton({ vm.player.prev() }) { Icon(Icons.Filled.SkipPrevious, t("Önceki sure")) }
                IconButton({ vm.player.back() }) { Icon(Icons.Filled.Replay10, t("Geri")) }
                FilledIconButton({ vm.player.toggle() }, Modifier.size(72.dp)) { Icon(if (ps.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, t("Oynat/Duraklat"), Modifier.size(40.dp)) }
                IconButton({ vm.player.fwd() }) { Icon(Icons.Filled.Forward10, t("İleri")) }
                IconButton({ vm.player.next() }) { Icon(Icons.Filled.SkipNext, t("Sonraki sure")) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ vm.player.cycleRepeat() }) { Icon(if (ps.repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, t("Tekrar"),
                    tint = if (ps.repeat == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                Text(t(when (ps.repeat) { Player.REPEAT_MODE_ONE -> "Bu sureyi tekrarla"; Player.REPEAT_MODE_ALL -> "Listeyi tekrarla"; else -> "Tekrar kapalı" }))
                Spacer(Modifier.width(12.dp))
                IconButton({ vm.player.stop() }) { Icon(Icons.Filled.Stop, t("Durdur")) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VolumeDown, null); Slider(ps.volume, { vm.player.setVolume(it) }, Modifier.weight(1f)); Icon(Icons.Filled.VolumeUp, null)
            }
        }
        Text(t("Oynatma listesi"), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(titles) { i, ti ->
                Text(ti, Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (i == ps.index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { vm.player.jump(i) }.padding(12.dp), fontWeight = if (i == ps.index) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- YER İMLERİ
@Composable fun BookmarksScreen(vm: AppVm, back: () -> Unit, go: (String) -> Unit) {
    val bms by vm.dao.bookmarks().collectAsState(emptyList())
    val tr = remember { vm.content.quran("tr") }
    Column(Modifier.fillMaxSize()) {
        Bar(t("Yer imleri ve favoriler"), back)
        if (bms.isEmpty()) Text(t("Henüz yer imi yok. Okurken ayetin yanındaki işarete dokunun."), Modifier.padding(16.dp))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bms) { b -> SectionCard(onClick = { go("reader/${b.surah}?a=${b.ayah}") }) {
                Text("${if (b.kind == "fav") "♥" else "🔖"} ${vm.content.surahs[b.surah - 1].tr} ${b.surah}:${b.ayah}", fontWeight = FontWeight.Bold)
                Text(tr[b.surah - 1][b.ayah - 1], maxLines = 3) } }
        }
    }
}
