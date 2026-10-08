package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.components.GlassCard
import com.example.core.components.TrackItemRow
import com.example.core.components.VmusixLoader
import com.example.core.util.AppBrandLogo
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Track
import com.example.ui.theme.VmusixSurfaceVariant
import com.example.ui.theme.VmusixTextMuted
import com.example.ui.theme.VmusixTextPrimary
import com.example.ui.theme.VmusixTextSecondary

@Composable
fun HomeScreen(
    isLoading: Boolean,
    quickPicks: List<Track>,
    recentlyPlayed: List<Track>,
    recommended: List<Track>,
    trending: List<Track>,
    newReleases: List<Album>,
    favoriteArtists: List<Artist>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackSelect: (Track, List<Track>) -> Unit,
    onLikeToggle: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && quickPicks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VmusixLoader(size = 52.dp)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Branding Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBrandLogo(size = 38.dp, shapeRadius = 10.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Vmusix",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = VmusixTextPrimary
                    )
                    Text(
                        text = "YouTube Music Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Continue Listening Hero Card (if recently played exists)
        if (recentlyPlayed.isNotEmpty()) {
            val lastTrack = recentlyPlayed.first()
            item {
                SectionTitle(title = "Lanjutkan Mendengarkan", subtitle = "Continue Listening")
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("hero_continue_listening"),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { onTrackSelect(lastTrack, recentlyPlayed) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VmusixSurfaceVariant)
                        ) {
                            AsyncImage(
                                model = lastTrack.artworkUrl,
                                contentDescription = lastTrack.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lastTrack.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VmusixTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastTrack.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = VmusixTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Putar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Lanjutkan",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Picks Section (2-row grid or list)
        if (quickPicks.isNotEmpty()) {
            item {
                SectionTitle(title = "Pilihan Cepat", subtitle = "Quick Picks")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    quickPicks.take(4).forEach { track ->
                        TrackItemRow(
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onTrackSelect(track, quickPicks) },
                            onLikeToggle = { onLikeToggle(track) }
                        )
                    }
                }
            }
        }

        // Recommended For You (Horizontal Cards)
        if (recommended.isNotEmpty()) {
            item {
                SectionTitle(title = "Direkomendasikan Untukmu", subtitle = "Recommended For You")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recommended) { track ->
                        MusicCard(
                            title = track.title,
                            subtitle = track.artist,
                            imageUrl = track.artworkUrl,
                            onClick = { onTrackSelect(track, recommended) }
                        )
                    }
                }
            }
        }

        // Trending Section
        if (trending.isNotEmpty()) {
            item {
                SectionTitle(title = "Sedang Tren Hari Ini", subtitle = "Trending Charts")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    trending.take(5).forEach { track ->
                        TrackItemRow(
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onTrackSelect(track, trending) },
                            onLikeToggle = { onLikeToggle(track) }
                        )
                    }
                }
            }
        }

        // New Releases
        if (newReleases.isNotEmpty()) {
            item {
                SectionTitle(title = "Rilis Terbaru", subtitle = "New Releases")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newReleases) { album ->
                        MusicCard(
                            title = album.title,
                            subtitle = "${album.artist} • ${album.year}",
                            imageUrl = album.coverUrl,
                            onClick = { }
                        )
                    }
                }
            }
        }

        // Favorite Artists
        if (favoriteArtists.isNotEmpty()) {
            item {
                SectionTitle(title = "Artis Favorit", subtitle = "Favorite Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteArtists) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(90.dp)
                                .clickable { }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(VmusixSurfaceVariant)
                            ) {
                                AsyncImage(
                                    model = artist.imageUrl,
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = VmusixTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = VmusixTextPrimary
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = VmusixTextMuted
            )
        }
    }
}

@Composable
fun MusicCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(VmusixSurfaceVariant)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = VmusixTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = VmusixTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
