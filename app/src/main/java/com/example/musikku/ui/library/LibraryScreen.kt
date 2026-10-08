package com.example.musikku.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.musikku.data.local.dao.PlaylistWithCount
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.playback.PlaybackState
import com.example.musikku.ui.player.NowPlayingSheet
import com.example.musikku.ui.playlist.AddToPlaylistDialog
import com.example.musikku.ui.playlist.CreatePlaylistDialog
import com.example.musikku.ui.playlist.PlaylistDetailSheet
import com.example.musikku.ui.playlist.RenamePlaylistDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onSongClick: (SongEntity) -> Unit = { viewModel.playSong(it) }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasPermission by remember {
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        hasPermission = audioGranted
        if (audioGranted) {
            viewModel.refreshLibrary()
        }
    }

    var selectedBottomTab by remember { mutableStateOf(AppBottomTab.LIBRARY) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<SongEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var playlistForRename by remember { mutableStateOf<PlaylistWithCount?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.userMessageEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selectedBottomTab == AppBottomTab.LIBRARY) {
                LibraryTopBar(
                    scrollBehavior = scrollBehavior,
                    searchQuery = uiState.searchQuery,
                    selectedTab = uiState.selectedTab,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onTabSelected = viewModel::selectTab,
                    onRefreshClick = viewModel::refreshLibrary
                )
            }
        },
        bottomBar = {
            Column {
                if (playbackState.currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onClick = { showNowPlaying = true },
                        onPlayPauseClick = viewModel::playPause,
                        onNextClick = viewModel::skipToNext,
                        onPrevClick = viewModel::skipToPrevious
                    )
                }
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedBottomTab == AppBottomTab.LIBRARY,
                        onClick = { selectedBottomTab = AppBottomTab.LIBRARY },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == AppBottomTab.EXPLORE,
                        onClick = { selectedBottomTab = AppBottomTab.EXPLORE },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Jelajah") },
                        label = { Text("Jelajah") }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == AppBottomTab.SETTINGS,
                        onClick = { selectedBottomTab = AppBottomTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                        label = { Text("Pengaturan") }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedBottomTab == AppBottomTab.SETTINGS) {
            com.example.musikku.ui.settings.SettingsScreen(
                currentThemeMode = uiState.themeMode,
                downloadOnlyWifi = uiState.downloadOnlyWifi,
                sleepTimerSeconds = playbackState.sleepTimerRemainingSeconds,
                contentPadding = innerPadding,
                onThemeModeChanged = viewModel::setThemeMode,
                onToggleDownloadOnlyWifi = viewModel::toggleDownloadOnlyWifi,
                onSetSleepTimer = viewModel::setSleepTimer,
                onCancelSleepTimer = viewModel::cancelSleepTimer
            )
        } else if (selectedBottomTab == AppBottomTab.EXPLORE) {
            PlaceholderTabContent(
                title = "Jelajah Musik",
                modifier = Modifier.padding(innerPadding)
            )
        } else if (!hasPermission) {
            PermissionEmptyState(
                onRequestPermission = { permissionLauncher.launch(permissionsToRequest) },
                modifier = Modifier.padding(innerPadding)
            )
        } else when {
            uiState.isLoading -> LoadingIndicator(Modifier.padding(innerPadding))

            uiState.selectedTab == LibraryTab.SONGS -> {
                if (uiState.songs.isEmpty()) {
                    EmptyState(
                        query = uiState.searchQuery,
                        onRefreshClick = viewModel::refreshLibrary,
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    SongList(
                        songs = uiState.songs,
                        currentSongId = playbackState.currentSong?.id,
                        favoriteSongIds = uiState.favoriteSongIds,
                        sortOrder = uiState.sortOrder,
                        onSortOrderChanged = viewModel::setSortOrder,
                        contentPadding = innerPadding,
                        onSongClick = onSongClick,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onPlayNext = viewModel::playNext,
                        onAddToPlaylist = { songForAddToPlaylist = it }
                    )
                }
            }

            uiState.selectedTab == LibraryTab.FAVORITES -> {
                if (uiState.favoriteSongs.isEmpty()) {
                    EmptyFavoritesState(modifier = Modifier.padding(innerPadding))
                } else {
                    FavoritesContent(
                        favoriteSongs = uiState.favoriteSongs,
                        currentSongId = playbackState.currentSong?.id,
                        favoriteSongIds = uiState.favoriteSongIds,
                        contentPadding = innerPadding,
                        onSongClick = { viewModel.playSong(it, uiState.favoriteSongs) },
                        onPlayAll = { viewModel.playAll(uiState.favoriteSongs, 0) },
                        onShuffleAll = { viewModel.playAll(uiState.favoriteSongs.shuffled(), 0) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onPlayNext = viewModel::playNext,
                        onAddToPlaylist = { songForAddToPlaylist = it }
                    )
                }
            }

            uiState.selectedTab == LibraryTab.PLAYLISTS -> {
                PlaylistsContent(
                    playlists = uiState.playlists,
                    contentPadding = innerPadding,
                    onCreateClick = { showCreatePlaylistDialog = true },
                    onPlaylistClick = { selectedPlaylistForDetail = it },
                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                    onRenamePlaylist = { playlistForRename = it }
                )
            }

            uiState.selectedTab == LibraryTab.JAMENDO -> {
                com.example.musikku.ui.jamendo.JamendoContent(
                    state = uiState.jamendoState,
                    currentPlayingUri = playbackState.currentSong?.contentUri,
                    isOnline = uiState.isOnline,
                    downloadOnlyWifi = uiState.downloadOnlyWifi,
                    contentPadding = innerPadding,
                    onSearch = viewModel::searchJamendo,
                    onStream = viewModel::playJamendoTrack,
                    onDownload = viewModel::downloadJamendoTrack,
                    onDeleteDownload = viewModel::deleteDownloadedTrack,
                    onToggleDownloadOnlyWifi = viewModel::toggleDownloadOnlyWifi
                )
            }
        }
    }

    // ─── Dialogs & Sheets ────────────────────────────────────────────────────────

    // Dialog Tambah ke Playlist
    songForAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            song = song,
            playlists = uiState.playlists,
            onDismiss = { songForAddToPlaylist = null },
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                songForAddToPlaylist = null
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            }
        )
    }

    // Dialog Buat Playlist Baru
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    // Dialog Ganti Nama Playlist
    playlistForRename?.let { targetPlaylist ->
        RenamePlaylistDialog(
            initialName = targetPlaylist.name,
            onDismiss = { playlistForRename = null },
            onConfirm = { newName ->
                viewModel.renamePlaylist(targetPlaylist.id, newName)
                playlistForRename = null
            }
        )
    }

    // Sheet Detail Playlist
    selectedPlaylistForDetail?.let { playlist ->
        val playlistSongs by viewModel.getSongsForPlaylist(playlist.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())

        PlaylistDetailSheet(
            playlist = playlist,
            songs = playlistSongs,
            currentSongId = playbackState.currentSong?.id,
            onDismiss = { selectedPlaylistForDetail = null },
            onPlayAll = { songs, index ->
                viewModel.playAll(songs, index)
            },
            onRemoveSong = { songId ->
                viewModel.removeSongFromPlaylist(playlist.id, songId)
            },
            onDeletePlaylist = {
                viewModel.deletePlaylist(playlist.id)
                selectedPlaylistForDetail = null
            },
            onRenamePlaylist = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
            }
        )
    }

    // Sheet Pemutar Penuh (Now Playing) & Antrean
    if (showNowPlaying && playbackState.currentSong != null) {
        val currentSongId = playbackState.currentSong?.id ?: -1L
        NowPlayingSheet(
            playbackState = playbackState,
            isFavorite = uiState.favoriteSongIds.contains(currentSongId),
            onToggleFavorite = { viewModel.toggleFavorite(currentSongId) },
            onDismiss = { showNowPlaying = false },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::skipToNext,
            onPrevious = viewModel::skipToPrevious,
            onSeekTo = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onToggleRepeat = viewModel::toggleRepeatMode,
            onPlayQueueItem = viewModel::playQueueItem,
            onRemoveQueueItem = viewModel::removeQueueItem,
            onMoveQueueItem = viewModel::moveQueueItem,
            onSetSleepTimer = viewModel::setSleepTimer,
            onCancelSleepTimer = viewModel::cancelSleepTimer,
            onUpdateAlbumCover = { songId, uri ->
                viewModel.updateAlbumCover(songId, uri, context)
            },
            onRemoveAlbumCover = { songId ->
                viewModel.removeCustomAlbumCover(songId)
            }
        )
    }
}

