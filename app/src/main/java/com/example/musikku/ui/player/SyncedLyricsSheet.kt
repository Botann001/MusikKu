package com.example.musikku.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.remote.lyrics.LyricLine
import com.example.musikku.data.remote.lyrics.LyricsUiState
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted
import com.example.musikku.ui.theme.TextWhite
import kotlinx.coroutines.delay

/**
 * Fullscreen Immersive Karaoke-Style Synced Lyrics Sheet (Lirik Berjalan Sinkron)
 * Diberi tenaga oleh LRCLIB API dengan estetika Convx:
 * - Dynamic Ambient Mesh Gradient Background dari sampul album
 * - Auto-scroll aktif mengikuti timestamp lagu yang sedang diputar
 * - Animasi highlight, pembesaran teks (scale 1.08x), dan opacity fokus karaoke
 * - Klik pada baris lirik mana saja untuk langsung melompat (seek)
 * - Tombol floating "Sinkronkan Lirik" bila pengguna menggeser secara manual
 * - Kontrol pemutar mini di bagian bawah agar tetap nyaman bernyanyi
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncedLyricsSheet(
    song: SongEntity,
    lyricsState: LyricsUiState,
    currentPosition: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val meshPalette = rememberArtworkMeshPalette(imageUrl = song.albumArtUri)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
            // ─── 1. Ambient Dynamic Mesh Gradient Background ─────────────────────
            MeshGradientBackground(
                palette = meshPalette,
                trackKey = song.id,
                modifier = Modifier.fillMaxSize()
            )

            // Scrim gelap tipis tambahan agar teks lirik putih tetap kontras maksimal
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxSize()
            ) {}

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp, bottom = 8.dp)
            ) {
                // ─── 2. Header Bar ───────────────────────────────────────────────
                LyricsHeader(
                    song = song,
                    lyricsState = lyricsState,
                    onDismiss = onDismiss
                )

                // ─── 3. Area Konten Lirik ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (lyricsState) {
                        is LyricsUiState.Loading -> {
                            LyricsLoadingView()
                        }

                        is LyricsUiState.Success -> {
                            if (lyricsState.isSynced && lyricsState.lines.isNotEmpty()) {
                                SyncedLyricsListView(
                                    lines = lyricsState.lines,
                                    currentPosition = currentPosition,
                                    onSeekTo = onSeekTo
                                )
                            } else {
                                PlainLyricsView(
                                    plainText = lyricsState.plainLyrics ?: lyricsState.lines.joinToString("\n") { it.text }
                                )
                            }
                        }

                        is LyricsUiState.Empty -> {
                            LyricsEmptyView(
                                song = song,
                                onRetry = onRetry
                            )
                        }

                        is LyricsUiState.Error -> {
                            LyricsErrorView(
                                message = lyricsState.message,
                                onRetry = onRetry
                            )
                        }

                        is LyricsUiState.Idle -> {
                            LyricsLoadingView()
                        }
                    }
                }

                // ─── 4. Bottom Mini Player Bar ────────────────────────────────────
                LyricsBottomPlayerBar(
                    song = song,
                    currentPosition = currentPosition,
                    isPlaying = isPlaying,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeekTo = onSeekTo
                )
            }
        }
    }

/**
 * Header dengan judul lagu, artis, lencana status LRCLIB, dan tombol tutup.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LyricsHeader(
    song: SongEntity,
    lyricsState: LyricsUiState,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Lencana status LRCLIB
                val isSynced = (lyricsState as? LyricsUiState.Success)?.isSynced == true
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSynced) ElectricLime.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isSynced) ElectricLime.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isSynced) ElectricLime else TextWhite.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSynced) "LRCLIB • Sinkron" else "LRCLIB • Teks",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSynced) ElectricLime else TextWhite.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Tombol tutup berbentuk circular glass button
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Lirik",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Tampilan lirik berjalan sinkron (LRC) interaktif karaoke-style.
 */
