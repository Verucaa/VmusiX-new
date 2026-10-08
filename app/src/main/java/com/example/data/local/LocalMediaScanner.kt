package com.example.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalMediaScanner(private val context: Context) {
    private val tag = "LocalMediaScanner"

    /**
     * Scans Android MediaStore for local audio files on device.
     */
    suspend fun scanDeviceMediaStore(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Track $id"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val albumId = it.getLong(albumIdCol)
                    val durationMs = it.getLong(durationCol)
                    val dataPath = it.getString(dataCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    tracks.add(
                        Track(
                            id = "local_ms_$id",
                            title = title,
                            artist = if (artist.equals("<unknown>", ignoreCase = true)) "Artis Lokal" else artist,
                            album = album,
                            durationSeconds = (durationMs / 1000).coerceAtLeast(1),
                            artworkUrl = albumArtUri,
                            streamUrl = contentUri,
                            localFilePath = dataPath,
                            isDownloaded = true,
                            isCached = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "MediaStore scan error: ${e.message}")
        }

        tracks
    }

    /**
     * Imports files selected by user using Storage Access Framework (SAF).
     * Extracts embedded ID3 artwork and song info using MediaMetadataRetriever.
     */
    suspend fun importAudioUris(uris: List<Uri>): List<Track> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<Track>()
        val coversDir = File(context.cacheDir, "imported_covers").apply { if (!exists()) mkdirs() }

        uris.forEachIndexed { index, uri ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: uri.lastPathSegment ?: "Imported Track $index"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Artis Lokal"
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?: "Koleksi Diunggah"
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationSec = (durationStr?.toLongOrNull() ?: 180000L) / 1000L

                // Extract embedded cover artwork if present
                var artworkPath = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                val artBytes = retriever.embeddedPicture
                if (artBytes != null && artBytes.isNotEmpty()) {
                    val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}_$index.jpg")
                    FileOutputStream(coverFile).use { it.write(artBytes) }
                    artworkPath = coverFile.absolutePath
                }

                retriever.release()

                imported.add(
                    Track(
                        id = "imported_${System.currentTimeMillis()}_$index",
                        title = title,
                        artist = artist,
                        album = album,
                        durationSeconds = durationSec,
                        artworkUrl = artworkPath,
                        streamUrl = uri.toString(),
                        localFilePath = uri.toString(),
                        isDownloaded = true,
                        isCached = true
                    )
                )
            } catch (e: Exception) {
                Log.w(tag, "Failed to parse uri $uri: ${e.message}")
            }
        }

        imported
    }

    fun loadPresetLibraryTracks(): List<Track> = Companion.loadPresetLibraryTracks()

    companion object {
        fun loadPresetLibraryTracks(): List<Track> = listOf(
        Track(
            id = "lib_starboy",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            artistId = "art_theweeknd",
            album = "Starboy",
            albumId = "alb_starboy",
            durationSeconds = 230,
            artworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isCached = true
        ),
        Track(
            id = "lib_blinding_lights",
            title = "Blinding Lights",
            artist = "The Weeknd",
            artistId = "art_theweeknd",
            album = "After Hours",
            albumId = "alb_afterhours",
            durationSeconds = 200,
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            isCached = true
        ),
        Track(
            id = "lib_levitating",
            title = "Levitating",
            artist = "Dua Lipa",
            artistId = "art_dualipa",
            album = "Future Nostalgia",
            albumId = "alb_futurenostalgia",
            durationSeconds = 203,
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3",
            isCached = true
        ),
        Track(
            id = "lib_midnight_city",
            title = "Midnight City",
            artist = "M83",
            artistId = "art_m83",
            album = "Hurry Up, We're Dreaming",
            albumId = "alb_m83",
            durationSeconds = 244,
            artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
            isCached = true
        ),
        Track(
            id = "lib_stay",
            title = "Stay",
            artist = "The Kid LAROI, Justin Bieber",
            artistId = "art_justinbieber",
            album = "F*CK LOVE 3: OVER YOU",
            albumId = "alb_stay",
            durationSeconds = 141,
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isCached = true
        ),
        Track(
            id = "lib_as_it_was",
            title = "As It Was",
            artist = "Harry Styles",
            artistId = "art_harrystyles",
            album = "Harry's House",
            albumId = "alb_harryshouse",
            durationSeconds = 167,
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            isCached = true
        ),
        Track(
            id = "lib_heat_waves",
            title = "Heat Waves",
            artist = "Glass Animals",
            artistId = "art_glassanimals",
            album = "Dreamland",
            albumId = "alb_dreamland",
            durationSeconds = 238,
            artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3",
            isCached = true
        ),
        Track(
            id = "lib_flowers",
            title = "Flowers",
            artist = "Miley Cyrus",
            artistId = "art_mileycyrus",
            album = "Endless Summer Vacation",
            albumId = "alb_flowers",
            durationSeconds = 200,
            artworkUrl = "https://images.unsplash.com/photo-1445985543470-41fdd6ce388d?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
            isCached = true
        ),
        Track(
            id = "lib_save_your_tears",
            title = "Save Your Tears",
            artist = "The Weeknd & Ariana Grande",
            artistId = "art_theweeknd",
            album = "After Hours (Deluxe)",
            albumId = "alb_afterhours",
            durationSeconds = 215,
            artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isCached = true
        ),
        Track(
            id = "lib_yellow",
            title = "Yellow",
            artist = "Coldplay",
            artistId = "art_coldplay",
            album = "Parachutes",
            albumId = "alb_parachutes",
            durationSeconds = 269,
            artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            isCached = true
        ),
        Track(
            id = "lib_electric_feel",
            title = "Electric Feel",
            artist = "MGMT",
            artistId = "art_mgmt",
            album = "Oracular Spectacular",
            albumId = "alb_mgmt",
            durationSeconds = 229,
            artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3",
            isCached = true
        ),
        Track(
            id = "lib_nightcall",
            title = "Nightcall",
            artist = "Kavinsky",
            artistId = "art_kavinsky",
            album = "OutRun",
            albumId = "alb_outrun",
            durationSeconds = 259,
            artworkUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
            isCached = true
        ),
        Track(
            id = "lib_get_lucky",
            title = "Get Lucky",
            artist = "Daft Punk ft. Pharrell Williams",
            artistId = "art_daftpunk",
            album = "Random Access Memories",
            albumId = "alb_ram",
            durationSeconds = 248,
            artworkUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isCached = true
        ),
        Track(
            id = "lib_instant_crush",
            title = "Instant Crush",
            artist = "Daft Punk ft. Julian Casablancas",
            artistId = "art_daftpunk",
            album = "Random Access Memories",
            albumId = "alb_ram",
            durationSeconds = 337,
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            isCached = true
        ),
        Track(
            id = "lib_clocks",
            title = "Clocks",
            artist = "Coldplay",
            artistId = "art_coldplay",
            album = "A Rush of Blood to the Head",
            albumId = "alb_coldplay_clocks",
            durationSeconds = 307,
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3",
            isCached = true
        ),
        Track(
            id = "lib_something_about_us",
            title = "Something About Us",
            artist = "Daft Punk",
            artistId = "art_daftpunk",
            album = "Discovery",
            albumId = "alb_discovery",
            durationSeconds = 231,
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
            isCached = true
        )
    )
    }
}
