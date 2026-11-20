package com.nurvakti.global.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nurvakti.global.data.Content
import com.nurvakti.global.data.Reciter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val connected: Boolean = false, val playing: Boolean = false, val buffering: Boolean = false,
    val title: String = "", val subtitle: String = "", val index: Int = 0, val count: Int = 0,
    val position: Long = 0, val duration: Long = 0, val repeat: Int = Player.REPEAT_MODE_OFF,
    val surah: Int = 0, val reciter: String = "", val error: String? = null, val volume: Float = 1f,
)

/** Arayüz ile PlaybackService arasındaki bağlantı (MediaController). */
class PlayerConn(private val ctx: Context, private val content: Content) {
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state
    private val scope = CoroutineScope(Dispatchers.Main)

    fun connect(onReady: (() -> Unit)? = null) {
        if (controller != null) { onReady?.invoke(); return }
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            runCatching { f.get() }.onSuccess { c ->
                controller = c
                c.addListener(object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) = refresh() })
                refresh(); onReady?.invoke()
                scope.launch { while (isActive) { if (controller?.isPlaying == true) refresh(); delay(500) } }
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun refresh() {
        val c = controller ?: return
        val md = c.mediaMetadata
        val ex = c.currentMediaItem?.mediaMetadata?.extras
        _state.value = PlayerState(true, c.isPlaying, c.playbackState == Player.STATE_BUFFERING, md.title?.toString() ?: "", md.artist?.toString() ?: "",
            c.currentMediaItemIndex, c.mediaItemCount, c.currentPosition, c.duration.coerceAtLeast(0), c.repeatMode,
            ex?.getInt("surah") ?: 0, ex?.getString("reciter") ?: "", c.playerError?.let { "Ses açılamadı: ${it.errorCodeName}. İnternet bağlantınızı veya indirmeyi kontrol edin." }, c.volume)
    }

    /** Hafızın mevcut surelerinden oynatma listesi kurar; indirilmiş dosya varsa internetsiz o dosya çalınır. */
    fun playSurahs(r: Reciter, surahs: List<Int>, start: Int) {
        connect {
            val c = controller ?: return@connect
            val list = surahs.filter { it in r.surahs }
            val items = list.map { n ->
                val f = content.quranFile(r.id, n)
                val uri = if (f.exists() && f.length() > 0) Uri.fromFile(f) else Uri.parse(r.surahUrl(n))
                val s = content.surahs[n - 1]
                MediaItem.Builder().setMediaId("${r.id}:$n").setUri(uri).setMediaMetadata(
                    MediaMetadata.Builder().setTitle("${s.n}. ${s.tr} Suresi").setArtist(r.tr).setAlbumTitle("Kur'an-ı Kerim")
                        .setExtras(Bundle().apply { putInt("surah", n); putString("reciter", r.id) }).build()).build()
            }
            if (items.isEmpty()) return@connect
            c.setMediaItems(items, list.indexOf(start).coerceAtLeast(0), 0)
            c.prepare(); c.play()
        }
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_IDLE) it.prepare(); it.play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }
    fun fwd() { controller?.seekForward() }
    fun back() { controller?.seekBack() }
    fun seek(ms: Long) { controller?.seekTo(ms) }
    fun stop() { controller?.stop(); controller?.clearMediaItems() }
    fun jump(i: Int) { controller?.seekTo(i, 0) }
    fun setVolume(v: Float) { controller?.volume = v; refresh() }
    fun cycleRepeat() {
        controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE; Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL; else -> Player.REPEAT_MODE_OFF } }
    }
    fun titles(): List<String> { val c = controller ?: return emptyList(); return (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaMetadata.title.toString() } }
}
