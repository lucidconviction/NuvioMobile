package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
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
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFFFFFFF)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val TertiaryText = Color(0xFF888888)
private val BorderColor = Color(0xFF2A2A2A)
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
    var showRecentSearches by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var savedSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSavedMenu by remember { mutableStateOf(false) }
    var showManage by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    LaunchedEffect(Unit) {
        recentSearches = VidNutzRecentSearchesStore.load()
        savedSearches = VidNutzSavedSearchesStore.load()
        VidNutzPendingSearch.query?.let { q ->
            VidNutzPendingSearch.query = null
            val fromSportNutz = VidNutzPendingSearch.isFromSportNutz
            VidNutzPendingSearch.isFromSportNutz = false
            if (q.isNotBlank()) {
                if (fromSportNutz && !savedSearches.any { it.equals(q, ignoreCase = true) }) {
                    val updated = (listOf(q) + savedSearches).distinct().take(20)
                    savedSearches = updated
                    VidNutzSavedSearchesStore.save(updated)
                }
                uiState = uiState.copy(searchQuery = q, searchResults = null, searchCurrentPage = 1, searchHasMore = true, isLoading = true)
                scope.launch {
                    val r = VidNutzRepository.search(q)
                    uiState = uiState.copy(searchResults = r, searchHasMore = r.isNotEmpty(), isLoading = false)
                }
            }
        }
    }

    fun addSavedSearch(q: String) {
        if (q.isBlank()) return
        val updated = (listOf(q) + savedSearches).distinct().take(20)
        savedSearches = updated
        VidNutzSavedSearchesStore.save(updated)
    }

    fun removeSavedSearch(q: String) {
        val updated = savedSearches.filterNot { it.equals(q, ignoreCase = true) }
        savedSearches = updated
        VidNutzSavedSearchesStore.save(updated)
    }

    fun saveSearch(query: String) {
        if (query.isBlank()) return
        val updated = (listOf(query) + recentSearches).distinct().take(10)
        recentSearches = updated
        VidNutzRecentSearchesStore.save(updated)
    }

    LaunchedEffect(uiState.selectedCategory) {
        if (uiState.searchQuery.isBlank()) {
            val cached = VidNutzRepository.getCachedCategory(uiState.selectedCategory)
            if (cached.isNotEmpty()) {
                uiState = uiState.copy(videos = cached, isLoading = false, hasMore = true)
            } else {
                uiState = uiState.copy(isLoading = true, currentPage = 1, hasMore = true)
            }
            scope.launch {
                val videos = VidNutzRepository.fetchByCategory(uiState.selectedCategory, page = 1)
                uiState = uiState.copy(videos = videos, isLoading = false, hasMore = videos.isNotEmpty())
                if (videos.isNotEmpty()) {
                    scope.launch { VidNutzRepository.fetchByCategory(uiState.selectedCategory, page = 2) }
                }
            }
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

    fun refreshHubInBackground(hub: VidNutzHub) {
        val subs = hub.subs
        if (subs.isEmpty()) return
        scope.launch {
            subs.forEach { sub ->
                try {
                    val fresh = VidNutzRepository.forceRefreshSubVideos(sub.id, sub.queries, 12)
                    if (fresh.isNotEmpty() && uiState.selectedHubId == hub.id) {
                        val cur = uiState
                        uiState = cur.copy(hubSections = cur.hubSections + (sub.id to fresh))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun selectHub(hub: VidNutzHub) {
        if (hub.id == uiState.selectedHubId && uiState.hubSections.isNotEmpty()) return
        val cached = VidNutzRepository.getCachedHub(hub)
        uiState = uiState.copy(
            selectedHubId = hub.id, videos = emptyList(), searchResults = null,
            searchQuery = "", hubSections = cached, hubLoading = cached.isEmpty(),
            currentPage = 1, hasMore = true,
            viewingSubId = null, subVideos = emptyList(),
        )
        if (cached.isEmpty()) {
            scope.launch {
                val sections = VidNutzRepository.getHubSections(hub)
                uiState = uiState.copy(hubSections = sections, hubLoading = false)
            }
        } else {
            // Content is cached — show immediately, refresh in background silently
            refreshHubInBackground(hub)
        }
        scope.launch {
            val hubs = VidNutzHubManager.menu
            val idx = hubs.indexOfFirst { it.id == hub.id }
            if (idx > 0) VidNutzRepository.prefetchHub(hubs[idx - 1])
            if (idx < hubs.size - 1) VidNutzRepository.prefetchHub(hubs[idx + 1])
        }
    }

    fun selectSubForView(sub: VidNutzSub) {
        scope.launch {
            val cached = VidNutzRepository.getSubVideos(sub.id, sub.queries, 28)
            uiState = uiState.copy(
                viewingSubId = sub.id, subVideos = cached, subHasMore = cached.isNotEmpty(), subPage = 1,
            )
            val fresh = VidNutzRepository.forceRefreshSubVideos(sub.id, sub.queries, 28)
            if (fresh.isNotEmpty()) {
                uiState = uiState.copy(subVideos = fresh, subHasMore = fresh.isNotEmpty())
            }
        }
    }

    fun loadMoreSubView() {
        if (!uiState.subHasMore) return
        val sub = uiState.viewingSubId ?: return
        val hub = VidNutzHubManager.byId(uiState.selectedHubId) ?: return
        val subObj = hub.subs.firstOrNull { it.id == sub } ?: return
        scope.launch {
            uiState = uiState.copy(isLoadingMore = true)
            val page = uiState.subPage + 1
            val more = VidNutzRepository.getMoreSubVideos(subObj.id, subObj.queries, page)
                .filter { v -> uiState.subVideos.none { it.videoId == v.videoId } }
            if (more.isNotEmpty()) {
                uiState = uiState.copy(
                    subVideos = uiState.subVideos + more, subPage = page, subHasMore = true, isLoadingMore = false,
                )
            } else {
                uiState = uiState.copy(subHasMore = false, isLoadingMore = false)
            }
        }
    }

    fun loadSubSection(sub: VidNutzSub) {
        if (sub.id in uiState.hubSections) return
        scope.launch {
            val v = VidNutzRepository.getSubVideos(sub.id, sub.queries, 12)
            val cur = uiState
            uiState = cur.copy(hubSections = cur.hubSections + (sub.id to v))
        }
    }

    fun loadMoreSub(sub: VidNutzSub) {
        val existing = uiState.hubSections[sub.id].orEmpty()
        if (existing.isEmpty()) return
        val page = (existing.size / 28) + 1
        scope.launch {
            val more = VidNutzRepository.getMoreSubVideos(sub.id, sub.queries, page)
                .filter { v -> existing.none { it.videoId == v.videoId } }
            val cur = uiState
            uiState = cur.copy(hubSections = cur.hubSections + (sub.id to (existing + more).distinctBy { it.videoId }))
        }
    }

    fun refreshSub(sub: VidNutzSub) {
        if (sub.id in uiState.refreshingSubIds) return
        uiState = uiState.copy(refreshingSubIds = uiState.refreshingSubIds + sub.id)
        scope.launch {
            try {
                val fresh = VidNutzRepository.forceRefreshSubVideos(sub.id, sub.queries, 12)
                val cur = uiState
                uiState = cur.copy(
                    hubSections = cur.hubSections + (sub.id to fresh),
                    refreshingSubIds = cur.refreshingSubIds - sub.id,
                )
            } catch (_: Exception) {
                uiState = uiState.copy(refreshingSubIds = uiState.refreshingSubIds - sub.id)
            }
        }
    }

    fun playVideoFromList(video: VidNutzVideo, list: List<VidNutzVideo>) {
        if (onPlayChannel == null) return
        if (uiState.resolvingVideoId != null) return
        uiState = uiState.copy(resolvingVideoId = video.videoId)
        if (uiState.searchQuery.isNotBlank()) saveSearch(uiState.searchQuery)
        VideoSelectionFeedback.start(video.title, "YouTube")
        scope.launch {
            try {
                val result = VidNutzRepository.resolveStream(video.videoId)
                if (result != null) {
                    val videoIdx = list.indexOfFirst { it.videoId == video.videoId }.coerceAtLeast(0)
                    val queueUrls = mutableListOf<String>()
                    val queueTitles = mutableListOf<String>()
                    for (v in list) {
                        if (v.videoId == video.videoId) {
                            queueUrls.add(result.url)
                            queueTitles.add(video.title)
                        } else {
                            val r = VidNutzRepository.resolveStream(v.videoId)
                            queueUrls.add(r?.url ?: "")
                            queueTitles.add(v.title)
                        }
                    }
                    onPlayChannel(PlayerLaunch(
                        profileId = 0, title = video.title, sourceUrl = result.url,
                        sourceHeaders = result.headers, streamTitle = video.title,
                        providerName = "YouTube",
                        parentMetaId = "youtube",
                        parentMetaType = "youtube",
                        autoPlayQueueUrls = queueUrls,
                        autoPlayQueueTitles = queueTitles,
                        autoPlayQueueIndex = videoIdx,
                    ))
                }
            } finally {
                VideoSelectionFeedback.stop()
                uiState = uiState.copy(resolvingVideoId = null)
            }
        }
    }

    fun swipeToCategory(direction: Int) {
        searchJob?.cancel()
        val hubs = VidNutzHubManager.menu
        val currentIdx = hubs.indexOfFirst { it.id == uiState.selectedHubId }.coerceAtLeast(0)
        val newIdx = when (direction) {
            1 -> if (currentIdx >= hubs.size - 1) 0 else currentIdx + 1
            -1 -> if (currentIdx <= 0) hubs.size - 1 else currentIdx - 1
            else -> currentIdx
        }
        selectHub(hubs[newIdx])
    }

    LaunchedEffect(Unit) {
        if (uiState.hubSections.isEmpty() && uiState.searchQuery.isBlank()) {
            selectHub(VidNutzHubManager.byId(uiState.selectedHubId) ?: VidNutzHubManager.menu.first())
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTvMode = maxWidth >= 1024.dp
        val isTablet = maxWidth >= 768.dp
        val margin = if (isTablet) 32.dp else 16.dp
        val columns = if (isTvMode) GridCells.Fixed(4) else if (isTablet) GridCells.Fixed(3) else GridCells.Fixed(2)

        LazyVerticalGrid(
            columns = columns,
            state = gridState,
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
            contentPadding = PaddingValues(horizontal = if (isTvMode) 48.dp else margin, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (isTvMode) 24.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 24.dp else 16.dp),
        ) {
            // ── Search bar (TV) ──
            if (isTvMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("VidNutz Hub", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(Modifier.width(32.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { q ->
                                uiState = uiState.copy(searchQuery = q, searchCurrentPage = 1, searchHasMore = true)
                                searchJob?.cancel()
                                showRecentSearches = q.isBlank()
                                if (q.isNotBlank()) {
                                    searchJob = scope.launch {
                                        delay(400)
                                        val results = VidNutzRepository.search(q)
                                        uiState = uiState.copy(searchResults = results, searchHasMore = results.isNotEmpty(), isLoading = false)
                                    }
                                } else {
                                    uiState = uiState.copy(searchResults = null)
                                }
                            },
                            placeholder = { Text("Search for technical content...", color = TertiaryText, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TertiaryText, modifier = Modifier.size(20.dp)) },
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
                                focusedBorderColor = OnSurfaceVariant, unfocusedBorderColor = Color.Transparent,
                                cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        SavedSearchMenu(
                            savedSearches = savedSearches,
                            currentQuery = uiState.searchQuery,
                            onRun = { t -> showSavedMenu = false; uiState = uiState.copy(searchQuery = t, searchResults = null, searchCurrentPage = 1, searchHasMore = true, isLoading = true); scope.launch { val r = VidNutzRepository.search(t); uiState = uiState.copy(searchResults = r, searchHasMore = r.isNotEmpty(), isLoading = false) } },
                            onSave = { addSavedSearch(it) },
                            onDelete = { removeSavedSearch(it) },
                        )
                        Spacer(Modifier.width(4.dp))
                        ManageButton(onClick = { showManage = true })
                    }
                }
            }

            // ── Search bar (Mobile) ──
            if (!isTvMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { q ->
                                uiState = uiState.copy(searchQuery = q, searchCurrentPage = 1, searchHasMore = true)
                                searchJob?.cancel()
                                showRecentSearches = q.isBlank()
                                if (q.isNotBlank()) {
                                    searchJob = scope.launch {
                                        delay(400)
                                        val results = VidNutzRepository.search(q)
                                        uiState = uiState.copy(searchResults = results, searchHasMore = results.isNotEmpty(), isLoading = false)
                                    }
                                } else {
                                    uiState = uiState.copy(searchResults = null)
                                }
                            },
                            placeholder = { Text("Search videos...", color = TertiaryText, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TertiaryText, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { uiState = uiState.copy(searchQuery = "", searchResults = null); showRecentSearches = false }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                                focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                                cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        SavedSearchMenu(
                            savedSearches = savedSearches,
                            currentQuery = uiState.searchQuery,
                            onRun = { t -> showSavedMenu = false; uiState = uiState.copy(searchQuery = t, searchResults = null, searchCurrentPage = 1, searchHasMore = true, isLoading = true); scope.launch { val r = VidNutzRepository.search(t); uiState = uiState.copy(searchResults = r, searchHasMore = r.isNotEmpty(), isLoading = false) } },
                            onSave = { addSavedSearch(it) },
                            onDelete = { removeSavedSearch(it) },
                        )
                        Spacer(Modifier.width(4.dp))
                        ManageButton(onClick = { showManage = true })
                    }
                }
            }

            // ── Hub chips ──
            item(span = { GridItemSpan(maxLineSpan) }) {
Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                            horizontalArrangement = if (isTablet) Arrangement.Center else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (uiState.viewingSubId != null) {
                                IconButton(onClick = { uiState = uiState.copy(viewingSubId = null, subVideos = emptyList()) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to hub", tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                val hub = VidNutzHubManager.byId(uiState.selectedHubId)
                                val sub = hub?.subs?.firstOrNull { it.id == uiState.viewingSubId }
                                Text(sub?.name ?: "View all", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.weight(1f))
                            } else {
                                VidNutzHubManager.menu.forEach { hub ->
                                    VidNutzChip(
                                        label = hub.displayName,
                                        isSelected = hub.id == uiState.selectedHubId,
                                        onClick = {
                                            searchJob?.cancel()
                                            selectHub(hub)
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = {
                                    val h = VidNutzHubManager.byId(uiState.selectedHubId) ?: VidNutzHubManager.menu.first()
                                    refreshHubInBackground(h)
                                }) {
                                    Icon(Icons.Filled.Refresh, "Refresh hub", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
            }

            // ── Recent Searches ──
            if (showRecentSearches && uiState.searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Recent Searches", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentSearches.forEach { search ->
                                Box(Modifier.clip(RoundedCornerShape(50)).background(SurfaceCard).clickable {
                                    showRecentSearches = false
                                    uiState = uiState.copy(searchQuery = search, searchCurrentPage = 1, searchHasMore = true, isLoading = true)
                                    scope.launch {
                                        val results = VidNutzRepository.search(search)
                                        uiState = uiState.copy(searchResults = results, searchHasMore = results.isNotEmpty(), isLoading = false)
                                    }
                                }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(search, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // ── Content ──
            val searching = uiState.searchQuery.isNotBlank() || uiState.searchResults != null
            if (searching) {
                val searchVids = uiState.searchResults ?: emptyList()
                if (uiState.isLoading && searchVids.isEmpty()) {
                    val skeletonCount = 6
                    items(skeletonCount) {
                        ShimmerVideoCard(isTvMode = isTvMode)
                    }
                } else if (searchVids.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No videos found", color = OnSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(searchVids, key = { it.videoId }) { video ->
                        val isResolving = uiState.resolvingVideoId == video.videoId
                        VidNutzVideoCard(
                            video = video,
                            isTvMode = isTvMode,
                            isLoading = isResolving,
                            onPlay = { playVideoFromList(video, searchVids) },
                        )
                    }
                    item(key = "__load_more__") {
                        val showLoading = uiState.isLoadingMore
                        val showButton = !uiState.isLoadingMore && uiState.searchHasMore && searchVids.isNotEmpty()
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showLoading) {
                                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else if (showButton) {
                                Button(
                                    onClick = { loadMore() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                ) {
                                    Text("LOAD MORE", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.SansSerif)
                                }
                            }
                        }
                    }
                }
            } else if (uiState.viewingSubId != null) {
                val sub = VidNutzHubManager.byId(uiState.selectedHubId)?.subs?.firstOrNull { it.id == uiState.viewingSubId }
                val subVids = uiState.subVideos
                if (subVids.isEmpty() && uiState.isLoading) {
                    val skeletonCount = 6
                    items(skeletonCount) {
                        ShimmerVideoCard(isTvMode = isTvMode)
                    }
                } else if (subVids.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No videos found", color = OnSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(subVids, key = { it.videoId }) { video ->
                        val isResolving = uiState.resolvingVideoId == video.videoId
                        VidNutzVideoCard(
                            video = video,
                            isTvMode = isTvMode,
                            isLoading = isResolving,
                            onPlay = { playVideoFromList(video, subVids) },
                        )
                    }
                    item(key = "__sub_load_more__") {
                        val showLoading = uiState.isLoadingMore
                        val showButton = !uiState.isLoadingMore && uiState.subHasMore && subVids.isNotEmpty()
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showLoading) {
                                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else if (showButton) {
                                Button(
                                    onClick = { loadMoreSubView() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                ) {
                                    Text("LOAD MORE", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.SansSerif)
                                }
                            }
                        }
                    }
                }
            } else {
                val hub = VidNutzHubManager.byId(uiState.selectedHubId) ?: VidNutzHubManager.menu.first()
                if (uiState.hubLoading && uiState.hubSections.isEmpty()) {
                    val skeletonCount = 6
                    items(skeletonCount) {
                        ShimmerVideoCard(isTvMode = isTvMode)
                    }
                } else {
                    hub.subs.forEach { sub ->
                        item(span = { GridItemSpan(maxLineSpan) }, key = sub.id) {
                            val videos = uiState.hubSections[sub.id]
                            if (videos == null) {
                                LaunchedEffect(sub.id) { loadSubSection(sub) }
                            }
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(sub.name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.width(4.dp))
                                    val isRefreshing = sub.id in uiState.refreshingSubIds
                                    if (isRefreshing) {
                                        CircularProgressIndicator(color = OnSurfaceVariant, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        IconButton(onClick = { refreshSub(sub) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Refresh, "Refresh", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { selectSubForView(sub) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.ArrowForward, "View all", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                when {
                                    videos == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = Color(0xFF4A90D9), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Loading...", color = OnSurfaceVariant, fontSize = 14.sp)
                                    }
                                    videos.isEmpty() -> Text("No videos found", color = OnSurfaceVariant, fontSize = 14.sp)
                                    else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        items(videos, key = { it.videoId }) { video ->
                                            VidRailCard(video = video, isTvMode = isTvMode, onPlay = { playVideoFromList(video, videos) })
                                        }
                                        item(key = "load_more_${sub.id}") {
                                            Button(
                                                onClick = { loadMoreSub(sub) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = OnSurface),
                                                modifier = Modifier.width(120.dp).height(72.dp),
                                            ) {
                                                Text("+ MORE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showManage) {
            VidNutzManageOverlay(
                onDismiss = { showManage = false },
                onCatalogChanged = {
                    VidNutzHubManager.reload()
                },
            )
        }
    }
}

@Composable
private fun ManageButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Edit, "Manage categories", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun VidNutzManageOverlay(
    onDismiss: () -> Unit,
    onCatalogChanged: () -> Unit,
) {
    var hubs by remember { mutableStateOf(VidNutzHubManager.menu) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showAddHub by remember { mutableStateOf(false) }
    var showAddSubFor by remember { mutableStateOf<String?>(null) }
    var showRenameHubId by remember { mutableStateOf<String?>(null) }
    var showRenameSub by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingDeleteHub by remember { mutableStateOf<String?>(null) }
    var pendingDeleteSub by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun apply(block: () -> Unit) {
        block()
        hubs = VidNutzHubManager.menu
        onCatalogChanged()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onDismiss() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
            }
            Text("Manage Categories", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { apply { VidNutzHubManager.resetToDefaults() } }) {
                Text("Reset", color = OnSurfaceVariant, fontSize = 13.sp)
            }
        }
        Text(
            "Add, delete, or reorder categories and sub-categories. Staff Picks is locked.",
            color = TertiaryText, fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(hubs, key = { it.id }) { hub ->
                val expanded = expandedId == hub.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .clickable { expandedId = if (expanded) null else hub.id }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            hub.displayName,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showAddSubFor = hub.id }) {
                            Icon(Icons.Filled.Add, "Add sub-category", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallIconButton(Icons.Filled.ArrowUpward) { apply { VidNutzHubManager.moveHubUp(hub.id) } }
                        SmallIconButton(Icons.Filled.ArrowDownward) { apply { VidNutzHubManager.moveHubDown(hub.id) } }
                        SmallIconButton(Icons.Filled.Edit) {
                            showRenameHubId = hub.id
                        }
                        SmallIconButton(Icons.Filled.Delete) {
                            pendingDeleteHub = hub.id
                        }
                    }

                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        hub.subs.forEach { sub ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            ) {
                                Text(
                                    sub.name,
                                    color = OnSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                SmallIconButton(Icons.Filled.ArrowUpward) { apply { VidNutzHubManager.moveSubUp(hub.id, sub.id) } }
                                SmallIconButton(Icons.Filled.ArrowDownward) { apply { VidNutzHubManager.moveSubDown(hub.id, sub.id) } }
                                SmallIconButton(Icons.Filled.Edit) { showRenameSub = Pair(hub.id, sub.id) }
                                SmallIconButton(Icons.Filled.Delete) { pendingDeleteSub = hub.id to sub.id }
                            }
                        }
                        TextButton(onClick = { showAddSubFor = hub.id }) {
                            Icon(Icons.Filled.Add, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add sub-category", color = OnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { showAddHub = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    if (showAddHub) {
        val onSave: (String) -> Unit = { name ->
            showAddHub = false
            apply { VidNutzHubManager.addHub(name) }
        }
        NamePromptDialog(
            title = "Add Category",
            confirmLabel = "Add",
            onDismiss = { showAddHub = false },
            onConfirm = onSave,
        )
    }

    showAddSubFor?.let { hubId ->
        val onSave: (String) -> Unit = { subName ->
            showAddSubFor = null
            apply { VidNutzHubManager.addSub(hubId, subName) }
        }
        NamePromptDialog(
            title = "Add Sub-Category",
            confirmLabel = "Add",
            onDismiss = { showAddSubFor = null },
            onConfirm = onSave,
        )
    }

    showRenameHubId?.let { hubId ->
        val current = hubs.firstOrNull { it.id == hubId }?.displayName ?: ""
        val onSave: (String) -> Unit = { name ->
            showRenameHubId = null
            apply { VidNutzHubManager.renameHub(hubId, name) }
        }
        NamePromptDialog(
            title = "Rename Category",
            initial = current,
            confirmLabel = "Rename",
            onDismiss = { showRenameHubId = null },
            onConfirm = onSave,
        )
    }

    showRenameSub?.let { (hubId, subId) ->
        val hub = hubs.firstOrNull { it.id == hubId }
        val current = hub?.subs?.firstOrNull { it.id == subId }?.name ?: ""
        val onSave: (String) -> Unit = { name ->
            showRenameSub = null
            apply { VidNutzHubManager.renameSub(hubId, subId, name) }
        }
        NamePromptDialog(
            title = "Rename Sub-Category",
            initial = current,
            confirmLabel = "Rename",
            onDismiss = { showRenameSub = null },
            onConfirm = onSave,
        )
    }

    pendingDeleteHub?.let { hubId ->
        val hubName = hubs.firstOrNull { it.id == hubId }?.displayName ?: ""
        AlertDialog(
            containerColor = SurfaceCard,
            titleContentColor = OnSurface,
            textContentColor = OnSurface,
            onDismissRequest = { pendingDeleteHub = null },
            title = { Text("Delete Category?", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"$hubName\" and all of its sub-categories? This cannot be undone.", color = OnSurfaceVariant, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteHub = null
                    apply { VidNutzHubManager.deleteHub(hubId) }
                }) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteHub = null }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            },
        )
    }

    pendingDeleteSub?.let { (hubId, subId) ->
        val hub = hubs.firstOrNull { it.id == hubId }
        val subName = hub?.subs?.firstOrNull { it.id == subId }?.name ?: ""
        AlertDialog(
            containerColor = SurfaceCard,
            titleContentColor = OnSurface,
            textContentColor = OnSurface,
            onDismissRequest = { pendingDeleteSub = null },
            title = { Text("Delete Sub-Category?", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"$subName\"? This cannot be undone.", color = OnSurfaceVariant, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteSub = null
                    apply { VidNutzHubManager.deleteSub(hubId, subId) }
                }) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSub = null }) {
                    Text("Cancel", color = OnSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun SmallIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun NamePromptDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initial: String = "",
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        containerColor = SurfaceCard,
        titleContentColor = OnSurface,
        textContentColor = OnSurface,
        onDismissRequest = onDismiss,
        title = { Text(title, color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Name", color = TertiaryText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedBorderColor = OnSurfaceVariant, unfocusedBorderColor = BorderColor,
                    cursorColor = OnSurface, focusedContainerColor = ObsidianBg, unfocusedContainerColor = ObsidianBg,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun SavedSearchMenu(
    savedSearches: List<String>,
    currentQuery: String,
    onRun: (String) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Bookmark, "Saved searches", tint = if (savedSearches.isEmpty()) TertiaryText else Color(0xFF4A90D9))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (savedSearches.isEmpty()) {
                DropdownMenuItem(text = { Text("No saved searches", fontSize = 14.sp) }, onClick = { expanded = false })
            } else {
                savedSearches.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t, fontSize = 14.sp) },
                        onClick = { expanded = false; onRun(t) },
                        trailingIcon = {
                            Box(Modifier.clickable { onDelete(t) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Clear, "Delete", tint = TertiaryText, modifier = Modifier.size(16.dp))
                            }
                        },
                    )
                }
            }
            if (currentQuery.isNotBlank() && savedSearches.none { it.equals(currentQuery, ignoreCase = true) }) {
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(text = { Text("💾 Save \"$currentQuery\"", fontSize = 14.sp) }, onClick = { expanded = false; onSave(currentQuery) })
            }
        }
    }
}

@Composable
private fun VidNutzChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isSelected -> Color.White
        isFocused -> Color.White.copy(alpha = 0.15f)
        else -> SurfaceCard
    }
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
private fun VidNutzVideoCard(video: VidNutzVideo, isTvMode: Boolean, isLoading: Boolean = false, onPlay: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)

    if (isTvMode) {
        Column(
            modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onPlay),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(thumbShape).background(SurfaceCard)
                    .then(if (isFocused) Modifier.border(2.dp, FocusRing, thumbShape) else Modifier),
            ) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (video.isLive) {
                    Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF0000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isLoading) 0.7f else 0.6f)), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow, "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)).padding(12.dp),
                        )
                    }
                }
                if (video.durationSeconds > 0) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Text(
                video.title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp),
            )
            Text(
                video.channelName, color = OnSurfaceVariant, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp),
            )
            if (video.viewCount > 0 || video.uploadDate.isNotEmpty()) {
                Text(
                    buildList {
                        if (video.viewCount > 0) add(formatViews(video.viewCount))
                        if (video.uploadDate.isNotEmpty()) { if (isNotEmpty()) add("·"); add(video.uploadDate) }
                    }.joinToString(" "),
                    color = TertiaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp, start = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onPlay),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(thumbShape).background(SurfaceCard)
                    .then(if (isFocused) Modifier.border(2.dp, FocusRing, thumbShape) else Modifier),
            ) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (video.isLive) {
                    Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF0000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (isFocused) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                }
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp).align(Alignment.Center))
                } else {
                    Icon(
                        Icons.Filled.PlayArrow, "Play",
                        tint = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
                        modifier = Modifier.size(if (isFocused) 56.dp else 48.dp).align(Alignment.Center).clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (isFocused) 0.5f else 0.3f)).padding(12.dp),
                    )
                }
                if (video.durationSeconds > 0) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp, start = 2.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceCard),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        video.channelName.take(1).uppercase(),
                        color = OnSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        video.title, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        video.channelName, color = OnSurfaceVariant, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    if (video.viewCount > 0 || video.uploadDate.isNotEmpty()) {
                        Text(
                            buildList {
                                if (video.viewCount > 0) add(formatViews(video.viewCount))
                                if (video.uploadDate.isNotEmpty()) { if (isNotEmpty()) add("·"); add(video.uploadDate) }
                            }.joinToString(" "),
                            color = TertiaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VidRailCard(video: VidNutzVideo, isTvMode: Boolean, onPlay: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val railWidth = if (isTvMode) 300.dp else 240.dp
    Column(
        modifier = Modifier.width(railWidth).focusable().onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onPlay),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(SurfaceCard)
                .then(if (isFocused) Modifier.border(2.dp, FocusRing, RoundedCornerShape(12.dp)) else Modifier),
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (video.isLive) {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF0000)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (isFocused) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            if (video.durationSeconds > 0) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Text(
            video.title, color = OnSurface, fontSize = if (isTvMode) 15.sp else 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp),
        )
        Text(video.channelName, color = OnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp, start = 2.dp))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ShimmerVideoCard(isTvMode: Boolean) {
    val shimmerColor = Color(0xFF2A2A2A)
    if (isTvMode) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(shimmerColor))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(shimmerColor))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(shimmerColor))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor))
                }
            }
        }
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
