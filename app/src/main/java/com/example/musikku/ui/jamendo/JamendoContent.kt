package com.example.musikku.ui.jamendo

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.musikku.ui.common.AlbumArtPlaceholder
import com.example.musikku.ui.library.JamendoSearchState
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.JetCard
import com.example.musikku.ui.theme.OnElectricLime
import com.example.musikku.ui.theme.TextMuted

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
    val genreChips = listOf("Populer", "Pop", "Lo-fi", "Elektronik", "Akustik", "Rock", "Jazz", "Chill")
    var selectedGenre by remember { mutableStateOf("Populer") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(contentPadding)
    ) {
        // ─── 1. Header (MUSIK LEGAL DARI JAMENDO & Jelajah) Sesuai Image 3 ───────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "MUSIK LEGAL DARI JAMENDO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Jelajah",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 34.sp
            )
        }

        // ─── 2. Search Bar Kapsul (Cari jutaan lagu gratis) ──────────────────────
        TextField(
            value = searchInput,
            onValueChange = { searchInput = it },
            placeholder = { Text("Cari jutaan lagu gratis", color = Color(0xFF7E807E)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = Color(0xFF7E807E)
                )
            },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = {
                        searchInput = ""
                        selectedGenre = "Populer"
                        onSearch("")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Hapus",
                            tint = Color.White
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = JetCard,
                unfocusedContainerColor = JetCard,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch(searchInput)
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // ─── 3. Genre Chips Kapsul (Populer, Pop, Lo-fi, Elektronik, Akustik) ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genreChips.forEach { genre ->
                val isSelected = (genre == "Populer" && searchInput.isBlank() && selectedGenre == "Populer") ||
                        (selectedGenre == genre && searchInput.isNotBlank()) ||
                        (searchInput.equals(genre, ignoreCase = true))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) ElectricLime else Color(0xFF1D1F1D),
                    modifier = Modifier.clickable {
                        selectedGenre = genre
                        if (genre == "Populer") {
                            searchInput = ""
                            onSearch("")
                        } else {
                            searchInput = genre
                            onSearch(genre)
                        }
                    }
                ) {
                    Text(
                        text = genre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) OnElectricLime else Color(0xFFE0E0E0),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ─── 4. Kartu Pengaturan: Unduh hanya lewat Wi-Fi ────────────────────────
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = JetCard),
            border = BorderStroke(1.dp, Color(0xFF262826)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unduh hanya lewat Wi-Fi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hemat kuota saat mengunduh",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = downloadOnlyWifi,
                    onCheckedChange = onToggleDownloadOnlyWifi,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnElectricLime,
                        checkedTrackColor = ElectricLime,
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF2E302E)
                    )
                )
            }
        }

        // Banner Offline jika tidak ada internet
        AnimatedVisibility(visible = !isOnline) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        text = "Mode Offline: Hubungkan ke internet untuk memutar lagu baru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        val displayList = if (state.query.isNotBlank()) state.results else state.popularTracks
        val sectionTitle = if (searchInput.isNotBlank()) "Hasil pencarian" else "Sedang populer"

        // ─── 5. Konten Daftar Lagu: Sedang Populer ──────────────────────────────
        when {
            state.isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricLime)
                }
            }

            state.errorMessage != null && displayList.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tidak dapat memuat lagu Jamendo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    items(items = displayList, key = { it.id }) { track ->
                        val isPlaying = track.audio != null && track.audio == currentPlayingUri
                        val safeId = track.id.toLongOrNull() ?: (track.id.hashCode().toLong().let { if (it < 0) -it else it })
                        val isDownloaded = state.downloadedIds.contains(safeId)
                        val isDownloading = state.downloadProgress.containsKey(safeId) || state.downloadingIds.contains(safeId)
                        val downloadPercent = state.downloadProgress[safeId]

                        val isPlayable = isOnline || isDownloaded

                        JamendoTrackCard(
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
private fun JamendoTrackCard(
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
    val rowAlpha = if (isPlayable) 1f else 0.45f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JetCard),
        border = BorderStroke(1.dp, Color(0xFF222422)),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Thumbnail dengan Cover Album atau Gradien Artistik
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF222422))
            ) {
                val artUrl = track.albumImage ?: track.image
                if (!artUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    AlbumArtPlaceholder(
                        seed = track.name,
                        modifier = Modifier.matchParentSize(),
                        iconSize = 24.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Judul & Artis
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) ElectricLime else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${track.artistName ?: "Jamendo"} · ${formatDurationSeconds(track.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tombol Unduh / Hapus / Progress Lingkaran
            when {
                isDownloading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(42.dp)
                    ) {
                        val progressFloat = (downloadPercent ?: 0) / 100f
                        CircularProgressIndicator(
                            progress = { progressFloat.coerceIn(0f, 1f) },
                            color = ElectricLime,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        if (downloadPercent != null && downloadPercent > 0) {
                            Text(
                                text = "$downloadPercent%",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricLime
                            )
                        }
                    }
                }

                isDownloaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF222522),
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(onClick = onDeleteClick, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sudah diunduh",
                                    tint = ElectricLime,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF222422),
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onDownloadClick,
                            enabled = isOnline,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Unduh lagu",
                                tint = if (isOnline) Color.White else Color(0xFF666666),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
