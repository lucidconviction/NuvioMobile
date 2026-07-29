package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.player.PlayerLaunch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ObsidianBg = Color(0xFF000000)
private val SurfaceLow = Color(0xFF121212)
private val SurfaceCard = Color(0xFF1B1B1B)
private val OnSurface = Color(0xFFFFFFFF)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val TertiaryText = Color(0xFF888888)
private val BorderColor = Color(0xFF2A2A2A)
private val InputBg = Color(0xFF121212)
private val FocusRing = Color(0xFFFFFFFF)
private val Accent = Color(0xFF4A90D9)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentOrange = Color(0xFFFF9800)

@Composable
fun MusicNutzScreen(onPlayChannel: ((PlayerLaunch) -> Unit)? = null) {
    var uiState by remember { mutableStateOf(MusicNutzUiState()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var swipeAccumulator by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    LaunchedEffect(Unit) {
        MusicNutzPlaylistStore.loadFromDisk()
        MusicDownloadStore.loadFromDisk()
        MusicNutzSavedAlbumsStore.loadFromDisk()
        uiState = uiState.copy(
            playlists = MusicNutzPlaylistStore.loadPlaylists(),
            downloads = MusicDownloadStore.loadDownloads(),
            savedAlbums = MusicNutzSavedAlbumsStore.loadSavedAlbums(),
        )
    }

    fun swipeCategory(direction: Int) {
        val entries = MusicNutzCategory.entries
        val idx = entries.indexOf(uiState.selectedCategory)
        val newIdx = when (direction) { 1 -> if (idx >= entries.size - 1) 0 else idx + 1; -1 -> if (idx <= 0) entries.size - 1 else idx - 1; else -> idx }
        uiState = uiState.copy(selectedCategory = entries[newIdx], tracks = emptyList(), albums = emptyList(), searchResults = null, albumResults = null, searchQuery = "", currentPage = 1, albumPage = 1, hasMore = true, albumHasMore = true)
    }

    LaunchedEffect(uiState.selectedCategory, uiState.mode) {
        if (uiState.mode == MusicNutzMode.PLAYLISTS || uiState.mode == MusicNutzMode.DOWNLOADS || uiState.mode == MusicNutzMode.SAVED_ALBUMS) return@LaunchedEffect
        if (uiState.searchQuery.isNotBlank() || uiState.selectedAlbum != null) return@LaunchedEffect
        if (uiState.mode == MusicNutzMode.TRACKS) {
            uiState = uiState.copy(isLoading = true, currentPage = 1, hasMore = true)
            val tracks = with(scope) { MusicNutzRepository.fetchByCategory(uiState.selectedCategory) }
            uiState = uiState.copy(tracks = tracks, isLoading = false, hasMore = true)
        } else {
            uiState = uiState.copy(isLoadingAlbums = true, albumPage = 1, albumHasMore = true)
            val albums = with(scope) { MusicNutzRepository.fetchAlbumsByCategory(uiState.selectedCategory) }
            uiState = uiState.copy(albums = albums, isLoadingAlbums = false, albumHasMore = true)
        }
    }

    fun playTrack(track: MusicTrack, queueItems: List<MusicTrack> = emptyList()) {
        uiState = uiState.copy(isPlayingTrack = true, streamError = null)
        scope.launch {
            val result = MusicNutzRepository.resolveStream(track)
            if (result != null && onPlayChannel != null) {
                val fullQueue = if (queueItems.isNotEmpty()) queueItems else uiState.tracks
                val trackIdx = fullQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                val allUrls = mutableListOf<String>()
                val allTitles = mutableListOf<String>()
                for (t in fullQueue) {
                    if (t.id == track.id) {
                        allUrls.add(result.url)
                        allTitles.add(track.title)
                    } else {
                        val r = MusicNutzRepository.resolveStream(t)
                        allUrls.add(r?.url ?: "")
                        allTitles.add(t.title)
                    }
                }
                onPlayChannel(PlayerLaunch(
                    profileId = 0, title = track.title, sourceUrl = result.url,
                    sourceAudioUrl = result.audioUrl, qualities = result.qualities,
                    sourceHeaders = result.headers, poster = track.albumCover,
                    streamTitle = track.title, streamSubtitle = "${track.artistName} · ${track.albumName}",
                    providerName = "MusicNutz", parentMetaId = "music", parentMetaType = "music",
                    autoPlayQueueUrls = allUrls,
                    autoPlayQueueTitles = allTitles,
                    autoPlayQueueIndex = trackIdx,
                ))
                uiState = uiState.copy(isPlayingTrack = false)
            } else {
                uiState = uiState.copy(isPlayingTrack = false, streamError = "Could not resolve stream for \"${track.title}\"")
            }
        }
    }

    fun downloadTrack(track: MusicTrack) {
        if (uiState.downloads.any { it.trackId == track.id }) return
        val dl = MusicDownload(trackId = track.id, title = track.title, artistName = track.artistName, albumCover = track.albumCover, isDownloading = true, progress = 0f)
        uiState = uiState.copy(downloads = uiState.downloads + dl)
        MusicDownloadStore.saveDownloads(uiState.downloads)
        scope.launch {
            try {
                val result = MusicNutzRepository.resolveStream(track) ?: throw Exception("Could not resolve stream")
                val client = okhttp3.OkHttpClient.Builder().build()
                val request = okhttp3.Request.Builder().url(result.url).build()
                val response = client.newCall(request).execute()
                val body = response.body ?: throw Exception("No response body")
                val contentLength = body.contentLength()
                val buffer = java.io.ByteArrayOutputStream()
                val sink = body.source()
                val buf = okio.Buffer()
                var totalRead = 0L
                while (sink.read(buf, 8192L) != -1L) {
                    buf.copyTo(buffer)
                    totalRead += buf.size
                    buf.clear()
                    if (contentLength > 0) {
                        val p = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                        val updated = uiState.downloads.toMutableList()
                        val idx = updated.indexOfFirst { it.trackId == track.id }
                        if (idx >= 0) updated[idx] = updated[idx].copy(progress = p)
                        uiState = uiState.copy(downloads = updated)
                    }
                }
                val bytes = buffer.toByteArray()
                val fileName = "${track.id}_${track.title.take(30).replace(Regex("[^a-zA-Z0-9 ]"), "_")}.mp3"
                val dir = java.io.File(MusicNutzDownloadStorage.downloadDirPath, "music")
                dir.mkdirs()
                val file = java.io.File(dir, fileName)
                file.writeBytes(bytes)
                val updated = uiState.downloads.toMutableList()
                val idx = updated.indexOfFirst { it.trackId == track.id }
                if (idx >= 0) updated[idx] = updated[idx].copy(isDownloading = false, progress = 1f, localFilePath = file.absolutePath)
                uiState = uiState.copy(downloads = updated)
                MusicDownloadStore.saveDownloads(updated)
            } catch (e: Exception) {
                val updated = uiState.downloads.toMutableList()
                val idx = updated.indexOfFirst { it.trackId == track.id }
                if (idx >= 0) updated[idx] = updated[idx].copy(isDownloading = false, error = e.message)
                uiState = uiState.copy(downloads = updated)
                MusicDownloadStore.saveDownloads(updated)
            }
        }
    }

    fun downloadAlbum(album: MusicAlbum) {
        scope.launch {
            val tracks = MusicNutzRepository.fetchAlbumTracks(album.id)
            tracks.forEach { track -> downloadTrack(track) }
        }
    }

    fun downloadPlaylist(playlist: MusicNutzPlaylist) {
        playlist.tracks.forEach { track -> downloadTrack(track) }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val playlist = MusicNutzPlaylist(id = "pl_${System.currentTimeMillis()}", name = name, createdAtEpochMs = System.currentTimeMillis())
        val updated = uiState.playlists + playlist
        uiState = uiState.copy(playlists = updated, newPlaylistName = "")
        MusicNutzPlaylistStore.savePlaylists(updated)
    }

    fun addToPlaylist(playlistId: String, track: MusicTrack) {
        val updated = uiState.playlists.map { pl ->
            if (pl.id == playlistId && pl.tracks.none { it.id == track.id }) pl.copy(tracks = pl.tracks + track) else pl
        }
        uiState = uiState.copy(playlists = updated, showAddToPlaylist = null)
        MusicNutzPlaylistStore.savePlaylists(updated)
    }

    fun addAlbumTracksToPlaylist(playlistId: String, album: MusicAlbum) {
        scope.launch {
            val tracks = MusicNutzRepository.fetchAlbumTracks(album.id)
            val updated = uiState.playlists.map { pl ->
                if (pl.id == playlistId) {
                    val newTracks = tracks.filter { t -> pl.tracks.none { it.id == t.id } }
                    pl.copy(tracks = pl.tracks + newTracks)
                } else pl
            }
            uiState = uiState.copy(playlists = updated)
            MusicNutzPlaylistStore.savePlaylists(updated)
        }
    }

    fun deletePlaylist(id: String) {
        val updated = uiState.playlists.filter { it.id != id }
        uiState = uiState.copy(playlists = updated, currentPlaylist = if (uiState.currentPlaylist?.id == id) null else uiState.currentPlaylist)
        MusicNutzPlaylistStore.savePlaylists(updated)
    }

    fun removeDownload(trackId: Long) {
        val dl = uiState.downloads.find { it.trackId == trackId }
        if (dl?.localFilePath != null) try { java.io.File(dl.localFilePath!!).delete() } catch (_: Exception) {}
        val updated = uiState.downloads.filter { it.trackId != trackId }
        uiState = uiState.copy(downloads = updated)
        MusicDownloadStore.saveDownloads(updated)
    }

    fun toggleSavedAlbum(album: MusicAlbum) {
        val updated = if (uiState.savedAlbums.any { it.id == album.id }) {
            uiState.savedAlbums.filter { it.id != album.id }
        } else {
            uiState.savedAlbums + album
        }
        uiState = uiState.copy(savedAlbums = updated)
        MusicNutzSavedAlbumsStore.saveSavedAlbums(updated)
    }

    if (uiState.currentPlaylist != null) {
        val pl = uiState.currentPlaylist!!
        PlaylistDetailView(
            playlist = pl,
            onBack = { uiState = uiState.copy(currentPlaylist = null) },
            onPlayTrack = { playTrack(it) },
            onRemoveTrack = { trackId ->
                val updated = uiState.playlists.map { p ->
                    if (p.id == pl.id) p.copy(tracks = p.tracks.filter { it.id != trackId }) else p
                }
                uiState = uiState.copy(playlists = updated)
                MusicNutzPlaylistStore.savePlaylists(updated)
                uiState = uiState.copy(currentPlaylist = updated.find { it.id == pl.id })
            },
            onDeletePlaylist = { deletePlaylist(pl.id); uiState = uiState.copy(currentPlaylist = null) },
            onDownloadPlaylist = { downloadPlaylist(pl) },
            onDownloadTrack = { track -> downloadTrack(track) },
            onAddToPlaylistTrack = { track -> uiState = uiState.copy(showAddToPlaylist = track) },
        )
        return
    }

    if (uiState.selectedAlbum != null) {
        AlbumDetailView(
            album = uiState.selectedAlbum!!,
            tracks = uiState.albumTracks,
            isLoading = uiState.isLoadingAlbumTracks,
            onBack = { uiState = uiState.copy(selectedAlbum = null, albumTracks = emptyList()) },
            onPlayTrack = { playTrack(it, uiState.albumTracks) },
            onPlaylistsChanged = {
                uiState = uiState.copy(playlists = MusicNutzPlaylistStore.loadPlaylists())
            },
            onDownloadTrack = { downloadTrack(it) },
            onAddToPlaylistTrack = { uiState = uiState.copy(showAddToPlaylist = it) },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        OutlinedTextField(value = uiState.searchQuery,
            onValueChange = { q ->
                if (uiState.mode == MusicNutzMode.PLAYLISTS || uiState.mode == MusicNutzMode.DOWNLOADS || uiState.mode == MusicNutzMode.SAVED_ALBUMS) return@OutlinedTextField
                uiState = uiState.copy(searchQuery = q, searchCurrentPage = 1, searchHasMore = true, albumPage = 1, albumHasMore = true)
                searchJob?.cancel()
                if (q.isNotBlank()) {
                    searchJob = scope.launch {
                        delay(400)
                        if (uiState.mode == MusicNutzMode.TRACKS) uiState = uiState.copy(searchResults = MusicNutzRepository.search(q), searchHasMore = true)
                        else uiState = uiState.copy(albumResults = MusicNutzRepository.searchAlbums(q), albumHasMore = true)
                    }
                } else { uiState = uiState.copy(searchResults = null, albumResults = null) }
            },
            placeholder = { Text(
                when (uiState.mode) {
                    MusicNutzMode.TRACKS -> "Search songs..."
                    MusicNutzMode.ALBUMS -> "Search albums..."
                    else -> "Search..."
                }, color = TertiaryText, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TertiaryText, modifier = Modifier.size(18.dp)) },
            trailingIcon = { if (uiState.searchQuery.isNotEmpty()) { IconButton(onClick = { uiState = uiState.copy(searchQuery = "", searchResults = null, albumResults = null) }) { Icon(Icons.Filled.Clear, "Clear", tint = OnSurfaceVariant) } } },
            singleLine = true, shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor, cursorColor = OnSurface, focusedContainerColor = InputBg, unfocusedContainerColor = InputBg),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))

        if (uiState.mode == MusicNutzMode.TRACKS || uiState.mode == MusicNutzMode.ALBUMS) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MusicNutzCategory.entries.forEach { c -> MusicChip(c.displayName, c == uiState.selectedCategory) { uiState = uiState.copy(selectedCategory = c, tracks = emptyList(), albums = emptyList(), searchResults = null, albumResults = null, searchQuery = "", currentPage = 1, albumPage = 1, hasMore = true, albumHasMore = true) } }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MusicChip("Tracks", uiState.mode == MusicNutzMode.TRACKS) { uiState = uiState.copy(mode = MusicNutzMode.TRACKS, searchResults = null, albumResults = null) }
            MusicChip("Albums", uiState.mode == MusicNutzMode.ALBUMS) { uiState = uiState.copy(mode = MusicNutzMode.ALBUMS, searchResults = null, albumResults = null) }
            MusicChip("Playlists", uiState.mode == MusicNutzMode.PLAYLISTS) { uiState = uiState.copy(mode = MusicNutzMode.PLAYLISTS) }
            MusicChip("Downloads", uiState.mode == MusicNutzMode.DOWNLOADS) { uiState = uiState.copy(mode = MusicNutzMode.DOWNLOADS) }
            MusicChip("Saved", uiState.mode == MusicNutzMode.SAVED_ALBUMS) { uiState = uiState.copy(mode = MusicNutzMode.SAVED_ALBUMS) }
        }

        uiState.streamError?.let { err ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x33FF4444)).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(err, color = Color(0xFFFF4444), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { uiState = uiState.copy(streamError = null) }) { Icon(Icons.Filled.Clear, null, tint = Color(0xFFFF4444).copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().pointerInput(uiState.searchQuery.isBlank() && uiState.selectedAlbum == null && uiState.mode.ordinal <= 1) {
            if (uiState.searchQuery.isBlank() && uiState.selectedAlbum == null) {
                detectHorizontalDragGestures(onHorizontalDrag = { _, d -> swipeAccumulator += d; if (swipeAccumulator > swipeThresholdPx) { swipeCategory(-1); swipeAccumulator = 0f } else if (swipeAccumulator < -swipeThresholdPx) { swipeCategory(1); swipeAccumulator = 0f } }, onDragEnd = { swipeAccumulator = 0f }, onDragCancel = { swipeAccumulator = 0f })
            }
        }) {
            when (uiState.mode) {
                MusicNutzMode.PLAYLISTS -> PlaylistGrid(uiState, onPlaylistClick = { uiState = uiState.copy(currentPlaylist = it) }, onCreatePlaylist = { n -> createPlaylist(n) }, onDeletePlaylist = { deletePlaylist(it) })
                MusicNutzMode.DOWNLOADS -> DownloadsGrid(uiState, onPlay = { dl ->
                    playTrack(MusicTrack(id = dl.trackId, title = dl.title, artistName = dl.artistName, albumName = "", albumCover = dl.albumCover ?: "", durationSeconds = 0, previewUrl = null))
                }, onRemove = { removeDownload(it) })
                MusicNutzMode.SAVED_ALBUMS -> SavedAlbumsGrid(uiState, onAlbumClick = { a ->
                    uiState = uiState.copy(selectedAlbum = a, isLoadingAlbumTracks = true)
                    scope.launch { uiState = uiState.copy(albumTracks = MusicNutzRepository.fetchAlbumTracks(a.id), isLoadingAlbumTracks = false) }
                }, onToggleSave = { toggleSavedAlbum(it) })
                MusicNutzMode.ALBUMS -> AlbumGrid(uiState, onAlbumClick = { a ->
                    uiState = uiState.copy(selectedAlbum = a, isLoadingAlbumTracks = true)
                    scope.launch { uiState = uiState.copy(albumTracks = MusicNutzRepository.fetchAlbumTracks(a.id), isLoadingAlbumTracks = false) }
                }, onToggleSave = { toggleSavedAlbum(it) })
                MusicNutzMode.TRACKS -> TrackGrid(uiState, onPlayTrack = { playTrack(it) }, onDownload = { downloadTrack(it) }, onAddToPlaylist = { uiState = uiState.copy(showAddToPlaylist = it) })
            }
        }
    }

    if (uiState.isPlayingTrack) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Accent, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Resolving stream...", color = TertiaryText, fontSize = 13.sp)
            }
        }
    }

    uiState.showAddToPlaylist?.let { track ->
        AlertDialog(
            onDismissRequest = { uiState = uiState.copy(showAddToPlaylist = null) },
            title = { Text("Add to Playlist", color = OnSurface) },
            text = {
                Column {
                    if (uiState.playlists.isEmpty()) Text("No playlists yet. Create one!", color = TertiaryText, fontSize = 14.sp)
                    uiState.playlists.forEach { pl ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable { addToPlaylist(pl.id, track) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LibraryMusic, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(pl.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("${pl.trackCount} tracks", color = TertiaryText, fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { val name = "Playlist ${uiState.playlists.size + 1}"; createPlaylist(name); uiState.playlists.lastOrNull()?.let { addToPlaylist(it.id, track) } }) { Text("New Playlist", color = Accent) } },
            dismissButton = { TextButton(onClick = { uiState = uiState.copy(showAddToPlaylist = null) }) { Text("Cancel", color = TertiaryText) } },
            containerColor = Color(0xFF1A1A1A),
        )
    }
}

@Composable private fun TrackGrid(uiState: MusicNutzUiState, onPlayTrack: (MusicTrack) -> Unit, onDownload: (MusicTrack) -> Unit, onAddToPlaylist: (MusicTrack) -> Unit) {
    val display = uiState.searchResults ?: uiState.tracks
    if (uiState.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(8.dp)); Text("Loading...", color = TertiaryText, fontSize = 13.sp) } }
    else if (display.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tracks found", color = TertiaryText, fontSize = 14.sp) } }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(display, key = { "t_${it.id}" }) { t ->
                TrackCard(t) { onPlayTrack(t) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceCard).clickable { onAddToPlaylist(t) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("+Playlist", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceCard).clickable { onDownload(t) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Download", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable private fun AlbumGrid(uiState: MusicNutzUiState, onAlbumClick: (MusicAlbum) -> Unit, onToggleSave: (MusicAlbum) -> Unit) {
    val display = uiState.albumResults ?: uiState.albums
    if (uiState.isLoadingAlbums) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp)); Spacer(Modifier.height(8.dp)); Text("Loading albums...", color = TertiaryText, fontSize = 13.sp) } }
    else if (display.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No albums found", color = TertiaryText, fontSize = 14.sp) } }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(display, key = { "a_${it.id}" }) { a ->
                AlbumCard(a, isSaved = uiState.savedAlbums.any { it.id == a.id }, onClick = { onAlbumClick(a) }, onToggleSave = { onToggleSave(a) })
            }
        }
    }
}

