package com.example.musikku.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/**
 * Pengelola cache pemutaran musik streaming berbasis Media3 SimpleCache.
 * Menyimpan data audio yang di-stream di cache lokal secara transparan
 * agar pemutaran ulang atau seeking tidak membebani jaringan berulang kali.
 */
@OptIn(UnstableApi::class)
object MusicCacheManager {

    @Volatile
    private var simpleCache: SimpleCache? = null
    private const val MAX_CACHE_SIZE = 100L * 1024 * 1024 // Batas ukuran cache 100 MB

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "media_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
                val databaseProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, databaseProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    /**
     * Membangun MediaSourceFactory yang dibungkus oleh CacheDataSource.Factory.
     * Secara otomatis meng-cache audio streaming HTTP dari Jamendo ke SimpleCache.
     */
    fun createMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)
    }
}
