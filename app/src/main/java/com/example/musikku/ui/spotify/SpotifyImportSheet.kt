package com.example.musikku.ui.spotify

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.musikku.data.repository.SpotifyImportResult
import com.example.musikku.data.repository.SpotifyTrack
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.common.LiveEqualizerIndicator
import com.example.musikku.ui.common.rememberSheetScrollBlocker
import com.example.musikku.ui.theme.EmeraldSpotify
import com.example.musikku.ui.theme.TextMuted

/**
 * Bottom Sheet untuk menampilkan hasil impor dari Spotify (Track, Album, atau Playlist).
 * Dilengkapi tombol Putar Semua, Acak, dan Simpan ke Playlist Pustaka MusikKu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportSheet(
    result: SpotifyImportResult,
    currentPlayingTitle: String?,
    isPlaying: Boolean,
    isLoadingTrackId: String?,
    onDismiss: () -> Unit,
    onPlayTrack: (SpotifyTrack) -> Unit,
    onPlayAll: (List<SpotifyTrack>, Int) -> Unit,
    onShuffleAll: (List<SpotifyTrack>) -> Unit,
    onSaveToLibrary: (SpotifyImportResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollBlocker = rememberSheetScrollBlocker()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131713),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
        ) {
            // ─── Top Bar: Badge Spotify & Tombol Tutup ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldSpotify.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldSpotify.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldSpotify)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPOTIFY IMPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldSpotify,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ─── Header Playlist / Album / Track ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover Art
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!result.coverArtUri.isNullOrBlank()) {
                        AsyncImage(
                            model = result.coverArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AlbumArtPlaceholder(
                            seed = result.title,
                            modifier = Modifier.fillMaxSize(),
                            iconSize = 36.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ─── Tombol Aksi: Putar Semua, Acak, Simpan ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlayAll(result.tracks, 0) },
                    enabled = result.tracks.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSpotify,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Putar Semua", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = { onShuffleAll(result.tracks) },
                    enabled = result.tracks.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Acak", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = { onSaveToLibrary(result) },
                    enabled = result.tracks.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3825),
                        contentColor = EmeraldSpotify
                    ),
                    modifier = Modifier.weight(1.0f)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }

            // ─── Daftar Lagu ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(sheetScrollBlocker),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = result.tracks,
                    key = { index, track -> "${track.id}_$index" }
                ) { index, track ->
                    val isCurrent = currentPlayingTitle != null &&
                            currentPlayingTitle.contains(track.title, ignoreCase = true)
                    val isLoadingThis = isLoadingTrackId == track.id

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) EmeraldSpotify.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) EmeraldSpotify.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Nomor Urut / Loading
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (isLoadingThis) {
                                    CircularProgressIndicator(
                                        color = EmeraldSpotify,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isCurrent && isPlaying) {
                                    LiveEqualizerIndicator(
                                        isPlaying = true,
                                        tint = EmeraldSpotify,
                                        maxHeight = 14.dp,
                                        barWidth = 2.5.dp,
                                        spacing = 1.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) EmeraldSpotify else TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Judul dan Artis
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) EmeraldSpotify else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Durasi Lagu
                            if (track.durationMs > 0) {
                                val totalSeconds = track.durationMs / 1000
                                val minutes = totalSeconds / 60
                                val seconds = totalSeconds % 60
                                Text(
                                    text = String.format("%d:%02d", minutes, seconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
