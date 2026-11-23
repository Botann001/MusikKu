package com.example.musikku.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.musikku.ui.theme.ElectricLime
import com.example.musikku.ui.theme.TextMuted
import java.io.File
import java.io.FileOutputStream

/**
 * Avatar Foto Profil Pengguna (Gaya Spotify).
 * Menampilkan foto profil dari URI file lokal atau inisial nama dengan gradien hijau zamrud.
 */
@Composable
fun UserProfileAvatar(
    userName: String,
    avatarUri: String,
    size: Dp = 38.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickModifier)
    ) {
        if (avatarUri.isNotBlank()) {
            val imageModel: Any = if (avatarUri.startsWith("/") || avatarUri.startsWith("file://")) {
                File(avatarUri.removePrefix("file://"))
            } else {
                avatarUri
            }
            AsyncImage(
                model = imageModel,
                contentDescription = userName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFF67E8A6).copy(alpha = 0.8f), CircleShape)
            )
        } else {
            val initial = userName.trim().firstOrNull()?.uppercaseChar()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1DB954), Color(0xFF0D5A2E))
                        )
                    )
                    .border(1.5.dp, Color(0xFFBAFF5C).copy(alpha = 0.7f), CircleShape)
            ) {
                if (initial != null) {
                    Text(
                        text = initial.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profil",
                        tint = Color.White,
                        modifier = Modifier.size((size.value * 0.55f).dp)
                    )
                }
            }
        }
    }
}

/**
 * Konten Panel Geser Samping (*Slide-over Drawer*) Profil Pengguna persis gaya Spotify.
 */
