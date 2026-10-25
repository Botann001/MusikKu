package com.example.musikku.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.common.LiveEqualizerIndicator
import com.example.musikku.ui.theme.MintGreen
import com.example.musikku.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<SongEntity>,
    currentIndex: Int,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    onDismiss: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onShuffleQueue: () -> Unit = {},
    onClearQueue: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSong = queue.getOrNull(currentIndex)
    val meshPalette = rememberArtworkMeshPalette(currentSong?.albumArtUri)

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
            // Layer 1: Ambient Mesh Gradient Background Sesuai Image 2
            MeshGradientBackground(
                palette = meshPalette,
                trackKey = currentSong?.id
            )

            // Scrim lembut
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxSize()
            ) {}

            // Layer 2: Konten Antrean
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // ─── Header: Tombol Back Bulat, Judul, Tombol Trash Bulat ───────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    // Tombol Back Lingkaran Glass
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup Antrean",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Judul Tengah: Antrean & Sumber
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.player_queue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sourceText = if (currentSong?.source == "YOUTUBE") "Dari Jelajah · YouTube Music"
                                         else if (currentSong?.source == "JAMENDO") "Dari Jelajah · Jamendo"
                                         else if (currentSong?.source == "DOWNLOADED") "Dari Library · Terunduh"
                                         else "Dari Library · Semua Lagu"
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }

                    // Tombol Hapus Semua / Trash Lingkaran Glass
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onClearQueue, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Bersihkan Antrean",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ─── Info Banner Sesuai Image 2 ──────────────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Seret gagang untuk mengubah urutan. Geser ke kiri untuk menghapus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                    }
                }

                // ─── List Antrean ────────────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ─── Section 1: SEDANG DIPUTAR ───────────────────────────────
                    if (currentSong != null) {
                        item(key = "now_playing_section_header") {
                            Text(
                                text = "SEDANG DIPUTAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.4.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }

                        item(key = "current_playing_card") {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x381C4830)
                                ),
                                border = BorderStroke(1.5.dp, MintGreen.copy(alpha = 0.85f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    // Artwork Squircle
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF163224))
                                    ) {
                                        if (currentSong.albumArtUri.isNotBlank()) {
                                            AsyncImage(
                                                model = currentSong.albumArtUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.matchParentSize()
                                            )
                                        } else {
                                            AlbumArtPlaceholder(
                                                seed = currentSong.title,
                                                modifier = Modifier.matchParentSize(),
                                                iconSize = 24.dp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSong.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MintGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        val timeStr = if (duration > 0) {
                                            "${currentSong.artist} · ${formatDuration(currentPosition)} / ${formatDuration(duration)}"
                                        } else {
                                            "${currentSong.artist} · ${formatDuration(currentSong.duration)}"
                                        }
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Live Equalizer Indicator
                                    Box(modifier = Modifier.padding(end = 4.dp)) {
                                        LiveEqualizerIndicator(
                                            isPlaying = true,
                                            tint = MintGreen,
                                            maxHeight = 18.dp,
                                            barWidth = 3.dp,
                                            spacing = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ─── Section 2: BERIKUTNYA ───────────────────────────────────
                    val upcomingCount = (queue.size - 1 - currentIndex).coerceAtLeast(0)
                    item(key = "upcoming_section_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "BERIKUTNYA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = "$upcomingCount lagu",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                    }

                    itemsIndexed(
                        items = queue,
                        key = { index, song -> "${song.id}_$index" }
                    ) { index, song ->
                        if (index != currentIndex) {
                            QueueSongItemCard(
                                song = song,
                                canMoveUp = index > 0,
                                canMoveDown = index < queue.lastIndex,
                                onClick = { onPlayQueueItem(index) },
                                onMoveUp = { onMoveQueueItem(index, index - 1) },
                                onMoveDown = { onMoveQueueItem(index, index + 1) },
                                onRemove = { onRemoveQueueItem(index) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── Bottom Floating Action Bar Sesuai Image 2 ───────────────────
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xD9102218),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tombol "Acak antrean" (Lime / Mint Green Capsule)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFBAFF5C))
                                .clickable(onClick = onShuffleQueue)
                                .padding(vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Acak antrean",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Tombol "Simpan jadi playlist" (Dark Glass Capsule)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(50))
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Simpan jadi playlist",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongItemCard(
    song: SongEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Squircle Cover
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                if (song.albumArtUri.isNotBlank()) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    AlbumArtPlaceholder(
                        seed = song.title,
                        modifier = Modifier.matchParentSize(),
                        iconSize = 22.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val durStr = formatDuration(song.duration)
                Text(
                    text = "${song.artist} · $durStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Gagang Pengubah Urutan / Menu Opsi Sesuai Image 2
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Ubah Urutan",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (canMoveUp) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Pindah ke atas") },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMoveUp()
                            }
                        )
                    }
                    if (canMoveDown) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Pindah ke bawah") },
                            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMoveDown()
                            }
                        )
                    }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Hapus dari antrean", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
