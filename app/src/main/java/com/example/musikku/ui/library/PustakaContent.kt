package com.example.musikku.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.common.SongAlbumArt
import com.example.musikku.data.local.dao.PlaylistWithCount
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.ui.theme.TextMuted

enum class PustakaFilter {
    ALL,
    PLAYLISTS,
    SONGS,
    ALBUMS
}

enum class PustakaSortOrder {
    DATE_ADDED,
    NAME,
    DATE_MODIFIED;

    @Composable
    fun getDisplayName(): String = when (this) {
        DATE_ADDED -> stringResource(R.string.library_sort_date_added)
        NAME -> stringResource(R.string.library_sort_name)
        DATE_MODIFIED -> stringResource(R.string.library_sort_date_modified)
    }
}

enum class PustakaViewMode {
    GRID,
    LIST
}

enum class PustakaCategory {
    FAVORITES,
    DOWNLOADED,
    TOP50,
    CACHE,
    UPLOADED,
    LOCAL
}

data class PustakaFolderItem(
    val category: PustakaCategory,
    val title: String,
    val icon: ImageVector,
    val subtitle: String
)

/**
 * Halaman Pustaka (Library):
 * Tampilan persis Gambar 1, 2, dan 3:
 * - Header "Pustaka" merah-coral dengan ikon Pengaturan di kanan atas
 * - Filter chip: [ Daftar putar ], [ Lagu ], [ Album ]
 * - Bar toolbar: Dropdown sort [ Tanggal ditambahkan ▼ ] + Toggle View (List ≡ vs Grid ⊞)
 * - 6 Folder utama: Disukai, Diunduh, Teratas Saya 50, Tersimpan di Cache, Diunggah, Local
 * - FAB bulat (+) di kanan bawah
 */
