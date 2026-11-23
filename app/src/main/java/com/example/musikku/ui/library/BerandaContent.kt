package com.example.musikku.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
import coil.compose.AsyncImage
import com.example.musikku.data.local.dao.PlaylistWithCount
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.ui.profile.UserProfileAvatar
import com.example.musikku.ui.spotify.SpotifyChartItem
import com.example.musikku.ui.spotify.SpotifyChartsSection
import com.example.musikku.ui.theme.TextMuted

data class CommunityPlaylist(
    val title: String,
    val author: String,
    val query: String,
    val color1: Color,
    val color2: Color,
    val color3: Color,
    val color4: Color
)

val defaultCommunityPlaylists = listOf(
    CommunityPlaylist(
        title = "Kompilasi Lagu",
        author = "Cadang Rustiawan",
        query = "kompilasi lagu indonesia pop hits",
        color1 = Color(0xFFE8F0FE),
        color2 = Color(0xFFC7D8E8),
        color3 = Color(0xFFA5B8C9),
        color4 = Color(0xFF8699AA)
    ),
    CommunityPlaylist(
        title = "santai",
        author = "Irudiana00",
        query = "lagu santai akustik cafe chill",
        color1 = Color(0xFFEADFD5),
        color2 = Color(0xFFC48E75),
        color3 = Color(0xFF4A443F),
        color4 = Color(0xFF8C3426)
    ),
    CommunityPlaylist(
        title = "galau",
        author = "Restu Musik",
        query = "lagu galau pop indonesia sedih",
        color1 = Color(0xFFD4A373),
        color2 = Color(0xFFCCD5AE),
        color3 = Color(0xFFE9EDC9),
        color4 = Color(0xFFFAEDCD)
    ),
    CommunityPlaylist(
        title = "Top Hits 2026",
        author = "MusikKu Chart",
        query = "top hits 2026 popular music",
        color1 = Color(0xFF3A86FF),
        color2 = Color(0xFF8338EC),
        color3 = Color(0xFFFF006E),
        color4 = Color(0xFFFB5607)
    ),
    CommunityPlaylist(
        title = "Pop Indonesia",
        author = "Indo Vibes",
        query = "pop indonesia trending",
        color1 = Color(0xFF06D6A0),
        color2 = Color(0xFF118AB2),
        color3 = Color(0xFF073B4C),
        color4 = Color(0xFFFFD166)
    ),
    CommunityPlaylist(
        title = "Lo-Fi Beats",
        author = "Chill Station",
        query = "lofi hip hop beats study relax",
        color1 = Color(0xFF6B4E71),
        color2 = Color(0xFF3A405A),
        color3 = Color(0xFFAEC5EB),
        color4 = Color(0xFFF9DEC9)
    )
)

/**
 * Halaman Beranda (Home):
 * Menampilkan Trending Community Playlists sesuai Gambar 6, Lagu Populer & Trending YouTube,
 * serta akses cepat ke musik pengguna.
 */
