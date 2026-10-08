package com.example

import com.example.data.innertube.InnerTubeClient
import com.example.data.local.dao.LyricsDao
import com.example.data.local.entities.LyricsCacheEntity
import com.example.data.lyrics.LyricsService
import com.example.domain.model.LyricsData
import com.example.domain.model.LyricsLine
import com.example.domain.model.LyricsProvider
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTrackDurationFormatted() {
        val track = Track(
            id = "test_1",
            title = "Test Song",
            artist = "Test Artist",
            durationSeconds = 230
        )
        assertEquals("3:50", track.durationFormatted)
    }

    @Test
    fun testLrcParser() {
        val dummyDao = object : LyricsDao {
            override suspend fun getLyrics(trackId: String): LyricsCacheEntity? = null
            override suspend fun saveLyrics(lyrics: LyricsCacheEntity) {}
            override suspend fun clearLyricsCache() {}
            override suspend fun getLyricsCacheCount(): Int = 0
        }
        val lyricsService = LyricsService(dummyDao)

        val lrc = """
            [00:04.20]First line of song
            [00:10.50]Second line of song
            [01:02.00]Chorus line
        """.trimIndent()

        val parsed = lyricsService.parseLrc(lrc)
        assertEquals(3, parsed.size)
        assertEquals(4200L, parsed[0].timeMs)
        assertEquals("First line of song", parsed[0].text)
        assertEquals(10500L, parsed[1].timeMs)
        assertEquals(62000L, parsed[2].timeMs)
    }

    @Test
    fun testLyricsActiveLineIndex() {
        val lines = listOf(
            LyricsLine(1000L, "Intro"),
            LyricsLine(5000L, "Verse 1"),
            LyricsLine(12000L, "Chorus")
        )
        val data = LyricsData(
            trackId = "test",
            lines = lines,
            isSynced = true,
            provider = LyricsProvider.LRCLIB
        )

        assertEquals(-1, data.findActiveLineIndex(500L))
        assertEquals(0, data.findActiveLineIndex(1500L))
        assertEquals(1, data.findActiveLineIndex(6000L))
        assertEquals(2, data.findActiveLineIndex(15000L))
    }

    @Test
    fun testInnerTubeCatalog() = runBlocking {
        val client = InnerTubeClient()
        val quickPicks = client.getQuickPicks()
        assertTrue(quickPicks.isNotEmpty())

        val trending = client.getTrending()
        assertTrue(trending.size >= 5)

        val searchResult = client.search("Starboy")
        assertTrue(searchResult.isNotEmpty())
        assertEquals("Starboy", searchResult[0].title)
    }

    @Test
    fun testPresetLibraryTracks() {
        val libraryTracks = com.example.data.local.LocalMediaScanner.loadPresetLibraryTracks()

        assertTrue(libraryTracks.isNotEmpty())
        assertTrue(libraryTracks.size >= 10)
        libraryTracks.forEach { track ->
            assertTrue(track.title.isNotBlank())
            assertTrue(track.artist.isNotBlank())
            assertTrue("Artwork/picture must be present", track.artworkUrl.isNotBlank())
            assertTrue("Audio stream URL must be present", track.streamUrl.isNotBlank())
            assertTrue(track.durationSeconds > 0)
        }
    }

    @Test
    fun testTrackEntityMappingAndLocalStorageState() {
        val originalTrack = Track(
            id = "offline_01",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationSeconds = 200,
            artworkUrl = "https://images.unsplash.com/sample.jpg",
            streamUrl = "https://example.com/stream.mp3",
            localFilePath = "/data/user/0/com.aistudio.vmusix.nxqlrt/files/downloads/offline_01.m4a",
            storageState = com.example.domain.model.LocalStorageState.DOWNLOADED,
            fileSizeBytes = 8_500_000L,
            mimeType = "audio/mp4",
            isDownloaded = true,
            isCached = true,
            isLiked = true
        )

        val entity = com.example.data.local.entities.TrackEntity.fromDomain(originalTrack)
        assertEquals("offline_01", entity.id)
        assertEquals(originalTrack.localFilePath, entity.localFilePath)
        assertEquals(8_500_000L, entity.fileSizeBytes)
        assertEquals("DOWNLOADED", entity.storageState)
        assertTrue(entity.offlineAvailable)
        assertTrue(entity.isDownloaded)

        val mappedBack = entity.toDomain()
        assertEquals(originalTrack.id, mappedBack.id)
        assertEquals(originalTrack.title, mappedBack.title)
        assertEquals(originalTrack.localFilePath, mappedBack.localFilePath)
        assertEquals(com.example.domain.model.LocalStorageState.DOWNLOADED, mappedBack.storageState)
        assertEquals(8_500_000L, mappedBack.fileSizeBytes)
        assertTrue(mappedBack.offlineAvailable)
        assertTrue(mappedBack.isPlayableOffline)
    }

    @Test
    fun testTrackOfflinePlayableLogic() {
        val onlineTrack = Track(
            id = "online_only",
            title = "Online Stream",
            artist = "Stream Artist",
            localFilePath = null,
            isDownloaded = false,
            isCached = false
        )
        assertFalse(onlineTrack.isPlayableOffline)
        assertEquals(com.example.domain.model.LocalStorageState.ONLINE_ONLY, onlineTrack.storageState)

        val cachedTrack = Track(
            id = "cached_track",
            title = "Cached Stream",
            artist = "Cache Artist",
            localFilePath = "/cache/audio_cache/cached_track.m4a",
            isCached = true,
            isDownloaded = false
        )
        assertTrue(cachedTrack.isPlayableOffline)
        assertEquals(com.example.domain.model.LocalStorageState.CACHED_STREAM, cachedTrack.storageState)
    }
}
