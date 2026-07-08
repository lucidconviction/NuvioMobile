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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun MusicNutzScreen(onPlayChannel: ((PlayerLaunch) -> Unit)? = null) {
    var uiState by remember { mutableStateOf(MusicNutzUiState()) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var swipeAccumulator by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    fun swipeCategory(direction: Int) {
        val entries = MusicNutzCategory.entries
        val idx = entries.indexOf(uiState.selectedCategory)
        val newIdx = when (direction) { 1 -> if (idx >= entries.size - 1) 0 else idx + 1; -1 -> if (idx <= 0) entries.size - 1 else idx - 1; else -> idx }
        uiState = uiState.copy(selectedCategory = entries[newIdx], tracks = emptyList(), albums = emptyList(), searchResults = null, albumResults = null, searchQuery = "", currentPage = 1, albumPage = 1, hasMore = true, albumHasMore = true)
    }

    LaunchedEffect(uiState.selectedCategory, uiState.mode) {
        if (uiState.searchQuery.isNotBlank() || uiState.selectedAlbum != null) return@LaunchedEffect
        if (uiState.mode == MusicNutzMode.TRACKS) {
            uiState = uiState.copy(isLoading = true, currentPage = 1, hasMore = true)
            uiState = uiState.copy(tracks = MusicNutzRepository.fetchByCategory(uiState.selectedCategory), isLoading = false, hasMore = true)
        } else {
            uiState = uiState.copy(isLoadingAlbums = true, albumPage = 1, albumHasMore = true)
            uiState = uiState.copy(albums = MusicNutzRepository.fetchAlbumsByCategory(uiState.selectedCategory), isLoadingAlbums = false, albumHasMore = true)
        }
    }

    fun playTrack(track: MusicTrack) {
        scope.launch {
            val result = MusicNutzRepository.resolveStream(track)
            if (result != null && onPlayChannel != null) {
                onPlayChannel(PlayerLaunch(profileId = 0, title = track.title, sourceUrl = result.url,
                    sourceHeaders = result.headers, poster = track.albumCover,
                    streamTitle = track.title, streamSubtitle = "${track.artistName} · ${track.albumName}",
                    providerName = "YouTube",
                    parentMetaId = "music", parentMetaType = "music"))
            }
        }
    }

    // Album detail view
    if (uiState.selectedAlbum != null) {
        AlbumDetailView(album = uiState.selectedAlbum!!, tracks = uiState.albumTracks, isLoading = uiState.isLoadingAlbumTracks,
            onBack = { uiState = uiState.copy(selectedAlbum = null, albumTracks = emptyList()) },
            onPlayTrack = { track -> playTrack(track) })
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        OutlinedTextField(value = uiState.searchQuery, onValueChange = { q ->
            uiState = uiState.copy(searchQuery = q, searchCurrentPage = 1, searchHasMore = true, albumPage = 1, albumHasMore = true)
            searchJob?.cancel()
            if (q.isNotBlank()) {
                searchJob = scope.launch {
                    delay(400)
                    if (uiState.mode == MusicNutzMode.TRACKS) uiState = uiState.copy(searchResults = MusicNutzRepository.search(q), searchHasMore = true)
                    else uiState = uiState.copy(albumResults = MusicNutzRepository.searchAlbums(q), albumHasMore = true)
                }
            } else { uiState = uiState.copy(searchResults = null, albumResults = null) }
        }, placeholder = { Text(if (uiState.mode == MusicNutzMode.TRACKS) "Search songs..." else "Search albums...", color = TertiaryText, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TertiaryText, modifier = Modifier.size(18.dp)) },
            trailingIcon = { if (uiState.searchQuery.isNotEmpty()) { IconButton(onClick = { uiState = uiState.copy(searchQuery = "", searchResults = null, albumResults = null) }) { Icon(Icons.Filled.Clear, "Clear", tint = OnSurfaceVariant) } } },
            singleLine = true, shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = OnSurface, unfocusedTextColor = OnSurface, focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor, cursorColor = OnSurface, focusedContainerColor = InputBg, unfocusedContainerColor = InputBg),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MusicNutzCategory.entries.forEach { c -> MusicChip(c.displayName, c == uiState.selectedCategory) { uiState = uiState.copy(selectedCategory = c, tracks = emptyList(), albums = emptyList(), searchResults = null, albumResults = null, searchQuery = "", currentPage = 1, albumPage = 1, hasMore = true, albumHasMore = true) } }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MusicChip("Tracks", uiState.mode == MusicNutzMode.TRACKS) { uiState = uiState.copy(mode = MusicNutzMode.TRACKS, searchResults = null, albumResults = null) }
            MusicChip("Albums", uiState.mode == MusicNutzMode.ALBUMS) { uiState = uiState.copy(mode = MusicNutzMode.ALBUMS, searchResults = null, albumResults = null) }
        }

        Box(modifier = Modifier.fillMaxSize().pointerInput(uiState.searchQuery.isBlank() && uiState.selectedAlbum == null) {
            if (uiState.searchQuery.isBlank() && uiState.selectedAlbum == null) {
                detectHorizontalDragGestures(onHorizontalDrag = { _, d -> swipeAccumulator += d; if (swipeAccumulator > swipeThresholdPx) { swipeCategory(-1); swipeAccumulator = 0f } else if (swipeAccumulator < -swipeThresholdPx) { swipeCategory(1); swipeAccumulator = 0f } }, onDragEnd = { swipeAccumulator = 0f }, onDragCancel = { swipeAccumulator = 0f })
            }
        }) {
            if (uiState.mode == MusicNutzMode.ALBUMS) AlbumGrid(uiState, onAlbumClick = { a ->
                uiState = uiState.copy(selectedAlbum = a, isLoadingAlbumTracks = true)
                scope.launch { uiState = uiState.copy(albumTracks = MusicNutzRepository.fetchAlbumTracks(a.id), isLoadingAlbumTracks = false) }
            }) else TrackGrid(uiState, onPlayTrack = { playTrack(it) })
        }
    }
}