@Composable
fun BerandaContent(
    userName: String,
    userAvatarUri: String,
    trendingTracks: List<YouTubeSearchItemDto>,
    localSongs: List<SongEntity>,
    favoriteSongs: List<SongEntity>,
    playlists: List<PlaylistWithCount>,
    contentPadding: PaddingValues,
    onOpenProfile: () -> Unit,
    onRefresh: () -> Unit,
    onPlayCommunityPlaylist: (CommunityPlaylist) -> Unit,
    onPlayYouTubeTrack: (YouTubeSearchItemDto) -> Unit,
    onDownloadYouTubeTrack: (YouTubeSearchItemDto) -> Unit,
    onToggleFavoriteYouTube: (YouTubeSearchItemDto) -> Unit,
    onPlayLocalSong: (SongEntity, List<SongEntity>) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylistDetail: (PlaylistWithCount) -> Unit,
    accountConnected: Boolean = false,
    accountUserName: String = "",
    accountUserAvatar: String = "",
    onOpenSpotifyAccount: () -> Unit = {},
    onSelectSpotifyChart: (SpotifyChartItem) -> Unit = {},
    onQuickPlaySpotifyChart: (SpotifyChartItem) -> Unit = {},
    onOpenSpotifyImportDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding()),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // ─── 1. Header Bar: Avatar, Sambutan, Tombol Refresh ──────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserProfileAvatar(
                        userName = userName,
                        avatarUri = userAvatarUri,
                        size = 44.dp,
                        onClick = onOpenProfile
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.nav_beranda),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (userName.isNotBlank()) "Hai, $userName" else "MusikKu • Ruang Musikmu",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Segarkan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ─── 2. Trending community playlists Sesuai Gambar 6 ──────────────────
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_community_playlists),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    items(defaultCommunityPlaylists, key = { it.title }) { playlist ->
                        CommunityPlaylistCard(
                            playlist = playlist,
                            onClick = { onPlayCommunityPlaylist(playlist) }
                        )
                    }
                }
            }
        }

        // ─── 3. Spotify Charts & Trending (Tahap 2 & 3) ───────────────────────
        item {
            SpotifyChartsSection(
                accountConnected = accountConnected,
                accountUserName = accountUserName,
                accountUserAvatar = accountUserAvatar,
                onChartClick = { chart -> onSelectSpotifyChart(chart) },
                onQuickPlayClick = { chart -> onQuickPlaySpotifyChart(chart) },
                onImportLinkClick = onOpenSpotifyImportDialog,
                onOpenAccountClick = onOpenSpotifyAccount
            )
        }

        // ─── 4. Lagu Populer & Trending YouTube ────────────────────────────────
        if (trendingTracks.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.explore_popular_trending),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        trendingTracks.take(8).forEach { track ->
                            TrendingTrackRowItem(
                                track = track,
                                onPlay = { onPlayYouTubeTrack(track) },
                                onDownload = { onDownloadYouTubeTrack(track) },
                                onToggleFavorite = { onToggleFavoriteYouTube(track) }
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Baru Saja Diputar / Koleksi Lokal Cepat ────────────────────────
        if (localSongs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.home_your_collection),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(localSongs.take(10), key = { it.id }) { song ->
                            QuickLocalSongCard(
                                song = song,
                                onClick = { onPlayLocalSong(song, localSongs) }
                            )
                        }
                    }
                }
            }
        }

        // ─── 5. Playlist Kamu ──────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.library_your_playlists),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tombol Buat Baru
                    item {
                        Surface(
                            onClick = onCreatePlaylist,
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF23251E),
                            modifier = Modifier.size(136.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF67E8A6).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Buat Playlist",
                                        tint = Color(0xFF67E8A6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Buat Baru",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Playlist yang dibuat pengguna
                    items(playlists, key = { it.id }) { pl ->
                        Surface(
                            onClick = { onOpenPlaylistDetail(pl) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF23251E),
                            modifier = Modifier
                                .width(136.dp)
                                .height(160.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF33382D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = Color(0xFF67E8A6),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pl.songCount} lagu",
                                    style = MaterialTheme.typography.bodySmall,
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

/**
 * Kartu Playlist Komunitas dengan 4-kuadran cover art sesuai Gambar 6
 */
@Composable
private fun CommunityPlaylistCard(
    playlist: CommunityPlaylist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF23251E),
            modifier = Modifier.size(140.dp)
        ) {
            // 4 Kuadran Cover Art Mosaic
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
                            fontSize = 18.sp,
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = playlist.author,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Baris lagu trending dengan tombol putar
 */
@Composable
private fun TrendingTrackRowItem(
    track: YouTubeSearchItemDto,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        onClick = onPlay,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1B1E17),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Art
            if (!track.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E3526)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF67E8A6)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val authorText = track.author?.ifBlank { "YouTube Music" } ?: "YouTube Music"
                val min = track.lengthSeconds / 60
                val sec = (track.lengthSeconds % 60).toString().padStart(2, '0')
                val durText = "$min:$sec"
                Text(
                    text = "$authorText • $durText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Putar",
                    tint = Color(0xFF67E8A6)
                )
            }
        }
    }
}

/**
 * Kartu cepat untuk lagu lokal
 */
@Composable
private fun QuickLocalSongCard(
    song: SongEntity,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1E17),
        modifier = Modifier
            .width(130.dp)
            .height(150.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2E3827), Color(0xFF1B2317))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF67E8A6),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
