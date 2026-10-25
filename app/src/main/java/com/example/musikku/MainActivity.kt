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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            SettingsRepository(newBase).appLanguage.first()
        }
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, language))
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
        enableEdgeToEdge()

        // Proteksi Anti-Spyware: Cegah serangan Tapjacking melalui overlay berbahaya
        window.decorView.filterTouchesWhenObscured = true

        checkAndRequestPermissions()

        setContent {
            val themeMode by appContainer.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            val appLanguage by appContainer.settingsRepository.appLanguage
                .collectAsStateWithLifecycle(initialValue = null)

            var initialLanguage by remember { mutableStateOf<AppLanguage?>(null) }

            LaunchedEffect(appLanguage) {
                if (appLanguage != null) {
                    if (initialLanguage == null) {
                        initialLanguage = appLanguage
                    } else if (initialLanguage != appLanguage) {
                        recreate()
                    }
                }
            }

            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1400L)
                showSplash = false
            }

            MusikKuTheme(themeMode = themeMode) {
                val viewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        repository = appContainer.musicRepository,
                        jamendoRepository = appContainer.jamendoRepository,
                        youTubeMusicRepository = appContainer.youTubeMusicRepository,
                        lyricsRepository = appContainer.lyricsRepository,
                        settingsRepository = appContainer.settingsRepository,
                        networkMonitor = appContainer.networkMonitor,
                        musicController = appContainer.musicController
                    )
                )

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