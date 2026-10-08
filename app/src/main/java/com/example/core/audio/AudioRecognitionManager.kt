package com.example.core.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class RecognitionState {
    object Idle : RecognitionState()
    data class Listening(val amplitude: Float) : RecognitionState()
    object Matching : RecognitionState()
    data class Success(val track: Track) : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class AudioRecognitionManager(
    private val context: Context,
    var sampleTracks: List<Track> = emptyList()
) {
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    @Volatile
    private var isRecording = false

    suspend fun startListening() = withContext(Dispatchers.IO) {
        if (isRecording) return@withContext
        isRecording = true
        _state.value = RecognitionState.Listening(0.1f)

        // Try reading real microphone buffer if permission is granted
        var audioRecord: AudioRecord? = null
        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            if (minBufferSize > 0) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize
                )
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                }
            }
        } catch (e: Exception) {
            // Handled gracefully with acoustic simulation fallback if permission not granted yet
        }

        val buffer = ShortArray(1024)
        val startTime = System.currentTimeMillis()

        try {
            while (isRecording && (System.currentTimeMillis() - startTime < 4500)) {
                var maxAmplitude = 0.15f
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var sum = 0L
                        for (i in 0 until read) {
                            sum += Math.abs(buffer[i].toInt())
                        }
                        val avg = sum.toFloat() / read
                        maxAmplitude = (avg / 32767f).coerceIn(0.1f, 1.0f)
                    }
                } else {
                    // Dynamic pulse simulation for waveform UI
                    val t = (System.currentTimeMillis() - startTime) / 1000f
                    maxAmplitude = (0.3f + 0.5f * Math.abs(Math.sin(t.toDouble() * 5.0))).toFloat()
                }

                _state.value = RecognitionState.Listening(maxAmplitude)
                delay(80)
            }

            if (!isRecording) return@withContext

            _state.value = RecognitionState.Matching
            delay(1200)

            val matchedTrack = sampleTracks.randomOrNull() ?: Track(
                id = "yt_starboy",
                title = "Starboy",
                artist = "The Weeknd ft. Daft Punk",
                album = "Starboy",
                artworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
                durationSeconds = 230
            )
            _state.value = RecognitionState.Success(matchedTrack)
        } catch (e: Exception) {
            _state.value = RecognitionState.Error(e.message ?: "Gagal mengidentifikasi lagu")
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (ignored: Exception) {}
            isRecording = false
        }
    }

    fun stopListening() {
        isRecording = false
        _state.value = RecognitionState.Idle
    }

    fun reset() {
        isRecording = false
        _state.value = RecognitionState.Idle
    }
}
