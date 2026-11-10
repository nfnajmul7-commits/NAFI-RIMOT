package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VirtualTvState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PowerRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VirtualTvPreview(
    tvState: VirtualTvState,
    tvName: String,
    ipAddress: String,
    latencyMs: Long?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1420),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2A344A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // TV Top Header (Bezel & Status LED)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Power LED indicator
                    val ledColor by animateColorAsState(
                        targetValue = if (tvState.isPoweredOn) StatusGreen else PowerRed,
                        label = "tv_led"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ledColor)
                    )
                    Text(
                        text = tvName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (latencyMs != null) {
                        Text(
                            text = "${latencyMs}ms",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = ipAddress,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // TV Glass Screen display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (tvState.isPoweredOn) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF131D33),
                                    Color(0xFF0D1424)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF070A10),
                                    Color(0xFF05070B)
                                )
                            )
                        }
                    )
                    .border(1.dp, Color(0xFF1E273A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!tvState.isPoweredOn) {
                    // Screen Off Standby
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "TV STANDBY",
                            color = Color(0xFF4A5568),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Press Power button on remote to turn ON",
                            color = Color(0xFF334155),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    // Screen ON Active Interface
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top info line on TV screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "▶ ${tvState.activeApp.uppercase()}",
                                color = AccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tvState.channelName,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Simulated TV UI Menu Grid (3x3 with selected focus)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val items = listOf("Home", "Cinema", "Apps", "Live TV", "HDMI", "Music")
                            items.take(4).forEachIndexed { index, title ->
                                val isFocused = (tvState.selectedMenuIndex % 4) == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isFocused) AccentCyan else Color(0xFF1E283D))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isFocused) Color.Black else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Bottom OSD (Volume / Last Command Overlay)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Volume bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (tvState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (tvState.isMuted) PowerRed else AccentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (tvState.isMuted) "MUTED" else "VOL ${tvState.volume}%",
                                    color = if (tvState.isMuted) PowerRed else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF263248))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(if (tvState.isMuted) 0f else (tvState.volume / 100f))
                                            .height(4.dp)
                                            .background(if (tvState.isMuted) PowerRed else AccentCyan)
                                    )
                                }
                            }

                            // Command received tag
                            Text(
                                text = "⚡ ${tvState.lastCommandReceived}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
