package com.example.model

data class SurahInfo(
    val number: Int,
    val nameAr: String,
    val nameTr: String,
    val meaningTr: String,
    val revelationTypeTr: String,
    val ayahCount: Int,
    val isPopular: Boolean = false
)

data class Reciter(
    val id: String,
    val nameTr: String,
    val titleTr: String,
    val countryTr: String,
    val styleTr: String,
    val initials: String,
    val everyAyahFolder: String = ""
)

data class Ayah(
    val globalNumber: Int,
    val numberInSurah: Int,
    val arabicText: String,
    val turkishText: String,
    val audioUrl: String,
    val juz: Int,
    val page: Int,
    val hizbQuarter: Int,
    val fallbackAudioUrl: String = ""
)

data class SurahContent(
    val surah: SurahInfo,
    val reciter: Reciter,
    val ayahs: List<Ayah>,
    val translationSource: String = "Diyanet İşleri Başkanlığı Meali"
)

data class JuzInfo(
    val number: Int,
    val titleTr: String,
    val rangeTr: String,
    val startSurah: Int,
    val startAyah: Int
)

data class DailyVerse(
    val surahNumber: Int,
    val surahNameTr: String,
    val ayahNumber: Int,
    val arabicText: String,
    val turkishText: String
)

sealed interface SearchResultItem {
    data class SurahMatch(val surah: SurahInfo) : SearchResultItem
    data class ReciterMatch(val reciter: Reciter) : SearchResultItem
    data class AyahMatch(
        val surahNumber: Int,
        val surahNameTr: String,
        val surahNameAr: String,
        val ayahNumber: Int,
        val matchedText: String,
        val isArabic: Boolean
    ) : SearchResultItem
}
