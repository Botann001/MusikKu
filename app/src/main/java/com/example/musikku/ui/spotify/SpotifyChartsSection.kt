package com.example.musikku.ui.spotify

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.musikku.R
import coil.compose.AsyncImage
import com.example.musikku.ui.theme.EmeraldSpotify
import com.example.musikku.ui.theme.TextMuted

/**
 * Representasi item chart Spotify unggulan di Beranda.
 */
data class SpotifyChartItem(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val badgeText: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

/**
 * Daftar chart & playlist Spotify terkurasi dan terpopuler untuk Beranda.
 */
val defaultSpotifyCharts = listOf(
    SpotifyChartItem(
        id = "37i9dQZEVXbObFQZ3JLcXt",
        title = "Top 50 - Indonesia",
        description = "Update harian lagu paling sering diputar di Indonesia",
        coverUrl = "https://charts-images.scdn.co/assets/locale_en/regional/daily/region_id_default.jpg",
        badgeText = "NO. 1 INDONESIA",
        primaryColor = Color(0xFF1DB954),
        secondaryColor = Color(0xFF0C3D1B)
    ),
    SpotifyChartItem(
        id = "37i9dQZF1DXa2EiKmMLhFD",
        title = "Hot Hits Indonesia",
        description = "Lagu-lagu terpanas dan terfavorit saat ini",
        coverUrl = "https://i.scdn.co/image/ab67706f000000024c0f879869b8da15659b8cea",
        badgeText = "HOT HITS",
        primaryColor = Color(0xFFFF5722),
        secondaryColor = Color(0xFF5A1C08)
    ),
    SpotifyChartItem(
        id = "37i9dQZF1DWWhB4HOWKFQc",
        title = "Lagi Viral",
        description = "Deretan lagu yang sedang viral & tren di sosmed",
        coverUrl = "https://i.scdn.co/image/ab67706f000000024c5b4a8f1e4e19720293fd6b",
        badgeText = "LAGI VIRAL",
        primaryColor = Color(0xFF8338EC),
        secondaryColor = Color(0xFF2C1052)
    ),
    SpotifyChartItem(
        id = "37i9dQZF1DX6yQB7bkflag",
        title = "Pop Rising Indonesia",
        description = "Bintang pop baru Indonesia yang sedang bersinar",
        coverUrl = "https://i.scdn.co/image/ab67706f000000029f3cc3d3846f7328394e2eca",
        badgeText = "POP RISING",
        primaryColor = Color(0xFF06D6A0),
        secondaryColor = Color(0xFF024231)
    ),
    SpotifyChartItem(
        id = "37i9dQZF1DXcBWIGoYBM5M",
        title = "Today's Top Hits",
        description = "Hits internasional paling populer hari ini",
        coverUrl = "https://i.scdn.co/image/ab67706f00000002ffe80c69d2e795ea01e63447",
        badgeText = "TOP HITS",
        primaryColor = Color(0xFFFF006E),
        secondaryColor = Color(0xFF590026)
    ),
    SpotifyChartItem(
        id = "37i9dQZEVXbMDoHDwVN2tF",
        title = "Top 50 - Global",
        description = "Lagu nomor satu terpopuler dari seluruh dunia",
        coverUrl = "https://charts-images.scdn.co/assets/locale_en/regional/daily/region_global_default.jpg",
        badgeText = "GLOBAL TOP",
        primaryColor = Color(0xFF3A86FF),
        secondaryColor = Color(0xFF102E66)
    )
)

/**
 * Ikon vektor Spotify tiga gelombang suara autentik.
 */
@Composable
fun SpotifyEmblem(
    modifier: Modifier = Modifier,
    circleBackground: Color = EmeraldSpotify,
    waveTint: Color = Color.Black
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(circleBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.65f)) {
            val w = size.width
            val h = size.height

            // 3 gelombang melengkung Spotify (atas, tengah, bawah)
            val stroke1 = w * 0.12f
            val stroke2 = w * 0.10f
            val stroke3 = w * 0.085f

            // Gelombang atas
            val p1 = Path().apply {
                moveTo(w * 0.18f, h * 0.36f)
                quadraticTo(w * 0.50f, h * 0.20f, w * 0.82f, h * 0.32f)
            }
            drawPath(p1, waveTint, style = Stroke(stroke1, cap = StrokeCap.Round))

            // Gelombang tengah
            val p2 = Path().apply {
                moveTo(w * 0.24f, h * 0.54f)
                quadraticTo(w * 0.50f, h * 0.40f, w * 0.78f, h * 0.50f)
            }
            drawPath(p2, waveTint, style = Stroke(stroke2, cap = StrokeCap.Round))

            // Gelombang bawah
            val p3 = Path().apply {
                moveTo(w * 0.30f, h * 0.72f)
                quadraticTo(w * 0.50f, h * 0.60f, w * 0.72f, h * 0.68f)
            }
            drawPath(p3, waveTint, style = Stroke(stroke3, cap = StrokeCap.Round))
        }
    }
}

