package com.nurvakti.global.prayer

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    fun format(lang: String): String = "$day ${Hijri.monthName(month, lang)} $year"
}
data class ReligiousDay(val date: LocalDate, val key: String, val tr: String, val en: String, val ar: String, val night: Boolean, val hijri: HijriDate)

/** Ümmü'l-Kurâ (Suudi resmî) takvimine dayalı Hicri tarih. Ülkelere göre ±1-2 gün fark olabileceği için ayarlanabilir düzeltme vardır. */
object Hijri {
    private val TR = listOf("Muharrem", "Safer", "Rebîülevvel", "Rebîülâhir", "Cemâziyelevvel", "Cemâziyelâhir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce")
    private val EN = listOf("Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah")
    private val AR = listOf("مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الآخِر", "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان", "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة")
    fun monthName(m: Int, lang: String) = when (lang) { "en" -> EN[m - 1]; "ar" -> AR[m - 1]; else -> TR[m - 1] }

    fun of(date: LocalDate, adjust: Int = 0): HijriDate {
        val d = date.plusDays(adjust.toLong())
        val cal = IslamicCalendar(ULocale("@calendar=islamic-umalqura"))
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis = d.atTime(12, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        cal.timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
        return HijriDate(cal.get(IslamicCalendar.DAY_OF_MONTH), cal.get(IslamicCalendar.MONTH) + 1, cal.get(IslamicCalendar.YEAR))
    }

    /** Verilen Miladi yıl için dini gün ve geceler. Kandiller "gecesi" bir önceki akşam başlar; tarih gecenin başladığı Miladi gündür. */
    fun religiousDays(year: Int, adjust: Int): List<ReligiousDay> {
        val res = ArrayList<ReligiousDay>()
        var d = LocalDate.of(year, 1, 1).minusDays(1)
        val end = LocalDate.of(year, 12, 31)
        var regaibDone = -1
        while (!d.isAfter(end)) {
            val next = d.plusDays(1)
            val h = of(d, adjust); val hn = of(next, adjust)
            fun add(date: LocalDate, key: String, tr: String, en: String, ar: String, night: Boolean, hd: HijriDate) {
                if (date.year == year) res += ReligiousDay(date, key, tr, en, ar, night, hd)
            }
            // Gündüz olayları
            if (h.month == 1 && h.day == 1) add(d, "hijri_new", "Hicri Yılbaşı", "Islamic New Year", "رأس السنة الهجرية", false, h)
            if (h.month == 1 && h.day == 10) add(d, "ashura", "Aşure Günü", "Day of Ashura", "يوم عاشوراء", false, h)
            if (h.month == 7 && h.day == 1) add(d, "uc_aylar", "Üç Ayların Başlangıcı", "Start of the Three Holy Months", "بداية الأشهر الثلاثة", false, h)
            if (h.month == 9 && h.day == 1) add(d, "ramazan", "Ramazan Başlangıcı", "Start of Ramadan", "بداية رمضان", false, h)
            if (hn.month == 10 && hn.day == 1) add(d, "arefe_r", "Ramazan Bayramı Arefesi", "Eve of Eid al-Fitr", "وقفة عيد الفطر", false, h)
            if (h.month == 10 && h.day in 1..3) add(d, "fitr${h.day}", "Ramazan Bayramı ${h.day}. Gün", "Eid al-Fitr Day ${h.day}", "عيد الفطر - اليوم ${h.day}", false, h)
            if (h.month == 12 && h.day == 9) add(d, "arefe", "Kurban Bayramı Arefesi", "Day of Arafah", "يوم عرفة", false, h)
            if (h.month == 12 && h.day in 10..13) add(d, "adha${h.day - 9}", "Kurban Bayramı ${h.day - 9}. Gün", "Eid al-Adha Day ${h.day - 9}", "عيد الأضحى - اليوم ${h.day - 9}", false, h)
            // Geceler (akşamdan başlar → d günü akşamı, ertesi Hicri güne aittir)
            if (hn.month == 3 && hn.day == 12) add(d, "mevlid", "Mevlid Kandili", "Mawlid an-Nabi (night)", "ليلة المولد النبوي", true, hn)
            if (hn.month == 7 && next.dayOfWeek == DayOfWeek.FRIDAY && hn.day <= 7 && regaibDone != hn.year) {
                add(d, "regaib", "Regaib Kandili", "Laylat al-Raghaib", "ليلة الرغائب", true, hn); regaibDone = hn.year
            }
            if (hn.month == 7 && hn.day == 27) add(d, "mirac", "Miraç Kandili", "Laylat al-Mi'raj", "ليلة المعراج", true, hn)
            if (hn.month == 8 && hn.day == 15) add(d, "berat", "Berat Kandili", "Laylat al-Bara'ah", "ليلة البراءة", true, hn)
            if (hn.month == 9 && hn.day == 27) add(d, "kadir", "Kadir Gecesi", "Laylat al-Qadr", "ليلة القدر", true, hn)
            d = next
        }
        return res.sortedBy { it.date }
    }

    /** Bir sonraki Ramazan'ın 1. günü (bugün Ramazan'daysa bu yılın) */
    fun nextRamadanStart(from: LocalDate, adjust: Int): LocalDate {
        var d = from
        repeat(400) { val h = of(d, adjust); if (h.month == 9 && h.day == 1) return d; if (h.month == 9) { return d.minusDays((h.day - 1).toLong()) }; d = d.plusDays(1) }
        return from
    }
    fun nextEid(from: LocalDate, adjust: Int): Pair<LocalDate, String> {
        var d = from
        repeat(400) { val h = of(d, adjust)
            if (h.month == 10 && h.day == 1) return d to "Ramazan Bayramı"
            if (h.month == 12 && h.day == 10) return d to "Kurban Bayramı"
            d = d.plusDays(1) }
        return from to ""
    }
}
