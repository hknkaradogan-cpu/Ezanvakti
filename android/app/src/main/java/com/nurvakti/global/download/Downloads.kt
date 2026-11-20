package com.nurvakti.global.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nurvakti.global.R
import com.nurvakti.global.alarm.Channels
import com.nurvakti.global.data.AppDb
import com.nurvakti.global.data.DownloadRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** İndirme merkezi: WorkManager ile arka planda, kaldığı yerden devam edebilen (HTTP Range) indirmeler. */
object Downloads {
    const val QUEUED = "QUEUED"; const val RUNNING = "RUNNING"; const val PAUSED = "PAUSED"; const val DONE = "DONE"; const val FAILED = "FAILED"

    suspend fun enqueue(ctx: Context, id: String, kind: String, title: String, url: String, path: String, expected: Long = 0) {
        val dao = AppDb.get(ctx).dao()
        val old = dao.download(id)
        if (old?.status == DONE && File(old.path).exists()) return
        dao.putDownload(DownloadRow(id, kind, title, url, path, old?.total ?: expected, old?.done ?: 0, QUEUED, null, System.currentTimeMillis()))
        start(ctx, id)
    }
    private fun start(ctx: Context, id: String) {
        val req = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(workDataOf("id" to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).addTag("dl").build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("dl:$id", ExistingWorkPolicy.KEEP, req)
    }
    suspend fun pause(ctx: Context, id: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("dl:$id")
        val dao = AppDb.get(ctx).dao(); dao.download(id)?.let { dao.progress(id, it.done, it.total, PAUSED, null) }
    }
    suspend fun resume(ctx: Context, id: String) {
        val dao = AppDb.get(ctx).dao(); dao.download(id)?.let { dao.progress(id, it.done, it.total, QUEUED, null); start(ctx, id) }
    }
    suspend fun cancel(ctx: Context, id: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork("dl:$id")
        val dao = AppDb.get(ctx).dao(); dao.download(id)?.let { File(it.path + ".part").delete(); File(it.path).delete() }; dao.delDownload(id)
    }
    suspend fun delete(ctx: Context, id: String) = cancel(ctx, id)

    fun freeBytes(ctx: Context): Long = StatFs(ctx.filesDir.path).availableBytes
    fun usedBytes(ctx: Context): Long = File(ctx.filesDir, "media").walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

class DownloadWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun getForegroundInfo(): ForegroundInfo = info("İndiriliyor…", 0)

    private fun info(text: String, pct: Int): ForegroundInfo {
        val n: Notification = NotificationCompat.Builder(applicationContext, Channels.DOWNLOAD).setSmallIcon(R.drawable.ic_stat_nur)
            .setContentTitle("Namaz Vakti · İndirme").setContentText(text).setProgress(100, pct, pct == 0).setOngoing(true).setSilent(true).build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(4242, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(4242, n)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("id") ?: return@withContext Result.failure()
        val dao = AppDb.get(applicationContext).dao()
        val row = dao.download(id) ?: return@withContext Result.failure()
        runCatching { setForeground(info(row.title, 0)) }
        val target = File(row.path); val part = File(row.path + ".part"); target.parentFile?.mkdirs()
        var done = if (part.exists()) part.length() else 0L
        try {
            val c = (URL(row.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000; readTimeout = 30000; setRequestProperty("User-Agent", "NurVaktiGlobal/1.0")
                if (done > 0) setRequestProperty("Range", "bytes=$done-")
            }
            val code = c.responseCode
            if (code == 200) done = 0 else if (code != 206) throw Exception("HTTP $code")
            val total = if (code == 206) done + c.contentLengthLong.coerceAtLeast(0) else c.contentLengthLong.coerceAtLeast(0)
            dao.progress(id, done, total, Downloads.RUNNING, null)
            RandomAccessFile(part, "rw").use { raf ->
                raf.setLength(done); raf.seek(done)
                c.inputStream.use { ins ->
                    val buf = ByteArray(64 * 1024); var last = 0L
                    while (true) {
                        if (!coroutineContext.isActive || isStopped) { dao.progress(id, done, total, Downloads.PAUSED, null); return@withContext Result.success() }
                        val r = ins.read(buf); if (r < 0) break
                        raf.write(buf, 0, r); done += r
                        if (done - last > 256 * 1024) { last = done; dao.progress(id, done, total, Downloads.RUNNING, null)
                            if (total > 0) runCatching { setForeground(info(row.title, (done * 100 / total).toInt())) } }
                    }
                }
            }
            if (total > 0 && done < total) throw Exception("Eksik indirme ($done/$total bayt)")
            if (done < 1000) throw Exception("Dosya geçersiz")
            target.delete(); part.renameTo(target)
            dao.progress(id, done, done, Downloads.DONE, null)
            Result.success()
        } catch (e: Exception) {
            dao.progress(id, done, row.total, Downloads.FAILED, e.message ?: e.javaClass.simpleName)
            Result.failure()
        }
    }
}
