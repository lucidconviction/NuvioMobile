package com.nuvio.app.features.sports

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
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

private val sportDisplayOrder = listOf(
    "Fighting" to "MMA",
    "Basketball" to "Basketball",
    "Football" to "Football",
    "Soccer" to "Soccer",
    "Baseball" to "Baseball",
    "Hockey" to "Hockey",
)

private fun displayLabel(sport: String): String =
    sportDisplayOrder.firstOrNull { it.first == sport }?.second ?: sport

private fun categorySortIndex(sport: String): Int {
    val idx = sportDisplayOrder.indexOfFirst { it.first == sport }
    return if (idx >= 0) idx else sportDisplayOrder.size
}

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

    LaunchedEffect(Unit) {
        SportsRepository.refresh()
        while (true) {
            delay(60_000L)
            SportsRepository.refresh()
        }
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

    Scaffold(
        modifier = modifier.fillMaxSize().background(SurfaceBg),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Sports Hub", color = Primary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.5).sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg.copy(alpha = 0.9f)),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { SportsRepository.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize().then(Modifier),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (uiState.isLoading) {
                    item { SportsSkeletonLoader() }
                } else if (uiState.error != null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = uiState.error ?: "Unknown error", color = ErrorRed, textAlign = TextAlign.Center, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Tap to retry", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { SportsRepository.refresh() }
                                    .background(SurfaceContainerHigh).padding(horizontal = 24.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            } else if (uiState.events.isEmpty() && uiState.news.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Sports content coming soon", color = OnSurfaceVariant, textAlign = TextAlign.Center, fontSize = 16.sp)
                    }
                }
            } else {
                // ── Category Chips ──
                item {
                    CategoryChipsRow(
                        events = uiState.events,
                        selectedSport = uiState.selectedSport,
                        onSportSelected = { SportsRepository.selectSport(it) },
                    )
                }

                // ── Date Navigation ──
                item {
                    DateNavigationRow(
                        selectedDate = uiState.selectedDate,
                        onDateSelected = { SportsRepository.selectDate(it) },
                    )
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

                // ── 1. Live & Upcoming Scores ──
                item {
                    LiveScoresSection(
                        events = uiState.events,
                        selectedSport = uiState.selectedSport,
                        onEventClick = playOrShowPicker,
                        onTeamClick = onTeamClick,
                    )
                }

                // ── 2. Standings (if available) ──
                if (uiState.standings.isNotEmpty()) {
                    item {
                        StandingsSection(
                            standings = uiState.standings,
                            selectedSport = uiState.selectedSport,
                            onTeamClick = onTeamClick,
                        )
                    }
                }

                // ── 3. YouTube Highlights (top of highlights) ──
                if (uiState.highlightVideos.isNotEmpty()) {
                    item {
                        HighlightVideosSection(
                            videos = uiState.highlightVideos,
                            events = uiState.events,
                            selectedSport = uiState.selectedSport,
                            onPlayVideo = { v ->
                                scope.launch {
                                    try {
                                        val result = YouTubeStreamResolver.resolveStream(v.videoId)
                                        if (result != null && onPlayChannel != null) {
                                            val launch = PlayerLaunch(
                                                profileId = 0,
                                                title = v.title,
                                                sourceUrl = result.url,
                                                sourceHeaders = result.headers,
                                                streamTitle = v.title,
                                                providerName = "YouTube",
                                                parentMetaId = "youtube",
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

                // ── 4. Trending News Videos ──
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
                                                profileId = 0,
                                                title = v.title,
                                                sourceUrl = result.url,
                                                sourceHeaders = result.headers,
                                                streamTitle = v.title,
                                                providerName = "YouTube",
                                                parentMetaId = "youtube",
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

                item { Spacer(Modifier.height(24.dp)) }
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
                                AsyncImage(model = video.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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

// ── Category Chips ────────────────────────────────────────────────────────
@Composable
private fun CategoryChipsRow(events: List<EspnProcessedEvent>, selectedSport: String?, onSportSelected: (String?) -> Unit) {
    val uniqueSports = events.map { it.sport }.distinct().sortedBy { categorySortIndex(it) }
    val displaySports = uniqueSports.map { displayLabel(it) to it }
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("All", selectedSport == null) { onSportSelected(null) }
        displaySports.forEach { (display, actual) ->
            Chip(display, selectedSport == actual) { onSportSelected(if (selectedSport == actual) null else actual) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) PrimaryContainer else SurfaceContainerHigh
    val textColor = if (selected) Color(0xFF00363A) else OnSurfaceVariant
    Box(modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(bgColor).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text = label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
            Text(text = "Live Now", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (liveEvents.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LivePulseDot()
                    Text(text = "${liveEvents.size} Active Game${if (liveEvents.size != 1) "s" else ""}", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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

@Composable
private fun ScoreCard(event: EspnProcessedEvent, isLive: Boolean, onClick: () -> Unit, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWinning = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWinning = homeScore != null && awayScore != null && awayScore > homeScore
    Box(modifier = Modifier.width(280.dp).clip(RoundedCornerShape(16.dp)).background(if (isLive) SurfaceContainer.copy(alpha = 0.85f) else SurfaceContainer.copy(alpha = 0.7f)).clickable(onClick = onClick)) {
        if (isLive) Box(modifier = Modifier.width(4.dp).height(140.dp).align(Alignment.CenterStart).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(Secondary))
        Column(modifier = Modifier.fillMaxWidth().padding(start = if (isLive) 16.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = event.detail.ifBlank { if (event.status.contains("FINAL")) "Final" else "Scheduled" }, color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.background(SurfaceContainerHighest, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
            }
            Spacer(Modifier.height(12.dp))
            TeamScoreRow(event.homeLogo, event.homeTeam, event.homeScore, homeWinning, onTeamClick = { onTeamClick?.invoke(event.homeTeam, event.homeLogo, event.sport) })
            Spacer(Modifier.height(10.dp))
            TeamScoreRow(event.awayLogo, event.awayTeam, event.awayScore, awayWinning, onTeamClick = { onTeamClick?.invoke(event.awayTeam, event.awayLogo, event.sport) })
            if (event.channel.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(text = event.channel, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun TeamScoreRow(logoUrl: String?, teamName: String, score: String?, isWinning: Boolean, onTeamClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerHighest), contentAlignment = Alignment.Center) {
                if (!logoUrl.isNullOrBlank()) AsyncImage(model = logoUrl, contentDescription = teamName, contentScale = ContentScale.Fit, modifier = Modifier.size(28.dp))
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
                                if (standing.logo != null) AsyncImage(model = standing.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp))
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
                            AsyncImage(model = highlight.video.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                            AsyncImage(model = video.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                            if (ch.logo != null) AsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
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
