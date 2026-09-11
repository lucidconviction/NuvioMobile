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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.nuvio.app.features.ai.AiChannelSearchResult
import com.nuvio.app.features.ai.AiSettingsStore
import com.nuvio.app.features.ai.AiChannelSearchStore
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.hub.HubReturnStore
import com.nuvio.app.features.hub.VideoSelectionFeedback
import com.nuvio.app.features.hub.VidNutzPendingSearch
import com.nuvio.app.features.iptv.EspnNewsArticle
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
import kotlinx.coroutines.flow.first
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
        // Pre-cache sports events/channels: render cached content immediately,
        // then refresh in the background without blanking.
        SportsRepository.warmupSports()
        if (uiState.daddyLiveEvents.isEmpty()) {
            SportsRepository.loadDaddyLiveEvents()
        }
        if (uiState.bkfcEvents.isEmpty()) SportsRepository.loadBkfcEvents()
        if (uiState.boxingEvents.isEmpty()) SportsRepository.loadBoxingEvents()
        if (uiState.pflEvents.isEmpty()) SportsRepository.loadPflEvents()
        if (uiState.powerSlapEvents.isEmpty()) SportsRepository.loadPowerSlapEvents()
        SportsRepository.detectAndSetRegion()
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
    var pickerChannels by remember { mutableStateOf<List<ChannelScore>>(emptyList()) }
    var pickerTitle by remember { mutableStateOf("") }

    val playOrShowPicker: (EspnProcessedEvent) -> Unit = { event ->
        if (onPlayChannel != null) {
            val sportEvents = EspnClient.toSportEvents(listOf(event))
            if (sportEvents.isNotEmpty()) {
                val se = sportEvents.first()
                val allChannels = IptvRepository.getAllChannels()
                scope.launch {
                    val scored = EspnClient.findScoredMatchingChannelsWithLazyEpg(
                        se, allChannels, IptvRepository.buildCurrentEpgTitleLookup()
                    ).filterNot { com.nuvio.app.features.iptv.StreamValidationStore.isKnownDeadSync(it.channel.url) }
                    if (scored.isNotEmpty()) {
                        val autoplay = ChannelScorer.autoplayCandidate(scored)
                        if (autoplay != null) {
                            launchChannel(autoplay.channel, allChannels, onPlayChannel)
                        } else {
                            pickerChannels = scored
                            pickerTitle = se.strEvent
                            showChannelPicker = true
                        }
                    }
                }
            }
        }
    }

    val playDaddyLiveEvent: (DaddyLiveEvent) -> Unit = { dlEvent ->
        if (onPlayChannel != null) {
            val allChannels = IptvRepository.getAllChannels()
            val scored = dlEvent.channels.mapNotNull { ch ->
                val hit = allChannels.firstOrNull { ictv ->
                    ictv.name.lowercase().contains(ch.name.lowercase().trim()) ||
                    ch.name.lowercase().trim().contains(ictv.name.lowercase())
                } ?: return@mapNotNull null
                ChannelScore(hit, MatchType.TEAM, 100, listOf("direct match"))
            }.distinctBy { it.channel.id to it.channel.sourceId }
            if (scored.isNotEmpty()) {
                val autoplay = ChannelScorer.autoplayCandidate(scored)
                if (autoplay != null) {
                    launchChannel(autoplay.channel, allChannels, onPlayChannel)
                } else {
                    pickerChannels = scored
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

    PlatformBackHandler(enabled = currentPage != SportNutzPage.LIVE) {
        currentPage = SportNutzPage.LIVE
    }

    if (showChannelPicker) {
        ChannelPickerDialog(
            title = pickerTitle,
            channels = pickerChannels,
            onDismiss = { showChannelPicker = false },
            onSelect = { score ->
                showChannelPicker = false
                val allChannels = IptvRepository.getAllChannels()
                val launch = buildPlayerLaunch(score.channel, allChannels)
                onPlayChannel?.invoke(launch)
            },
        )
    }

    val goToLive: () -> Unit = { currentPage = SportNutzPage.LIVE }
    val goToEvent: () -> Unit = { currentPage = SportNutzPage.EVENT }
    val goToStandings: () -> Unit = { currentPage = SportNutzPage.STANDINGS }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
                // ── Category Chip Bar (top) ──
                CategoryChipsBar(
                    leagues = SportsRepository.leagues,
                    selectedLeague = uiState.selectedLeague,
                    onLeagueSelected = { SportsRepository.selectLeague(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.fillMaxWidth(),
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
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
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
                Text(leagueLabel(event.league).uppercase().take(10), color = OnSurfaceVariant, fontSize = 10.sp,
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
            Text(timeLabel, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
private fun CoroutineScope.playYouTubeVideo(video: YouTubeVideo, onPlayChannel: ((PlayerLaunch) -> Unit)?, queue: List<YouTubeVideo>? = null) {
    VideoSelectionFeedback.start(video.title, "YouTube")
    launch {
        try {
            val result = com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(video.videoId)
            if (result != null && onPlayChannel != null) {
                val queueUrls = queue?.map { v -> "yt://${v.videoId}" } ?: emptyList()
                val queueTitles = queue?.map { it.title } ?: emptyList()
                val currentIdx = queue?.indexOfFirst { it.videoId == video.videoId }?.coerceIn(0, queue.size - 1) ?: 0
                val launch = PlayerLaunch(
                    profileId = 0, title = video.title, sourceUrl = result.url,
                    sourceHeaders = result.headers, streamTitle = video.title,
                    providerName = "YouTube", parentMetaId = "youtube",
                    parentMetaType = "youtube",
                    sourceAudioUrl = result.audioUrl, qualities = result.qualities,
                    autoPlayQueueUrls = queueUrls, autoPlayQueueTitles = queueTitles,
                    autoPlayQueueIndex = currentIdx,
                )
                onPlayChannel(launch)
            }
        } catch (_: Exception) { }
        VideoSelectionFeedback.stop()
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

private fun BkfcEvent.toEspnProcessed(leagueAbbr: String): EspnProcessedEvent {
    val f1 = fighter1.ifBlank { mainEvent.substringBefore(" vs ").trim() }
    val f2 = fighter2.ifBlank { mainEvent.substringAfter(" vs ").trim() }
    val shortTitle = if (f1.isNotBlank() && f2.isNotBlank()) "$f1 vs $f2" else title
    return EspnProcessedEvent(
        id = "bkfc_$slug",
        title = shortTitle,
        homeTeam = f2, awayTeam = f1,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = "", status = "Scheduled", detail = location, date = date.take(10), rawDate = date,
        timeStr = time.ifBlank { null }, sport = "Fighting", league = leagueAbbr, isLive = false, isPpv = false,
    )
}

private fun PflEvent.toEspnProcessed(leagueAbbr: String): EspnProcessedEvent {
    val shortTitle = if (fighter1.isNotBlank() && fighter2.isNotBlank()) "$fighter1 vs $fighter2" else title
    return EspnProcessedEvent(
        id = "pfl_${title.hashCode()}",
        title = shortTitle,
        homeTeam = fighter2, awayTeam = fighter1,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = "", status = "Scheduled", detail = location, date = date.take(10), rawDate = date,
        timeStr = time.ifBlank { null }, sport = "Fighting", league = leagueAbbr, isLive = false, isPpv = false,
    )
}

private fun PowerSlapEvent.toEspnProcessed(leagueAbbr: String): EspnProcessedEvent {
    val shortTitle = if (fighter1.isNotBlank() && fighter2.isNotBlank()) "$fighter1 vs $fighter2" else title
    return EspnProcessedEvent(
        id = "slap_${title.hashCode()}",
        title = shortTitle,
        homeTeam = fighter2, awayTeam = fighter1,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = "", status = "Scheduled", detail = location, date = date.take(10), rawDate = date,
        timeStr = null, sport = "Fighting", league = leagueAbbr, isLive = false, isPpv = false,
    )
}

private fun BoxingSceneEvent.toEspnProcessed(leagueAbbr: String): EspnProcessedEvent {
    val vs = Regex("""(.+?)\s+vs\.?\s+(.+)""").find(title)
    val f1 = vs?.groupValues?.get(1)?.trim().orEmpty()
    val f2 = vs?.groupValues?.get(2)?.trim().orEmpty()
    return EspnProcessedEvent(
        id = "boxing_${title.hashCode()}",
        title = title,
        homeTeam = f2, awayTeam = f1,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = network, status = "Scheduled", detail = location, date = date.take(10), rawDate = date,
        timeStr = time.ifBlank { null }, sport = "Fighting", league = leagueAbbr, isLive = false, isPpv = false,
    )
}

private fun DaddyLiveEvent.toEspnProcessed(leagueAbbr: String): EspnProcessedEvent {
    val vs = Regex("""(.+?)\s+vs\.?\s+(.+)""").find(eventName)
    val f1 = vs?.groupValues?.get(1)?.trim().orEmpty()
    val f2 = vs?.groupValues?.get(2)?.trim().orEmpty()
    return EspnProcessedEvent(
        id = "dl_$id",
        title = eventName,
        homeTeam = if (f2.isNotBlank()) f2 else f1,
        awayTeam = f1,
        homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
        channel = channels.firstOrNull()?.name ?: "", status = "Scheduled", detail = category, date = day,
        rawDate = null, timeStr = localTime, sport = "Fighting", league = leagueAbbr, isLive = isLive, isPpv = false,
    )
}

@Composable
private fun LeagueNewsSection(articles: List<EspnNewsArticle>, onArticleClick: (EspnNewsArticle) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NEWS", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("${articles.size} STORIES", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.height(6.dp))
        articles.take(10).forEach { article ->
            NewsArticleCard(article, onClick = { onArticleClick(article) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NewsArticleCard(article: EspnNewsArticle, onClick: () -> Unit) {
    val img = article.images.maxByOrNull { it.width * it.height }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        if (!img?.url.isNullOrBlank()) {
            SportsAsyncImage(model = img!!.url, article.headline, Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(8.dp))
        }
        Text(article.headline, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (article.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(article.description, color = OnSurfaceVariant, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        val meta = listOfNotNull(article.byline.ifBlank { null }, shortNewsDate(article.published).ifBlank { null })
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(meta.joinToString(" · "), color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun NewsArticleDialog(article: EspnNewsArticle, onDismiss: () -> Unit) {
    val img = article.images.maxByOrNull { it.width * it.height }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        title = { Text(article.headline, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (!img?.url.isNullOrBlank()) {
                    SportsAsyncImage(model = img!!.url, article.headline, Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(10.dp))
                }
                if (article.description.isNotBlank()) {
                    Text(article.description, color = OnSurfaceVariant, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                val meta = listOfNotNull(article.byline.ifBlank { null }, shortNewsDate(article.published).ifBlank { null })
                if (meta.isNotEmpty()) {
                    Text(meta.joinToString(" · "), color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                article.links?.web?.href?.let { link ->
                    Spacer(Modifier.height(10.dp))
                    Text(link, color = Primary, fontSize = 10.sp, textDecoration = TextDecoration.Underline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Primary, fontWeight = FontWeight.Bold) }
        },
    )
}

private fun shortNewsDate(pub: String): String {
    if (pub.isBlank()) return ""
    val s = if (pub.contains(", ")) pub.substringAfter(", ").trim() else pub
    return s.take(16)
}

@Composable
private fun FightingLeagueCard(
    title: String,
    events: List<EspnProcessedEvent>,
    isTablet: Boolean,
    onEventClick: (EspnProcessedEvent) -> Unit,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)?,
    subtitle: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    val liveCount = events.count { it.isLive }

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
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column {
                        Text(title.uppercase(), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                        if (subtitle.isNotBlank()) {
                            Text(subtitle, color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("$liveCount LIVE", color = Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(if (expanded) "▲" else "▼", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (expanded) {
            if (events.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    Text("No fights right now", color = OnSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isTablet) {
                        events.chunked(2).forEach { row ->
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
                        events.forEach { event ->
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
        var selectedArticle by remember { mutableStateOf<EspnNewsArticle?>(null) }
        PullToRefreshBox(
            isRefreshing = uiState.isLoading || uiState.allLiveLoading || uiState.refreshing,
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
                if (uiState.allLiveLoading && uiState.allLiveEvents.isEmpty()) {
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
                    if (uiState.allLiveLoading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = Primary,
                                trackColor = SurfaceContainerHigh,
                            )
                        }
                    }
                    val nonFighting = uiState.allLiveEvents.filter { !it.sport.equals("Fighting", ignoreCase = true) }
                    val grouped = nonFighting.groupBy { leagueLabel(it.league).ifBlank { "Other Sports" } }
                    val fightingEvents = uiState.allLiveEvents.filter { it.sport.equals("Fighting", ignoreCase = true) }
                    val bkfcEvents = (fightingEvents.filter { it.league.equals("Bkfc", ignoreCase = true) } +
                        uiState.bkfcEvents.map { it.toEspnProcessed("BKFC") })
                        .distinctBy { it.id }
                        .distinctBy { it.title.trim().lowercase() }
                    val boxingEvents = (fightingEvents.filter { it.league.equals("Boxing", ignoreCase = true) } +
                        uiState.boxingEvents.map { it.toEspnProcessed("BOXING") } +
                        uiState.daddyLiveEvents.filter { it.category.equals("boxing", ignoreCase = true) }.map { it.toEspnProcessed("BOXING") })
                        .distinctBy { it.id }
                        .distinctBy { it.title.trim().lowercase() }
                    val powerSlapEvents = (fightingEvents.filter { it.league.equals("Powerslap", ignoreCase = true) } +
                        uiState.powerSlapEvents.map { it.toEspnProcessed("SLAP") })
                        .distinctBy { it.id }
                        .distinctBy { it.title.trim().lowercase() }
                    val otherMmaEvents = (fightingEvents.filter { ev ->
                        !ev.league.equals("Bkfc", ignoreCase = true) &&
                            !ev.league.equals("Boxing", ignoreCase = true) &&
                            !ev.league.equals("Powerslap", ignoreCase = true)
                    } + uiState.pflEvents.map { it.toEspnProcessed("PFL") } +
                        uiState.daddyLiveEvents.filter { it.category.equals("mma", ignoreCase = true) }.map { it.toEspnProcessed("MMA") })
                        .distinctBy { it.id }
                        .distinctBy { it.title.trim().lowercase() }
                    item {
                        var expandedLeagues by remember { mutableStateOf(emptySet<String>()) }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("LEAGUES", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
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
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
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

                            FightingLeagueCard(
                                title = "BKFC",
                                events = bkfcEvents,
                                isTablet = isTablet,
                                onEventClick = onEventClick,
                                onTeamClick = onTeamClick,
                            )
                            FightingLeagueCard(
                                title = "Boxing",
                                events = boxingEvents,
                                isTablet = isTablet,
                                onEventClick = onEventClick,
                                onTeamClick = onTeamClick,
                            )
                            FightingLeagueCard(
                                title = "UFC/MMA",
                                events = otherMmaEvents,
                                isTablet = isTablet,
                                onEventClick = onEventClick,
                                onTeamClick = onTeamClick,
                            )
                            FightingLeagueCard(
                                title = "Power Slap",
                                events = powerSlapEvents,
                                isTablet = isTablet,
                                onEventClick = onEventClick,
                                onTeamClick = onTeamClick,
                            )
                        }
                    }
                }
            } else {
                if (uiState.isLoading && uiState.events.isEmpty()) {
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
                    if (uiState.refreshing && uiState.events.isNotEmpty()) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = Primary,
                                trackColor = SurfaceContainerHigh,
                            )
                        }
                    }
                    val selectedSport = uiState.selectedLeague?.let { sportForLeague(it.id) }

                    if (uiState.leagueNews.isNotEmpty()) {
                        item {
                            NewsTicker(headlines = uiState.leagueNews.map { it.headline })
                        }
                    }

                    item {
                        FinalScoreScroller(events = uiState.events, onScoreClick = { ev ->
                            HubReturnStore.subScreen = "VidNutz"
                            VidNutzPendingSearch.query = if (ev.title.isNotBlank()) "${ev.title} highlights"
                                else "${ev.awayTeam} vs ${ev.homeTeam} highlights"
                        })
                    }

                    item {
                        LiveScoresSection(
                            events = uiState.events,
                            selectedSport = selectedSport,
                            onEventClick = onEventClick,
                            onTeamClick = onTeamClick,
                            isTablet = isTablet,
                        )
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
                                onViewAll = {
                                    HubReturnStore.subScreen = "VidNutz"
                                    VidNutzPendingSearch.query = uiState.selectedLeague?.let { "${it.name} highlights" } ?: "sports highlights"
                                },
                            )
                        }
                    }

                    if (uiState.leagueNews.isNotEmpty()) {
                        item {
                            LeagueNewsSection(
                                articles = uiState.leagueNews,
                                onArticleClick = { selectedArticle = it },
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
                        onPlayVideo = { v -> scope.playYouTubeVideo(v, onPlayChannel, uiState.trendingNewsVideos) },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    selectedArticle?.let { article ->
        NewsArticleDialog(article = article, onDismiss = { selectedArticle = null })
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

    LaunchedEffect(event.id) {
        SportsRepository.loadEventNews(event)
    }

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
                aliveByUrl = uiState.aliveByUrl,
                videos = uiState.sportEventVideos,
                videosLoading = uiState.sportVideosLoading,
                news = uiState.eventNews,
                newsLoading = uiState.eventNewsLoading,
                activeTab = uiState.activeEventTab,
                userRegion = uiState.userRegion,
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
                onSetTab = { SportsRepository.setActiveEventTab(it) },
                onSearchVideos = { isFuture -> SportsRepository.searchSportVideos(event, isFuture) },
                onSearchNews = { SportsRepository.loadEventNews(event) },
                onPlayHighlight = { ev ->
                    VideoSelectionFeedback.start("${ev.awayTeam} vs ${ev.homeTeam}", "Highlights")
                    scope.launch {
                        try {
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
                        } finally {
                            VideoSelectionFeedback.stop()
                        }
                    }
                },
                onSearchPortals = { ev ->
                    HubReturnStore.subScreen = "VidNutz"
                    VidNutzPendingSearch.query = "${ev.awayTeam} vs ${ev.homeTeam}"
                    VidNutzPendingSearch.isFromSportNutz = true
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
    var groupFilter by remember { mutableStateOf("All") }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    val currentYear = 2026
    val seasonOptions = listOf("Current" to (null as Int?)) + (currentYear downTo (currentYear - 3)).map { "$it" to it }
    val selectedLeague = SportsRepository.leagues.find { it.id == selectedSportId }
    val layout = if (selectedLeague != null) detectStandingsLayout(selectedLeague) else StandingsLayout.NONE

    LaunchedEffect(selectedSportId, selectedSeason) {
        val league = SportsRepository.leagues.find { it.id == selectedSportId }
        if (league != null) {
            groupFilter = "All"
            SportsRepository.loadStandingsGroups(league, selectedSeason)
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
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(9999.dp)).background(SurfaceContainerHigh).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("CURRENT SEASON", color = OnSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }

        // ── League chip selector (universal sub-filter root) ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SportsRepository.leagues.forEach { league ->
                val isSelected = selectedSportId == league.id
                Box(Modifier.clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) PrimaryContainer else SurfaceContainerHigh)
                    .clickable { selectedSportId = league.id }
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        league.logoUrl?.let {
                            AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
                        }
                        Text(league.abbreviation.uppercase(),
                            color = if (isSelected) Color.White else OnSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        // ── Year selector (change season, up to 3 years back) ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            seasonOptions.forEach { (label, year) ->
                val isSelected = selectedSeason == year
                Box(Modifier.clip(RoundedCornerShape(9999.dp))
                    .background(if (isSelected) Primary else SurfaceContainerHigh)
                    .clickable { selectedSeason = year }
                    .padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(label, color = if (isSelected) Color.White else OnSurfaceVariant,
                        fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // ── Universal sub-filter bar (conference / division / table / ranked list) ──
        if (selectedLeague != null && uiState.standingsGroups.isNotEmpty()) {
            val filters = listOf("All") + uiState.standingsGroups.map { it.name }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                filters.forEach { f ->
                    val isSelected = groupFilter == f
                    Box(Modifier.clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) Primary else SurfaceContainerHigh)
                        .clickable { groupFilter = f }
                        .padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(f, color = if (isSelected) Color.White else OnSurfaceVariant,
                            fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        if (selectedLeague == null) {
            // ── Trophy empty state (no league chip selected) ──
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\ud83c\udfc6", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Standings & Rankings", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Select a league above to see tables, rankings and points.", color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Loading standings...", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else if (uiState.error != null && uiState.standingsGroups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\ud83c\udfc6", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error, color = AccentOrange, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable {
                        val league = SportsRepository.leagues.find { it.id == selectedSportId }
                        if (league != null) SportsRepository.loadStandingsGroups(league)
                    }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Retry", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (uiState.standingsGroups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\ud83c\udfc6", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No standings yet for ${selectedLeague.name}", color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        } else {
            val visibleGroups = if (groupFilter == "All") uiState.standingsGroups
                else uiState.standingsGroups.filter { it.name == groupFilter }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                visibleGroups.forEach { group ->
                    item(key = group.name) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(group.name.ifBlank { selectedLeague.name }, color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                            Spacer(Modifier.height(6.dp))
                            when (layout) {
                                StandingsLayout.SOCCER -> SoccerStandingsTable(group, onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                StandingsLayout.COMBAT -> CombatStandingsList(group, onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                StandingsLayout.INDIVIDUAL -> RankedStandingsList(group, columns = listOf("Points", "Rank"), onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                StandingsLayout.MOTORSPORT -> RankedStandingsList(group, columns = listOf("Points", "Wins"), onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                StandingsLayout.STANDARD_TEAM, StandingsLayout.COLLEGE ->
                                    if (group.rows.firstOrNull()?.stats?.containsKey("wins") == true) StandardTeamTable(group, onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                    else RankedStandingsList(group, columns = listOf("Record", "Rating"), onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                                StandingsLayout.NONE -> RankedStandingsList(group, columns = listOf("Points", "Rank"), onClick = { r -> onTeamClick?.invoke(r.teamName, r.logo, selectedLeague.id) })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }
}

// ── Adaptive Standings & Rankings renderers ────────────────────────────────
@Composable
private fun CellText(text: String, width: androidx.compose.ui.unit.Dp, bold: Boolean = false, color: Color = OnSurface) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(width))
}

@Composable
private fun StandardTeamTable(group: StandingsGroup, onClick: (StandingsRow) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            CellText("RK", 30.dp, bold = true, color = OnSurfaceVariant)
            CellText("TEAM", 150.dp, bold = true, color = OnSurfaceVariant)
            CellText("W-L", 48.dp, bold = true, color = OnSurfaceVariant)
            CellText("PCT", 46.dp, bold = true, color = OnSurfaceVariant)
            CellText("GB", 36.dp, bold = true, color = OnSurfaceVariant)
            CellText("STRK", 44.dp, bold = true, color = OnSurfaceVariant)
        }
        group.rows.forEachIndexed { idx, row ->
            val wins = row.stat("wins"); val losses = row.stat("losses")
            val record = if (wins != "-" || losses != "-") "$wins-$losses" else row.stat("winpercent")
            val playoffTint = row.rank <= 4 || idx < group.rows.size / 2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (playoffTint) Primary.copy(alpha = 0.08f) else SurfaceContainerLow)
                    .clickable { onClick(row) }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .then(if (row.rank == 1) Modifier.border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier),
            ) {
                CellText("${row.rank}", 30.dp, bold = row.rank <= 4, color = if (row.rank <= 4) Primary else OnSurface)
                Row(Modifier.width(150.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit) }
                    Spacer(Modifier.width(6.dp))
                    Text(row.teamName, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CellText(record, 48.dp)
                CellText(row.stat("winpercent"), 46.dp)
                CellText(row.stat("gamesbehind").ifEmpty { "-" }, 36.dp)
                val streak = row.stat("streak")
                CellText(streak, 44.dp, bold = streak.startsWith("W"), color = if (streak.startsWith("W")) AccentGreen else OnSurface)
            }
        }
    }
}

@Composable
private fun SoccerStandingsTable(group: StandingsGroup, onClick: (StandingsRow) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            CellText("POS", 32.dp, bold = true, color = OnSurfaceVariant)
            CellText("CLUB", 150.dp, bold = true, color = OnSurfaceVariant)
            CellText("MP", 30.dp, bold = true, color = OnSurfaceVariant)
            CellText("W", 28.dp, bold = true, color = OnSurfaceVariant)
            CellText("D", 28.dp, bold = true, color = OnSurfaceVariant)
            CellText("L", 28.dp, bold = true, color = OnSurfaceVariant)
            CellText("GF:GA", 52.dp, bold = true, color = OnSurfaceVariant)
            CellText("GD", 38.dp, bold = true, color = OnSurfaceVariant)
            CellText("PTS", 40.dp, bold = true, color = OnSurfaceVariant)
        }
        group.rows.forEachIndexed { idx, row ->
            val gf = row.stat("goalsFor").ifEmpty { row.stat("pointsFor") }
            val ga = row.stat("goalsAgainst").ifEmpty { row.stat("pointsAgainst") }
            val gd = row.stat("goalDifference")
            val zoneColor = when {
                row.rank <= 4 -> Primary.copy(alpha = 0.5f)
                row.rank <= 6 -> AccentOrange.copy(alpha = 0.5f)
                row.rank >= 17 -> Color(0xFFFF5252).copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (idx % 2 == 0) SurfaceContainerLow else SurfaceContainer)
                    .border(1.dp, zoneColor, RoundedCornerShape(8.dp))
                    .clickable { onClick(row) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                CellText("${row.rank}", 32.dp, bold = row.rank <= 4, color = if (row.rank <= 4) Primary else OnSurface)
                Row(Modifier.width(150.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit) }
                    Spacer(Modifier.width(6.dp))
                    Text(row.teamName, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CellText(row.stat("played"), 30.dp)
                CellText(row.stat("wins"), 28.dp)
                CellText(row.stat("ties").ifEmpty { row.stat("draws") }, 28.dp)
                CellText(row.stat("losses"), 28.dp)
                CellText(if (gf != "-" && ga != "-") "$gf:$ga" else "-", 52.dp)
                CellText(if (gd != "-") if (gd.startsWith("-")) gd else "+$gd" else "-", 38.dp, bold = gd != "-", color = if (gd.startsWith("-")) Color(0xFFFF5252) else AccentGreen)
                CellText(row.stat("points"), 40.dp, bold = true)
            }
        }
    }
}

@Composable
private fun CombatStandingsList(group: StandingsGroup, onClick: (StandingsRow) -> Unit) {
    Column {
        val isChampionGroup = group.name.contains("champion", ignoreCase = true) || group.name.contains("p4p", ignoreCase = true)
        group.rows.forEachIndexed { idx, row ->
            val primary = when {
                row.stat("points") != "-" -> "PTS ${row.stat("points")}"
                row.stat("rating") != "-" -> "RTG ${row.stat("rating")}"
                else -> row.stat("wins")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (isChampionGroup && idx == 0) Color(0x33FFB300) else if (idx % 2 == 0) SurfaceContainerLow else SurfaceContainer)
                    .border(if (isChampionGroup && idx == 0) 1.dp else 0.dp, AccentOrange, RoundedCornerShape(10.dp))
                    .clickable { onClick(row) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Box(Modifier.width(34.dp)) {
                    Text(if (isChampionGroup && idx == 0) "\uD83D\uDC51" else "${row.rank}", color = if (isChampionGroup && idx == 0) AccentOrange else OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.width(190.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit) }
                    Spacer(Modifier.width(8.dp))
                    Text(row.teamName, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Text(primary, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RankedStandingsList(group: StandingsGroup, columns: List<String>, onClick: (StandingsRow) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
            CellText("RANK", 44.dp, bold = true, color = OnSurfaceVariant)
            CellText("PLAYER", 150.dp, bold = true, color = OnSurfaceVariant)
            Spacer(Modifier.weight(1f))
            columns.forEach { c -> CellText(c.uppercase(), 64.dp, bold = true, color = OnSurfaceVariant) }
        }
        group.rows.forEachIndexed { idx, row ->
            val values = columns.map { c -> row.stat(c.lowercase().replace(" ", "")) }.ifEmpty { listOf(row.stat("points")) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (idx % 2 == 0) SurfaceContainerLow else SurfaceContainer)
                    .clickable { onClick(row) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                CellText("#${row.rank}", 44.dp, bold = row.rank <= 3, color = if (row.rank <= 3) Primary else OnSurface)
                Row(Modifier.width(150.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit) }
                    Spacer(Modifier.width(6.dp))
                    Text(row.teamName, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                values.forEach { v -> CellText(v, 64.dp, bold = true) }
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

// ── Auto-scrolling league news ticker ──────────────────────────────────────
@Composable
private fun NewsTicker(headlines: List<String>) {
    if (headlines.isEmpty()) return
    val listState = rememberLazyListState()
    var tickerIndex by remember { mutableStateOf(0) }
    LaunchedEffect(headlines.size) {
        while (headlines.size > 1) {
            delay(2600)
            val next = (tickerIndex + 1) % headlines.size
            tickerIndex = next
            runCatching { listState.animateScrollToItem(next) }
        }
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceContainer)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        LazyRow(state = listState, modifier = Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), contentPadding = PaddingValues(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            items(headlines) { h ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("\uD83D\uDCF0", fontSize = 13.sp)
                    Text(h, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ── Auto-scrolling recent final-score cards (tap → VidNutz highlights) ─────
@Composable
private fun FinalScoreScroller(events: List<EspnProcessedEvent>, onScoreClick: (EspnProcessedEvent) -> Unit) {
    val finals = events.filter { isEventCompleted(it) }.distinctBy { it.id }.take(14)
    if (finals.isEmpty()) return
    val listState = rememberLazyListState()
    var scIdx by remember { mutableStateOf(0) }
    LaunchedEffect(finals.size) {
        while (finals.size > 1) {
            delay(3000)
            val next = (scIdx + 1) % finals.size
            scIdx = next
            runCatching { listState.animateScrollToItem(next) }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RECENT FINAL SCORES", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text("auto-scroll", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
        }
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            items(finals, key = { it.id }) { ev ->
                Box(
                    Modifier.width(210.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceContainer)
                        .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .clickable { onScoreClick(ev) }
                        .padding(12.dp),
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(leagueLabel(ev.league).uppercase().take(10), color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("FINAL", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${ev.homeTeam}   ${ev.homeScore ?: "0"}", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${ev.awayTeam}   ${ev.awayScore ?: "0"}", color = OnSurface.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap for highlights \u25B6", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
    var leagueFilter by remember { mutableStateOf<String?>(null) }
    val leagueFiltered = if (leagueFilter != null) filtered.filter { ev ->
        val l = SportsRepository.leagues.firstOrNull { it.id == leagueFilter }
        ev.league == leagueFilter || ev.league == l?.abbreviation || ev.league == l?.slug?.substringAfter("/")
    } else filtered
    val liveEvents = leagueFiltered.filter { it.isLive }
    val upcomingEvents = leagueFiltered.filter { !it.isLive }
    val upcoming24h = upcomingEvents.filter { !isEventCompleted(it) && isEventWithin24h(it.rawDate ?: it.date) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (filtered.isEmpty() || (liveEvents.isEmpty() && upcoming24h.isEmpty())) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\ud83c\udfc6", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No live or upcoming games in the next 24 hours", color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable {
                        SportsRepository.refresh()
                    }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Refresh", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        // ── Header: Live & Next 24h + league dropdown ──
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\uD83D\uDD34 Live & Next 24h", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Primary).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${liveEvents.size} LIVE · ${upcoming24h.size} SOON", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            var ddOpen by remember { mutableStateOf(false) }
            val selLeague = SportsRepository.leagues.firstOrNull { it.id == leagueFilter }
            Box {
                androidx.compose.material3.Surface(
                    onClick = { ddOpen = true },
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.height(36.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 14.dp)) {
                        if (selLeague != null) {
                            AsyncImage(model = selLeague.logoUrl, contentDescription = null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                        }
                        Text(
                            if (selLeague != null) selLeague.name else "All Leagues",
                            color = if (selLeague != null) OnSurface else OnSurfaceVariant,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        )
                        Text("▾", color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                }
                DropdownMenu(expanded = ddOpen, onDismissRequest = { ddOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("All Leagues", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface) },
                        onClick = { leagueFilter = null; ddOpen = false },
                    )
                    SportsRepository.leagues.forEachIndexed { index, league ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("${index + 1}.", color = Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    league.logoUrl?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit) }
                                    Text(league.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                }
                            },
                            onClick = { leagueFilter = league.id; ddOpen = false },
                        )
                    }
                }
            }
        }

        // ── LIVE NOW ──
        if (liveEvents.isNotEmpty()) {
            Text("LIVE NOW", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 6.dp))
            if (isTablet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    liveEvents.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { event ->
                                MatchCard(modifier = Modifier.weight(1f), event = event, isPhone = false, onClick = { onEventClick?.invoke(event) ?: Unit }, onTeamClick = onTeamClick)
                            }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    liveEvents.forEach { event ->
                        MatchCard(event = event, isPhone = true, onClick = { onEventClick?.invoke(event) ?: Unit }, onTeamClick = onTeamClick)
                    }
                }
            }
        }

        // ── NEXT 24 HOURS (countdown cards) ──
        if (upcoming24h.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("NEXT 24 HOURS", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 6.dp))
            if (isTablet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming24h.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { event ->
                                MatchCard(modifier = Modifier.weight(1f), event = event, isPhone = false, onClick = { onEventClick?.invoke(event) ?: Unit }, onTeamClick = onTeamClick)
                            }
                            if (row.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming24h.forEach { event ->
                        MatchCard(event = event, isPhone = true, onClick = { onEventClick?.invoke(event) ?: Unit }, onTeamClick = onTeamClick)
                    }
                }
            }
        }
    }
}

// ── Schedule list row (compact, uniform) ───────────────────────────────────
@Composable
private fun ScheduleListRow(
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
            val h = hms.getOrNull(0)?.toIntOrNull() ?: 0
            val m = hms.getOrNull(1)?.toIntOrNull() ?: 0
            val ap = if (h < 12) "AM" else "PM"
            val h12 = if (h % 12 == 0) 12 else h % 12
            "$h12:${m.toString().padStart(2, '0')} $ap"
        } catch (_: Exception) { "" }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(Modifier.width(72.dp)) {
            Text(dateStr, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (timeStr.isNotBlank()) Text(timeStr, color = OnSurfaceVariant, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(event.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tvChannels.isNotEmpty()) {
                Text(tvChannels.take(2).joinToString(" · ") { it.name }, color = OnSurfaceVariant.copy(alpha = 0.8f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (event.leagueName.isNotBlank()) {
            Text(event.leagueName.take(10), color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.background(SurfaceContainerHigh, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaddyLiveEventCard(event: DaddyLiveEvent, isLive: Boolean, onPlay: () -> Unit) {
    val chCount = event.channels.size
    var showChannels by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(SurfaceContainerLow)
            .border(0.5.dp, if (isLive) ErrorRed.copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable(onClick = { if (chCount > 0) showChannels = true else onPlay() })
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

    if (showChannels) {
        ModalBottomSheet(
            onDismissRequest = { showChannels = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().heightIn(max = 500.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(event.eventName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    Text("$chCount source${if (chCount != 1) "s" else ""}", color = OnSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(event.channels) { ch ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow)
                                .clickable { showChannels = false; onPlay() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, color = OnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Play", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
        else -> leagueLabel(event.league).take(4).uppercase()
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
                    text = leagueLabel(event.league).uppercase().take(8),
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
    onViewAll: (() -> Unit)? = null,
) {
    val eventMap = events.associateBy { it.id }
    val filtered = if (selectedSport != null) videos.filter { highlight ->
        eventMap[highlight.eventId]?.sport == selectedSport || highlight.sport == selectedSport
    } else videos
    val display = filtered.take(6)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TRENDING HIGHLIGHTS", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            if (onViewAll != null) {
                Text("VIEW ALL (${display.size})", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onViewAll).padding(horizontal = 8.dp, vertical = 2.dp))
            }
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
private fun ChannelPickerDialog(title: String, channels: List<ChannelScore>, onDismiss: () -> Unit, onSelect: (ChannelScore) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Choose Channel", color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                channels.forEach { score ->
                    val ch = score.channel
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable { onSelect(score) }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (ch.logo != null) SportsAsyncImage(model = ch.logo, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = ch.name, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (score.reasons.isNotEmpty()) {
                                    Text(
                                        text = "${ch.sourceType.label} • ${score.reasons.joinToString(" • ")} • ${score.matchType.label}",
                                        color = OnSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
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
    modifier: Modifier = Modifier,
) {
    val quickLeagues = listOf("nfl", "nba", "mlb", "nhl", "ufc", "bkfc", "mls")
    val quickList = leagues.filter { it.id.lowercase() in quickLeagues }
    val orderedQuick = quickLeagues.mapNotNull { abbr -> quickList.find { it.id.lowercase() == abbr } }
    var showMore by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(top = 16.dp)) {
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
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (isLive) Color.Black else OnSurfaceVariant))
                    Text("LIVE",
                        color = if (isLive) Color.Black else OnSurfaceVariant,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }

            orderedQuick.forEach { league ->
                val isSelected = selectedLeague?.id == league.id
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) Primary else SurfaceContainer)
                        .border(if (!isSelected) 0.5.dp else 0.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                        .clickable { onLeagueSelected(if (isSelected) null else league) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        league.logoUrl?.let {
                            AsyncImage(model = it, contentDescription = league.abbreviation,
                                modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
                        }
                        Text(league.abbreviation.uppercase(),
                            color = if (isSelected) Color.Black else OnSurfaceVariant,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            // MORE chip
            Box(
                modifier = Modifier.clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { showMore = !showMore }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (showMore) "\u25B2" else "\u25BC", fontSize = 9.sp, color = OnSurfaceVariant)
                    Text("MORE", color = OnSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            league.logoUrl?.let {
                                AsyncImage(model = it, contentDescription = league.abbreviation,
                                    modifier = Modifier.size(14.dp), contentScale = ContentScale.Fit)
                            }
                            Text(league.abbreviation.uppercase(),
                                color = if (isSelected) Color.Black else OnSurfaceVariant,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
    aliveByUrl: Map<String, Boolean>,
    videos: List<SportEventVideo>,
    videosLoading: Boolean,
    news: List<EspnNewsArticle>,
    newsLoading: Boolean,
    activeTab: EventTab,
    userRegion: BroadcastRegion = BroadcastRegion.OTHER,
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayPlayerLaunch: ((PlayerLaunch) -> Unit)? = null,
    onSetTab: (EventTab) -> Unit,
    onSearchVideos: (Boolean) -> Unit,
    onSearchNews: () -> Unit,
    onPlayHighlight: (EspnProcessedEvent) -> Unit,
    onSearchPortals: (EspnProcessedEvent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isLive = event.isLive
    var aiSearchQuery by remember { mutableStateOf("") }
    var aiSearchResult by remember { mutableStateOf<AiChannelSearchResult?>(null) }
    var aiSearchLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeTab) {
        if (activeTab != EventTab.LIVE) {
            aiSearchQuery = ""
            aiSearchResult = null
        }
    }
    var selectedArticle by remember { mutableStateOf<EspnNewsArticle?>(null) }
    val tabs = buildList {
        add(EventTab.HIGHLIGHTS)
        add(EventTab.LIVE)
        add(EventTab.NEWS)
        if (!isLive) add(EventTab.PRE_MATCH)
    }

    LaunchedEffect(event) {
        onSetTab(EventTab.HIGHLIGHTS)
        onSearchVideos(false)
    }

    selectedArticle?.let { article ->
        NewsArticleDialog(article = article, onDismiss = { selectedArticle = null })
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
                        if (tab == EventTab.NEWS) onSearchNews()
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text(tab.label, color = if (isSelected) Color(0xFF00363A) else OnSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (activeTab) {
            EventTab.LIVE -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All regions", color = OnSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${if (aiSearchResult != null) aiSearchResult!!.filteredChannels.size else matchedChannels.size} channels", color = OnSurfaceVariant, fontSize = 12.sp)
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryContainer.copy(alpha = 0.3f)).clickable { onSearchPortals(event) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, "Search Portals", tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Search Portals", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (AiSettingsStore.isEnabled()) {
                    AiChannelSearchBar(
                        query = aiSearchQuery,
                        isLoading = aiSearchLoading,
                        onQueryChange = { aiSearchQuery = it },
                        onSearch = { q ->
                            aiSearchLoading = true
                            scope.launch {
                                val result = AiChannelSearchStore.searchChannels(
                                    event = event,
                                    allChannels = IptvRepository.getAllChannels(),
                                    matchedChannels = matchedChannels,
                                    query = q,
                                    region = userRegion,
                                ).first()
                                aiSearchResult = result
                                aiSearchLoading = false
                            }
                        },
                        onClear = { aiSearchQuery = ""; aiSearchResult = null },
                    )
                    aiSearchResult?.let { result ->
                        AiChannelSearchExplanationCard(result = result)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                val displayChannels = aiSearchResult?.filteredChannels?.ifEmpty { matchedChannels } ?: matchedChannels
                ChannelListContent(displayChannels, channelsLoading, event, aliveByUrl, onPlayChannel, onPlayHighlight, onSearchPortals)
            }
            EventTab.NEWS -> {
                if (newsLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (news.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No news found", color = OnSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)
                                .clickable(onClick = onSearchNews)
                                .padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text("Retry", color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        items(news, key = { it.links?.web?.href ?: it.headline }) { article ->
                            NewsArticleCard(article, onClick = { selectedArticle = article })
                        }
                    }
                }
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
                                    if (result != null) {
                                        val queueUrls = videos.map { v -> "yt://${v.videoId}" }
                                        val queueTitles = videos.map { it.title }
                                        val currentIdx = videos.indexOfFirst { it.videoId == video.videoId }.coerceAtLeast(0)
                                        onPlayPlayerLaunch?.invoke(PlayerLaunch(
                                            profileId = 0, title = video.title, sourceUrl = result.url,
                                            sourceHeaders = result.headers, streamTitle = video.title,
                                            providerName = "YouTube", parentMetaId = "youtube",
                                            parentMetaType = "youtube",
                                            sourceAudioUrl = result.audioUrl, qualities = result.qualities,
                                            autoPlayQueueUrls = queueUrls, autoPlayQueueTitles = queueTitles,
                                            autoPlayQueueIndex = currentIdx,
                                        ))
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
    aliveByUrl: Map<String, Boolean>,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayHighlight: (EspnProcessedEvent) -> Unit,
    onSearchPortals: (EspnProcessedEvent) -> Unit,
) {
    if (matchedChannels.isNotEmpty()) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Primary,
                trackColor = SurfaceContainerHigh,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(matchedChannels) { index, matched ->
                val isBestMatch = index == 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (isBestMatch) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainer)
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(matched.channel.name, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                            if (isBestMatch) {
                                Box(modifier = Modifier.background(Primary, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text("BEST", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Text(matched.sourceName.ifBlank { matched.providerGroup.label }, color = OnSurfaceVariant, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (aliveByUrl[matched.channel.url]) {
                            true -> Text("✓ Live", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            null -> Text("checking", color = OnSurfaceVariant, fontSize = 10.sp)
                            else -> Text("offline", color = Color(0xFFFF6E6E), fontSize = 10.sp)
                        }
                        Text(matched.providerGroup.label, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.background(SurfaceContainerHigh, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text(matched.matchType.label, color = Color(0xFF00363A), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(PrimaryContainer.copy(alpha = 0.9f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    } else if (isLoading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No IPTV channels found for this game", color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh).clickable { onPlayHighlight(event) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Watch Highlights Instead", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryContainer.copy(alpha = 0.3f)).clickable { onSearchPortals(event) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, "Search Portals", tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search Portals", color = Primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
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
private fun parseEventMillis(raw: String): Long? {
    val s = raw.trim()
    if (s.isBlank()) return null
    val isoParsed = com.nuvio.app.features.trakt.parseTraktIsoDateTimeToEpochMs(s)
    if (isoParsed != null) return isoParsed

    val sWithZ = if (s.length == 19 && s.contains("T")) "${s}Z" else s
    val withZParsed = com.nuvio.app.features.trakt.parseTraktIsoDateTimeToEpochMs(sWithZ)
    if (withZParsed != null) return withZParsed

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    val re = Regex("""([A-Za-z]+)\s+(\d{1,2}),?\s+(\d{4})(?:\s+(\d{1,2}):(\d{2})\s*(am|pm))?""", RegexOption.IGNORE_CASE)
    val m = re.find(s)
    if (m != null) {
        val month = monthNames.indexOfFirst { it.equals(m.groupValues[1], ignoreCase = true) } + 1
        if (month > 0) {
            val day = m.groupValues[2].toIntOrNull() ?: 1
            val year = m.groupValues[3].toIntOrNull() ?: 2026
            var hour = m.groupValues[4].toIntOrNull() ?: 12
            val minute = m.groupValues[5].toIntOrNull() ?: 0
            val ap = m.groupValues[6].lowercase()
            if (ap == "pm" && hour < 12) hour += 12
            if (ap == "am" && hour == 12) hour = 0
            val monthStr = if (month < 10) "0$month" else "$month"
            val dayStr = if (day < 10) "0$day" else "$day"
            val hourStr = if (hour < 10) "0$hour" else "$hour"
            val minStr = if (minute < 10) "0$minute" else "$minute"
            val iso = "${year}-${monthStr}-${dayStr}T${hourStr}:${minStr}:00Z"
            return com.nuvio.app.features.trakt.parseTraktIsoDateTimeToEpochMs(iso)
        }
    }
    return null
}

private fun formatTimeFromEpochMs(epochMs: Long, offsetHours: Int = 0): String {
    val totalSec = (epochMs / 1000L) + (offsetHours * 3600L)
    val totalMin = totalSec / 60L
    val totalHours = totalMin / 60L
    val hourOfDay = (((totalHours % 24) + 24) % 24).toInt()
    val minute = (((totalMin % 60) + 60) % 60).toInt()
    val h12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
    val ap = if (hourOfDay < 12) "AM" else "PM"
    val mStr = if (minute < 10) "0$minute" else "$minute"
    return "$h12:$mStr$ap"
}

private fun eventZonesLine(rawDate: String): String {
    if (rawDate.isBlank()) return ""
    val epochMs = parseEventMillis(rawDate) ?: return ""
    val et = formatTimeFromEpochMs(epochMs, -5)
    val ct = formatTimeFromEpochMs(epochMs, -6)
    val mt = formatTimeFromEpochMs(epochMs, -7)
    val pt = formatTimeFromEpochMs(epochMs, -8)
    val uk = formatTimeFromEpochMs(epochMs, 0)
    val ca = formatTimeFromEpochMs(epochMs, -5)
    return "US ET $et · CT $ct · MT $mt · PT $pt · UK $uk · CA $ca"
}

/** True when the event has ended (Final/FT/ended/etc.). */
private fun isEventCompleted(event: EspnProcessedEvent): Boolean {
    if (event.isLive) return false
    val s = (event.status + " " + (event.detail ?: "")).uppercase()
    val finishedWords = listOf("FINAL", "FULL TIME", "FT", "ENDED", "COMPLETE", "FINAL/OT", "FINAL OT", "FINAL SO",
        "CANCELED", "CANCELLED", "POSTPONED", "SUSPENDED", "FORFEIT", "ABANDONED", "RESULT")
    if (finishedWords.any { s.contains(it) }) return true
    // A score with a start time well in the past = finished.
    if (!event.homeScore.isNullOrBlank() || !event.awayScore.isNullOrBlank()) {
        val t = event.rawDate?.let { parseEventMillis(it) }
        if (t != null && t < com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs() - 3_600_000L) return true
    }
    return false
}

/** Map ESPN league codes to readable labels (soccer slugs etc.). */
private fun leagueLabel(code: String): String = when (code.lowercase()) {
    "usa.1" -> "MLS"; "uefa.champions" -> "UCL"
    "college-football" -> "CFB"; "mens-college-basketball" -> "CBB"
    else -> code
}

/** True when the event starts within the next 24 hours (or is live now). */
private fun isEventWithin24h(rawDate: String): Boolean {
    if (rawDate.isBlank()) return false
    val t = parseEventMillis(rawDate) ?: return false
    val now = com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs()
    return t in (now - 3_600_000L)..(now + 86_400_000L)
}

/** Countdown label like "in 3h 12m" / "in 1d 4h" / "now". */
private fun countdownLabel(rawDate: String): String {
    if (rawDate.isBlank()) return ""
    val t = parseEventMillis(rawDate) ?: return ""
    val now = com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs()
    val diff = t - now
    if (diff <= 0) return "LIVE"
    val minutes = diff / 60_000L
    if (minutes < 60) return "in ${minutes}m"
    val hours = minutes / 60
    if (hours < 24) return "in ${hours}h ${minutes % 60}m"
    return "in ${hours / 24}d ${hours % 24}h"
}

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
    val timeSource = event.rawDate ?: event.date
    val hasTime = timeSource.contains("T") || timeSource.contains(":") ||
        timeSource.contains("PM", ignoreCase = true) || timeSource.contains("AM", ignoreCase = true)
    val zoneLine = eventZonesLine(timeSource)
    val league = SportsRepository.leagues.firstOrNull { l -> l.abbreviation.equals(event.league, ignoreCase = true) || l.name.equals(event.league, ignoreCase = true) }

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
                    val timeLabel = run {
                        val t = event.timeStr?.takeIf { it.isNotBlank() }
                        when {
                            t != null -> t
                            hasTime -> parseEventMillis(timeSource)?.let { ms ->
                                formatTimeFromEpochMs(ms, 0)
                            } ?: "time unknown"
                            else -> "time unknown"
                        }
                    }
                    Text(timeLabel, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    val completed = isEventCompleted(event)
                    when {
                        event.isLive -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            LivePulseDotSmall()
                            Text("LIVE", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                        completed -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("FINAL", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                        else -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(Primary.copy(alpha = 0.15f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("UPCOMING", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    league?.logoUrl?.let {
                        AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit)
                    }
                    Text(leagueLabel(event.league).uppercase().take(10), color = OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
                }
            }
            if (dateLabel.isNotBlank() && !event.isLive) {
                Spacer(Modifier.height(2.dp))
                Text(dateLabel, color = OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            if (hasTime && zoneLine.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(zoneLine, color = AccentGreen.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!event.isLive && !isEventCompleted(event) && isEventWithin24h(event.rawDate ?: event.date)) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.clip(RoundedCornerShape(9999.dp)).background(Primary.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(countdownLabel(event.rawDate ?: event.date), color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
            val fightCardShowsTeams = event.sport.equals("Fighting", ignoreCase = true) &&
                event.homeTeam.isNotBlank() && event.awayTeam.isNotBlank()
            if (!fightCardShowsTeams) {
                Text(event.title, color = OnSurface, fontSize = if (isPhone) 15.sp else 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
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
                        val completed = isEventCompleted(event)
                        if (completed) {
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

private fun parseIsoMillis(dateStr: String): Long? = parseEventMillis(dateStr)

private fun splitEventTeams(title: String): Pair<String, String> {
    val parts = title.split(Regex("\\s+[vV][sS]\\s+|\\s*@\\s*"))
    val first = parts.firstOrNull()?.trim().orEmpty()
    val second = parts.getOrNull(1)?.trim().orEmpty()
    if (first.isBlank() || second.isBlank()) return Pair("Away", "Home")
    return Pair(first, second)
}

// ── AI Channel Search Bar ────────────────────────────────────────────────────
@Composable
private fun AiChannelSearchBar(
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("AI: e.g. 'ESPN channels in HD', 'my region, no duplicates'",
            color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "AI Search",
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Primary,
                    )
                } else {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, "Clear", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary.copy(alpha = 0.6f),
            unfocusedBorderColor = SurfaceContainerHighest,
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
            cursorColor = Primary,
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { if (query.isNotBlank()) onSearch(query) },
        ),
    )
}

// ── AI Channel Search Explanation Card ───────────────────────────────────────
@Composable
private fun AiChannelSearchExplanationCard(result: AiChannelSearchResult) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PrimaryContainer.copy(alpha = 0.2f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Icon(Icons.Default.Search, "AI Insight", tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("AI Insight", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (result.latencyMs > 0) {
                Spacer(Modifier.width(4.dp))
                Text("${result.latencyMs}ms", color = OnSurfaceVariant, fontSize = 10.sp)
            }
        }
        Text(result.explanation, color = OnSurface, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (result.suggestedPortals.isNotEmpty()) {
            Text("Suggested portals:", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(result.suggestedPortals) { portal ->
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(PrimaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(portal, color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

