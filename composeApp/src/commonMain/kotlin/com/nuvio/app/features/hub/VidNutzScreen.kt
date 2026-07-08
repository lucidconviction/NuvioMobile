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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun VidNutzScreen(
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
) {
    var uiState by remember { mutableStateOf(VidNutzUiState()) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var swipeAccumulator by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    fun swipeToCategory(direction: Int) {
        val entries = VidNutzCategory.entries
        val currentIdx = entries.indexOf(uiState.selectedCategory)
        val newIdx = when (direction) {
            1 -> if (currentIdx >= entries.size - 1) 0 else currentIdx + 1
            -1 -> if (currentIdx <= 0) entries.size - 1 else currentIdx - 1
            else -> currentIdx
        }
        uiState = uiState.copy(
            selectedCategory = entries[newIdx],
            videos = emptyList(),
            searchResults = null,
            searchQuery = "",
            currentPage = 1,
            hasMore = true,
        )
    }

    LaunchedEffect(uiState.selectedCategory) {
        if (uiState.searchQuery.isBlank()) {
            uiState = uiState.copy(isLoading = true, currentPage = 1, hasMore = true)
            val videos = VidNutzRepository.fetchByCategory(uiState.selectedCategory, page = 1)
            uiState = uiState.copy(videos = videos, isLoading = false, hasMore = videos.isNotEmpty())
        }
    }

    fun loadMore() {
        if (uiState.isLoadingMore || !uiState.hasMore) return
        val isSearching = uiState.searchQuery.isNotBlank()
        val nextPage = if (isSearching) uiState.searchCurrentPage + 1 else uiState.currentPage + 1
        scope.launch {
            uiState = uiState.copy(isLoadingMore = true)
            val more = if (isSearching) {
                VidNutzRepository.search(uiState.searchQuery, nextPage)
            } else {
                VidNutzRepository.fetchByCategory(uiState.selectedCategory, nextPage)
            }
            if (more.isNotEmpty()) {
                if (isSearching) {
                    uiState = uiState.copy(
                        searchResults = (uiState.searchResults ?: emptyList()) + more,
                        searchCurrentPage = nextPage,
                        searchHasMore = true,
                        isLoadingMore = false,
                    )
                } else {
                    uiState = uiState.copy(
                        videos = uiState.videos + more,
                        currentPage = nextPage,
                        hasMore = true,
                        isLoadingMore = false,
                    )
                }
            } else {
                uiState = uiState.copy(
                    isLoadingMore = false,
                    hasMore = false,
                    searchHasMore = false,
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ObsidianBg),
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { q ->
                uiState = uiState.copy(searchQuery = q, searchCurrentPage = 1, searchHasMore = true)
                searchJob?.cancel()
                if (q.isNotBlank()) {
                    searchJob = scope.launch {
                        delay(400)
                        val results = VidNutzRepository.search(q)
                        uiState = uiState.copy(searchResults = results, searchHasMore = results.isNotEmpty())
                    }
                } else {
                    uiState = uiState.copy(searchResults = null)
                }
            },
            placeholder = { Text("Search videos...", color = TertiaryText, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TertiaryText, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { uiState = uiState.copy(searchQuery = "", searchResults = null) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                cursorColor = OnSurface, focusedContainerColor = InputBg, unfocusedContainerColor = InputBg,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VidNutzCategory.entries.forEach { category ->
                CategoryChip(
                    label = category.displayName,
                    isSelected = category == uiState.selectedCategory,
                    onClick = {
                        uiState = uiState.copy(
                            selectedCategory = category, videos = emptyList(), searchResults = null,
                            searchQuery = "", currentPage = 1, hasMore = true,
                        )
                    },
                )
            }
        }

        val displayVideos = if (uiState.searchResults != null) uiState.searchResults!! else uiState.videos

        Box(
            modifier = Modifier.fillMaxSize().pointerInput(uiState.searchQuery.isBlank()) {
                if (uiState.searchQuery.isBlank()) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            swipeAccumulator += dragAmount
                            if (swipeAccumulator > swipeThresholdPx) { swipeToCategory(-1); swipeAccumulator = 0f }
                            else if (swipeAccumulator < -swipeThresholdPx) { swipeToCategory(1); swipeAccumulator = 0f }
                        },
                        onDragEnd = { swipeAccumulator = 0f },
                        onDragCancel = { swipeAccumulator = 0f },
                    )
                }
            },
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...", color = OnSurfaceVariant, fontSize = 14.sp) }
            } else if (displayVideos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No videos found", color = OnSurfaceVariant, fontSize = 14.sp) }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(displayVideos, key = { it.videoId }) { video ->
                        VideoCard(video = video, onPlay = {
                            scope.launch {
                                val result = VidNutzRepository.resolveStream(video.videoId)
                                if (result != null && onPlayChannel != null) {
                                    onPlayChannel(PlayerLaunch(
                                        profileId = 0, title = video.title, sourceUrl = result.url,
                                        sourceHeaders = result.headers, streamTitle = video.title,
                                        providerName = "YouTube", parentMetaId = "youtube", parentMetaType = "youtube",
                                    ))
                                }
                            }
                        })
                    }
                    item(key = "__load_more__") {
                        val showLoading = uiState.isLoadingMore
                        val showButton = !uiState.isLoadingMore && uiState.hasMore && displayVideos.isNotEmpty()
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            if (showLoading) {
                                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else if (showButton) {
                                Button(onClick = { loadMore() }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                                    Text("Load More", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when { isSelected -> Color.White; isFocused -> Color.White.copy(alpha = 0.15f); else -> Color.Transparent }
    val textColor = if (isSelected || isFocused) Color.Black else OnSurfaceVariant
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bgColor)
            .then(if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick).focusable().onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(label, color = textColor, fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable
private fun VideoCard(video: VidNutzVideo, onPlay: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { isFocused = it.isFocused }.clickable(onClick = onPlay),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(thumbShape).background(SurfaceLow)
            .then(if (isFocused) Modifier.border(2.dp, FocusRing, thumbShape) else Modifier)) {
            AsyncImage(model = video.thumbnail, contentDescription = video.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isFocused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
            Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
                modifier = Modifier.size(if (isFocused) 56.dp else 48.dp).align(Alignment.Center).clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isFocused) 0.5f else 0.3f)).padding(12.dp))
            if (video.durationSeconds > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
        Text(video.title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
        Text(video.channelName, color = TertiaryText, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
        if (video.viewCount > 0 || video.uploadDate.isNotEmpty()) {
            Text(buildString {
                if (video.viewCount > 0) append(formatViews(video.viewCount))
                if (video.uploadDate.isNotEmpty()) { if (isNotEmpty()) append(" · "); append(video.uploadDate) }
            }, color = TertiaryText, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1, modifier = Modifier.padding(top = 1.dp, start = 2.dp))
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) "${if (hours < 10) "0$hours" else "$hours"}:${if (minutes < 10) "0$minutes" else "$minutes"}:${if (secs < 10) "0$secs" else "$secs"}"
    else "${if (minutes < 10) "0$minutes" else "$minutes"}:${if (secs < 10) "0$secs" else "$secs"}"
}

private fun formatViews(count: Long): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M views"
    count >= 1_000 -> "${count / 1_000}K views"
    else -> "$count views"
}
