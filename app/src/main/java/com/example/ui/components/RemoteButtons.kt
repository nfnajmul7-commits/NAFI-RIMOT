package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RemoteCommand
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.ButtonBorder
import com.example.ui.theme.ButtonSurface
import com.example.ui.theme.ButtonSurfacePressed
import com.example.ui.theme.DisneyBlue
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.PowerRed
import com.example.ui.theme.PowerRedGlow
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.RemoteCard
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed

/**
 * Tactile rounded circular button with press scaling and feedback
 */
@Composable
fun TactileIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    iconSize: Dp = 24.dp,
    containerColor: Color = ButtonSurface,
    contentColor: Color = TextPrimary,
    glowColor: Color? = null,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (glowColor != null) 8.dp else 4.dp,
                shape = CircleShape,
                ambientColor = glowColor ?: Color.Black,
                spotColor = glowColor ?: Color.Black
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed) listOf(
                        ButtonSurfacePressed,
                        containerColor
                    ) else listOf(containerColor, ButtonSurface)
                )
            )
            .border(
                width = 1.dp,
                color = if (isPressed) AccentCyan else (glowColor ?: ButtonBorder),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag.ifBlank { contentDescription }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) AccentCyan else contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * High-precision D-Pad navigation wheel with Center OK button
 */
@Composable
fun DPadCluster(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val dpadSize = 220.dp
    val okButtonSize = 84.dp

    Box(
        modifier = modifier
            .size(dpadSize)
            .shadow(12.dp, CircleShape, spotColor = AccentCyanGlow)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ButtonSurface,
                        RemoteCard
                    )
                )
            )
            .border(1.5.dp, ButtonBorder, CircleShape)
            .testTag("dpad_cluster"),
        contentAlignment = Alignment.Center
    ) {
        // UP Button
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDescription = "DPad Up",
                onClick = { onCommand(RemoteCommand.DPAD_UP) },
                testTag = "dpad_up"
            )
        }

        // DOWN Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "DPad Down",
                onClick = { onCommand(RemoteCommand.DPAD_DOWN) },
                testTag = "dpad_down"
            )
        }

        // LEFT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
        ) {
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowLeft,
                contentDescription = "DPad Left",
                onClick = { onCommand(RemoteCommand.DPAD_LEFT) },
                testTag = "dpad_left"
            )
        }

        // RIGHT Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        ) {
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "DPad Right",
                onClick = { onCommand(RemoteCommand.DPAD_RIGHT) },
                testTag = "dpad_right"
            )
        }

        // OK Center Button
        val okInteraction = remember { MutableInteractionSource() }
        val okPressed by okInteraction.collectIsPressedAsState()
        val okScale by animateFloatAsState(
            targetValue = if (okPressed) 0.90f else 1.0f,
            label = "ok_scale"
        )

        Box(
            modifier = Modifier
                .size(okButtonSize)
                .scale(okScale)
                .shadow(8.dp, CircleShape, spotColor = AccentCyan)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (okPressed) listOf(
                            AccentCyan,
                            AccentBlue
                        ) else listOf(
                            Color(0xFF2C3852),
                            Color(0xFF1B2333)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            AccentCyan,
                            AccentBlue,
                            AccentCyan
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = okInteraction,
                    indication = null,
                    onClick = { onCommand(RemoteCommand.OK) }
                )
                .testTag("dpad_ok_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = if (okPressed) Color.Black else AccentCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun DPadDirectionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, label = "dir_scale")

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) AccentCyan else TextPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

/**
 * Volume and Channel Rocker Pods
 */
@Composable
fun VolumeChannelRockers(
    onCommand: (RemoteCommand) -> Unit,
    isMuted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume Rocker Pod
        RockerPod(
            title = "VOL",
            onUp = { onCommand(RemoteCommand.VOLUME_UP) },
            onDown = { onCommand(RemoteCommand.VOLUME_DOWN) },
            upIcon = Icons.Default.VolumeUp,
            downIcon = Icons.Default.VolumeDown,
            upTag = "vol_up_button",
            downTag = "vol_down_button"
        )

        // Center Mute & Input Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MUTE button
            TactileIconButton(
                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                contentDescription = "Mute",
                onClick = { onCommand(RemoteCommand.MUTE) },
                contentColor = if (isMuted) PowerRed else TextPrimary,
                glowColor = if (isMuted) PowerRedGlow else null,
                size = 50.dp,
                testTag = "mute_button"
            )

            // SOURCE / INPUT button
            TactileIconButton(
                icon = Icons.Default.Input,
                contentDescription = "Source Input",
                onClick = { onCommand(RemoteCommand.INPUT) },
                contentColor = AccentCyan,
                size = 50.dp,
                testTag = "source_input_button"
            )
        }

        // Channel Rocker Pod
        RockerPod(
            title = "CH",
            onUp = { onCommand(RemoteCommand.CHANNEL_UP) },
            onDown = { onCommand(RemoteCommand.CHANNEL_DOWN) },
            upIcon = Icons.Default.KeyboardArrowUp,
            downIcon = Icons.Default.KeyboardArrowDown,
            upTag = "ch_up_button",
            downTag = "ch_down_button"
        )
    }
}

