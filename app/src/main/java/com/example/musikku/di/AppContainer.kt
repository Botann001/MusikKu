package com.example.musikku.di

import android.content.Context
import androidx.room.Room
import com.example.musikku.data.local.AppDatabase
import com.example.musikku.data.remote.JamendoApiService
import com.example.musikku.data.repository.JamendoRepository
import com.example.musikku.data.repository.MusicRepository
import com.example.musikku.data.scanner.MusicScanner
import com.example.musikku.playback.MusicController
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Dependency injection manual — satu instance per aplikasi.
 * Dibuat di MusikKuApp.onCreate(), diakses via extension property.
 */
class AppContainer(context: Context) {

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "musikku.db"
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_1_4,
            AppDatabase.MIGRATION_2_4,
            AppDatabase.MIGRATION_1_5,
            AppDatabase.MIGRATION_2_5,
            AppDatabase.MIGRATION_3_5
        )
        .build()

    val settingsRepository = com.example.musikku.data.preferences.SettingsRepository(context.applicationContext)

    val networkMonitor = com.example.musikku.util.NetworkMonitor(context.applicationContext)

    private val musicScanner = MusicScanner(context.applicationContext)

    val musicRepository = MusicRepository(
        songDao = database.songDao(),
        favoriteDao = database.favoriteDao(),
        playlistDao = database.playlistDao(),
        musicScanner = musicScanner
    )

    /** Alias songRepository sesuai kontrak spesifikasi */
    val songRepository: MusicRepository get() = musicRepository

    val musicController = MusicController(context.applicationContext)
    /** Alias playerController sesuai kontrak spesifikasi */
    val playerController: MusicController get() = musicController

    // Konfigurasi OkHttpClient dengan User-Agent untuk akses streaming dan API
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    // Konfigurasi Retrofit untuk Jamendo API
    private val retrofit = Retrofit.Builder()
        .baseUrl(JamendoApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val jamendoApiService: JamendoApiService = retrofit.create(JamendoApiService::class.java)

    val jamendoRepository = JamendoRepository(
        context = context.applicationContext,
        apiService = jamendoApiService,
        songDao = database.songDao(),
        settingsRepository = settingsRepository,
        clientId = com.example.musikku.BuildConfig.JAMENDO_CLIENT_ID
    )

    // Konfigurasi Retrofit untuk Invidious / YouTube Music API
    val youTubeApiService: com.example.musikku.data.remote.youtube.YouTubeApiService = Retrofit.Builder()
        .baseUrl("https://invidious.f5.si/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(com.example.musikku.data.remote.youtube.YouTubeApiService::class.java)

    val youTubeMusicRepository = com.example.musikku.data.repository.YouTubeMusicRepository(
        context = context.applicationContext,
        apiService = youTubeApiService,
        songDao = database.songDao()
    )
}
