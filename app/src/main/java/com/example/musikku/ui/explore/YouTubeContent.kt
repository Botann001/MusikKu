package com.example.musikku.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.library.YouTubeSearchState
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.JetCard
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted

@Composable
fun YouTubeContent(
    state: YouTubeSearchState,
    currentPlayingTitle: String?,
    isPlaying: Boolean,
    isOnline: Boolean,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onPlay: (YouTubeSearchItemDto) -> Unit,
    onAddToQueue: (YouTubeSearchItemDto) -> Unit,
    onAddToPlaylist: (YouTubeSearchItemDto) -> Unit
) {
    var searchInput by remember { mutableStateOf(state.query) }
    val focusManager = LocalFocusManager.current
    val genreChips = listOf(
        "🔥 Trending",
        "Pop Indonesia",
        "Pop Barat",
        "Rock",
        "Lo-Fi Beats",
        "K-Pop",
        "Akustik",
        "Dangdut Koplo",
        "Anime OST",
        "R&B"
    )
    var selectedGenre by remember { mutableStateOf("🔥 Trending") }

    val displayItems = if (state.query.isBlank()) state.trendingTracks else state.results

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 6.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ─── 1. Search Bar Kapsul Modern ──────────────────────────────────────────
        item(key = "yt_search_bar") {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Cari lagu, artis, atau album di YouTube...", color = Color(0xFF7E807E), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = ElectricLime
                        )
                    },
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                selectedGenre = "🔥 Trending"
                                onSearch("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = JetCard,
                        unfocusedContainerColor = JetCard,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearch(searchInput)
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ─── 2. Genre Chips Kapsul ───────────────────────────────────────────────
        item(key = "yt_genre_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genreChips.forEach { genre ->
                    val isSelected = (genre == "🔥 Trending" && searchInput.isBlank() && selectedGenre == "🔥 Trending") ||
                            (selectedGenre == genre && searchInput.isNotBlank()) ||
                            (searchInput.equals(genre.removePrefix("🔥 "), ignoreCase = true))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) ElectricLime else Color(0xFF1D1F1D),
                        modifier = Modifier.clickable {
                            selectedGenre = genre
                            if (genre == "🔥 Trending") {
                                searchInput = ""
                                onSearch("")
                            } else {
                                val clean = genre.removePrefix("🔥 ")
                                searchInput = clean
                                onSearch(clean)
                            }
                        }
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OnElectricLime else Color(0xFFE0E0E0),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // ─── 3. Offline Warning Banner ───────────────────────────────────────────
        if (!isOnline) {
            item(key = "yt_offline_banner") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2C1E1E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Perangkat sedang offline. Sambungkan internet untuk memutar streaming YouTube.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                }
            }
        }

        // ─── 3b. Info Notice Banner ──────────────────────────────────────────────
        item(key = "yt_notice_banner") {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                Surface(
                    color = JetCard.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Streaming YouTube publik sewaktu-waktu dibatasi oleh Google. Jika terkendala, nikmati jutaan musik bebas hambatan di tab Jamendo.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // ─── 4. Main Body: Results / Loading / Empty ─────────────────────────────
        when {
            state.isSearching -> {
                item(key = "yt_loading") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = ElectricLime,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.query.isBlank()) "Memuat trending YouTube Music..." else "Mencari di YouTube Music...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            state.errorMessage != null && displayItems.isEmpty() -> {
                item(key = "yt_error") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF7E807E),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (state.query.isNotBlank()) onSearch(state.query) else onSearch("")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricLime,
                                contentColor = OnElectricLime
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coba Lagi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            displayItems.isEmpty() -> {
                item(key = "yt_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF4A4D4A),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.query.isBlank()) "Ketik lagu atau artis untuk mulai mencari" else "Tidak ada lagu yang ditemukan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            else -> {
                item(key = "yt_section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.query.isBlank()) "Lagu Populer & Trending" else "Hasil Pencarian (\"${state.query}\")",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${displayItems.size} Lagu",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }

                items(displayItems, key = { it.videoId }) { item ->
                    val isCurrentTrack = currentPlayingTitle != null &&
                            (currentPlayingTitle.contains(item.title, ignoreCase = true) ||
                                    item.title.contains(currentPlayingTitle, ignoreCase = true))
                    val isResolvingThis = state.isLoadingStreamId == item.videoId

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        YouTubeTrackCard(
                            track = item,
                            isCurrentlyPlaying = isCurrentTrack && isPlaying,
                            isLoadingStream = isResolvingThis,
                            onPlayClick = { onPlay(item) },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeTrackCard(
    track: YouTubeSearchItemDto,
    isCurrentlyPlaying: Boolean,
    isLoadingStream: Boolean,
    onPlayClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) Color(0xFF1F281B) else JetCard
        ),
        border = BorderStroke(
            1.dp,
            if (isCurrentlyPlaying) ElectricLime else Color(0xFF262826)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlayClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail dengan indikator durasi
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF202220))
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Durasi di pojok kanan bawah thumbnail
                if (track.lengthSeconds > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = formatDuration(track.lengthSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Overlay status pemutaran
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Sedang diputar",
                            tint = ElectricLime,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Lagu: Judul, Artis, Tag YouTube
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) ElectricLime else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.author ?: "YouTube Music",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Pill Badge "YT Music"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF281C1C)
                    ) {
                        Text(
                            text = "YT Music",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6666),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Aksi: Tombol Play / Loading / Menu
            if (isLoadingStream) {
                CircularProgressIndicator(
                    color = ElectricLime,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(4.dp)
                )
            } else {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentlyPlaying) ElectricLime else Color(0xFF262826))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = if (isCurrentlyPlaying) OnElectricLime else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // More Options Menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Lagu",
                        tint = TextMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF202220))
                ) {
                    DropdownMenuItem(
                        text = { Text("Putar Sekarang", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricLime)
                        },
                        onClick = {
                            menuExpanded = false
                            onPlayClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tambah ke Antrean", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = Color(0xFFCCCCCC))
                        },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tambah ke Playlist", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = Color(0xFFCCCCCC))
                        },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
