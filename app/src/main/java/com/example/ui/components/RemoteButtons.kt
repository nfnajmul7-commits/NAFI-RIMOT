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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import com.example.ui.AppIcons
import androidx.compose.material3.Icon
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

// Color tokens matching the reference design
val CyanAccent = Color(0xFF00D2FF)
val DpadDarkBg = Color(0xFF141D2E)
val RockerBg = Color(0xFF131A29)
val UtilityCardBg = Color(0xFF172033)
val CardBorder = Color(0xFF243048)
val TextGray = Color(0xFF8F9FB8)

/**
 * Top 4 utility buttons: Menu, Settings, Hold to talk, Type
 */
@Composable
fun TopUtilityActionsRow(
    onCommand: (RemoteCommand) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenType: () -> Unit,
    onHoldToTalk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityActionButton(
            icon = Icons.Default.Menu,
            label = "Menu",
            onClick = { onCommand(RemoteCommand.MENU) },
            testTag = "utility_menu"
        )

        UtilityActionButton(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onOpenSettings,
            testTag = "utility_settings"
        )

        UtilityActionButton(
            icon = AppIcons.Mic,
            label = "Hold to talk",
            onClick = onHoldToTalk,
            testTag = "utility_mic"
        )

        UtilityActionButton(
            icon = AppIcons.Keyboard,
            label = "Type",
            onClick = onOpenType,
            testTag = "utility_type"
        )
    }
}

@Composable
private fun UtilityActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "util_scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(54.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            color = if (isPressed) Color(0xFF222F48) else UtilityCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPressed) CyanAccent else Color(0xFFD6E2F0),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Horizontal App Launcher Row: YouTube, Netflix, Prime, Disney+, Spotify, Edit
 */