// ─── Top Bar & Tabs ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    searchQuery: String,
    selectedTab: LibraryTab,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onRefreshClick: () -> Unit
) {
    Column {
        LargeTopAppBar(
            title = { Text("MusikKu") },
            scrollBehavior = scrollBehavior,
            actions = {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Pindai Musik"
                    )
                }
            }
        )

        // Bilah Pencarian (hanya untuk tab lokal)
        if (selectedTab != LibraryTab.JAMENDO) {
            SearchBar(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Baris Tab: Semua Lagu, Favorit, Playlist, Jamendo (Scrollable agar tidak terpotong)
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == LibraryTab.SONGS,
                onClick = { onTabSelected(LibraryTab.SONGS) },
                text = { Text("Semua Lagu") }
            )
            Tab(
                selected = selectedTab == LibraryTab.FAVORITES,
                onClick = { onTabSelected(LibraryTab.FAVORITES) },
                text = { Text("Favorit") }
            )
            Tab(
                selected = selectedTab == LibraryTab.PLAYLISTS,
                onClick = { onTabSelected(LibraryTab.PLAYLISTS) },
                text = { Text("Playlist") }
            )
            Tab(
                selected = selectedTab == LibraryTab.JAMENDO,
                onClick = { onTabSelected(LibraryTab.JAMENDO) },
                text = { Text("Jamendo") }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Cari lagu, artis, album…") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Cari")
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = modifier
    )
}

