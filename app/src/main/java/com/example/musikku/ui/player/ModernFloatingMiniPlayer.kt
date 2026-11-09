package com.example.musikku.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.playback.PlaybackState
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.FavoriteRed
import com.example.musikku.ui.theme.JetPill
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Mini Player Melayang bergaya Liquid Glass / Apple Music (seperti pada Convx):
 * - Kapsul melayang rounded 22dp dengan border kaca tipis.
 * - Horizontal swipe gesture: geser kiri untuk lagu berikutnya, geser kanan untuk lagu sebelumnya.
 * - Smooth Marquee text pada judul dan artis.
 * - Tombol cepat favorit dan play/pause.
 * - Progress bar tipis menyatu di bagian bawah kapsul.
 */
@Composable
fun ModernFloatingMiniPlayer(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    val progress = if (playbackState.duration > 0) {
        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val offsetXAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val swipeSpring = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)
    }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetXAnimatable.snapTo(offsetXAnimatable.value + (delta * 0.85f))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD9163224), // Frosted emerald glass
                        Color(0xCC0E2218)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val currentOffset = offsetXAnimatable.value
                    val threshold = 70f
                    if (abs(currentOffset) > threshold || abs(velocity) > 600f) {
                        if (currentOffset > 0 || velocity > 600f) {
                            onPrevious()
                        } else {
                            onNext()
                        }
                    }
                    coroutineScope.launch {
                        offsetXAnimatable.animateTo(0f, swipeSpring)
                    }
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
            ) {
                // Artwork Squircle (42dp, corner 12dp)
                var isMiniError by remember(song.albumArtUri) { mutableStateOf(false) }
                val showMiniPlaceholder = song.albumArtUri.isBlank() || isMiniError

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(JetPill)
                ) {
                    if (!showMiniPlaceholder) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onError = { isMiniError = true },
                            onSuccess = { isMiniError = false },
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        AlbumArtPlaceholder(
                            seed = song.title,
                            modifier = Modifier.matchParentSize(),
                            iconSize = 20.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Metadata: Judul (Marquee) & Artis + Durasi (Marquee)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${formatDuration(playbackState.currentPosition)} / ${formatDuration(playbackState.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                // Tombol Cepat Favorit
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) FavoriteRed else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Tombol Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Lagu Berikutnya",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Tombol Play / Pause Utama (Bulat Putih Ikon Hitam Sesuai Gambar 1, 3, 4)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPlayPauseClick)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Progress Bar Tipis Elegan di Bagian Bawah Kapsul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF67E8A6).copy(alpha = 0.7f),
                                    Color(0xFF67E8A6)
                                )
                            )
                        )
                )
            }
        }
    }
}
