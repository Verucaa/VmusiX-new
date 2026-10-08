package com.example.domain.model

enum class LyricsProvider(val displayName: String) {
    YOUTUBE_MUSIC("YouTube Music"),
    LRCLIB("LRCLIB"),
    BETTER_LYRICS("Better Lyrics"),
    KUGOU("KuGou"),
    PAXSENIX("Paxsenix"),
    OFFLINE_CACHE("Offline Cache")
}

data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null
)

data class LyricsData(
    val trackId: String,
    val lines: List<LyricsLine> = emptyList(),
    val isSynced: Boolean = true,
    val provider: LyricsProvider = LyricsProvider.LRCLIB,
    val plainLyrics: String = ""
) {
    fun findActiveLineIndex(currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= currentPositionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
