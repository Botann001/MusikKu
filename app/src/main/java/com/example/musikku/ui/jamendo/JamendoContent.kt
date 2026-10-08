package com.example.musikku.ui.jamendo

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.data.remote.dto.JamendoTrackDto
import com.example.musikku.ui.library.JamendoSearchState

@Composable
fun JamendoContent(
    state: JamendoSearchState,
    currentPlayingUri: String?,
    isOnline: Boolean,
    downloadOnlyWifi: Boolean,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onStream: (JamendoTrackDto) -> Unit,
    onDownload: (JamendoTrackDto) -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onToggleDownloadOnlyWifi: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var searchInput by remember { mutableStateOf(state.query) }
    val focusManager = LocalFocusManager.current
    val genreChips = listOf("Rock", "Pop", "Electronic", "Jazz", "Chill", "Classical", "Acoustic")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Banner Indikator Offline
        AnimatedVisibility(visible = !isOnline) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode Offline: Lagu streaming yang belum diunduh tidak dapat diputar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Bilah Input Pencarian Jamendo
        OutlinedTextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            placeholder = { Text("Cari lagu Creative Commons di Jamendo…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = {
                        searchInput = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch(searchInput)
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Chip Genre Populer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genreChips.forEach { genre ->
                FilterChip(
                    selected = searchInput.equals(genre, ignoreCase = true),
                    onClick = {
                        searchInput = genre
                        focusManager.clearFocus()
                        onSearch(genre)
                    },
                    label = { Text(genre) }
                )
            }
        }

        // Pengaturan Unduhan: Unduh Hanya via Wi-Fi (DataStore Preferences)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unduh hanya via Wi-Fi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = downloadOnlyWifi,
                onCheckedChange = onToggleDownloadOnlyWifi,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Konten Hasil / Populer / Loading / Error
        when {
            state.isSearching -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchInput.isNotBlank()) "Mencari musik di Jamendo…" else "Memuat lagu populer…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.errorMessage != null && state.results.isEmpty() && state.popularTracks.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                val displayList = if (state.results.isNotEmpty()) state.results else state.popularTracks
                val headerTitle = if (state.results.isNotEmpty()) {
                    "Hasil Pencarian (${displayList.size} lagu)"
                } else {
                    "Daftar Populer Jamendo"
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(items = displayList, key = { it.id }) { track ->
                        val isPlaying = track.audio != null && track.audio == currentPlayingUri
                        val safeId = track.id.toLongOrNull() ?: (track.id.hashCode().toLong().let { if (it < 0) -it else it })
                        val isDownloaded = state.downloadedIds.contains(safeId)
                        val isDownloading = state.downloadProgress.containsKey(safeId) || state.downloadingIds.contains(safeId)
                        val downloadPercent = state.downloadProgress[safeId]

                        val isPlayable = isOnline || isDownloaded

                        JamendoTrackRow(
                            track = track,
                            isPlaying = isPlaying,
                            isPlayable = isPlayable,
                            isOnline = isOnline,
                            isDownloaded = isDownloaded,
                            isDownloading = isDownloading,
                            downloadPercent = downloadPercent,
                            onClick = {
                                if (isPlayable) {
                                    onStream(track)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Mode Offline: Hubungkan ke internet untuk memutar lagu streaming.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDownloadClick = {
                                if (isOnline) {
                                    onDownload(track)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Tidak dapat mengunduh saat perangkat sedang offline.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onDeleteClick = {
                                onDeleteDownload(safeId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JamendoTrackRow(
    track: JamendoTrackDto,
    isPlaying: Boolean,
    isPlayable: Boolean,
    isOnline: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadPercent: Int?,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val rowAlpha = if (isPlayable) 1f else 0.42f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cover Art (Coil AsyncImage)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isPlaying) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            val artUrl = track.albumImage ?: track.image
            AsyncImage(
                model = artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (artUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info Judul, Artis & Durasi
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Text(
                        text = "TERUNDUH • ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${track.artistName ?: "Jamendo"} • ${formatDurationSeconds(track.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tombol Streaming Play
        IconButton(
            onClick = onClick,
            enabled = isPlayable
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Putar",
                tint = if (isPlaying) MaterialTheme.colorScheme.primary
                else if (isPlayable) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.outline
            )
        }

        // Tombol / Status Unduhan
        when {
            isDownloading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                ) {
                    val progressFloat = (downloadPercent ?: 0) / 100f
                    if (progressFloat > 0f) {
                        CircularProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                    if (downloadPercent != null && downloadPercent > 0) {
                        Text(
                            text = "$downloadPercent%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            isDownloaded -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sudah diunduh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus unduhan",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            else -> {
                IconButton(
                    onClick = onDownloadClick,
                    enabled = isOnline
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Unduh lagu",
                        tint = if (isOnline) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun formatDurationSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