@Composable
private fun SyncedLyricsListView(
    lines: List<LyricLine>,
    currentPosition: Long,
    onSeekTo: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    // Hitung baris aktif saat ini
    val activeIndex = remember(currentPosition, lines) {
        val idx = lines.indexOfLast { it.timeMs <= currentPosition }
        if (idx >= 0) idx else 0
    }

    // Deteksi jika user scroll manual -> nonaktifkan sementara autoscroll
    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            isAutoScrollEnabled = false
        }
    }

    // Auto-scroll ke baris aktif
    LaunchedEffect(activeIndex, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && lines.isNotEmpty() && activeIndex >= 0) {
            // Taruh baris aktif kira-kira di sepertiga atas layar
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(index = targetScroll)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 40.dp,
                bottom = 120.dp,
                start = 24.dp,
                end = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = lines,
                key = { index, item -> "$index-${item.timeMs}" }
            ) { index, line ->
                val isActive = index == activeIndex
                val distance = kotlin.math.abs(index - activeIndex)

                // Animasi skala dan opacity sesuai kedekatan dengan baris aktif
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.06f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "lyric_scale"
                )

                val alpha by animateFloatAsState(
                    targetValue = when {
                        isActive -> 1.0f
                        distance == 1 -> 0.55f
                        distance == 2 -> 0.35f
                        else -> 0.20f
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "lyric_alpha"
                )

                val textColor = if (isActive) Color.White else Color.White.copy(alpha = 0.85f)
                val textWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold
                val textSize = if (isActive) 23.sp else 19.sp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSeekTo(line.timeMs)
                            isAutoScrollEnabled = true
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = line.text.ifBlank { "•••" },
                        fontSize = textSize,
                        fontWeight = textWeight,
                        color = textColor,
                        lineHeight = if (isActive) 32.sp else 26.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // Tombol Floating "Sinkronkan Lirik" jika auto-scroll sedang nonaktif
        AnimatedVisibility(
            visible = !isAutoScrollEnabled,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Surface(
                onClick = { isAutoScrollEnabled = true },
                shape = CircleShape,
                color = ElectricLime,
                shadowElevation = 8.dp,
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sinkronkan Lirik",
                        tint = OnElectricLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sinkronkan Lirik",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnElectricLime
                    )
                }
            }
        }
    }
}

/**
 * Tampilan lirik teks biasa ketika file LRC bertanda waktu tidak tersedia.
 */
@Composable
private fun PlainLyricsView(plainText: String) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            Text(
                text = plainText,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite.copy(alpha = 0.9f),
                lineHeight = 30.sp
            )
        }
    }
}

/**
 * Indikator Loading berdenyut halus.
 */
@Composable
private fun LyricsLoadingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = ElectricLime,
            modifier = Modifier.size(44.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Mencari lirik di LRCLIB...",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Menyinkronkan bait karaoke",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

/**
 * Tampilan saat lirik tidak ditemukan di LRCLIB.
 */
@Composable
private fun LyricsEmptyView(
    song: SongEntity,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricLime.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lirik Belum Tersedia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Belum ada lirik tersinkronisasi untuk '${song.title}' di database LRCLIB.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                onClick = onRetry,
                shape = CircleShape,
                color = ElectricLime,
                modifier = Modifier.height(42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Coba Lagi",
                        tint = OnElectricLime,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cari Ulang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OnElectricLime
                    )
                }
            }
        }
    }
}

/**
 * Tampilan error ketika jaringan atau fetch bermasalah.
 */
@Composable
private fun LyricsErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gagal Memuat Lirik",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            onClick = onRetry,
            shape = CircleShape,
            color = ElectricLime
        ) {
            Text(
                text = "Coba Lagi",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                fontWeight = FontWeight.Bold,
                color = OnElectricLime
            )
        }
    }
}

/**
 * Mini Player di bagian bawah tampilan lirik agar pengguna tetap bisa mengontrol lagu.
 */
@Composable
private fun LyricsBottomPlayerBar(
    song: SongEntity,
    currentPosition: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val duration = song.duration.coerceAtLeast(1L)
    val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Black.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Slider Progres
            Slider(
                value = progress,
                onValueChange = { newProg ->
                    val targetMs = (newProg * duration).toLong()
                    onSeekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricLime,
                    activeTrackColor = ElectricLime,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 11.sp,
                    color = TextMuted
                )

                // Tombol Kontrol: Prev, Play/Pause, Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Sebelumnya",
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = ElectricLime,
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = OnElectricLime,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Berikutnya",
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = formatTime(duration),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
