package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    primaryContainer = ButtonSurface,
    onPrimaryContainer = AccentCyan,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = RemoteBody,
    onSurface = TextPrimary,
    surfaceVariant = RemoteCard,
    onSurfaceVariant = TextSecondary,
    outline = ButtonBorder
)

// The remote is intentionally optimized for a luxury sleek dark remote hardware aesthetic
private val LightColorScheme = DarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We intentionally deliver the sleek dark remote aesthetic for immersion
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
