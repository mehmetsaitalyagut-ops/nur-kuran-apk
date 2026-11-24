package com.example.data.remote

import android.util.Log
import com.example.model.Ayah
import com.example.model.DailyVerse
import com.example.model.QuranCatalog
import com.example.model.Reciter
import com.example.model.SearchResultItem
import com.example.model.SurahContent
import com.example.model.SurahInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.text.Normalizer
import java.util.concurrent.TimeUnit

class QuranRepository {

    private val api: AlQuranApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/v1/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlQuranApiService::class.java)
    }

    private val surahCache = mutableMapOf<Pair<Int, String>, SurahContent>()
    private val cacheMutex = Mutex()

    suspend fun getSurahContent(
        surahNumber: Int,
        reciter: Reciter
    ): Result<SurahContent> = withContext(Dispatchers.IO) {
        val cacheKey = surahNumber to reciter.id
        cacheMutex.withLock {
            surahCache[cacheKey]?.let { return@withContext Result.success(it) }
        }

        try {
            val response = try {
                api.getSurahEditions(
                    surahNumber = surahNumber,
                    editions = "quran-uthmani,tr.diyanet,${reciter.id}"
                )
            } catch (e: HttpException) {
                Log.w(TAG, "3-edition fetch returned ${e.code()} for ${reciter.id}, retrying text + fallback audio CDN", e)
                api.getSurahEditions(
                    surahNumber = surahNumber,
                    editions = "quran-uthmani,tr.diyanet"
                )
            }

            val editions = response.data
            if (editions.size < 2) {
                return@withContext Result.failure(IllegalStateException("Sure verileri eksik döndü."))
            }

            val arabicEdition = editions.firstOrNull { it.edition.identifier == "quran-uthmani" } ?: editions[0]
            val turkishEdition = editions.firstOrNull { it.edition.identifier == "tr.diyanet" } ?: editions[1]
            val audioEdition = editions.firstOrNull { it.edition.identifier == reciter.id }

            val surahMeta = QuranCatalog.surahByNumber(surahNumber)
            val ayahs = arabicEdition.ayahs.mapIndexed { index, arAyah ->
                val trAyah = turkishEdition.ayahs.getOrNull(index)
                val audioAyah = audioEdition?.ayahs?.getOrNull(index)
                val rawResolvedAudio = audioAyah?.audio?.takeIf { it.isNotBlank() }
                    ?: audioAyah?.audioSecondary?.firstOrNull { it.isNotBlank() }
                    ?: "https://cdn.islamic.network/quran/audio/128/${reciter.id}/${arAyah.number}.mp3"
                val resolvedAudio = rawResolvedAudio.replaceFirst("http://", "https://")
                val fallbackAudio = QuranCatalog.buildEveryAyahUrl(
                    reciter = reciter,
                    surahNumber = surahMeta.number,
                    ayahInSurah = arAyah.numberInSurah
                )

                Ayah(
                    globalNumber = arAyah.number,
                    numberInSurah = arAyah.numberInSurah,
                    arabicText = arAyah.text,
                    turkishText = trAyah?.text.orEmpty(),
                    audioUrl = resolvedAudio,
                    juz = arAyah.juz,
                    page = arAyah.page,
                    hizbQuarter = arAyah.hizbQuarter,
                    fallbackAudioUrl = fallbackAudio
                )
            }

            val content = SurahContent(
                surah = surahMeta,
                reciter = reciter,
                ayahs = ayahs
            )
            cacheMutex.withLock {
                surahCache[cacheKey] = content
            }
            Result.success(content)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Network error loading surah $surahNumber", e)
            Result.failure(IOException("İnternet bağlantısı kurulamadı. Lütfen bağlantınızı kontrol edin."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load surah $surahNumber", e)
            Result.failure(IllegalStateException("Sure yüklenirken bir sorun oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }

    suspend fun getDailyVerse(dayOfYear: Int): DailyVerse = withContext(Dispatchers.IO) {
        val fallback = QuranCatalog.verseOfToday(dayOfYear)
        try {
            val response = api.getAyahEditions(
                reference = "${fallback.surahNumber}:${fallback.ayahNumber}",
                editions = "quran-uthmani,tr.diyanet"
            )
            val ar = response.data.firstOrNull { it.edition.identifier == "quran-uthmani" }
            val tr = response.data.firstOrNull { it.edition.identifier == "tr.diyanet" }
            if (ar != null && tr != null) {
                fallback.copy(arabicText = ar.text, turkishText = tr.text)
            } else {
                fallback
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun smartSearch(rawQuery: String): Result<List<SearchResultItem>> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@withContext Result.success(emptyList())

        val results = mutableListOf<SearchResultItem>()
        val normalizedQuery = normalizeForSearch(query)

        // 1. Check Surah:Ayah pattern like "2:255", "36:1", "2 255"
        val refMatch = Regex("^(\\d{1,3})\\s*[:\\s/]\\s*(\\d{1,3})$").find(query)
        if (refMatch != null) {
            val sNum = refMatch.groupValues[1].toIntOrNull()
            val aNum = refMatch.groupValues[2].toIntOrNull()
            if (sNum != null && aNum != null && sNum in 1..114) {
                val surah = QuranCatalog.surahByNumber(sNum)
                if (aNum in 1..surah.ayahCount) {
                    try {
                        val resp = api.getAyahEditions("$sNum:$aNum", "quran-uthmani,tr.diyanet")
                        val trText = resp.data.firstOrNull { it.edition.identifier == "tr.diyanet" }?.text
                            ?: "${surah.nameTr} Suresi $aNum. Ayet"
                        results.add(
                            SearchResultItem.AyahMatch(
                                surahNumber = sNum,
                                surahNameTr = surah.nameTr,
                                surahNameAr = surah.nameAr,
                                ayahNumber = aNum,
                                matchedText = trText,
                                isArabic = false
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        results.add(
                            SearchResultItem.AyahMatch(
                                surahNumber = sNum,
                                surahNameTr = surah.nameTr,
                                surahNameAr = surah.nameAr,
                                ayahNumber = aNum,
                                matchedText = "${surah.nameTr} Suresi $aNum. Ayet",
                                isArabic = false
                            )
                        )
                    }
                }
            }
        }

        // 2. Match Reciters by name/country/style
        QuranCatalog.reciters.forEach { reciter ->
            val searchable = normalizeForSearch("${reciter.nameTr} ${reciter.titleTr} ${reciter.countryTr}")
            if (searchable.contains(normalizedQuery)) {
                results.add(SearchResultItem.ReciterMatch(reciter))
            }
        }

        // 3. Match Surahs by number, Turkish name, Turkish meaning, or Arabic name
        val asNumber = query.toIntOrNull()
        QuranCatalog.surahs.forEach { surah ->
            val matchesNumber = asNumber != null && surah.number == asNumber
            val matchesText = normalizeForSearch("${surah.nameTr} ${surah.meaningTr}").contains(normalizedQuery) ||
                surah.nameAr.contains(query)
            if (matchesNumber || matchesText) {
                results.add(SearchResultItem.SurahMatch(surah))
            }
        }

        // 4. Live full-Quran search in Turkish translation or Arabic text (if query >= 3 chars)
        if (query.length >= 3 && asNumber == null && refMatch == null) {
            val isArabicInput = query.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
            val edition = if (isArabicInput) "quran-uthmani" else "tr.diyanet"
            try {
                val apiSearch = api.searchQuran(keyword = query, edition = edition)
                val matches = apiSearch.data?.matches.orEmpty().take(20)
                matches.forEach { match ->
                    val surah = QuranCatalog.surahByNumber(match.surah.number)
                    results.add(
                        SearchResultItem.AyahMatch(
                            surahNumber = surah.number,
                            surahNameTr = surah.nameTr,
                            surahNameAr = surah.nameAr,
                            ayahNumber = match.numberInSurah,
                            matchedText = match.text,
                            isArabic = isArabicInput
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                // AlQuran Cloud returns 404 when no keyword matches are found
                if (e.code() != 404) {
                    Log.w(TAG, "Search HTTP error ${e.code()}", e)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Search network error", e)
                if (results.isEmpty()) {
                    return@withContext Result.failure(IOException("Arama için internet bağlantısı gerekli."))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Search failed", e)
            }
        }

        Result.success(results)
    }

    private fun normalizeForSearch(input: String): String {
        val replaced = input
            .replace("â", "a").replace("Â", "a")
            .replace("î", "i").replace("Î", "i")
            .replace("û", "u").replace("Û", "u")
            .replace("ı", "i").replace("İ", "i")
            .replace("ş", "s").replace("Ş", "s")
            .replace("ğ", "g").replace("Ğ", "g")
            .replace("ü", "u").replace("Ü", "u")
            .replace("ö", "o").replace("Ö", "o")
            .replace("ç", "c").replace("Ç", "c")
            .replace("'", "").replace("’", "")
            .lowercase()
        return Normalizer.normalize(replaced, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private companion object {
        const val TAG = "QuranRepository"
    }
}
