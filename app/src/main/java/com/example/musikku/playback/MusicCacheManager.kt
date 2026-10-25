package com.example.musikku.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
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
     * Secara otomatis meng-cache audio streaming HTTP dari Jamendo & YouTube ke SimpleCache,
     * serta menyuntikkan header yang kompatibel untuk streaming audio YouTube.
     */
    fun createMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val cache = getCache(context)
        val baseHttpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:153.0) Gecko/20100101 Firefox/153.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(25_000)

        val youtubeAwareHttpFactory = YouTubeAwareDataSourceFactory(baseHttpDataSourceFactory)
        val upstreamFactory = DefaultDataSource.Factory(context, youtubeAwareHttpFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)
    }
}

/**
 * DataSource yang secara dinamis menyesuaikan header HTTP saat melakukan streaming dari server YouTube (googlevideo.com).
 * Menambahkan Origin, Referer, serta User-Agent yang sesuai dengan jenis stream (misal visionOS)
 * agar tidak diblokir oleh anti-bot atau pembatasan server Google Video (HTTP 403 Forbidden).
 */
@OptIn(UnstableApi::class)
class YouTubeAwareDataSource(
    private val delegate: DataSource
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        delegate.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val uriStr = dataSpec.uri.toString()
        val isYouTube = uriStr.contains("googlevideo.com") || uriStr.contains("youtube.com")

        val targetSpec = if (isYouTube) {
            val headers = HashMap(dataSpec.httpRequestHeaders)
            headers["Origin"] = "https://www.youtube.com"
            headers["Referer"] = "https://www.youtube.com/"
            if (uriStr.contains("c=VISIONOS") || uriStr.contains("client=VISIONOS")) {
                headers["User-Agent"] = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/605.1.15"
            }
            dataSpec.buildUpon().setHttpRequestHeaders(headers).build()
        } else {
            dataSpec
        }

        return delegate.open(targetSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return delegate.read(buffer, offset, length)
    }

    override fun getUri(): Uri? {
        return delegate.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return delegate.responseHeaders
    }

    override fun close() {
        delegate.close()
    }
}

/**
 * Factory untuk membuat instance YouTubeAwareDataSource.
 */
@OptIn(UnstableApi::class)
class YouTubeAwareDataSourceFactory(
    private val baseFactory: DataSource.Factory
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return YouTubeAwareDataSource(baseFactory.createDataSource())
    }
}
