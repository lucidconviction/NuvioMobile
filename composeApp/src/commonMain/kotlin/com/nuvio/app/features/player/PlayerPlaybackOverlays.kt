package com.nuvio.app.features.player

import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.SportEvent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        val dlEvents = SportsNowStore.daddyLiveEvents
        val hasAny = espnEvents.isNotEmpty() || dlEvents.isNotEmpty()

        var pickerEvent by remember { mutableStateOf<Pair<String, List<IptvChannel>>?>(null) }

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
                        itemsIndexed(channels) { _, ch ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(8.dp)).clickable {
                                    pickerEvent = null
                                    SportsNowStore.onSwitchToEvent?.invoke(
                                        espnEvents.firstOrNull { e ->
                                            ch.name.lowercase().contains(e.channel.lowercase()) || e.channel.lowercase().contains(ch.name.lowercase())
                                        } ?: return@clickable
                                    )
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                    if (!ch.group.isNullOrBlank()) {
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
                                            val matches = IptvRepository.getAllChannels().let { allCh ->
                                                EspnClient.findAllMatchingChannels(se.first(), allCh)
                                            }
                                            if (matches.isEmpty()) {
                                                SportsNowStore.onSwitchToEvent?.invoke(event)
                                            } else if (matches.size == 1) {
                                                SportsNowStore.onSwitchToEvent?.invoke(event)
                                            } else {
                                                pickerEvent = event.title to matches
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
        val accentPurple = Color(0xFF7C3AED)
        val accentPurpleLight = Color(0xFFD2BBFF)
        val onSurface = Color(0xFFDAE2FD)
        val onSurfaceVariant = Color(0xFFCCC3D8)
        val selectedBg = Color(0xFF2D3449).copy(alpha = 0.5f)

        val hasHistory = historyNames != null && historyNames.isNotEmpty()
        val hasFavorites = favoriteIds != null && favoriteIds.isNotEmpty()

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
                    .clickable { overlayMode = null; searchQuery = "" },
            )

            AnimatedVisibility(
                visible = overlayMode != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val listState = rememberLazyListState()
                var lastScrollTime by remember(overlayMode) { mutableStateOf(0L) }
                LaunchedEffect(overlayMode) {
                    if (currentChannelIndex > 0 && overlayMode == "list") {
                        listState.animateScrollToItem(currentChannelIndex)
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
                        if (idleMs >= 8_000L) {
                            overlayMode = null
                            searchQuery = ""
                            break
                        }
                    }
                }

                val showingHistory = overlayMode == "history"
                val showingFavorites = overlayMode == "favorites"
                val entries = when (overlayMode) {
                    "history" -> historyNames ?: emptyList()
                    "favorites" -> {
                        val favIndices = channelNames.indices.filter {
                            channelIds?.getOrNull(it)?.let { id -> favoriteIds?.contains(id) } == true
                        }
                        favIndices.map { channelNames[it] }
                    }
                    else -> channelNames
                }
                val entryUrls = when (overlayMode) {
                    "history" -> historyUrls ?: emptyList()
                    "favorites" -> emptyList()
                    else -> channelUrls
                }
                val entryLogos = when (overlayMode) {
                    "history" -> historyLogos ?: emptyList()
                    "favorites" -> emptyList()
                    else -> channelLogos
                }
                val entryIds = when (overlayMode) {
                    "history" -> historyIds ?: emptyList()
                    "favorites" -> emptyList()
                    else -> channelIds
                }

                val query = searchQuery.trim().lowercase()
                val filteredHistoryIndices = if (showingHistory) {
                    entries.indices.filter { query.isEmpty() || entries[it].lowercase().contains(query) }
                } else emptyList()
                val filteredListIndices = if (!showingHistory) {
                    entries.indices.filter { query.isEmpty() || entries[it].lowercase().contains(query) }
                } else emptyList()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize()
                        .background(Color(0xFF0B1326).copy(alpha = 0.20f))
                        .padding(bottom = overlayBottomPadding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000000).copy(alpha = 0.25f))
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when (overlayMode) {
                                    "favorites" -> "⭐ Favorites"
                                    "history" -> "🕐 History"
                                    else -> "📋 Channels"
                                },
                                color = onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${if (showingHistory) filteredHistoryIndices.size else filteredListIndices.size}",
                                    color = onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
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

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...", color = onSurfaceVariant.copy(alpha = 0.3f), fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 14.sp),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentPurple,
                                unfocusedBorderColor = onSurfaceVariant.copy(alpha = 0.2f),
                                cursorColor = accentPurple,
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.6f),
                            ),
                        )

                        if (showingHistory) {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                if (filteredHistoryIndices.isEmpty()) {
                                    item { Text("No history", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                                }
                                filteredHistoryIndices.forEach { idx ->
                                    item(key = "hist_$idx") {
                                        val histId = historyIds?.getOrNull(idx)
                                        val isFav = histId?.let { favoriteIds?.contains(it) } == true
                                        ChannelListItem(
                                            index = idx,
                                            name = entries[idx],
                                            logo = historyLogos?.getOrNull(idx),
                                            url = historyUrls?.getOrNull(idx),
                                            isCurrent = false,
                                            isFavorite = isFav,
                                            onTap = { overlayMode = null; onSwitchChannel(findChannelIndex(historyIds, channelIds, histId).coerceAtLeast(0)) },
                                            onToggleFav = { histId?.let { onToggleFavorite?.invoke(it) } },
                                            onAddMultiView = onAddToMultiView,
                                            accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                            onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                        )
                                    }
                                }
                            }
                        } else {
                            val favIndices = if (!showingFavorites) {
                                filteredListIndices.filter { idx ->
                                    entryIds?.getOrNull(idx)?.let { favoriteIds?.contains(it) } == true
                                }
                            } else emptyList()
                            val nonFavIndices = filteredListIndices.filter { it !in favIndices }

                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                                if (showingFavorites) {
                                    if (filteredListIndices.isEmpty()) {
                                        item { Text("No favorites yet", color = onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(20.dp)) }
                                    }
                                    filteredListIndices.forEach { idx ->
                                        item(key = "fav_$idx") {
                                            val chId = channelIds?.getOrNull(idx)
                                            ChannelListItem(
                                                index = idx,
                                                name = entries[idx],
                                                logo = entryLogos?.getOrNull(idx),
                                                url = channelUrls?.getOrNull(idx),
                                                isCurrent = idx == currentChannelIndex && !showingFavorites,
                                                isFavorite = true,
                                                onTap = { overlayMode = null; onSwitchChannel(idx) },
                                                onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                                onAddMultiView = onAddToMultiView,
                                                accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                                onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                            )
                                        }
                                    }
                                } else {
                                    if (favIndices.isNotEmpty()) {
                                        item {
                                            Text("★ Favorites", color = accentPurpleLight, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                                        }
                                        favIndices.forEach { idx ->
                                            item(key = "fav_$idx") {
                                                val chId = channelIds?.getOrNull(idx)
                                                ChannelListItem(
                                                    index = idx, name = entries[idx], logo = entryLogos?.getOrNull(idx),
                                                    url = channelUrls?.getOrNull(idx),
                                                    isCurrent = idx == currentChannelIndex, isFavorite = true,
                                                    onTap = { overlayMode = null; onSwitchChannel(idx) },
                                                    onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                                    onAddMultiView = onAddToMultiView,
                                                    accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                                    onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                                )
                                            }
                                        }
                                        if (nonFavIndices.isNotEmpty()) {
                                            item {
                                                HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                                                Text("All Channels", color = onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                                            }
                                        }
                                    }
                                    nonFavIndices.forEach { idx ->
                                        item(key = "ch_$idx") {
                                            val chId = channelIds?.getOrNull(idx)
                                            ChannelListItem(
                                                index = idx, name = entries[idx], logo = entryLogos?.getOrNull(idx),
                                                url = channelUrls?.getOrNull(idx),
                                                isCurrent = idx == currentChannelIndex,
                                                isFavorite = chId?.let { favoriteIds?.contains(it) } == true,
                                                onTap = { overlayMode = null; onSwitchChannel(idx) },
                                                onToggleFav = { chId?.let { onToggleFavorite?.invoke(it) } },
                                                onAddMultiView = onAddToMultiView,
                                                accentPurpleLight = accentPurpleLight, accentPurple = accentPurple,
                                                onSurface = onSurface, onSurfaceVariant = onSurfaceVariant, selectedBg = selectedBg,
                                            )
                                        }
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
    return channelIds.indexOf(targetId).coerceAtLeast(0)
}

@Composable
private fun PopupOption(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFFDAE2FD),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    )
}

@Composable
private fun ChannelListItem(
    index: Int,
    name: String,
    logo: String?,
    url: String?,
    isCurrent: Boolean,
    isFavorite: Boolean,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
        Text(
            text = name,
            color = if (isCurrent) accentPurpleLight else onSurface.copy(alpha = 0.85f),
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
