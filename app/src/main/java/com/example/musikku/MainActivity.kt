package com.example.musikku

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musikku.ui.library.LibraryScreen
import com.example.musikku.ui.library.LibraryViewModel
import com.example.musikku.ui.theme.MusikKuTheme

import android.content.Context
import android.content.res.Configuration
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.data.preferences.AppLanguage
import com.example.musikku.data.preferences.SettingsRepository
import com.example.musikku.data.preferences.ThemeMode
import com.example.musikku.ui.splash.SplashScreen
import com.example.musikku.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var mainViewModel: LibraryViewModel? = null
    private var customResources: android.content.res.Resources? = null

    fun updateCustomConfiguration(config: Configuration) {
        try {
            customResources = createConfigurationContext(config).resources
        } catch (_: Exception) {}
    }

    override fun getResources(): android.content.res.Resources {
        return customResources ?: super.getResources()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleSpotifyIntent(intent)
    }

    private fun handleSpotifyIntent(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "musikku" && uri.host == "spotify-callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                mainViewModel?.handleSpotifyAuthCallback(code)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            SettingsRepository(newBase).appLanguage.first()
        }
        val localizedContext = LocaleHelper.createLocalizedContext(newBase, language)
        val config = Configuration(localizedContext.resources.configuration)
        try {
            applyOverrideConfiguration(config)
        } catch (_: Exception) {}
        super.attachBaseContext(localizedContext)
    }

    /**
     * Launcher untuk meminta izin audio dan notifikasi secara bersamaan.
     */
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Konfigurasi awal Edge-to-Edge transparan
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        // Proteksi Anti-Spyware: Cegah serangan Tapjacking melalui overlay berbahaya
        window.decorView.filterTouchesWhenObscured = true

        checkAndRequestPermissions()

        setContent {
            val themeMode by appContainer.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Update warna dan kontras Navigation Bar & Status Bar secara dinamis agar 100% transparan
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
                onDispose {}
            }

            val appLanguage by appContainer.settingsRepository.appLanguage
                .collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)

            // Terapkan ke Android 13+ LocaleManager & context resources saat bahasa dipilih
            LaunchedEffect(appLanguage) {
                LocaleHelper.applyLocaleToSystem(this@MainActivity, appLanguage)
            }

            val targetLocale = remember(appLanguage) {
                LocaleHelper.resolveLocale(appLanguage)
            }
            val baseConfig = LocalConfiguration.current
            val localizedConfig = remember(baseConfig, targetLocale) {
                Configuration(baseConfig).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
            }
            DisposableEffect(localizedConfig) {
                updateCustomConfiguration(localizedConfig)
                onDispose {}
            }

            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1400L)
                showSplash = false
                focusManager.clearFocus(force = true)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                MusikKuTheme(themeMode = themeMode) {
                    val viewModel: LibraryViewModel = viewModel(
                        factory = LibraryViewModel.Factory(
                            repository = appContainer.musicRepository,
                            jamendoRepository = appContainer.jamendoRepository,
                            youTubeMusicRepository = appContainer.youTubeMusicRepository,
                            lyricsRepository = appContainer.lyricsRepository,
                            settingsRepository = appContainer.settingsRepository,
                            networkMonitor = appContainer.networkMonitor,
                            musicController = appContainer.musicController,
                            listeningStatsRepository = appContainer.listeningStatsRepository,
                            spotifyRepository = appContainer.spotifyRepository
                        )
                    )

                    LaunchedEffect(viewModel) {
                        mainViewModel = viewModel
                        handleSpotifyIntent(intent)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LibraryScreen(viewModel = viewModel)

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showSplash,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )
                        ) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}