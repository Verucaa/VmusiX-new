package com.example.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.GlassCard
import com.example.core.components.TrackItemRow
import com.example.core.components.VmusixLoader
import com.example.data.cache.CacheManager
import com.example.data.download.DownloadManager
import com.example.data.lastfm.LastFmService
import com.example.domain.model.AppThemePreset
import com.example.domain.model.DownloadItem
import com.example.domain.model.DownloadStatus
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MidnightPurple
import com.example.ui.theme.VmusixCardBorder
import com.example.ui.theme.VmusixSurfaceVariant
import com.example.ui.theme.VmusixTextMuted
import com.example.ui.theme.VmusixTextPrimary
import com.example.ui.theme.VmusixTextSecondary
import kotlinx.coroutines.launch

enum class LibraryTab {
    DASHBOARD, ALL_MUSIC, CACHE, LIKED, DOWNLOADS, TOP50, UPLOADED, PLAYLISTS, SETTINGS
}

@Composable
fun LibraryScreen(
    likedTracks: List<Track>,
    downloadedTracks: List<Track>,
    top50Tracks: List<Track>,
    allLibraryTracks: List<Track>,
    uploadedTracks: List<Track>,
    isScanningLibrary: Boolean,
    playlists: List<Playlist>,
    cacheManager: CacheManager,
    downloadManager: DownloadManager,
    lastFmService: LastFmService,
    currentThemePreset: AppThemePreset,
    onThemeSelected: (AppThemePreset) -> Unit,
    onRefreshLibrary: () -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackSelect: (Track, List<Track>) -> Unit,
    onLikeToggle: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(LibraryTab.DASHBOARD) }
    val cacheStats by cacheManager.stats.collectAsState()
    val autoCacheEnabled by cacheManager.autoCacheEnabled.collectAsState()
    val downloads by downloadManager.downloadsFlow.collectAsState(initial = emptyList())
    val lastFmConfig by lastFmService.config.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // SAF Document Picker for importing music without permissions
    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onImportUris(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (activeTab == LibraryTab.DASHBOARD) "Lainnya" else getTabTitle(activeTab),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = VmusixTextPrimary
                )
                Text(
                    text = "Perpustakaan & Musik Lengkap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (activeTab != LibraryTab.DASHBOARD) {
                TextButton(
                    onClick = { activeTab = LibraryTab.DASHBOARD },
                    modifier = Modifier.testTag("library_back_button")
                ) {
                    Text("Kembali", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefreshLibrary,
                        modifier = Modifier.testTag("refresh_library_button")
                    ) {
                        if (isScanningLibrary) {
                            VmusixLoader(size = 20.dp, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Pindai Musik",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { activeTab = LibraryTab.SETTINGS },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Pengaturan Tema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Body Content
        when (activeTab) {
            LibraryTab.DASHBOARD -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hero Card: Semua Musik Perpustakaan
                    item {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_all_music_hero"),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            onClick = { activeTab = LibraryTab.ALL_MUSIC }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(MidnightPurple, ElectricBlue)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Semua Musik",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = VmusixTextPrimary
                                        )
                                        Text(
                                            text = "${allLibraryTracks.size} lagu dimuat beserta gambar & audio",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    if (isScanningLibrary) {
                                        VmusixLoader(size = 24.dp, strokeWidth = 2.5.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (allLibraryTracks.isNotEmpty()) {
                                                onTrackSelect(allLibraryTracks.first(), allLibraryTracks)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Putar Semua", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onRefreshLibrary,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pindai Ulang", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        ModernLibraryCard(
                            title = "Diunggah & File Lokal",
                            subtitle = "${uploadedTracks.size} file musik lokal terdeteksi",
                            icon = Icons.Default.CloudUpload,
                            accentColor = Color(0xFF8B5CF6),
                            onClick = { activeTab = LibraryTab.UPLOADED }
                        )
                    }

                    item {
                        ModernLibraryCard(
                            title = "Disukai",
                            subtitle = "${likedTracks.size} lagu tersimpan",
                            icon = Icons.Default.Favorite,
                            accentColor = Color(0xFFEF4444),
                            onClick = { activeTab = LibraryTab.LIKED }
                        )
                    }
                    item {
                        ModernLibraryCard(
                            title = "Diunduh",
                            subtitle = "${downloadedTracks.size} audio siap diputar offline",
                            icon = Icons.Default.Download,
                            accentColor = ElectricBlue,
                            onClick = { activeTab = LibraryTab.DOWNLOADS }
                        )
                    }
                    item {
                        ModernLibraryCard(
                            title = "Cache & Penyimpanan",
                            subtitle = "${"%.1f".format(cacheStats.totalCacheMb)} MB digunakan • ${cacheStats.lyricsCacheCount} lirik offline",
                            icon = Icons.Default.Storage,
                            accentColor = MidnightPurple,
                            onClick = { activeTab = LibraryTab.CACHE }
                        )
                    }
                    item {
                        ModernLibraryCard(
                            title = "50 Teratas",
                            subtitle = "Tangga lagu terpopuler minggu ini",
                            icon = Icons.Default.Leaderboard,
                            accentColor = Color(0xFFF59E0B),
                            onClick = { activeTab = LibraryTab.TOP50 }
                        )
                    }
                    item {
                        ModernLibraryCard(
                            title = "Playlist",
                            subtitle = "${playlists.size} daftar putar kustom & pintar",
                            icon = Icons.Default.PlaylistPlay,
                            accentColor = Color(0xFF10B981),
                            onClick = { activeTab = LibraryTab.PLAYLISTS }
                        )
                    }
                    item {
                        ModernLibraryCard(
                            title = "Last.fm Scrobbler",
                            subtitle = "${lastFmConfig.totalScrobbles} lagu telah di-scrobble",
                            icon = Icons.Default.Radio,
                            accentColor = Color(0xFFEC4899),
                            onClick = { activeTab = LibraryTab.SETTINGS }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            LibraryTab.ALL_MUSIC -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Control Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (allLibraryTracks.isNotEmpty()) {
                                    onTrackSelect(allLibraryTracks.first(), allLibraryTracks)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putar Semua (${allLibraryTracks.size})")
                        }

                        Button(
                            onClick = {
                                if (allLibraryTracks.isNotEmpty()) {
                                    val shuffled = allLibraryTracks.shuffled()
                                    onTrackSelect(shuffled.first(), shuffled)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Acak Lagu")
                        }
                    }

                    if (allLibraryTracks.isEmpty()) {
                        if (isScanningLibrary) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                VmusixLoader(size = 48.dp)
                            }
                        } else {
                            EmptyStateView(
                                icon = Icons.Default.LibraryMusic,
                                title = "Belum Ada Musik di Library",
                                desc = "Ketuk Pindai Ulang untuk memuat semua musik perpustakaan."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            items(allLibraryTracks) { track ->
                                TrackItemRow(
                                    track = track,
                                    isPlaying = track.id == currentTrackId && isPlaying,
                                    onClick = { onTrackSelect(track, allLibraryTracks) },
                                    onLikeToggle = { onLikeToggle(track) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            LibraryTab.UPLOADED -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Action Buttons: Scan & SAF Import
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRefreshLibrary,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pindai Perangkat", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                musicPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pilih File Audio", fontSize = 12.sp)
                        }
                    }

                    val displayTracks = if (uploadedTracks.isNotEmpty()) uploadedTracks else allLibraryTracks

                    if (displayTracks.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.CloudUpload,
                            title = "File Lokal & Diunggah",
                            desc = "Pindai musik dari perangkat atau pilih berkas audio langsung dari penyimpanan."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            items(displayTracks) { track ->
                                TrackItemRow(
                                    track = track,
                                    isPlaying = track.id == currentTrackId && isPlaying,
                                    onClick = { onTrackSelect(track, displayTracks) },
                                    onLikeToggle = { onLikeToggle(track) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            LibraryTab.LIKED -> {
                if (likedTracks.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Favorite,
                        title = "Belum Ada Lagu yang Disukai",
                        desc = "Ketuk ikon hati pada lagu apa pun untuk menyimpannya di sini."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        item {
                            Button(
                                onClick = { onTrackSelect(likedTracks.first(), likedTracks) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Putar Semua Lagu Disukai")
                            }
                        }
                        items(likedTracks) { track ->
                            TrackItemRow(
                                track = track,
                                isPlaying = track.id == currentTrackId && isPlaying,
                                onClick = { onTrackSelect(track, likedTracks) },
                                onLikeToggle = { onLikeToggle(track) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            LibraryTab.DOWNLOADS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (downloads.isEmpty() && downloadedTracks.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Download,
                                title = "Belum Ada Lagu yang Diunduh",
                                desc = "Unduh lagu, album, atau playlist favorit untuk mendengarkan tanpa kuota."
                            )
                        }
                    } else {
                        items(downloads) { item ->
                            DownloadRow(
                                item = item,
                                onPause = { downloadManager.pauseDownload(item.trackId) },
                                onResume = { downloadManager.resumeDownload(item.trackId) },
                                onCancel = { downloadManager.cancelDownload(item.trackId) }
                            )
                        }
                        items(downloadedTracks) { track ->
                            TrackItemRow(
                                track = track,
                                isPlaying = track.id == currentTrackId && isPlaying,
                                onClick = { onTrackSelect(track, downloadedTracks) },
                                onLikeToggle = { onLikeToggle(track) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            LibraryTab.CACHE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Statistik Cache",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VmusixTextPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            StatRow("Audio Cache", "${"%.1f".format(cacheStats.audioCacheMb)} MB")
                            StatRow("Lirik Offline", "${cacheStats.lyricsCacheCount} track")
                            StatRow("Gambar & Artwork", "${"%.1f".format(cacheStats.imageCacheMb)} MB")
                            Spacer(modifier = Modifier.height(8.dp))
                            StatRow("Total Penyimpanan", "${"%.1f".format(cacheStats.totalCacheMb)} MB", isHighlight = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto Cache Lagu",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VmusixTextPrimary
                                )
                                Text(
                                    text = "Simpan audio & lirik otomatis saat diputar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VmusixTextMuted
                                )
                            }
                            Switch(
                                checked = autoCacheEnabled,
                                onCheckedChange = { cacheManager.setAutoCache(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            scope.launch { cacheManager.clearCache() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Seluruh Cache")
                    }
                }
            }

            LibraryTab.TOP50 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(top50Tracks) { track ->
                        TrackItemRow(
                            track = track,
                            isPlaying = track.id == currentTrackId && isPlaying,
                            onClick = { onTrackSelect(track, top50Tracks) },
                            onLikeToggle = { onLikeToggle(track) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            LibraryTab.PLAYLISTS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buat Playlist Baru")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playlists) { pl ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = VmusixTextPrimary
                                        )
                                        Text(
                                            text = if (pl.isAutoGenerated) "Smart Auto Playlist" else "Playlist Kustom",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VmusixTextMuted
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            LibraryTab.SETTINGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Theme Selector Section
                    item {
                        Text(
                            text = "Tema & Identitas Visual",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VmusixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AppThemePreset.values().forEach { preset ->
                            val isSelected = preset == currentThemePreset
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else VmusixCardBorder,
                                borderWidth = if (isSelected) 2.dp else 1.dp,
                                onClick = { onThemeSelected(preset) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(preset.primaryHex), Color(preset.secondaryHex))
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else VmusixTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Text("Aktif", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Last.fm Scrobbler Configuration
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last.fm Scrobbler",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VmusixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scrobble Otomatis",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = VmusixTextPrimary
                                    )
                                    Switch(
                                        checked = lastFmConfig.isScrobblingEnabled,
                                        onCheckedChange = {
                                            lastFmService.updateConfig(it, lastFmConfig.username)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Akun: ${lastFmConfig.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VmusixTextSecondary
                                )
                                Text(
                                    text = "Total scrobbles: ${lastFmConfig.totalScrobbles} track",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VmusixTextMuted
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = VmusixSurfaceVariant,
            title = { Text("Buat Playlist Baru", color = VmusixTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    placeholder = { Text("Nama playlist...", color = VmusixTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VmusixTextPrimary,
                        unfocusedTextColor = VmusixTextPrimary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistTitle.isNotBlank()) {
                            onCreatePlaylist(newPlaylistTitle.trim())
                            newPlaylistTitle = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ModernLibraryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VmusixTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VmusixTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.trackTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = VmusixTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.artist} • Status: ${item.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VmusixTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.status == DownloadStatus.DOWNLOADING) {
                    IconButton(onClick = onPause) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Jeda", tint = VmusixTextSecondary)
                    }
                } else if (item.status == DownloadStatus.PAUSED) {
                    IconButton(onClick = onResume) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Lanjutkan", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isHighlight) VmusixTextPrimary else VmusixTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = if (isHighlight) MaterialTheme.colorScheme.secondary else VmusixTextPrimary
        )
    }
}

@Composable
private fun EmptyStateView(icon: ImageVector, title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = VmusixTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = VmusixTextMuted
        )
    }
}

private fun getTabTitle(tab: LibraryTab): String = when (tab) {
    LibraryTab.DASHBOARD -> "Lainnya"
    LibraryTab.ALL_MUSIC -> "Semua Musik Perpustakaan"
    LibraryTab.CACHE -> "Cache & Penyimpanan"
    LibraryTab.LIKED -> "Lagu Disukai"
    LibraryTab.DOWNLOADS -> "Unduhan Offline"
    LibraryTab.TOP50 -> "50 Teratas"
    LibraryTab.UPLOADED -> "File Diunggah & Lokal"
    LibraryTab.PLAYLISTS -> "Daftar Putar"
    LibraryTab.SETTINGS -> "Pengaturan & Tema"
}
