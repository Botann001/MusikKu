package com.example.musikku.ui.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.playback.PlaybackState
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.ElectricLimeContainer
import com.example.musikku.ui.theme.FavoriteRed
import com.example.musikku.ui.theme.JetBlack
import com.example.musikku.ui.theme.JetCard
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onSetSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onUpdateAlbumCover: (Long, Uri) -> Unit = { _, _ -> },
    onRemoveAlbumCover: (Long) -> Unit = {},
    onDeleteSong: (SongEntity) -> Unit = {},
    onDownload: () -> Unit = {},
    lyricsState: com.example.musikku.data.remote.lyrics.LyricsUiState = com.example.musikku.data.remote.lyrics.LyricsUiState.Idle,
    onRetryLyrics: () -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var isImageError by remember(song.albumArtUri) { mutableStateOf(false) }

    BackHandler(enabled = showLyricsDialog) {
        showLyricsDialog = false
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateAlbumCover(song.id, uri)
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isDraggingSlider) {
        draggedPosition.toLong()
    } else {
        playbackState.currentPosition
    }

    val duration = playbackState.duration.coerceAtLeast(1L)
    val sliderValue = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    val meshPalette = rememberArtworkMeshPalette(song.albumArtUri)
    val ambientCoverColor = remember(meshPalette) {
        meshPalette.colors.firstOrNull()?.tuned() ?: ElectricLime
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
            // Layer 1: Ambient Mesh Gradient Background ala Convx / Apple Music
            MeshGradientBackground(
                palette = meshPalette,
                trackKey = song.id
            )

            // Layer 2: Konten Pemutar atau Lirik Berjalan
            if (!showLyricsDialog) {
                Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 10.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ─── 1. Header: Chevron Down, "MEMUTAR DARI", Queue ─────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tombol Tutup (Lingkaran Glass Sesuai Image 5)
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Tutup Layar Pemutar",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Judul Tengah: MEMUTAR DARI
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MEMUTAR DARI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sourceText = if (song.source == "YOUTUBE") "Jelajah · YouTube Music"
                                         else if (song.source == "JAMENDO") "Jelajah · Jamendo"
                                         else if (song.source == "DOWNLOADED") "Library · Terunduh"
                                         else "Library · Semua Lagu"
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Tombol Antrean (Lingkaran Glass Sesuai Image 5)
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = { showQueue = true }, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Buka Antrean",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ─── 2. Cover Album Besar Sesuai Image 5 ─────────────────────────
                val hasCustomCover = song.albumArtUri.startsWith("file://")
                val isBlankOrError = song.albumArtUri.isBlank() || isImageError

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 12.dp)
                ) {
                    // Ambient Glow di belakang cover (warna dinamis dari artwork)
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ambientCoverColor.copy(alpha = 0.50f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                    )

                    // Kartu Cover Utama Squircle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(270.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF26B06E), Color(0xFF147547))
                                )
                            )
                            .clickable {
                                if (hasCustomCover) showCoverOptionsDialog = true
                                else pickImageLauncher.launch("image/*")
                            }
                    ) {
                        if (!isBlankOrError) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = "Cover Album ${song.album}",
                                contentScale = ContentScale.Crop,
                                onError = { isImageError = true },
                                onSuccess = { isImageError = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(88.dp)
                                )
                            }
                        }
                    }
                }

                // ─── 3. Kartu Kaca Frosted Bawah (Container Card Sesuai Image 5) ────────
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xD910241A),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        // Judul Lagu, Artis & Tombol Favorit Glass Sesuai Image 5
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val sourceBadge = when (song.source) {
                                    "YOUTUBE" -> "YouTube Music"
                                    "DOWNLOADED" -> "Terunduh"
                                    "JAMENDO" -> "Jamendo"
                                    else -> "Terunduh"
                                }
                                Text(
                                    text = "${song.artist} · $sourceBadge",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Tombol Hati Lingkaran Glass Sesuai Image 5
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                modifier = Modifier.size(46.dp)
                            ) {
                                IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                                        tint = if (isFavorite) FavoriteRed else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Seekbar / Slider Mint Green Sesuai Image 5
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { fraction ->
                                    isDraggingSlider = true
                                    draggedPosition = fraction * duration
                                },
                                onValueChangeFinished = {
                                    onSeekTo(draggedPosition.toLong())
                                    isDraggingSlider = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF67E8A6),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(currentPosition),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatDuration(duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Kontrol Pemutar Musik (Shuffle, Prev, Giant Mint Play, Next, Repeat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(42.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Acak Lagu",
                                    tint = if (playbackState.isShuffleEnabled) Color(0xFF67E8A6) else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Lagu Sebelumnya",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            // Tombol Play/Pause Besar Warna Mint Green Bercahaya (Sesuai Image 5)
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF67E8A6),
                                shadowElevation = 14.dp,
                                modifier = Modifier.size(72.dp)
                            ) {
                                IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                                        tint = Color.Black,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Lagu Berikutnya",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(42.dp)) {
                                val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                                val repeatTint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) Color(0xFF67E8A6) else Color.White.copy(alpha = 0.8f)
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = "Ulangi Lagu",
                                    tint = repeatTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Baris Tiga Tombol Pill (Timer Tidur, Lirik, Offline) Sesuai Image 5
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pill 1: Timer tidur
                            NowPlayingActionPill(
                                icon = Icons.Default.Bedtime,
                                label = "Timer tidur",
                                isActive = playbackState.sleepTimerRemainingSeconds > 0,
                                onClick = { showSleepTimerDialog = true },
                                modifier = Modifier.weight(1f)
                            )

                            // Pill 2: Lirik
                            val hasLyrics = (lyricsState as? com.example.musikku.data.remote.lyrics.LyricsUiState.Success) != null
                            NowPlayingActionPill(
                                icon = Icons.Default.GraphicEq,
                                label = "Lirik",
                                isActive = hasLyrics,
                                onClick = { showLyricsDialog = true },
                                modifier = Modifier.weight(1f)
                            )

                            // Pill 3: Offline / Download
                            val isDownloadedOrLocal = song.source == "DOWNLOADED" || song.source == "LOCAL"
                            NowPlayingActionPill(
                                icon = if (isDownloadedOrLocal) Icons.Default.DownloadDone else Icons.Default.Download,
                                label = if (isDownloadedOrLocal) "Offline" else "Download",
                                isActive = isDownloadedOrLocal,
                                onClick = {
                                    if (!isDownloadedOrLocal) {
                                        onDownload()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }
    } else {
        SyncedLyricsSheet(
            song = song,
            lyricsState = lyricsState,
            currentPosition = currentPosition,
            isPlaying = playbackState.isPlaying,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeekTo = onSeekTo,
            onRetry = onRetryLyrics,
            onDismiss = { showLyricsDialog = false }
        )
    }
}
}

    // ─── Bottom Sheet Antrean Lagu (QueueSheet) ──────────────────────────────
    if (showQueue) {
        QueueSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            currentPosition = currentPosition,
            duration = duration,
            onDismiss = { showQueue = false },
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onMoveQueueItem = onMoveQueueItem,
            onShuffleQueue = onToggleShuffle,
            onClearQueue = {
                for (i in (playbackState.queue.size - 1) downTo (playbackState.currentIndex + 1)) {
                    onRemoveQueueItem(i)
                }
            }
        )
    }

    // ─── Dialog Opsi Sampul Album (Ganti / Hapus) ────────────────────────────
    if (showCoverOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showCoverOptionsDialog = false },
            title = { Text("Sampul Album Kustom", fontWeight = FontWeight.Bold) },
            text = { Text("Pilih tindakan untuk sampul album lagu '${song.title}':") },
            confirmButton = {
                TextButton(onClick = {
                    showCoverOptionsDialog = false
                    pickImageLauncher.launch("image/*")
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ganti Foto")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showCoverOptionsDialog = false
                        onRemoveAlbumCover(song.id)
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showCoverOptionsDialog = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    // ─── Dialog Pengaturan Sleep Timer ───────────────────────────────────────
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingSeconds = playbackState.sleepTimerRemainingSeconds,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { minutes ->
                onSetSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                onCancelSleepTimer()
                showSleepTimerDialog = false
            }
        )
    }
}

@Composable
private fun NowPlayingActionPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isActive) Color(0x381C4830) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (isActive) Color(0xFF67E8A6).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.14f)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF67E8A6) else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color(0xFF67E8A6) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SleepTimerDialog(
    remainingSeconds: Long,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val options = listOf(15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Sleep Timer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remainingSeconds > 0) {
                    val min = remainingSeconds / 60
                    val sec = remainingSeconds % 60
                    Text(
                        text = "Timer aktif: %02d:%02d tersisa".format(min, sec),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricLime
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text(
                        text = "Pilih durasi waktu sebelum musik berhenti otomatis:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                options.forEach { minutes ->
                    TextButton(
                        onClick = { onSetTimer(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$minutes Menit",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color = Color.White
                        )
                    }
                }

                if (remainingSeconds > 0) {
                    TextButton(
                        onClick = onCancelTimer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Matikan Timer",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = ElectricLime)
            }
        }
    )
}

/** Format milidetik ke teks "m:ss" atau "h:mm:ss". */
internal fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
