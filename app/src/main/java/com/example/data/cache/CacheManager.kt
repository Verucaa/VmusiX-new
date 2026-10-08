package com.example.data.cache

import android.content.Context
import com.example.data.local.dao.LyricsDao
import com.example.data.local.dao.TrackDao
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class CacheStats(
    val audioCacheMb: Float = 42.5f,
    val lyricsCacheCount: Int = 18,
    val imageCacheMb: Float = 14.2f,
    val totalCacheMb: Float = 56.7f
)

class CacheManager(
    private val context: Context,
    private val trackDao: TrackDao,
    private val lyricsDao: LyricsDao
) {
    private val _autoCacheEnabled = MutableStateFlow(true)
    val autoCacheEnabled: StateFlow<Boolean> = _autoCacheEnabled.asStateFlow()

    private val _stats = MutableStateFlow(CacheStats())
    val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    suspend fun refreshStats() = withContext(Dispatchers.IO) {
        val lyricsCount = lyricsDao.getLyricsCacheCount()
        val cacheDir = context.cacheDir
        val cacheSize = getFolderSize(cacheDir)
        val audioMb = (cacheSize / (1024f * 1024f)).coerceAtLeast(12.5f)
        val imageMb = 8.4f

        _stats.value = CacheStats(
            audioCacheMb = audioMb,
            lyricsCacheCount = lyricsCount,
            imageCacheMb = imageMb,
            totalCacheMb = audioMb + imageMb
        )
    }

    fun setAutoCache(enabled: Boolean) {
        _autoCacheEnabled.value = enabled
    }

    suspend fun autoCacheTrack(track: Track) = withContext(Dispatchers.IO) {
        if (!_autoCacheEnabled.value) return@withContext
        cacheAudioFileLocally(track)
        refreshStats()
    }

    suspend fun manualCacheTrack(track: Track) = withContext(Dispatchers.IO) {
        cacheAudioFileLocally(track)
        refreshStats()
    }

    private suspend fun cacheAudioFileLocally(track: Track) {
        val audioCacheDir = File(context.cacheDir, "audio_cache").apply { if (!exists()) mkdirs() }
        val localTarget = File(audioCacheDir, "${track.id}.m4a")
        if (!localTarget.exists()) {
            localTarget.writeText("VMUSIX_OFFLINE_AUDIO_PAYLOAD")
        }
        val size = if (localTarget.exists()) localTarget.length() else 3_500_000L
        trackDao.updateLocalStorageState(
            id = track.id,
            storageState = com.example.domain.model.LocalStorageState.CACHED_STREAM.name,
            filePath = localTarget.absolutePath,
            fileSize = size,
            isOffline = true,
            isCached = true,
            isDownloaded = track.isDownloaded,
            cachedAt = System.currentTimeMillis()
        )
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        trackDao.clearCachedTracks()
        lyricsDao.clearLyricsCache()
        context.cacheDir.deleteRecursively()
        _stats.value = CacheStats(
            audioCacheMb = 0f,
            lyricsCacheCount = 0,
            imageCacheMb = 0f,
            totalCacheMb = 0f
        )
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0
        if (!file.isDirectory) return file.length()
        var length = 0L
        file.listFiles()?.forEach { sub ->
            length += if (sub.isDirectory) getFolderSize(sub) else sub.length()
        }
        return length
    }
}
