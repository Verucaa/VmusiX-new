package com.example.data.lastfm

import com.example.domain.model.LastFmConfig
import com.example.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LastFmService {
    private val _config = MutableStateFlow(
        LastFmConfig(
            isScrobblingEnabled = true,
            username = "VmusixAudience",
            sessionKey = "live_session_9281",
            totalScrobbles = 142
        )
    )
    val config: StateFlow<LastFmConfig> = _config.asStateFlow()

    private val _scrobbledTracks = MutableStateFlow<List<Track>>(emptyList())
    val scrobbledTracks: StateFlow<List<Track>> = _scrobbledTracks.asStateFlow()

    fun updateConfig(enabled: Boolean, username: String) {
        _config.value = _config.value.copy(
            isScrobblingEnabled = enabled,
            username = username
        )
    }

    fun scrobbleTrack(track: Track) {
        if (!_config.value.isScrobblingEnabled) return
        _config.value = _config.value.copy(
            totalScrobbles = _config.value.totalScrobbles + 1
        )
        val currentList = _scrobbledTracks.value.toMutableList()
        currentList.add(0, track)
        _scrobbledTracks.value = currentList.take(50)
    }
}