// ─── Song List ──────────────────────────────────────────────────────────────────

@Composable
private fun SongList(
    songs: List<SongEntity>,
    currentSongId: Long?,
    favoriteSongIds: Set<Long>,
    sortOrder: SongSortOrder = SongSortOrder.TITLE_AZ,
    onSortOrderChanged: (SongSortOrder) -> Unit = {},
    contentPadding: PaddingValues,
    onSongClick: (SongEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        // Baris Header: Jumlah lagu dan Pilihan Urutan (A-Z, Baru Ditambah, dll.)
        item(key = "sort_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${songs.size} Lagu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == SongSortOrder.TITLE_AZ,
                        onClick = { onSortOrderChanged(SongSortOrder.TITLE_AZ) },
                        label = { Text("A-Z") },
                        leadingIcon = if (sortOrder == SongSortOrder.TITLE_AZ) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = sortOrder == SongSortOrder.DATE_ADDED,
                        onClick = { onSortOrderChanged(SongSortOrder.DATE_ADDED) },
                        label = { Text("Baru Ditambah") },
                        leadingIcon = if (sortOrder == SongSortOrder.DATE_ADDED) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Pilihan Urutan Lainnya",
                                tint = if (sortOrder == SongSortOrder.TITLE_ZA || sortOrder == SongSortOrder.ARTIST)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nama (A-Z)") },
                                onClick = {
                                    onSortOrderChanged(SongSortOrder.TITLE_AZ)
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortOrder == SongSortOrder.TITLE_AZ) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Nama (Z-A)") },
                                onClick = {
                                    onSortOrderChanged(SongSortOrder.TITLE_ZA)
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortOrder == SongSortOrder.TITLE_ZA) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Baru Ditambah") },
                                onClick = {
                                    onSortOrderChanged(SongSortOrder.DATE_ADDED)
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortOrder == SongSortOrder.DATE_ADDED) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Artis") },
                                onClick = {
                                    onSortOrderChanged(SongSortOrder.ARTIST)
                                    showSortMenu = false
                                },
                                leadingIcon = if (sortOrder == SongSortOrder.ARTIST) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        items(items = songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                isCurrentPlaying = song.id == currentSongId,
                isFavorite = favoriteSongIds.contains(song.id),
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onAddToQueue = { onAddToQueue(song) },
                onPlayNext = { onPlayNext(song) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

@Composable
private fun SongItem(
    song: SongEntity,
    isCurrentPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val titleColor = if (isCurrentPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Thumbnail Album Cover (Coil)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCurrentPlaying) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (song.albumArtUri.isBlank()) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrentPlaying) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Normal,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${formatDuration(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Tombol Cepat Favorit (Hati)
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Hapus favorit" else "Tambah favorit",
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        // Tombol Menu Opsi
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu Opsi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Putar berikutnya") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                    onClick = {
                        onPlayNext()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tambah ke antrean") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        onAddToQueue()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Tambah ke playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        onAddToPlaylist()
                        showMenu = false
                    }
                )
            }
        }
    }
}

// ─── Konten Favorit ─────────────────────────────────────────────────────────────

@Composable
private fun FavoritesContent(
    favoriteSongs: List<SongEntity>,
    currentSongId: Long?,
    favoriteSongIds: Set<Long>,
    contentPadding: PaddingValues,
    onSongClick: (SongEntity) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Putar Semua (${favoriteSongs.size})")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = onShuffleAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acak")
                }
            }
        }

        items(items = favoriteSongs, key = { it.id }) { song ->
            SongItem(
                song = song,
                isCurrentPlaying = song.id == currentSongId,
                isFavorite = favoriteSongIds.contains(song.id),
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onAddToQueue = { onAddToQueue(song) },
                onPlayNext = { onPlayNext(song) },
                onAddToPlaylist = { onAddToPlaylist(song) }
            )
        }
    }
}

