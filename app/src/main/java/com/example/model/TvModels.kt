package com.example.model

/**
 * Supported Smart TV communication protocols over Wi-Fi
 */
enum class TvProtocol(val displayName: String, val defaultPort: Int, val description: String) {
    ROKU_ECP(
        displayName = "Roku TV (ECP)",
        defaultPort = 8060,
        description = "Standard Roku External Control Protocol over HTTP POST"
    ),
    ANDROID_TV_TCP(
        displayName = "Android / Google TV (TCP)",
        defaultPort = 5555,
        description = "Direct TCP socket keycode transmission (e.g. ADB/TCP bridge)"
    ),
    UNIVERSAL_REST(
        displayName = "Universal REST API",
        defaultPort = 8080,
        description = "JSON HTTP POST /api/command endpoint for smart TV bridges"
    ),
    VIRTUAL_SIMULATOR(
        displayName = "Virtual TV Simulator",
        defaultPort = 0,
        description = "Simulated local TV for testing and demonstration"
    )
}

/**
 * Discovered or paired TV device
 */
data class TvDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val protocol: TvProtocol = TvProtocol.ROKU_ECP,
    val serviceType: String = "",
    val isVirtual: Boolean = false,
    val isSaved: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)

/**
 * Standard remote control commands with corresponding Android keycodes and Roku key strings
 */
enum class RemoteCommand(
    val label: String,
    val androidKeyCode: Int,
    val rokuKey: String,
    val jsonKey: String
) {
    // Power & Source
    POWER("Power", 26, "Power", "POWER"),
    INPUT("Source", 178, "InputTuner", "INPUT"),
    SETTINGS("Settings", 176, "Settings", "SETTINGS"),

    // Volume & Audio
    VOLUME_UP("Vol +", 24, "VolumeUp", "VOLUME_UP"),
    VOLUME_DOWN("Vol -", 25, "VolumeDown", "VOLUME_DOWN"),
    MUTE("Mute", 164, "VolumeMute", "MUTE"),

    // Navigation & D-Pad
    DPAD_UP("Up", 19, "Up", "UP"),
    DPAD_DOWN("Down", 20, "Down", "DOWN"),
    DPAD_LEFT("Left", 21, "Left", "LEFT"),
    DPAD_RIGHT("Right", 22, "Right", "RIGHT"),
    OK("OK", 23, "Select", "SELECT"),

    // System navigation
    BACK("Back", 4, "Back", "BACK"),
    HOME("Home", 3, "Home", "HOME"),
    MENU("Menu", 82, "Info", "MENU"),

    // Channels
    CHANNEL_UP("CH +", 166, "ChannelUp", "CHANNEL_UP"),
    CHANNEL_DOWN("CH -", 167, "ChannelDown", "CHANNEL_DOWN"),
    GUIDE("Guide", 172, "Guide", "GUIDE"),

    // Media playback
    PLAY_PAUSE("Play/Pause", 85, "Play", "PLAY_PAUSE"),
    REWIND("Rewind", 89, "Rev", "REWIND"),
    FAST_FORWARD("Fast Forward", 90, "Fwd", "FAST_FORWARD"),
    STOP("Stop", 86, "Stop", "STOP"),

    // Numeric Keys
    NUM_0("0", 7, "Lit_0", "0"),
    NUM_1("1", 8, "Lit_1", "1"),
    NUM_2("2", 9, "Lit_2", "2"),
    NUM_3("3", 10, "Lit_3", "3"),
    NUM_4("4", 11, "Lit_4", "4"),
    NUM_5("5", 12, "Lit_5", "5"),
    NUM_6("6", 13, "Lit_6", "6"),
    NUM_7("7", 14, "Lit_7", "7"),
    NUM_8("8", 18, "Lit_8", "8"),
    NUM_9("9", 19, "Lit_9", "9"),

    // Color Keys
    COLOR_RED("Red", 183, "Red", "RED"),
    COLOR_GREEN("Green", 184, "Green", "GREEN"),
    COLOR_YELLOW("Yellow", 185, "Yellow", "YELLOW"),
    COLOR_BLUE("Blue", 186, "Blue", "BLUE"),

    // Text & Keyboard Commands
    ENTER("Enter", 66, "Enter", "ENTER"),
    BACKSPACE("Backspace", 67, "Backspace", "BACKSPACE"),
    SPACE("Space", 62, "Lit_%20", "SPACE"),

    // Quick App Shortcuts
    APP_NETFLIX("Netflix", 0, "launch/12", "NETFLIX"),
    APP_YOUTUBE("YouTube", 0, "launch/837", "YOUTUBE"),
    APP_PRIME("Prime Video", 0, "launch/13", "PRIME"),
    APP_DISNEY("Disney+", 0, "launch/291097", "DISNEY"),
    APP_SPOTIFY("Spotify", 0, "launch/22271", "SPOTIFY"),
    APP_BROWSER("Browser", 0, "launch/browser", "BROWSER")
}

/**
 * Result of a sent command with latency and status
 */
data class CommandResult(
    val command: RemoteCommand,
    val success: Boolean,
    val message: String,
    val latencyMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Simulated state for demonstration and testing when physical TV is not on LAN
 */
data class VirtualTvState(
    val isPoweredOn: Boolean = true,
    val volume: Int = 30,
    val isMuted: Boolean = false,
    val currentChannel: Int = 5,
    val channelName: String = "Channel 05 - Cinema HD",
    val activeApp: String = "Live TV",
    val selectedMenuIndex: Int = 0,
    val lastCommandReceived: String = "READY",
    val typedText: String = ""
)
