package com.example.musikku.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.preferences.AppLanguage
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
import android.content.Intent
import android.net.Uri
import com.example.musikku.data.remote.youtube.toYouTubeSearchItemDto
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: MusicRepository,
    val jamendoRepository: JamendoRepository,
    val youTubeMusicRepository: com.example.musikku.data.repository.YouTubeMusicRepository,
    val lyricsRepository: com.example.musikku.data.repository.LyricsRepository,
    val settingsRepository: SettingsRepository,
    val networkMonitor: NetworkMonitor,
    val musicController: MusicController,
    val listeningStatsRepository: com.example.musikku.data.repository.ListeningStatsRepository? = null,
    val spotifyRepository: com.example.musikku.data.repository.SpotifyRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    private val _isLoading = MutableStateFlow(true)
    private val _exploreSourceTab = MutableStateFlow(ExploreSourceTab.YOUTUBE)
    private val _youTubeState = MutableStateFlow(YouTubeSearchState())
    private val _jamendoState = MutableStateFlow(JamendoSearchState())
    private val _spotifyImportState = MutableStateFlow(SpotifyImportState())
    private val _spotifyAccountState = MutableStateFlow(SpotifyAccountState())
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
        val spotifyImportState: SpotifyImportState,
        val spotifyAccountState: SpotifyAccountState,
        val sortOrder: SongSortOrder,
        val isLoading: Boolean,
        val isOnline: Boolean,
        val downloadOnlyWifi: Boolean,
        val themeMode: ThemeMode,
        val appLanguage: AppLanguage,
        val recentSearches: List<String>,
        val userName: String,
        val userAvatarUri: String,
        val userBio: String,
        val listeningStats: com.example.musikku.data.repository.ListeningTimeStats
    )

    private val defaultListeningStatsFlow = MutableStateFlow(com.example.musikku.data.repository.ListeningTimeStats())

    private val tabQueryFlow = combine(
        combine(_selectedTab, _searchQuery, _exploreSourceTab, _youTubeState) { tab, q, expTab, ytState ->
            listOf(tab, q, expTab, ytState)
        },
        combine(_jamendoState, _sortOrder, _isLoading, _spotifyImportState, _spotifyAccountState) { jamendo, sort, loading, spotify, account ->
            listOf(jamendo, sort, loading, spotify, account)
        },
        combine(
            networkMonitor.isOnline,
            settingsRepository.downloadOnlyWifi,
            settingsRepository.themeMode,
            settingsRepository.appLanguage,
            settingsRepository.recentSearches
        ) { online, wifiOnly, theme, lang, recents ->
            listOf(online, wifiOnly, theme, lang, recents)
        },
        combine(
            settingsRepository.userName,
            settingsRepository.userAvatarUri,
            settingsRepository.userBio,
            listeningStatsRepository?.liveStats ?: defaultListeningStatsFlow
        ) { name, avatar, bio, stats ->
            listOf(name, avatar, bio, stats)
        }
    ) { list1, list2, list3, list4 ->
        @Suppress("UNCHECKED_CAST")
        CombinedTabState(
            tab = list1[0] as LibraryTab,
            query = list1[1] as String,
            exploreSourceTab = list1[2] as ExploreSourceTab,
            youTubeState = list1[3] as YouTubeSearchState,
            jamendoState = list2[0] as JamendoSearchState,
            sortOrder = list2[1] as SongSortOrder,
            isLoading = list2[2] as Boolean,
            spotifyImportState = list2[3] as SpotifyImportState,
            spotifyAccountState = list2[4] as SpotifyAccountState,
            isOnline = list3[0] as Boolean,
            downloadOnlyWifi = list3[1] as Boolean,
            themeMode = list3[2] as ThemeMode,
            appLanguage = list3[3] as AppLanguage,
            recentSearches = list3[4] as List<String>,
            userName = list4[0] as String,
            userAvatarUri = list4[1] as String,
            userBio = list4[2] as String,
            listeningStats = list4[3] as com.example.musikku.data.repository.ListeningTimeStats
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
            spotifyImportState = tabState.spotifyImportState,
            spotifyAccountState = tabState.spotifyAccountState,
            isOnline = tabState.isOnline,
            downloadOnlyWifi = tabState.downloadOnlyWifi,
            themeMode = tabState.themeMode,
            appLanguage = tabState.appLanguage,
            sortOrder = tabState.sortOrder,
            recentSearches = tabState.recentSearches,
            userName = tabState.userName,
            userAvatarUri = tabState.userAvatarUri,
            userBio = tabState.userBio,
            listeningStats = tabState.listeningStats,
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

    /** State lirik berjalan sinkron (karaoke) dari LRCLIB */
    private val _lyricsState = MutableStateFlow<com.example.musikku.data.remote.lyrics.LyricsUiState>(
        com.example.musikku.data.remote.lyrics.LyricsUiState.Idle
    )
    val lyricsState: StateFlow<com.example.musikku.data.remote.lyrics.LyricsUiState> = _lyricsState.asStateFlow()

    init {
        refreshLibrary()

        // Sambungkan resolver stream audio untuk MusicController (Spotify & YouTube expired)
        musicController.onResolveSongStream = { song ->
            resolveSongIfNeeded(song)
        }

        // Pantau pergantian lagu dan muat lirik otomatis dari LRCLIB
        viewModelScope.launch {
            playbackState
                .map { it.currentSong }
                .distinctUntilChangedBy { it?.id }
                .collect { song ->
                    if (song != null) {
                        fetchLyrics(song)
                    } else {
                        _lyricsState.value = com.example.musikku.data.remote.lyrics.LyricsUiState.Idle
                    }
                }
        }

        // Pantau progres unduhan dari WorkManager
        viewModelScope.launch {
            jamendoRepository.downloadProgressMap.collect { progressMap ->
                _jamendoState.update { it.copy(downloadProgress = progressMap) }
                _youTubeState.update { it.copy(downloadProgress = progressMap) }
            }
        }

        // Pantau ID lagu-lagu yang telah terunduh ke Room
        viewModelScope.launch {
            jamendoRepository.downloadedSongIds.collect { downloadedIds ->
                val idSet = downloadedIds.toSet()
                _jamendoState.update { it.copy(downloadedIds = idSet) }
                _youTubeState.update { it.copy(downloadedIds = idSet) }
            }
        }

        // Pantau status login / sinkronisasi Akun Spotify (Tahap 3)
        viewModelScope.launch {
            settingsRepository.spotifyAuth.collect { auth ->
                _spotifyAccountState.update { state ->
                    state.copy(
                        isConnected = auth.isConnected,
                        profile = if (auth.isConnected && auth.userId.isNotBlank()) {
                            com.example.musikku.data.repository.SpotifyUserProfile(
                                id = auth.userId,
                                displayName = auth.userName.ifBlank { "Pengguna Spotify" },
                                avatarUrl = auth.userAvatar.ifBlank { null }
                            )
                        } else null
                    )
                }
                if (auth.isConnected && auth.accessToken.isNotBlank()) {
                    refreshSpotifyProfileAndPlaylists(auth.accessToken)
                }
            }
        }

        // Muat lagu populer awal untuk Jamendo dan YouTube Music
        loadPopularJamendo()
        loadTrendingYouTube()
    }

    /** Mengambil lirik lagu dari LRCLIB */
    fun fetchLyrics(song: SongEntity? = playbackState.value.currentSong) {
        val current = song ?: return
        viewModelScope.launch {
            _lyricsState.value = com.example.musikku.data.remote.lyrics.LyricsUiState.Loading
            try {
                val result = lyricsRepository.getLyrics(
                    title = current.title,
                    artist = current.artist,
                    durationMs = current.duration
                )
                _lyricsState.value = result
            } catch (e: Exception) {
                _lyricsState.value = com.example.musikku.data.remote.lyrics.LyricsUiState.Error(
                    e.localizedMessage ?: "Gagal memuat lirik dari LRCLIB"
                )
            }
        }
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

    fun loadPopularJamendo(forceRefresh: Boolean = false) {
        if (!forceRefresh && _jamendoState.value.popularTracks.isNotEmpty()) {
            _jamendoState.update {
                it.copy(
                    query = "",
                    results = emptyList(),
                    isSearching = false,
                    errorMessage = null
                )
            }
            return
        }
        viewModelScope.launch {
            _jamendoState.update {
                it.copy(
                    query = "",
                    results = emptyList(),
                    isSearching = true,
                    errorMessage = null
                )
            }
            val result = jamendoRepository.getPopularTracks()
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuJamendo", "Loaded popular tracks: ${tracks.size}")
                _jamendoState.update {
                    it.copy(
                        popularTracks = tracks,
                        query = "",
                        results = emptyList(),
                        isSearching = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuJamendo", "Failed to load popular tracks", error)
                _jamendoState.update {
                    it.copy(
                        query = "",
                        isSearching = false,
                        errorMessage = "Gagal memuat lagu populer: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    fun clearJamendoSearch() {
        _jamendoState.update {
            it.copy(
                query = "",
                results = emptyList(),
                isSearching = false,
                errorMessage = null
            )
        }
        if (_jamendoState.value.popularTracks.isEmpty()) {
            loadPopularJamendo()
        }
    }

    fun searchJamendo(query: String) {
        val queryClean = query.trim()
        if (queryClean.isBlank()) {
            clearJamendoSearch()
            return
        }

        viewModelScope.launch {
            settingsRepository.addRecentSearch(queryClean)
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
        if (tab == ExploreSourceTab.JAMENDO && _jamendoState.value.popularTracks.isEmpty()) {
            loadPopularJamendo()
        } else if (tab == ExploreSourceTab.YOUTUBE && _youTubeState.value.trendingTracks.isEmpty()) {
            loadTrendingYouTube()
        }
    }

    fun loadTrendingYouTube(forceRefresh: Boolean = false) {
        if (!forceRefresh && _youTubeState.value.trendingTracks.isNotEmpty()) {
            _youTubeState.update {
                it.copy(
                    query = "",
                    results = emptyList(),
                    isSearching = false,
                    errorMessage = null
                )
            }
            return
        }
        viewModelScope.launch {
            _youTubeState.update {
                it.copy(
                    query = "",
                    results = emptyList(),
                    isSearching = true,
                    errorMessage = null
                )
            }
            val result = youTubeMusicRepository.getTrendingMusic()
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuYouTube", "Loaded trending tracks: ${tracks.size}")
                _youTubeState.update {
                    it.copy(
                        trendingTracks = tracks,
                        query = "",
                        results = emptyList(),
                        isSearching = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuYouTube", "Failed to load trending", error)
                _youTubeState.update {
                    it.copy(
                        query = "",
                        isSearching = false,
                        errorMessage = "Gagal memuat musik trending: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    fun clearYouTubeSearch() {
        _youTubeState.update {
            it.copy(
                query = "",
                results = emptyList(),
                isSearching = false,
                errorMessage = null
            )
        }
        if (_youTubeState.value.trendingTracks.isEmpty()) {
            loadTrendingYouTube()
        }
    }

    fun resetExploreToTrending() {
        loadTrendingYouTube(forceRefresh = false)
        loadPopularJamendo(forceRefresh = false)
    }

    suspend fun searchYouTubeForCommunity(query: String): List<com.example.musikku.data.remote.youtube.YouTubeSearchItemDto> {
        return youTubeMusicRepository.searchMusic(query.trim()).getOrNull() ?: emptyList()
    }

    fun searchYouTube(query: String) {
        val queryClean = query.trim()
        if (queryClean.isBlank()) {
            clearYouTubeSearch()
            return
        }

        // Jika input adalah tautan Spotify (Track/Album/Playlist), alihkan ke import Spotify otomatis
        if (isSpotifyLink(queryClean)) {
            importSpotifyLink(queryClean)
            return
        }

        viewModelScope.launch {
            settingsRepository.addRecentSearch(queryClean)
            _youTubeState.update {
                it.copy(
                    query = queryClean,
                    isSearching = true,
                    isLoadingMore = false,
                    hasMoreResults = true,
                    errorMessage = null
                )
            }
            val result = youTubeMusicRepository.searchMusic(queryClean)
            result.onSuccess { tracks ->
                android.util.Log.d("MusikKuYouTube", "Search for '$queryClean' returned: ${tracks.size} tracks")
                _youTubeState.update {
                    it.copy(
                        results = tracks,
                        isSearching = false,
                        isLoadingMore = false,
                        hasMoreResults = tracks.isNotEmpty() && youTubeMusicRepository.hasMore(),
                        errorMessage = if (tracks.isEmpty()) "Tidak ada hasil untuk \"$queryClean\"" else null
                    )
                }
            }.onFailure { error ->
                android.util.Log.e("MusikKuYouTube", "Search for '$queryClean' failed", error)
                _youTubeState.update {
                    it.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        hasMoreResults = false,
                        errorMessage = "Gagal mencari di YouTube: ${error.localizedMessage ?: "Periksa koneksi internet"}"
                    )
                }
            }
        }
    }

    /**
     * Memuat halaman lagu berikutnya dari YouTube (Infinite Scroll).
     */
    fun loadMoreYouTube() {
        val currentState = _youTubeState.value
        if (currentState.isSearching || currentState.isLoadingMore || !currentState.hasMoreResults || currentState.query.isBlank()) {
            return
        }

        viewModelScope.launch {
            _youTubeState.update { it.copy(isLoadingMore = true) }
            val result = youTubeMusicRepository.loadMoreSearchResults()
            result.onSuccess { newTracks ->
                android.util.Log.d("MusikKuYouTube", "Load more returned: ${newTracks.size} new tracks")
                _youTubeState.update { state ->
                    val existingIds = state.results.map { it.videoId }.toSet()
                    val filteredNew = newTracks.filterNot { existingIds.contains(it.videoId) }
                    state.copy(
                        results = state.results + filteredNew,
                        isLoadingMore = false,
                        hasMoreResults = filteredNew.isNotEmpty() && youTubeMusicRepository.hasMore()
                    )
                }
            }.onFailure {
                _youTubeState.update { it.copy(isLoadingMore = false, hasMoreResults = false) }
            }
        }
    }

    /**
     * Memeriksa apakah input adalah tautan Spotify yang valid.
     */
    fun isSpotifyLink(input: String): Boolean {
        return spotifyRepository?.isSpotifyLink(input) == true
    }

    /**
     * Mengimpor metadata dari link Spotify publik (Lagu, Album, Playlist)
     * dan menampilkan hasilnya di SpotifyImportSheet.
     */
    fun importSpotifyLink(urlOrUri: String) {
        val repo = spotifyRepository ?: return
        val trimmed = urlOrUri.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _spotifyImportState.update { it.copy(isImporting = true, errorMessage = null) }
            val result = repo.fetchSpotifyMetadata(trimmed)
            result.onSuccess { importResult ->
                _spotifyImportState.update {
                    it.copy(
                        isImporting = false,
                        result = importResult,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _spotifyImportState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = error.localizedMessage ?: "Gagal mengimpor link Spotify"
                    )
                }
                musicController.postUserMessage("Gagal impor Spotify: ${error.localizedMessage}")
            }
        }
    }

    fun clearSpotifyImport() {
        _spotifyImportState.update { SpotifyImportState() }
    }

    fun playSpotifyTrack(track: com.example.musikku.data.repository.SpotifyTrack) {
        val songId = ((("spotify_" + track.id).hashCode().toLong() and 0x7FFFFFFF) or 0x50000000L)
        val songEntity = SongEntity(
            id = songId,
            title = track.title,
            artist = track.artist,
            album = "Spotify",
            duration = track.durationMs,
            albumArtUri = track.coverArtUri.orEmpty(),
            contentUri = "spotify:${track.id}",
            source = "YOUTUBE",
            dateAdded = System.currentTimeMillis() / 1000,
            size = 0L,
            isFavorite = false,
            filePath = ""
        )
        val allTracks = _spotifyImportState.value.result?.tracks
        if (!allTracks.isNullOrEmpty() && allTracks.any { it.id == track.id }) {
            playAllSpotifyTracks(allTracks, allTracks.indexOfFirst { it.id == track.id })
        } else {
            musicController.playSongs(listOf(songEntity), 0)
        }
    }

    fun playAllSpotifyTracks(tracks: List<com.example.musikku.data.repository.SpotifyTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val albumName = _spotifyImportState.value.result?.title?.ifBlank { "Spotify" } ?: "Spotify"
        val songEntities = tracks.map { track ->
            val songId = ((("spotify_" + track.id).hashCode().toLong() and 0x7FFFFFFF) or 0x50000000L)
            SongEntity(
                id = songId,
                title = track.title,
                artist = track.artist,
                album = albumName,
                duration = track.durationMs,
                albumArtUri = track.coverArtUri.orEmpty(),
                contentUri = "spotify:${track.id}",
                source = "YOUTUBE",
                dateAdded = System.currentTimeMillis() / 1000,
                size = 0L,
                isFavorite = false,
                filePath = ""
            )
        }
        val validIndex = startIndex.coerceIn(songEntities.indices)
        musicController.playSongs(songEntities, validIndex)
    }

    fun shuffleAllSpotifyTracks(tracks: List<com.example.musikku.data.repository.SpotifyTrack>) {
        if (tracks.isEmpty()) return
        playAllSpotifyTracks(tracks.shuffled(), 0)
    }

    /**
     * Memutar langsung lagu pertama dari Chart/Playlist Spotify dari Beranda dan membuka sheet.
     */
    fun playSpotifyPlaylistDirectly(playlistId: String) {
        val repo = spotifyRepository ?: return
        viewModelScope.launch {
            _spotifyImportState.update { it.copy(isImporting = true, errorMessage = null) }
            val result = repo.fetchSpotifyMetadata("https://open.spotify.com/playlist/$playlistId")
            result.onSuccess { importResult ->
                _spotifyImportState.update {
                    it.copy(
                        isImporting = false,
                        result = importResult,
                        errorMessage = null
                    )
                }
                if (importResult.tracks.isNotEmpty()) {
                    playSpotifyTrack(importResult.tracks.first())
                }
            }.onFailure { error ->
                _spotifyImportState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = error.localizedMessage ?: "Gagal memuat chart Spotify"
                    )
                }
                musicController.postUserMessage("Gagal memuat chart Spotify: ${error.localizedMessage}")
            }
        }
    }

    fun saveSpotifyPlaylistToLibrary(result: com.example.musikku.data.repository.SpotifyImportResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playlistName = result.title.ifBlank { "Playlist Spotify" }
                val playlistId = repository.createPlaylist(playlistName)

                var count = 0
                for (track in result.tracks) {
                    val songId = ((("spotify_" + track.id).hashCode().toLong() and 0x7FFFFFFF) or 0x50000000L)
                    val songEntity = SongEntity(
                        id = songId,
                        title = track.title,
                        artist = track.artist,
                        album = result.title,
                        duration = track.durationMs,
                        albumArtUri = track.coverArtUri.orEmpty(),
                        contentUri = "spotify:${track.id}",
                        source = "YOUTUBE",
                        dateAdded = System.currentTimeMillis() / 1000,
                        size = 0L,
                        isFavorite = false,
                        filePath = ""
                    )
                    repository.saveOnlineSongForPlaylist(songEntity)
                    repository.addSongToPlaylist(playlistId, songId)
                    count++
                }
                musicController.postUserMessage("Playlist '$playlistName' berhasil disimpan ($count lagu)")
            } catch (e: Exception) {
                musicController.postUserMessage("Gagal menyimpan playlist: ${e.localizedMessage}")
            }
        }
    }

    // ─── Metode Akun & Sinkronisasi Spotify (Tahap 3) ────────────────────────

    fun startSpotifyLogin(context: Context, customClientId: String = "") {
        val repo = spotifyRepository ?: return
        val clientId = customClientId.trim()
        if (clientId.isBlank()) {
            musicController.postUserMessage("Silakan isi Spotify Client ID terlebih dahulu, atau gunakan tab 'Token Cepat'.")
            return
        }
        viewModelScope.launch {
            val verifier = repo.generateCodeVerifier()
            val challenge = repo.generateCodeChallenge(verifier)
            settingsRepository.saveSpotifyCodeVerifier(verifier, clientId)

            val authUri = repo.buildAuthUri(clientId = clientId, codeChallenge = challenge)
            val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                musicController.postUserMessage("Tidak dapat membuka browser: ${e.localizedMessage}")
            }
        }
    }

    fun handleSpotifyAuthCallback(code: String) {
        val repo = spotifyRepository ?: return
        viewModelScope.launch {
            _spotifyAccountState.update { it.copy(isLoading = true, errorMessage = null) }
            val authData = settingsRepository.spotifyAuth.first()
            val verifier = authData.codeVerifier
            val clientId = authData.clientId.ifBlank { "0d3c01c6db9848248cbb5920ba8f3ce8" }

            val tokenResult = repo.exchangeCodeForToken(code = code, verifier = verifier, clientId = clientId)
            tokenResult.onSuccess { (accessToken, refreshToken) ->
                val profileResult = repo.fetchCurrentUserProfile(accessToken)
                val profile = profileResult.getOrNull()
                settingsRepository.saveSpotifyAuth(
                    accessToken = accessToken,
                    refreshToken = refreshToken.orEmpty(),
                    userId = profile?.id.orEmpty(),
                    userName = profile?.displayName.orEmpty(),
                    userAvatar = profile?.avatarUrl.orEmpty()
                )
                _spotifyAccountState.update {
                    it.copy(
                        isConnected = true,
                        isLoading = false,
                        profile = profile,
                        errorMessage = null
                    )
                }
                musicController.postUserMessage("Akun Spotify berhasil terhubung: ${profile?.displayName ?: ""}")
                refreshSpotifyProfileAndPlaylists(accessToken)
            }.onFailure { err ->
                _spotifyAccountState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
                musicController.postUserMessage("Gagal menghubungkan Spotify: ${err.localizedMessage}")
            }
        }
    }

    fun connectSpotifyWithAccessToken(token: String) {
        val repo = spotifyRepository ?: return
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return

        viewModelScope.launch {
            _spotifyAccountState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileResult = repo.fetchCurrentUserProfile(cleanToken)
            profileResult.onSuccess { profile ->
                settingsRepository.saveSpotifyAuth(
                    accessToken = cleanToken,
                    userId = profile.id,
                    userName = profile.displayName,
                    userAvatar = profile.avatarUrl.orEmpty()
                )
                _spotifyAccountState.update {
                    it.copy(
                        isConnected = true,
                        isLoading = false,
                        profile = profile,
                        errorMessage = null
                    )
                }
                musicController.postUserMessage("Akun Spotify berhasil terhubung: ${profile.displayName}")
                refreshSpotifyProfileAndPlaylists(cleanToken)
            }.onFailure { err ->
                _spotifyAccountState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
                musicController.postUserMessage("Token Spotify tidak valid atau telah kedaluwarsa")
            }
        }
    }

    fun refreshSpotifyProfileAndPlaylists(explicitToken: String? = null) {
        val repo = spotifyRepository ?: return
        viewModelScope.launch {
            val token = explicitToken ?: settingsRepository.spotifyAuth.first().accessToken
            if (token.isBlank()) return@launch

            _spotifyAccountState.update { it.copy(isLoading = true) }
            val profileResult = repo.fetchCurrentUserProfile(token)
            val playlistsResult = repo.fetchCurrentUserPlaylists(token)

            _spotifyAccountState.update { state ->
                state.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull() ?: state.profile,
                    userPlaylists = playlistsResult.getOrNull() ?: state.userPlaylists
                )
            }
        }
    }

    fun syncSpotifyLikedSongs() {
        val repo = spotifyRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val token = settingsRepository.spotifyAuth.first().accessToken
            if (token.isBlank()) {
                musicController.postUserMessage("Silakan hubungkan akun Spotify terlebih dahulu.")
                return@launch
            }

            _spotifyAccountState.update { it.copy(isSyncingLikedSongs = true) }
            val result = repo.fetchCurrentUserLikedSongs(token, limit = 50)
            _spotifyAccountState.update { it.copy(isSyncingLikedSongs = false) }

            result.onSuccess { tracks ->
                if (tracks.isEmpty()) {
                    musicController.postUserMessage("Tidak ada lagu di Liked Songs akun Spotify Anda.")
                    return@onSuccess
                }

                val playlistId = repository.createPlaylist("Lagu Disukai Spotify")
                var savedCount = 0
                for (track in tracks) {
                    val songId = ((("spotify_liked_" + track.id).hashCode().toLong() and 0x7FFFFFFF) or 0x51000000L)
                    val songEntity = SongEntity(
                        id = songId,
                        title = track.title,
                        artist = track.artist,
                        album = "Spotify Liked Songs",
                        duration = track.durationMs,
                        albumArtUri = track.coverArtUri.orEmpty(),
                        contentUri = "spotify:${track.id}",
                        source = "YOUTUBE",
                        dateAdded = System.currentTimeMillis() / 1000,
                        size = 0L,
                        isFavorite = true,
                        filePath = ""
                    )
                    repository.saveOnlineSongForPlaylist(songEntity)
                    repository.addSongToPlaylist(playlistId, songId)
                    savedCount++
                }
                musicController.postUserMessage("$savedCount Lagu Disukai Spotify berhasil disinkronkan!")
            }.onFailure { err ->
                musicController.postUserMessage("Gagal sinkronisasi Liked Songs: ${err.localizedMessage}")
            }
        }
    }

    fun importSpotifyUserPlaylist(playlist: com.example.musikku.data.repository.SpotifyUserPlaylist) {
        val repo = spotifyRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val token = settingsRepository.spotifyAuth.first().accessToken
            if (token.isBlank()) return@launch

            _spotifyAccountState.update { it.copy(syncingPlaylistId = playlist.id) }
            val result = repo.fetchUserPlaylistTracks(token, playlist.id)
            _spotifyAccountState.update { it.copy(syncingPlaylistId = null) }

            result.onSuccess { tracks ->
                val targetPlaylistId = repository.createPlaylist(playlist.name)
                var count = 0
                for (track in tracks) {
                    val songId = ((("spotify_user_pl_" + track.id).hashCode().toLong() and 0x7FFFFFFF) or 0x52000000L)
                    val songEntity = SongEntity(
                        id = songId,
                        title = track.title,
                        artist = track.artist,
                        album = playlist.name,
                        duration = track.durationMs,
                        albumArtUri = track.coverArtUri.orEmpty(),
                        contentUri = "spotify:${track.id}",
                        source = "YOUTUBE",
                        dateAdded = System.currentTimeMillis() / 1000,
                        size = 0L,
                        isFavorite = false,
                        filePath = ""
                    )
                    repository.saveOnlineSongForPlaylist(songEntity)
                    repository.addSongToPlaylist(targetPlaylistId, songId)
                    count++
                }
                musicController.postUserMessage("Playlist '${playlist.name}' berhasil diimpor ($count lagu)!")
            }.onFailure { err ->
                musicController.postUserMessage("Gagal mengimpor playlist: ${err.localizedMessage}")
            }
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch {
            settingsRepository.clearSpotifyAuth()
            _spotifyAccountState.value = SpotifyAccountState()
            musicController.postUserMessage("Akun Spotify berhasil diputuskan.")
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
                val errorMsg = error.message.orEmpty()
                val msg = if (errorMsg.contains("dibatasi") || errorMsg.contains("diblokir") || errorMsg.contains("PO-Token") || errorMsg.contains("403") || errorMsg.contains("500") || errorMsg.contains("Connect")) {
                    "Lagu '${track.title}' tidak dapat diputar: YouTube membatasi streaming audio publik (PO-Token/500). Silakan gunakan tab Jamendo."
                } else {
                    "Gagal memutar audio YouTube: ${error.localizedMessage ?: "Server sedang sibuk"}"
                }
                musicController.postUserMessage(msg)
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
                repository.saveOnlineSongForPlaylist(song)
                repository.addSongToPlaylist(playlistId, song.id)
                musicController.postUserMessage("Lagu YouTube ditambahkan ke playlist")
            }.onFailure { error ->
                musicController.postUserMessage("Gagal menambahkan ke playlist: ${error.localizedMessage}")
            }
        }
    }

    /**
     * Mengunduh audio YouTube di latar belakang via WorkManager agar tersimpan secara lokal dan dapat diputar offline.
     */
    fun downloadYouTubeTrack(track: com.example.musikku.data.remote.youtube.YouTubeSearchItemDto) {
        viewModelScope.launch {
            val safeId = (track.videoId.hashCode().toLong().let { if (it < 0) -it else it })
            _youTubeState.update { it.copy(downloadingIds = it.downloadingIds + safeId) }
            musicController.postUserMessage("Menyiapkan unduhan '${track.title}'...")
            val result = youTubeMusicRepository.downloadTrack(track)
            result.onSuccess {
                musicController.postUserMessage("Mulai mengunduh '${track.title}'...")
            }.onFailure { err ->
                _youTubeState.update { it.copy(downloadingIds = it.downloadingIds - safeId) }
                musicController.postUserMessage("Gagal mengunduh: ${err.localizedMessage ?: "Stream tidak tersedia"}")
            }
        }
    }

    /**
     * Mengunduh lagu yang sedang diputar di NowPlayingSheet (baik YouTube maupun Jamendo).
     */
    fun downloadCurrentPlayingSong() {
        val current = musicController.playbackState.value.currentSong ?: return
        if (current.source == "DOWNLOADED" || current.source == "LOCAL") {
            musicController.postUserMessage("Lagu sudah tersimpan offline di HP")
            return
        }
        if (current.source == "YOUTUBE") {
            val trackDto = current.toYouTubeSearchItemDto()
            downloadYouTubeTrack(trackDto)
        } else if (current.source == "JAMENDO") {
            val trackDto = JamendoTrackDto(
                id = current.id.toString(),
                name = current.title,
                duration = current.duration / 1000L,
                artistName = current.artist,
                albumName = current.album,
                albumImage = current.albumArtUri,
                audio = current.contentUri,
                audioDownload = current.contentUri,
                image = current.albumArtUri
            )
            downloadJamendoTrack(trackDto)
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
     * Ubah bahasa tampilan aplikasi.
     */
    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(language)
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            settingsRepository.removeRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            settingsRepository.clearRecentSearches()
        }
    }

    /** Pembaruan Profil Pengguna (Gaya Spotify) */
    fun updateUserName(name: String) {
        viewModelScope.launch {
            settingsRepository.setUserName(name)
        }
    }

    fun updateUserAvatarUri(uri: String) {
        viewModelScope.launch {
            settingsRepository.setUserAvatarUri(uri)
        }
    }

    fun updateUserBio(bio: String) {
        viewModelScope.launch {
            settingsRepository.setUserBio(bio)
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

    /**
     * Menyelesaikan stream audio YouTube untuk lagu online (misal dari Spotify atau YouTube expired).
     * Jika sukses, mengembalikan SongEntity dengan contentUri siap streaming dan menyimpan ke Room.
     */
    suspend fun resolveSongIfNeeded(song: SongEntity): SongEntity? {
        if (song.source != "YOUTUBE") return song

        val isExpired = try {
            val exp = Uri.parse(song.contentUri).getQueryParameter("expire")?.toLongOrNull()
            exp != null && (System.currentTimeMillis() / 1000L) >= (exp - 60)
        } catch (_: Exception) {
            false
        }
        val isSpotifyTrack = song.contentUri.startsWith("spotify:")
        val isBlankUri = song.contentUri.isBlank()

        if (!isExpired && !isSpotifyTrack && !isBlankUri && song.contentUri.startsWith("http")) {
            return song
        }

        val trackDto = song.toYouTubeSearchItemDto()
        val targetDto = if (trackDto.videoId.isNotBlank()) {
            trackDto
        } else {
            val artistClean = if (song.artist.isNotBlank() && song.artist != "Artis Spotify" && song.artist != "Artis Tidak Diketahui") {
                song.artist
            } else ""
            val query1 = if (artistClean.isNotBlank()) "$artistClean - ${song.title}" else song.title
            var found = youTubeMusicRepository.searchMusic(query1).getOrNull()?.firstOrNull()
            if (found == null && artistClean.isNotBlank()) {
                val query2 = "${song.title} $artistClean"
                found = youTubeMusicRepository.searchMusic(query2).getOrNull()?.firstOrNull()
            }
            found
        }

        if (targetDto == null) {
            android.util.Log.e("LibraryViewModel", "Tidak dapat menemukan video YouTube untuk '${song.title}'")
            return null
        }

        val result = youTubeMusicRepository.resolveAudioStream(targetDto)
        return result.map { freshSong ->
            val mergedSong = freshSong.copy(
                id = song.id,
                title = song.title,
                artist = if (song.artist.isNotBlank() && song.artist != "Artis Spotify" && song.artist != "Artis Tidak Diketahui") song.artist else freshSong.artist,
                album = song.album.ifBlank { freshSong.album },
                albumArtUri = song.albumArtUri.ifBlank { freshSong.albumArtUri },
                isFavorite = song.isFavorite
            )
            repository.saveOnlineSongForPlaylist(mergedSong)
            mergedSong
        }.getOrNull()
    }

    fun playSong(song: SongEntity, customQueue: List<SongEntity>? = null) {
        val playlist = customQueue ?: run {
            when {
                uiState.value.favoriteSongs.any { it.id == song.id } -> uiState.value.favoriteSongs
                uiState.value.songs.any { it.id == song.id } -> uiState.value.songs
                else -> listOf(song)
            }
        }
        val foundIndex = playlist.indexOfFirst { it.id == song.id }
        val finalPlaylist = if (foundIndex >= 0) playlist else listOf(song)
        val startIndex = if (foundIndex >= 0) foundIndex else 0

        musicController.playSongs(finalPlaylist, startIndex)
    }

    fun playAll(songs: List<SongEntity>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            val validIndex = startIndex.coerceIn(songs.indices)
            musicController.playSongs(songs, validIndex)
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
            val current = musicController.playbackState.value.currentSong
            val fallback = if (current?.id == songId) current else null
            repository.toggleFavorite(songId, fallback)
        }
    }

    fun toggleYouTubeFavorite(track: com.example.musikku.data.remote.youtube.YouTubeSearchItemDto) {
        viewModelScope.launch {
            repository.toggleYouTubeFavorite(track)
            val safeId = (track.videoId.hashCode().toLong().let { if (it < 0) -it else it })
            val isNowFav = repository.favoriteSongIds.first().contains(safeId)
            val msg = if (isNowFav) "Lagu ditambahkan ke Favorit" else "Lagu dihapus dari Favorit"
            musicController.postUserMessage(msg)
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
        private val lyricsRepository: com.example.musikku.data.repository.LyricsRepository,
        private val settingsRepository: SettingsRepository,
        private val networkMonitor: NetworkMonitor,
        private val musicController: MusicController,
        private val listeningStatsRepository: com.example.musikku.data.repository.ListeningStatsRepository? = null,
        private val spotifyRepository: com.example.musikku.data.repository.SpotifyRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(
                repository = repository,
                jamendoRepository = jamendoRepository,
                youTubeMusicRepository = youTubeMusicRepository,
                lyricsRepository = lyricsRepository,
                settingsRepository = settingsRepository,
                networkMonitor = networkMonitor,
                musicController = musicController,
                listeningStatsRepository = listeningStatsRepository,
                spotifyRepository = spotifyRepository
            ) as T
        }
    }
}