@Composable
private fun RockerPod(
    title: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    upIcon: ImageVector,
    downIcon: ImageVector,
    upTag: String,
    downTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(68.dp)
            .height(148.dp)
            .shadow(8.dp, RoundedCornerShape(34.dp)),
        shape = RoundedCornerShape(34.dp),
        color = ButtonSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Up)
            RockerHalfButton(
                icon = upIcon,
                contentDescription = "$title Up",
                onClick = onUp,
                isTop = true,
                testTag = upTag
            )

            // Center Label
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            // Bottom Section (Down)
            RockerHalfButton(
                icon = downIcon,
                contentDescription = "$title Down",
                onClick = onDown,
                isTop = false,
                testTag = downTag
            )
        }
    }
}

@Composable
private fun RockerHalfButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isTop: Boolean,
    testTag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(
                if (isTop) RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
                else RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
            )
            .background(if (isPressed) ButtonSurfacePressed else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPressed) AccentCyan else TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Navigation Bar row (Back, Home, Menu)
 */
@Composable
fun SystemNavRow(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TactileIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Back",
            onClick = { onCommand(RemoteCommand.BACK) },
            testTag = "nav_back_button"
        )

        TactileIconButton(
            icon = Icons.Default.Home,
            contentDescription = "Home",
            onClick = { onCommand(RemoteCommand.HOME) },
            contentColor = AccentCyan,
            size = 60.dp,
            iconSize = 28.dp,
            testTag = "nav_home_button"
        )

        TactileIconButton(
            icon = Icons.Default.Menu,
            contentDescription = "Menu",
            onClick = { onCommand(RemoteCommand.MENU) },
            testTag = "nav_menu_button"
        )
    }
}

/**
 * Media playback row (Rewind, Play/Pause, Fast Forward, Stop)
 */
@Composable
fun MediaPlaybackRow(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TactileIconButton(
            icon = Icons.Default.FastRewind,
            contentDescription = "Rewind",
            onClick = { onCommand(RemoteCommand.REWIND) },
            size = 46.dp,
            iconSize = 22.dp,
            testTag = "media_rewind"
        )

        TactileIconButton(
            icon = Icons.Default.PlayArrow,
            contentDescription = "Play/Pause",
            onClick = { onCommand(RemoteCommand.PLAY_PAUSE) },
            size = 54.dp,
            iconSize = 26.dp,
            contentColor = AccentCyan,
            glowColor = AccentCyanGlow,
            testTag = "media_play_pause"
        )

        TactileIconButton(
            icon = Icons.Default.FastForward,
            contentDescription = "Fast Forward",
            onClick = { onCommand(RemoteCommand.FAST_FORWARD) },
            size = 46.dp,
            iconSize = 22.dp,
            testTag = "media_fast_forward"
        )

        TactileIconButton(
            icon = Icons.Default.Stop,
            contentDescription = "Stop",
            onClick = { onCommand(RemoteCommand.STOP) },
            size = 46.dp,
            iconSize = 22.dp,
            testTag = "media_stop"
        )
    }
}

/**
 * Quick App Launch Grid (Netflix, YouTube, Prime, Disney+, Spotify, Browser)
 */
