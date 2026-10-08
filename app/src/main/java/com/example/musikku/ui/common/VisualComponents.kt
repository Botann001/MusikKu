package com.example.musikku.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Animasi 3-bar equalizer bergerak naik-turun saat lagu aktif diputar.
 * Jika musik di-pause, batang berhenti di posisi istirahat (resting height).
 */
@Composable
fun LiveEqualizerIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    maxHeight: Dp = 16.dp,
    minHeight: Dp = 4.dp,
    barWidth: Dp = 3.dp,
    spacing: Dp = 2.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizer_pulse")

    val bar1Height by transition.animateFloat(
        initialValue = if (isPlaying) 0.3f else 0.45f,
        targetValue = if (isPlaying) 1.0f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = if (isPlaying) 0.85f else 0.65f,
        targetValue = if (isPlaying) 0.2f else 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = if (isPlaying) 0.25f else 0.5f,
        targetValue = if (isPlaying) 0.95f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val heights = listOf(bar1Height, bar2Height, bar3Height)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val fraction = heights[i % heights.size]
            val currentHeight = minHeight + (maxHeight - minHeight) * fraction
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(currentHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(tint)
            )
        }
    }
}

/**
 * Menghasilkan gradien warna artistik unik berdasarkan string seed (misal judul lagu).
 * Mencegah tampilan cover lagu polosan kotak abu-abu mati.
 */
fun getArtGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF00E676), Color(0xFF004D20)),
        listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
        listOf(Color(0xFFF38181), Color(0xFFFCE38A)),
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFFF5E62), Color(0xFFFF9966)),
        listOf(Color(0xFF00F260), Color(0xFF0575E6)),
        listOf(Color(0xFFB92B27), Color(0xFF1565C0)),
        listOf(Color(0xFF3A1C71), Color(0xFFD76D77))
    )
    val index = abs(seed.hashCode()) % palettes.size
    return Brush.linearGradient(palettes[index])
}

/**
 * Placeholder cover album dengan gradien cerah dan ikon musik bernuansa modern.
 */
@Composable
fun AlbumArtPlaceholder(
    seed: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(getArtGradient(seed))
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(iconSize)
        )
    }
}
