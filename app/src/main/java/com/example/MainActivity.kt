package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.HomeScreen
import com.example.feature.library.LibraryScreen
import com.example.feature.player.FullPlayerSheet
import com.example.feature.player.MiniPlayer
import com.example.feature.search.SearchScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.VmusixSurface
import com.example.ui.theme.VmusixTheme

enum class BottomNavDestination(
    val title: String,
    val iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    val iconUnselected: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Cari", Icons.Filled.Search, Icons.Outlined.Search),
    LIBRARY("Lainnya", Icons.Filled.Widgets, Icons.Outlined.Widgets)
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreset by viewModel.themePreset.collectAsState()

            VmusixTheme(preset = themePreset) {
                // Audio & Media permission launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Handled gracefully */ }

                LaunchedEffect(Unit) {
                    val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionsLauncher.launch(perms.toTypedArray())
                }

                VmusixMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VmusixMainApp(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(BottomNavDestination.HOME) }
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val themePreset by viewModel.themePreset.collectAsState()

    // Handle back button when full player is open
    BackHandler(enabled = isFullPlayerVisible) {
        viewModel.hideFullPlayer()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = VmusixSurface,
            bottomBar = {
                VmusixBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    playbackState = playbackState,
                    onPlayPause = { viewModel.playPause() },
                    onNext = { viewModel.next() },
                    onMiniPlayerClick = { viewModel.showFullPlayer() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    BottomNavDestination.HOME -> {
                        val isHomeLoading by viewModel.isHomeLoading.collectAsState()
                        val quickPicks by viewModel.quickPicks.collectAsState()
                        val recentlyPlayed by viewModel.recentHistory.collectAsState()
                        val recommended by viewModel.recommended.collectAsState()
                        val trending by viewModel.trending.collectAsState()
                        val newReleases by viewModel.newReleases.collectAsState()
                        val favoriteArtists by viewModel.favoriteArtists.collectAsState()

                        HomeScreen(
                            isLoading = isHomeLoading,
                            quickPicks = quickPicks,
                            recentlyPlayed = recentlyPlayed,
                            recommended = recommended,
                            trending = trending,
                            newReleases = newReleases,
                            favoriteArtists = favoriteArtists,
                            currentTrackId = playbackState.currentTrack?.id,
                            isPlaying = playbackState.isPlaying,
                            onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                            onLikeToggle = { track -> viewModel.toggleLike(track) }
                        )
                    }

                    BottomNavDestination.SEARCH -> {
                        val searchResults by viewModel.searchResults.collectAsState()
                        val isSearching by viewModel.isSearching.collectAsState()

                        SearchScreen(
                            onSearchQuery = { q, cat -> viewModel.search(q, cat) },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            currentTrackId = playbackState.currentTrack?.id,
                            isPlaying = playbackState.isPlaying,
                            onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                            onLikeToggle = { track -> viewModel.toggleLike(track) },
                            onAddToPlaylist = { track ->
                                val defaultPl = viewModel.playlists.value.firstOrNull()
                                if (defaultPl != null) {
                                    viewModel.addTrackToPlaylist(defaultPl.id, track)
                                }
                            },
                            audioRecognitionManager = viewModel.audioRecognitionManager
                        )
                    }

                    BottomNavDestination.LIBRARY -> {
                        val likedTracks by viewModel.likedTracks.collectAsState()
                        val downloadedTracks by viewModel.downloadedTracks.collectAsState()
                        val top50Tracks by viewModel.top50Tracks.collectAsState()
                        val playlists by viewModel.playlists.collectAsState()
                        val allLibraryTracks by viewModel.allLibraryTracks.collectAsState()
                        val uploadedTracks by viewModel.uploadedTracks.collectAsState()
                        val isScanningLibrary by viewModel.isScanningLibrary.collectAsState()

                        LibraryScreen(
                            likedTracks = likedTracks,
                            downloadedTracks = downloadedTracks,
                            top50Tracks = top50Tracks,
                            allLibraryTracks = allLibraryTracks,
                            uploadedTracks = uploadedTracks,
                            isScanningLibrary = isScanningLibrary,
                            playlists = playlists,
                            cacheManager = viewModel.cacheManager,
                            downloadManager = viewModel.downloadManager,
                            lastFmService = viewModel.lastFmService,
                            currentThemePreset = themePreset,
                            onThemeSelected = { viewModel.setThemePreset(it) },
                            onRefreshLibrary = { viewModel.refreshLibraryMusic() },
                            onImportUris = { uris -> viewModel.importAudioFiles(uris) },
                            currentTrackId = playbackState.currentTrack?.id,
                            isPlaying = playbackState.isPlaying,
                            onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                            onLikeToggle = { track -> viewModel.toggleLike(track) },
                            onCreatePlaylist = { name -> viewModel.createPlaylist(name) }
                        )
                    }
                }
            }
        }

        // Full Player Modal Screen
        AnimatedVisibility(
            visible = isFullPlayerVisible && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val currentTrack = playbackState.currentTrack
            if (currentTrack != null) {
                FullPlayerSheet(
                    playbackState = playbackState,
                    lyricsData = currentLyrics,
                    isLyricsLoading = isLyricsLoading,
                    isLiked = currentTrack.isLiked,
                    onDismiss = { viewModel.hideFullPlayer() },
                    onPlayPause = { viewModel.playPause() },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onLikeToggle = { viewModel.toggleLike(currentTrack) },
                    onDownload = { viewModel.downloadTrack(currentTrack) },
                    onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                    onSetSleepTimer = { viewModel.setSleepTimer(it) },
                    onSetVolume = { viewModel.setVolume(it) },
                    onSelectQueueTrack = { viewModel.playTrack(it, playbackState.queue) }
                )
            }
        }
    }
}

@Composable
fun VmusixBottomBar(
    selectedTab: BottomNavDestination,
    onTabSelected: (BottomNavDestination) -> Unit,
    playbackState: com.example.domain.model.PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onMiniPlayerClick: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VmusixSurface)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Docked Mini Player
        if (playbackState.currentTrack != null) {
            MiniPlayer(
                playbackState = playbackState,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClick = onMiniPlayerClick
            )
        }

        // 3-Menu Bottom Navigation
        NavigationBar(
            containerColor = VmusixSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.testTag("bottom_navigation_bar")
        ) {
            BottomNavDestination.values().forEach { destination ->
                val isSelected = destination == selectedTab
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.iconSelected else destination.iconUnselected,
                            contentDescription = destination.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8)
                        )
                    },
                    label = {
                        Text(
                            text = destination.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                )
            }
        }
    }
}
