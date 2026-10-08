package com.example.data.download

import android.content.Context
import android.util.Log
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.TrackDao
import com.example.data.local.entities.DownloadEntity
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val trackDao: TrackDao,
    private val scope: CoroutineScope
) {
    private val activeJobs = mutableMapOf<String, Job>()
    private val downloadDir: File by lazy {
        File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
    }

    val downloadsFlow: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.map { it.toDomain() }
    }

    fun downloadTrack(track: Track) {
        scope.launch(Dispatchers.IO) {
            val existing = downloadDao.getDownload(track.id)
            if (existing != null && existing.status == DownloadStatus.COMPLETED.name) {
                return@launch
            }

            val item = DownloadEntity(
                trackId = track.id,
                trackTitle = track.title,
                artist = track.artist,
                artworkUrl = track.artworkUrl,
                progress = 0f,
                status = DownloadStatus.DOWNLOADING.name,
                totalBytes = 8_500_000L, // Estimated ~8.5MB high-res audio
                downloadedBytes = 0L,
                localPath = File(downloadDir, "${track.id}.m4a").absolutePath
            )
            downloadDao.insertOrUpdate(item)
            startSimulatedDownload(track.id, item.localPath!!)
        }
    }

    fun downloadAlbum(albumTitle: String, tracks: List<Track>) {
        tracks.forEach { track ->
            downloadTrack(track)
        }
    }

    fun downloadPlaylist(playlistTitle: String, tracks: List<Track>) {
        tracks.forEach { track ->
            downloadTrack(track)
        }
    }

    fun pauseDownload(trackId: String) {
        activeJobs[trackId]?.cancel()
        activeJobs.remove(trackId)
        scope.launch(Dispatchers.IO) {
            val d = downloadDao.getDownload(trackId) ?: return@launch
            downloadDao.updateProgress(trackId, DownloadStatus.PAUSED.name, d.progress, d.downloadedBytes)
        }
    }

    fun resumeDownload(trackId: String) {
        scope.launch(Dispatchers.IO) {
            val d = downloadDao.getDownload(trackId) ?: return@launch
            downloadDao.updateProgress(trackId, DownloadStatus.DOWNLOADING.name, d.progress, d.downloadedBytes)
            startSimulatedDownload(trackId, d.localPath ?: File(downloadDir, "$trackId.m4a").absolutePath)
        }
    }

    fun cancelDownload(trackId: String) {
        activeJobs[trackId]?.cancel()
        activeJobs.remove(trackId)
        scope.launch(Dispatchers.IO) {
            val d = downloadDao.getDownload(trackId)
            if (d?.localPath != null) {
                File(d.localPath).delete()
            }
            downloadDao.deleteDownload(trackId)
            trackDao.clearOfflineStorage(trackId)
        }
    }

    private fun startSimulatedDownload(trackId: String, targetPath: String) {
        activeJobs[trackId]?.cancel()
        val job = scope.launch(Dispatchers.IO) {
            try {
                var currentProgress = downloadDao.getDownload(trackId)?.progress ?: 0f
                val totalBytes = 8_500_000L
                while (currentProgress < 1.0f) {
                    delay(300)
                    currentProgress = (currentProgress + 0.08f).coerceAtMost(1.0f)
                    val downloaded = (totalBytes * currentProgress).toLong()
                    downloadDao.updateProgress(trackId, DownloadStatus.DOWNLOADING.name, currentProgress, downloaded)
                }

                // Create dummy file for local storage confirmation
                val file = File(targetPath)
                file.parentFile?.mkdirs()
                if (!file.exists()) file.writeText("VMUSIX_AUDIO_BINARY_CACHE")

                downloadDao.updateProgress(trackId, DownloadStatus.COMPLETED.name, 1.0f, totalBytes)
                trackDao.updateLocalStorageState(
                    id = trackId,
                    storageState = com.example.domain.model.LocalStorageState.DOWNLOADED.name,
                    filePath = targetPath,
                    fileSize = totalBytes,
                    isOffline = true,
                    isCached = true,
                    isDownloaded = true,
                    cachedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("DownloadManager", "Error downloading $trackId", e)
            } finally {
                activeJobs.remove(trackId)
            }
        }
        activeJobs[trackId] = job
    }
}
