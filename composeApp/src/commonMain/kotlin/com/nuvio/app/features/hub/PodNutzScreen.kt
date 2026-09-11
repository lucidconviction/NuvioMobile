package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.WatchProgressUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PodBg = Color(0xFF000000)
private val PodSurface = Color(0xFF1B1B1B)
private val PodSurfaceLow = Color(0xFF121212)
private val PodCard = Color(0xFF1B1B1B)
private val PodOnSurface = Color(0xFFFFFFFF)
private val PodSecondary = Color(0xFFB0B0B0)
private val PodTertiary = Color(0xFF888888)
private val PodBorder = Color(0xFF2A2A2A)
private val PodFocus = Color(0xFFFFFFFF)
private val PodAccent = Color(0xFF4A90D9)

private val podGenreChips = listOf(
    "True Crime",
    "Comedy",
    "News",
    "Sports",
    "Technology",
    "History",
    "Sci-Fi",
    "Business",
    "Health",
    "Horror",
)

@Composable
fun PodNutzScreen(onPlayChannel: ((PlayerLaunch) -> Unit)? = null) {
    var featured by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Podcast>?>(null) }
    var selected by remember { mutableStateOf<Podcast?>(null) }
    var episodes by remember { mutableStateOf<List<PodcastEpisode>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var saved by remember { mutableStateOf(PodNutzStore.loadSaved()) }
    val progressState by WatchProgressRepository.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    fun isSaved(pod: Podcast): Boolean = saved.any { it.id == pod.id }

    fun toggleSave(pod: Podcast) {
        val updated = saved.toMutableList()
        val exists = updated.any { it.id == pod.id }
        if (exists) {
            updated.removeAll { it.id == pod.id }
        } else {
            updated.add(0, pod)
        }
        saved = updated
        PodNutzStore.saveSaved(updated)
    }

    LaunchedEffect(Unit) {
        WatchProgressRepository.ensureLoaded()
        if (featured.isEmpty()) {
            loading = true
            featured = PodNutzRepository.fetchFeatured()
            loading = false
        }
    }

    LaunchedEffect(selected?.id) {
        val pod = selected ?: return@LaunchedEffect
        loading = true
        episodes = PodNutzRepository.fetchEpisodes(pod.feedUrl)
        loading = false
    }

    fun clearSelection() {
        selected = null
        episodes = emptyList()
    }

    PlatformBackHandler(enabled = selected != null) {
        clearSelection()
    }

    fun playEpisode(pod: Podcast, ep: PodcastEpisode, queue: List<PodcastEpisode>) {
        val idx = queue.indexOfFirst { it.id == ep.id }.coerceAtLeast(0)
        val savedProgress = WatchProgressRepository.progressForVideo(videoId = ep.id, parentMetaId = pod.id)
            ?.takeIf { !it.isCompleted && it.lastPositionMs > 0L }
        onPlayChannel?.invoke(
            PlayerLaunch(
                profileId = 0,
                title = ep.title,
                sourceUrl = ep.audioUrl,
                sourceAudioUrl = ep.audioUrl,
                streamTitle = ep.title,
                streamSubtitle = "${pod.name} · ${pod.artist}",
                providerName = "PodNutz",
                parentMetaId = pod.id,
                parentMetaType = "audio",
                videoId = ep.id,
                initialPositionMs = savedProgress?.lastPositionMs ?: 0L,
                logo = pod.artwork,
                poster = pod.artwork,
                autoPlayQueueUrls = queue.map { it.audioUrl },
                autoPlayQueueTitles = queue.map { it.title },
                autoPlayQueueIndex = idx,
            ),
        )
    }

    val current = selected
    if (current != null) {
        PodcastDetailView(
            pod = current,
            episodes = episodes,
            loading = loading,
            progressState = progressState,
            isSaved = isSaved(current),
            onToggleSave = { toggleSave(current) },
            onBack = { clearSelection() },
            onPlay = { ep -> playEpisode(current, ep, episodes) },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PodBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🎙 PodNutz", color = PodAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text("Podcasts & Shows", color = PodTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { q ->
                searchText = q
                searchJob?.cancel()
                if (q.isNotBlank()) {
                    searchJob = scope.launch {
                        delay(400)
                        searching = true
                        searchResults = PodNutzRepository.searchPodcasts(q.trim())
                        searching = false
                    }
                } else {
                    searchResults = null
                }
            },
            placeholder = { Text("Search podcasts...", color = PodTertiary, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = PodTertiary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = ""; searchResults = null }) {
                        Icon(Icons.Filled.Clear, "Clear", tint = PodSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PodOnSurface,
                unfocusedTextColor = PodOnSurface,
                focusedBorderColor = PodBorder,
                unfocusedBorderColor = PodBorder,
                cursorColor = PodOnSurface,
                focusedContainerColor = PodSurfaceLow,
                unfocusedContainerColor = PodSurfaceLow,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            podGenreChips.forEach { genre ->
                PodChip(genre, genre == searchText) {
                    searchText = genre
                    searchJob?.cancel()
                    searchJob = scope.launch {
                        delay(200)
                        searching = true
                        searchResults = PodNutzRepository.searchPodcasts(genre)
                        searching = false
                    }
                }
            }
        }

        if (searchText.isBlank() && saved.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔖 Saved", color = PodOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("(${saved.size})", color = PodTertiary, fontSize = 12.sp)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(saved, key = { "saved_${it.id}" }) { pod ->
                        SavedPodCard(pod) { selected = pod }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val display = searchResults
            when {
                searching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PodAccent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Searching...", color = PodTertiary, fontSize = 13.sp)
                        }
                    }
                }
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PodAccent, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    }
                }
                display == null && featured.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No podcasts found", color = PodTertiary, fontSize = 14.sp)
                    }
                }
                else -> {
                    val list = display ?: featured
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No podcasts found", color = PodTertiary, fontSize = 14.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(list, key = { "pod_${it.id}" }) { pod ->
                                PodcastCard(
                                    pod = pod,
                                    isSaved = isSaved(pod),
                                    onToggleSave = { toggleSave(pod) },
                                ) {
                                    selected = pod
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastCard(
    pod: Podcast,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val ts = RoundedCornerShape(12.dp)
    Column(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ts).background(PodSurfaceLow)
                .then(if (focused) Modifier.border(2.dp, PodFocus, ts) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (!pod.artwork.isNullOrBlank()) {
                AsyncImage(model = pod.artwork, pod.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("🎙", color = PodAccent, fontSize = 44.sp)
            }
            if (focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(
                Icons.Filled.PlayArrow,
                "View",
                tint = Color.White.copy(alpha = if (focused) 1f else 0.7f),
                modifier = Modifier.size(if (focused) 44.dp else 36.dp).align(Alignment.Center)
                    .clip(CircleShape).background(Color.Black.copy(alpha = if (focused) 0.5f else 0.3f)).padding(8.dp),
            )
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(32.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (isSaved) "Remove from saved" else "Save",
                    tint = if (isSaved) PodAccent else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(pod.name, color = PodOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        if (pod.artist.isNotBlank()) Text(pod.artist, color = PodTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SavedPodCard(pod: Podcast, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val ts = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier.width(110.dp).focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.size(110.dp).clip(ts).background(PodSurfaceLow)
                .then(if (focused) Modifier.border(2.dp, PodFocus, ts) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (!pod.artwork.isNullOrBlank()) {
                AsyncImage(model = pod.artwork, pod.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("🎙", color = PodAccent, fontSize = 32.sp)
            }
        }
        Text(pod.name, color = PodOnSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PodcastDetailView(
    pod: Podcast,
    episodes: List<PodcastEpisode>,
    loading: Boolean,
    progressState: WatchProgressUiState,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onBack: () -> Unit,
    onPlay: (PodcastEpisode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(PodBg)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PodOnSurface) }
            Column(Modifier.weight(1f)) {
                Text(pod.name, color = PodOnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pod.artist, color = PodTertiary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (pod.genres.isNotEmpty()) {
                Text(pod.genres.take(3).joinToString(" · "), color = PodAccent, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            IconButton(onClick = onToggleSave) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (isSaved) "Remove from saved" else "Save podcast",
                    tint = if (isSaved) PodAccent else PodSecondary,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item(key = "__pod_header__") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Box(Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)).background(PodSurfaceLow), contentAlignment = Alignment.Center) {
                        if (!pod.artwork.isNullOrBlank()) {
                            AsyncImage(model = pod.artwork, pod.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text("🎙", color = PodAccent, fontSize = 48.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(pod.name, color = PodOnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(pod.artist, color = PodSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            item(key = "__ep_count__") {
                Text("Episodes (${episodes.size})", color = PodOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            if (loading) {
                item(key = "__loading__") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PodAccent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (episodes.isEmpty()) {
                item(key = "__empty__") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No episodes found", color = PodTertiary, fontSize = 14.sp)
                    }
                }
            } else {
                items(episodes, key = { "ep_${it.id}" }) { ep ->
                    val progress = progressState.progressForVideo(videoId = ep.id, parentMetaId = pod.id)
                    EpisodeRow(ep, progress) { onPlay(ep) }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(ep: PodcastEpisode, progress: WatchProgressEntry?, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val resumable = progress?.takeIf { !it.isCompleted && it.lastPositionMs > 0L }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) PodCard else Color.Transparent)
            .then(if (focused) Modifier.border(1.5.dp, PodFocus, RoundedCornerShape(8.dp)) else Modifier)
            .focusable().onFocusChanged { focused = it.isFocused }.clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, "Play", tint = if (focused) Color.White else PodSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(ep.title, color = PodOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = mutableListOf<String>()
            if (resumable != null) meta.add("▶ Resume ${formatPosition(resumable.lastPositionMs)}")
            if (ep.pubDate.isNotBlank()) meta.add(ep.pubDate)
            if (ep.duration.isNotBlank()) meta.add(ep.duration)
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(meta.joinToString(" · "), color = if (resumable != null) PodAccent else PodTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (progress != null && progress.progressFraction > 0f && progress.progressFraction < 1f) {
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(PodSurfaceLow)) {
                    Box(Modifier.fillMaxWidth(progress.progressFraction).height(3.dp).clip(RoundedCornerShape(2.dp)).background(PodAccent))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(if (resumable != null) "⏵" else "▶", color = if (resumable != null) PodAccent else PodAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatPosition(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "${m}:${s.toString().padStart(2, '0')}"
}

@Composable
private fun PodChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(when { isSelected -> Color.White; focused -> Color.White.copy(alpha = 0.15f); else -> Color.Transparent })
            .then(if (!isSelected && focused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick).focusable().onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (isSelected || focused) Color.Black else PodSecondary, fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
    }
}