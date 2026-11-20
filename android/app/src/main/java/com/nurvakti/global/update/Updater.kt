package com.nurvakti.global.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nurvakti.global.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val code: Int, val notes: String)

/** Uygulama içi güncelleme: GitHub Releases'taki son sürümü denetler, APK'yı indirip Android yükleyicisini açar. */
object Updater {
    private const val REPO = "hknkaradogan-cpu/Ezanvakti"
    const val APK_URL = "https://github.com/$REPO/releases/latest/download/NamazVakti.apk"

    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "NamazVakti-Android")
            }
            if (c.responseCode != 200) return@runCatching null
            val o = Json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonObject
            val tag = o["tag_name"]!!.jsonPrimitive.content.removePrefix("v")
            val code = tag.substringAfterLast('.').toIntOrNull() ?: return@runCatching null
            if (code > BuildConfig.VERSION_CODE) UpdateInfo(tag, code, o["body"]?.jsonPrimitive?.content ?: "") else null
        }.getOrNull()
    }

    /** APK'yı indirir; ilerleme 0..100. Başarılıysa dosyayı döndürür. */
    suspend fun download(ctx: Context, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val f = File(ctx.cacheDir, "update/NamazVakti.apk").apply { parentFile?.mkdirs(); delete() }
        var url = URL(APK_URL)
        var c: HttpURLConnection
        var hops = 0
        while (true) { // GitHub yönlendirmelerini izle
            c = (url.openConnection() as HttpURLConnection).apply { instanceFollowRedirects = false; connectTimeout = 20000; readTimeout = 30000; setRequestProperty("User-Agent", "NamazVakti-Android") }
            val code = c.responseCode
            if (code in 300..399 && hops < 6) { url = URL(url, c.getHeaderField("Location")); hops++; continue }
            if (code != 200) throw Exception("HTTP $code")
            break
        }
        val total = c.contentLengthLong
        c.inputStream.use { ins -> f.outputStream().use { out ->
            val buf = ByteArray(64 * 1024); var done = 0L
            while (true) { val r = ins.read(buf); if (r < 0) break; out.write(buf, 0, r); done += r; if (total > 0) progress((done * 100 / total).toInt()) }
        } }
        if (f.length() < 1_000_000) throw Exception("Dosya eksik indi")
        f
    }

    fun install(ctx: Context, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInBrowser(ctx: Context) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
