package com.nuvio.app.features.sports

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.player.SportsNowStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

// ── Design System Colors (sourced from app theme) ─────────────────────────
private val SurfaceBg @Composable get() = MaterialTheme.colorScheme.background
private val SurfaceContainer @Composable get() = MaterialTheme.colorScheme.surface
private val SurfaceContainerHigh @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val SurfaceContainerHighest @Composable get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
private val OnSurface @Composable get() = MaterialTheme.colorScheme.onSurface
private val OnSurfaceVariant @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Primary @Composable get() = MaterialTheme.colorScheme.primary
private val PrimaryContainer @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val Secondary @Composable get() = MaterialTheme.colorScheme.secondary
private val ErrorRed @Composable get() = MaterialTheme.colorScheme.error
private val AccentOrange @Composable get() = MaterialTheme.colorScheme.tertiary
private val SurfaceContainerLow @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
private val OutlineVariant @Composable get() = MaterialTheme.colorScheme.outlineVariant

// ── Page Navigation ──────────────────────────────────────────────────────
private enum class SportNutzPage { LIVE, EVENT, STANDINGS }

private val pageLabels = mapOf(
    SportNutzPage.LIVE to "Live",
    SportNutzPage.EVENT to "Event",
    SportNutzPage.STANDINGS to "Standings",
)

private val pageIcons = mapOf(
    SportNutzPage.LIVE to "⚡",
    SportNutzPage.EVENT to "🎯",
    SportNutzPage.STANDINGS to "🏆",
)

