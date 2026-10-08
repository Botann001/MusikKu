package com.example.musikku.ui.library

import com.example.musikku.data.local.dao.PlaylistWithCount
import com.example.musikku.data.local.entity.SongEntity
import com.example.musikku.data.preferences.ThemeMode
import com.example.musikku.data.remote.dto.JamendoTrackDto

enum class LibraryTab {
    SONGS,
    FAVORITES,
    PLAYLISTS,
    JAMENDO
}

data class JamendoSearchState(
    val query: String = "",
    val results: List<JamendoTrackDto> = emptyList(),
    val popularTracks: List<JamendoTrackDto> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val downloadingIds: Set<Long> = emptySet(),
    val downloadProgress: Map<Long, Int> = emptyMap(),
    val downloadedIds: Set<Long> = emptySet()
)

/** State untuk LibraryScreen mencakup Semua Lagu, Favorit, Playlist, dan Jamendo. */
data class LibraryUiState(
    val songs: List<SongEntity> = emptyList(),
    val favoriteSongs: List<SongEntity> = emptyList(),
    val favoriteSongIds: Set<Long> = emptySet(),
    val playlists: List<PlaylistWithCount> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val searchQuery: String = "",
    val jamendoState: JamendoSearchState = JamendoSearchState(),
    val isOnline: Boolean = true,
    val downloadOnlyWifi: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)
