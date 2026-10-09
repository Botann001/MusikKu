package com.example.musikku.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.preferences.SettingsRepository
import com.example.musikku.data.preferences.ThemeMode
import com.example.musikku.data.remote.dto.JamendoTrackDto
import com.example.musikku.data.remote.toSongEntity
import com.example.musikku.data.repository.JamendoRepository
import com.example.musikku.data.repository.MusicRepository
import com.example.musikku.playback.MusicController
import com.example.musikku.playback.PlaybackState
import com.example.musikku.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: MusicRepository,
    val jamendoRepository: JamendoRepository,
    val youTubeMusicRepository: com.example.musikku.data.repository.YouTubeMusicRepository,
    val settingsRepository: SettingsRepository,
    val networkMonitor: NetworkMonitor,
    val musicController: MusicController
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    private val _isLoading = MutableStateFlow(true)
    private val _exploreSourceTab = MutableStateFlow(ExploreSourceTab.YOUTUBE)
    private val _youTubeState = MutableStateFlow(YouTubeSearchState())
    private val _jamendoState = MutableStateFlow(JamendoSearchState())
    private val _sortOrder = MutableStateFlow(SongSortOrder.TITLE_AZ)

    private val songsFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.allSongs
        else repository.searchSongs(query)
    }

    private val favoriteSongIdsFlow = repository.favoriteSongIds.map { it.toSet() }

    private data class RepoData(
        val songs: List<SongEntity>,
        val favoriteSongs: List<SongEntity>,
        val favoriteSongIds: Set<Long>,
        val playlists: List<com.example.musikku.data.local.dao.PlaylistWithCount>
    )

    private val repositoryDataFlow = combine(
        songsFlow,
        repository.favoriteSongs,
        favoriteSongIdsFlow,
        repository.playlistsWithCount
    ) { songs, favSongs, favIds, playlists ->
        RepoData(songs, favSongs, favIds, playlists)
    }

    private data class CombinedTabState(
        val tab: LibraryTab,
        val query: String,
        val exploreSourceTab: ExploreSourceTab,
        val youTubeState: YouTubeSearchState,
        val jamendoState: JamendoSearchState,
        val sortOrder: SongSortOrder,
        val isLoading: Boolean,
        val isOnline: Boolean,
        val downloadOnlyWifi: Boolean,
        val themeMode: ThemeMode
    )

    private val tabQueryFlow = combine(
        combine(_selectedTab, _searchQuery, _exploreSourceTab, _youTubeState) { tab, q, expTab, ytState ->
            listOf(tab, q, expTab, ytState)
        },
        combine(_jamendoState, _sortOrder, _isLoading) { jamendo, sort, loading ->
            listOf(jamendo, sort, loading)
        },
        combine(networkMonitor.isOnline, settingsRepository.downloadOnlyWifi, settingsRepository.themeMode) { online, wifiOnly, theme ->
            listOf(online, wifiOnly, theme)
        }
    ) { list1, list2, list3 ->
        @Suppress("UNCHECKED_CAST")
        CombinedTabState(
            tab = list1[0] as LibraryTab,
            query = list1[1] as String,
            exploreSourceTab = list1[2] as ExploreSourceTab,
            youTubeState = list1[3] as YouTubeSearchState,
            jamendoState = list2[0] as JamendoSearchState,
            sortOrder = list2[1] as SongSortOrder,
            isLoading = list2[2] as Boolean,
            isOnline = list3[0] as Boolean,
            downloadOnlyWifi = list3[1] as Boolean,
            themeMode = list3[2] as ThemeMode
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repositoryDataFlow,
        tabQueryFlow
    ) { repoData, tabState ->
        val sortedSongs = when (tabState.sortOrder) {
            SongSortOrder.TITLE_AZ -> repoData.songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOrder.DATE_ADDED -> repoData.songs.sortedByDescending { it.dateAdded }
            SongSortOrder.TITLE_ZA -> repoData.songs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            SongSortOrder.ARTIST -> repoData.songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
        }

        LibraryUiState(
            songs = sortedSongs,
            favoriteSongs = repoData.favoriteSongs,
            favoriteSongIds = repoData.favoriteSongIds,
            playlists = repoData.playlists,
            selectedTab = tabState.tab,
            searchQuery = tabState.query,
            exploreSourceTab = tabState.exploreSourceTab,
            youTubeState = tabState.youTubeState,
            jamendoState = tabState.jamendoState,
            isOnline = tabState.isOnline,
            downloadOnlyWifi = tabState.downloadOnlyWifi,
            themeMode = tabState.themeMode,
            sortOrder = tabState.sortOrder,
            isLoading = tabState.isLoading,
            isEmpty = !tabState.isLoading && when (tabState.tab) {
                LibraryTab.SONGS -> sortedSongs.isEmpty()
                LibraryTab.FAVORITES -> repoData.favoriteSongs.isEmpty()
                LibraryTab.PLAYLISTS -> repoData.playlists.isEmpty()
                LibraryTab.JAMENDO -> tabState.jamendoState.results.isEmpty() &&
                        tabState.jamendoState.popularTracks.isEmpty() &&
                        !tabState.jamendoState.isSearching
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    /** State pemutaran musik lengkap dari MusicController */
    val playbackState: StateFlow<PlaybackState> = musicController.playbackState

    init {
        refreshLibrary()

        // Pantau progres unduhan dari WorkManager
        viewModelScope.launch {
            jamendoRepository.downloadProgressMap.collect { progressMap ->
                _jamendoState.update { it.copy(downloadProgress = progressMap) }
            }
        }

        // Pantau ID lagu-lagu yang telah terunduh ke Room
        viewModelScope.launch {
            jamendoRepository.downloadedSongIds.collect { downloadedIds ->
                _jamendoState.update { it.copy(downloadedIds = downloadedIds.toSet()) }
            }
        }

        // Muat lagu populer awal untuk Jamendo dan YouTube Music
        loadPopularJamendo()
        loadTrendingYouTube()
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.update { tab }
    }

    fun setSortOrder(order: SongSortOrder) {
        _sortOrder.update { order }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _isLoading.update { true }
            repository.refreshFromDevice()
            _isLoading.update { false }
        }
    }

    // ─── Jamendo (Pencarian, Populer, Streaming, Unduhan) ────────────────────────

    fun onJamendoQueryChanged(query: String) {
        _jamendoState.update { it.copy(query = query) }
        if (query.isBlank()) {
            _jamendoState.update { it.copy(results = emptyList(), errorMessage = null) }
        }
    }

    fun loadPopularJamendo() {
        viewModelScope.launch {
            _jamendoState.update { it.copy(isSearching = true, errorMessage = null) }
            val result = jamendoRepository.getPopularTracks()
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuJamendo", "Loaded popular tracks: ${tracks.size}")
                _jamendoState.update {
                    it.copy(
                        popularTracks = tracks,
                        isSearching = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuJamendo", "Failed to load popular tracks", error)
                _jamendoState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Gagal memuat lagu populer: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    fun searchJamendo(query: String) {
        val queryClean = query.trim()
        if (queryClean.isBlank()) {
            loadPopularJamendo()
            return
        }

        viewModelScope.launch {
            _jamendoState.update { it.copy(query = queryClean, isSearching = true, errorMessage = null) }
            val result = jamendoRepository.searchTracks(queryClean)
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuJamendo", "Search for '$queryClean' returned: ${tracks.size} tracks")
                _jamendoState.update {
                    it.copy(
                        results = tracks,
                        isSearching = false,
                        errorMessage = if (tracks.isEmpty()) "Tidak ada hasil untuk \"$queryClean\"" else null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuJamendo", "Search for '$queryClean' failed", error)
                _jamendoState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Gagal memuat: ${error.localizedMessage ?: "Koneksi internet bermasalah"}"
                    )
                }
            }
        }
    }

    // ─── Operasi YouTube Music ─────────────────────────────────────────────────

    fun setExploreSourceTab(tab: ExploreSourceTab) {
        _exploreSourceTab.update { tab }
    }

    fun loadTrendingYouTube() {
        viewModelScope.launch {
            _youTubeState.update { it.copy(isSearching = true, errorMessage = null) }
            val result = youTubeMusicRepository.getTrendingMusic()
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuYouTube", "Loaded trending tracks: ${tracks.size}")
                _youTubeState.update {
                    it.copy(
                        trendingTracks = tracks,
                        isSearching = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuYouTube", "Failed to load trending", error)
                _youTubeState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Gagal memuat musik trending: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    fun searchYouTube(query: String) {
        val queryClean = query.trim()
        if (queryClean.isBlank()) {
            loadTrendingYouTube()
            return
        }

        viewModelScope.launch {
            _youTubeState.update { it.copy(query = queryClean, isSearching = true, errorMessage = null) }
            val result = youTubeMusicRepository.searchMusic(queryClean)
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuYouTube", "Search for '$queryClean' returned: ${tracks.size} tracks")
                _youTubeState.update {
                    it.copy(
                        results = tracks,
                        isSearching = false,
                        errorMessage = if (tracks.isEmpty()) "Tidak ada hasil untuk \"$queryClean\"" else null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuYouTube", "Search for '$queryClean' failed", error)
                _youTubeState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "Gagal mencari di YouTube: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    fun playYouTubeTrack(track: com.example.musikku.data.remote.youtube.YouTubeSearchItemDto) {
        viewModelScope.launch {
            _youTubeState.update { it.copy(isLoadingStreamId = track.videoId) }
            val result = youTubeMusicRepository.resolveAudioStream(track)
            _youTubeState.update { it.copy(isLoadingStreamId = null) }
            result.onSuccess { song ->
                musicController.playSongs(listOf(song), 0)
            }.onFailure { error ->
                musicController.postUserMessage("Gagal memutar audio YouTube: ${error.localizedMessage ?: "Format tidak didukung"}")
            }
        }
    }

    fun addYouTubeTrackToQueue(track: com.example.musikku.data.remote.youtube.YouTubeSearchItemDto) {
        viewModelScope.launch {
            _youTubeState.update { it.copy(isLoadingStreamId = track.videoId) }
            val result = youTubeMusicRepository.resolveAudioStream(track)
            _youTubeState.update { it.copy(isLoadingStreamId = null) }
            result.onSuccess { song ->
                musicController.addToQueue(song)
                musicController.postUserMessage("Ditambahkan ke antrean: ${song.title}")
            }.onFailure { error ->
                musicController.postUserMessage("Gagal menambahkan ke antrean: ${error.localizedMessage}")
            }
        }
    }

    fun addYouTubeTrackToPlaylist(track: com.example.musikku.data.remote.youtube.YouTubeSearchItemDto, playlistId: Long) {
        viewModelScope.launch {
            _youTubeState.update { it.copy(isLoadingStreamId = track.videoId) }
            val result = youTubeMusicRepository.resolveAudioStream(track)
            _youTubeState.update { it.copy(isLoadingStreamId = null) }
            result.onSuccess { song ->
                repository.addSongToPlaylist(playlistId, song.id)
                musicController.postUserMessage("Lagu YouTube ditambahkan ke playlist")
            }.onFailure { error ->
                musicController.postUserMessage("Gagal menambahkan ke playlist: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Memutar lagu Jamendo secara langsung via streaming online menggunakan ExoPlayer.
     */
    fun playJamendoTrack(track: JamendoTrackDto) {
        val song = track.toSongEntity()
        musicController.playSongs(listOf(song), 0)
    }

    /**
     * Mengunduh lagu Jamendo di latar belakang via WorkManager.
     */
    fun downloadJamendoTrack(track: JamendoTrackDto) {
        viewModelScope.launch {
            val safeId = track.id.toLongOrNull() ?: (track.id.hashCode().toLong().let { if (it < 0) -it else it })
            _jamendoState.update { it.copy(downloadingIds = it.downloadingIds + safeId) }
            jamendoRepository.downloadTrack(track)
        }
    }

    /**
     * Hapus lagu yang telah diunduh dari memori aplikasi dan database Room.
     */
    fun deleteDownloadedTrack(songId: Long) {
        viewModelScope.launch {
            jamendoRepository.deleteDownloadedTrack(songId)
            musicController.onSongDeleted(songId)
        }
    }

    /**
     * Hapus lagu dari penyimpanan fisik dan database Room.
     * Jika lagu sedang diputar, hentikan atau lewati pemutaran secara aman.
     */
    fun deleteSong(song: SongEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!song.filePath.isNullOrBlank()) {
                try {
                    val file = File(song.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            repository.deleteSong(song.id)
            musicController.onSongDeleted(song.id)
        }
    }

    /**
     * Toggle pengaturan download hanya melalui Wi-Fi (DataStore Preferences).
     */
    fun toggleDownloadOnlyWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDownloadOnlyWifi(enabled)
        }
    }

    /**
     * Ubah tema tampilan aplikasi (SYSTEM, LIGHT, DARK).
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    /**
     * Atur Sleep Timer dalam menit (15, 30, 45, 60, dll).
     */
    fun setSleepTimer(minutes: Int) {
        musicController.setSleepTimer(minutes)
    }

    /**
     * Batalkan Sleep Timer yang sedang berjalan.
     */
    fun cancelSleepTimer() {
        musicController.cancelSleepTimer()
    }

    /**
     * Event pesan / notifikasi untuk pengguna (misal file terhapus, timer selesai).
     */
    val userMessageEvent = musicController.userMessageEvent

    // ─── Playback & Antrean ─────────────────────────────────────────────────────

    fun playSong(song: SongEntity, customQueue: List<SongEntity>? = null) {
        val playlist = customQueue ?: when (uiState.value.selectedTab) {
            LibraryTab.SONGS -> uiState.value.songs
            LibraryTab.FAVORITES -> uiState.value.favoriteSongs
            LibraryTab.PLAYLISTS, LibraryTab.JAMENDO -> uiState.value.songs
        }
        val startIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        musicController.playSongs(playlist, startIndex)
    }

    fun playAll(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            musicController.playSongs(songs, startIndex)
        }
    }

    fun playQueueItem(index: Int) {
        musicController.playQueueItem(index)
    }

    fun playPause() {
        musicController.playPause()
    }

    fun skipToNext() {
        musicController.skipToNext()
    }

    fun skipToPrevious() {
        musicController.skipToPrevious()
    }

    fun playNext(song: SongEntity) {
        musicController.playNext(song)
    }

    fun addToQueue(song: SongEntity) {
        musicController.addToQueue(song)
    }

    fun removeQueueItem(index: Int) {
        musicController.removeQueueItem(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicController.moveQueueItem(fromIndex, toIndex)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(songId)
        }
    }

    // ─── Operasi Playlist ───────────────────────────────────────────────────────

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long) = repository.getSongsForPlaylist(playlistId)

    // ─── Kontrol Pemutar ────────────────────────────────────────────────────────

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
    }

    fun toggleShuffle() {
        musicController.toggleShuffle()
    }

    fun toggleRepeatMode() {
        musicController.toggleRepeatMode()
    }

    /** Simpan file cover baru ke penyimpanan aplikasi dan update database & pemutar. */
    fun updateAlbumCover(songId: Long, uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val coversDir = File(context.filesDir, "album_covers").apply { mkdirs() }
                val destFile = File(coversDir, "cover_${songId}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newArtUri = Uri.fromFile(destFile).toString()
                repository.updateSongAlbumArt(songId, newArtUri)
                musicController.updateSongArtwork(songId, newArtUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Hapus cover kustom dan kembalikan ke default. */
    fun removeCustomAlbumCover(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateSongAlbumArt(songId, "")
                musicController.updateSongArtwork(songId, "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Factory manual AppContainer tanpa Hilt. */
    class Factory(
        private val repository: MusicRepository,
        private val jamendoRepository: JamendoRepository,
        private val youTubeMusicRepository: com.example.musikku.data.repository.YouTubeMusicRepository,
        private val settingsRepository: SettingsRepository,
        private val networkMonitor: NetworkMonitor,
        private val musicController: MusicController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(
                repository = repository,
                jamendoRepository = jamendoRepository,
                youTubeMusicRepository = youTubeMusicRepository,
                settingsRepository = settingsRepository,
                networkMonitor = networkMonitor,
                musicController = musicController
            ) as T
        }
    }
}
