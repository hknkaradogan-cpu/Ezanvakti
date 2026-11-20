package com.nurvakti.global.data

import kotlinx.serialization.Serializable

@Serializable data class Surah(val n: Int, val ar: String, val tl: String, val tr: String, val en: String, val type: String, val ayahs: Int)
@Serializable data class DuaCat(val id: String, val tr: String, val en: String)
@Serializable data class Dua(
    val id: Int, val cat: String, val title: String, val ar: String, val okunus: String,
    val tr: String, val en: String, val src: String, val quran: Boolean, val ref: String? = null,
)
@Serializable data class DuaFile(val cats: List<DuaCat>, val items: List<Dua>)
@Serializable data class Esma(val n: Int, val ar: String, val okunus: String, val tr: String, val en: String)
@Serializable data class Hadis(val tr: String, val en: String, val src: String)
@Serializable data class Ayet(val ar: String, val tr: String, val en: String, val ref: String, val s: Int, val a: Int, val name: String)
@Serializable data class Zikir(val ar: String, val okunus: String, val tr: String, val en: String, val count: Int, val src: String)
@Serializable data class Rehber(
    val id: String, val tr: String, val en: String, val steps: List<String>, val farz: String? = null,
    val safii: String? = null, val rekat: String? = null, val en_summary: String = "",
)
@Serializable data class Reciter(
    val id: String, val tr: String, val en: String, val info: String, val source: String,
    val edition: String, val bitrate: Int, val url: String, val surahs: List<Int>,
    val sizes: Map<String, Long> = emptyMap(), val verified: Boolean = false,
) {
    fun surahUrl(n: Int) = url.replace("{nnn}", "%03d".format(n)).replace("{n}", n.toString())
    val complete get() = surahs.size == 114
}
@Serializable data class ReciterFile(val generated: String = "", val reciters: List<Reciter>)
@Serializable data class EzanItem(
    val id: String, val tr: String, val en: String, val file: String, val bundled: Boolean,
    val author: String, val license: String, val source: String, val changes: String = "",
    val available: Boolean = true, val size: Long = 0,
)
@Serializable data class EzanFile(val ezan: List<EzanItem>)
@Serializable data class Country(val c: String, val n: String, val lat: Double, val lng: Double, val tz: List<List<String>> = emptyList())

/** Bir şehir / ilçe / il kaydı */
data class Place(val name: String, val region: String, val lat: Double, val lng: Double, val country: String)
