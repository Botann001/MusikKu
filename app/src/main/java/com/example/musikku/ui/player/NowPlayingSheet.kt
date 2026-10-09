package com.example.musikku.ui.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
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
    onRemoveAlbumCover: (Long) -> Unit = {}
) {
    val song = playbackState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showCoverOptionsDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var isImageError by remember(song.albumArtUri) { mutableStateOf(false) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JetBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
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
                // Tombol Tutup (Lingkaran Gelap)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1E1C),
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
                    val sourceText = if (song.source == "JAMENDO") "Jelajah · Jamendo"
                                     else if (song.source == "DOWNLOADED") "Library · Terunduh"
                                     else "Library · Semua Lagu"
                    Text(
                        text = sourceText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Tombol Antrean (Lingkaran Gelap)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1E1C),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { showQueue = true }, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Buka Antrean",
                            tint = if (showQueue) ElectricLime else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── 2. Cover Album Besar dengan Ambient Glow & Tombol "+ Tambah cover" ─
            val hasCustomCover = song.albumArtUri.startsWith("file://")
            val isBlankOrError = song.albumArtUri.isBlank() || isImageError

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                // Ambient Glow di belakang cover
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .shadow(
                            elevation = 42.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = ElectricLime.copy(alpha = 0.35f),
                            spotColor = ElectricLime.copy(alpha = 0.35f)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricLime.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                )

                // Kartu Cover Utama
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(JetCard)
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
                        AlbumArtPlaceholder(
                            seed = song.title,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 64.dp,
                            showConcentricRings = true
                        )
                    }

                    // Tombol "+ Tambah cover" di pojok kiri bawah (Sesuai Image 2)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clickable {
                                if (hasCustomCover) showCoverOptionsDialog = true
                                else pickImageLauncher.launch("image/*")
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tambah cover",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── 3. Judul Lagu, Artis & Tombol Favorit ──────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val sourceBadge = when (song.source) {
                        "DOWNLOADED" -> "Terunduh"
                        "JAMENDO" -> "Jamendo Online"
                        else -> "Penyimpanan Lokal"
                    }
                    Text(
                        text = "${song.artist} · $sourceBadge",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tombol Hati (Favorit) Berbentuk Lingkaran Gelap
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1C1E1C),
                    border = BorderStroke(1.dp, Color(0xFF282B28)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                            tint = if (isFavorite) FavoriteRed else Color(0xFF8E908E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── 4. Seekbar / Slider dengan Garis Lime & Waktu ─────────────────────
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
                        activeTrackColor = ElectricLime,
                        inactiveTrackColor = Color(0xFF2E312E)
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
                        color = TextMuted
                    )
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─── 5. Kontrol Pemutar Musik (Shuffle, Prev, Big Lime Play, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak Lagu",
                        tint = if (playbackState.isShuffleEnabled) ElectricLime else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Lagu Sebelumnya",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Tombol Play/Pause Besar Warna Neon Lime Bercahaya
                Surface(
                    shape = CircleShape,
                    color = ElectricLime,
                    shadowElevation = 16.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                            tint = OnElectricLime,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Lagu Berikutnya",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat, modifier = Modifier.size(44.dp)) {
                    val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val repeatTint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) ElectricLime else Color.White
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Ulangi Lagu",
                        tint = repeatTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── 6. Baris Tiga Tombol Pill (Timer Tidur, Lirik, Offline) ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Spacer(modifier = Modifier.width(8.dp))

                // Pill 2: Lirik
                NowPlayingActionPill(
                    icon = Icons.Default.GraphicEq,
                    label = "Lirik",
                    isActive = false,
                    onClick = { showLyricsDialog = true },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Pill 3: Offline
                NowPlayingActionPill(
                    icon = Icons.Default.Download,
                    label = "Offline",
                    isActive = song.source == "DOWNLOADED" || song.source == "LOCAL",
                    onClick = { },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // ─── Bottom Sheet Antrean Lagu (QueueSheet) ──────────────────────────────
    if (showQueue) {
        QueueSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            onDismiss = { showQueue = false },
            onPlayQueueItem = onPlayQueueItem,
            onRemoveQueueItem = onRemoveQueueItem,
            onMoveQueueItem = onMoveQueueItem
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

    // ─── Dialog Info Lirik ───────────────────────────────────────────────────
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lirik Lagu", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Fitur sinkronisasi lirik otomatis akan tersedia di pembaruan berikutnya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsDialog = false }) {
                    Text("Tutup", color = ElectricLime)
                }
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
        shape = RoundedCornerShape(50),
        color = if (isActive) ElectricLimeContainer else Color(0xFF1A1C1A),
        border = BorderStroke(1.dp, if (isActive) ElectricLime.copy(alpha = 0.5f) else Color(0xFF282B28)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) ElectricLime else Color(0xFFCCCCCC),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isActive) ElectricLime else Color(0xFFCCCCCC),
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
private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