@Composable private fun SavedAlbumsGrid(uiState: MusicNutzUiState, onAlbumClick: (MusicAlbum) -> Unit, onToggleSave: (MusicAlbum) -> Unit) {
    if (uiState.savedAlbums.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Bookmark, null, tint = TertiaryText, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(8.dp)); Text("No saved albums", color = TertiaryText, fontSize = 14.sp); Spacer(Modifier.height(4.dp)); Text("Save albums from the Albums tab", color = TertiaryText.copy(alpha = 0.6f), fontSize = 12.sp) } } }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(uiState.savedAlbums, key = { "sa_${it.id}" }) { a -> AlbumCard(a, isSaved = true, onClick = { onAlbumClick(a) }, onToggleSave = { onToggleSave(a) }) }
        }
    }
}

@Composable private fun PlaylistGrid(uiState: MusicNutzUiState, onPlaylistClick: (MusicNutzPlaylist) -> Unit, onCreatePlaylist: (String) -> Unit, onDeletePlaylist: (String) -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false; newName = "" },
            title = { Text("New Playlist", color = OnSurface) },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, placeholder = { Text("Playlist name", color = TertiaryText, fontSize = 14.sp) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, focusedBorderColor = Accent, unfocusedBorderColor = BorderColor, cursorColor = Accent, focusedContainerColor = InputBg, unfocusedContainerColor = InputBg), modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { onCreatePlaylist(newName); showCreate = false; newName = "" }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent), enabled = newName.isNotBlank()) { Text("Create", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showCreate = false; newName = "" }) { Text("Cancel", color = TertiaryText) } },
            containerColor = Color(0xFF1A1A1A),
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).clickable { showCreate = true }.padding(16.dp), contentAlignment = Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Filled.Add, null, tint = Accent, modifier = Modifier.size(20.dp)); Text("Create Playlist", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold) } } }
        if (uiState.playlists.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No playlists yet", color = TertiaryText, fontSize = 14.sp) } } }
        items(uiState.playlists, key = { it.id }) { pl ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).clickable { onPlaylistClick(pl) }.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LibraryMusic, null, tint = Accent, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(pl.name, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("${pl.trackCount} tracks · ${pl.durationSeconds / 60} min", color = TertiaryText, fontSize = 12.sp) }
                    IconButton(onClick = { onDeletePlaylist(pl.id) }) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF4444), modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable private fun DownloadsGrid(uiState: MusicNutzUiState, onPlay: (MusicDownload) -> Unit, onRemove: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uiState.downloads.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No downloads yet", color = TertiaryText, fontSize = 14.sp) } } }
        items(uiState.downloads, key = { "dl_${it.trackId}" }) { dl ->
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceLow), contentAlignment = Alignment.Center) {
                        if (!dl.albumCover.isNullOrBlank()) AsyncImage(dl.albumCover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Filled.LibraryMusic, null, tint = TertiaryText, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(dl.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dl.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (dl.error != null) Text(dl.error, color = Color(0xFFFF4444), fontSize = 10.sp, maxLines = 1)
                        if (dl.isDownloading && dl.progress > 0f) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { dl.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = Accent, trackColor = SurfaceCard)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (dl.isDownloading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            if (dl.progress > 0f) Text("${(dl.progress * 100).toInt()}%", color = Accent, fontSize = 9.sp)
                        }
                    } else if (dl.localFilePath != null) { IconButton(onClick = { onPlay(dl) }) { Icon(Icons.Filled.PlayArrow, "Play", tint = AccentGreen, modifier = Modifier.size(24.dp)) } }
                    IconButton(onClick = { onRemove(dl.trackId) }) { Icon(Icons.Filled.Delete, "Remove", tint = Color(0xFFFF4444).copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

@Composable private fun PlaylistDetailView(playlist: MusicNutzPlaylist, onBack: () -> Unit, onPlayTrack: (MusicTrack) -> Unit, onRemoveTrack: (Long) -> Unit, onDeletePlaylist: () -> Unit, onDownloadPlaylist: (() -> Unit)? = null, onDownloadTrack: ((MusicTrack) -> Unit)? = null, onAddToPlaylistTrack: ((MusicTrack) -> Unit)? = null) {
    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface) }
            Column(Modifier.weight(1f)) { Text(playlist.name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${playlist.trackCount} tracks · ${playlist.durationSeconds / 60} min", color = TertiaryText, fontSize = 12.sp) }
            if (onDownloadPlaylist != null) {
                IconButton(onClick = onDownloadPlaylist) { Icon(Icons.Filled.Download, "Download All", tint = AccentGreen, modifier = Modifier.size(20.dp)) }
            }
            IconButton(onClick = onDeletePlaylist) { Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF4444)) }
        }
        LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            if (playlist.tracks.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No tracks in this playlist", color = TertiaryText, fontSize = 14.sp) } } }
            items(playlist.tracks, key = { "plt_${it.id}" }) { track ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPlayTrack(track) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) { Text(track.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artistName, color = TertiaryText, fontSize = 11.sp) }
                    Text(musicDuration(track.durationSeconds), color = TertiaryText, fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    if (onDownloadTrack != null) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AccentGreen.copy(alpha = 0.2f)).clickable { onDownloadTrack(track) }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("DL", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (onAddToPlaylistTrack != null) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceCard).clickable { onAddToPlaylistTrack(track) }.padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Text("+PL", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { onRemoveTrack(track.id) }) { Icon(Icons.Filled.Clear, "Remove", tint = Color(0xFFFF4444).copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

@Composable private fun AlbumDetailView(album: MusicAlbum, tracks: List<MusicTrack>, isLoading: Boolean, onBack: () -> Unit, onPlayTrack: (MusicTrack) -> Unit, onPlaylistsChanged: () -> Unit = {}, onDownloadTrack: ((MusicTrack) -> Unit)? = null, onAddToPlaylistTrack: ((MusicTrack) -> Unit)? = null) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var playlistPickerAlbum by remember { mutableStateOf<MusicAlbum?>(null) }
    val scope = rememberCoroutineScope()

    if (showPlaylistPicker && playlistPickerAlbum != null) {
        val albumRef = playlistPickerAlbum!!
        val plState = MusicNutzPlaylistStore.loadPlaylists()
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false; playlistPickerAlbum = null },
            title = { Text("Add album tracks to...", color = OnSurface) },
            text = {
                Column {
                    if (plState.isEmpty()) Text("No playlists yet", color = TertiaryText, fontSize = 14.sp)
                    plState.forEach { pl ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable {
                            scope.launch {
                                val albumTracks = MusicNutzRepository.fetchAlbumTracks(albumRef.id)
                                val updatedPl = MusicNutzPlaylistStore.loadPlaylists().map { p ->
                                    if (p.id == pl.id) {
                                        val newTracks = albumTracks.filter { t -> p.tracks.none { it.id == t.id } }
                                        p.copy(tracks = p.tracks + newTracks)
                                    } else p
                                }
                                MusicNutzPlaylistStore.savePlaylists(updatedPl)
                                onPlaylistsChanged()
                            }
                            showPlaylistPicker = false; playlistPickerAlbum = null
                        }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LibraryMusic, null, tint = OnSurface, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) { Text(pl.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text("${pl.trackCount} tracks", color = TertiaryText, fontSize = 11.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlaylistPicker = false; playlistPickerAlbum = null }) { Text("Cancel", color = TertiaryText) } },
            containerColor = Color(0xFF1A1A1A),
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface) }
            Text(album.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { playlistPickerAlbum = album; showPlaylistPicker = true }) { Icon(Icons.Filled.LibraryAdd, "Add to Playlist", tint = Accent) }
        }
        LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item(key = "__header__") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    AsyncImage(model = album.coverUrl, contentDescription = album.title, modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceLow), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(12.dp))
                    Text(album.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(album.artistName, color = OnSurfaceVariant, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    if (album.releaseDate.isNotBlank()) Text(album.releaseDate.take(4), color = TertiaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    if (tracks.isNotEmpty()) Text("${tracks.size} tracks", color = TertiaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(AccentGreen.copy(alpha = 0.2f)).clickable {
                            scope.launch {
                                val albumTracks = MusicNutzRepository.fetchAlbumTracks(album.id)
                                albumTracks.forEach { track -> onDownloadTrack?.invoke(track) }
                            }
                        }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("Download All", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable { playlistPickerAlbum = album; showPlaylistPicker = true }.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text("Add All to Playlist", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (isLoading) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) } } }
            items(tracks, key = { "at_${it.id}" }) { track ->
                AlbumTrackRow(
                    track = track,
                    onPlay = { onPlayTrack(track) },
                    onDownload = { onDownloadTrack?.invoke(track) },
                    onAddToPlaylist = { onAddToPlaylistTrack?.invoke(track) },
                )
            }
        }
    }
}

@Composable private fun AlbumTrackRow(track: MusicTrack, onPlay: () -> Unit, onDownload: () -> Unit = {}, onAddToPlaylist: () -> Unit = {}) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) SurfaceCard else Color.Transparent)
        .then(if (focused) Modifier.border(1.5.dp, FocusRing, RoundedCornerShape(8.dp)) else Modifier).focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.PlayArrow, "Play", tint = if (focused) Color.White else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) { Text(track.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(track.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Text(musicDuration(track.durationSeconds), color = TertiaryText, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Spacer(Modifier.width(4.dp))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AccentGreen.copy(alpha = 0.2f)).clickable(onClick = onDownload).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text("DL", color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceCard).clickable(onClick = onAddToPlaylist).padding(horizontal = 6.dp, vertical = 3.dp)) {
            Text("+PL", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun MusicChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(when { isSelected -> Color.White; focused -> Color.White.copy(alpha = 0.15f); else -> Color.Transparent })
        .then(if (!isSelected && focused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier).clickable(onClick = onClick).focusable().onFocusChanged { focused = it.isFocused }.padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(label, color = if (isSelected || focused) Color.Black else OnSurfaceVariant, fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
    }
}

@Composable private fun TrackCard(track: MusicTrack, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }; val ts = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onPlay)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ts).background(SurfaceLow).then(if (focused) Modifier.border(2.dp, FocusRing, ts) else Modifier)) {
            AsyncImage(track.albumCover, track.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White.copy(alpha = if (focused) 1f else 0.7f), modifier = Modifier.size(if (focused) 44.dp else 36.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = if (focused) 0.5f else 0.3f)).padding(8.dp))
            if (track.durationSeconds > 0) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(musicDuration(track.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
        }
        Text(track.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        Text(track.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        if (track.albumName.isNotBlank()) Text(track.albumName, color = TertiaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable private fun AlbumCard(album: MusicAlbum, isSaved: Boolean, onClick: () -> Unit, onToggleSave: () -> Unit) {
    var focused by remember { mutableStateOf(false) }; val ts = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ts).background(SurfaceLow).then(if (focused) Modifier.border(2.dp, FocusRing, ts) else Modifier)) {
            AsyncImage(album.coverUrl, album.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Filled.PlayArrow, "View", tint = Color.White.copy(alpha = if (focused) 1f else 0.7f), modifier = Modifier.size(if (focused) 44.dp else 36.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = if (focused) 0.5f else 0.3f)).padding(8.dp))
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp).clip(CircleShape).background(if (isSaved) AccentGreen else Color.Black.copy(alpha = 0.5f)).clickable { onToggleSave() }.padding(6.dp)) {
                Icon(if (isSaved) Icons.Filled.Bookmark else Icons.Filled.Bookmark, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(album.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        Text(album.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        if (album.releaseDate.isNotBlank()) Text(album.releaseDate.take(4), color = TertiaryText, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

private fun musicDuration(sec: Int): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
