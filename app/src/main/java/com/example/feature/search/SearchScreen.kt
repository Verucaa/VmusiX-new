package com.example.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.audio.AudioRecognitionManager
import com.example.core.audio.RecognitionState
import com.example.core.components.AudioWaveformVisualizer
import com.example.core.components.GlassCard
import com.example.core.components.TrackItemRow
import com.example.core.components.VmusixLoader
import com.example.domain.model.Track
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.VmusixCardBorder
import com.example.ui.theme.VmusixSurfaceVariant
import com.example.ui.theme.VmusixTextMuted
import com.example.ui.theme.VmusixTextPrimary
import com.example.ui.theme.VmusixTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onSearchQuery: (String, String) -> Unit,
    searchResults: List<Track>,
    isSearching: Boolean,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackSelect: (Track, List<Track>) -> Unit,
    onLikeToggle: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    audioRecognitionManager: AudioRecognitionManager,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var showShazamModal by remember { mutableStateOf(false) }
    val recognitionState by audioRecognitionManager.state.collectAsState()

    val categories = listOf("Semua", "Lagu", "Artis", "Album", "Playlist", "Podcast", "Video Musik")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Bar with Real-time Debounce & Shazam Recognition Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { newText ->
                    queryText = newText
                    debounceJob?.cancel()
                    debounceJob = scope.launch {
                        delay(300) // 300ms debounce
                        onSearchQuery(newText, selectedCategory)
                    }
                },
                placeholder = {
                    Text(
                        text = "Cari lagu, artis, album, podcast...",
                        color = VmusixTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (queryText.isNotEmpty()) {
                        IconButton(onClick = {
                            queryText = ""
                            onSearchQuery("", selectedCategory)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = VmusixTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = VmusixCardBorder,
                    focusedContainerColor = VmusixSurfaceVariant,
                    unfocusedContainerColor = VmusixSurfaceVariant,
                    focusedTextColor = VmusixTextPrimary,
                    unfocusedTextColor = VmusixTextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearchQuery(queryText, selectedCategory)
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Shazam Audio Recognition Button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MidnightPurple, ElectricBlue)
                        )
                    )
                    .clickable {
                        showShazamModal = true
                        scope.launch {
                            audioRecognitionManager.startListening()
                        }
                    }
                    .testTag("shazam_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Identifikasi Lagu Shazam",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = category
                        onSearchQuery(queryText, category)
                    },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = VmusixSurfaceVariant,
                        labelColor = VmusixTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) Color.Transparent else VmusixCardBorder,
                        enabled = true,
                        selected = isSelected
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Results / Loader / Empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isSearching) {
                VmusixLoader(size = 48.dp)
            } else if (searchResults.isEmpty()) {
                if (queryText.isBlank()) {
                    // Search discovery suggestions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Jelajahi Jutaan Musik & Podcast",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VmusixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan tombol Shazam di samping untuk mendengarkan lagu di sekitarmu",
                            style = MaterialTheme.typography.bodySmall,
                            color = VmusixTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Tidak ditemukan hasil untuk \"$queryText\"",
                        color = VmusixTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(searchResults) { track ->
                        TrackItemRow(
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onTrackSelect(track, searchResults) },
                            onLikeToggle = { onLikeToggle(track) },
                            onOptionsClick = { onAddToPlaylist(track) }
                        )
                    }
                }
            }
        }
    }

    // Shazam Audio Recognition Modal
    if (showShazamModal) {
        AlertDialog(
            onDismissRequest = {
                audioRecognitionManager.stopListening()
                showShazamModal = false
            },
            containerColor = VmusixSurfaceVariant,
            title = {
                Text(
                    text = "Identifikasi Lagu",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VmusixTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = recognitionState) {
                        is RecognitionState.Idle, is RecognitionState.Listening -> {
                            val amp = if (state is RecognitionState.Listening) state.amplitude else 0.2f
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AudioWaveformVisualizer(
                                barCount = 14,
                                activeAmplitude = amp,
                                isPlaying = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Mendengarkan musik di sekitarmu...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VmusixTextSecondary
                            )
                        }
                        is RecognitionState.Matching -> {
                            VmusixLoader(size = 48.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Mencocokkan sidik jari audio...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        is RecognitionState.Success -> {
                            val recognizedTrack = state.track
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    AsyncImage(
                                        model = recognizedTrack.artworkUrl,
                                        contentDescription = recognizedTrack.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = recognizedTrack.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VmusixTextPrimary
                                )
                                Text(
                                    text = recognizedTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VmusixTextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onTrackSelect(recognizedTrack, listOf(recognizedTrack))
                                            showShazamModal = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar")
                                    }
                                    Button(
                                        onClick = {
                                            onAddToPlaylist(recognizedTrack)
                                            showShazamModal = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Playlist")
                                    }
                                }
                            }
                        }
                        is RecognitionState.Error -> {
                            Text(
                                text = state.message,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        audioRecognitionManager.stopListening()
                        showShazamModal = false
                    }
                ) {
                    Text("Tutup", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
