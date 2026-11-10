package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight, zero-overhead standalone ImageVectors replacing the heavy
 * material-icons-extended dependency to keep APK size strictly under 5MB.
 */
object AppIcons {

    private fun buildIcon(name: String, pathData: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(pathData).toNodes(),
                fill = SolidColor(Color.White)
            )
        }.build()
    }

    val Power: ImageVector by lazy {
        buildIcon("Power", "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42C17.99 7.86 19 9.81 19 12c0 3.87-3.13 7-7 7s-7-3.13-7-7c0-2.19 1.01-4.14 2.58-5.42L6.17 5.17C4.23 6.82 3 9.26 3 12c0 4.97 4.03 9 9 9s9-4.03 9-9c0-2.74-1.23-5.18-3.17-6.83z")
    }

    val Tv: ImageVector by lazy {
        buildIcon("Tv", "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z")
    }

    val Wifi: ImageVector by lazy {
        buildIcon("Wifi", "M12 4C7.31 4 3.07 5.9 0 8.98L12 21 24 8.98A16.88 16.88 0 0 0 12 4zm0 4c2.97 0 5.67 1.15 7.7 3.03L12 18.5 4.3 11.03C6.33 9.15 9.03 8 12 8z")
    }

    val Refresh: ImageVector by lazy {
        buildIcon("Refresh", "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z")
    }

    val GridView: ImageVector by lazy {
        buildIcon("GridView", "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z")
    }

    val TouchApp: ImageVector by lazy {
        buildIcon("TouchApp", "M9 11.24V7.5a2.5 2.5 0 0 1 5 0v3.74c1.21-.81 2-2.18 2-3.74C16 5.01 13.99 3 11.5 3S7 5.01 7 7.5c0 1.56.79 2.93 2 3.74zm9.84 4.63l-4.54-2.26c-.17-.07-.35-.11-.54-.11H13v-6c0-.83-.67-1.5-1.5-1.5S10 6.67 10 7.5v10.74l-3.44-.72c-.08-.02-.17-.03-.25-.03-.35 0-.67.14-.9.37l-.89.88 4.96 4.96c.27.27.64.42 1.02.42H17c.76 0 1.4-.55 1.52-1.3l.63-4.58c.02-.15 0-.3-.07-.43z")
    }

    val VolumeUp: ImageVector by lazy {
        buildIcon("VolumeUp", "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z")
    }

    val VolumeOff: ImageVector by lazy {
        buildIcon("VolumeOff", "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27l4.73 4.73H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z")
    }

    val VolumeMute: ImageVector by lazy {
        buildIcon("VolumeMute", "M7 9v6h4l5 5V4L11 9H7z")
    }

    val FastForward: ImageVector by lazy {
        buildIcon("FastForward", "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z")
    }

    val FastRewind: ImageVector by lazy {
        buildIcon("FastRewind", "M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z")
    }

    val Info: ImageVector by lazy {
        buildIcon("Info", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z")
    }

    val ArrowDropDown: ImageVector by lazy {
        buildIcon("ArrowDropDown", "M7 10l5 5 5-5z")
    }

    val NetworkCheck: ImageVector by lazy {
        buildIcon("NetworkCheck", "M15.9 5c-.17 0-.32.09-.41.23l-.07.15-2 3.46-.07.16c-.05.14-.05.3 0 .44l.07.16 2 3.46.07.15c.09.14.24.23.41.23h4.2c.17 0 .32-.09.41-.23l.07-.15 2-3.46.07-.16c.05-.14.05-.3 0-.44l-.07-.16-2-3.46-.07-.15a.49.49 0 0 0-.41-.23h-4.2zM2 13h2v2H2v-2zm0-4h2v2H2V9zm0-4h2v2H2V5zm4 12h2v2H6v-2zm0-4h2v2H6v-2zm0-4h2v2H6V9zm0-4h2v2H6V5z")
    }

    val Backspace: ImageVector by lazy {
        buildIcon("Backspace", "M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z")
    }

    val KeyboardReturn: ImageVector by lazy {
        buildIcon("KeyboardReturn", "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7h-2z")
    }

    val SpaceBar: ImageVector by lazy {
        buildIcon("SpaceBar", "M18 9v4H6V9H4v6h16V9z")
    }

    val ArrowLeft: ImageVector by lazy {
        buildIcon("ArrowLeft", "M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z")
    }

    val ArrowRight: ImageVector by lazy {
        buildIcon("ArrowRight", "M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z")
    }

    val Remove: ImageVector by lazy {
        buildIcon("Remove", "M19 13H5v-2h14v2z")
    }

    val Mic: ImageVector by lazy {
        buildIcon("Mic", "M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5-3c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z")
    }

    val Keyboard: ImageVector by lazy {
        buildIcon("Keyboard", "M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z")
    }

    val ErrorOutline: ImageVector by lazy {
        buildIcon("ErrorOutline", "M11 15h2v2h-2zm0-8h2v6h-2zm1-5C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z")
    }
}
