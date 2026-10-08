package com.example.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.components.AudioWaveformVisualizer
import com.example.core.components.GlassCard
import com.example.core.components.TrackItemRow
import com.example.core.components.VmusixLoader
import com.example.domain.model.LyricsData
import com.example.domain.model.PlaybackState
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.VmusixCardBorder
import com.example.ui.theme.VmusixSurface
import com.example.ui.theme.VmusixSurfaceVariant
import com.example.ui.theme.VmusixTextMuted
import com.example.ui.theme.VmusixTextPrimary
import com.example.ui.theme.VmusixTextSecondary
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    val track = playbackState.currentTrack ?: return

    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player")
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            onClick = onClick
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VmusixSurface)
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.size(46.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = VmusixTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = VmusixTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            .testTag("mini_play_pause")
                    ) {
                        if (playbackState.isLoading) {
                            VmusixLoader(size = 20.dp, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Next button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = VmusixTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Smooth bottom progress bar line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playbackState: PlaybackState,
    lyricsData: LyricsData?,
    isLyricsLoading: Boolean,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onLikeToggle: () -> Unit,
    onDownload: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onSetVolume: (Float) -> Unit,
    onSelectQueueTrack: (Track) -> Unit
) {
    val track = playbackState.currentTrack ?: return
    var showLyricsView by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isUserDraggingSlider) {
        sliderDragValue.toLong()
    } else {
        playbackState.currentPositionMs
    }

    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val sliderProgress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VmusixSurface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -35) {
                        onNext()
                    } else if (dragAmount > 35) {
                        onPrevious()
                    }
                }
            }
            .testTag("full_player_screen")
    ) {
        // Ambient background blur of artwork
        AsyncImage(
            model = track.artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.28f
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0xDD090812),
                            Color(0xFF090812)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("full_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tutup",
                        tint = VmusixTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SEDANG MEMUTAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = track.album.ifEmpty { "Vmusix Soundscape" },
                        style = MaterialTheme.typography.bodySmall,
                        color = VmusixTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.testTag("full_player_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Antrean",
                        tint = VmusixTextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Content: Cover Artwork or Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (showLyricsView) {
                    SyncedLyricsPanel(
                        lyricsData = lyricsData,
                        isLoading = isLyricsLoading,
                        currentPositionMs = playbackState.currentPositionMs,
                        onSeekTo = onSeekTo
                    )
                } else {
                    // Large Album Artwork
                    Box(
                        modifier = Modifier
                            .size(310.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(VmusixSurfaceVariant)
                            .testTag("full_player_artwork")
                    ) {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title, Artist & Like/Lyrics Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = VmusixTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = VmusixTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Synced Lyrics Toggle Button
                    IconButton(
                        onClick = { showLyricsView = !showLyricsView },
                        modifier = Modifier.testTag("toggle_lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lyrics,
                            contentDescription = "Lirik",
                            tint = if (showLyricsView) MaterialTheme.colorScheme.secondary else VmusixTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Like Button
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier.testTag("full_player_like")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Suka",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else VmusixTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Download Button
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.testTag("full_player_download")
                    ) {
                        Icon(
                            imageVector = if (track.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = "Unduh",
                            tint = if (track.isDownloaded) MaterialTheme.colorScheme.secondary else VmusixTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Slider
            Slider(
                value = sliderProgress,
                onValueChange = { newFrac ->
                    isUserDraggingSlider = true
                    sliderDragValue = (newFrac * duration)
                },
                onValueChangeFinished = {
                    onSeekTo(sliderDragValue.toLong())
                    isUserDraggingSlider = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_slider")
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = VmusixTextMuted
                )
                Text(
                    text = formatMs(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = VmusixTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("btn_shuffle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak",
                        tint = if (playbackState.isShuffle) MaterialTheme.colorScheme.secondary else VmusixTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp).testTag("btn_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = VmusixTextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable(onClick = onPlayPause)
                        .testTag("btn_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackState.isLoading) {
                        VmusixLoader(size = 30.dp, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp).testTag("btn_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = VmusixTextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("btn_repeat")
                ) {
                    val icon = when (playbackState.repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val tint = when (playbackState.repeatMode) {
                        RepeatMode.OFF -> VmusixTextMuted
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Ulangi",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Extra Utility Bar: Speed & Sleep Timer & Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Button
                TextButton(
                    onClick = { showSpeedDialog = true },
                    modifier = Modifier.testTag("speed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = VmusixTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${playbackState.playbackSpeed}x",
                        color = VmusixTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Sleep Timer Button
                TextButton(
                    onClick = { showTimerDialog = true },
                    modifier = Modifier.testTag("sleep_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (playbackState.sleepTimerRemainingSeconds != null) MaterialTheme.colorScheme.secondary else VmusixTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (playbackState.sleepTimerRemainingSeconds != null) {
                            "${playbackState.sleepTimerRemainingSeconds / 60}m"
                        } else "Sleep Timer",
                        color = if (playbackState.sleepTimerRemainingSeconds != null) MaterialTheme.colorScheme.secondary else VmusixTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = VmusixSurface,
            modifier = Modifier.testTag("queue_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Antrean Lagu (${playbackState.queue.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VmusixTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(playbackState.queue) { index, item ->
                        val isCurrent = index == playbackState.queueIndex
                        TrackItemRow(
                            track = item,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            onClick = {
                                onSelectQueueTrack(item)
                                showQueueSheet = false
                            },
                            onLikeToggle = { }
                        )
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = VmusixSurfaceVariant,
            title = { Text("Kecepatan Putar", color = VmusixTextPrimary) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        TextButton(
                            onClick = {
                                onSetSpeed(speed)
                                showSpeedDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${speed}x" + if (speed == playbackState.playbackSpeed) " ✓" else "",
                                color = if (speed == playbackState.playbackSpeed) MaterialTheme.colorScheme.secondary else VmusixTextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Tutup", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            containerColor = VmusixSurfaceVariant,
            title = { Text("Pengatur Waktu Tidur", color = VmusixTextPrimary) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(
                            onClick = {
                                onSetSleepTimer(mins)
                                showTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$mins Menit", color = VmusixTextPrimary)
                        }
                    }
                    TextButton(
                        onClick = {
                            onSetSleepTimer(null)
                            showTimerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Matikan Pengatur Waktu", color = Color.Red.copy(alpha = 0.8f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun SyncedLyricsPanel(
    lyricsData: LyricsData?,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VmusixLoader(size = 48.dp)
        }
        return
    }

    if (lyricsData == null || lyricsData.lines.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Lirik tidak tersedia untuk lagu ini",
                color = VmusixTextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val activeIndex = lyricsData.findActiveLineIndex(currentPositionMs)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to active lyric line smoothly
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(
                    index = (activeIndex - 2).coerceAtLeast(0)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("synced_lyrics_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIRIK SINKRON",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Sumber: ${lyricsData.provider.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = VmusixTextMuted
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(lyricsData.lines) { index, line ->
                val isActive = index == activeIndex
                val isPast = index < activeIndex

                Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isActive) 21.sp else 16.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = if (isActive) 28.sp else 22.sp
                    ),
                    color = when {
                        isActive -> Color.White
                        isPast -> VmusixTextSecondary.copy(alpha = 0.8f)
                        else -> VmusixTextMuted.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeekTo(line.timeMs) }
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .testTag("lyric_line_$index")
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