@Composable
fun SpotifyProfileDrawerContent(
    userName: String,
    avatarUri: String,
    userBio: String,
    totalSongs: Int,
    totalFavorites: Int,
    totalPlaylists: Int,
    listeningStats: com.example.musikku.data.repository.ListeningTimeStats = com.example.musikku.data.repository.ListeningTimeStats(),
    onEditProfile: () -> Unit,
    onViewStats: () -> Unit,
    onRecentlyPlayed: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF121412))
            .statusBarsPadding()
            .padding(vertical = 16.dp, horizontal = 18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ─── 1. Header Kartu Profil Pengguna (Sesuai Gambar 2 Spotify) ─────────────
        Surface(
            onClick = onEditProfile,
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserProfileAvatar(
                    userName = userName,
                    avatarUri = avatarUri,
                    size = 54.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "Pengguna" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (userName.isBlank()) stringResource(R.string.profile_edit_name) else stringResource(R.string.profile_view_profile),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // ─── 2. Daftar Menu Spotify (Statistik, Baru Diputar, Pengaturan) ────────
        DrawerMenuItem(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            label = stringResource(R.string.profile_listening_stats),
            subtitle = "${listeningStats.weeklyFormattedCompact} minggu ini • $totalSongs Lagu",
            onClick = onViewStats
        )

        Spacer(modifier = Modifier.height(6.dp))

        DrawerMenuItem(
            icon = Icons.Default.History,
            label = stringResource(R.string.profile_recently_played),
            subtitle = stringResource(R.string.profile_recently_played_desc),
            onClick = onRecentlyPlayed
        )

        Spacer(modifier = Modifier.height(6.dp))

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.profile_settings_privacy),
            subtitle = stringResource(R.string.profile_settings_privacy_desc),
            onClick = onOpenSettings
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(28.dp))

        // ─── 3. Bio & Versi Aplikasi ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            if (userBio.isNotBlank()) {
                Text(
                    text = "\"$userBio\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "MusikKu v1.0.0 • Modern Music Player",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ElectricLime,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Dialog Edit Profil Pengguna (Ubah Foto dari Galeri, Nama, & Bio).
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    currentAvatarUri: String,
    currentBio: String,
    onSave: (name: String, avatarUri: String, bio: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(currentName) }
    var bioInput by remember { mutableStateOf(currentBio) }
    var selectedAvatarUri by remember { mutableStateOf(currentAvatarUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val profileFile = File(context.filesDir, "user_profile_avatar.jpg")
                FileOutputStream(profileFile).use { output ->
                    inputStream?.copyTo(output)
                }
                selectedAvatarUri = profileFile.absolutePath
            } catch (e: Exception) {
                selectedAvatarUri = it.toString()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E221E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Edit Profil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Foto profil dengan overlay tombol kamera
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(86.dp)
                        .clickable { photoPickerLauncher.launch("image/*") }
                ) {
                    UserProfileAvatar(
                        userName = nameInput,
                        avatarUri = selectedAvatarUri,
                        size = 86.dp
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ElectricLime)
                            .border(2.dp, Color(0xFF1E221E), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "Ketuk untuk memilih foto dari galeri",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                // Input Nama
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Pengguna") },
                    placeholder = { Text("Masukkan nama pengguna...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = ElectricLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Input Bio / Motto
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("Status / Motto Musik") },
                    placeholder = { Text("Tulis status atau motto...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = ElectricLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(nameInput.trim(), selectedAvatarUri, bioInput.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * Dialog Statistik Mendengarkan Musik Pengguna (Mingguan, Bulanan, Tahunan, dan Koleksi).
 */
@Composable
fun ListeningStatsDialog(
    totalSongs: Int,
    totalFavorites: Int,
    totalPlaylists: Int,
    listeningStats: com.example.musikku.data.repository.ListeningTimeStats = com.example.musikku.data.repository.ListeningTimeStats(),
    isPlaying: Boolean = false,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E221E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ─── Bagian 1: Waktu Mendengarkan Musik (Minggu, Bulan, Tahun) ─────────
                Text(
                    text = stringResource(R.string.stats_time_section),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ElectricLime,
                    letterSpacing = 1.2.sp
                )

                // 1. Minggu Ini
                ListeningTimeCard(
                    periodLabel = stringResource(R.string.stats_this_week),
                    durationText = listeningStats.weeklyFormatted,
                    subtext = "Rata-rata: ${listeningStats.dailyAverageFormatted} / hari",
                    badgeText = if (isPlaying) "● Live Memutar" else "Tersimpan",
                    badgeColor = if (isPlaying) ElectricLime else Color(0xFF67E8A6)
                )

                // 2. Bulan Ini
                ListeningTimeCard(
                    periodLabel = stringResource(R.string.stats_this_month),
                    durationText = listeningStats.monthlyFormatted,
                    subtext = stringResource(R.string.stats_accumulation_month),
                    badgeText = if (isPlaying) "Bertambah" else "Tersimpan",
                    badgeColor = Color(0xFF60A5FA)
                )

                // 3. Tahun Ini
                ListeningTimeCard(
                    periodLabel = stringResource(R.string.stats_this_year),
                    durationText = listeningStats.yearlyFormatted,
                    subtext = stringResource(R.string.stats_total_year),
                    badgeText = if (isPlaying) "Bertambah" else "Tersimpan",
                    badgeColor = Color(0xFFFBBF24)
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                // ─── Bagian 2: Koleksi & Interaksi ──────────────────────────────────
                Text(
                    text = stringResource(R.string.stats_collection_section),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.2.sp
                )

                StatCardItem(
                    icon = Icons.Default.LibraryMusic,
                    title = stringResource(R.string.stats_music_collection),
                    value = "$totalSongs Lagu",
                    desc = stringResource(R.string.stats_music_collection_desc)
                )
                StatCardItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.stats_liked_songs),
                    value = "$totalFavorites Lagu",
                    desc = stringResource(R.string.stats_liked_songs_desc)
                )
                StatCardItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = stringResource(R.string.library_your_playlists),
                    value = "$totalPlaylists Playlist",
                    desc = "Playlist buatan Anda sendiri"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.stats_close), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Kartu durasi mendengarkan musik dengan aksen warna dan badge status.
 */
@Composable
private fun ListeningTimeCard(
    periodLabel: String,
    durationText: String,
    subtext: String,
    badgeText: String,
    badgeColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.40f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatCardItem(
    icon: ImageVector,
    title: String,
    value: String,
    desc: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF67E8A6).copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF67E8A6),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
