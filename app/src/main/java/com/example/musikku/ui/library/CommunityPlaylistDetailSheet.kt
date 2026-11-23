package com.example.musikku.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.musikku.ui.common.SongAlbumArt
import com.example.musikku.ui.common.rememberSheetScrollBlocker
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.ui.theme.TextMuted

/**
 * Bottom Sheet Detail untuk Trending Community Playlist di halaman Beranda.
 * Menampilkan cover 4-kuadran, judul, pembuat, tombol Putar & Acak,
 * serta daftar lagu tanpa berpindah ke tab Pencarian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPlaylistDetailSheet(
    playlist: CommunityPlaylist,
    onDismiss: () -> Unit,
    onPlayTrack: (YouTubeSearchItemDto) -> Unit,
    onPlayAll: (List<YouTubeSearchItemDto>) -> Unit,
    onShuffleAll: (List<YouTubeSearchItemDto>) -> Unit,
    loadTracks: suspend (String) -> List<YouTubeSearchItemDto>,
    currentPlayingTitle: String?,
    isPlaying: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tracks by remember(playlist.query) { mutableStateOf<List<YouTubeSearchItemDto>>(emptyList()) }
    var isLoading by remember(playlist.query) { mutableStateOf(true) }

    LaunchedEffect(playlist.query) {
        isLoading = true
        tracks = loadTracks(playlist.query)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161913),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // ─── Tombol Tutup ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // ─── Header Info Playlist: Cover Mozaik, Judul, Pembuat ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mozaik 4 Kuadran
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF23251E),
                    modifier = Modifier.size(105.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(playlist.color1),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = playlist.title.take(1),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(playlist.color2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(playlist.color3),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.Black.copy(alpha = 0.35f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(playlist.color4),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "♪",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${playlist.author} • MusikKu Community",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${tracks.size} lagu pilihan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF67E8A6)
                        )
                    }
                }
            }

            // ─── Tombol Aksi: Putar Semua & Acak ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayAll(tracks) },
                    enabled = tracks.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF67E8A6),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Putar Semua", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onShuffleAll(tracks) },
                    enabled = tracks.isNotEmpty(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF383D31),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acak", fontWeight = FontWeight.Bold)
                }
            }

            // ─── Daftar Lagu Playlist Komunitas ──────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF67E8A6), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memuat lagu komunitas...", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada lagu yang ditemukan untuk playlist ini",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val sheetScrollBlocker = rememberSheetScrollBlocker()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(sheetScrollBlocker),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(tracks, key = { _, track -> track.videoId }) { index, track ->
                        val isCurrentPlaying = isPlaying && currentPlayingTitle == track.title
                        Surface(
                            onClick = { onPlayTrack(track) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrentPlaying) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nomor urut
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrentPlaying) Color(0xFF67E8A6) else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )

                                // Cover Gambar
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF282D22)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SongAlbumArt(
                                        albumArtUri = track.thumbnailUrl,
                                        title = track.title,
                                        modifier = Modifier.fillMaxSize(),
                                        iconSize = 22.dp
                                    )
                                    if (isCurrentPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF67E8A6),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Judul dan Artis
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentPlaying) Color(0xFF67E8A6) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val durationMin = track.lengthSeconds / 60
                                    val durationSec = track.lengthSeconds % 60
                                    val formattedDuration = if (track.lengthSeconds > 0) " • ${durationMin}:${durationSec.toString().padStart(2, '0')}" else ""
                                    Text(
                                        text = "${track.author}$formattedDuration",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
