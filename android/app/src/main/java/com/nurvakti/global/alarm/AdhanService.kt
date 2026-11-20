package com.nurvakti.global.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nurvakti.global.MainActivity
import com.nurvakti.global.R
import com.nurvakti.global.data.Content
import java.io.File

/** Ezanı ön plan servisinde çalar: ekran kapalıyken ve uygulama kapalıyken de çalışır. Ses, ALARM akışını kullanır. */
class AdhanService : Service() {
    private var player: ExoPlayer? = null

    companion object {
        const val STOP = "com.nurvakti.global.STOP_ADHAN"
        fun start(ctx: Context, prayer: String, short: Boolean, sound: String, custom: String?) {
            val i = Intent(ctx, AdhanService::class.java).putExtra("prayer", prayer).putExtra("short", short)
                .putExtra("sound", sound).putExtra("custom", custom)
            ContextCompat.startForegroundService(ctx, i)
        }
        fun preview(ctx: Context, sound: String, custom: String?) = start(ctx, "preview", false, sound, custom)
        fun stop(ctx: Context) { ctx.startService(Intent(ctx, AdhanService::class.java).setAction(STOP)) }

        /** Ses kaynağı: indirilen dosya > uygulamaya gömülü dosya > kullanıcının seçtiği dosya. Hiçbiri yoksa gömülü klasik ezan. */
        fun resolve(ctx: Context, sound: String, custom: String?, short: Boolean): Uri {
            if (sound == "custom" && custom != null) return Uri.parse(custom)
            val content = Content(ctx)
            val id = if (short) "kisa" else sound
            val item = content.ezans.firstOrNull { it.id == id } ?: content.ezans.first()
            val f = content.ezanFile(item)
            if (f.exists() && f.length() > 0) return Uri.fromFile(f)
            if (item.bundled) return Uri.parse("asset:///ezan/${item.file}")
            return Uri.parse("asset:///ezan/klasik.mp3")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        val prayer = intent?.getStringExtra("prayer") ?: "preview"
        val name = PRAYER_TR[prayer] ?: "Ezan"
        val n = buildNotification(if (prayer == "preview") "Ezan önizleme" else "$name ezanı", "Durdurmak için dokunun")
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, 77, n, type)

        val uri = resolve(this, intent?.getStringExtra("sound") ?: "klasik", intent?.getStringExtra("custom"), intent?.getBooleanExtra("short", false) == true)
        player?.release()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_ALARM).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            setWakeMode(C.WAKE_MODE_LOCAL)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) stopSelf() }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    // Seçilen dosya açılamazsa gömülü klasik ezana dön
                    if (uri.toString() != "asset:///ezan/klasik.mp3") { setMediaItem(MediaItem.fromUri("asset:///ezan/klasik.mp3")); prepare(); play() } else stopSelf()
                }
            })
            setMediaItem(MediaItem.fromUri(uri)); prepare(); play()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(title: String, text: String): Notification {
        val stop = PendingIntent.getService(this, 1, Intent(this, AdhanService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, Channels.EZAN).setSmallIcon(R.drawable.ic_stat_nur).setContentTitle(title).setContentText(text)
            .setOngoing(true).setContentIntent(stop).setDeleteIntent(stop).addAction(0, "Durdur", stop).addAction(0, "Uygulamayı aç", open)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()
    }

    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
