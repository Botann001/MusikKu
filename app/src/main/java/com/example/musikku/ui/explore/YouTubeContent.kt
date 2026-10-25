package com.example.musikku.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.musikku.ui.theme.FavoriteRed
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
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
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
import com.example.musikku.ui.theme.MintGreen
import com.example.musikku.ui.theme.JetCard
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted
import com.example.musikku.data.remote.youtube.songId
import com.example.musikku.ui.theme.FavoriteRed

@Composable
fun YouTubeContent(
    state: YouTubeSearchState,
    currentPlayingTitle: String?,
    isPlaying: Boolean,
    isOnline: Boolean,
    favoriteSongIds: Set<Long> = emptySet(),
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onPlay: (YouTubeSearchItemDto) -> Unit,
    onAddToQueue: (YouTubeSearchItemDto) -> Unit,
    onAddToPlaylist: (YouTubeSearchItemDto) -> Unit,
    onToggleFavorite: (YouTubeSearchItemDto) -> Unit = {},
    onDownload: (YouTubeSearchItemDto) -> Unit = {}
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
                    placeholder = { Text(stringResource(R.string.explore_search_hint), color = Color(0xFF7E807E), fontSize = 14.sp) },
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
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(50))
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
                        color = if (isSelected) Color(0xFFBAFF5C) else Color.White.copy(alpha = 0.08f),
                        border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
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
                            color = if (isSelected) Color.Black else Color.White,
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
                    val isFav = favoriteSongIds.contains(item.songId)
                    val isDownloaded = state.downloadedIds.contains(item.songId)
                    val isDownloading = state.downloadingIds.contains(item.songId)
                    val progress = state.downloadProgress[item.songId] ?: 0

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        YouTubeTrackCard(
                            track = item,
                            isCurrentlyPlaying = isCurrentTrack && isPlaying,
                            isLoadingStream = isResolvingThis,
                            isFavorite = isFav,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            downloadProgress = progress,
                            onPlayClick = { onPlay(item) },
                            onAddToQueue = { onAddToQueue(item) },
                            onAddToPlaylist = { onAddToPlaylist(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onDownload = { onDownload(item) }
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
    isFavorite: Boolean = false,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0,
    onPlayClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) Color(0x381C4830) else Color.White.copy(alpha = 0.07f)
        ),
        border = BorderStroke(
            if (isCurrentlyPlaying) 1.5.dp else 1.dp,
            if (isCurrentlyPlaying) MintGreen.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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

            // Info Lagu: Judul, Artis, Tag YouTube & Downloaded
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) MintGreen else Color.White,
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

                    // Pill Badge "Offline" jika sudah didownload
                    if (isDownloaded) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1B2E1B)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = ElectricLime,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricLime
                                )
                            }
                        }
                    }
                }
            }

            // Tombol Favorit Cepat
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                    tint = if (isFavorite) FavoriteRed else Color(0xFF7E807E),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Aksi: Unduh / Loading Stream / Tombol Play
            if (isDownloading) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { if (downloadProgress > 0) downloadProgress / 100f else 0.4f },
                        color = ElectricLime,
                        trackColor = Color(0xFF2A2D2A),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (isLoadingStream) {
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
                        text = {
                            Text(
                                if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) FavoriteRed else Color(0xFFCCCCCC)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                when {
                                    isDownloaded -> "Tersimpan di Lokal (Offline)"
                                    isDownloading -> "Sedang Mengunduh ($downloadProgress%)..."
                                    else -> "Download Lagu (Simpan Offline)"
                                },
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = null,
                                tint = if (isDownloaded) ElectricLime else Color(0xFFCCCCCC)
                            )
                        },
                        enabled = !isDownloaded && !isDownloading,
                        onClick = {
                            menuExpanded = false
                            onDownload()
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