@Composable
fun QuickAppsGrid(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppButton(
                title = "NETFLIX",
                color = NetflixRed,
                onClick = { onCommand(RemoteCommand.APP_NETFLIX) },
                modifier = Modifier.weight(1f),
                testTag = "app_netflix"
            )
            AppButton(
                title = "YouTube",
                color = YouTubeRed,
                onClick = { onCommand(RemoteCommand.APP_YOUTUBE) },
                modifier = Modifier.weight(1f),
                testTag = "app_youtube"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppButton(
                title = "prime video",
                color = PrimeBlue,
                onClick = { onCommand(RemoteCommand.APP_PRIME) },
                modifier = Modifier.weight(1f),
                testTag = "app_prime"
            )
            AppButton(
                title = "Disney+",
                color = DisneyBlue,
                onClick = { onCommand(RemoteCommand.APP_DISNEY) },
                modifier = Modifier.weight(1f),
                testTag = "app_disney"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppButton(
                title = "Spotify",
                color = SpotifyGreen,
                onClick = { onCommand(RemoteCommand.APP_SPOTIFY) },
                modifier = Modifier.weight(1f),
                testTag = "app_spotify"
            )
            AppButton(
                title = "Browser",
                color = AccentCyan,
                onClick = { onCommand(RemoteCommand.APP_BROWSER) },
                modifier = Modifier.weight(1f),
                testTag = "app_browser"
            )
        }
    }
}

@Composable
private fun AppButton(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Surface(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (isPressed) ButtonSurfacePressed else ButtonSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Numeric Keypad with Color Keys
 */
@Composable
fun NumpadCluster(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color keys row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorKeyButton(Color(0xFFE53935), "Red") { onCommand(RemoteCommand.COLOR_RED) }
            ColorKeyButton(Color(0xFF43A047), "Green") { onCommand(RemoteCommand.COLOR_GREEN) }
            ColorKeyButton(Color(0xFFFDD835), "Yellow") { onCommand(RemoteCommand.COLOR_YELLOW) }
            ColorKeyButton(Color(0xFF1E88E5), "Blue") { onCommand(RemoteCommand.COLOR_BLUE) }
        }

        // 1 2 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumButton("1") { onCommand(RemoteCommand.NUM_1) }
            NumButton("2") { onCommand(RemoteCommand.NUM_2) }
            NumButton("3") { onCommand(RemoteCommand.NUM_3) }
        }

        // 4 5 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumButton("4") { onCommand(RemoteCommand.NUM_4) }
            NumButton("5") { onCommand(RemoteCommand.NUM_5) }
            NumButton("6") { onCommand(RemoteCommand.NUM_6) }
        }

        // 7 8 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumButton("7") { onCommand(RemoteCommand.NUM_7) }
            NumButton("8") { onCommand(RemoteCommand.NUM_8) }
            NumButton("9") { onCommand(RemoteCommand.NUM_9) }
        }

        // Guide, 0, OK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumButton("Guide", isSecondary = true) { onCommand(RemoteCommand.GUIDE) }
            NumButton("0") { onCommand(RemoteCommand.NUM_0) }
            NumButton("OK", isSecondary = true) { onCommand(RemoteCommand.OK) }
        }
    }
}

@Composable
private fun NumButton(
    label: String,
    isSecondary: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    Surface(
        modifier = Modifier
            .size(if (isSecondary) 64.dp else 60.dp)
            .shadow(4.dp, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag("num_button_$label"),
        shape = CircleShape,
        color = if (isPressed) ButtonSurfacePressed else ButtonSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ButtonBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSecondary) AccentCyan else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (label.length > 2) 13.sp else 20.sp
            )
        }
    }
}

@Composable
private fun ColorKeyButton(
    color: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color)
            .clickable(onClick = onClick)
            .testTag("color_key_${contentDescription.lowercase()}")
    )
}

/**
 * Touchpad / Swipe Surface to swipe Up/Down/Left/Right or tap OK
 */
@Composable
fun TouchpadSurface(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var accumulatedDy by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 65f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 24.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .border(1.5.dp, ButtonBorder, RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDx += dragAmount.x
                        accumulatedDy += dragAmount.y

                        if (Math.abs(accumulatedDx) > swipeThreshold) {
                            if (accumulatedDx > 0) {
                                onCommand(RemoteCommand.DPAD_RIGHT)
                            } else {
                                onCommand(RemoteCommand.DPAD_LEFT)
                            }
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                        } else if (Math.abs(accumulatedDy) > swipeThreshold) {
                            if (accumulatedDy > 0) {
                                onCommand(RemoteCommand.DPAD_DOWN)
                            } else {
                                onCommand(RemoteCommand.DPAD_UP)
                            }
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                        }
                    }
                )
            }
            .clickable { onCommand(RemoteCommand.OK) }
            .testTag("touchpad_surface"),
        shape = RoundedCornerShape(24.dp),
        color = ButtonSurface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SWIPE TO NAVIGATE\nTAP FOR OK",
                        textAlign = TextAlign.Center,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
