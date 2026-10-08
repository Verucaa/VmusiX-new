package com.example.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.PlaybackState
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus automatically
            )
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    var onTrackPlayed: ((Track) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = isPlaying,
                isLoading = false
            )
            if (isPlaying) {
                startPositionTracker()
            } else {
                stopPositionTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    _playbackState.value = _playbackState.value.copy(isLoading = true)
                }
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        isLoading = false,
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    )
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _playbackState.value = _playbackState.value.copy(isLoading = false)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIdx = exoPlayer.currentMediaItemIndex
            val queue = _playbackState.value.queue
            if (currentIdx in queue.indices) {
                val newTrack = queue[currentIdx]
                _playbackState.value = _playbackState.value.copy(
                    currentTrack = newTrack,
                    queueIndex = currentIdx
                )
                onTrackPlayed?.invoke(newTrack)
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val queueIndex = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = newQueue,
            queueIndex = queueIndex,
            isLoading = true
        )

        val mediaItems = newQueue.map { item ->
            val uri = if (item.offlineAvailable && !item.localFilePath.isNullOrEmpty()) {
                item.localFilePath
            } else {
                item.streamUrl
            }
            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(uri)
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, queueIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        onTrackPlayed?.invoke(track)
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0, 0L)
            }
            exoPlayer.play()
        }
    }

    fun next() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        if (state.isShuffle) {
            val nextIndex = (0 until state.queue.size).filter { it != state.queueIndex }.randomOrNull() ?: 0
            playTrack(state.queue[nextIndex], state.queue)
        } else if (state.queueIndex < state.queue.size - 1) {
            val nextTrack = state.queue[state.queueIndex + 1]
            playTrack(nextTrack, state.queue)
        } else if (state.repeatMode == RepeatMode.ALL) {
            playTrack(state.queue[0], state.queue)
        }
    }

    fun previous() {
        val state = _playbackState.value
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
            return
        }
        if (state.queueIndex > 0) {
            val prevTrack = state.queue[state.queueIndex - 1]
            playTrack(prevTrack, state.queue)
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        _playbackState.value = _playbackState.value.copy(isShuffle = newShuffle)
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = nextMode)
        exoPlayer.repeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.playbackParameters = PlaybackParameters(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        exoPlayer.volume = clamped
        _playbackState.value = _playbackState.value.copy(volume = clamped)
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = null)
            return
        }

        var remainingSeconds = minutes * 60L
        _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = remainingSeconds)

        sleepTimerJob = scope.launch(Dispatchers.Default) {
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = remainingSeconds)
            }
            // Timer expired: stop playback gracefully
            withContext(Dispatchers.Main) {
                exoPlayer.pause()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    sleepTimerRemainingSeconds = null
                )
            }
        }
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    next()
                } else {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                }
            }
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = currentPos,
                    durationMs = if (duration > 0) duration else _playbackState.value.durationMs
                )
                delay(250)
            }
        }
    }

    private fun stopPositionTracker() {
        positionJob?.cancel()
        positionJob = null
    }

    fun release() {
        stopPositionTracker()
        sleepTimerJob?.cancel()
        exoPlayer.release()
    }
}