/**
 * Section Spotify Charts & Trending di Beranda (Tahap 2).
 */
@Composable
fun SpotifyChartsSection(
    charts: List<SpotifyChartItem> = defaultSpotifyCharts,
    accountConnected: Boolean = false,
    accountUserName: String = "",
    accountUserAvatar: String = "",
    onChartClick: (SpotifyChartItem) -> Unit,
    onQuickPlayClick: (SpotifyChartItem) -> Unit,
    onImportLinkClick: () -> Unit,
    onOpenAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // ─── Header Section: Spotify Badge, Judul, & Tombol Aksi ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpotifyEmblem(
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(R.string.home_spotify_charts),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.home_spotify_charts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tombol Aksi: Akun Spotify & Impor Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tombol Akun Spotify
                Surface(
                    onClick = onOpenAccountClick,
                    shape = RoundedCornerShape(50),
                    color = if (accountConnected) EmeraldSpotify.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    contentColor = if (accountConnected) EmeraldSpotify else Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (accountConnected && accountUserAvatar.isNotBlank()) {
                            AsyncImage(
                                model = accountUserAvatar,
                                contentDescription = "Spotify User",
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Akun Spotify",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (accountConnected && accountUserName.isNotBlank()) {
                                accountUserName.split(" ").firstOrNull()?.take(8) ?: stringResource(R.string.home_account)
                            } else {
                                stringResource(R.string.home_account)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Tombol Cepat Impor Link
                Surface(
                    onClick = onImportLinkClick,
                    shape = RoundedCornerShape(50),
                    color = EmeraldSpotify.copy(alpha = 0.15f),
                    contentColor = EmeraldSpotify
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Impor Link",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_import),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─── Daftar Kartu Chart Spotify (LazyRow Horizontal) ─────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(charts, key = { it.id }) { chart ->
                SpotifyChartCard(
                    chart = chart,
                    onClick = { onChartClick(chart) },
                    onQuickPlay = { onQuickPlayClick(chart) }
                )
            }
        }
    }
}

/**
 * Kartu individual Spotify Chart dengan visual artwork, badge tag, dan tombol play mengambang.
 */
@Composable
fun SpotifyChartCard(
    chart: SpotifyChartItem,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1E211E),
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Gradient Halus Berdasarkan Warna Chart
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    chart.primaryColor.copy(alpha = 0.45f),
                                    chart.secondaryColor,
                                    Color(0xFF121412)
                                )
                            )
                        )
                )

                // Gambar Cover Art Spotify
                AsyncImage(
                    model = chart.coverUrl,
                    contentDescription = chart.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )

                // Lapisan Proteksi Gradien Bawah agar badge & tombol tetap kontras
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Badge Tag Spotify di Kiri Atas
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpotifyEmblem(modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chart.badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Tombol Putar Cepat Mengambang di Kanan Bawah
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(EmeraldSpotify)
                        .clickable(onClick = onQuickPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar Langsung",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Judul Chart
        Text(
            text = chart.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Deskripsi Singkat Chart
        Text(
            text = chart.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}
