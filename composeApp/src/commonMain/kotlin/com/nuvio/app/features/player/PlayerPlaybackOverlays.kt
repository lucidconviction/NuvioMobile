package com.nuvio.app.features.player

import com.nuvio.app.features.iptv.ChannelQuickSearchStore
import com.nuvio.app.features.iptv.EpgProgram
import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.QuickChannel
import com.nuvio.app.features.iptv.QuickChannelList
import com.nuvio.app.features.iptv.SourceType
import com.nuvio.app.features.sports.ChannelScore
import com.nuvio.app.features.sports.ChannelScorer
import com.nuvio.app.features.sports.DaddyLiveEvent
import com.nuvio.app.features.trakt.TraktPlatformClock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nuvio.app.features.p2p.P2pLoadingStatus
import com.nuvio.app.features.player.skip.NextEpisodeCard
import com.nuvio.app.features.player.skip.NextEpisodeInfo
import com.nuvio.app.features.player.skip.SkipIntroButton
import com.nuvio.app.features.player.skip.SkipInterval

@Composable
internal fun BoxScope.PlayerPlaybackOverlays(
    channelOverlayTrigger: Long = 0L,
    historyOverlayTrigger: Long = 0L,
    showLiveGamesOverlay: Boolean = false,
    onDismissLiveGames: (() -> Unit)? = null,
    playerControlsLocked: Boolean,
    lockedOverlayVisible: Boolean,
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    horizontalSafePadding: Dp,
    onUnlock: () -> Unit,
    showOpeningOverlay: Boolean,
    backdropArtwork: String?,
    logo: String?,
    title: String,
    onBackWithProgress: () -> Unit,
    p2pInitialLoadingMessage: String?,
    p2pInitialLoadingProgress: Float?,
    showP2pRebufferStats: Boolean,
    p2pRebufferMessage: String?,
    p2pRebufferProgress: Float?,
    currentGestureFeedback: GestureFeedbackState?,
    renderedGestureFeedback: GestureFeedbackState?,
    initialLoadCompleted: Boolean,
    pausedOverlayVisible: Boolean,
    activeSkipInterval: SkipInterval?,
    skipIntervalDismissed: Boolean,
    controlsVisible: Boolean,
    onSkipInterval: (SkipInterval) -> Unit,
    onDismissSkipInterval: () -> Unit,
    sliderEdgePadding: Dp,
    overlayBottomPadding: Dp,
    isSeries: Boolean,
    nextEpisodeInfo: NextEpisodeInfo?,
    showNextEpisodeCard: Boolean,
    nextEpisodeAutoPlaySearching: Boolean,
    nextEpisodeAutoPlaySourceName: String?,
    nextEpisodeAutoPlayCountdown: Int?,
    onPlayNextEpisode: () -> Unit,
    onDismissNextEpisode: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
    channelNames: List<String>? = null,
    channelUrls: List<String>? = null,
    channelLogos: List<String>? = null,
    channelIds: List<String>? = null,
    favoriteIds: Set<String>? = null,
    epgLoader: (suspend (channelId: String?, channelName: String) -> Pair<EpgProgram?, EpgProgram?>)? = null,
    historyNames: List<String>? = null,
    historyUrls: List<String>? = null,
    historyLogos: List<String>? = null,
    historyIds: List<String>? = null,
    currentChannelIndex: Int = 0,
    onSwitchChannel: ((Int) -> Unit)? = null,
    onToggleFavorite: ((String) -> Unit)? = null,
    onAddToMultiView: ((name: String, url: String, logo: String?) -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = playerControlsLocked && lockedOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LockedPlayerOverlay(
            playbackSnapshot = playbackSnapshot,
            displayedPositionMs = displayedPositionMs,
            metrics = metrics,
            horizontalSafePadding = horizontalSafePadding,
            onUnlock = onUnlock,
            modifier = Modifier.fillMaxSize(),
        )
    }

    AnimatedVisibility(
        visible = showOpeningOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        OpeningOverlay(
            artwork = backdropArtwork,
            logo = logo,
            title = title,
            onBack = onBackWithProgress,
            horizontalSafePadding = horizontalSafePadding,
            modifier = Modifier.fillMaxSize(),
            message = p2pInitialLoadingMessage,
            progress = p2pInitialLoadingProgress,
        )
    }

    P2pLoadingStatus(
        visible = showP2pRebufferStats && errorMessage == null,
        message = p2pRebufferMessage,
        progress = p2pRebufferProgress,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(top = 58.dp),
    )

    AnimatedVisibility(
        visible = currentGestureFeedback != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            renderedGestureFeedback?.let { feedback ->
                GestureFeedbackPill(
                    feedback = feedback,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                        .padding(horizontal = horizontalSafePadding)
                        .padding(top = 40.dp),
                )
            }
        }
    }

    if (!playerControlsLocked) {
        SkipIntroButton(
            interval = if (!initialLoadCompleted || pausedOverlayVisible) null else activeSkipInterval,
            dismissed = skipIntervalDismissed,
            controlsVisible = controlsVisible,
            onSkip = {
                activeSkipInterval?.let(onSkipInterval)
            },
            onDismiss = onDismissSkipInterval,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = sliderEdgePadding, bottom = overlayBottomPadding),
        )
    }

    if (isSeries && !playerControlsLocked) {
        NextEpisodeCard(
            nextEpisode = nextEpisodeInfo,
            visible = showNextEpisodeCard,
            isAutoPlaySearching = nextEpisodeAutoPlaySearching,
            autoPlaySourceName = nextEpisodeAutoPlaySourceName,
            autoPlayCountdownSec = nextEpisodeAutoPlayCountdown,
            onPlayNext = onPlayNextEpisode,
            onDismiss = onDismissNextEpisode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = sliderEdgePadding, bottom = overlayBottomPadding),
        )
    }

    if (errorMessage != null) {
        ErrorModal(
            message = errorMessage,
            onDismiss = onDismissError,
        )
    }

    // Live Games overlay
    if (showLiveGamesOverlay) {
        val todayPrefix = (TraktPlatformClock.nowEpochMs() + TraktPlatformClock.localTimezoneOffsetMs()).let { epochMs ->
            val totalDays = (epochMs / 86400000L).toInt()
            var y = 1970
            var rem = totalDays
            while (true) { val dim = if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 366 else 365; if (rem < dim) break; rem -= dim; y++ }
            val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
            val months = if (leap) intArrayOf(31,29,31,30,31,30,31,31,30,31,30,31) else intArrayOf(31,28,31,30,31,30,31,31,30,31,30,31)
            var m = 0; while (m < 12 && rem >= months[m]) { rem -= months[m]; m++ }
            "${y}-${(m+1).toString().padStart(2,'0')}-${(rem+1).toString().padStart(2,'0')}"
        }
        val espnEvents = SportsNowStore.liveEvents.filter { it.isLive }.filter { it.rawDate?.startsWith(todayPrefix) == true || it.rawDate == null }
        val dlEvents = SportsNowStore.daddyLiveEvents.filter { it.isLive }
        val hasAny = espnEvents.isNotEmpty() || dlEvents.isNotEmpty()

        var pickerEvent by remember { mutableStateOf<Pair<String, List<ChannelScore>>?>(null) }
        val overlayScope = rememberCoroutineScope()

        val listState = rememberLazyListState()
        var lastScrollTime by remember { mutableStateOf(0L) }
        LaunchedEffect(showLiveGamesOverlay) {
            if (showLiveGamesOverlay) lastScrollTime = TraktPlatformClock.nowEpochMs()
        }
        LaunchedEffect(showLiveGamesOverlay, listState) {
            if (!showLiveGamesOverlay) return@LaunchedEffect
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { lastScrollTime = TraktPlatformClock.nowEpochMs() }
        }
        LaunchedEffect(showLiveGamesOverlay) {
            if (!showLiveGamesOverlay) return@LaunchedEffect
            while (true) {
                delay(500)
                if (TraktPlatformClock.nowEpochMs() - lastScrollTime >= 10_000L) {
                    onDismissLiveGames?.invoke()
                    break
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onDismissLiveGames?.invoke() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (pickerEvent != null) {
                val (pickerTitle, channels) = pickerEvent!!
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Channels: $pickerTitle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("\u2715", color = Color(0xFF888888), fontSize = 22.sp, modifier = Modifier.clickable { pickerEvent = null })
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(360.dp).fillMaxWidth()) {
                        itemsIndexed(channels) { _, score ->
                            val ch = score.channel
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(8.dp)).clickable {
                                    pickerEvent = null
                                    SportsNowStore.onSwitchToChannel?.let { it(ch) }
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                    if (score.reasons.isNotEmpty()) {
                                        Text(score.reasons.joinToString(" • "), color = Color(0xFF888888), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else if (!ch.group.isNullOrBlank()) {
                                        Text(ch.group, color = Color(0xFF888888), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Text("Play", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Live Games", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (espnEvents.isNotEmpty()) Text("${espnEvents.size} ESPN", color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (dlEvents.isNotEmpty()) Text("${dlEvents.size} DADDYLIVE", color = Color(0xFFE8553A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("\u2715", color = Color(0xFF888888), fontSize = 22.sp, modifier = Modifier.clickable { onDismissLiveGames?.invoke() })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!hasAny) {
                        Text("No live games right now", color = Color(0xFF888888))
                    } else {
                        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(360.dp)) {
                            itemsIndexed(espnEvents) { _, event ->
                                Row(
                                                                                        modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(8.dp)).clickable {
                                        val se = EspnClient.toSportEvents(listOf(event))
                                        if (se.isNotEmpty()) {
                                            overlayScope.launch {
                                                val scored = IptvRepository.getAllChannels().let { allCh ->
                                                    EspnClient.findScoredMatchingChannelsWithLazyEpg(
                                                        se.first(), allCh, IptvRepository.buildCurrentEpgTitleLookup()
                                                    ).filterNot { com.nuvio.app.features.iptv.StreamValidationStore.isKnownDeadSync(it.channel.url) }
                                                }
                                                val autoplay = ChannelScorer.autoplayCandidate(scored)
                                                if (autoplay != null) {
                                                    SportsNowStore.onSwitchToEvent?.invoke(event)
                                                } else if (scored.isNotEmpty()) {
                                                    pickerEvent = event.title to scored
                                                }
                                            }
                                        }
                                    }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${event.awayTeam} vs ${event.homeTeam}", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(event.league, color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(event.detail, color = Color(0xFF00FF00), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text("${event.awayScore ?: "-"} - ${event.homeScore ?: "-"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
                                    if (event.channel.isNotBlank()) {
                                        Text(event.channel, color = Color(0xFF888888), fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                                    }
                                    Text("Switch", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            itemsIndexed(dlEvents) { _, dlEvent ->
                                val isLive = dlEvent.isLive
                                val chNames = dlEvent.channels.joinToString(", ") { it.name }
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1A0A0A), RoundedCornerShape(8.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dlEvent.eventName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(dlEvent.category, color = Color(0xFFE8553A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (isLive) {
                                                Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF00FF00)))
                                                Text("LIVE", color = Color(0xFF00FF00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (chNames.isNotEmpty()) {
                                            Text(chNames.take(80), color = Color(0xFF888888), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE8553A).copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                        Text(if (isLive) "LIVE" else "UPCOMING", color = Color(0xFFE8553A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (channelNames != null && channelUrls != null && onSwitchChannel != null && !playerControlsLocked) {
        var overlayMode by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }

        val colors = MaterialTheme.colorScheme
        val accentPurple = colors.primary
        val onSurface = colors.onSurface
        val onSurfaceVariant = colors.onSurfaceVariant
        val selectedBg = colors.surfaceContainerHigh.copy(alpha = 0.6f)
        val sheetBg = colors.surfaceContainerLow
        val chipBg = colors.surfaceContainerHigh

        val hasHistory = historyNames != null && historyNames.isNotEmpty()
        val hasFavorites = favoriteIds != null && favoriteIds.isNotEmpty()

        val showingQuick = overlayMode == "quick"

        LaunchedEffect(channelOverlayTrigger) {
            if (channelOverlayTrigger > 0L) {
                overlayMode = "list"
                searchQuery = ""
            }
        }

        LaunchedEffect(historyOverlayTrigger) {
            if (historyOverlayTrigger > 0L) {
                overlayMode = "history"
                searchQuery = ""
            }
        }

        if (overlayMode != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { overlayMode = null; searchQuery = "" },
            )

            val accentPurpleLight = colors.primary.copy(alpha = 0.75f)
            val listState = rememberLazyListState()
            var lastScrollTime by remember(overlayMode) { mutableStateOf(0L) }

            val showingHistory = overlayMode == "history"
            val showingFavorites = overlayMode == "favorites"
            val showingSaved = overlayMode == "saved"

            val favoriteIndices = remember(channelNames, channelIds, favoriteIds) {
                val names = channelNames ?: emptyList()
                val ids = channelIds ?: emptyList()
                val favs = favoriteIds ?: emptySet()
                names.indices.filter { idx ->
                    ids.getOrNull(idx)?.let { id -> favs.contains(id) } == true
                }
            }
            val entries = when (overlayMode) {
                "history" -> historyNames ?: emptyList()
                "favorites" -> favoriteIndices.mapNotNull { channelNames?.getOrNull(it) }
                else -> channelNames ?: emptyList()
            }
            val entryUrls = when (overlayMode) {
                "history" -> historyUrls ?: emptyList()
                else -> channelUrls
            }
            val entryLogos = when (overlayMode) {
                "history" -> historyLogos ?: emptyList()
                else -> channelLogos
            }
            val entryIds = when (overlayMode) {
                "history" -> historyIds ?: emptyList()
                else -> channelIds
            }

            val query = searchQuery.trim().lowercase()
            val filteredHistoryIndices = remember(showingHistory, entries, query) {
                if (showingHistory) {
                    entries.indices.filter { idx ->
                        query.isEmpty() || (entries.getOrNull(idx) ?: "").lowercase().contains(query)
                    }
                } else emptyList()
            }
            val filteredListIndices = remember(showingHistory, showingQuick, entries, query) {
                if (!showingHistory && !showingQuick) {
                    entries.indices.filter { idx ->
                        query.isEmpty() || (entries.getOrNull(idx) ?: "").lowercase().contains(query)
                    }
                } else emptyList()
            }

            LaunchedEffect(overlayMode) {
                if (overlayMode == "list" && currentChannelIndex >= 0) {
                    try {
                        val total = filteredListIndices.size
                        if (total > 0) {
                            val safeIdx = currentChannelIndex.coerceIn(0, total - 1)
                            listState.scrollToItem(safeIdx)
                        }
                    } catch (_: Throwable) { }
                }
            }
            LaunchedEffect(overlayMode) {
                if (overlayMode != null) {
                    lastScrollTime = TraktPlatformClock.nowEpochMs()
                }
            }
            LaunchedEffect(overlayMode, listState) {
                if (overlayMode == null) return@LaunchedEffect
                snapshotFlow {
                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                }.collect {
                    lastScrollTime = TraktPlatformClock.nowEpochMs()
                }
            }
            LaunchedEffect(overlayMode) {
                if (overlayMode == null) return@LaunchedEffect
                while (true) {
                    delay(500)
                    val idleMs = TraktPlatformClock.nowEpochMs() - lastScrollTime
                    if (idleMs >= 12_000L) {
                        overlayMode = null
                        searchQuery = ""
                        break
                    }
                }
            }

            AnimatedVisibility(
                visible = overlayMode != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.70f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(sheetBg)
                        .padding(bottom = overlayBottomPadding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(onSurfaceVariant.copy(alpha = 0.3f)),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when (overlayMode) {
                                    "quick" -> "Quick Channels"
                                    "favorites" -> "Favorites"
                                    "history" -> "History"
                                    "saved" -> "Saved Searches"
                                    else -> "Channels"
                                },
                                color = onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when {
                                        showingQuick -> "Layers"
                                        showingHistory -> "${filteredHistoryIndices.size}"
                                        showingSaved -> "${ChannelQuickSearchStore.loadTerms().size}"
                                        else -> "${filteredListIndices.size}"
                                    },
                                    color = onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "✕",
                                    color = onSurfaceVariant,
                                    fontSize = 22.sp,
                                    modifier = Modifier.clickable { overlayMode = null; searchQuery = "" },
                                )
                            }
                        }

                        val tabs = buildList {
                            add("list" to "Channels")
                            add("quick" to "Quick Channels")
                            add("saved" to "Saved Searches")
                            if (hasFavorites) add("favorites" to "Favorites")
                            if (hasHistory) add("history" to "History")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tabs.forEach { (id, label) ->
                                val selected = overlayMode == id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (selected) accentPurple else chipBg)
                                        .clickable { overlayMode = id; searchQuery = "" }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) colors.onPrimary else onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        if (!showingQuick && !showingSaved) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search channels...", color = onSurfaceVariant.copy(alpha = 0.4f), fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 14.sp),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentPurple,
                                    unfocusedBorderColor = onSurfaceVariant.copy(alpha = 0.2f),
                                    cursorColor = accentPurple,
                                    focusedContainerColor = chipBg,
                                    unfocusedContainerColor = chipBg,
                                ),
                            )
                        }

                        if (showingHistory) {
                            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                                if (filteredHistoryIndices.isEmpty()) {
                                    item { Text("No history", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                                }
                                items(
                                    items = filteredHistoryIndices,
                                    key = { idx -> "hist_${idx}_${historyIds?.getOrNull(idx) ?: idx}" }
                                ) { idx ->
                                    val histId = historyIds?.getOrNull(idx)
                                    val isFav = histId?.let { favoriteIds?.contains(it) } == true
                                    val chName = historyNames?.getOrNull(idx) ?: entries.getOrNull(idx) ?: ""
                                    var nowNext by remember(histId, chName) { mutableStateOf<Pair<EpgProgram?, EpgProgram?>?>(null) }
                                    LaunchedEffect(histId, chName) {
                                        try {
                                            nowNext = if (epgLoader != null) epgLoader(histId, chName) else null
                                        } catch (_: Throwable) { }
                                    }
                                    ChannelListItem(
                                        index = idx,
                                        name = chName,
                                        logo = historyLogos?.getOrNull(idx),
                                        url = historyUrls?.getOrNull(idx),
                                        isCurrent = false,
                                        isFavorite = isFav,
                                        nowNext = nowNext,
                                        epgEnabled = epgLoader != null,
                                        onTap = {
                                            overlayMode = null
                                            searchQuery = ""
                                            onSwitchChannel(findChannelIndex(historyIds, channelIds, histId).coerceAtLeast(0))
                                        },
                                        onToggleFav = { histId?.let { onToggleFavorite?.invoke(it) } },
                                        onAddMultiView = onAddToMultiView,
                                        accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                        onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                    )
                                }
                            }
                        } else if (showingSaved) {
                            val savedList = remember { ChannelQuickSearchStore.loadTerms() }
                            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                                if (savedList.isEmpty()) {
                                    item { Text("No saved searches yet", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                                }
                                items(
                                    items = savedList,
                                    key = { term -> "saved_$term" }
                                ) { term ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp)).background(chipBg)
                                            .clickable {
                                                searchQuery = term
                                                ChannelQuickSearchStore.addTerm(term)
                                                overlayMode = "list"
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(term, color = accentPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Box(
                                            Modifier.clip(CircleShape).clickable {
                                                ChannelQuickSearchStore.removeTerm(term)
                                                searchQuery = ""
                                            }.padding(6.dp),
                                        ) {
                                            Text("✕", color = onSurfaceVariant, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else if (showingQuick) {
                            QuickChannelsLayered(
                                channelNames = channelNames,
                                channelIds = channelIds,
                                channelUrls = channelUrls,
                                channelLogos = channelLogos,
                                currentChannelIndex = currentChannelIndex,
                                onSwitchChannel = { idx ->
                                    overlayMode = null
                                    searchQuery = ""
                                    onSwitchChannel(idx)
                                },
                                onAddToMultiView = onAddToMultiView,
                                accentPurple = accentPurple,
                                accentPurpleLight = accentPurpleLight,
                                onSurface = onSurface,
                                onSurfaceVariant = onSurfaceVariant,
                                selectedBg = selectedBg,
                                chipBg = chipBg,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            val favIndices = if (!showingFavorites) {
                                filteredListIndices.filter { idx ->
                                    entryIds?.getOrNull(idx)?.let { favoriteIds?.contains(it) } == true
                                }
                            } else emptyList()
                            val nonFavIndices = filteredListIndices.filter { it !in favIndices }

                            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                                if (showingFavorites) {
                                    if (filteredListIndices.isEmpty()) {
                                        item { Text("No favorites yet", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                                    }
                                    items(
                                        items = filteredListIndices,
                                        key = { idx -> "fav_${idx}_${favoriteIndices.getOrNull(idx) ?: idx}" }
                                    ) { idx ->
                                        val realIdx = favoriteIndices.getOrNull(idx) ?: return@items
                                        val chId = channelIds?.getOrNull(realIdx)
                                        val chName = channelNames?.getOrNull(realIdx) ?: entries.getOrNull(idx) ?: ""
                                        var nowNext by remember(chId, chName) { mutableStateOf<Pair<EpgProgram?, EpgProgram?>?>(null) }
                                        LaunchedEffect(chId, chName) {
                                            try {
                                                nowNext = if (epgLoader != null) epgLoader(chId, chName) else null
                                            } catch (_: Throwable) { }
                                        }
                                        ChannelListItem(
                                            index = realIdx,
                                            name = chName,
                                            logo = channelLogos?.getOrNull(realIdx),
                                            url = channelUrls?.getOrNull(realIdx),
                                            isCurrent = realIdx == currentChannelIndex,
                                            isFavorite = true,
                                            nowNext = nowNext,
                                            epgEnabled = epgLoader != null,
                                            onTap = {
                                                overlayMode = null
                                                searchQuery = ""
                                                onSwitchChannel(realIdx)
                                            },
                                            onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                            onAddMultiView = onAddToMultiView,
                                            accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                            onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                        )
                                    }
                                } else {
                                    if (favIndices.isNotEmpty()) {
                                        item {
                                            Text("★ Favorites", color = accentPurpleLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                                        }
                                        items(
                                            items = favIndices,
                                            key = { idx -> "fav_in_list_${idx}_${channelIds?.getOrNull(idx) ?: idx}" }
                                        ) { idx ->
                                            val chId = channelIds?.getOrNull(idx)
                                            val chName = channelNames?.getOrNull(idx) ?: entries.getOrNull(idx) ?: ""
                                            var nowNext by remember(chId, chName) { mutableStateOf<Pair<EpgProgram?, EpgProgram?>?>(null) }
                                            LaunchedEffect(chId, chName) {
                                                try {
                                                    nowNext = if (epgLoader != null) epgLoader(chId, chName) else null
                                                } catch (_: Throwable) { }
                                            }
                                            ChannelListItem(
                                                index = idx, name = chName, logo = entryLogos?.getOrNull(idx),
                                                url = channelUrls?.getOrNull(idx),
                                                isCurrent = idx == currentChannelIndex, isFavorite = true,
                                                nowNext = nowNext,
                                                epgEnabled = epgLoader != null,
                                                onTap = {
                                                    overlayMode = null
                                                    searchQuery = ""
                                                    onSwitchChannel(idx)
                                                },
                                                onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                                onAddMultiView = onAddToMultiView,
                                                accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                                onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                            )
                                        }
                                        if (nonFavIndices.isNotEmpty()) {
                                            item {
                                                HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                                                Text("All Channels", color = onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                                            }
                                        }
                                    }
                                    items(
                                        items = nonFavIndices,
                                        key = { idx -> "ch_${idx}_${channelIds?.getOrNull(idx) ?: idx}" }
                                    ) { idx ->
                                        val chId = channelIds?.getOrNull(idx)
                                        val chName = channelNames?.getOrNull(idx) ?: entries.getOrNull(idx) ?: ""
                                        var nowNext by remember(chId, chName) { mutableStateOf<Pair<EpgProgram?, EpgProgram?>?>(null) }
                                        LaunchedEffect(chId, chName) {
                                            try {
                                                nowNext = if (epgLoader != null) epgLoader(chId, chName) else null
                                            } catch (_: Throwable) { }
                                        }
                                        ChannelListItem(
                                            index = idx, name = chName, logo = entryLogos?.getOrNull(idx),
                                            url = channelUrls?.getOrNull(idx),
                                            isCurrent = idx == currentChannelIndex,
                                            isFavorite = chId?.let { favoriteIds?.contains(it) } == true,
                                            nowNext = nowNext,
                                            epgEnabled = epgLoader != null,
                                            onTap = {
                                                overlayMode = null
                                                searchQuery = ""
                                                onSwitchChannel(idx)
                                            },
                                            onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                            onAddMultiView = onAddToMultiView,
                                            accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                            onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                        )
                                    }
                                    if (filteredListIndices.isEmpty()) {
                                        item { Text("No channels match \"$searchQuery\"", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
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

private fun findChannelIndex(historyIds: List<String>?, channelIds: List<String>?, targetId: String?): Int {
    if (targetId == null || channelIds == null) return 0
    val idx = channelIds.indexOf(targetId)
    return if (idx >= 0) idx else 0
}

@Composable
private fun ChannelListItem(
    index: Int,
    name: String,
    logo: String?,
    url: String?,
    isCurrent: Boolean,
    isFavorite: Boolean,
    nowNext: Pair<EpgProgram?, EpgProgram?>? = null,
    epgEnabled: Boolean = false,
    onTap: () -> Unit,
    onToggleFav: (() -> Unit)?,
    onAddMultiView: ((name: String, url: String, logo: String?) -> Unit)?,
    accentPurpleLight: Color,
    accentPurple: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    selectedBg: Color,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusBg = accentPurple.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(if (isFocused) Modifier.border(2.dp, accentPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onTap)
            .background(if (isCurrent) selectedBg else if (isFocused) focusBg else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFocused) accentPurple.copy(alpha = 0.35f) else accentPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!logo.isNullOrBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Text("${index + 1}", color = accentPurpleLight.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                color = if (isCurrent) accentPurpleLight else onSurface.copy(alpha = 0.85f),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val current = nowNext?.first
            val next = nowNext?.second
            if (nowNext == null && epgEnabled && !isCurrent) {
                Text(
                    text = "Loading guide…",
                    color = onSurfaceVariant.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (current != null && !isCurrent) {
                Text(
                    text = "NOW ${formatEpgTime(current.startTime)} ${current.title}",
                    color = accentPurpleLight.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (next != null) {
                Text(
                    text = "NEXT ${formatEpgTime(next.startTime)} ${next.title}",
                    color = onSurfaceVariant.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onAddMultiView != null && url != null) {
            Text(
                "MW",
                color = accentPurpleLight.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentPurple.copy(alpha = 0.12f))
                    .clickable { onAddMultiView(name, url, logo) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onToggleFav != null) {
            Text(
                if (isFavorite) "♥" else "♡",
                color = if (isFavorite) Color(0xFFE91E63) else onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onToggleFav),
            )
        }
        if (isCurrent) {
            Text("NOW", color = accentPurpleLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
    HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.06f))
}

@Composable
private fun QuickChannelsLayered(
    channelNames: List<String>?,
    channelIds: List<String>?,
    channelUrls: List<String>?,
    channelLogos: List<String>?,
    currentChannelIndex: Int,
    onSwitchChannel: (Int) -> Unit,
    onAddToMultiView: ((name: String, url: String, logo: String?) -> Unit)?,
    accentPurple: Color,
    accentPurpleLight: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    selectedBg: Color,
    chipBg: Color,
    modifier: Modifier = Modifier,
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedQc by remember { mutableStateOf<QuickChannel?>(null) }
    var subChannelSearch by remember { mutableStateOf("") }
    var playlistSearch by remember { mutableStateOf("") }

    val categories = remember { QuickChannelList.categories }

    // Layer 3: Playlist Streams for selected QuickChannel
    if (selectedQc != null) {
        val qc = selectedQc!!
        val names = channelNames ?: emptyList()
        val urls = channelUrls ?: emptyList()
        val logos = channelLogos ?: emptyList()
        val ids = channelIds ?: emptyList()

        // Matched indices in current playlist calculated off main thread
        val matchedIndices by produceState(initialValue = emptyList<Int>(), key1 = qc, key2 = names) {
            value = withContext(Dispatchers.Default) {
                names.mapIndexedNotNull { idx, n ->
                    val ch = IptvChannel(
                        id = ids.getOrNull(idx) ?: "",
                        name = n,
                        logo = logos.getOrNull(idx),
                        group = null,
                        url = urls.getOrNull(idx) ?: "",
                        sourceType = SourceType.M3U,
                        sourceId = "unknown",
                    )
                    if (QuickChannelList.matches(qc, ch)) idx else null
                }
            }
        }

        val filteredIndices = remember(matchedIndices, playlistSearch, names) {
            val q = playlistSearch.trim().lowercase()
            if (q.isEmpty()) matchedIndices
            else matchedIndices.filter { idx -> (names.getOrNull(idx) ?: "").lowercase().contains(q) }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .clickable { selectedQc = null; playlistSearch = "" }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("← Back", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(qc.displayName, color = onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${matchedIndices.size} streams in playlist", color = onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }

            // Playlist search
            OutlinedTextField(
                value = playlistSearch,
                onValueChange = { playlistSearch = it },
                placeholder = { Text("Filter ${qc.displayName} streams...", color = onSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentPurple,
                    unfocusedBorderColor = onSurfaceVariant.copy(alpha = 0.2f),
                    cursorColor = accentPurple,
                    focusedContainerColor = chipBg,
                    unfocusedContainerColor = chipBg,
                ),
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                if (filteredIndices.isEmpty()) {
                    item {
                        Text(
                            if (matchedIndices.isEmpty()) "No streams found in your playlist matching \"${qc.displayName}\""
                            else "No streams match \"$playlistSearch\"",
                            color = onSurfaceVariant, fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                items(
                    items = filteredIndices,
                    key = { idx -> "pl_stream_${idx}_${ids.getOrNull(idx) ?: idx}" }
                ) { idx ->
                    val streamName = names.getOrNull(idx) ?: qc.displayName
                    val streamLogo = logos.getOrNull(idx)
                    val streamUrl = urls.getOrNull(idx)
                    val isCurrent = idx == currentChannelIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) selectedBg else chipBg.copy(alpha = 0.5f))
                            .clickable { onSwitchChannel(idx) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!streamLogo.isNullOrBlank()) {
                            AsyncImage(
                                model = streamLogo,
                                contentDescription = streamName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1A1A)),
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Box(
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(accentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${idx + 1}", color = accentPurpleLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = streamName,
                                color = if (isCurrent) accentPurpleLight else onSurface,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        if (onAddToMultiView != null && streamUrl != null) {
                            Text(
                                "MW",
                                color = accentPurpleLight.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentPurple.copy(alpha = 0.12f))
                                    .clickable { onAddToMultiView(streamName, streamUrl, streamLogo) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }

                        Text(
                            if (isCurrent) "NOW" else "Zap",
                            color = if (isCurrent) accentPurpleLight else accentPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
    // Layer 2: Sub-channels list in selected Category
    else if (selectedCategoryId != null) {
        val catId = selectedCategoryId!!
        val catObj = categories.find { it.id == catId }
        val catTitle = catObj?.title ?: catId
        val subChannels = remember(catId) { QuickChannelList.getChannelsForCategory(catId) }
        val filteredSub = remember(subChannels, subChannelSearch) {
            val q = subChannelSearch.trim().lowercase()
            if (q.isEmpty()) subChannels
            else subChannels.filter { qc ->
                qc.displayName.lowercase().contains(q) || qc.aliases.any { it.lowercase().contains(q) }
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .clickable { selectedCategoryId = null; subChannelSearch = "" }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text("← Categories", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(catTitle, color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${subChannels.size} sub-channels", color = onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }

            // Sub-channel search
            OutlinedTextField(
                value = subChannelSearch,
                onValueChange = { subChannelSearch = it },
                placeholder = { Text("Search in $catTitle...", color = onSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 13.sp),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentPurple,
                    unfocusedBorderColor = onSurfaceVariant.copy(alpha = 0.2f),
                    cursorColor = accentPurple,
                    focusedContainerColor = chipBg,
                    unfocusedContainerColor = chipBg,
                ),
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                if (filteredSub.isEmpty()) {
                    item {
                        Text("No sub-channels match \"$subChannelSearch\"", color = onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    }
                }

                items(
                    items = filteredSub,
                    key = { qc -> "sub_${qc.displayName}" }
                ) { qc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBg.copy(alpha = 0.6f))
                            .clickable { selectedQc = qc }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(qc.displayName, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (qc.regions.isNotEmpty()) {
                                Text(qc.regions.joinToString(", "), color = onSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("▸", color = accentPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    // Layer 1: Categories (All, US, UK, CA, Bay Area, Premium, Sports, News)
    else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    "Quick Channel Categories",
                    color = onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(
                items = categories,
                key = { it.id }
            ) { cat ->
                val count = remember(cat.id) { QuickChannelList.getChannelsForCategory(cat.id).size }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .clickable { selectedCategoryId = cat.id }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            cat.title.take(2).uppercase(),
                            color = accentPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.title, color = onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(cat.description, color = onSurfaceVariant.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentPurple.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("$count", color = accentPurpleLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(6.dp))
                    Text("▸", color = onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                }
            }
        }
    }
}

private fun formatEpgTime(epochMs: Long): String {
    val totalSeconds = epochMs / 1000L
    val hours = ((totalSeconds / 3600) % 24).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
