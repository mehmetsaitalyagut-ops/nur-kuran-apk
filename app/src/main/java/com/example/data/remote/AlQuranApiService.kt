package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class SurahEditionsResponse(
    val code: Int,
    val status: String,
    val data: List<ApiSurahEditionDto>
)

@JsonClass(generateAdapter = true)
data class ApiSurahEditionDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<ApiAyahDto>,
    val edition: ApiEditionDto
)

@JsonClass(generateAdapter = true)
data class ApiAyahDto(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val hizbQuarter: Int,
    val audio: String? = null,
    val audioSecondary: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiEditionDto(
    val identifier: String,
    val language: String,
    val name: String,
    val format: String
)

@JsonClass(generateAdapter = true)
data class AyahEditionsResponse(
    val code: Int,
    val status: String,
    val data: List<ApiSingleAyahEditionDto>
)

@JsonClass(generateAdapter = true)
data class ApiSingleAyahEditionDto(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val hizbQuarter: Int,
    val audio: String? = null,
    val surah: ApiSurahMetaDto,
    val edition: ApiEditionDto
)

@JsonClass(generateAdapter = true)
data class ApiSurahMetaDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val code: Int,
    val status: String,
    val data: ApiSearchDataDto? = null
)

@JsonClass(generateAdapter = true)
data class ApiSearchDataDto(
    val count: Int = 0,
    val matches: List<ApiSearchMatchDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiSearchMatchDto(
    val number: Int,
    val text: String,
    val numberInSurah: Int,
    val surah: ApiSurahMetaDto
)

interface AlQuranApiService {
    @GET("surah/{number}/editions/{editions}")
    suspend fun getSurahEditions(
        @Path("number") surahNumber: Int,
        @Path("editions") editions: String
    ): SurahEditionsResponse

    @GET("ayah/{reference}/editions/{editions}")
    suspend fun getAyahEditions(
        @Path("reference") reference: String,
        @Path("editions") editions: String
    ): AyahEditionsResponse

    @GET("search/{keyword}/all/{edition}")
    suspend fun searchQuran(
        @Path("keyword") keyword: String,
        @Path("edition") edition: String
    ): SearchResponse
}
