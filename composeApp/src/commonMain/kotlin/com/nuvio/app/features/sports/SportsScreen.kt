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

// ── Page Navigation ──────────────────────────────────────────────────────
private enum class SportNutzPage { LIVE, FIGHT_CENTER, STANDINGS }

private val pageLabels = mapOf(
    SportNutzPage.LIVE to "Live",
    SportNutzPage.FIGHT_CENTER to "Fight Center",
    SportNutzPage.STANDINGS to "Standings",
)

private val pageIcons = mapOf(
    SportNutzPage.LIVE to "⚡",
    SportNutzPage.FIGHT_CENTER to "🥊",
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
            "ppv" -> SportsRepository.loadPpvEvents()
            null -> {}
            else -> {
                SportsRepository.loadLeagueEvents(league)
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
        currentPage = SportNutzPage.FIGHT_CENTER
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
    val goToFightCenter: () -> Unit = { currentPage = SportNutzPage.FIGHT_CENTER }
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
                SportNutzPage.FIGHT_CENTER -> Page2FightCenter(
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

                // ── Date Nav + Standings link + Refresh ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DateNavigationRow(
                            selectedDate = uiState.selectedDate,
                            onDateSelected = { SportsRepository.selectDate(it) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceContainerHigh)
                                .clickable(onClick = onStandingsClick)
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("🏆 Standings", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            onPlayVideo = { v ->
                                scope.launch {
                                    try {
                                        val result = YouTubeStreamResolver.resolveStream(v.videoId)
                                        if (result != null && onPlayChannel != null) {
                                            val launch = PlayerLaunch(
                                                profileId = 0, title = v.title, sourceUrl = result.url,
                                                sourceHeaders = result.headers, streamTitle = v.title,
                                                providerName = "YouTube", parentMetaId = "youtube",
                                                parentMetaType = "youtube",
                                            )
                                            onPlayChannel(launch)
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                            onEventClick = playOrShowPicker,
                        )
                    }
                }

                // ── Live & Upcoming Scores ──
                item {
                    LiveScoresSection(
                        events = uiState.events,
                        selectedSport = uiState.selectedLeague?.let { league ->
                            when (league.id) {
                                "ufc", "boxing", "pfl", "ppv" -> "Fighting"
                                "nfl" -> "Football"
                                "nba" -> "Basketball"
                                "mlb" -> "Baseball"
                                "nhl" -> "Hockey"
                                "soccer" -> "Soccer"
                                else -> null
                            }
                        },
                        onEventClick = onEventClick,
                        onTeamClick = onTeamClick,
                    )
                }

                // ── Standings preview (compact) ──
                if (uiState.standings.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Standings", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("View All →", color = Primary.copy(alpha = 0.7f), fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onStandingsClick).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            StandingsSection(
                                standings = uiState.standings.take(4),
                                selectedSport = uiState.selectedLeague?.let { league ->
                                    when (league.id) {
                                        "ufc", "boxing", "pfl", "ppv" -> "Fighting"
                                        "nfl" -> "Football"
                                        "nba" -> "Basketball"
                                        "mlb" -> "Baseball"
                                        "nhl" -> "Hockey"
                                        "soccer" -> "Soccer"
                                        else -> null
                                    }
                                },
                                onTeamClick = onTeamClick,
                            )
                        }
                    }
                }

                // ── YouTube Highlights ──
                if (uiState.highlightVideos.isNotEmpty()) {
                    item {
                        HighlightVideosSection(
                            videos = uiState.highlightVideos,
                            events = uiState.events,
                            selectedSport = null,
                            onPlayVideo = { v ->
                                scope.launch {
                                    try {
                                        val result = YouTubeStreamResolver.resolveStream(v.videoId)
                                        if (result != null && onPlayChannel != null) {
                                            val launch = PlayerLaunch(
                                                profileId = 0, title = v.title, sourceUrl = result.url,
                                                sourceHeaders = result.headers, streamTitle = v.title,
                                                providerName = "YouTube", parentMetaId = "youtube",
                                                parentMetaType = "youtube",
                                            )
                                            onPlayChannel(launch)
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                        )
                    }
                }

                // ── Trending News Videos ──
                if (uiState.trendingNewsVideos.isNotEmpty()) {
                    item {
                        TrendingNewsVideosSection(
                            videos = uiState.trendingNewsVideos,
                            onPlayVideo = { v ->
                                scope.launch {
                                    try {
                                        val result = YouTubeStreamResolver.resolveStream(v.videoId)
                                        if (result != null && onPlayChannel != null) {
                                            val launch = PlayerLaunch(
                                                profileId = 0, title = v.title, sourceUrl = result.url,
                                                sourceHeaders = result.headers, streamTitle = v.title,
                                                providerName = "YouTube", parentMetaId = "youtube",
                                                parentMetaType = "youtube",
                                            )
                                            onPlayChannel(launch)
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                        )
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Page 2: Fight Center ──────────────────────────────────────────────────
@Composable
private fun Page2FightCenter(
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
                Text("🥊", fontSize = 40.sp)
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

    Column(Modifier.fillMaxSize()) {
        // ── Compact header with back + standings link ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("←", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Live", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.awayTeam + " vs " + event.homeTeam, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatEventDetail(event), color = OnSurfaceVariant, fontSize = 10.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh).clickable(onClick = onStandingsClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("🏆", color = OnSurfaceVariant, fontSize = 13.sp)
            }
        }

        // ── Event detail panel ──
        Box(Modifier.weight(1f).fillMaxWidth()) {
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
            Text("Standings & Rankings", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // ── League chips ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val allSports = listOf("All" to null) + SportsRepository.leagues.map { it.name to it.id }
            allSports.forEach { (name, id) ->
                val isSelected = selectedSportId == id
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                    .clickable { selectedSportId = id }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(name, color = if (isSelected) Color(0xFF00363A) else OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Content ──
        if (uiState.standings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No standings data yet", color = OnSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            val filtered = if (selectedSportId != null) {
                uiState.standings.filter { it.league == selectedSportId }
            } else uiState.standings

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("${filtered.size} team${if (filtered.size != 1) "s" else ""}", color = OnSurfaceVariant, fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp))
                }
                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No standings for this league", color = OnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
                items(filtered, key = { it.teamName + it.league }) { standing ->
                    StandingsRow(standing = standing, onTeamClick = onTeamClick, onEventClick = onEventClick)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

// ── Standings Row (compact) ──────────────────────────────────────────────
@Composable
private fun StandingsRow(
    standing: TeamStanding,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onEventClick: (EspnProcessedEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceContainer).padding(10.dp),
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            if (!standing.logo.isNullOrBlank()) {
                SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(28.dp))
            } else {
                Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(standing.teamName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(standing.record, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text(standing.sport.take(3).uppercase(), color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.background(PrimaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
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

// ── Date Navigation ───────────────────────────────────────────────────────
@Composable
private fun DateNavigationRow(selectedDate: String, onDateSelected: (String) -> Unit) {
    val today = SportsRepository.getToday()
    val dates = (-1..5).map { SportsRepository.addDays(today, it) }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEach { date ->
            val isSelected = date == selectedDate || (selectedDate.isBlank() && date == today)
            val bgColor = if (isSelected) PrimaryContainer else SurfaceContainerHigh
            val textColor = if (isSelected) Color(0xFF00363A) else OnSurfaceVariant
            Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(bgColor).padding(horizontal = 16.dp, vertical = 8.dp).clickable { onDateSelected(date) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = SportsRepository.dateLabel(date), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ── Live Scores Section ───────────────────────────────────────────────────
@Composable
private fun LiveScoresSection(events: List<EspnProcessedEvent>, selectedSport: String?, onEventClick: ((EspnProcessedEvent) -> Unit)?, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val filtered = if (selectedSport != null) events.filter { it.sport == selectedSport } else events
    val liveEvents = filtered.filter { it.isLive }
    val upcomingEvents = filtered.filter { !it.isLive }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Live / Upcoming", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (liveEvents.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LivePulseDot()
                        Text(text = "${liveEvents.size} Active Game${if (liveEvents.size != 1) "s" else ""}", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                if (upcomingEvents.isNotEmpty()) {
                    Text(text = "•", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text(text = "${upcomingEvents.size} Upcoming", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (filtered.isEmpty()) {
            Text(text = "No events available", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
            return
        }
        if (liveEvents.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(liveEvents, key = { it.id }) { event ->
                    ScoreCard(event = event, isLive = true, onClick = { onEventClick?.invoke(event) }, onTeamClick = onTeamClick)
                }
            }
        }
        if (upcomingEvents.isNotEmpty()) {
            if (liveEvents.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(text = "Upcoming", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp)) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(upcomingEvents, key = { it.id }) { event ->
                    ScoreCard(event = event, isLive = false, onClick = { onEventClick?.invoke(event) }, onTeamClick = onTeamClick)
                }
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
    Box(modifier = Modifier.width(280.dp).clip(RoundedCornerShape(16.dp)).background(if (isLive) SurfaceContainer.copy(alpha = 0.85f) else SurfaceContainer.copy(alpha = 0.7f)).clickable(onClick = onClick)) {
        if (isLive) Box(modifier = Modifier.width(4.dp).height(if (isLive) 180.dp else 140.dp).align(Alignment.CenterStart).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(Secondary))
        Column(modifier = Modifier.fillMaxWidth().padding(start = if (isLive) 16.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        Text("LIVE", color = Color(0xFF00FF00), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color(0xFF00FF00).copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(text = formatEventDetail(event), color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.background(SurfaceContainerHighest, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!event.eventImage.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                    SportsAsyncImage(model = event.eventImage, null, Modifier.fillMaxSize(), ContentScale.Crop)
                }
                Spacer(Modifier.height(10.dp))
            }
            TeamScoreRow(event.homeLogo, event.homeTeam, event.homeScore, homeWinning, onTeamClick = { onTeamClick?.invoke(event.homeTeam, event.homeLogo, event.sport) })
            Spacer(Modifier.height(10.dp))
            TeamScoreRow(event.awayLogo, event.awayTeam, event.awayScore, awayWinning, onTeamClick = { onTeamClick?.invoke(event.awayTeam, event.awayLogo, event.sport) })
            if (event.channel.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(text = event.channel, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (isLive) {
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(PrimaryContainer).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("CH", color = Color(0xFF00363A), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
private fun TeamScoreRow(logoUrl: String?, teamName: String, score: String?, isWinning: Boolean, onTeamClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                if (!logoUrl.isNullOrBlank()) SportsAsyncImage(model = logoUrl, contentDescription = teamName, modifier = Modifier.size(28.dp), contentScale = ContentScale.Fit)
                else Text(text = teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = teamName.split(" ").lastOrNull()?.take(4)?.uppercase() ?: teamName.take(4).uppercase(),
                color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = if (onTeamClick != null) Modifier.clickable(onClick = onTeamClick) else Modifier,
            )
        }
        Text(text = score ?: "-", color = if (isWinning) Secondary else OnSurface, fontWeight = if (isWinning) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 18.sp)
    }
}

// ── Standings Section ─────────────────────────────────────────────────────
@Composable
private fun StandingsSection(standings: List<TeamStanding>, selectedSport: String?, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val filtered = if (selectedSport != null) standings.filter { it.sport == selectedSport } else standings
    if (filtered.isEmpty()) return
    val grouped = filtered.groupBy { it.league }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Standings", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        grouped.forEach { (league, teams) ->
            Text(text = league, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                teams.take(8).forEach { standing ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                                if (standing.logo != null) SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit)
                                else Text(text = standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
                            }
                            Text(
                                text = standing.teamName, color = OnSurface, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = if (onTeamClick != null) Modifier.clickable { onTeamClick(standing.teamName, standing.logo, standing.sport) } else Modifier,
                            )
                        }
                        Text(text = standing.record, color = Secondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── YouTube Highlights Section ────────────────────────────────────────────
@Composable
private fun HighlightVideosSection(
    videos: List<HighlightVideo>,
    events: List<EspnProcessedEvent>,
    selectedSport: String?,
    onPlayVideo: (YouTubeVideo) -> Unit,
) {
    val eventMap = events.associateBy { it.id }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Video Highlights", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        val filtered = if (selectedSport != null) videos.filter { highlight ->
            eventMap[highlight.eventId]?.sport == selectedSport || highlight.sport == selectedSport
        } else videos
        val display = filtered.take(4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            display.forEach { highlight ->
                val title = eventMap[highlight.eventId]?.title ?: highlight.video.title
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).clickable { onPlayVideo(highlight.video) }) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Thumbnail with play overlay
                        Box(modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                            SportsAsyncImage(model = highlight.video.thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                Text(text = "\u25B6", color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(text = highlight.video.channelName, color = OnSurfaceVariant, fontSize = 11.sp)
                        }
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

// ── League Chips Row (TV-style) ──────────────────────────────────────────────
@Composable
private fun LeagueChipsRow(
    leagues: List<SportLeague>,
    selectedLeague: SportLeague?,
    onLeagueSelected: (SportLeague?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(leagues) { league ->
            val isSelected = selectedLeague?.id == league.id
            val bgColor = if (isSelected) PrimaryContainer else SurfaceContainerHigh
            val textColor = if (isSelected) Color(0xFF00363A) else OnSurfaceVariant
            Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(bgColor)
                .clickable {
                    onLeagueSelected(if (isSelected) null else league)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(text = league.name, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
