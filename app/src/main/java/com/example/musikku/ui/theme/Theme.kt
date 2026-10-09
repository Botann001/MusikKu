package com.example.musikku.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.musikku.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = ElectricLime,
    onPrimary = OnElectricLime,
    primaryContainer = ElectricLimeContainer,
    onPrimaryContainer = ElectricLime,
    secondary = ElectricLimeBright,
    onSecondary = OnElectricLime,
    secondaryContainer = JetCardHigh,
    onSecondaryContainer = ElectricLime,
    tertiary = ElectricLimeDim,
    onTertiary = OnElectricLime,
    background = JetBlack,
    onBackground = TextWhite,
    surface = JetBlack,
    onSurface = TextWhite,
    surfaceVariant = JetCard,
    onSurfaceVariant = TextMuted,
    surfaceContainerHigh = JetCardHigh,
    outline = JetBorder,
    outlineVariant = Color(0xFF222522),
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEF6DB),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF286643),
    onSecondaryContainer = Color(0xFF072111),
    secondaryContainer = Color(0xFFD4EBDC),
    tertiary = EmeraldSpotify,
    background = EmeraldLightBg,
    onBackground = TextLightPrimary,
    surface = EmeraldLightSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = EmeraldLightSurfaceVariant,
    onSurfaceVariant = TextLightSecondary,
    surfaceContainerHigh = EmeraldLightCardHigh,
    outline = EmeraldLightBorder,
    outlineVariant = Color(0xFFD8E4DC),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MusikKuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}