@Composable
fun AppLauncherCarousel(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        // YouTube (Red)
        item {
            AppTile(
                title = "YouTube",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFFFF3333), Color(0xFFCC0000))),
                onClick = { onCommand(RemoteCommand.APP_YOUTUBE) },
                testTag = "app_tile_youtube"
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "YouTube",
                        tint = Color(0xFFE50914),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Netflix (Black/Red)
        item {
            AppTile(
                title = "Netflix",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF2B0A0D), Color(0xFF140204))),
                onClick = { onCommand(RemoteCommand.APP_NETFLIX) },
                testTag = "app_tile_netflix"
            ) {
                Text(
                    text = "N",
                    color = Color(0xFFE50914),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }
        }

        // Prime Video (Teal/Blue)
        item {
            AppTile(
                title = "Prime Video",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF00A8E1), Color(0xFF007399))),
                onClick = { onCommand(RemoteCommand.APP_PRIME) },
                testTag = "app_tile_prime"
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "prime video",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⌣",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
            }
        }

        // Disney+ (Deep Blue/Purple)
        item {
            AppTile(
                title = "Disney+",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF283C86), Color(0xFF1A1A40))),
                onClick = { onCommand(RemoteCommand.APP_DISNEY) },
                testTag = "app_tile_disney"
            ) {
                Text(
                    text = "Disney+",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Spotify (Emerald Green)
        item {
            AppTile(
                title = "Spotify",
                backgroundBrush = Brush.verticalGradient(listOf(Color(0xFF1ED760), Color(0xFF159643))),
                onClick = { onCommand(RemoteCommand.APP_SPOTIFY) },
                testTag = "app_tile_spotify"
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.height(2.5.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.height(2.5.dp))
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                }
            }
        }

        // Edit / Pencil icon
        item {
            Surface(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onCommand(RemoteCommand.MENU) },
                shape = RoundedCornerShape(18.dp),
                color = UtilityCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Apps",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    title: String,
    backgroundBrush: Brush,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "app_scale")

    Surface(
        modifier = Modifier
            .size(62.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Mode selector pills: [ D-Pad | Touchpad ]       [ Numbers ]
 */
@Composable
fun ModeSelectorPillsRow(
    isTouchpadActive: Boolean,
    isNumbersActive: Boolean,
    onSelectDpad: () -> Unit,
    onSelectTouchpad: () -> Unit,
    onSelectNumbers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Combined pill: D-Pad and Touchpad
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = UtilityCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-Pad Pill
                val isDpad = !isTouchpadActive && !isNumbersActive
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDpad) CyanAccent else Color.Transparent)
                        .clickable(onClick = onSelectDpad)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("mode_pill_dpad"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.GridView,
                            contentDescription = null,
                            tint = if (isDpad) Color.Black else TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "D-Pad",
                            color = if (isDpad) Color.Black else TextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Touchpad Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTouchpadActive) CyanAccent else Color.Transparent)
                        .clickable(onClick = onSelectTouchpad)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("mode_pill_touchpad"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.TouchApp,
                            contentDescription = null,
                            tint = if (isTouchpadActive) Color.Black else TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Touchpad",
                            color = if (isTouchpadActive) Color.Black else TextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Numbers Pill Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onSelectNumbers)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("mode_pill_numbers"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = AppIcons.GridView,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Numbers",
                    color = CyanAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * The Exact Center Layout:
 * [ VOL Rocker ]   [ Rounded Squircle D-Pad with OK ]   [ CH Rocker ]
 */
@Composable
fun ExactCentralControlCluster(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Vertical VOL Rocker
        ExactVerticalRocker(
            label = "VOL",
            onPlus = { onCommand(RemoteCommand.VOLUME_UP) },
            onMinus = { onCommand(RemoteCommand.VOLUME_DOWN) },
            hasProgressFill = true,
            modifier = Modifier.weight(0.24f),
            plusTag = "vol_plus",
            minusTag = "vol_minus"
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Large Center Rounded Squircle D-Pad
        ExactSquircleDpad(
            onCommand = onCommand,
            modifier = Modifier.weight(0.52f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Right Vertical CH Rocker
        ExactVerticalRocker(
            label = "CH",
            onPlus = { onCommand(RemoteCommand.CHANNEL_UP) },
            onMinus = { onCommand(RemoteCommand.CHANNEL_DOWN) },
            hasProgressFill = false,
            modifier = Modifier.weight(0.24f),
            plusTag = "ch_plus",
            minusTag = "ch_minus"
        )
    }
}

/**
 * Full-height rounded vertical rocker slider with + at top, label in middle, - at bottom
 */
@Composable
fun ExactVerticalRocker(
    label: String,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    hasProgressFill: Boolean,
    modifier: Modifier = Modifier,
    plusTag: String,
    minusTag: String
) {
    Surface(
        modifier = modifier
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = RockerBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Optional subtle dark cyan fill for volume rocker (as seen in screenshot)
            if (hasProgressFill) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(Color(0xFF0C3348))
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Plus button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable(onClick = onPlus)
                        .testTag(plusTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "$label +",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Middle Label
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Bottom Minus button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable(onClick = onMinus)
                        .testTag(minusTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Remove,
                        contentDescription = "$label -",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Center Large Squircle D-Pad with 4 chevron arrows and center Cyan circular OK button
 */
@Composable
fun ExactSquircleDpad(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(260.dp)
            .shadow(12.dp, RoundedCornerShape(44.dp))
            .clip(RoundedCornerShape(44.dp)),
        shape = RoundedCornerShape(44.dp),
        color = DpadDarkBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // UP Chevron
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.TopCenter)
                    .clickable { onCommand(RemoteCommand.DPAD_UP) }
                    .testTag("dpad_up"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.KeyboardArrowUp,
                    contentDescription = "Up",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // DOWN Chevron
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .clickable { onCommand(RemoteCommand.DPAD_DOWN) }
                    .testTag("dpad_down"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.KeyboardArrowDown,
                    contentDescription = "Down",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // LEFT Chevron
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onCommand(RemoteCommand.DPAD_LEFT) }
                    .testTag("dpad_left"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // RIGHT Chevron
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(100.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { onCommand(RemoteCommand.DPAD_RIGHT) }
                    .testTag("dpad_right"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Center Circular OK button (Bright Cyan)
            val okInteraction = remember { MutableInteractionSource() }
            val okPressed by okInteraction.collectIsPressedAsState()
            val okScale by animateFloatAsState(if (okPressed) 0.90f else 1f, label = "ok_scale")

            Surface(
                modifier = Modifier
                    .size(92.dp)
                    .scale(okScale)
                    .shadow(12.dp, CircleShape, spotColor = CyanAccent)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = okInteraction,
                        indication = null,
                        onClick = { onCommand(RemoteCommand.OK) }
                    )
                    .testTag("dpad_ok_button"),
                shape = CircleShape,
                color = CyanAccent
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Exact Home & Back Pill Row:
 * [ ⌂ Home ]          [ ← Back ]
 */
@Composable
fun ExactHomeBackRow(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Home Pill Button
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCommand(RemoteCommand.HOME) }
                .testTag("btn_home"),
            shape = RoundedCornerShape(16.dp),
            color = UtilityCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Home",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Back Pill Button
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCommand(RemoteCommand.BACK) }
                .testTag("btn_back"),
            shape = RoundedCornerShape(16.dp),
            color = UtilityCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Exact Bottom Media Bar:
 * [ Mute ]   [ << ]   [ ▶ (Cyan Circle) ]   [ >> ]   [ ⓘ ]
 */
@Composable
fun ExactBottomMediaBar(
    onCommand: (RemoteCommand) -> Unit,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        Icon(
            imageVector = if (isMuted) AppIcons.VolumeOff else AppIcons.VolumeMute,
            contentDescription = "Mute",
            tint = if (isMuted) Color(0xFFFF5252) else Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable { onCommand(RemoteCommand.MUTE) }
                .testTag("media_mute")
        )

        // Rewind
        Icon(
            imageVector = AppIcons.FastRewind,
            contentDescription = "Rewind",
            tint = Color.White,
            modifier = Modifier
                .size(26.dp)
                .clickable { onCommand(RemoteCommand.REWIND) }
                .testTag("media_rewind")
        )

        // Center Cyan Circular Play/Pause button
        val playInteraction = remember { MutableInteractionSource() }
        val isPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(if (isPressed) 0.90f else 1f, label = "play_scale")

        Surface(
            modifier = Modifier
                .size(52.dp)
                .scale(playScale)
                .clip(CircleShape)
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = { onCommand(RemoteCommand.PLAY_PAUSE) }
                )
                .testTag("media_play_pause"),
            shape = CircleShape,
            color = CyanAccent
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Fast Forward
        Icon(
            imageVector = AppIcons.FastForward,
            contentDescription = "Fast Forward",
            tint = Color.White,
            modifier = Modifier
                .size(26.dp)
                .clickable { onCommand(RemoteCommand.FAST_FORWARD) }
                .testTag("media_fast_forward")
        )

        // Info
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable { onCommand(RemoteCommand.MENU) }
                .testTag("media_info")
        )
    }
}

/**
 * Touchpad Gesture Navigation Surface
 */
@Composable
fun TouchpadSurface(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var accumulatedDy by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 60f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
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
                            if (accumulatedDx > 0) onCommand(RemoteCommand.DPAD_RIGHT)
                            else onCommand(RemoteCommand.DPAD_LEFT)
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                        } else if (Math.abs(accumulatedDy) > swipeThreshold) {
                            if (accumulatedDy > 0) onCommand(RemoteCommand.DPAD_DOWN)
                            else onCommand(RemoteCommand.DPAD_UP)
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                        }
                    }
                )
            }
            .clickable { onCommand(RemoteCommand.OK) }
            .testTag("touchpad_surface"),
        shape = RoundedCornerShape(28.dp),
        color = DpadDarkBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = AppIcons.KeyboardArrowUp,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SWIPE TO NAVIGATE\nTAP FOR OK",
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = AppIcons.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Icon(
                    imageVector = AppIcons.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Numeric Keypad
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumButton("1") { onCommand(RemoteCommand.NUM_1) }
            NumButton("2") { onCommand(RemoteCommand.NUM_2) }
            NumButton("3") { onCommand(RemoteCommand.NUM_3) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumButton("4") { onCommand(RemoteCommand.NUM_4) }
            NumButton("5") { onCommand(RemoteCommand.NUM_5) }
            NumButton("6") { onCommand(RemoteCommand.NUM_6) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumButton("7") { onCommand(RemoteCommand.NUM_7) }
            NumButton("8") { onCommand(RemoteCommand.NUM_8) }
            NumButton("9") { onCommand(RemoteCommand.NUM_9) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
    Surface(
        modifier = Modifier
            .size(if (isSecondary) 64.dp else 60.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("num_button_$label"),
        shape = CircleShape,
        color = UtilityCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSecondary) CyanAccent else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (label.length > 2) 13.sp else 20.sp
            )
        }
    }
}
