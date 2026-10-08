package com.example

import android.app.Application
import com.example.core.audio.AudioRecognitionManager
import com.example.core.player.MusicPlayerManager
import com.example.data.cache.CacheManager
import com.example.data.download.DownloadManager
import com.example.data.innertube.InnerTubeClient
import com.example.data.lastfm.LastFmService
import com.example.data.local.LocalMediaScanner
import com.example.data.local.VmusixDatabase
import com.example.data.lyrics.LyricsService
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VmusixApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: VmusixDatabase
        private set
    lateinit var innerTubeClient: InnerTubeClient
        private set
    lateinit var lyricsService: LyricsService
        private set
    lateinit var downloadManager: DownloadManager
        private set
    lateinit var cacheManager: CacheManager
        private set
    lateinit var lastFmService: LastFmService
        private set
    lateinit var audioRecognitionManager: AudioRecognitionManager
        private set
    lateinit var playerManager: MusicPlayerManager
        private set
    lateinit var trackCacheRepository: com.example.data.repository.TrackCacheRepository
        private set
    lateinit var repository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = VmusixDatabase.getInstance(this)
        innerTubeClient = InnerTubeClient()
        lyricsService = LyricsService(database.lyricsDao())
        downloadManager = DownloadManager(this, database.downloadDao(), database.trackDao(), applicationScope)
        cacheManager = CacheManager(this, database.trackDao(), database.lyricsDao())
        lastFmService = LastFmService()
        playerManager = MusicPlayerManager(this, applicationScope)
        trackCacheRepository = com.example.data.repository.TrackCacheRepositoryImpl(database.trackDao(), this)

        val localMediaScanner = LocalMediaScanner(this)
        audioRecognitionManager = AudioRecognitionManager(
            context = this,
            sampleTracks = LocalMediaScanner.loadPresetLibraryTracks()
        )

        repository = MusicRepository(
            database = database,
            innerTubeClient = innerTubeClient,
            lyricsService = lyricsService,
            downloadManager = downloadManager,
            cacheManager = cacheManager,
            lastFmService = lastFmService,
            audioRecognitionManager = audioRecognitionManager,
            localMediaScanner = localMediaScanner,
            trackCacheRepository = trackCacheRepository
        )

        playerManager.onTrackPlayed = { track ->
            applicationScope.launch {
                repository.recordHistory(track)
            }
        }

        // Asynchronous background preloading tasks
        applicationScope.launch {
            cacheManager.refreshStats()
            repository.loadAllLibraryMusic()
        }
    }
}