@Composable
fun PustakaContent(
    songs: List<SongEntity>,
    favoriteSongs: List<SongEntity>,
    playlists: List<PlaylistWithCount>,
    currentSongId: Long?,
    isPlaying: Boolean,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (PlaylistWithCount) -> Unit,
    onSongClick: (SongEntity) -> Unit,
    onPlayAll: (List<SongEntity>, Int) -> Unit,
    onSongWithQueueClick: (SongEntity, List<SongEntity>) -> Unit = { song, queue -> onPlayAll(queue, queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)) },
    onShuffleAll: (List<SongEntity>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf<PustakaCategory?>(null) }
    var activeFilter by remember { mutableStateOf(PustakaFilter.ALL) }
    var sortOrder by remember { mutableStateOf(PustakaSortOrder.DATE_ADDED) }
    var viewMode by remember { mutableStateOf(PustakaViewMode.GRID) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Tangani tombol Back jika sedang berada di dalam sub-kategori
    BackHandler(enabled = activeCategory != null || activeFilter != PustakaFilter.ALL) {
        if (activeCategory != null) {
            activeCategory = null
        } else {
            activeFilter = PustakaFilter.ALL
        }
    }

    val titleFavorites = stringResource(R.string.library_favorites)
    val titleDownloaded = stringResource(R.string.library_downloaded)
    val titleTop50 = stringResource(R.string.library_top50)
    val titleCache = stringResource(R.string.library_cache)
    val titleUploaded = stringResource(R.string.library_uploaded)
    val titleLocal = stringResource(R.string.library_local)

    val systemFolders = remember(favoriteSongs.size, songs.size, titleFavorites, titleDownloaded, titleTop50, titleCache, titleUploaded, titleLocal) {
        listOf(
            PustakaFolderItem(
                category = PustakaCategory.FAVORITES,
                title = titleFavorites,
                icon = Icons.Default.FavoriteBorder,
                subtitle = "${favoriteSongs.size} lagu"
            ),
            PustakaFolderItem(
                category = PustakaCategory.DOWNLOADED,
                title = titleDownloaded,
                icon = Icons.Default.FileDownloadDone,
                subtitle = "${songs.count { it.source == "DOWNLOADED" }} lagu"
            ),
            PustakaFolderItem(
                category = PustakaCategory.TOP50,
                title = titleTop50,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                subtitle = "50 lagu"
            ),
            PustakaFolderItem(
                category = PustakaCategory.CACHE,
                title = titleCache,
                icon = Icons.Default.Sync,
                subtitle = "Cache offline"
            ),
            PustakaFolderItem(
                category = PustakaCategory.UPLOADED,
                title = titleUploaded,
                icon = Icons.Default.CloudUpload,
                subtitle = "Cloud"
            ),
            PustakaFolderItem(
                category = PustakaCategory.LOCAL,
                title = titleLocal,
                icon = Icons.Default.Folder,
                subtitle = "${songs.size} lagu"
            )
        )
    }

    val albumGroups = remember(songs) { songs.groupBy { it.album.ifBlank { "Unknown Album" } } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
        // ─── KONTEN SUB-KATEGORI AKTIF (Disukai, Local, Diunduh, dll) ────────
        if (activeCategory != null) {
            SubCategoryDetailView(
                category = activeCategory!!,
                songs = songs,
                favoriteSongs = favoriteSongs,
                currentSongId = currentSongId,
                isPlaying = isPlaying,
                onBack = { activeCategory = null },
                onSongClick = onSongWithQueueClick,
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
                onToggleFavorite = onToggleFavorite,
                onAddToQueue = onAddToQueue,
                onPlayNext = onPlayNext,
                onAddToPlaylist = onAddToPlaylist,
                onDeleteSong = onDeleteSong
            )
        } else {
            // ─── TAMPILAN UTAMA PUSTAKA (Sesuai Gambar 1, 2, 3) ────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header Bar: "Pustaka" + Ikon Pengaturan di Kanan Atas
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nav_pustaka),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 32.sp
                        )

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = Color(0xFF67E8A6), // Emerald green persis screenshot
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // 2. Baris Filter Chips: [ Daftar putar ] [ Lagu ] [ Album ]
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PustakaFilterChip(
                            label = stringResource(R.string.library_playlists),
                            isSelected = activeFilter == PustakaFilter.PLAYLISTS,
                            onClick = {
                                activeFilter = if (activeFilter == PustakaFilter.PLAYLISTS) PustakaFilter.ALL else PustakaFilter.PLAYLISTS
                            }
                        )

                        PustakaFilterChip(
                            label = stringResource(R.string.library_songs),
                            isSelected = activeFilter == PustakaFilter.SONGS,
                            onClick = {
                                activeFilter = if (activeFilter == PustakaFilter.SONGS) PustakaFilter.ALL else PustakaFilter.SONGS
                            }
                        )

                        PustakaFilterChip(
                            label = stringResource(R.string.library_albums),
                            isSelected = activeFilter == PustakaFilter.ALBUMS,
                            onClick = {
                                activeFilter = if (activeFilter == PustakaFilter.ALBUMS) PustakaFilter.ALL else PustakaFilter.ALBUMS
                            }
                        )
                    }
                }

                // 3. Bar Toolbar: Dropdown Sort Pill + View Mode Toggle (≡ vs ⊞)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dropdown Pill Sort
                        Box {
                            Surface(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFD6DEC9), // Sage beige persis screenshot
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sortOrder.getDisplayName(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E241A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Pilih urutan",
                                        tint = Color(0xFF1E241A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Menu Dropdown Sort (Sesuai Gambar 2)
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF20231D))
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                PustakaSortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = order.getDisplayName(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White
                                                )
                                                RadioButton(
                                                    selected = sortOrder == order,
                                                    onClick = {
                                                        sortOrder = order
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Toggle Mode Tampilan (List ≡ vs Grid ⊞)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tombol List Mode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (viewMode == PustakaViewMode.LIST) Color(0xFFD6DEC9) else Color(0xFF282C22))
                                    .clickable { viewMode = PustakaViewMode.LIST }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Mode Daftar",
                                    tint = if (viewMode == PustakaViewMode.LIST) Color(0xFF1E241A) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Tombol Grid Mode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (viewMode == PustakaViewMode.GRID) Color(0xFFD6DEC9) else Color(0xFF282C22))
                                    .clickable { viewMode = PustakaViewMode.GRID }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Mode Grid",
                                    tint = if (viewMode == PustakaViewMode.GRID) Color(0xFF1E241A) else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (activeFilter == PustakaFilter.SONGS) {
                    // Tampilkan daftar lagu jika filter Lagu dipilih
                    items(songs, key = { it.id }) { song ->
                        PustakaSongRow(
                            song = song,
                            isPlaying = isPlaying && currentSongId == song.id,
                            onClick = { onSongClick(song) }
                        )
                    }
                } else if (activeFilter == PustakaFilter.ALBUMS) {
                    // Tampilkan grup album
                    items(albumGroups.keys.toList()) { albumName ->
                        val albumSongs = albumGroups[albumName] ?: emptyList()
                        Surface(
                            onClick = { onPlayAll(albumSongs, 0) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF23261F),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val artUri = albumSongs.firstOrNull { it.albumArtUri.isNotBlank() }?.albumArtUri ?: ""
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF353A2F)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SongAlbumArt(
                                        albumArtUri = artUri,
                                        title = albumName,
                                        modifier = Modifier.fillMaxSize(),
                                        iconSize = 22.dp,
                                        showConcentricRings = true
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(albumName, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${albumSongs.size} lagu", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                } else if (viewMode == PustakaViewMode.GRID) {
                    // Mode Grid 3 Kolom Sesuai Gambar 1
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Baris 1: Disukai, Diunduh, Teratas Saya 50
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                systemFolders.take(3).forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        PustakaGridCard(
                                            item = item,
                                            onClick = { activeCategory = item.category }
                                        )
                                    }
                                }
                            }

                            // Baris 2: Tersimpan di Cache, Diunggah, Local
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                systemFolders.drop(3).take(3).forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        PustakaGridCard(
                                            item = item,
                                            onClick = { activeCategory = item.category }
                                        )
                                    }
                                }
                            }

                            // Playlist pengguna tambahan
                            if (playlists.isNotEmpty() && activeFilter != PustakaFilter.SONGS) {
                                Text(
                                    text = stringResource(R.string.library_your_playlists),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 8.dp)
                                )

                                val chunkedPlaylists = playlists.chunked(3)
                                chunkedPlaylists.forEach { rowPlaylists ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowPlaylists.forEach { pl ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                PustakaPlaylistGridCard(
                                                    playlist = pl,
                                                    onClick = { onPlaylistClick(pl) }
                                                )
                                            }
                                        }
                                        // Isi spacer kosong jika kurang dari 3
                                        repeat(3 - rowPlaylists.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Mode List Sesuai Gambar 3
                    items(systemFolders, key = { it.title }) { item ->
                        PustakaListRow(
                            item = item,
                            onClick = { activeCategory = item.category }
                        )
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 66.dp)
                        )
                    }

                    // Playlist Pengguna pada mode list
                    if (playlists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Daftar Putar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }

                        items(playlists, key = { it.id }) { pl ->
                            Surface(
                                onClick = { onPlaylistClick(pl) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF353A2F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                            contentDescription = null,
                                            tint = Color(0xFFD6DEC9),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${pl.songCount} lagu",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(start = 66.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── 5. FLOATING ACTION BUTTON (Hanya di Tampilan Utama Pustaka) ────────
        if (activeCategory == null) {
            FloatingActionButton(
                onClick = onCreatePlaylistClick,
                shape = CircleShape,
                containerColor = Color(0xFF434A3E), // Olive gelap persis screenshot
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Buat Playlist Baru",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Filter Chip Pustaka [ Daftar putar ] [ Lagu ] [ Album ]
 */
@Composable
private fun PustakaFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF383E31) else Color(0xFF22261E),
        modifier = Modifier.height(38.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFFD6DEC9) else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Kartu Grid 3 Kolom Pustaka Sesuai Gambar 1
 */
@Composable
private fun PustakaGridCard(
    item: PustakaFolderItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF383D31), // Kotak gelap persis screenshot
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = Color(0xFFD6DEC9), // Ikon abu-sage persis screenshot
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Kartu Playlist Pengguna dalam Mode Grid
 */
@Composable
private fun PustakaPlaylistGridCard(
    playlist: PlaylistWithCount,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF383D31),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = playlist.name,
                    tint = Color(0xFFD6DEC9),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Baris List Pustaka Sesuai Gambar 3
 */
@Composable
private fun PustakaListRow(
    item: PustakaFolderItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF383D31)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = Color(0xFFD6DEC9),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * Tampilan detail sub-kategori ketika kartu (Disukai, Local, Diunduh, dll) diketuk.
 * Disesuaikan persis dengan screenshot tampilan Lagu Local (Hero, Putar, Acak, About, Sortir, dan Search Bar).
 */
@Composable
private fun SubCategoryDetailView(
    category: PustakaCategory,
    songs: List<SongEntity>,
    favoriteSongs: List<SongEntity>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAll: (List<SongEntity>, Int) -> Unit,
    onShuffleAll: (List<SongEntity>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit
) {
    val titleFavorites = stringResource(R.string.library_favorites)
    val titleLocal = stringResource(R.string.library_local)
    val titleDownloaded = stringResource(R.string.library_downloaded)
    val titleTop50 = stringResource(R.string.library_top50)
    val titleCache = stringResource(R.string.library_cache)
    val titleUploaded = stringResource(R.string.library_uploaded)

    val (title, displaySongs) = when (category) {
        PustakaCategory.FAVORITES -> Pair(titleFavorites, favoriteSongs)
        PustakaCategory.LOCAL     -> Pair(titleLocal, songs)
        PustakaCategory.DOWNLOADED -> Pair(titleDownloaded, songs.filter { it.source == "DOWNLOADED" }.ifEmpty { songs.take(15) })
        PustakaCategory.TOP50     -> Pair(titleTop50, songs.take(50))
        PustakaCategory.CACHE     -> Pair(titleCache, songs.take(10))
        PustakaCategory.UPLOADED  -> Pair(titleUploaded, emptyList<SongEntity>())
    }

    // Durasi total gabungan lagu
    val totalDurationMs = remember(displaySongs) { displaySongs.sumOf { it.duration } }
    val formattedDuration = remember(totalDurationMs) {
        val hours = totalDurationMs / 3600000
        val minutes = (totalDurationMs % 3600000) / 60000
        val seconds = (totalDurationMs % 60000) / 1000
        if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var subSortOrder by remember { mutableStateOf(PustakaSortOrder.DATE_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Tangani tombol back perangkat Android
    BackHandler {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else if (isSearchVisible) {
            isSearchVisible = false
        } else {
            onBack()
        }
    }

    // Filter dan Urutkan Lagu
    val filteredSongs = remember(displaySongs, searchQuery, subSortOrder) {
        val searched = if (searchQuery.isBlank()) {
            displaySongs
        } else {
            val query = searchQuery.trim().lowercase()
            displaySongs.filter {
                it.title.lowercase().contains(query) ||
                it.artist.lowercase().contains(query) ||
                it.album.lowercase().contains(query)
            }
        }
        when (subSortOrder) {
            PustakaSortOrder.DATE_ADDED -> searched.sortedByDescending { it.id }
            PustakaSortOrder.NAME -> searched.sortedBy { it.title.lowercase() }
            PustakaSortOrder.DATE_MODIFIED -> searched.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ─── 1. TOP BAR ROW (Kembali, Muat Ulang, Toggle Search) ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color(0xFF383D31),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { /* Refresh */ },
                    shape = CircleShape,
                    color = Color(0xFF383D31),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat ulang",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    onClick = { isSearchVisible = !isSearchVisible },
                    shape = CircleShape,
                    color = if (isSearchVisible) Color(0xFF67E8A6) else Color(0xFF383D31),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari lagu",
                            tint = if (isSearchVisible) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ─── 2. SEARCH BAR PROMINEN (Bisa ditoggle atau langsung digunakan) ──
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2E3328),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFD6DEC9),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari judul, artis, atau album...",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── 3. DAFTAR HERO HEADER & LAGU ────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Title Persis Layout: ♫ Local songs
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${displaySongs.size} lagu • $formattedDuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }

            // Tombol Aksi: [ ▶ Putar ] [ 🔀 Acak ] ( ⋮ )
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Putar (Pill Putih Elegan)
                    Surface(
                        onClick = { if (filteredSongs.isNotEmpty()) onPlayAll(filteredSongs, 0) },
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Putar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }

                    // Acak (Pill Dark Olive / Transparan)
                    Surface(
                        onClick = { if (filteredSongs.isNotEmpty()) onShuffleAll(filteredSongs) },
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF383D31),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Acak",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    // More Options (⋮)
                    Box {
                        Surface(
                            onClick = { showMoreMenu = true },
                            shape = CircleShape,
                            color = Color(0xFF383D31),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tambah semua ke antrean") },
                                onClick = {
                                    filteredSongs.forEach { onAddToQueue(it) }
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Putar berikutnya") },
                                onClick = {
                                    filteredSongs.firstOrNull()?.let { onPlayNext(it) }
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Seksi "About"
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$title features ${displaySongs.size} lagu. Combined duration is $formattedDuration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        lineHeight = 20.sp
                    )
                }
            }

            // Tombol Sortir [ Tanggal ditambahkan ▼ ] (Sage / Cream persis Pustaka)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFCED6C2),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subSortOrder.getDisplayName(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            PustakaSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.getDisplayName()) },
                                    onClick = {
                                        subSortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "${filteredSongs.size} ditemukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF67E8A6)
                        )
                    }
                }
            }

            // Daftar Lagu Terfilter
            if (filteredSongs.isNotEmpty()) {
                items(filteredSongs, key = { it.id }) { song ->
                    PustakaSongRow(
                        song = song,
                        isPlaying = isPlaying && currentSongId == song.id,
                        onClick = { onSongClick(song, filteredSongs) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Tidak ada lagu yang cocok dengan \"$searchQuery\"" else "Belum ada lagu di $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Baris item lagu di dalam Pustaka
 */
@Composable
private fun PustakaSongRow(
    song: SongEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val durationText = remember(song.duration) {
        val totalSec = song.duration / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        String.format("%d:%02d", min, sec)
    }

    var isImageError by remember(song.albumArtUri) { mutableStateOf(false) }
    val showPlaceholder = song.albumArtUri.isBlank() || isImageError

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) Color.White.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E3328)),
                contentAlignment = Alignment.Center
            ) {
                if (!showPlaceholder) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        onError = { isImageError = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AlbumArtPlaceholder(
                        seed = song.title,
                        modifier = Modifier.fillMaxSize(),
                        iconSize = 22.dp,
                        showConcentricRings = true
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Sedang diputar",
                            tint = Color(0xFF67E8A6),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) Color(0xFF67E8A6) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • $durationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
