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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musikku.data.preferences.ThemeMode
import com.example.musikku.ui.splash.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

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

            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1000L)
                showSplash = false
            }

            MusikKuTheme(themeMode = themeMode) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    val viewModel: LibraryViewModel = viewModel(
                        factory = LibraryViewModel.Factory(
                            repository = appContainer.musicRepository,
                            jamendoRepository = appContainer.jamendoRepository,
                            settingsRepository = appContainer.settingsRepository,
                            networkMonitor = appContainer.networkMonitor,
                            musicController = appContainer.musicController
                        )
                    )
                    LibraryScreen(viewModel = viewModel)
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