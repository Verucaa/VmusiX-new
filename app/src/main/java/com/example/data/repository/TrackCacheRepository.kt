package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.dao.TrackDao
import com.example.data.local.entities.TrackEntity
import com.example.domain.model.LocalStorageState
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

interface TrackCacheRepository {
    val allOfflineTracks: Flow<List<Track>>
    val cachedTracks: Flow<List<Track>>
    val downloadedTracks: Flow<List<Track>>

    suspend fun getTrackById(id: String): Track?
    suspend fun getTrackByFilePath(filePath: String): Track?
    suspend fun cacheTrackForOffline(track: Track, localFilePath: String, fileSizeBytes: Long): Track
    suspend fun markAsDownloaded(track: Track, localFilePath: String, fileSizeBytes: Long): Track
    suspend fun updateStorageState(
        trackId: String,
        state: LocalStorageState,
        localFilePath: String?,
        fileSizeBytes: Long
    )
    suspend fun removeOfflineCache(trackId: String): Boolean
    suspend fun getTrackForPlayback(trackId: String): Track?
    suspend fun isTrackOfflineReady(trackId: String): Boolean
    suspend fun recordPlaybackStats(trackId: String)
    suspend fun getTotalOfflineStorageBytes(): Long
    suspend fun clearExpiredCache()
    fun searchOfflineTracks(query: String): Flow<List<Track>>
}

class TrackCacheRepositoryImpl(
    private val trackDao: TrackDao,
    private val context: Context
) : TrackCacheRepository {
    private val tag = "TrackCacheRepository"

    override val allOfflineTracks: Flow<List<Track>> =
        trackDao.getAllOfflineTracks().map { list -> list.map { it.toDomain() } }

    override val cachedTracks: Flow<List<Track>> =
        trackDao.getCachedTracks().map { list -> list.map { it.toDomain() } }

    override val downloadedTracks: Flow<List<Track>> =
        trackDao.getDownloadedTracks().map { list -> list.map { it.toDomain() } }

    override suspend fun getTrackById(id: String): Track? = withContext(Dispatchers.IO) {
        trackDao.getTrackById(id)?.toDomain()
    }

    override suspend fun getTrackByFilePath(filePath: String): Track? = withContext(Dispatchers.IO) {
        trackDao.getTrackByFilePath(filePath)?.toDomain()
    }

    override suspend fun cacheTrackForOffline(
        track: Track,
        localFilePath: String,
        fileSizeBytes: Long
    ): Track = withContext(Dispatchers.IO) {
        val updated = track.copy(
            localFilePath = localFilePath,
            storageState = LocalStorageState.CACHED_STREAM,
            fileSizeBytes = fileSizeBytes,
            offlineAvailable = true,
            isCached = true,
            cachedAt = System.currentTimeMillis()
        )
        trackDao.insertOrUpdate(TrackEntity.fromDomain(updated))
        updated
    }

    override suspend fun markAsDownloaded(
        track: Track,
        localFilePath: String,
        fileSizeBytes: Long
    ): Track = withContext(Dispatchers.IO) {
        val updated = track.copy(
            localFilePath = localFilePath,
            storageState = LocalStorageState.DOWNLOADED,
            fileSizeBytes = fileSizeBytes,
            offlineAvailable = true,
            isDownloaded = true,
            cachedAt = System.currentTimeMillis()
        )
        trackDao.insertOrUpdate(TrackEntity.fromDomain(updated))
        updated
    }

    override suspend fun updateStorageState(
        trackId: String,
        state: LocalStorageState,
        localFilePath: String?,
        fileSizeBytes: Long
    ) = withContext(Dispatchers.IO) {
        val isOffline = state != LocalStorageState.ONLINE_ONLY && !localFilePath.isNullOrBlank()
        val isCached = state == LocalStorageState.CACHED_STREAM
        val isDownloaded = state == LocalStorageState.DOWNLOADED
        trackDao.updateLocalStorageState(
            id = trackId,
            storageState = state.name,
            filePath = localFilePath,
            fileSize = fileSizeBytes,
            isOffline = isOffline,
            isCached = isCached,
            isDownloaded = isDownloaded
        )
    }

    override suspend fun removeOfflineCache(trackId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = trackDao.getTrackById(trackId)
            if (entity?.localFilePath != null && !entity.localFilePath.startsWith("content://")) {
                val file = File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            trackDao.clearOfflineStorage(trackId)
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove offline cache for $trackId", e)
            false
        }
    }

    override suspend fun getTrackForPlayback(trackId: String): Track? = withContext(Dispatchers.IO) {
        val entity = trackDao.getTrackById(trackId) ?: return@withContext null
        val domain = entity.toDomain()

        // Verify local file availability if marked offline
        if (domain.offlineAvailable && !domain.localFilePath.isNullOrBlank()) {
            if (domain.localFilePath.startsWith("content://") || File(domain.localFilePath).exists()) {
                // Returns track ready to play directly from local storage
                return@withContext domain
            } else {
                // File was deleted from disk; revert offline status
                trackDao.updateLocalStorageState(
                    id = trackId,
                    storageState = LocalStorageState.ONLINE_ONLY.name,
                    filePath = null,
                    fileSize = 0L,
                    isOffline = false,
                    isCached = false,
                    isDownloaded = false
                )
                return@withContext domain.copy(
                    localFilePath = null,
                    offlineAvailable = false,
                    storageState = LocalStorageState.ONLINE_ONLY
                )
            }
        }

        domain
    }

    override suspend fun isTrackOfflineReady(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val entity = trackDao.getTrackById(trackId) ?: return@withContext false
        if (!entity.offlineAvailable || entity.localFilePath.isNullOrBlank()) return@withContext false
        if (entity.localFilePath.startsWith("content://")) return@withContext true
        File(entity.localFilePath).exists()
    }

    override suspend fun recordPlaybackStats(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.updatePlaybackStats(trackId)
    }

    override suspend fun getTotalOfflineStorageBytes(): Long = withContext(Dispatchers.IO) {
        trackDao.getTotalOfflineStorageBytes()
    }

    override suspend fun clearExpiredCache() = withContext(Dispatchers.IO) {
        trackDao.clearCachedTracks()
    }

    override fun searchOfflineTracks(query: String): Flow<List<Track>> {
        return trackDao.searchOfflineTracks(query).map { list -> list.map { it.toDomain() } }
    }
}
