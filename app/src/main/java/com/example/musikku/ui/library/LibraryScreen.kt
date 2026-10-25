package com.example.musikku.ui.library

import android.Manifest
import com.example.musikku.data.remote.youtube.toSongEntity
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
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.musikku.ui.theme.MintGreen
import com.example.musikku.ui.theme.GlassSurface
import com.example.musikku.ui.theme.GlassBorder
import com.example.musikku.ui.theme.GlassEmeraldActive
import com.example.musikku.ui.theme.GlassActiveBorder
import com.example.musikku.ui.player.ModernFloatingMiniPlayer
import com.example.musikku.ui.player.MeshGradientBackground
import com.example.musikku.ui.player.rememberArtworkMeshPalette
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
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
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()


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
    var pendingYouTubeTrackForPlaylist by remember { mutableStateOf<com.example.musikku.data.remote.youtube.YouTubeSearchItemDto?>(null) }

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

    val focusManager = LocalFocusManager.current

    // Pastikan keyboard tidak terbuka otomatis saat aplikasi pertama kali dibuka
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.userMessageEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val meshPalette = rememberArtworkMeshPalette(playbackState.currentSong?.albumArtUri)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        // Layer 1: Ambient Mesh Gradient Background ala Convx / Apple Music
        MeshGradientBackground(
            palette = meshPalette,
            trackKey = playbackState.currentSong?.id,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim gelap lembut agar teks dan kartu kontras maksimal
        Surface(
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxSize()
        ) {}

        Scaffold(
            containerColor = Color.Transparent,
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
                Column(modifier = Modifier.background(Color.Transparent)) {
                    if (playbackState.currentSong != null) {
                        val currentSongId = playbackState.currentSong?.id ?: -1L
                        ModernFloatingMiniPlayer(
                            playbackState = playbackState,
                            isFavorite = uiState.favoriteSongIds.contains(currentSongId),
                            onToggleFavorite = { viewModel.toggleFavorite(currentSongId) },
                            onClick = { showNowPlaying = true },
                            onPlayPauseClick = viewModel::playPause,
                            onNext = viewModel::skipToNext,
                            onPrevious = viewModel::skipToPrevious
                        )
                    }
                    ModernBottomNavigationBar(
                        selectedTab = selectedBottomTab,
                        onTabSelected = { tab ->
                            selectedBottomTab = tab
                            if (tab == AppBottomTab.EXPLORE) {
                                viewModel.loadTrendingYouTube()
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
                currentLanguage = uiState.appLanguage,
                downloadOnlyWifi = uiState.downloadOnlyWifi,
                sleepTimerSeconds = playbackState.sleepTimerRemainingSeconds,
                contentPadding = innerPadding,
                onThemeModeChanged = viewModel::setThemeMode,
                onLanguageChanged = viewModel::setAppLanguage,
                onToggleDownloadOnlyWifi = viewModel::toggleDownloadOnlyWifi,
                onSetSleepTimer = viewModel::setSleepTimer,
                onCancelSleepTimer = viewModel::cancelSleepTimer
            )
        } else if (selectedBottomTab == AppBottomTab.EXPLORE) {
            com.example.musikku.ui.explore.ExploreScreen(
                currentTab = uiState.exploreSourceTab,
                onTabSelected = viewModel::setExploreSourceTab,
                youTubeState = uiState.youTubeState,
                jamendoState = uiState.jamendoState,
                currentPlayingTitle = playbackState.currentSong?.title,
                currentPlayingUri = playbackState.currentSong?.contentUri,
                isPlaying = playbackState.isPlaying,
                isOnline = uiState.isOnline,
                downloadOnlyWifi = uiState.downloadOnlyWifi,
                favoriteSongIds = uiState.favoriteSongIds,
                contentPadding = innerPadding,
                onYouTubeSearch = viewModel::searchYouTube,
                onYouTubePlay = viewModel::playYouTubeTrack,
                onYouTubeAddToQueue = viewModel::addYouTubeTrackToQueue,
                onYouTubeAddToPlaylist = { track -> pendingYouTubeTrackForPlaylist = track },
                onYouTubeToggleFavorite = viewModel::toggleYouTubeFavorite,
                onYouTubeDownload = viewModel::downloadYouTubeTrack,
                onJamendoSearch = viewModel::searchJamendo,
                onJamendoStream = viewModel::playJamendoTrack,
                onJamendoDownload = viewModel::downloadJamendoTrack,
                onJamendoDeleteDownload = viewModel::deleteDownloadedTrack,
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
                    favoriteCount = uiState.favoriteSongs.size,
                    onOpenFavorites = { viewModel.selectTab(LibraryTab.FAVORITES) },
                    onPlayFavorites = { if (uiState.favoriteSongs.isNotEmpty()) viewModel.playAll(uiState.favoriteSongs, 0) },
                    contentPadding = innerPadding,
                    onCreateClick = { showCreatePlaylistDialog = true },
                    onPlaylistClick = { selectedPlaylistForDetail = it },
                    onDeletePlaylist = { viewModel.deletePlaylist(it.id) },
                    onRenamePlaylist = { playlistForRename = it }
                )
            }

            uiState.selectedTab == LibraryTab.JAMENDO -> {
                com.example.musikku.ui.explore.ExploreScreen(
                    currentTab = uiState.exploreSourceTab,
                    onTabSelected = viewModel::setExploreSourceTab,
                    youTubeState = uiState.youTubeState,
                    jamendoState = uiState.jamendoState,
                    currentPlayingTitle = playbackState.currentSong?.title,
                    currentPlayingUri = playbackState.currentSong?.contentUri,
                    isPlaying = playbackState.isPlaying,
                    isOnline = uiState.isOnline,
                    downloadOnlyWifi = uiState.downloadOnlyWifi,
                    contentPadding = innerPadding,
                    onYouTubeSearch = viewModel::searchYouTube,
                    onYouTubePlay = viewModel::playYouTubeTrack,
                    onYouTubeAddToQueue = viewModel::addYouTubeTrackToQueue,
                    onYouTubeAddToPlaylist = { track -> pendingYouTubeTrackForPlaylist = track },
                    onJamendoSearch = viewModel::searchJamendo,
                    onJamendoStream = viewModel::playJamendoTrack,
                    onJamendoDownload = viewModel::downloadJamendoTrack,
                    onJamendoDeleteDownload = viewModel::deleteDownloadedTrack,
                    onToggleDownloadOnlyWifi = viewModel::toggleDownloadOnlyWifi
                )
            }
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

    // Dialog Tambah Lagu YouTube ke Playlist
    pendingYouTubeTrackForPlaylist?.let { track ->
        val dummySong = track.toSongEntity()
        AddToPlaylistDialog(
            song = dummySong,
            playlists = uiState.playlists,
            onDismiss = { pendingYouTubeTrackForPlaylist = null },
            onAddToPlaylist = { playlistId ->
                viewModel.addYouTubeTrackToPlaylist(track, playlistId)
                pendingYouTubeTrackForPlaylist = null
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
            onDeleteSong = { songToDelete = it },
            onDownload = viewModel::downloadCurrentPlayingSong,
            lyricsState = lyricsState,
            onRetryLyrics = { viewModel.fetchLyrics() }
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
    Surface(
        color = Color(0xF508140E),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
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
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), CircleShape)
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

        // ─── Pill Search Bar (Translucent Glass) ──────────────────────────
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
                label = stringResource(R.string.library_songs),
                isSelected = selectedTab == LibraryTab.SONGS,
                onClick = { onTabSelected(LibraryTab.SONGS) }
            )
            FilterPill(
                label = stringResource(R.string.library_favorites),
                isSelected = selectedTab == LibraryTab.FAVORITES,
                onClick = { onTabSelected(LibraryTab.FAVORITES) }
            )
            FilterPill(
                label = stringResource(R.string.library_playlists),
                isSelected = selectedTab == LibraryTab.PLAYLISTS,
                onClick = { onTabSelected(LibraryTab.PLAYLISTS) }
            )
            FilterPill(
                label = stringResource(R.string.explore_jamendo),
                isSelected = selectedTab == LibraryTab.JAMENDO,
                onClick = { onTabSelected(LibraryTab.JAMENDO) }
            )
        }
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
            .background(if (isSelected) MintGreen else Color.White.copy(alpha = 0.08f))
            .border(
                BorderStroke(1.dp, if (isSelected) MintGreen else Color.White.copy(alpha = 0.14f)),
                RoundedCornerShape(50)
            )
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
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(50))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            placeholder = {
                Text(
                    text = stringResource(R.string.library_search_hint),
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
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MintGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
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
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
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

                // Tombol Acak Bulat Glass
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), CircleShape)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) Color(0x381C4830) else Color.White.copy(alpha = 0.07f)
        ),
        border = if (isCurrentPlaying) {
            BorderStroke(1.5.dp, MintGreen.copy(alpha = 0.85f))
        } else {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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
                    .background(Color.White.copy(alpha = 0.08f))
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
                    color = if (isCurrentPlaying) MintGreen else Color.White,
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
                    tint = MintGreen,
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

// ─── Konten Favorit Sesuai Desain Glass ──────────────────────────────────────────

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
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        // Header Tindakan: Putar semua, Acak, Jumlah Lagu
        item(key = "fav_action_header") {
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
                        .clickable(onClick = onPlayAll)
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

                // Tombol Acak Bulat Glass
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), CircleShape)
                        .clickable(onClick = onShuffleAll)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak Semua",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${favoriteSongs.size} lagu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
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

// ─── Konten Playlist Sesuai Image 4 ─────────────────────────────────────────────

@Composable
private fun PlaylistGridCover(name: String, modifier: Modifier = Modifier) {
    val hash = name.hashCode()
    val palette = listOf(
        Color(0xFF4EE1A0),
        Color(0xFFFF9566),
        Color(0xFF8E7CFF),
        Color(0xFFFFD166),
        Color(0xFF5BA4E6),
        Color(0xFFFF6B8B),
        Color(0xFF2EC4B6),
        Color(0xFFB388FF)
    )
    val c1 = palette[kotlin.math.abs(hash) % palette.size]
    val c2 = palette[kotlin.math.abs(hash shr 2) % palette.size]
    val c3 = palette[kotlin.math.abs(hash shr 4) % palette.size]
    val c4 = palette[kotlin.math.abs(hash shr 6) % palette.size]

    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(topStart = 6.dp)).background(c1))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(topEnd = 6.dp)).background(c2))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(bottomStart = 6.dp)).background(c3))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp).clip(RoundedCornerShape(bottomEnd = 6.dp)).background(c4))
            }
        }
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<PlaylistWithCount>,
    favoriteCount: Int,
    onOpenFavorites: () -> Unit,
    onPlayFavorites: () -> Unit,
    contentPadding: PaddingValues,
    onCreateClick: () -> Unit,
    onPlaylistClick: (PlaylistWithCount) -> Unit,
    onDeletePlaylist: (PlaylistWithCount) -> Unit,
    onRenamePlaylist: (PlaylistWithCount) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(
            bottom = contentPadding.calculateBottomPadding() + 80.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        // Header "Playlist kamu" + "+ Buat baru" Pill (Image 4)
        item(key = "playlist_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Playlist kamu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 24.sp
                )

                // Tombol "+ Buat baru" (Lime / Mint Green Pill)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFBAFF5C))
                        .clickable(onClick = onCreateClick)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Buat baru",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Kartu Unggulan: "Lagu favorit" (Coral/Peach Gradient Card Sesuai Image 4)
        item(key = "featured_favorite_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onOpenFavorites),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB07C), Color(0xFFFF7A7A))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Kotak Ikon Hati
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFF1E1410),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lagu favorit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1410)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$favoriteCount lagu, otomatis",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3E2018)
                            )
                        }

                        // Tombol Play Lingkaran Hitam
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1410))
                                .clickable(onClick = onPlayFavorites)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Putar Lagu Favorit",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daftar Playlist dalam Translucent Glass Cards
        if (playlists.isEmpty()) {
            item(key = "empty_playlists") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada playlist kustom",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(items = playlists, key = { it.id }) { playlist ->
                var showOptionsDropdown by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clickable { onPlaylistClick(playlist) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.07f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        // 4-Grid Colorful Squircle Cover Sesuai Image 4
                        PlaylistGridCover(name = playlist.name)

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${playlist.songCount} lagu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }

                        // Three dots menu
                        Box {
                            IconButton(onClick = { showOptionsDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opsi Playlist",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsDropdown,
                                onDismissRequest = { showOptionsDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ganti Nama") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showOptionsDropdown = false
                                        onRenamePlaylist(playlist)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hapus Playlist", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOptionsDropdown = false
                                        onDeletePlaylist(playlist)
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 4.dp)
            .navigationBarsPadding()
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xD9102218), // Frosted dark emerald glass
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Library
                BottomNavTabItem(
                    label = stringResource(R.string.nav_library),
                    icon = Icons.Default.LibraryMusic,
                    isSelected = selectedTab == AppBottomTab.LIBRARY,
                    onClick = { onTabSelected(AppBottomTab.LIBRARY) }
                )

                // Tab 2: Jelajah
                BottomNavTabItem(
                    label = stringResource(R.string.nav_explore),
                    icon = Icons.Default.Explore,
                    isSelected = selectedTab == AppBottomTab.EXPLORE,
                    onClick = { onTabSelected(AppBottomTab.EXPLORE) }
                )

                // Tab 3: Pengaturan
                BottomNavTabItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Default.Tune,
                    isSelected = selectedTab == AppBottomTab.SETTINGS,
                    onClick = { onTabSelected(AppBottomTab.SETTINGS) }
                )
            }
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
        // Active capsule pill: Soft green translucent glass with green border
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF67E8A6).copy(alpha = 0.18f))
                .border(BorderStroke(1.2.dp, Color(0xFF67E8A6).copy(alpha = 0.55f)), RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF67E8A6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF67E8A6)
                )
            }
        }
    } else {
        // Inactive: Ikon dan teks halus transparan
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
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
