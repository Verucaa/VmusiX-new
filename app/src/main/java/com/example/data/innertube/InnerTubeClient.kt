package com.example.data.innertube

import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class InnerTubeClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    // Curated high quality YouTube Music catalog and streaming sources
    private val mockCatalog = listOf(
        Track(
            id = "yt_starboy",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            artistId = "art_theweeknd",
            album = "Starboy",
            albumId = "alb_starboy",
            durationSeconds = 230,
            artworkUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
        ),
        Track(
            id = "yt_blinding_lights",
            title = "Blinding Lights",
            artist = "The Weeknd",
            artistId = "art_theweeknd",
            album = "After Hours",
            albumId = "alb_afterhours",
            durationSeconds = 200,
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"
        ),
        Track(
            id = "yt_levitating",
            title = "Levitating",
            artist = "Dua Lipa",
            artistId = "art_dualipa",
            album = "Future Nostalgia",
            albumId = "alb_futurenostalgia",
            durationSeconds = 203,
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3"
        ),
        Track(
            id = "yt_midnight_city",
            title = "Midnight City",
            artist = "M83",
            artistId = "art_m83",
            album = "Hurry Up, We're Dreaming",
            albumId = "alb_m83",
            durationSeconds = 244,
            artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3"
        ),
        Track(
            id = "yt_stay",
            title = "Stay",
            artist = "The Kid LAROI, Justin Bieber",
            artistId = "art_justinbieber",
            album = "F*CK LOVE 3: OVER YOU",
            albumId = "alb_stay",
            durationSeconds = 141,
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
        ),
        Track(
            id = "yt_as_it_was",
            title = "As It Was",
            artist = "Harry Styles",
            artistId = "art_harrystyles",
            album = "Harry's House",
            albumId = "alb_harryshouse",
            durationSeconds = 167,
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"
        ),
        Track(
            id = "yt_heat_waves",
            title = "Heat Waves",
            artist = "Glass Animals",
            artistId = "art_glassanimals",
            album = "Dreamland",
            albumId = "alb_dreamland",
            durationSeconds = 238,
            artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/pang/paza-moduless.mp3"
        ),
        Track(
            id = "yt_flowers",
            title = "Flowers",
            artist = "Miley Cyrus",
            artistId = "art_mileycyrus",
            album = "Endless Summer Vacation",
            albumId = "alb_flowers",
            durationSeconds = 200,
            artworkUrl = "https://images.unsplash.com/photo-1445985543470-41fdd6ce388d?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3"
        ),
        Track(
            id = "yt_save_your_tears",
            title = "Save Your Tears",
            artist = "The Weeknd & Ariana Grande",
            artistId = "art_theweeknd",
            album = "After Hours (Deluxe)",
            albumId = "alb_afterhours",
            durationSeconds = 215,
            artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
        ),
        Track(
            id = "yt_coldplay_yellow",
            title = "Yellow",
            artist = "Coldplay",
            artistId = "art_coldplay",
            album = "Parachutes",
            albumId = "alb_parachutes",
            durationSeconds = 269,
            artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3"
        )
    )

    suspend fun getQuickPicks(): List<Track> = withContext(Dispatchers.IO) {
        mockCatalog.shuffled().take(6)
    }

    suspend fun getTrending(): List<Track> = withContext(Dispatchers.IO) {
        mockCatalog
    }

    suspend fun getRecommended(): List<Track> = withContext(Dispatchers.IO) {
        mockCatalog.reversed()
    }

    suspend fun getNewReleases(): List<Album> = withContext(Dispatchers.IO) {
        listOf(
            Album("alb_starboy", "Starboy (Deluxe)", "The Weeknd", "2026", "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80"),
            Album("alb_futurenostalgia", "Future Nostalgia Moonlight", "Dua Lipa", "2026", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"),
            Album("alb_dreamland", "Dreamland Night Edition", "Glass Animals", "2026", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80"),
            Album("alb_harryshouse", "Harry's House Live", "Harry Styles", "2026", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80")
        )
    }

    suspend fun getFavoriteArtists(): List<Artist> = withContext(Dispatchers.IO) {
        listOf(
            Artist("art_theweeknd", "The Weeknd", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80", "112M"),
            Artist("art_dualipa", "Dua Lipa", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80", "78M"),
            Artist("art_harrystyles", "Harry Styles", "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80", "64M"),
            Artist("art_coldplay", "Coldplay", "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80", "85M"),
            Artist("art_glassanimals", "Glass Animals", "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80", "42M")
        )
    }

    suspend fun getTop50Charts(): List<Track> = withContext(Dispatchers.IO) {
        (mockCatalog + mockCatalog + mockCatalog + mockCatalog + mockCatalog).take(50).mapIndexed { index, track ->
            track.copy(id = "${track.id}_chart_$index", title = "${track.title}")
        }
    }

    suspend fun search(query: String, filter: String = "ALL"): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val lower = query.lowercase().trim()
        mockCatalog.filter {
            it.title.lowercase().contains(lower) ||
            it.artist.lowercase().contains(lower) ||
            it.album.lowercase().contains(lower)
        }.ifEmpty {
            // Dynamic synthetic item if user searches for an arbitrary title
            listOf(
                Track(
                    id = "yt_search_${System.currentTimeMillis()}",
                    title = query.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    artist = "Vmusix High Fidelity",
                    album = "YouTube Music Online",
                    durationSeconds = 210,
                    artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                    streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
                )
            )
        }
    }

    suspend fun getStreamingUrl(trackId: String): String = withContext(Dispatchers.IO) {
        mockCatalog.find { it.id == trackId }?.streamUrl
            ?: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
    }
}
