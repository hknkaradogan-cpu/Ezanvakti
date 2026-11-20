package com.nurvakti.global.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** Uygulama içine gömülü (internetsiz çalışan) içerik deposu. Tüm dosyalar web/data klasöründen gelir. */
class Content(private val ctx: Context) {
    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private fun text(path: String): String = ctx.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    val surahs: List<Surah> by lazy { json.decodeFromString(text("quran/surahs.json")) }
    private val quranCache = HashMap<String, List<List<String>>>()
    @Synchronized fun quran(edition: String): List<List<String>> =
        quranCache.getOrPut(edition) { json.decodeFromString(text("quran/$edition.json")) }

    val duas: DuaFile by lazy { json.decodeFromString(text("content/dualar.json")) }
    val esma: List<Esma> by lazy { json.decodeFromString(text("content/esma.json")) }
    val hadis: List<Hadis> by lazy { json.decodeFromString(text("content/hadis.json")) }
    val ayet: List<Ayet> by lazy { json.decodeFromString(text("content/ayet.json")) }
    val zikir: List<Zikir> by lazy { json.decodeFromString(text("content/zikir.json")) }
    val rehber: List<Rehber> by lazy { json.decodeFromString(text("content/rehber.json")) }
    val reciters: List<Reciter> by lazy { json.decodeFromString<ReciterFile>(text("media/reciters.json")).reciters }
    val ezans: List<EzanItem> by lazy { json.decodeFromString<EzanFile>(text("media/ezan.json")).ezan }
    val countries: List<Country> by lazy { json.decodeFromString(text("loc/countries.json")) }
    val i18n: Map<String, Map<String, String>> by lazy {
        runCatching { json.decodeFromString<Map<String, Map<String, String>>>(text("i18n.json")) }.getOrDefault(emptyMap())
    }

    private val placeCache = HashMap<String, List<Place>>()
    /** Ülkenin il/eyalet ve şehir/ilçe listesi (countries-states-cities-database, ODbL). */
    @Synchronized fun places(cc: String): List<Place> = placeCache.getOrPut(cc) {
        val o = runCatching { json.parseToJsonElement(text("loc/$cc.json")) as JsonObject }.getOrNull() ?: return@getOrPut emptyList()
        val states = (o["s"] as? JsonArray)?.associate { val a = it.jsonArray; a[0].jsonPrimitive.content to a[1].jsonPrimitive.content } ?: emptyMap()
        val res = ArrayList<Place>()
        (o["s"] as? JsonArray)?.forEach { val a = it.jsonArray
            res += Place(a[1].jsonPrimitive.content, "", a[2].jsonPrimitive.double, a[3].jsonPrimitive.double, cc) }
        (o["c"] as? JsonArray)?.forEach { val a = it.jsonArray
            res += Place(a[0].jsonPrimitive.content, states[a[1].jsonPrimitive.content] ?: "", a[2].jsonPrimitive.double, a[3].jsonPrimitive.double, cc) }
        res
    }

    fun dayIndex(): Int = (System.currentTimeMillis() / 86_400_000L).toInt()
    fun ayetOfDay() = ayet[dayIndex() % ayet.size]
    fun hadisOfDay() = hadis[dayIndex() % hadis.size]
    fun duaOfDay() = duas.items[dayIndex() % duas.items.size]
    fun esmaOfDay() = esma[dayIndex() % esma.size]
    fun zikirOfDay() = zikir[dayIndex() % zikir.size]

    fun ezanFile(item: EzanItem): File = File(ctx.filesDir, "media/ezan/${item.file}")
    fun quranFile(reciter: String, n: Int): File = File(ctx.filesDir, "media/quran/$reciter/$n.mp3")
}
