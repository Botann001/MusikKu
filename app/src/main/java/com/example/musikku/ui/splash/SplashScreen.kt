package com.example.musikku.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animations")

    // Animasi denyut halus logo
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // Animasi gelombang pendaran (glow ripple)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Animasi bar equalizer mini
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 22f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08140E),
                        Color(0xFF0E2419),
                        Color(0xFF07110C)
                    )
                )
            )
    ) {
        // Lingkaran pendaran ambient blur di latar belakang
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale * 1.15f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBAFF5C).copy(alpha = glowAlpha * 0.35f),
                            Color(0xFF104E35).copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Container Logo Kaca Frosted Glass dengan Aksen Neon Lime
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(116.dp)
                    .scale(scale)
            ) {
                // Outer subtle ring
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(
                            BorderStroke(1.5.dp, Color(0xFFBAFF5C).copy(alpha = 0.35f)),
                            CircleShape
                        )
                )

                // Inner frosted container
                Surface(
                    shape = CircleShape,
                    color = Color(0xD0132B1F),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    shadowElevation = 18.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "MusikKu Logo",
                            tint = Color(0xFFBAFF5C),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nama Aplikasi "MusikKu"
            Text(
                text = "MusikKu",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badge Pill Subtitle
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBAFF5C))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pemutar Musik Hi-Fi & Bebas Royalti",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Indikator Equalizer Animasi Hidup
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar1.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBAFF5C))
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF80FFA2))
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBAFF5C))
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(bar1.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF80FFA2))
                )
            }
        }

        // Teks Footer di bagian bawah layar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "VERSION 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.35f),
                letterSpacing = 2.sp
            )
        }
    }
}