// ── Main Screen ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsScreen(
    modifier: Modifier = Modifier,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
) {
    val uiState by SportsRepository.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Page navigation state
    var currentPage by remember { mutableStateOf(SportNutzPage.LIVE) }

    // Load trending YouTube highlights on first open (fast)
    LaunchedEffect(Unit) {
        if (uiState.trendingNewsVideos.isEmpty()) {
            SportsRepository.refresh()
        }
    }

    // League selection effect
    LaunchedEffect(uiState.selectedLeague) {
        val league = uiState.selectedLeague
        when (league?.id) {
            "now" -> SportsRepository.loadAllLiveEvents()
            null -> {}
            else -> {
                SportsRepository.loadLeagueEvents(league, uiState.selectedDate.ifBlank { null })
                SportsRepository.startAutoRefresh(league)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { SportsRepository.stopAutoRefresh() }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }

    // Channel picker state
    var showChannelPicker by remember { mutableStateOf(false) }
    var pickerChannels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var pickerTitle by remember { mutableStateOf("") }

    val playOrShowPicker: (EspnProcessedEvent) -> Unit = { event ->
        if (onPlayChannel != null) {
            val sportEvents = EspnClient.toSportEvents(listOf(event))
            if (sportEvents.isNotEmpty()) {
                val se = sportEvents.first()
                val allChannels = IptvRepository.getAllChannels()
                val matched = EspnClient.findAllMatchingChannels(se, allChannels)
                if (matched.isNotEmpty()) {
                    if (matched.size == 1) {
                        launchChannel(matched.first(), allChannels, onPlayChannel)
                    } else {
                        pickerChannels = matched
                        pickerTitle = se.strEvent
                        showChannelPicker = true
                    }
                }
            }
        }
    }

    val onEventClick: (EspnProcessedEvent) -> Unit = { event ->
        SportsRepository.selectEvent(event)
        currentPage = SportNutzPage.EVENT
    }

    if (showChannelPicker) {
        ChannelPickerDialog(
            title = pickerTitle,
            channels = pickerChannels,
            onDismiss = { showChannelPicker = false },
            onSelect = { channel ->
                showChannelPicker = false
                val allChannels = IptvRepository.getAllChannels()
                launchChannel(channel, allChannels, onPlayChannel!!)
            },
        )
    }

    val goToLive: () -> Unit = { currentPage = SportNutzPage.LIVE }
    val goToEvent: () -> Unit = { currentPage = SportNutzPage.EVENT }
    val goToStandings: () -> Unit = { currentPage = SportNutzPage.STANDINGS }

    Column(modifier = modifier.fillMaxSize().background(SurfaceBg)) {
        // ── Content Area ──
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentPage) {
                SportNutzPage.LIVE -> Page1Live(
                    uiState = uiState,
                    scope = scope,
                    listState = listState,
                    onPlayChannel = onPlayChannel,
                    onTeamClick = onTeamClick,
                    onEventClick = onEventClick,
                    playOrShowPicker = playOrShowPicker,
                    onStandingsClick = goToStandings,
                )
                SportNutzPage.EVENT -> Page2Event(
                    uiState = uiState,
                    scope = scope,
                    onPlayChannel = onPlayChannel,
                    onTeamClick = onTeamClick,
                    onBack = goToLive,
                    onStandingsClick = goToStandings,
                )
                SportNutzPage.STANDINGS -> Page3Standings(
                    uiState = uiState,
                    onTeamClick = onTeamClick,
                    onBack = goToLive,
                    onEventClick = onEventClick,
                )
            }
        }

        // ── Bottom Navigation Pills ──
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceBg).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SportNutzPage.entries.forEach { page ->
                val isActive = currentPage == page
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isActive) PrimaryContainer else SurfaceContainerHigh)
                        .clickable { currentPage = page }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(pageIcons[page] ?: "", fontSize = 12.sp)
                        Text(
                            pageLabels[page] ?: "",
                            color = if (isActive) Color(0xFF00363A) else OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ── Helper to play a YouTube video ───────────────────────────────────────
private fun CoroutineScope.playYouTubeVideo(video: YouTubeVideo, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    launch {
        try {
            val result = YouTubeStreamResolver.resolveStream(video.videoId)
            if (result != null && onPlayChannel != null) {
                val launch = PlayerLaunch(
                    profileId = 0, title = video.title, sourceUrl = result.url,
                    sourceHeaders = result.headers, streamTitle = video.title,
                    providerName = "YouTube", parentMetaId = "youtube",
                    parentMetaType = "youtube",
                )
                onPlayChannel(launch)
            }
        } catch (_: Exception) { }
    }
}

private fun sportForLeague(leagueId: String): String? = when (leagueId) {
    "ufc", "boxing", "pfl" -> "Fighting"
    "nfl" -> "Football"
    "nba" -> "Basketball"
    "mlb" -> "Baseball"
    "nhl" -> "Hockey"
    "soccer" -> "Soccer"
    else -> null
}

// ── Page 1: Live ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page1Live(
    uiState: SportsUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onEventClick: (EspnProcessedEvent) -> Unit,
    playOrShowPicker: (EspnProcessedEvent) -> Unit,
    onStandingsClick: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { SportsRepository.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── "Sports Now" All Live Events Grid ──
        if (uiState.selectedLeague?.id == "now") {
            if (uiState.allLiveLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning all leagues for live events...", color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else if (uiState.allLiveEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No live events right now", color = OnSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                ) {
                    items(uiState.allLiveEvents, key = { it.id }) { event ->
                        ScoreCard(event = event, isLive = true, onClick = { onEventClick(event) },
                            onTeamClick = onTeamClick)
                    }
                }
            }
            return@PullToRefreshBox
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.isLoading) {
                item { SportsSkeletonLoader() }
            } else if (uiState.error != null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.error ?: "Unknown error", color = ErrorRed, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Tap to retry", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { SportsRepository.refresh() }
                                    .background(SurfaceContainerHigh).padding(horizontal = 24.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            } else {
                // ── League Chips ──
                item {
                    LeagueChipsRow(
                        leagues = SportsRepository.leagues,
                        selectedLeague = uiState.selectedLeague,
                        onLeagueSelected = { SportsRepository.selectLeague(it) },
                    )
                }

                // ── Standings link + Refresh (date nav removed) ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceContainerHigh)
                                .clickable(onClick = onStandingsClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("🏆", color = OnSurfaceVariant, fontSize = 13.sp)
                            }
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceContainerHigh)
                                .clickable { SportsRepository.refresh() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("↻", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Search Bar ──
                item {
                    SearchBar(
                        query = uiState.searchQuery,
                        isSearching = uiState.isSearching,
                        onSearch = { SportsRepository.searchSports(it) },
                        onClear = { SportsRepository.clearSearch() },
                    )
                }

                // ── Search Results ──
                if (uiState.searchQuery.isNotBlank()) {
                    item {
                        SearchResultsSection(
                            videos = uiState.searchResults,
                            events = uiState.searchedEvents,
                            isSearching = uiState.isSearching,
                            onPlayVideo = { v -> scope.playYouTubeVideo(v, onPlayChannel) },
                            onEventClick = playOrShowPicker,
                        )
                    }
                }

                // ── Live / Upcoming Scores ──
                val selectedSport = uiState.selectedLeague?.let { sportForLeague(it.id) }
                item {
                    LiveScoresSection(
                        events = uiState.events,
                        selectedSport = selectedSport,
                        onEventClick = onEventClick,
                        onTeamClick = onTeamClick,
                    )
                }

                // ── Standings Preview (bento) ──
                if (uiState.standings.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Standings", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif)
                                Text("VIEW ALL →", color = Primary.copy(alpha = 0.7f), fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onStandingsClick)
                                        .padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            StandingsBento(
                                standings = uiState.standings.take(4),
                                selectedSport = selectedSport,
                                onTeamClick = onTeamClick,
                            )
                        }
                    }
                }

                // ── Trending Highlights ──
                if (uiState.highlightVideos.isNotEmpty()) {
                    item {
                        HighlightVideosSection(
                            videos = uiState.highlightVideos,
                            events = uiState.events,
                            selectedSport = null,
                            onPlayVideo = { v -> scope.playYouTubeVideo(v, onPlayChannel) },
                        )
                    }
                }

                // ── Trending News Videos ──
                if (uiState.trendingNewsVideos.isNotEmpty()) {
                    item {
                        TrendingNewsVideosSection(
                            videos = uiState.trendingNewsVideos,
                            onPlayVideo = { v -> scope.playYouTubeVideo(v, onPlayChannel) },
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Page 2: Event Detail (all sports) ─────────────────────────────────────
@Composable
private fun Page2Event(
    uiState: SportsUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onBack: () -> Unit,
    onStandingsClick: () -> Unit,
) {
    val event = uiState.selectedEvent

    if (event == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎯", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text("Select an event from the Live page", color = OnSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryContainer).clickable(onClick = onBack).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Browse Events", color = Color(0xFF00363A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        return
    }

    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val isLive = event.isLive

    Column(Modifier.fillMaxSize()) {
        // ── Header: back + LIVE badge + event title + trophy ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh).clickable(onClick = onBack).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("←", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Live", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isLive) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Primary.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                LivePulseDot()
                                Text("LIVE", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(event.awayTeam + " vs " + event.homeTeam, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatEventDetail(event), color = OnSurfaceVariant, fontSize = 10.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh).clickable(onClick = onStandingsClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("🏆", color = OnSurfaceVariant, fontSize = 13.sp)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            // ── Reuse existing SportEventDetailPanel ──
            SportEventDetailPanel(
                event = event,
                matchedChannels = uiState.matchedChannels,
                channelsLoading = uiState.channelsLoading,
                videos = uiState.sportEventVideos,
                videosLoading = uiState.sportVideosLoading,
                regionFilter = uiState.regionFilter,
                activeTab = uiState.activeEventTab,
                onBack = {
                    SportsRepository.clearSportSelection()
                    onBack()
                },
                onPlayChannel = { channel ->
                    val allChannels = IptvRepository.getAllChannels()
                    launchChannel(channel, allChannels, onPlayChannel!!)
                },
                onPlayPlayerLaunch = onPlayChannel,
                onSetRegion = { SportsRepository.setRegionFilter(it) },
                onSetTab = { SportsRepository.setActiveEventTab(it) },
                onSearchVideos = { isFuture -> SportsRepository.searchSportVideos(event, isFuture) },
                onPlayHighlight = { ev ->
                    scope.launch {
                        val query = "${ev.awayTeam} vs ${ev.homeTeam} highlights"
                        val searchResults = YouTubeHighlightClient.searchHighlights(query)
                        searchResults.firstOrNull()?.let { ytVideo ->
                            val streamResult = YouTubeStreamResolver.resolveStream(ytVideo.videoId)
                            streamResult?.let { sr ->
                                val launch = PlayerLaunch(
                                    profileId = 0, title = ytVideo.title, sourceUrl = sr.url,
                                    sourceHeaders = sr.headers, streamTitle = ytVideo.title,
                                    providerName = "YouTube", parentMetaId = "youtube",
                                    parentMetaType = "youtube",
                                )
                                onPlayChannel?.invoke(launch)
                            }
                        }
                    }
                },
            )
        }
    }
}

// ── Page 3: Standings ────────────────────────────────────────────────────
@Composable
private fun Page3Standings(
    uiState: SportsUiState,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onBack: () -> Unit,
    onEventClick: (EspnProcessedEvent) -> Unit,
) {
    var selectedSportId by remember { mutableStateOf<String?>(null) }
    val currentYear = 2026
    var selectedSeason by remember { mutableStateOf(uiState.selectedSeason) }

    // Auto-load standings when league selection changes
    LaunchedEffect(selectedSportId, selectedSeason) {
        val league = SportsRepository.leagues.find { it.id == selectedSportId }
        if (league != null) {
            SportsRepository.loadStandings(league, selectedSeason.takeIf { it > 0 })
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh)
                .clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("←", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Live", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Text("Standings", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Text("Standings & Rankings", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = -0.3.sp)
            }
        }

        // ── League chips + Season selector ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // League chips (scrollable)
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val allSports = listOf("All" to null) + SportsRepository.leagues.map { it.name to it.id }
                allSports.forEach { (name, id) ->
                    val isSelected = selectedSportId == id
                    Box(Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                        .clickable { selectedSportId = id }
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(name,
                            color = if (isSelected) Color.White else OnSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            // Season year selector
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Previous season
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        val next = selectedSeason - 1
                        if (next >= 2020) selectedSeason = next
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("◀", color = if (selectedSeason > 2020) Primary else OnSurfaceVariant.copy(alpha = 0.3f),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    // Current year label
                    Text(
                        if (selectedSeason <= 0 || selectedSeason >= currentYear) "$currentYear" else "$selectedSeason",
                        color = if (selectedSeason > 0 && selectedSeason < currentYear) Primary else OnSurface,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                    // Next season
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        if (selectedSeason <= 0) selectedSeason = currentYear - 1
                        else if (selectedSeason < currentYear) selectedSeason++
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("▶", color = if (selectedSeason < currentYear) Primary else OnSurfaceVariant.copy(alpha = 0.3f),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Content ──
        if (uiState.isLoading && uiState.standings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Loading standings...", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else if (uiState.standings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Select a league above to view standings", color = OnSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            val sorted = uiState.standings
                .filter { if (selectedSportId != null) it.league == selectedSportId else true }
                .sortedBy { it.rank.coerceAtLeast(1) }
            val filtered = sorted.ifEmpty {
                if (selectedSportId != null) emptyList() else sorted
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // ── Top rank summary card ──
                if (filtered.isNotEmpty()) {
                    item {
                        val top = filtered.first()
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLow)
                                .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(Primary))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    val seasonLabel = if (selectedSeason > 0 && selectedSeason < currentYear) "SEASON $selectedSeason" else ""
                                    Text(if (seasonLabel.isNotBlank()) "RANK #1 · $seasonLabel" else "RANK #1",
                                        color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Text(top.teamName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text(top.record, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // ── "X TEAMS RANKED" header ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${filtered.size} TEAM${if (filtered.size != 1) "S" else ""} RANKED",
                            color = OnSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                // ── Empty state for filtered-out leagues ──
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No standings for this league", color = OnSurfaceVariant, fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)
                                    .clickable { selectedSportId = null }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text("RESET FILTERS", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }
                }

                // ── Standings rows ──
                itemsIndexed(filtered, key = { _, s -> s.teamName + s.league }) { index, standing ->
                    StandingsRow(
                        standing = standing,
                        rank = index + 1,
                        isFirst = index == 0,
                        onClick = { onTeamClick?.invoke(standing.teamName, standing.logo, standing.sport) },
                    )
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}

// ── Standings Row (ranked, with net rating) ──────────────────────────────
@Composable
private fun StandingsRow(
    standing: TeamStanding,
    rank: Int,
    isFirst: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .then(
                if (isFirst) Modifier.border(
                    width = 0.5.dp, color = Primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                ) else Modifier
            )
            .padding(start = 2.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Left accent border for #1
        if (isFirst) {
            Box(Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(Primary))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.width(6.dp))

        // Rank
        Text(
            text = rank.toString().padStart(2, '0'),
            color = if (isFirst) Primary else OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(22.dp),
        )

        // Team logo
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (!standing.logo.isNullOrBlank()) {
                SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(8.dp))

        // Team name + league/location
        Column(modifier = Modifier.weight(1f)) {
            Text(
                standing.teamName,
                color = OnSurface,
                fontSize = 12.sp,
                fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (standing.location.isNotBlank()) "${standing.league.uppercase()} · ${standing.location}"
                else standing.league.uppercase()
            Text(subtitle, color = OnSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Record + net rating
        Column(horizontalAlignment = Alignment.End) {
            Text(standing.record, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (standing.netRating.isNotBlank()) {
                val isPositive = standing.netRating.startsWith("+")
                Box(
                    Modifier.clip(RoundedCornerShape(2.dp))
                        .background(if (isPositive) Primary.copy(alpha = 0.15f) else SurfaceContainerHigh)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        standing.netRating,
                        color = if (isPositive) Primary else OnSurfaceVariant,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ── Search Bar ─────────────────────────────────────────────────────────────
@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    var text by remember(query) { mutableStateOf(query) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Search teams, leagues, players, or any sports topic", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = OnSurfaceVariant) },
        trailingIcon = {
            if (text.isNotBlank()) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { text = ""; onClear() }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = OnSurfaceVariant)
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { if (text.isNotBlank()) onSearch(text.trim()) },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            cursorColor = Primary,
            focusedBorderColor = Primary.copy(alpha = 0.5f),
            unfocusedBorderColor = SurfaceContainerHighest,
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Search Results Section ─────────────────────────────────────────────────
@Composable
private fun SearchResultsSection(
    videos: List<YouTubeVideo>,
    events: List<EspnProcessedEvent>,
    isSearching: Boolean,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onEventClick: (EspnProcessedEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Search Results", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        if (isSearching) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
            }
            return
        }
        if (videos.isEmpty() && events.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(text = "No results found", color = OnSurfaceVariant, fontSize = 14.sp)
            }
            return
        }
        // Matched events
        if (events.isNotEmpty()) {
            Text(text = "${events.size} event${if (events.size != 1) "s" else ""} found", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events, key = { it.id }) { event ->
                    ScoreCard(event = event, isLive = event.isLive, onClick = { onEventClick(event) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        // YouTube video results
        if (videos.isNotEmpty()) {
            Text(text = "Video results", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                videos.take(6).forEach { video ->
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).clickable { onPlayVideo(video) }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
SportsAsyncImage(model = video.thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                    Text(text = "\u25B6", color = Color.White, fontSize = 16.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = video.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(text = video.channelName, color = OnSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Live Games Section (render-match) ─────────────────────────────────────
@Composable
private fun LiveScoresSection(events: List<EspnProcessedEvent>, selectedSport: String?, onEventClick: ((EspnProcessedEvent) -> Unit)?, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val filtered = if (selectedSport != null) events.filter { it.sport == selectedSport } else events
    val liveEvents = filtered.filter { it.isLive }
    val upcomingEvents = filtered.filter { !it.isLive }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Live Games ──
        if (liveEvents.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LIVE GAMES", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${liveEvents.size} ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                Text("VIEW ALL", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(liveEvents, key = { it.id }) { event ->
                    ScoreCard(event = event, isLive = true, onClick = { onEventClick?.invoke(event) }, onTeamClick = onTeamClick)
                }
            }
        }

        // ── Upcoming Matches ──
        if (upcomingEvents.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UPCOMING MATCHES", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${upcomingEvents.size} UPCOMING", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(upcomingEvents, key = { it.id }) { event ->
                    UpcomingCard(event = event, onClick = { onEventClick?.invoke(event) })
                }
            }
        }

        if (filtered.isEmpty()) {
            Text("No events available", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

// ── Upcoming Match Card (render-match: vs logos, time, Set Alert) ────────
@Composable
private fun UpcomingCard(event: EspnProcessedEvent, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Time
            val timeLabel = if (event.timeStr.isNullOrBlank()) "TBD" else event.timeStr
            Text(timeLabel, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))

            // Team logos vs
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.homeLogo.isNullOrBlank()) SportsAsyncImage(event.homeLogo, null, Modifier.size(32.dp), ContentScale.Fit)
                    else Text(event.homeTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
                }
                Text("VS", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.awayLogo.isNullOrBlank()) SportsAsyncImage(event.awayLogo, null, Modifier.size(32.dp), ContentScale.Fit)
                    else Text(event.awayTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Match name
            val matchName = event.title.ifBlank { "${event.awayTeam}" }
            Text(matchName, color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))

            // Set Alert button
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Primary).padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("SET ALERT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
        }
    }
}

@Composable
private fun LivePulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val alpha by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0.3f, animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "pulseAlpha")
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ErrorRed.copy(alpha = alpha)))
}

private fun formatEventDetail(event: EspnProcessedEvent): String {
    if (event.detail.isNotBlank()) return event.detail
    if (event.status.contains("FINAL")) return "Final"
    // Build from date + time when detail is missing
    val parts = mutableListOf<String>()
    if (event.date.length == 10) {
        val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val ym = event.date.split("-")
        if (ym.size >= 3) {
            val month = ym[1].toIntOrNull()?.let { months.getOrNull(it) } ?: ym[1]
            val day = ym[2].toIntOrNull()?.toString() ?: ym[2]
            parts.add("$month $day")
        }
    }
    if (!event.timeStr.isNullOrBlank()) parts.add(event.timeStr!!)
    return if (parts.isNotEmpty()) parts.joinToString(" · ") else "Scheduled"
}

@Composable
private fun ScoreCard(event: EspnProcessedEvent, isLive: Boolean, onClick: () -> Unit, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWinning = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWinning = homeScore != null && awayScore != null && awayScore > homeScore
    val leagueAbbr = when (event.league.uppercase()) {
        "NFL" -> "NFL"; "NBA" -> "NBA"; "MLB" -> "MLB"; "NHL" -> "NHL"
        "UFC" -> "UFC"; "BOXING" -> "BOX"; "PFL" -> "PFL"
        "SOCCER", "MLS" -> "MLS"
        else -> event.league.take(4).uppercase()
    }
    val isFighting = event.sport == "Fighting"
    val hasEventImage = !event.eventImage.isNullOrBlank()

    Box(
        modifier = Modifier.width(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            // Accent bar on top
            Box(Modifier.fillMaxWidth().height(4.dp).background(if (isLive) Primary else OutlineVariant.copy(alpha = 0.3f)))

            Column(Modifier.padding(12.dp)) {
                // Metadata row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatEventDetail(event).uppercase(),
                        color = if (isLive) Primary else OnSurfaceVariant,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (event.subEventCount != null) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHigh).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                Text("${event.subEventCount} FIGHTS", color = OnSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                        Text(leagueAbbr, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ── Fighting events: show event image + main event ──
                if (isFighting && hasEventImage) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        SportsAsyncImage(model = event.eventImage, contentDescription = event.title, Modifier.fillMaxSize())
                        // Gradient overlay for readability
                        Box(
                            Modifier.fillMaxSize()
                                .background(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                        )
                        // Event title overlay
                        Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                            Text(event.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    // ── Team rows (traditional sports) ──
                    TeamScoreCompact(
                        logoUrl = event.homeLogo, teamName = event.homeTeam,
                        score = event.homeScore, isWinning = homeWinning,
                        onClick = { onTeamClick?.invoke(event.homeTeam, event.homeLogo, event.sport) },
                    )
                    Spacer(Modifier.height(8.dp))
                    TeamScoreCompact(
                        logoUrl = event.awayLogo, teamName = event.awayTeam,
                        score = event.awayScore, isWinning = awayWinning,
                        onClick = { onTeamClick?.invoke(event.awayTeam, event.awayLogo, event.sport) },
                    )
                }

                // Tap hint for live
                if (isLive) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Primary.copy(alpha = 0.15f))
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("TAP TO WATCH", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamScoreCompact(logoUrl: String?, teamName: String, score: String?, isWinning: Boolean, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Team logo
        Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            if (!logoUrl.isNullOrBlank()) SportsAsyncImage(model = logoUrl, contentDescription = teamName, Modifier.size(24.dp), ContentScale.Fit)
            else Text(teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
        }
        Spacer(Modifier.width(10.dp))
        // Abbreviation
        val abbr = teamName.split(" ").lastOrNull()?.take(4)?.uppercase() ?: teamName.take(4).uppercase()
        Text(abbr, color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,)
        Spacer(Modifier.weight(1f))
        // Score
        Text(score ?: "-", color = if (isWinning) Primary else OnSurfaceVariant,
            fontSize = 22.sp, fontWeight = if (isWinning) FontWeight.Bold else FontWeight.Medium)
    }
}



// ── Standings Section ─────────────────────────────────────────────────────
@Composable
private fun StandingsBento(standings: List<TeamStanding>, selectedSport: String?, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val filtered = if (selectedSport != null) standings.filter { it.sport == selectedSport } else standings
    if (filtered.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(10.dp),
    ) {
        // Header row: RK | TEAM | W-L
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("RK", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                modifier = Modifier.width(24.dp))
            Text("TEAM", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f))
            Text("W-L", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        filtered.take(5).forEachIndexed { index, standing ->
            val isFirst = index == 0
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (isFirst) Modifier.background(SurfaceContainerHigh) else Modifier)
                    .padding(horizontal = 2.dp, vertical = 5.dp)
                    .then(
                        if (onTeamClick != null) Modifier.clickable { onTeamClick(standing.teamName, standing.logo, standing.sport) }
                        else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = if (isFirst) Primary else OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(24.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!standing.logo.isNullOrBlank()) SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(16.dp))
                        else Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 8.sp)
                    }
                    Text(standing.teamName, color = OnSurface, fontSize = 11.sp,
                        fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(standing.record, color = if (isFirst) Primary else OnSurface, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── YouTube Highlights Section (2-column grid, render-match) ──────────────
@Composable
private fun HighlightVideosSection(
    videos: List<HighlightVideo>,
    events: List<EspnProcessedEvent>,
    selectedSport: String?,
    onPlayVideo: (YouTubeVideo) -> Unit,
) {
    val eventMap = events.associateBy { it.id }
    val filtered = if (selectedSport != null) videos.filter { highlight ->
        eventMap[highlight.eventId]?.sport == selectedSport || highlight.sport == selectedSport
    } else videos
    val display = filtered.take(6)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TRENDING HIGHLIGHTS", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("VIEW ALL (${display.size})", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        // 2-column grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            display.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { highlight ->
                        val title = eventMap[highlight.eventId]?.title ?: highlight.video.title
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainer)
                                .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { onPlayVideo(highlight.video) },
                        ) {
                            Column {
                                // Thumbnail
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .background(SurfaceContainerHigh),
                                ) {
                                    SportsAsyncImage(highlight.video.thumbnail, null, Modifier.fillMaxSize())
                                    // Play overlay on hover
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                            Text("\u25B6", color = Color.White, fontSize = 18.sp)
                                        }
                                    }
                                    // Duration badge
                                    if (highlight.video.durationSeconds > 0) {
                                        Box(
                                            Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        ) {
                                            val m = highlight.video.durationSeconds / 60
                                            val s = highlight.video.durationSeconds % 60
                                            Text("${m}:${s.toString().padStart(2, '0')}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                // Info
                                Column(Modifier.padding(8.dp)) {
                                    Text(title, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val channelName = highlight.video.channelName.ifBlank { highlight.sport.uppercase() }
                                    Text(channelName, color = OnSurfaceVariant, fontSize = 9.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    // If odd count, fill remaining space
                    if (row.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Trending News Videos Section ──────────────────────────────────────────
@Composable
private fun TrendingNewsVideosSection(
    videos: List<YouTubeVideo>,
    onPlayVideo: (YouTubeVideo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Trending News", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            videos.take(4).forEach { video ->
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).clickable { onPlayVideo(video) }) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                            SportsAsyncImage(model = video.thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                Text(text = "\u25B6", color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = video.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(text = video.channelName, color = OnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── IPTV Playback Helpers ─────────────────────────────────────────────────
private fun launchChannel(channel: IptvChannel, allChannels: List<IptvChannel>, onPlayChannel: (PlayerLaunch) -> Unit) {
    val channelIndex = allChannels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId }
    val history = IptvRepository.getHistoryChannels()
    val launch = PlayerLaunch(
        profileId = 0, title = channel.name, sourceUrl = channel.url, streamTitle = channel.name,
        providerName = "Sports", parentMetaId = "iptv", parentMetaType = "tv", logo = channel.logo,
        channelNames = allChannels.map { it.name }, channelUrls = allChannels.map { it.url },
        channelLogos = allChannels.map { it.logo ?: "" }, channelIds = allChannels.map { it.id },
        currentChannelIndex = if (channelIndex >= 0) channelIndex else 0,
        historyChannelNames = history.map { it.name }, historyChannelUrls = history.map { it.url },
        historyChannelLogos = history.map { it.logo ?: "" }, historyChannelIds = history.map { it.id },
    )
    val id = PlayerLaunchStore.put(launch)
    PlayerLaunchStore.get(id)?.let { onPlayChannel(it) }
}

@Composable
private fun ChannelPickerDialog(title: String, channels: List<IptvChannel>, onDismiss: () -> Unit, onSelect: (IptvChannel) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Choose Channel", color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                channels.forEach { ch ->
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable { onSelect(ch) }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (ch.logo != null) SportsAsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Text(text = ch.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = { Text(text = "Cancel", color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDismiss() }.padding(horizontal = 16.dp, vertical = 10.dp)) },
        containerColor = SurfaceContainer,
        shape = RoundedCornerShape(20.dp),
    )
}

// ── League Chips Row (render-match) ─────────────────────────────────────────
@Composable
private fun LeagueChipsRow(
    leagues: List<SportLeague>,
    selectedLeague: SportLeague?,
    onLeagueSelected: (SportLeague?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "ALL LEAGUES" chip — always rendered, acts as "deselect all"
        val allSelected = selectedLeague == null
        Box(
            modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                .background(if (allSelected) Color.White else SurfaceContainer)
                .border(if (allSelected) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                .clickable { if (!allSelected) onLeagueSelected(null) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("★", color = if (allSelected) Color.Black else OnSurfaceVariant, fontSize = 11.sp)
                Text("ALL LEAGUES", color = if (allSelected) Color.Black else OnSurfaceVariant,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }

        leagues.forEach { league ->
            val isSelected = selectedLeague?.id == league.id
            Box(
                modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                    .background(if (isSelected) Primary else SurfaceContainer)
                    .border(if (isSelected) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .clickable { onLeagueSelected(if (isSelected) null else league) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(league.abbreviation.uppercase(),
                    color = if (isSelected) Color.White else OnSurfaceVariant,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ── Sport Event Detail Panel (with tabs) ─────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SportEventDetailPanel(
    event: EspnProcessedEvent,
    matchedChannels: List<MatchedChannel>,
    channelsLoading: Boolean,
    videos: List<SportEventVideo>,
    videosLoading: Boolean,
    regionFilter: String,
    activeTab: EventTab,
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayPlayerLaunch: ((PlayerLaunch) -> Unit)? = null,
    onSetRegion: (String) -> Unit,
    onSetTab: (EventTab) -> Unit,
    onSearchVideos: (Boolean) -> Unit,
    onPlayHighlight: (EspnProcessedEvent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isLive = event.isLive
    val tabs = buildList {
        add(EventTab.LIVE)         // always show — channels matter for upcoming events too
        add(EventTab.HIGHLIGHTS)
        if (!isLive) add(EventTab.PRE_MATCH)
    }

    LaunchedEffect(event) {
        onSetTab(tabs.first())
        if (!isLive) onSearchVideos(false)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Primary)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("${event.awayTeam} vs ${event.homeTeam}", color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                Text(formatEventDetail(event), color = OnSurfaceVariant, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val isSelected = tab == activeTab
                Box(modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                    .clickable {
                        onSetTab(tab)
                        if (tab == EventTab.HIGHLIGHTS || tab == EventTab.PRE_MATCH) {
                            onSearchVideos(tab == EventTab.PRE_MATCH)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(tab.label, color = if (isSelected) Color(0xFF00363A) else OnSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (activeTab) {
            EventTab.LIVE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("ALL" to "All", "US" to "US", "UK" to "UK", "CA" to "CA").forEach { (id, label) ->
                        val isSelected = regionFilter == id
                        Box(modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (isSelected) Primary else SurfaceContainerHigh)
                            .clickable { onSetRegion(id) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Text(label, color = if (isSelected) Color.White else OnSurfaceVariant,
                                fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val filtered = if (regionFilter == "ALL") matchedChannels
                else matchedChannels.filter { it.region == regionFilter || (it.region == "Other" && regionFilter == "US") }
                ChannelListContent(filtered, channelsLoading, event, onPlayChannel, onPlayHighlight)
            }
            EventTab.HIGHLIGHTS, EventTab.PRE_MATCH -> {
                if (videosLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (videos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No videos found", color = OnSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)
                                .clickable { onSearchVideos(activeTab == EventTab.PRE_MATCH) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Retry", color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(videos, key = { it.videoId }) { video ->
                            VideoCardSmall(
                                video = video,
                                onClick = {
                                    scope.launch {
                                        val result = YouTubeStreamResolver.resolveStream(video.videoId)
                                        result?.let { sr ->
                                            val launch = PlayerLaunch(
                                                profileId = 0, title = video.title, sourceUrl = sr.url,
                                                sourceHeaders = sr.headers, streamTitle = video.title,
                                                providerName = "YouTube", parentMetaId = "youtube",
                                                parentMetaType = "youtube",
                                            )
                                            onPlayPlayerLaunch?.invoke(launch)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelListContent(
    matchedChannels: List<MatchedChannel>,
    isLoading: Boolean,
    event: EspnProcessedEvent,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayHighlight: (EspnProcessedEvent) -> Unit,
) {
    if (isLoading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    } else if (matchedChannels.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No IPTV channels found for this game", color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable { onPlayHighlight(event) }
                .padding(horizontal = 20.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Highlights Instead", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matchedChannels) { matched ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)
                        .clickable { onPlayChannel(matched.channel) }.padding(12.dp),
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!matched.channel.logo.isNullOrBlank()) {
                            SportsAsyncImage(model = matched.channel.logo, contentDescription = null, modifier = Modifier.size(32.dp), contentScale = ContentScale.Fit)
                        } else {
                            Text(matched.channel.name.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(matched.channel.name, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                        Text(matched.sourceName, color = OnSurfaceVariant, fontSize = 11.sp)
                    }
                    Text(matched.matchType.label, color = Color(0xFF00363A), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(PrimaryContainer.copy(alpha = 0.9f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun VideoCardSmall(video: SportEventVideo, onClick: () -> Unit) {
    val ts = RoundedCornerShape(8.dp)
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(ts).background(SurfaceContainerHigh)) {
            SportsAsyncImage(model = video.thumbnailUrl, video.title, Modifier.fillMaxSize())
            if (video.durationSeconds > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(32.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).padding(6.dp))
        }
        Text(video.title, color = OnSurface, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}:${s.toString().padStart(2, '0')}"
}

@Composable
private fun SportsAsyncImage(model: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalPlatformContext.current
    if (model.isNullOrBlank()) {
        Box(modifier = modifier.background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            Text("?", color = OnSurfaceVariant.copy(alpha = 0.3f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Box(modifier = modifier) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

// ── Skeleton Loader ─────────────────────────────────────────────────────────
@Composable
private fun SportsSkeletonLoader() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Category chip row skeleton
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) {
                Box(
                    Modifier
                        .width(60.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHighest.copy(alpha = shimmerAlpha))
                )
            }
        }
        // Date pill row skeleton
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) {
                Box(
                    Modifier
                        .width(56.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(SurfaceContainerHighest.copy(alpha = shimmerAlpha))
                )
            }
        }
        // Score card skeletons
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHighest.copy(alpha = shimmerAlpha))
            )
        }
        // Section header + video card row
        Text(" ", modifier = Modifier.height(20.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .width(200.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerHighest.copy(alpha = shimmerAlpha))
                )
            }
        }
        // Another section header + card row
        Text(" ", modifier = Modifier.height(20.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .width(200.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerHighest.copy(alpha = shimmerAlpha))
                )
            }
        }
        // Standings skeleton
        Text(" ", modifier = Modifier.height(20.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        repeat(4) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
                Box(Modifier.width(40.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
            }
        }
    }
}
