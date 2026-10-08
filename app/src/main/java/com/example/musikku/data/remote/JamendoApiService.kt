package com.example.musikku.data.remote

import com.example.musikku.data.remote.dto.JamendoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit untuk berkomunikasi dengan Jamendo API v3.0.
 */
interface JamendoApiService {

    /**
     * Cari lagu berdasarkan kata kunci nama/judul atau genre tag.
     */
    @GET("v3.0/tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("namesearch") nameSearch: String? = null,
        @Query("fuzzytags") fuzzyTags: String? = null,
        @Query("order") order: String = "popularity_month",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDlFormat: String = "mp32"
    ): JamendoResponse

    /**
     * Ambil daftar lagu paling populer di Jamendo.
     */
    @GET("v3.0/tracks/")
    suspend fun getPopularTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 30,
        @Query("order") order: String = "popularity_total",
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("audiodlformat") audioDlFormat: String = "mp32"
    ): JamendoResponse

    companion object {
        const val BASE_URL = "https://api.jamendo.com/"
    }
}
