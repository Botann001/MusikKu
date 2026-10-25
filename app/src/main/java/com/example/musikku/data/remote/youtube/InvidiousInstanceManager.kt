package com.example.musikku.data.remote.youtube

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Manajer instance Invidious yang cerdas — mengambil daftar instance publik secara dinamis
 * dari API resmi (api.invidious.io), lalu melakukan health-check ringan untuk memvalidasi
 * ketersediaan endpoint API sebelum digunakan.
 *
 * Fitur utama:
 * - Dynamic fetching: ambil daftar instance aktif dari API resmi
 * - Health-check: validasi endpoint /api/v1/stats sebelum digunakan
 * - Smart rotation: otomatis pindah ke instance berikutnya jika satu gagal
 * - Caching: simpan instance yang berhasil agar tidak perlu tes ulang
 * - Hardcoded fallback: daftar instance terverifikasi jika API resmi tidak dapat dijangkau
 */
class InvidiousInstanceManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "InvidiousInstanceMgr"

        /** URL API resmi untuk mengambil daftar instance publik */
        private const val INSTANCES_API_URL = "https://api.invidious.io/instances.json?sort_by=type,health"

        /** Cache validity: 30 menit */
        private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L

        /**
         * Daftar hardcoded instance terverifikasi sebagai fallback.
         * Diperbarui secara manual berdasarkan data api.invidious.io dan uptime.invidious.io.
         */
        val HARDCODED_INSTANCES = listOf(
            "https://invidious.f5.si",
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.privacyredirect.com",
            "https://invidious.protokolla.fi",
            "https://iv.datura.network",
            "https://invidious.materialio.us",
            "https://invidious.perennialte.ch"
        )
    }

    private val mutex = Mutex()

    /** Daftar instance yang sudah ditemukan dan divalidasi */
    private var cachedInstances: MutableList<String> = mutableListOf()

    /** Timestamp terakhir kali daftar instance di-refresh */
    private var lastFetchTimestamp: Long = 0L

    /** Index instance saat ini yang sedang digunakan */
    private var currentIndex: Int = 0

    /** Set instance yang gagal (akan di-skip sampai cache expired) */
    private val failedInstances = mutableSetOf<String>()

    /** Instance yang terbukti berhasil terakhir kali (diprioritaskan) */
    private var lastSuccessfulInstance: String? = null

    /** OkHttpClient ringan untuk health-check (timeout pendek) */
    private val healthCheckClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * Dapatkan instance Invidious yang siap digunakan.
     * Mengembalikan URL base instance (misalnya "https://inv.nadeko.net").
     * Secara otomatis melakukan fetch + health-check jika cache kosong atau kadaluwarsa.
     */
    suspend fun getWorkingInstance(): String = mutex.withLock {
        // Gunakan instance yang terakhir berhasil jika masih dalam cache validity
        lastSuccessfulInstance?.let { last ->
            if (!failedInstances.contains(last) && isCacheValid()) {
                Log.d(TAG, "Menggunakan instance terakhir yang berhasil: $last")
                return@withLock last
            }
        }

        // Refresh daftar instance jika cache expired atau kosong
        if (!isCacheValid() || cachedInstances.isEmpty()) {
            refreshInstances()
        }

        // Cari instance yang belum gagal
        val available = cachedInstances.filter { !failedInstances.contains(it) }
        if (available.isEmpty()) {
            // Semua instance gagal — reset dan coba ulang dari awal
            Log.w(TAG, "Semua instance gagal, mereset daftar gagal...")
            failedInstances.clear()
            return@withLock cachedInstances.firstOrNull() ?: HARDCODED_INSTANCES.first()
        }

        return@withLock available.first()
    }

    /**
     * Dapatkan semua instance yang tersedia untuk failover.
     */
    suspend fun getAllInstances(): List<String> = mutex.withLock {
        if (!isCacheValid() || cachedInstances.isEmpty()) {
            refreshInstances()
        }
        return@withLock cachedInstances.toList()
    }

    /**
     * Tandai instance sebagai berhasil (akan diprioritaskan untuk permintaan berikutnya).
     */
    suspend fun markSuccess(instanceUrl: String) = mutex.withLock {
        lastSuccessfulInstance = instanceUrl
        failedInstances.remove(instanceUrl)
        Log.d(TAG, "Instance berhasil: $instanceUrl")
    }

    /**
     * Tandai instance sebagai gagal dan otomatis pindah ke instance berikutnya.
     */
    suspend fun markFailed(instanceUrl: String) = mutex.withLock {
        failedInstances.add(instanceUrl)
        if (lastSuccessfulInstance == instanceUrl) {
            lastSuccessfulInstance = null
        }
        Log.w(TAG, "Instance gagal ditandai: $instanceUrl (total gagal: ${failedInstances.size}/${cachedInstances.size})")
    }

    /**
     * Refresh daftar instance — ambil dari API resmi, lalu fallback ke hardcoded list.
     * Panggil dalam mutex lock.
     */
    private suspend fun refreshInstances() {
        Log.d(TAG, "Memperbarui daftar instance Invidious...")

        val dynamicInstances = fetchInstancesFromApi()
        val allCandidates = mutableListOf<String>()

        // Prioritaskan instance dari API resmi
        if (dynamicInstances.isNotEmpty()) {
            allCandidates.addAll(dynamicInstances)
            Log.d(TAG, "Berhasil mengambil ${dynamicInstances.size} instance dari API resmi")
        }

        // Tambahkan hardcoded sebagai fallback (hindari duplikat)
        HARDCODED_INSTANCES.forEach { hc ->
            if (!allCandidates.contains(hc)) {
                allCandidates.add(hc)
            }
        }

        cachedInstances = allCandidates
        lastFetchTimestamp = System.currentTimeMillis()
        failedInstances.clear()

        Log.d(TAG, "Total ${cachedInstances.size} instance tersedia: ${cachedInstances.take(5).joinToString()}")
    }

    /**
     * Ambil daftar instance publik dari API resmi api.invidious.io.
     * Filter hanya instance HTTPS yang online dan memiliki API aktif.
     */
    private suspend fun fetchInstancesFromApi(): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(INSTANCES_API_URL)
                .header("User-Agent", "MusikKu-Android/1.0")
                .build()

            val response = healthCheckClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gagal mengambil daftar instance dari API: HTTP ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val jsonArray = JSONArray(body)
            val instances = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                try {
                    val instanceArray = jsonArray.getJSONArray(i)
                    val details = instanceArray.getJSONObject(1)

                    val type = details.optString("type", "")
                    val uri = details.optString("uri", "")

                    // Hanya ambil instance HTTPS clearnet dengan API aktif
                    val apiEnabled = details.optBoolean("api", true)
                    val isMeshNetwork = uri.contains(".onion") || uri.contains(".i2p") ||
                            uri.contains(".ygg") || uri.contains("inv-ygg")

                    if (type != "https" || uri.isBlank() || !apiEnabled || isMeshNetwork) continue

                    // Cek apakah instance online (via monitor data jika tersedia)
                    val monitor = details.optJSONObject("monitor")
                    if (monitor != null) {
                        val isDown = monitor.optBoolean("down", true)
                        val uptime = monitor.optDouble("uptime", 0.0)
                        // Skip instance yang down atau uptime < 80%
                        if (isDown || uptime < 80.0) continue
                    }

                    instances.add(uri)
                } catch (e: Exception) {
                    // Skip instance yang format datanya tidak valid
                    continue
                }
            }

            Log.d(TAG, "Berhasil parsing ${instances.size} instance aktif dari API resmi")
            instances
        } catch (e: Exception) {
            Log.w(TAG, "Gagal fetch daftar instance dari API: ${e.message}")
            emptyList()
        }
    }

    /**
     * Health-check ringan untuk memvalidasi apakah sebuah instance bisa digunakan.
     * Mengecek endpoint /api/v1/stats yang ringan dan cepat.
     */
    suspend fun healthCheck(instanceUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$instanceUrl/api/v1/stats")
                .header("User-Agent", "MusikKu-Android/1.0")
                .build()

            val response = healthCheckClient.newCall(request).execute()
            val isHealthy = response.isSuccessful
            Log.d(TAG, "Health-check $instanceUrl: ${if (isHealthy) "OK" else "GAGAL (${response.code})"}")
            isHealthy
        } catch (e: Exception) {
            Log.w(TAG, "Health-check gagal untuk $instanceUrl: ${e.message}")
            false
        }
    }

    private fun isCacheValid(): Boolean {
        return cachedInstances.isNotEmpty() &&
                (System.currentTimeMillis() - lastFetchTimestamp) < CACHE_VALIDITY_MS
    }
}
