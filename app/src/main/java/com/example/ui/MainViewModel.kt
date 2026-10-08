package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VmusixApplication
import com.example.domain.model.Album
import com.example.domain.model.AppThemePreset
import com.example.domain.model.Artist
import com.example.domain.model.LyricsData
import com.example.domain.model.PlaybackState
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VmusixApplication
    val repository = app.repository
    val playerManager = app.playerManager
    val downloadManager = app.downloadManager
    val cacheManager = app.cacheManager
    val lastFmService = app.lastFmService
    val audioRecognitionManager = app.audioRecognitionManager

    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState

    // Home screen states
    private val _quickPicks = MutableStateFlow<List<Track>>(emptyList())
    val quickPicks: StateFlow<List<Track>> = _quickPicks.asStateFlow()

    private val _recommended = MutableStateFlow<List<Track>>(emptyList())
    val recommended: StateFlow<List<Track>> = _recommended.asStateFlow()

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    val trending: StateFlow<List<Track>> = _trending.asStateFlow()

    private val _newReleases = MutableStateFlow<List<Album>>(emptyList())
    val newReleases: StateFlow<List<Album>> = _newReleases.asStateFlow()

    private val _favoriteArtists = MutableStateFlow<List<Artist>>(emptyList())
    val favoriteArtists: StateFlow<List<Artist>> = _favoriteArtists.asStateFlow()

    private val _top50Tracks = MutableStateFlow<List<Track>>(emptyList())
    val top50Tracks: StateFlow<List<Track>> = _top50Tracks.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(true)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    // Search screen states
    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Library & Persisted flows
    val likedTracks = repository.likedTracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val downloadedTracks = repository.downloadedTracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val playlists = repository.playlists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentHistory = repository.recentHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allLibraryTracks = repository.allLibraryTracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val uploadedTracks = repository.uploadedAndLocalTracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allOfflineTracks = repository.allOfflineTracks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _isScanningLibrary = MutableStateFlow(false)
    val isScanningLibrary: StateFlow<Boolean> = _isScanningLibrary.asStateFlow()

    // Player & Lyrics
    private val _currentLyrics = MutableStateFlow<LyricsData?>(null)
    val currentLyrics: StateFlow<LyricsData?> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    // Theme preset
    private val _themePreset = MutableStateFlow(AppThemePreset.MIDNIGHT_PURPLE)
    val themePreset: StateFlow<AppThemePreset> = _themePreset.asStateFlow()

    init {
        loadHomeData()
        refreshLibraryMusic()
    }

    fun refreshLibraryMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningLibrary.value = true
            try {
                repository.loadAllLibraryMusic()
            } catch (e: Exception) {
                // Handled
            } finally {
                _isScanningLibrary.value = false
            }
        }
    }

    fun importAudioFiles(uris: List<android.net.Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningLibrary.value = true
            try {
                repository.importAudioUris(uris)
            } finally {
                _isScanningLibrary.value = false
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isHomeLoading.value = true
            try {
                _quickPicks.value = repository.getQuickPicks()
                _trending.value = repository.getTrending()
                _recommended.value = repository.getRecommended()
                _newReleases.value = repository.getNewReleases()
                _favoriteArtists.value = repository.getFavoriteArtists()
                _top50Tracks.value = repository.getTop50()
            } catch (e: Exception) {
                // Keep default catalog
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun search(query: String, filter: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                _searchResults.value = repository.search(query, filter)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue)
        loadLyricsForTrack(track)
    }

    private fun loadLyricsForTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLyricsLoading.value = true
            try {
                _currentLyrics.value = repository.getLyrics(track)
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    fun playPause() = playerManager.playPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(ms: Long) = playerManager.seekTo(ms)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun setSleepTimer(minutes: Int?) = playerManager.setSleepTimerMinutes(minutes)
    fun setVolume(vol: Float) = playerManager.setVolume(vol)

    fun toggleLike(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleLike(track)
        }
    }

    fun downloadTrack(track: Track) {
        downloadManager.downloadTrack(track)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(name)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun showFullPlayer() {
        _isFullPlayerVisible.value = true
    }

    fun hideFullPlayer() {
        _isFullPlayerVisible.value = false
    }

    fun setThemePreset(preset: AppThemePreset) {
        _themePreset.value = preset
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