@Composable private fun TrackGrid(uiState: MusicNutzUiState, onPlayTrack: (MusicTrack) -> Unit) {
    val display = uiState.searchResults ?: uiState.tracks
    if (uiState.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = OnSurfaceVariant, fontSize = 14.sp) } }
    else if (display.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tracks found", color = OnSurfaceVariant, fontSize = 14.sp) } }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(display, key = { "t_${it.id}" }) { t -> TrackCard(t) { onPlayTrack(t) } }
        }
    }
}

@Composable private fun AlbumGrid(uiState: MusicNutzUiState, onAlbumClick: (MusicAlbum) -> Unit) {
    val display = uiState.albumResults ?: uiState.albums
    if (uiState.isLoadingAlbums) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = OnSurfaceVariant, fontSize = 14.sp) } }
    else if (display.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No albums found", color = OnSurfaceVariant, fontSize = 14.sp) } }
    else {
        LazyVerticalGrid(columns = GridCells.Fixed(2), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(display, key = { "a_${it.id}" }) { a -> AlbumCard(a) { onAlbumClick(a) } }
        }
    }
}

@Composable
private fun AlbumDetailView(album: MusicAlbum, tracks: List<MusicTrack>, isLoading: Boolean, onBack: () -> Unit, onPlayTrack: (MusicTrack) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface) }
            Text(album.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                }
            }
            if (isLoading) { item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp)) } } }
            items(tracks, key = { "at_${it.id}" }) { track -> AlbumTrackRow(track) { onPlayTrack(track) } }
        }
    }
}

@Composable private fun AlbumTrackRow(track: MusicTrack, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) SurfaceCard else Color.Transparent)
        .then(if (focused) Modifier.border(1.5.dp, FocusRing, RoundedCornerShape(8.dp)) else Modifier).focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.PlayArrow, "Play", tint = if (focused) Color.White else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(musicDuration(track.durationSeconds), color = TertiaryText, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}

@Composable private fun MusicChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(when { isSelected -> Color.White; focused -> Color.White.copy(alpha = 0.15f); else -> Color.Transparent })
        .then(if (!isSelected && focused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier).clickable(onClick = onClick).focusable().onFocusChanged { focused = it.isFocused }.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, color = if (isSelected || focused) Color.Black else OnSurfaceVariant, fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable private fun TrackCard(track: MusicTrack, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }; val ts = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onPlay)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ts).background(SurfaceLow).then(if (focused) Modifier.border(2.dp, FocusRing, ts) else Modifier)) {
            AsyncImage(track.albumCover, track.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White.copy(alpha = if (focused) 1f else 0.7f),
                modifier = Modifier.size(if (focused) 44.dp else 36.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = if (focused) 0.5f else 0.3f)).padding(8.dp))
            if (track.durationSeconds > 0) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(musicDuration(track.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
        }
        Text(track.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        Text(track.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        if (track.albumName.isNotBlank()) Text(track.albumName, color = TertiaryText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable private fun AlbumCard(album: MusicAlbum, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }; val ts = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ts).background(SurfaceLow).then(if (focused) Modifier.border(2.dp, FocusRing, ts) else Modifier)) {
            AsyncImage(album.coverUrl, album.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Filled.PlayArrow, "View", tint = Color.White.copy(alpha = if (focused) 1f else 0.7f),
                modifier = Modifier.size(if (focused) 44.dp else 36.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = if (focused) 0.5f else 0.3f)).padding(8.dp))
        }
        Text(album.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        Text(album.artistName, color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        if (album.releaseDate.isNotBlank()) Text(album.releaseDate.take(4), color = TertiaryText, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

private fun musicDuration(sec: Int): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
