package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.HistoryEntity
import com.example.data.local.entities.LyricsCacheEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistTrackCrossRef
import com.example.data.local.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC")
    fun getAllLibraryTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE localFilePath IS NOT NULL OR isDownloaded = 1 ORDER BY dateAdded DESC")
    fun getUploadedAndLocalTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY dateAdded DESC")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isCached = 1 ORDER BY dateAdded DESC")
    fun getCachedTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET isLiked = :isLiked WHERE id = :id")
    suspend fun setLiked(id: String, isLiked: Boolean)

    @Query("UPDATE tracks SET isDownloaded = :isDownloaded, localFilePath = :path WHERE id = :id")
    suspend fun setDownloaded(id: String, isDownloaded: Boolean, path: String?)

    @Query("UPDATE tracks SET isCached = :isCached WHERE id = :id")
    suspend fun setCached(id: String, isCached: Boolean)

    @Query("SELECT * FROM tracks WHERE offlineAvailable = 1 OR localFilePath IS NOT NULL ORDER BY dateAdded DESC")
    fun getAllOfflineTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE storageState = :storageState ORDER BY dateAdded DESC")
    fun getTracksByStorageState(storageState: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE localFilePath = :filePath LIMIT 1")
    suspend fun getTrackByFilePath(filePath: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE (offlineAvailable = 1 OR localFilePath IS NOT NULL) AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchOfflineTracks(query: String): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET storageState = :storageState, localFilePath = :filePath, fileSizeBytes = :fileSize, offlineAvailable = :isOffline, isCached = :isCached, isDownloaded = :isDownloaded, cachedAt = :cachedAt WHERE id = :id")
    suspend fun updateLocalStorageState(
        id: String,
        storageState: String,
        filePath: String?,
        fileSize: Long,
        isOffline: Boolean,
        isCached: Boolean,
        isDownloaded: Boolean,
        cachedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE tracks SET localFilePath = NULL, storageState = 'ONLINE_ONLY', fileSizeBytes = 0, offlineAvailable = 0, isCached = 0, isDownloaded = 0 WHERE id = :id")
    suspend fun clearOfflineStorage(id: String)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: String)

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE id = :id")
    suspend fun updatePlaybackStats(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM tracks WHERE offlineAvailable = 1")
    suspend fun getTotalOfflineStorageBytes(): Long

    @Query("DELETE FROM tracks WHERE isCached = 1 AND isDownloaded = 0 AND isLiked = 0 AND (localFilePath IS NULL OR localFilePath NOT LIKE 'content://%')")
    suspend fun clearCachedTracks()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.orderIndex ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY trackTitle ASC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId")
    suspend fun getDownload(trackId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("UPDATE downloads SET status = :status, progress = :progress, downloadedBytes = :downloadedBytes WHERE trackId = :trackId")
    suspend fun updateProgress(trackId: String, status: String, progress: Float, downloadedBytes: Long)
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE trackId = :trackId")
    suspend fun getLyrics(trackId: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(lyrics: LyricsCacheEntity)

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearLyricsCache()

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    suspend fun getLyricsCacheCount(): Int
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
