package com.example.musikku.ui.library

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.common.LiveEqualizerIndicator
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.unit.sp
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.JetBlack
import com.example.musikku.ui.theme.JetSurface
import com.example.musikku.ui.theme.JetCard
import com.example.musikku.ui.theme.JetCardHigh
import com.example.musikku.ui.theme.JetPill
import com.example.musikku.ui.theme.JetBorder
import com.example.musikku.ui.theme.TextWhite
import com.example.musikku.ui.theme.TextMuted
import com.example.musikku.ui.theme.FavoriteRed
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
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }
    var pendingSystemDeleteSong by remember { mutableStateOf<SongEntity?>(null) }

    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val song = pendingSystemDeleteSong
        if (result.resultCode == Activity.RESULT_OK && song != null) {
            viewModel.deleteSong(song)
            Toast.makeText(context, "Lagu \"${song.title}\" berhasil dihapus", Toast.LENGTH_SHORT).show()
        } else if (song != null) {
            Toast.makeText(context, "Penghapusan lagu dibatalkan", Toast.LENGTH_SHORT).show()
        }
        pendingSystemDeleteSong = null
    }

    val executeDeleteSong: (SongEntity) -> Unit = { song ->
        val uri = try { Uri.parse(song.contentUri) } catch (e: Exception) { null }
        if (song.source == "DOWNLOADED" || (song.filePath != null && !song.filePath.startsWith("/storage/emulated/0/"))) {
            viewModel.deleteSong(song)
            Toast.makeText(context, "Lagu \"${song.title}\" berhasil dihapus", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri != null && uri.scheme == "content") {
            try {
                pendingSystemDeleteSong = song
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                deleteIntentLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (e: Exception) {
                try {
                    val count = context.contentResolver.delete(uri, null, null)
                    if (count > 0) {
                        viewModel.deleteSong(song)
                        Toast.makeText(context, "Lagu \"${song.title}\" berhasil dihapus", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.deleteSong(song)
                        Toast.makeText(context, "Lagu dihapus dari daftar", Toast.LENGTH_SHORT).show()
                    }
                } catch (sec: Exception) {
                    viewModel.deleteSong(song)
                    Toast.makeText(context, "Lagu dihapus dari daftar", Toast.LENGTH_SHORT).show()
                }
                pendingSystemDeleteSong = null
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null && uri.scheme == "content") {
            try {
                val count = context.contentResolver.delete(uri, null, null)
                if (count > 0) {
                    viewModel.deleteSong(song)
                    Toast.makeText(context, "Lagu \"${song.title}\" berhasil dihapus", Toast.LENGTH_SHORT).show()
                }
            } catch (sec: SecurityException) {
                if (sec is RecoverableSecurityException) {
                    pendingSystemDeleteSong = song
                    deleteIntentLauncher.launch(
                        IntentSenderRequest.Builder(sec.userAction.actionIntent.intentSender).build()
                    )
                } else {
                    Toast.makeText(context, "Izin ditolak untuk menghapus file", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            try {
                if (uri != null && uri.scheme == "content") {
                    context.contentResolver.delete(uri, null, null)
                }
                viewModel.deleteSong(song)
                Toast.makeText(context, "Lagu \"${song.title}\" berhasil dihapus", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menghapus lagu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.userMessageEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = JetBlack,
        topBar = {
            if (selectedBottomTab == AppBottomTab.LIBRARY) {
                LibraryTopBar(
                    searchQuery = uiState.searchQuery,
                    selectedTab = uiState.selectedTab,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onTabSelected = { tab ->
                        if (tab == LibraryTab.JAMENDO) {
                            selectedBottomTab = AppBottomTab.EXPLORE
                            viewModel.loadPopularJamendo()
                        } else {
                            viewModel.selectTab(tab)
                        }
                    },
                    onRefreshClick = viewModel::refreshLibrary
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(JetBlack)) {
                if (playbackState.currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        onClick = { showNowPlaying = true },
                        onPlayPauseClick = viewModel::playPause
                    )
                }
                ModernBottomNavigationBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { tab ->
                        selectedBottomTab = tab
                        if (tab == AppBottomTab.EXPLORE) {
                            viewModel.loadPopularJamendo()
                        }
                    }
                )
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
                        isPlaying = playbackState.isPlaying,
                        favoriteSongIds = uiState.favoriteSongIds,
                        sortOrder = uiState.sortOrder,
                        onSortOrderChanged = viewModel::setSortOrder,
                        contentPadding = innerPadding,
                        onSongClick = onSongClick,
                        onPlayAll = { songs, index -> viewModel.playAll(songs, index) },
                        onShuffleAll = { songs -> viewModel.playAll(songs.shuffled(), 0) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onPlayNext = viewModel::playNext,
                        onAddToPlaylist = { songForAddToPlaylist = it },
                        onDeleteSong = { songToDelete = it }
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
                        isPlaying = playbackState.isPlaying,
                        favoriteSongIds = uiState.favoriteSongIds,
                        contentPadding = innerPadding,
                        onSongClick = { viewModel.playSong(it, uiState.favoriteSongs) },
                        onPlayAll = { viewModel.playAll(uiState.favoriteSongs, 0) },
                        onShuffleAll = { viewModel.playAll(uiState.favoriteSongs.shuffled(), 0) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToQueue = viewModel::addToQueue,
                        onPlayNext = viewModel::playNext,
                        onAddToPlaylist = { songForAddToPlaylist = it },
                        onDeleteSong = { songToDelete = it }
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
            },
            onDeleteSong = { songToDelete = it }
        )
    }

    // Dialog Konfirmasi Hapus Lagu dari Perangkat
    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hapus Lagu?",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus lagu ini dari penyimpanan perangkat?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${song.title}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricLime
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "File akan dihapus secara permanen dari perangkat dan tidak dapat dipulihkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = song
                        songToDelete = null
                        executeDeleteSong(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Batal", color = Color.White)
                }
            },
            containerColor = JetCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ─── Top Bar & Tabs Sesuai Image 1 ───────────────────────────────────────────

@Composable
private fun LibraryTopBar(
    searchQuery: String,
    selectedTab: LibraryTab,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (LibraryTab) -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JetBlack)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // ─── Header: KOLEKSIMU / Library + Circular Refresh Button ───────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KOLEKSIMU",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(JetPill)
                    .clickable(onClick = onRefreshClick)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Pindai Musik",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── Pill Search Bar ──────────────────────────────────────────────
        if (selectedTab != LibraryTab.JAMENDO) {
            SearchBar(
                query = searchQuery,
                onQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ─── Filter Pills Row: Semua Lagu, Favorit, Playlist, Jamendo ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                label = "Semua Lagu",
                isSelected = selectedTab == LibraryTab.SONGS,
                onClick = { onTabSelected(LibraryTab.SONGS) }
            )
            FilterPill(
                label = "Favorit",
                isSelected = selectedTab == LibraryTab.FAVORITES,
                onClick = { onTabSelected(LibraryTab.FAVORITES) }
            )
            FilterPill(
                label = "Playlist",
                isSelected = selectedTab == LibraryTab.PLAYLISTS,
                onClick = { onTabSelected(LibraryTab.PLAYLISTS) }
            )
            FilterPill(
                label = "Jamendo",
                isSelected = selectedTab == LibraryTab.JAMENDO,
                onClick = { onTabSelected(LibraryTab.JAMENDO) }
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) ElectricLime else JetPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) OnElectricLime else Color.White
        )
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
        placeholder = {
            Text(
                text = "Cari lagu, artis, album",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Hapus pencarian",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = JetCard,
            unfocusedContainerColor = JetCard,
            disabledContainerColor = JetCard,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = ElectricLime
        ),
        modifier = modifier.height(50.dp)
    )
}

// ─── Song List Sesuai Image 1 ───────────────────────────────────────────────────

@Composable
private fun SongList(
    songs: List<SongEntity>,
    currentSongId: Long?,
    isPlaying: Boolean = false,
    favoriteSongIds: Set<Long>,
    sortOrder: SongSortOrder = SongSortOrder.TITLE_AZ,
    onSortOrderChanged: (SongSortOrder) -> Unit = {},
    contentPadding: PaddingValues,
    onSongClick: (SongEntity) -> Unit,
    onPlayAll: (List<SongEntity>, Int) -> Unit = { _, _ -> },
    onShuffleAll: (List<SongEntity>) -> Unit = {},
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit = {}
) {
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize()
    ) {
        // ─── Header Tindakan: Putar semua, Acak, Pilihan Urutan (A-Z) ───────
        item(key = "action_header") {
            var showSortMenu by remember { mutableStateOf(false) }
            val sortLabel = when (sortOrder) {
                SongSortOrder.TITLE_AZ -> "A–Z"
                SongSortOrder.DATE_ADDED -> "Baru"
                SongSortOrder.TITLE_ZA -> "Z–A"
                SongSortOrder.ARTIST -> "Artis"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol "Putar semua" (White Capsule)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable(onClick = { if (songs.isNotEmpty()) onPlayAll(songs, 0) })
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Putar semua",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Tombol Acak Bulat Gelap
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(JetPill)
                        .clickable(onClick = { if (songs.isNotEmpty()) onShuffleAll(songs) })
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak Semua",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Indikator `${songs.size} lagu · A–Z`
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${songs.size} lagu · $sortLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
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
                                { Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Baru Ditambah") },
                            onClick = {
                                onSortOrderChanged(SongSortOrder.DATE_ADDED)
                                showSortMenu = false
                            },
                            leadingIcon = if (sortOrder == SongSortOrder.DATE_ADDED) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Nama (Z-A)") },
                            onClick = {
                                onSortOrderChanged(SongSortOrder.TITLE_ZA)
                                showSortMenu = false
                            },
                            leadingIcon = if (sortOrder == SongSortOrder.TITLE_ZA) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime) }
                            } else null
                        )
                        DropdownMenuItem(
                            text = { Text("Artis") },
                            onClick = {
                                onSortOrderChanged(SongSortOrder.ARTIST)
                                showSortMenu = false
                            },
                            leadingIcon = if (sortOrder == SongSortOrder.ARTIST) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = ElectricLime) }
                            } else null
                        )
                    }
                }
            }
        }

        items(items = songs, key = { it.id }) { song ->
            val isCurrent = song.id == currentSongId
            SongItem(
                song = song,
                isCurrentPlaying = isCurrent,
                isPlaying = isCurrent && isPlaying,
                isFavorite = favoriteSongIds.contains(song.id),
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onAddToQueue = { onAddToQueue(song) },
                onPlayNext = { onPlayNext(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onDeleteFromDevice = { onDeleteSong(song) }
            )
        }
    }
}

@Composable
private fun SongItem(
    song: SongEntity,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDeleteFromDevice: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    var isImageError by remember(song.albumArtUri) { mutableStateOf(false) }
    val showPlaceholder = song.albumArtUri.isBlank() || isImageError

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) JetCardHigh else JetCard
        ),
        border = if (isCurrentPlaying) {
            BorderStroke(1.dp, ElectricLime.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, JetBorder.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Thumbnail Album Cover (Squircle 52.dp, rounded 14.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(JetPill)
            ) {
                if (!showPlaceholder) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onError = { isImageError = true },
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    AlbumArtPlaceholder(
                        seed = song.title,
                        modifier = Modifier.matchParentSize(),
                        iconSize = 24.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentPlaying) ElectricLime else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${song.artist} · ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indikator Live Equalizer saat sedang memutar lagu
            if (isCurrentPlaying) {
                LiveEqualizerIndicator(
                    isPlaying = isPlaying,
                    tint = ElectricLime,
                    maxHeight = 16.dp,
                    barWidth = 2.5.dp,
                    spacing = 2.dp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Tombol Cepat Favorit (Hati Coral Red / Subtle Outline)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus favorit" else "Tambah favorit",
                    tint = if (isFavorite) FavoriteRed else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tombol Menu Opsi (Antrean, Playlist)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu Opsi",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
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
                    DropdownMenuItem(
                        text = { Text("Hapus dari perangkat", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteFromDevice()
                        }
                    )
                }
            }
        }
    }
}

// ─── Konten Favorit ─────────────────────────────────────────────────────────────

@Composable
private fun FavoritesContent(
    favoriteSongs: List<SongEntity>,
    currentSongId: Long?,
    isPlaying: Boolean = false,
    favoriteSongIds: Set<Long>,
    contentPadding: PaddingValues,
    onSongClick: (SongEntity) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAddToQueue: (SongEntity) -> Unit,
    onPlayNext: (SongEntity) -> Unit,
    onAddToPlaylist: (SongEntity) -> Unit,
    onDeleteSong: (SongEntity) -> Unit = {}
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
            val isCurrent = song.id == currentSongId
            SongItem(
                song = song,
                isCurrentPlaying = isCurrent,
                isPlaying = isCurrent && isPlaying,
                isFavorite = favoriteSongIds.contains(song.id),
                onClick = { onSongClick(song) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onAddToQueue = { onAddToQueue(song) },
                onPlayNext = { onPlayNext(song) },
                onAddToPlaylist = { onAddToPlaylist(song) },
                onDeleteFromDevice = { onDeleteSong(song) }
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
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .clickable { onPlaylistClick(playlist) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { onDeletePlaylist(playlist) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Hapus Playlist",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Mini Player Sesuai Image 1 ─────────────────────────────────────────────────

@Composable
private fun MiniPlayer(
    playbackState: PlaybackState,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    val progress = if (playbackState.duration > 0) {
        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = JetCard),
        border = BorderStroke(1.dp, JetBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                // Thumbnail Album Cover (Squircle 48.dp, rounded 14.dp)
                var isMiniError by remember(song.albumArtUri) { mutableStateOf(false) }
                val showMiniPlaceholder = song.albumArtUri.isBlank() || isMiniError

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(JetPill)
                ) {
                    if (!showMiniPlaceholder) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onError = { isMiniError = true },
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} · ${formatDuration(playbackState.currentPosition)} / ${formatDuration(playbackState.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Bulat Electric Lime Putar/Jeda (46.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ElectricLime)
                        .clickable(onClick = onPlayPauseClick)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Jeda" else "Putar",
                        tint = OnElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Micro-progress bar di bagian bawah
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = ElectricLime,
                trackColor = Color.Transparent
            )
        }
    }
}

// ─── Modern Bottom Navigation Bar Sesuai Image 1 & 3 ───────────────────────────

@Composable
private fun ModernBottomNavigationBar(
    selectedTab: AppBottomTab,
    onTabSelected: (AppBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = JetSurface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Library
            BottomNavTabItem(
                label = "Library",
                icon = Icons.Default.LibraryMusic,
                isSelected = selectedTab == AppBottomTab.LIBRARY,
                onClick = { onTabSelected(AppBottomTab.LIBRARY) }
            )

            // Tab 2: Jelajah
            BottomNavTabItem(
                label = "Jelajah",
                icon = Icons.Default.Explore,
                isSelected = selectedTab == AppBottomTab.EXPLORE,
                onClick = { onTabSelected(AppBottomTab.EXPLORE) }
            )

            // Tab 3: Pengaturan
            BottomNavTabItem(
                label = "Pengaturan",
                icon = Icons.Default.Tune,
                isSelected = selectedTab == AppBottomTab.SETTINGS,
                onClick = { onTabSelected(AppBottomTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        // Active capsule pill: Electric Lime dengan icon & text hitam pekat
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ElectricLime)
                .clickable(onClick = onClick)
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = OnElectricLime,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnElectricLime
                )
            }
        }
    } else {
        // Inactive: Ikon dan teks vertikal halus abu-abu
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
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
