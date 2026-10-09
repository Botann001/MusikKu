package com.example.musikku.ui.explore

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musikku.data.remote.dto.JamendoTrackDto
import com.example.musikku.data.remote.youtube.YouTubeSearchItemDto
import com.example.musikku.ui.jamendo.JamendoContent
import com.example.musikku.ui.library.ExploreSourceTab
import com.example.musikku.ui.library.JamendoSearchState
import com.example.musikku.ui.library.YouTubeSearchState
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted

@Composable
fun ExploreScreen(
    currentTab: ExploreSourceTab,
    onTabSelected: (ExploreSourceTab) -> Unit,
    youTubeState: YouTubeSearchState,
    jamendoState: JamendoSearchState,
    currentPlayingTitle: String?,
    currentPlayingUri: String?,
    isPlaying: Boolean,
    isOnline: Boolean,
    downloadOnlyWifi: Boolean,
    contentPadding: PaddingValues,
    onYouTubeSearch: (String) -> Unit,
    onYouTubePlay: (YouTubeSearchItemDto) -> Unit,
    onYouTubeAddToQueue: (YouTubeSearchItemDto) -> Unit,
    onYouTubeAddToPlaylist: (YouTubeSearchItemDto) -> Unit,
    onJamendoSearch: (String) -> Unit,
    onJamendoStream: (JamendoTrackDto) -> Unit,
    onJamendoDownload: (JamendoTrackDto) -> Unit,
    onJamendoDeleteDownload: (Long) -> Unit,
    onToggleDownloadOnlyWifi: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ─── Top Header: Subtitle, Title, Segmented Tabs ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (currentTab == ExploreSourceTab.YOUTUBE) "STREAMING JUTAAN MUSIK ONLINE" else "MUSIK LEGAL BEBAS ROYALTI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Jelajah",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented Switcher Bar (YouTube Music | Jamendo)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF1B1D1B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab YouTube Music
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == ExploreSourceTab.YOUTUBE) ElectricLime else Color.Transparent)
                            .clickable { onTabSelected(ExploreSourceTab.YOUTUBE) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "YouTube Music",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentTab == ExploreSourceTab.YOUTUBE) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentTab == ExploreSourceTab.YOUTUBE) OnElectricLime else Color(0xFFCCCCCC)
                        )
                    }

                    // Tab Jamendo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (currentTab == ExploreSourceTab.JAMENDO) ElectricLime else Color.Transparent)
                            .clickable { onTabSelected(ExploreSourceTab.JAMENDO) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Jamendo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentTab == ExploreSourceTab.JAMENDO) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentTab == ExploreSourceTab.JAMENDO) OnElectricLime else Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }

        // ─── Active Content (YouTube or Jamendo) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Crossfade(targetState = currentTab, label = "ExploreSourceCrossfade") { tab ->
                when (tab) {
                    ExploreSourceTab.YOUTUBE -> {
                        YouTubeContent(
                            state = youTubeState,
                            currentPlayingTitle = currentPlayingTitle,
                            isPlaying = isPlaying,
                            isOnline = isOnline,
                            contentPadding = contentPadding,
                            onSearch = onYouTubeSearch,
                            onPlay = onYouTubePlay,
                            onAddToQueue = onYouTubeAddToQueue,
                            onAddToPlaylist = onYouTubeAddToPlaylist
                        )
                    }

                    ExploreSourceTab.JAMENDO -> {
                        JamendoContent(
                            state = jamendoState,
                            currentPlayingUri = currentPlayingUri,
                            isOnline = isOnline,
                            downloadOnlyWifi = downloadOnlyWifi,
                            contentPadding = contentPadding,
                            showHeader = false,
                            onSearch = onJamendoSearch,
                            onStream = onJamendoStream,
                            onDownload = onJamendoDownload,
                            onDeleteDownload = onJamendoDeleteDownload,
                            onToggleDownloadOnlyWifi = onToggleDownloadOnlyWifi
                        )
                    }
                }
            }
        }
    }
}
