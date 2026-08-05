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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
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
import kotlinx.coroutines.Job
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
private val AccentGreen @Composable get() = Color(0xFF22C55E)
private val SurfaceContainerLow @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
private val OutlineVariant @Composable get() = MaterialTheme.colorScheme.outlineVariant

private val tvMargin = 48.dp

// ── Page Navigation ──────────────────────────────────────────────────────
private enum class SportNutzPage { LIVE, EVENT, STANDINGS }

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

    var currentPage by remember { mutableStateOf(SportNutzPage.LIVE) }

    LaunchedEffect(Unit) {
        if (uiState.trendingNewsVideos.isEmpty()) {
            SportsRepository.refresh()
        }
        if (uiState.daddyLiveEvents.isEmpty()) {
            SportsRepository.loadDaddyLiveEvents()
        }
    }

    LaunchedEffect(uiState.selectedLeague) {
        val league = uiState.selectedLeague
        when (league?.id) {
            null -> {
                if (uiState.allLiveEvents.isEmpty()) {
                    SportsRepository.loadAllLiveEvents()
                }
            }
            else -> {
                SportsRepository.loadLeagueEvents(league, uiState.selectedDate.ifBlank { null })
                SportsRepository.startAutoRefresh(league)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { SportsRepository.stopAutoRefresh() }
    }

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

    val playDaddyLiveEvent: (DaddyLiveEvent) -> Unit = { dlEvent ->
        if (onPlayChannel != null) {
            val allChannels = IptvRepository.getAllChannels()
            val matched = dlEvent.channels.mapNotNull { ch ->
                allChannels.firstOrNull { ictv ->
                    ictv.name.lowercase().contains(ch.name.lowercase().trim()) ||
                    ch.name.lowercase().trim().contains(ictv.name.lowercase())
                }
            }.distinct()
            if (matched.isNotEmpty()) {
                if (matched.size == 1) {
                    launchChannel(matched.first(), allChannels, onPlayChannel)
                } else {
                    pickerChannels = matched
                    pickerTitle = dlEvent.eventName
                    showChannelPicker = true
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
                val launch = buildPlayerLaunch(channel, allChannels)
                onPlayChannel?.invoke(launch)
            },
        )
    }

    val goToLive: () -> Unit = { currentPage = SportNutzPage.LIVE }
    val goToEvent: () -> Unit = { currentPage = SportNutzPage.EVENT }
    val goToStandings: () -> Unit = { currentPage = SportNutzPage.STANDINGS }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(SurfaceBg)) {
        val isTvMode = maxWidth >= 1024.dp

        if (isTvMode) {
            SportsTvMode(
                uiState = uiState,
                scope = scope,
                onPlayChannel = onPlayChannel,
                onTeamClick = onTeamClick,
                onEventClick = onEventClick,
                playOrShowPicker = playOrShowPicker,
                playDaddyLiveEvent = playDaddyLiveEvent,
            )
        } else {
            // ── Mobile Layout ──
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentPage) {
                        SportNutzPage.LIVE ->                         Page1Live(
                            uiState = uiState,
                            scope = scope,
                            listState = rememberLazyListState(),
                            onPlayChannel = onPlayChannel,
                            onTeamClick = onTeamClick,
                            onEventClick = onEventClick,
                            playDaddyLiveEvent = playDaddyLiveEvent,
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
                // ── Category Chip Bar (mobile) ──
                CategoryChipsBar(
                    leagues = SportsRepository.leagues,
                    selectedLeague = uiState.selectedLeague,
                    onLeagueSelected = { SportsRepository.selectLeague(it) },
                )
            }
        }
    }
}

// ── TV Mode ──────────────────────────────────────────────────────────────

@Composable
private fun SportsTvMode(
    uiState: SportsUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onEventClick: (EspnProcessedEvent) -> Unit,
    playOrShowPicker: (EspnProcessedEvent) -> Unit,
    playDaddyLiveEvent: (DaddyLiveEvent) -> Unit,
) {
    val scrollState = rememberScrollState()
    var tvSearchJob by remember { mutableStateOf<Job?>(null) }
    val leagueChips = SportsRepository.leagues
    val liveEvents = uiState.events.filter { it.isLive }
    val upcomingEvents = uiState.events.filter { !it.isLive }

    Column(modifier = Modifier.fillMaxSize().background(SurfaceBg)) {
        // ── Scrollable Content (header + tabs + body scroll together) ──
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = tvMargin),
        ) {
            // ── TopAppBar ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SportNutz Hub", color = Primary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { q ->
                            tvSearchJob?.cancel()
                            tvSearchJob = scope.launch {
                                delay(400)
                                SportsRepository.searchSports(q)
                            }
                        },
                        placeholder = { Text("Search teams, leagues, players, or any sports topic", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { SportsRepository.clearSearch() }) {
                                    Icon(Icons.Filled.Close, "Clear", tint = OnSurfaceVariant)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedBorderColor = Primary, unfocusedBorderColor = OutlineVariant.copy(alpha = 0.5f),
                            cursorColor = Primary,
                            focusedContainerColor = SurfaceContainer,
                            unfocusedContainerColor = SurfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    )
                }
                Icon(Icons.Filled.Notifications, "Notifications", tint = OnSurface, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AccountCircle, "Account", tint = OnSurface, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── Category Chip Bar (TV) ──
            CategoryChipsBar(
                leagues = SportsRepository.leagues,
                selectedLeague = uiState.selectedLeague,
                onLeagueSelected = { SportsRepository.selectLeague(it) },
            )

            Spacer(Modifier.height(24.dp))

            // ── Live Events Grid ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Live Now", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.5.sp)
                Text("${liveEvents.size} ACTIVE STREAMS", color = OnSurfaceVariant, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(16.dp))

            val displayEvents = if (uiState.selectedLeague == null) uiState.allLiveEvents
                else liveEvents.take(6)

            if (displayEvents.isNotEmpty()) {
                // 3-column grid for live events
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    userScrollEnabled = false,
                ) {
                    items(displayEvents.take(6), key = { it.id }) { event ->
                        TvScoreCard(
                            event = event,
                            isLive = event.isLive,
                            onClick = { onEventClick(event) },
                            onTeamClick = onTeamClick,
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // ── Standings Bento + Highlights ──
            if (uiState.standings.isNotEmpty() || uiState.highlightVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Standings Bento (4-col)
                    if (uiState.standings.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1f)) {
                            TvStandingsBento(
                                standings = uiState.standings.take(5),
                                onTeamClick = onTeamClick,
                            )
                        }
                    }

                    // Trending Highlights (8-col)
                    if (uiState.highlightVideos.isNotEmpty()) {
                        Box(modifier = Modifier.weight(2f)) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Trending Highlights", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                                    Text("VIEW ALL", color = OnSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    uiState.highlightVideos.take(2).forEach { highlight ->
                                        Box(
                                            modifier = Modifier.weight(1f).aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceContainerHigh)
                                                .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    scope.launch {
                                                        val ytVideo = highlight.video
                                                        val result = com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(ytVideo.videoId)
                                                        if (result != null && onPlayChannel != null) {
                                                            onPlayChannel(PlayerLaunch(
                                                                profileId = 0, title = ytVideo.title, sourceUrl = result.url,
                                                                sourceHeaders = result.headers, streamTitle = ytVideo.title,
                                                                providerName = "YouTube", parentMetaId = "youtube",
                                                                parentMetaType = "youtube",
                                                                sourceAudioUrl = result.audioUrl, qualities = result.qualities,
                                                            ))
                                                        }
                                                    }
                                                },
                                        ) {
                                            if (highlight.video.thumbnail.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                                        .data(highlight.video.thumbnail).crossfade(true).build(),
                                                    contentDescription = highlight.video.title,
                                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                                                )
                                            }
                                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Filled.PlayArrow, "Play", tint = Primary, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                            if (highlight.video.durationSeconds > 0) {
                                                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                                                    val m = highlight.video.durationSeconds / 60
                                                    val s = highlight.video.durationSeconds % 60
                                                    Text("$m:${s.toString().padStart(2, '0')}", color = Primary, fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                            Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                                Column {
                                                    val eventTitle = uiState.events.find { it.id == highlight.eventId }?.title ?: highlight.video.title
                                                    Text(eventTitle, color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(highlight.video.channelName, color = OnSurfaceVariant, fontSize = 11.sp,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // ── Upcoming Today ──
            if (upcomingEvents.isNotEmpty()) {
                Text("Upcoming Today", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(upcomingEvents.take(8), key = { i, e -> "tv_up_${i}_${e.id}" }) { _, event ->
                        TvUpcomingCard(event = event, onClick = { onEventClick(event) })
                    }
                }
            }

            // ── Sync2Cal Upcoming Schedule ──
            val sync2CalAllEvents = uiState.sync2CalEventsByLeague.values.flatten().sortedBy { it.startTime }.take(20)
            if (sync2CalAllEvents.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Upcoming Schedule", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text("${sync2CalAllEvents.size} EVENTS", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(sync2CalAllEvents.take(12), key = { i, e -> "sync2cal_${i}_${e.id}" }) { _, event ->
                        Sync2CalEventCard(
                            event = event,
                            tvChannels = uiState.sync2CalTvChannels[event.id] ?: emptyList(),
                        )
                    }
                }
            }

            // ── DaddyLive Sports Now/Later (TV) ──
            if (uiState.daddyLiveEvents.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sports Now/Later", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(AccentOrange).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("DADDYLIVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).clickable { SportsRepository.loadDaddyLiveEvents() }.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Refresh, null, tint = OnSurface, modifier = Modifier.size(14.dp))
                            Text("REFRESH", color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.daddyLiveEvents.take(15).forEach { dlEvent ->
                        TvDaddyLiveCard(event = dlEvent, onClick = {
                            val allChannels = IptvRepository.getAllChannels()
                            val match = dlEvent.channels.firstNotNullOfOrNull { ch ->
                                allChannels.firstOrNull { ictv ->
                                    ictv.name.lowercase().contains(ch.name.lowercase().trim()) ||
                                    ch.name.lowercase().trim().contains(ictv.name.lowercase())
                                }
                            }
                            if (match != null && onPlayChannel != null) {
                                launchChannel(match, allChannels, onPlayChannel)
                            }
                        })
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun TvScoreCard(
    event: EspnProcessedEvent,
    isLive: Boolean,
    onClick: () -> Unit,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
) {
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWinning = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWinning = homeScore != null && awayScore != null && awayScore > homeScore

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, if (isLive) Primary.copy(alpha = 0.25f) else OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: time + live badge + league
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        event.timeStr ?: event.detail.ifBlank { event.status }.uppercase(),
                        color = if (isLive) Primary else OnSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    )
                    if (isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            LivePulseDotSmall()
                            Text("LIVE", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(9999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text("UPCOMING", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Text(event.league.uppercase().take(8), color = OnSurfaceVariant, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Team 1 row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.homeLogo.isNullOrBlank()) {
                        SportsAsyncImage(model = event.homeLogo, contentDescription = event.homeTeam, modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text(event.homeTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(event.homeTeam, color = if (homeWinning) OnSurface else OnSurface.copy(alpha = 0.85f),
                    fontSize = 16.sp, fontWeight = if (homeWinning) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(event.homeTeam, event.homeLogo, event.sport) } else Modifier))
                Text(event.homeScore ?: "-", color = if (homeWinning) AccentGreen else OnSurface.copy(alpha = 0.7f),
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(12.dp))

            // Team 2 row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.awayLogo.isNullOrBlank()) {
                        SportsAsyncImage(model = event.awayLogo, contentDescription = event.awayTeam, modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text(event.awayTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(event.awayTeam, color = if (awayWinning) OnSurface else OnSurface.copy(alpha = 0.85f),
                    fontSize = 16.sp, fontWeight = if (awayWinning) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(event.awayTeam, event.awayLogo, event.sport) } else Modifier))
                Text(event.awayScore ?: "-", color = if (awayWinning) AccentGreen else OnSurface.copy(alpha = 0.7f),
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }

            if (isLive) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Primary.copy(alpha = 0.15f)).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text("WATCH LIVE", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

@Composable
private fun TeamLogoSmall(logoUrl: String?, modifier: Modifier = Modifier.size(32.dp), contentDescription: String? = null) {
    Box(modifier.clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(logoUrl).crossfade(true).build(),
                contentDescription = contentDescription, modifier = Modifier.size(28.dp), contentScale = ContentScale.Fit,
            )
        } else {
            Text("?", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TvStandingsBento(
    standings: List<TeamStanding>,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text("NBA Standings", color = Primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("#", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp))
            Text("TEAM", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("W-L", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))

        standings.forEachIndexed { index, standing ->
            val isFirst = index == 0
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFirst) SurfaceContainerHigh else Color.Transparent)
                    .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(standing.teamName, standing.logo, standing.sport) } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    color = if (isFirst) Primary else OnSurfaceVariant,
                    fontSize = 11.sp, fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!standing.logo.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current).data(standing.logo).crossfade(true).build(),
                                contentDescription = null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 8.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(standing.teamName, color = OnSurface, fontSize = 12.sp,
                        fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(standing.record, color = if (isFirst) Primary else OnSurface,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (index < standings.size - 1) {
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.15f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun TvUpcomingCard(event: EspnProcessedEvent, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(240.dp).clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val timeLabel = (event.timeStr ?: "").ifBlank { "TBD" }
            Text(timeLabel, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.homeLogo.isNullOrBlank()) {
                        SportsAsyncImage(model = event.homeLogo, contentDescription = event.homeTeam, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text(event.homeTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("VS", color = OnSurface.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Box(Modifier.size(44.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                    if (!event.awayLogo.isNullOrBlank()) {
                        SportsAsyncImage(model = event.awayLogo, contentDescription = event.awayTeam, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text(event.awayTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(event.awayTeam.take(24), color = OnSurface, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Primary).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                Text("SET ALERT", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            }
        }
    }
}

@Composable
private fun TvDaddyLiveCard(event: DaddyLiveEvent, onClick: () -> Unit) {
    val isLive = event.isLive
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(0.5.dp, if (isLive) AccentOrange.copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isLive) AccentOrange.copy(alpha = 0.2f) else SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            Text(event.category.take(2).uppercase(), color = if (isLive) AccentOrange else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(event.eventName, color = Primary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isLive) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(ErrorRed).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White))
                            Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(event.localDate, color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("\u00b7", color = OnSurfaceVariant.copy(alpha = 0.4f))
                Text(event.localTime, color = OnSurfaceVariant, fontSize = 12.sp)
                Text("\u00b7", color = OnSurfaceVariant.copy(alpha = 0.4f))
                Text("${event.channels.size} channel${if (event.channels.size != 1) "s" else ""}", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isLive) AccentOrange else SurfaceContainerHigh).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(if (isLive) "\u25b6 WATCH" else "VIEW", color = if (isLive) Color.Black else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ── Helper to play a YouTube video ───────────────────────────────────────
private fun CoroutineScope.playYouTubeVideo(video: YouTubeVideo, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    launch {
        try {
            val result = com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(video.videoId)
            if (result != null && onPlayChannel != null) {
                val launch = PlayerLaunch(
                    profileId = 0, title = video.title, sourceUrl = result.url,
                    sourceHeaders = result.headers, streamTitle = video.title,
                    providerName = "YouTube", parentMetaId = "youtube",
                    parentMetaType = "youtube",
                    sourceAudioUrl = result.audioUrl, qualities = result.qualities,
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

// ── Page 1: Live (mobile) ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page1Live(
    uiState: SportsUiState,
    scope: kotlinx.coroutines.CoroutineScope,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    onEventClick: (EspnProcessedEvent) -> Unit,
    playDaddyLiveEvent: (DaddyLiveEvent) -> Unit,
    playOrShowPicker: (EspnProcessedEvent) -> Unit,
    onStandingsClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.allLiveLoading,
            onRefresh = { SportsRepository.refresh(); SportsRepository.loadAllLiveEvents() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val isAllLiveMode = uiState.selectedLeague == null

                // ── League pills bar (always open) ──
                item {
                    CategoryChipsBar(
                        leagues = SportsRepository.leagues,
                        selectedLeague = uiState.selectedLeague,
                        onLeagueSelected = { SportsRepository.selectLeague(it) },
                    )
                }

                // ── Standings & Refresh buttons ──
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
                                Text("\ud83c\udfc6", color = OnSurfaceVariant, fontSize = 13.sp)
                            }
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceContainerHigh)
                                .clickable { SportsRepository.refresh() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("\u21bb", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Search bar (live search with debounce) ──
                item {
                    var searchJob by remember { mutableStateOf<Job?>(null) }
                    SearchBar(
                        query = uiState.searchQuery,
                        isSearching = uiState.isSearching,
                        onSearch = { q ->
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                delay(400)
                                SportsRepository.searchSports(q)
                            }
                        },
                        onClear = { SportsRepository.clearSearch() },
                    )
                }

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

            // ── All Live Events or Specific League Content ──
            if (isAllLiveMode) {
                if (uiState.allLiveLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Scanning all leagues for live events...", color = OnSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (uiState.allLiveEvents.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No live events right now", color = OnSurfaceVariant)
                        }
                    }
                } else {
                    val grouped = uiState.allLiveEvents.groupBy { it.league.ifBlank { "Other Sports" } }
                    item {
                        var expandedLeagues by remember { mutableStateOf(emptySet<String>()) }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            grouped.forEach { (leagueName, leagueEvents) ->
                                val isExpanded = expandedLeagues.contains(leagueName)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainer)
                                        .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedLeagues = if (isExpanded) expandedLeagues - leagueName else expandedLeagues + leagueName
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(leagueName.uppercase(), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            ) {
                                                val liveCount = leagueEvents.count { it.isLive }
                            Text("${liveCount} LIVE", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(if (isExpanded) "▲" else "▼", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (isExpanded) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            if (isTablet) {
                                                leagueEvents.chunked(2).forEach { row ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        row.forEach { event ->
                                                            MatchCard(
                                                                modifier = Modifier.weight(1f),
                                                                event = event,
                                                                isPhone = false,
                                                                onClick = { onEventClick(event) },
                                                                onTeamClick = onTeamClick,
                                                            )
                                                        }
                                                        if (row.size < 2) Spacer(Modifier.weight(1f))
                                                    }
                                                }
                                            } else {
                                                leagueEvents.forEach { event ->
                                                    MatchCard(
                                                        event = event,
                                                        isPhone = true,
                                                        onClick = { onEventClick(event) },
                                                        onTeamClick = onTeamClick,
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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
                    val selectedSport = uiState.selectedLeague?.let { sportForLeague(it.id) }

                    item {
                        LiveScoresSection(
                            events = uiState.events,
                            selectedSport = selectedSport,
                            onEventClick = onEventClick,
                            onTeamClick = onTeamClick,
                            isTablet = isTablet,
                        )
                    }

                    item {
                        DaddyLiveSection(
                            events = uiState.daddyLiveEvents,
                            isLoading = uiState.daddyLiveLoading,
                            onRefresh = { SportsRepository.loadDaddyLiveEvents() },
                            onPlayEvent = playDaddyLiveEvent,
                        )
                    }

                    val s2cEvents = uiState.sync2CalEventsByLeague.values.flatten().sortedBy { it.startTime }.take(15)
                    if (s2cEvents.isNotEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SCHEDULE", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Text("${s2cEvents.size} EVENTS", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    itemsIndexed(s2cEvents.subList(0, minOf(10, s2cEvents.size)), key = { i, e -> "s2c_${i}_${e.id}" }) { _, event ->
                                        Sync2CalEventCard(
                                            event = event,
                                            tvChannels = uiState.sync2CalTvChannels[event.id] ?: emptyList(),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.standings.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Standings", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("VIEW ALL \u2192", color = Primary.copy(alpha = 0.7f), fontSize = 10.sp,
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
                }
            }

            // ── Trending news videos (always visible at bottom) ──
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
                Text("\ud83c\udfaf", fontSize = 40.sp)
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

    val isLive = event.isLive

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh).clickable(onClick = onBack).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\u2190", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                Text("\ud83c\udfc6", color = OnSurfaceVariant, fontSize = 13.sp)
            }
        }

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
                    val launch = buildPlayerLaunch(channel, allChannels)
                    onPlayChannel?.invoke(launch)
                },
                onPlayPlayerLaunch = onPlayChannel,
                onSetRegion = { SportsRepository.setRegionFilter(it) },
                onSetTab = { SportsRepository.setActiveEventTab(it) },
                onSearchVideos = { isFuture -> SportsRepository.searchSportVideos(event, isFuture) },
                onPlayHighlight = { ev ->
                    scope.launch {
                        val query = "${ev.awayTeam} vs ${ev.homeTeam} highlights"
                        val searchResults = com.nuvio.app.features.sports.YouTubeHighlightClient.searchHighlights(query)
                        searchResults.firstOrNull()?.let { ytVideo ->
                            val streamResult = com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(ytVideo.videoId)
                            streamResult?.let { sr ->
                                val launch = PlayerLaunch(
                                    profileId = 0, title = ytVideo.title, sourceUrl = sr.url,
                                    sourceHeaders = sr.headers, streamTitle = ytVideo.title,
                                    providerName = "YouTube", parentMetaId = "youtube",
                                    parentMetaType = "youtube",
                                    sourceAudioUrl = sr.audioUrl, qualities = sr.qualities,
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

// ── Page 3: Standings (mobile) ────────────────────────────────────────────
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

    LaunchedEffect(selectedSportId, selectedSeason) {
        val league = SportsRepository.leagues.find { it.id == selectedSportId }
        if (league != null) {
            SportsRepository.loadStandings(league, selectedSeason.takeIf { it > 0 })
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh)
                .clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("\u2190", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        val next = selectedSeason - 1
                        if (next >= 2020) selectedSeason = next
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("\u25c0", color = if (selectedSeason > 2020) Primary else OnSurfaceVariant.copy(alpha = 0.3f),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (selectedSeason <= 0 || selectedSeason >= currentYear) "$currentYear" else "$selectedSeason",
                        color = if (selectedSeason > 0 && selectedSeason < currentYear) Primary else OnSurface,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    )
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        if (selectedSeason <= 0) selectedSeason = currentYear - 1
                        else if (selectedSeason < currentYear) selectedSeason++
                    }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("\u25b6", color = if (selectedSeason < currentYear) Primary else OnSurfaceVariant.copy(alpha = 0.3f),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.error != null && uiState.standings.isEmpty() && selectedSportId != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u26A0\uFE0F", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error, color = AccentOrange, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable {
                        val league = SportsRepository.leagues.find { it.id == selectedSportId }
                        if (league != null) SportsRepository.loadStandings(league, selectedSeason)
                    }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Retry", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (uiState.isLoading && uiState.standings.isEmpty()) {
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
                    Text("\ud83c\udfc6", fontSize = 40.sp)
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
                                    Text(if (seasonLabel.isNotBlank()) "RANK #1 \u00b7 $seasonLabel" else "RANK #1",
                                        color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Text(top.teamName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text(top.record, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${filtered.size} TEAM${if (filtered.size != 1) "S" else ""} RANKED",
                            color = OnSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

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

// ── Standings Row (mobile) ────────────────────────────────────────────────
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
        if (isFirst) {
            Box(Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(Primary))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.width(6.dp))

        Text(
            text = rank.toString().padStart(2, '0'),
            color = if (isFirst) Primary else OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(22.dp),
        )

        Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            if (!standing.logo.isNullOrBlank()) {
                SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(standing.teamName, color = OnSurface, fontSize = 12.sp,
                fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = if (standing.location.isNotBlank()) "${standing.league.uppercase()} \u00b7 ${standing.location}"
                else standing.league.uppercase()
            Text(subtitle, color = OnSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(standing.record, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (standing.netRating.isNotBlank()) {
                val isPositive = standing.netRating.startsWith("+")
                Box(Modifier.clip(RoundedCornerShape(2.dp))
                    .background(if (isPositive) Primary.copy(alpha = 0.15f) else SurfaceContainerHigh)
                    .padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text(standing.netRating, color = if (isPositive) Primary else OnSurfaceVariant,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Search Bar (mobile) ────────────────────────────────────────────────────
@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf(query) }
    LaunchedEffect(query) { if (query != text) text = query }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            if (newText.isNotBlank()) {
                onSearch(newText.trim())
            } else {
                onClear()
            }
        },
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
            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
            cursorColor = Primary, focusedBorderColor = Primary.copy(alpha = 0.5f),
            unfocusedBorderColor = SurfaceContainerHighest, focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Search Results Section (mobile) ────────────────────────────────────────
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
        if (events.isNotEmpty()) {
            Text(text = "${events.size} event${if (events.size != 1) "s" else ""} found", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events, key = { it.id }) { event ->
                    ScoreCard(modifier = Modifier.width(280.dp), event = event, isLive = event.isLive, onClick = { onEventClick(event) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (videos.isNotEmpty()) {
            Text(text = "Video results", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                videos.take(6).forEach { video ->
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).clickable { onPlayVideo(video) }) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                                SportsAsyncImage(model = video.thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                    Text(text = "\u25b6", color = Color.White, fontSize = 16.sp)
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

// ── Live Games Section (mobile, redesigned) ───────────────────────────────
@Composable
private fun LiveScoresSection(
    events: List<EspnProcessedEvent>,
    selectedSport: String?,
    onEventClick: ((EspnProcessedEvent) -> Unit)?,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
    isTablet: Boolean = false,
) {
    val filtered = if (selectedSport != null) events.filter { it.sport == selectedSport } else events
    val liveEvents = filtered.filter { it.isLive }
    val upcomingEvents = filtered.filter { !it.isLive }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── Live Events ──
        if (liveEvents.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LIVE GAMES", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${liveEvents.size} ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
            if (isTablet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    liveEvents.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { event ->
                                MatchCard(
                                    modifier = Modifier.weight(1f),
                                    event = event,
                                    isPhone = false,
                                    onClick = { onEventClick?.invoke(event) ?: Unit },
                                    onTeamClick = onTeamClick,
                                )
                            }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    liveEvents.forEach { event ->
                        MatchCard(
                            event = event,
                            isPhone = true,
                            onClick = { onEventClick?.invoke(event) ?: Unit },
                            onTeamClick = onTeamClick,
                        )
                    }
                }
            }
        }

        // ── Upcoming Events ──
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
            if (isTablet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingEvents.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { event ->
                                MatchCard(
                                    modifier = Modifier.weight(1f),
                                    event = event,
                                    isPhone = false,
                                    onClick = { onEventClick?.invoke(event) ?: Unit },
                                    onTeamClick = onTeamClick,
                                )
                            }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcomingEvents.forEach { event ->
                        MatchCard(
                            event = event,
                            isPhone = true,
                            onClick = { onEventClick?.invoke(event) ?: Unit },
                            onTeamClick = onTeamClick,
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Text("No events available", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

// ── Sync2Cal Event Card (shared TV + mobile) ──────────────────────────────

@Composable
private fun Sync2CalEventCard(
    event: Sync2CalEvent,
    tvChannels: List<Sync2CalTvChannel>,
) {
    val dateStr = remember(event.startTime) {
        try {
            val parts = event.startTime.substringBefore("T").split("-")
            if (parts.size == 3) "${parts[1]}/${parts[2]}" else event.startTime.take(10)
        } catch (_: Exception) { event.startTime.take(10) }
    }
    val timeStr = remember(event.startTime) {
        try {
            val t = event.startTime.substringAfter("T").substringBefore("Z").substringBefore("+").substringBefore("-")
            val hms = t.split(":")
            if (hms.size >= 2) "${hms[0]}:${hms[1]}" else event.startTime.take(5)
        } catch (_: Exception) { event.startTime.take(5) }
    }

    Box(
        modifier = Modifier.width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(dateStr, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                if (event.leagueName.isNotBlank()) {
                    Text(event.leagueName, color = OnSurfaceVariant.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(timeStr, color = OnSurfaceVariant, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                event.title.removePrefix("\u26bd ").removePrefix("\ud83c\udfc0").removePrefix("\ud83c\udfc8").removePrefix("\u26be").removePrefix("\ud83c\udfd2").removePrefix("\ud83c\udfb3").removePrefix("\ud83c\udfaf").trim(),
                color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (event.location != null) {
                Spacer(Modifier.height(2.dp))
                Text(event.location, color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (tvChannels.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tvChannels.take(3).forEach { ch ->
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(PrimaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(ch.name, color = Primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (tvChannels.size > 3) {
                        Text("+${tvChannels.size - 3}", color = OnSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Upcoming Match Card (mobile) ─────────────────────────────────────────
@Composable
private fun UpcomingCard(event: EspnProcessedEvent, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            val timeLabel = if (event.timeStr.isNullOrBlank()) "TBD" else event.timeStr
            Text(timeLabel, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!event.awayLogo.isNullOrBlank()) {
                    AsyncImage(model = event.awayLogo, contentDescription = event.awayTeam, modifier = Modifier.size(16.dp), contentScale = ContentScale.Fit)
                }
                Text(event.awayTeam, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!event.homeLogo.isNullOrBlank()) {
                    AsyncImage(model = event.homeLogo, contentDescription = event.homeTeam, modifier = Modifier.size(16.dp), contentScale = ContentScale.Fit)
                }
                Text(event.homeTeam, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val leagueName = event.league.takeIf { it.isNotBlank() }
            if (leagueName != null) {
                Spacer(Modifier.height(4.dp))
                Text(leagueName, color = OnSurfaceVariant, fontSize = 9.sp, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DaddyLiveSection(
    events: List<DaddyLiveEvent>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onPlayEvent: (DaddyLiveEvent) -> Unit,
) {
    var collapsed by remember { mutableStateOf(true) }
    val live = events.filter { it.isLive }.take(10)
    val upcoming = events.filter { !it.isLive }.take(10)
    val totalCount = live.size + upcoming.size

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { collapsed = !collapsed }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SPORTS NOW/LATER", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(AccentOrange).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("DADDYLIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                if (!collapsed && totalCount > 0) {
                    Text("$totalCount", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceContainerHigh).clickable(onClick = onRefresh).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("\u21bb", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(if (collapsed) "\u25bc" else "\u25b2", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Fetching live events...", color = OnSurfaceVariant, fontSize = 12.sp)
                }
            }
        } else if (!collapsed && events.isEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No events — tap \u21bb to check", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        } else if (!collapsed) {
            if (live.isNotEmpty()) {
                Text("LIVE", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    live.forEach { dlEvent ->
                        DaddyLiveEventCard(event = dlEvent, isLive = true, onPlay = { onPlayEvent(dlEvent) })
                    }
                }
                if (upcoming.isNotEmpty()) Spacer(Modifier.height(12.dp))
            }
            if (upcoming.isNotEmpty()) {
                Text("UPCOMING", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    upcoming.forEach { dlEvent ->
                        DaddyLiveEventCard(event = dlEvent, isLive = false, onPlay = { onPlayEvent(dlEvent) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DaddyLiveEventCard(event: DaddyLiveEvent, isLive: Boolean, onPlay: () -> Unit) {
    val chCount = event.channels.size
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, if (isLive) ErrorRed.copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable(onClick = onPlay)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isLive) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ErrorRed))
                }
                Text(event.eventName, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 3.dp)) {
                Text(event.category, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("\u00b7", color = OnSurfaceVariant.copy(alpha = 0.4f))
                Text(if (isLive) event.localTime else "${event.localDate} ${event.localTime}", color = OnSurfaceVariant, fontSize = 10.sp)
                if (chCount > 0) {
                    Text("\u00b7", color = OnSurfaceVariant.copy(alpha = 0.4f))
                    Text("$chCount ch", color = AccentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (chCount > 0) {
                Text(
                    event.channels.take(5).joinToString(", ") { it.name } + if (chCount > 5) " +${chCount - 5} more" else "",
                    color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isLive) ErrorRed.copy(alpha = 0.2f) else SurfaceContainerHigh).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(if (isLive) "WATCH" else "VIEW", color = if (isLive) ErrorRed else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    return if (parts.isNotEmpty()) parts.joinToString(" \u00b7 ") else "Scheduled"
}

@Composable
private fun ScoreCard(
    modifier: Modifier = Modifier,
    event: EspnProcessedEvent, isLive: Boolean,
    onClick: () -> Unit,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
) {
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
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(4.dp).background(if (isLive) Primary else OutlineVariant.copy(alpha = 0.3f)))

            Column(Modifier.padding(12.dp)) {
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

                if (isFighting && hasEventImage) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        SportsAsyncImage(model = event.eventImage, contentDescription = event.title, Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                        Box(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
                            Text(event.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
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

                if (isLive) {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Primary.copy(alpha = 0.15f)).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                        Text("TAP TO WATCH", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ErrorRed.copy(alpha = 0.12f)).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                        Text("UPCOMING", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextScoreCard(
    modifier: Modifier = Modifier,
    event: EspnProcessedEvent,
    isLive: Boolean,
    onClick: () -> Unit,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
) {
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWinning = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWinning = homeScore != null && awayScore != null && awayScore > homeScore

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatEventDetail(event).uppercase(),
                    color = if (isLive) Primary else OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                )
                Text(
                    text = event.league.uppercase().take(6),
                    color = OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(event.homeTeam, event.homeLogo, event.sport) } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    if (!event.homeLogo.isNullOrBlank()) {
                        AsyncImage(model = event.homeLogo, contentDescription = event.homeTeam, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                    }
                    Text(
                        event.homeTeam,
                        color = OnSurface,
                        fontSize = 13.sp,
                        fontWeight = if (homeWinning) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    event.homeScore ?: "-",
                    color = if (homeWinning) Primary else OnSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = if (homeWinning) FontWeight.Bold else FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(event.awayTeam, event.awayLogo, event.sport) } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    if (!event.awayLogo.isNullOrBlank()) {
                        AsyncImage(model = event.awayLogo, contentDescription = event.awayTeam, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                    }
                    Text(
                        event.awayTeam,
                        color = OnSurface,
                        fontSize = 13.sp,
                        fontWeight = if (awayWinning) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    event.awayScore ?: "-",
                    color = if (awayWinning) Primary else OnSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = if (awayWinning) FontWeight.Bold else FontWeight.Medium,
                )
            }

            if (!event.channel.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    event.channel.uppercase(),
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isLive) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary.copy(alpha = 0.15f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("TAP TO WATCH", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}


@Composable
private fun TeamScoreCompact(logoUrl: String?, teamName: String, score: String?, isWinning: Boolean, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
            if (!logoUrl.isNullOrBlank()) SportsAsyncImage(model = logoUrl, contentDescription = teamName, Modifier.size(24.dp), ContentScale.Fit)
            else Text(teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
        }
        Spacer(Modifier.width(10.dp))
        val abbr = teamName.split(" ").lastOrNull()?.take(4)?.uppercase() ?: teamName.take(4).uppercase()
        Text(abbr, color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        Spacer(Modifier.weight(1f))
        Text(score ?: "-", color = if (isWinning) Primary else OnSurfaceVariant,
            fontSize = 22.sp, fontWeight = if (isWinning) FontWeight.Bold else FontWeight.Medium)
    }
}

// ── Standings Section (mobile) ───────────────────────────────────────────
@Composable
private fun StandingsBento(standings: List<TeamStanding>, selectedSport: String?, onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null) {
    val filtered = if (selectedSport != null) standings.filter { it.sport == selectedSport } else standings
    if (filtered.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("RK", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.width(24.dp))
            Text("TEAM", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
            Text("W-L", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        filtered.take(5).forEachIndexed { index, standing ->
            val isFirst = index == 0
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (isFirst) Modifier.background(SurfaceContainerHigh) else Modifier)
                    .padding(horizontal = 2.dp, vertical = 5.dp)
                    .then(if (onTeamClick != null) Modifier.clickable { onTeamClick(standing.teamName, standing.logo, standing.sport) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text((index + 1).toString().padStart(2, '0'), color = if (isFirst) Primary else OnSurfaceVariant,
                    fontSize = 10.sp, fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!standing.logo.isNullOrBlank()) SportsAsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(16.dp))
                        else Text(standing.teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 8.sp)
                    }
                    Text(standing.teamName, color = OnSurface, fontSize = 11.sp,
                        fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(standing.record, color = if (isFirst) Primary else OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── YouTube Highlights Section (mobile) ──────────────────────────────────
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
                                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(SurfaceContainerHigh)) {
                                    SportsAsyncImage(highlight.video.thumbnail, null, Modifier.fillMaxSize())
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                            Text("\u25b6", color = Color.White, fontSize = 18.sp)
                                        }
                                    }
                                    if (highlight.video.durationSeconds > 0) {
                                        Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            val m = highlight.video.durationSeconds / 60
                                            val s = highlight.video.durationSeconds % 60
                                            Text("$m:${s.toString().padStart(2, '0')}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Column(Modifier.padding(8.dp)) {
                                    Text(title, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val channelName = highlight.video.channelName.ifBlank { highlight.sport.uppercase() }
                                    Text(channelName, color = OnSurfaceVariant, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Trending News Videos Section (mobile) ────────────────────────────────
@Composable
private fun TrendingNewsVideosSection(
    videos: List<YouTubeVideo>,
    onPlayVideo: (YouTubeVideo) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Trending News", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 12.dp))
        val taken = videos.take(15)
        val rows = taken.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { video ->
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainer)
                                .clickable { onPlayVideo(video) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(SurfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SportsAsyncImage(model = video.thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                        Text(text = "\u25b6", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                Text(
                                    text = video.title,
                                    color = OnSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    if (rowItems.size < 3) {
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ── IPTV Playback Helpers ─────────────────────────────────────────────────
private fun buildPlayerLaunch(channel: IptvChannel, allChannels: List<IptvChannel>): PlayerLaunch {
    val channelIndex = allChannels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId }
    val history = IptvRepository.getHistoryChannels()
    return PlayerLaunch(
        profileId = 0, title = channel.name, sourceUrl = channel.url, streamTitle = channel.name,
        providerName = "Sports", parentMetaId = "iptv", parentMetaType = "tv", logo = channel.logo,
        channelNames = allChannels.map { it.name }, channelUrls = allChannels.map { it.url },
        channelLogos = allChannels.map { it.logo ?: "" }, channelIds = allChannels.map { it.id },
        currentChannelIndex = if (channelIndex >= 0) channelIndex else 0,
        historyChannelNames = history.map { it.name }, historyChannelUrls = history.map { it.url },
        historyChannelLogos = history.map { it.logo ?: "" }, historyChannelIds = history.map { it.id },
    )
}

private fun launchChannel(channel: IptvChannel, allChannels: List<IptvChannel>, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    val launch = buildPlayerLaunch(channel, allChannels)
    val id = PlayerLaunchStore.put(launch)
    PlayerLaunchStore.get(id)?.let { onPlayChannel?.invoke(it) }
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

// ── Category Chips Bar (Live, NFL, NBA, MLB, NHL, UFC, More...) ──────────
@Composable
private fun CategoryChipsBar(
    leagues: List<SportLeague>,
    selectedLeague: SportLeague?,
    onLeagueSelected: (SportLeague?) -> Unit,
) {
    val quickLeagues = listOf("nfl", "nba", "mlb", "nhl", "ufc", "bkfc", "mls")
    val quickList = leagues.filter { it.id.lowercase() in quickLeagues }
    val orderedQuick = quickLeagues.mapNotNull { abbr -> quickList.find { it.id.lowercase() == abbr } }
    var showMore by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // LIVE chip (default, selected when selectedLeague == null)
            val isLive = selectedLeague == null
            Box(
                modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                    .background(if (isLive) Primary else SurfaceContainer)
                    .border(if (!isLive) 0.5.dp else 0.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .clickable { onLeagueSelected(null) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (isLive) Color.Black else OnSurfaceVariant))
                    Text("LIVE",
                        color = if (isLive) Color.Black else OnSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }

            orderedQuick.forEach { league ->
                val isSelected = selectedLeague?.id == league.id
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) Primary else SurfaceContainer)
                        .border(if (!isSelected) 0.5.dp else 0.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                        .clickable { onLeagueSelected(if (isSelected) null else league) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        league.logoUrl?.let {
                            AsyncImage(model = it, contentDescription = league.abbreviation,
                                modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
                        }
                        Text(league.abbreviation.uppercase(),
                            color = if (isSelected) Color.Black else OnSurfaceVariant,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            // MORE chip
            Box(
                modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { showMore = !showMore }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (showMore) "\u25B2" else "\u25BC", fontSize = 9.sp, color = OnSurfaceVariant)
                    Text("MORE", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }

        // Expanded more leagues
        if (showMore) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val moreLeagues = leagues.filter { it.id.lowercase() !in quickLeagues }
                moreLeagues.forEach { league ->
                    val isSelected = selectedLeague?.id == league.id
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) Primary else SurfaceContainer)
                            .border(if (!isSelected) 0.5.dp else 0.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable { onLeagueSelected(if (isSelected) null else league) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            league.logoUrl?.let {
                                AsyncImage(model = it, contentDescription = league.abbreviation,
                                    modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
                            }
                            Text(league.abbreviation.uppercase(),
                                color = if (isSelected) Color.Black else OnSurfaceVariant,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Sport Event Detail Panel (with tabs) ─────────────────────────────────
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
        add(EventTab.LIVE)
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
                        if (tab == EventTab.HIGHLIGHTS || tab == EventTab.PRE_MATCH) onSearchVideos(tab == EventTab.PRE_MATCH)
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
                            VideoCardSmall(video = video, onClick = {
                                scope.launch {
                                    val result = com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(video.videoId)
                                    result?.let { sr ->
                                        val launch = PlayerLaunch(
                                            profileId = 0, title = video.title, sourceUrl = sr.url,
                                            sourceHeaders = sr.headers, streamTitle = video.title,
                                            providerName = "YouTube", parentMetaId = "youtube",
                                            parentMetaType = "youtube",
                                            sourceAudioUrl = sr.audioUrl, qualities = sr.qualities,
                                        )
                                        onPlayPlayerLaunch?.invoke(launch)
                                    }
                                }
                            })
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
    return "$m:${s.toString().padStart(2, '0')}"
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

// ── REDESIGN: Match Card (text-only, card-based layout) ─────────────────────
@Composable
private fun MatchCard(
    modifier: Modifier = Modifier,
    event: EspnProcessedEvent,
    isPhone: Boolean,
    onClick: () -> Unit,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
) {
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWinning = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWinning = homeScore != null && awayScore != null && awayScore > homeScore
    val dateLabel = event.rawDate?.take(10) ?: event.date.ifBlank { "" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, if (event.isLive) Primary.copy(alpha = 0.2f) else OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timeLabel = event.timeStr ?: event.detail.ifBlank { "TBD" }
                    Text(timeLabel, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (event.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            LivePulseDotSmall()
                            Text("LIVE", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("UPCOMING", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Text(event.league.uppercase().take(8), color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
            }
            if (dateLabel.isNotBlank() && !event.isLive) {
                Spacer(Modifier.height(2.dp))
                Text(dateLabel, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))
            Text(event.title, color = OnSurface, fontSize = if (isPhone) 15.sp else 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(event.homeTeam, color = if (homeWinning) OnSurface else OnSurface.copy(alpha = 0.85f), fontSize = if (isPhone) 13.sp else 14.sp, fontWeight = if (homeWinning) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (event.isLive) {
                        Text("${event.homeScore ?: "0"} - ${event.awayScore ?: "0"}", color = if (homeScore != null && awayScore != null) AccentGreen else OnSurface, fontSize = if (isPhone) 22.sp else 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                        val timeRemaining = event.detail.ifBlank { event.status }
                        Text(if (timeRemaining.contains("'")) timeRemaining else event.status.take(6).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    } else {
                        val isFinal = event.status.contains("FINAL", ignoreCase = true)
                        if (isFinal) {
                            Text("${event.homeScore ?: "0"} - ${event.awayScore ?: "0"}", color = OnSurface, fontSize = if (isPhone) 22.sp else 26.sp, fontWeight = FontWeight.ExtraBold)
                        } else {
                            Box(modifier = Modifier.background(SurfaceContainerHigh, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text("VS", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(event.awayTeam, color = if (awayWinning) OnSurface else OnSurface.copy(alpha = 0.85f), fontSize = if (isPhone) 13.sp else 14.sp, fontWeight = if (awayWinning) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun LivePulseDotSmall() {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulseSmall")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
            .clip(CircleShape)
            .background(ErrorRed),
    )
}

@Composable
private fun MatchOddsChip(label: String, odds: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(SurfaceContainerHigh, RoundedCornerShape(6.dp))
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = OnSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
        )
        Text(
            odds,
            color = Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Skeleton Loader ─────────────────────────────────────────────────────────
@Composable
private fun SportsSkeletonLoader() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "shimmerAlpha",
    )

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) {
                Box(Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) {
                Box(Modifier.width(56.dp).height(26.dp).clip(RoundedCornerShape(13.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
            }
        }
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        }
        Text(" ", modifier = Modifier.height(20.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(Modifier.width(200.dp).height(140.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
            }
        }
        Text(" ", modifier = Modifier.height(20.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(Modifier.width(200.dp).height(140.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHighest.copy(alpha = shimmerAlpha)))
            }
        }
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
