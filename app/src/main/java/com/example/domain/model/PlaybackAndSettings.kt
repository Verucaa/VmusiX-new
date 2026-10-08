package com.example.domain.model

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val sleepTimerRemainingSeconds: Long? = null
)

data class LastFmConfig(
    val isScrobblingEnabled: Boolean = false,
    val username: String = "",
    val sessionKey: String = "",
    val totalScrobbles: Long = 0
)

enum class AppThemePreset(
    val id: String,
    val displayName: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val surfaceHex: Long
) {
    MIDNIGHT_PURPLE("midnight_purple", "Midnight Purple & Electric Blue", 0xFF6D28D9, 0xFF3B82F6, 0xFF0B0B14),
    CYBER_NEON("cyber_neon", "Cyber Neon & Violet", 0xFF8B5CF6, 0xFF06B6D4, 0xFF09090F),
    DEEP_OLED("deep_oled", "Deep OLED Black", 0xFF7C3AED, 0xFF60A5FA, 0xFF000000),
    SUNSET_VIOLET("sunset_violet", "Sunset Velvet", 0xFF9333EA, 0xFFF43F5E, 0xFF0F0B18),
    OCEAN_BLUE("ocean_blue", "Oceanic Depths", 0xFF2563EB, 0xFF38BDF8, 0xFF080D1A)
}
