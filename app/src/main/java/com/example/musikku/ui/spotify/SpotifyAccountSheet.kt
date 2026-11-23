package com.example.musikku.ui.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.data.repository.SpotifyUserPlaylist
import com.example.musikku.ui.common.rememberSheetScrollBlocker
import com.example.musikku.ui.library.SpotifyAccountState
import com.example.musikku.ui.theme.EmeraldSpotify
import com.example.musikku.ui.theme.TextMuted

/**
 * Bottom sheet untuk mengelola Akun Spotify pengguna (Tahap 3).
 * Mendukung Login via OAuth PKCE, Token Cepat, Sinkronisasi Liked Songs,
 * dan Impor Playlist pribadi ke database Room MusikKu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyAccountSheet(
    accountState: SpotifyAccountState,
    onDismiss: () -> Unit,
    onLoginClick: (Context, String) -> Unit,
    onConnectTokenClick: (String) -> Unit,
    onSyncLikedSongs: () -> Unit,
    onImportPlaylist: (SpotifyUserPlaylist) -> Unit,
    onRefreshData: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollBlocker = rememberSheetScrollBlocker()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedAuthTab by remember { mutableIntStateOf(0) } // 0: Login Browser (PKCE), 1: Token Cepat
    var customClientId by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141714),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ─── Header: Badge Spotify, Judul, & Tombol Tutup ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = EmeraldSpotify.copy(alpha = 0.15f),
                    contentColor = EmeraldSpotify
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpotifyEmblem(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SINKRONISASI AKUN SPOTIFY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (accountState.isConnected) {
                // ═════════════════════════════════════════════════════════════
                // TAMPILAN JIKA SUDAH TERHUBUNG (CONNECTED)
                // ═════════════════════════════════════════════════════════════
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profil Pengguna Spotify
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E241E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSpotify.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSpotify.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!accountState.profile?.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = accountState.profile?.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = accountState.profile?.displayName?.take(1)?.uppercase() ?: "S",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            color = EmeraldSpotify
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = accountState.profile?.displayName ?: "Pengguna Spotify",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Terverifikasi",
                                            tint = EmeraldSpotify,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Terhubung • ${accountState.profile?.product?.uppercase() ?: "SPOTIFY"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldSpotify,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(
                                    onClick = onRefreshData,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Segarkan",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Info Kebijakan Spotify untuk Akun Free
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF262013),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Catatan Kebijakan API Spotify (2024):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFB74D))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Spotify mewajibkan akun pembuat App memiliki langganan Spotify Premium aktif untuk sinkronisasi otomatis via API.\n\n" +
                                            "💡 Solusi Gratis (Tanpa Perlu Premium):\n" +
                                            "1. Di Spotify, buat daftar putar (atau masukkan Lagu yang Disukai ke playlist).\n" +
                                            "2. Tekan Bagikan -> Salin Tautan ke Daftar Putar.\n" +
                                            "3. Tempelkan tautan tersebut menggunakan tombol 'Impor' di Beranda MusikKu (100% Gratis & tanpa batas).",
                                    fontSize = 11.5.sp,
                                    color = Color.White.copy(alpha = 0.88f),
                                    lineHeight = 16.5.sp
                                )
                            }
                        }
                    }

                    // 2. Aksi 1: Sinkronkan Lagu yang Disukai (Liked Songs)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E211E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE91E63).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFFE91E63),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Lagu yang Disukai",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Impor ke tab Disukai MusikKu",
                                            color = TextMuted,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = onSyncLikedSongs,
                                    enabled = !accountState.isSyncingLikedSongs,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSpotify,
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (accountState.isSyncingLikedSongs) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sinkron", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Aksi 2: Daftar Playlist Pengguna Spotify
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Playlist Spotify Anda (${accountState.userPlaylists.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "Pilih playlist untuk disalin ke Daftar Putar Kamu di MusikKu",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    if (accountState.userPlaylists.isEmpty() && !accountState.isLoading) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Belum ada playlist yang terdeteksi di akun Spotify Anda.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(accountState.userPlaylists, key = { it.id }) { pl ->
                            val isSyncingThis = accountState.syncingPlaylistId == pl.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E211E),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2A2E2A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!pl.coverUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = pl.coverUrl,
                                                contentDescription = pl.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = null,
                                                tint = EmeraldSpotify,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.trackCount} lagu",
                                            color = TextMuted,
                                            fontSize = 11.5.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onImportPlaylist(pl) },
                                        enabled = !isSyncingThis,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = EmeraldSpotify
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSpotify.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        if (isSyncingThis) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = EmeraldSpotify,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Impor", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Tombol Putuskan Sambungan
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF5252)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putuskan Sambungan Akun Spotify", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // ═════════════════════════════════════════════════════════════
                // TAMPILAN JIKA BELUM TERHUBUNG (NOT CONNECTED)
                // ═════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Banner Penjelasan
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1B221B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSpotify.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpotifyEmblem(modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Hubungkan Akun Spotify Kamu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Otomatis bawa semua 'Lagu yang Disukai' (Liked Songs) serta playlist buatanmu dari Spotify ke MusikKu untuk diputar bebas tanpa DRM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Tab Pilihan Metode Login
                    PrimaryTabRow(
                        selectedTabIndex = selectedAuthTab,
                        containerColor = Color.Transparent,
                        contentColor = EmeraldSpotify,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedAuthTab),
                                color = EmeraldSpotify
                            )
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedAuthTab == 0,
                            onClick = { selectedAuthTab = 0 },
                            text = {
                                Text(
                                    "Login Browser (PKCE)",
                                    fontWeight = if (selectedAuthTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedAuthTab == 1,
                            onClick = { selectedAuthTab = 1 },
                            text = {
                                Text(
                                    "Token Cepat (Manual)",
                                    fontWeight = if (selectedAuthTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp
                                )
                            }
                        )
                    }

                    if (selectedAuthTab == 0) {
                        // ─── Metode 1: OAuth PKCE Browser ────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Masuk secara resmi melalui browser atau aplikasi Spotify. Membutuhkan Spotify Client ID dari dashboard pengembang.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )

                            // Kartu Panduan Client ID
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF161F17),
                                border = BorderStroke(1.dp, EmeraldSpotify.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldSpotify, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Panduan Setup Client ID (1 Menit):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldSpotify)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "1. Buka developer.spotify.com/dashboard lalu buat App.\n" +
                                                "2. Tambahkan Redirect URI:\n" +
                                                "   musikku://spotify-callback\n" +
                                                "3. Salin Client ID dan tempelkan di bawah ini.",
                                        fontSize = 11.5.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("musikku://spotify-callback"))
                                                Toast.makeText(context, "Redirect URI disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Salin URI", fontSize = 11.sp, maxLines = 1)
                                        }
                                        Button(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.spotify.com/dashboard")).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSpotify, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dashboard", fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customClientId,
                                onValueChange = { customClientId = it },
                                label = { Text("Spotify Client ID") },
                                placeholder = { Text("Tempel Client ID di sini...") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text.orEmpty()
                                            if (clip.isNotBlank()) customClientId = clip.trim()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Tempel", tint = EmeraldSpotify)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSpotify,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = EmeraldSpotify,
                                    unfocusedLabelColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onLoginClick(context, customClientId) },
                                enabled = customClientId.isNotBlank(),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSpotify,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk dengan Akun Spotify", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // ─── Metode 2: Token Cepat Manual ────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Cara termudah & instan tanpa membuat App di dashboard. Cukup salin Access Token akun Spotify Anda dari Web Console lalu tempel di bawah ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.spotify.com/documentation/web-api/tutorials/getting-started")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Panduan & Token Console Spotify", fontSize = 12.sp)
                            }

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("Spotify Access Token") },
                                placeholder = { Text("Tempel OAuth Token...") },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text.orEmpty()
                                            if (clip.isNotBlank()) tokenInput = clip.trim()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Tempel", tint = EmeraldSpotify)
                                    }
                                },
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldSpotify,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = EmeraldSpotify,
                                    unfocusedLabelColor = TextMuted
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { onConnectTokenClick(tokenInput) },
                                enabled = tokenInput.isNotBlank() && !accountState.isLoading,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSpotify,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (accountState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hubungkan dengan Token", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