// ─── Konten Playlist ────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsContent(
    playlists: List<PlaylistWithCount>,
    contentPadding: PaddingValues,
    onCreateClick: () -> Unit,
    onPlaylistClick: (PlaylistWithCount) -> Unit,
    onDeletePlaylist: (PlaylistWithCount) -> Unit,
    onRenamePlaylist: (PlaylistWithCount) -> Unit = {}
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Tombol Tambah Playlist
            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buat Playlist Baru",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum ada playlist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketuk tombol di atas untuk membuat playlist pertama Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = playlists, key = { it.id }) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onPlaylistClick(playlist) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${playlist.songCount} lagu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onRenamePlaylist(playlist) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ganti Nama Playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onDeletePlaylist(playlist) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Mini Player ────────────────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(
    playbackState: PlaybackState,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    val progress = if (playbackState.duration > 0) {
        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    if (song.albumArtUri.isBlank()) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPrevClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Lagu Sebelumnya"
                    )
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Lagu Berikutnya"
                    )
                }
            }
        }
    }
}

// ─── Empty & Loading States ─────────────────────────────────────────────────────

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}

enum class AppBottomTab {
    LIBRARY,
    EXPLORE,
    SETTINGS
}

@Composable
private fun PermissionEmptyState(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Izin Akses Musik Diperlukan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MusikKu memerlukan izin membaca penyimpanan audio agar dapat memindai dan memutar file lagu di perangkat Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRequestPermission) {
                    Text("Minta Izin")
                }
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pengaturan HP")
                }
            }
        }
    }
}

@Composable
private fun PlaceholderTabContent(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Placeholder untuk navigasi tahap berikutnya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (query.isBlank()) "Belum ada musik di perangkat"
                       else "Tidak ditemukan untuk \"$query\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.isBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pindai Ulang")
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Belum ada lagu favorit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ketuk ikon hati pada lagu mana pun untuk menambahkannya ke daftar favorit Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Util ───────────────────────────────────────────────────────────────────────

/** Format milidetik ke "m:ss" atau "h:mm:ss". */
private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
