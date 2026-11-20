package com.nurvakti.global.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nurvakti.global.alarm.AlarmScheduler
import com.nurvakti.global.audio.PlayerConn
import com.nurvakti.global.data.AppDb
import com.nurvakti.global.data.Content
import com.nurvakti.global.data.Favorite
import com.nurvakti.global.data.Place
import com.nurvakti.global.data.SavedPlace
import com.nurvakti.global.data.Settings
import com.nurvakti.global.data.SettingsStore
import com.nurvakti.global.download.Downloads
import com.nurvakti.global.location.Geo
import com.nurvakti.global.prayer.DayTimes
import com.nurvakti.global.prayer.Times
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AppVm(app: Application) : AndroidViewModel(app) {
    val ctx = app
    val content = Content(app)
    val store = SettingsStore(app)
    val dao = AppDb.get(app).dao()
    val player = PlayerConn(app, content)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    val settings: StateFlow<Settings?> = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _now = MutableStateFlow(ZonedDateTime.now())
    val now: StateFlow<ZonedDateTime> = _now
    private val _today = MutableStateFlow<DayTimes?>(null)
    val today: StateFlow<DayTimes?> = _today
    val favorites = dao.favorites().map { l -> l.map { "${it.kind}:${it.key}" }.toSet() }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            while (true) {
                val s = settings.value
                val z = if (s != null) Times.nowZ(s) else ZonedDateTime.now()
                _now.value = z
                if (s != null && (_today.value?.date != z.toLocalDate() || _today.value?.zone != Times.zoneOf(s))) recompute(s)
                delay(1000)
            }
        }
        viewModelScope.launch {
            settings.distinctUntilChangedBy { it?.let { s -> listOf(s.place, s.method, s.autoMethod, s.asr, s.highLat, s.offsets, s.source, s.alerts, s.checkMinutes, s.hadisMode, s.hadisHour) } }.collect { s ->
                if (s != null) {
                    recompute(s)
                    withContext(Dispatchers.Default) { AlarmScheduler.schedule(ctx, s) }
                }
            }
        }
    }

    private suspend fun recompute(s: Settings) {
        _today.value = withContext(Dispatchers.Default) { Times.day(ctx, s, Times.nowZ(s).toLocalDate()) }
    }

    fun update(f: (Settings) -> Settings) = viewModelScope.launch { store.update(f) }
    fun toast(m: String) { messages.tryEmit(m) }

    fun days(from: LocalDate, n: Int): List<DayTimes> { val s = settings.value ?: return emptyList(); return Times.range(ctx, s, from, n) }

    fun setPlace(p: Place, gps: Boolean = false) = viewModelScope.launch {
        val tzList = content.countries.firstOrNull { it.c == p.country }?.tz ?: emptyList()
        val tz = if (gps) ZoneId.systemDefault().id else Geo.zoneFor(p.country, p.lng, tzList)
        store.update { it.copy(place = SavedPlace(p.name, p.region, p.lat, p.lng, tz, p.country, gps)) }
        toast("${p.name} · ${tz}")
    }

    fun useGps(onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val loc = Geo.current(ctx)
        if (loc == null) { toast("Konum alınamadı. GPS'in açık olduğundan emin olun."); onDone(false); return@launch }
        val (name, region, cc) = Geo.reverse(ctx, loc.latitude, loc.longitude)
        val country = cc ?: settings.value?.place?.country ?: "TR"
        store.update { it.copy(place = SavedPlace(name, region, loc.latitude, loc.longitude, ZoneId.systemDefault().id, country, true)) }
        toast("Konum: $name"); onDone(true)
    }

    fun fetchOnline() = viewModelScope.launch {
        val s = settings.value ?: return@launch
        val err = Times.fetchAlAdhan(ctx, s)
        if (err != null) toast("$err — cihaz üzerindeki hesaplama kullanılıyor.") else { toast("AlAdhan verisi alındı"); recompute(s) }
    }

    fun toggleFav(kind: String, key: String) = viewModelScope.launch {
        if ("$kind:$key" in favorites.value) dao.delFav(kind, key) else dao.addFav(Favorite(kind, key))
    }

    fun downloadSurah(reciterId: String, n: Int) = viewModelScope.launch {
        val r = content.reciters.firstOrNull { it.id == reciterId } ?: return@launch
        if (n !in r.surahs) { toast("Bu hafızın bu sure kaydı mevcut değil"); return@launch }
        val s = content.surahs[n - 1]
        Downloads.enqueue(ctx, "q:${r.id}:$n", "quran", "${s.tr} · ${r.tr}", r.surahUrl(n), content.quranFile(r.id, n).path, r.sizes[n.toString()] ?: 0)
    }
    fun downloadAllSurahs(reciterId: String) = viewModelScope.launch {
        val r = content.reciters.firstOrNull { it.id == reciterId } ?: return@launch
        r.surahs.forEach { n -> val s = content.surahs[n - 1]
            Downloads.enqueue(ctx, "q:${r.id}:$n", "quran", "${s.tr} · ${r.tr}", r.surahUrl(n), content.quranFile(r.id, n).path, r.sizes[n.toString()] ?: 0) }
        toast("${r.surahs.size} sure indirme kuyruğuna eklendi")
    }
    fun downloadEzan(id: String) = viewModelScope.launch {
        val e = content.ezans.firstOrNull { it.id == id } ?: return@launch
        // Ezan dosyaları depo içindeki gh-pages dalından (GitHub) indirilir; GitHub Pages kapalı olsa da çalışır
        Downloads.enqueue(ctx, "ezan:$id", "ezan", e.tr, "https://raw.githubusercontent.com/hknkaradogan-cpu/Ezanvakti/gh-pages/audio/ezan/" + e.file, content.ezanFile(e).path, e.size)
    }

    fun today(): String = now.value.toLocalDate().toString()

    // ---------- uygulama içi güncelleme
    val update = MutableStateFlow<com.nurvakti.global.update.UpdateInfo?>(null)
    val updateProgress = MutableStateFlow(-1)   // -1: inmiyor
    fun checkUpdate(manual: Boolean = false) = viewModelScope.launch {
        val u = com.nurvakti.global.update.Updater.check()
        update.value = u
        if (manual && u == null) toast("Uygulama güncel")
    }
    fun startUpdate() = viewModelScope.launch {
        updateProgress.value = 0
        runCatching { com.nurvakti.global.update.Updater.download(ctx) { updateProgress.value = it } }
            .onSuccess { updateProgress.value = -1; update.value = null; runCatching { com.nurvakti.global.update.Updater.install(ctx, it) }.onFailure { com.nurvakti.global.update.Updater.openInBrowser(ctx) } }
            .onFailure { updateProgress.value = -1; toast("Güncelleme indirilemedi: ${it.message}"); com.nurvakti.global.update.Updater.openInBrowser(ctx) }
    }
}

