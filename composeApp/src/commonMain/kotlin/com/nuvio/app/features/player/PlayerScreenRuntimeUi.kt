package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged

import com.nuvio.app.features.iptv.EpgProgram
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.trakt.TraktPlatformClock

import com.nuvio.app.features.hub.MultiWindowPositionPicker
import com.nuvio.app.features.hub.MultiWindowStore
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.p2p.P2pStreamingState
import com.nuvio.app.features.p2p.formatP2pMegabytes
import com.nuvio.app.features.p2p.formatP2pSpeed
import com.nuvio.app.isIos
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*

@Composable
internal fun PlayerScreenRuntime.RenderPlayerRuntimeUi() {
    val runtime = this
    val displayedPositionMs = scrubbingPositionMs ?: playbackSnapshot.positionMs
    val isEpisode = activeSeasonNumber != null && activeEpisodeNumber != null
    val currentGestureFeedback = liveGestureFeedback ?: gestureFeedback
    val isP2pPlaybackActive = activeTorrentInfoHash != null
    val p2pStats = p2pStreamingState as? P2pStreamingState.Streaming
    val p2pPeerInfo = p2pStats?.let { stats ->
        org.jetbrains.compose.resources.stringResource(
            nuvio.composeapp.generated.resources.Res.string.player_torrent_peer_info,
            stats.seeds,
            stats.peers,
        )
    }
    val p2pDownloadSpeed = p2pStats?.let { formatP2pSpeed(it.downloadSpeed) }
    val p2pInitialLoadingMessage = when {
        !isP2pPlaybackActive || initialLoadCompleted -> null
        p2pStreamingState is P2pStreamingState.Connecting -> {
            org.jetbrains.compose.resources.stringResource(
                nuvio.composeapp.generated.resources.Res.string.player_torrent_connecting_peers,
            )
        }
        p2pStats != null -> {
            if (p2pSettingsUiState.hideTorrentStats) {
                null
            } else {
                org.jetbrains.compose.resources.stringResource(
                    nuvio.composeapp.generated.resources.Res.string.player_torrent_buffered_status,
                    formatP2pMegabytes(p2pStats.preloadedBytes),
                    p2pPeerInfo.orEmpty(),
                    p2pDownloadSpeed.orEmpty(),
                )
            }
        }
        else -> org.jetbrains.compose.resources.stringResource(
            nuvio.composeapp.generated.resources.Res.string.player_torrent_starting_engine,
        )
    }
    val p2pInitialLoadingProgress = when {
        !isP2pPlaybackActive || initialLoadCompleted || p2pStats == null -> null
        else -> (p2pStats.preloadedBytes.toFloat() / P2pInitialPreloadTargetBytes.toFloat()).coerceIn(0f, 1f)
    }
    val showP2pRebufferStats = isP2pPlaybackActive &&
        initialLoadCompleted &&
        playbackSnapshot.isLoading &&
        p2pStats != null &&
        !p2pSettingsUiState.hideTorrentStats
    val p2pRebufferMessage = when {
        !showP2pRebufferStats -> null
        else -> {
            val bufferedSeconds = ((playbackSnapshot.bufferedPositionMs - playbackSnapshot.positionMs) / 1000L)
                .coerceAtLeast(0L)
            "${bufferedSeconds}s buffered · ${p2pPeerInfo.orEmpty()} · ${p2pDownloadSpeed.orEmpty()}"
        }
    }
    val p2pRebufferProgress = when {
        !showP2pRebufferStats -> null
        else -> {
            val bufferedSeconds = ((playbackSnapshot.bufferedPositionMs - playbackSnapshot.positionMs) / 1000f)
                .coerceAtLeast(0f)
            (bufferedSeconds / 10f).coerceIn(0f, 1f)
        }
    }
    val gestureCallbacks = rememberSurfaceGestureCallbacks()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { layoutSize = it }
            .playerSurfaceTapGestures(
                layoutSize = layoutSize,
                playerControlsLockedState = gestureCallbacks.playerControlsLocked,
                onSurfaceTap = gestureCallbacks.onSurfaceTap,
                onSurfaceDoubleTap = gestureCallbacks.onSurfaceDoubleTap,
                activateHoldToSpeedState = gestureCallbacks.activateHoldToSpeed,
                deactivateHoldToSpeedState = gestureCallbacks.deactivateHoldToSpeed,
                revealLockedOverlayState = gestureCallbacks.revealLockedOverlay,
            )
            .playerSurfaceDragGestures(
                gestureController = gestureController,
                layoutSize = layoutSize,
                sideGestureSystemEdgeExclusionPx = sideGestureSystemEdgeExclusionPx,
                playerControlsLockedState = gestureCallbacks.playerControlsLocked,
                touchGesturesEnabledState = gestureCallbacks.touchGesturesEnabled,
                isHoldToSpeedGestureActiveState = gestureCallbacks.isHoldToSpeedGestureActive,
                currentPositionMsState = gestureCallbacks.currentPositionMs,
                currentDurationMsState = gestureCallbacks.currentDurationMs,
                deactivateHoldToSpeedState = gestureCallbacks.deactivateHoldToSpeed,
                showHorizontalSeekPreviewState = gestureCallbacks.showHorizontalSeekPreview,
                showBrightnessFeedbackState = gestureCallbacks.showBrightnessFeedback,
                showVolumeFeedbackState = gestureCallbacks.showVolumeFeedback,
                clearLiveGestureFeedbackState = gestureCallbacks.clearLiveGestureFeedback,
                revealLockedOverlayState = gestureCallbacks.revealLockedOverlay,
                commitHorizontalSeekState = gestureCallbacks.commitHorizontalSeek,
            ),
    ) {
        val playerSurfaceSourceUrl = if (isP2pPlaybackActive) p2pResolvedSourceUrl else activeSourceUrl
        if (playerSurfaceSourceUrl != null) {
            PlatformPlayerSurface(
                sourceUrl = playerSurfaceSourceUrl,
                sourceAudioUrl = activeSourceAudioUrl,
                sourceHeaders = activeSourceHeaders,
                sourceResponseHeaders = activeSourceResponseHeaders,
                externalSubtitles = externalSubtitles,
                streamType = activeStreamType,
                modifier = Modifier.fillMaxSize(),
                playWhenReady = shouldPlay,
                resizeMode = resizeMode,
                onControllerReady = { controller ->
                    playerController = controller
                    playerControllerSourceUrl = activeSourceUrl
                },
                onSnapshot = { snapshot ->
                    playbackSnapshot = snapshot
                    if (!snapshot.isLoading) initialLoadCompleted = true
                    if (snapshot.isEnded) {
                        shouldPlay = false
                        controlsVisible = !playerControlsLocked
                    }
                },
                onError = { message ->
                    if (message != null && tryRefreshCredentialedSourceAfterError(message)) {
                        return@PlatformPlayerSurface
                    }
                    errorMessage = message
                    if (message != null) {
                        controlsVisible = !playerControlsLocked
                        removeFailedStreamFromCache()
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = pausedOverlayVisible && !controlsVisible && !playerControlsLocked,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        ) {
            PauseMetadataOverlay(
                title = title,
                logo = logo,
                isEpisode = isEpisode,
                seasonNumber = activeSeasonNumber,
                episodeNumber = activeEpisodeNumber,
                episodeTitle = activeEpisodeTitle,
                pauseDescription = pauseDescription ?: activeStreamSubtitle,
                providerName = activeProviderName,
                metrics = metrics,
                horizontalSafePadding = horizontalSafePadding,
                modifier = Modifier.fillMaxSize(),
            )
        }

        RenderPlayerControls(displayedPositionMs = displayedPositionMs, isEpisode = isEpisode)
        RenderPlaybackOverlays(
            runtime = runtime,
            displayedPositionMs = displayedPositionMs,
            currentGestureFeedback = currentGestureFeedback,
            p2pInitialLoadingMessage = p2pInitialLoadingMessage,
            p2pInitialLoadingProgress = p2pInitialLoadingProgress,
            showP2pRebufferStats = showP2pRebufferStats,
            p2pRebufferMessage = p2pRebufferMessage,
            p2pRebufferProgress = p2pRebufferProgress,
        )
        

        // IPTV channel navigation arrows (hideaway, shown with controls)
        val iptvChUrls = args.iptvChannelUrls
        val isIptvPlayback = args.parentMetaId == "iptv" && iptvChUrls != null && iptvChUrls.isNotEmpty()
        if (isIptvPlayback) {
            LaunchedEffect(controlsVisible) {
                if (controlsVisible) iptvChannelNavVisible = true
            }
            LaunchedEffect(iptvChannelNavVisible, iptvChannelNavTouch) {
                if (iptvChannelNavVisible) {
                    delay(3000)
                    iptvChannelNavVisible = false
                }
            }
        }
        if (isIptvPlayback && controlsVisible && iptvChannelNavVisible) {
            val chIdx = args.iptvCurrentChannelIndex
            val chUrls = iptvChUrls
            val chNames = args.iptvChannelNames
            val chLogos = args.iptvChannelLogos
            val hasPrevCh = chIdx > 0
            val hasNextCh = chIdx < chUrls.size - 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 4.dp, end = 4.dp),
            ) {
                if (hasPrevCh) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                iptvChannelNavTouch++
                                val newIdx = chIdx - 1
                                val newLogo = chLogos?.getOrNull(newIdx).takeIf { !it.isNullOrBlank() }
                                val newLaunch = PlayerLaunchStore.get(args.launchId)?.copy(
                                    title = chNames?.getOrNull(newIdx) ?: "",
                                    sourceUrl = chUrls[newIdx],
                                    streamTitle = chNames?.getOrNull(newIdx) ?: "",
                                    currentChannelIndex = newIdx,
                                    logo = newLogo,
                                    poster = newLogo,
                                    initialPositionMs = 0L,
                                    initialProgressFraction = null,
                                )
                                if (newLaunch != null) {
                                    flushWatchProgress()
                                    args.onSwitchIptvChannel?.invoke(PlayerLaunchStore.put(newLaunch))
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\u25C0", color = Color.White, fontSize = 20.sp)
                    }
                }
                if (hasNextCh) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                iptvChannelNavTouch++
                                val newIdx = chIdx + 1
                                val newLogo = chLogos?.getOrNull(newIdx).takeIf { !it.isNullOrBlank() }
                                val newLaunch = PlayerLaunchStore.get(args.launchId)?.copy(
                                    title = chNames?.getOrNull(newIdx) ?: "",
                                    sourceUrl = chUrls[newIdx],
                                    streamTitle = chNames?.getOrNull(newIdx) ?: "",
                                    currentChannelIndex = newIdx,
                                    logo = newLogo,
                                    poster = newLogo,
                                    initialPositionMs = 0L,
                                    initialProgressFraction = null,
                                )
                                if (newLaunch != null) {
                                    flushWatchProgress()
                                    args.onSwitchIptvChannel?.invoke(PlayerLaunchStore.put(newLaunch))
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\u25B6", color = Color.White, fontSize = 20.sp)
                    }
                }
            }
        }

        if (args.parentMetaId == "iptv") {
            IptvEpgOverlay(
                sourceUrl = activeSourceUrl,
                visible = controlsVisible && !playerControlsLocked,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = overlayBottomPadding + 110.dp),
            )
        }

        RenderPlayerModals(displayedPositionMs = displayedPositionMs)

        // Toast overlay for user feedback
        val toastMessage = multiToastMessage
        if (toastMessage != null) {
            LaunchedEffect(toastMessage) {
                kotlinx.coroutines.delay(2000)
                multiToastMessage = null
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = toastMessage,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

internal fun PlayerScreenRuntime.navigateQueue(newIndex: Int, queueUrls: List<String>, queueTitles: List<String>) {
    val nextEntry = queueUrls.getOrNull(newIndex) ?: return
    val nextTitle = queueTitles.getOrElse(newIndex) { "" }
    val resolved = if (nextEntry.startsWith("yt://")) {
        runCatching { runBlocking { com.nuvio.app.features.sports.YouTubeStreamResolver.resolveStream(nextEntry.removePrefix("yt://")) } }.getOrNull()
    } else null
    val nextLaunch = PlayerLaunch(
        profileId = args.profileId, title = nextTitle,
        sourceUrl = resolved?.url ?: nextEntry,
        sourceHeaders = resolved?.headers ?: emptyMap(),
        sourceAudioUrl = resolved?.audioUrl,
        qualities = resolved?.qualities ?: emptyList(),
        streamTitle = nextTitle,
        streamSubtitle = args.streamSubtitle,
        providerName = if (nextEntry.startsWith("yt://")) "YouTube" else args.providerName,
        parentMetaId = args.parentMetaId, parentMetaType = args.parentMetaType,
        poster = args.poster, logo = args.logo,
        autoPlayQueueUrls = queueUrls, autoPlayQueueTitles = queueTitles,
        autoPlayQueueIndex = newIndex,
    )
    flushWatchProgress()
    val onSwitch = if (args.parentMetaId == "iptv") args.onSwitchIptvChannel else args.onAutoPlayNext
    onSwitch?.invoke(PlayerLaunchStore.put(nextLaunch))
}

@Composable
private fun PlayerScreenRuntime.RenderPlayerControls(displayedPositionMs: Long, isEpisode: Boolean) {
    AnimatedVisibility(
        visible = (controlsVisible || showParentalGuide) && !playerControlsLocked,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        PlayerControlsShell(
            title = title,
            streamTitle = activeStreamTitle,
            providerName = activeProviderName,
            seasonNumber = activeSeasonNumber,
            episodeNumber = activeEpisodeNumber,
            episodeTitle = activeEpisodeTitle,
            playbackSnapshot = playbackSnapshot,
            displayedPositionMs = displayedPositionMs,
            metrics = metrics,
            resizeMode = resizeMode,
            isLocked = playerControlsLocked,
            showPlaybackControls = controlsVisible,
            onLockToggle = {
                if (playerControlsLocked) unlockPlayerControls() else lockPlayerControls()
            },
            onBack = {
                flushWatchProgress()
                args.onBack()
            },
            onTogglePlayback = { togglePlayback() },
            onSeekBack = { seekBy(-10_000L) },
            onSeekForward = { seekBy(10_000L) },
            onPrev = if (args.autoPlayQueueIndex > 0) { { navigateQueue(args.autoPlayQueueIndex - 1, args.autoPlayQueueUrls, args.autoPlayQueueTitles) } } else null,
            onNext = if (args.autoPlayQueueIndex < args.autoPlayQueueUrls.size - 1) { { navigateQueue(args.autoPlayQueueIndex + 1, args.autoPlayQueueUrls, args.autoPlayQueueTitles) } } else null,
            onResizeModeClick = { cycleResizeMode() },
            onSpeedClick = { cyclePlaybackSpeed() },
            onSubtitleClick = {
                refreshTracks()
                showSubtitleModal = true
            },
            onAudioClick = {
                refreshTracks()
                showAudioModal = true
            },
            onHistoryClick = null,
            onVideoSettingsClick = if (isIos) {
                {
                    showVideoSettingsModal = true
                    controlsVisible = true
                }
            } else {
                null
            },
            onLiveGamesClick = if (SportsNowStore.liveEvents.isNotEmpty()) { { showLiveGamesOverlay = !showLiveGamesOverlay } } else null,
            onSourcesClick = if (activeVideoId != null) { { openSourcesPanel() } } else null,
            onChannelsClick = { channelOverlayTrigger++ },
            onEpisodesClick = if (isSeries) { { openEpisodesPanel() } } else null,
            // Volume button removed as requested
            onQualityClick = if (qualities.isNotEmpty()) { { showQualitySelector = !showQualitySelector } } else null,
            onMultiViewClick = { showMultiViewPicker = true },
            showVolumeSlider = showVolumeSlider,
            volume = volume,
            onVolumeChanged = { v ->
                volume = v
                playerController?.setVolume(v)
            },
            qualityLabel = qualities.getOrNull(selectedQualityIndex)?.let { "${it.height}p" },
            onOpenInExternalPlayer = args.onOpenInExternalPlayer?.let { openExternal ->
                {
                    val loadedSubtitles = addonSubtitles
                        .takeIf { it.isNotEmpty() }
                        ?.map { sub ->
                            SubtitleInput(
                                url = sub.url,
                                name = buildString {
                                    if (!sub.addonName.isNullOrBlank()) append("[${sub.addonName}] ")
                                    append(sub.display)
                                },
                                lang = sub.language,
                            )
                        }
                    openExternal(
                        ExternalPlayerPlaybackRequest(
                            sourceUrl = activeSourceUrl,
                            title = title,
                            streamTitle = activeStreamTitle,
                            sourceHeaders = activeSourceHeaders,
                            resumePositionMs = playbackSnapshot.positionMs,
                            subtitles = loadedSubtitles,
                            season = activeSeasonNumber,
                            episode = activeEpisodeNumber,
                            episodeTitle = activeEpisodeTitle,
                        ),
                    )
                }
            },
            onSubmitIntroClick = if (
                isSeries &&
                playerSettingsUiState.introSubmitEnabled &&
                playerSettingsUiState.introDbApiKey.isNotBlank()
            ) {
                { showSubmitIntroModal = true }
            } else {
                null
            },
            parentalWarnings = parentalWarnings,
            showParentalGuide = showParentalGuide,
            onParentalGuideAnimationComplete = { showParentalGuide = false },
            onScrubChange = { positionMs ->
                isScrubbingTimeline = true
                scrubbingPositionMs = positionMs
            },
            onScrubFinished = { positionMs ->
                isScrubbingTimeline = false
                scrubbingPositionMs = null
                playerController?.seekTo(positionMs)
                scheduleProgressSyncAfterSeek()
            },
            horizontalSafePadding = horizontalSafePadding,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BoxScope.RenderPlaybackOverlays(
    runtime: PlayerScreenRuntime,
    displayedPositionMs: Long,
    currentGestureFeedback: GestureFeedbackState?,
    p2pInitialLoadingMessage: String?,
    p2pInitialLoadingProgress: Float?,
    showP2pRebufferStats: Boolean,
    p2pRebufferMessage: String?,
    p2pRebufferProgress: Float?,
) {
    var showChannelMultiViewPicker by remember { mutableStateOf(false) }
    var channelPickerName by remember { mutableStateOf("") }
    var channelPickerUrl by remember { mutableStateOf("") }
    var channelPickerLogo by remember { mutableStateOf<String?>(null) }

    runtime.run {
        val iptvLaunch = remember(args.launchId) { PlayerLaunchStore.get(args.launchId) }
        var iptvChannelNames = iptvLaunch?.channelNames ?: args.iptvChannelNames
        var iptvChannelUrls = iptvLaunch?.channelUrls ?: args.iptvChannelUrls
        var iptvChannelLogos = iptvLaunch?.channelLogos ?: args.iptvChannelLogos
        var iptvChannelIds = iptvLaunch?.channelIds ?: args.iptvChannelIds
        if (iptvChannelNames == null && args.parentMetaId == "iptv") {
            val allCh = com.nuvio.app.features.iptv.IptvRepository.getAllChannels()
            iptvChannelNames = allCh.map { it.name }
            iptvChannelUrls = allCh.map { it.url }
            iptvChannelLogos = allCh.map { it.logo ?: "" }
            iptvChannelIds = allCh.map { it.id }
        }
        val iptvFavoriteIds = args.iptvFavoriteIds
        val iptvHistoryNames = args.iptvHistoryNames
        val iptvHistoryUrls = args.iptvHistoryUrls
        val iptvHistoryLogos = args.iptvHistoryLogos
        val iptvHistoryIds = args.iptvHistoryIds
        val iptvCurrentChannelIndex = iptvLaunch?.currentChannelIndex ?: iptvChannelUrls?.indexOfFirst { it == args.sourceUrl }?.coerceAtLeast(0) ?: 0

        PlayerPlaybackOverlays(
            channelOverlayTrigger = channelOverlayTrigger,
            historyOverlayTrigger = historyOverlayTrigger,
            showLiveGamesOverlay = showLiveGamesOverlay,
            onDismissLiveGames = { showLiveGamesOverlay = false },
            playerControlsLocked = playerControlsLocked,
            lockedOverlayVisible = lockedOverlayVisible,
            playbackSnapshot = playbackSnapshot,
            displayedPositionMs = displayedPositionMs,
            metrics = metrics,
            horizontalSafePadding = horizontalSafePadding,
            onUnlock = { unlockPlayerControls() },
            showOpeningOverlay = playerSettingsUiState.showLoadingOverlay && !initialLoadCompleted && errorMessage == null,
            backdropArtwork = background ?: poster,
            logo = logo,
            title = title,
            onBackWithProgress = {
                flushWatchProgress()
                args.onBack()
            },
            p2pInitialLoadingMessage = p2pInitialLoadingMessage,
            p2pInitialLoadingProgress = p2pInitialLoadingProgress,
            showP2pRebufferStats = showP2pRebufferStats,
            p2pRebufferMessage = p2pRebufferMessage,
            p2pRebufferProgress = p2pRebufferProgress,
            currentGestureFeedback = currentGestureFeedback,
            renderedGestureFeedback = renderedGestureFeedback,
            initialLoadCompleted = initialLoadCompleted,
            pausedOverlayVisible = pausedOverlayVisible,
            activeSkipInterval = activeSkipInterval,
            skipIntervalDismissed = skipIntervalDismissed,
            controlsVisible = controlsVisible,
            onSkipInterval = { interval ->
                playerController?.seekTo((interval.endTime * 1000).toLong())
                scheduleProgressSyncAfterSeek()
                skipIntervalDismissed = true
            },
            onDismissSkipInterval = { skipIntervalDismissed = true },
            sliderEdgePadding = sliderEdgePadding,
            overlayBottomPadding = overlayBottomPadding,
            isSeries = isSeries,
            nextEpisodeInfo = nextEpisodeInfo,
            showNextEpisodeCard = showNextEpisodeCard,
            nextEpisodeAutoPlaySearching = nextEpisodeAutoPlaySearching,
            nextEpisodeAutoPlaySourceName = nextEpisodeAutoPlaySourceName,
            nextEpisodeAutoPlayCountdown = nextEpisodeAutoPlayCountdown,
            onPlayNextEpisode = {
                nextEpisodeAutoPlayJob?.cancel()
                playNextEpisode()
            },
            onDismissNextEpisode = {
                nextEpisodeAutoPlayJob?.cancel()
                showNextEpisodeCard = false
                nextEpisodeAutoPlaySearching = false
                nextEpisodeAutoPlaySourceName = null
                nextEpisodeAutoPlayCountdown = null
            },
            errorMessage = errorMessage,
            onDismissError = {
                flushWatchProgress()
                args.onBack()
            },
            channelNames = iptvChannelNames,
            channelUrls = iptvChannelUrls,
            channelLogos = iptvChannelLogos,
            channelIds = iptvChannelIds,
            favoriteIds = iptvFavoriteIds,
            historyNames = iptvHistoryNames,
            historyUrls = iptvHistoryUrls,
            historyLogos = iptvHistoryLogos,
            historyIds = iptvHistoryIds,
            currentChannelIndex = iptvCurrentChannelIndex,
            onToggleFavorite = { channelId ->
                com.nuvio.app.features.iptv.IptvRepository.toggleFavorite(channelId)
            },
            onAddToMultiView = { chName, chUrl, chLogo ->
                channelPickerName = chName
                channelPickerUrl = chUrl
                channelPickerLogo = chLogo
                showChannelMultiViewPicker = true
            },
            onSwitchChannel = { index ->
                if (iptvChannelUrls != null && iptvChannelNames != null && index >= 0 && index < iptvChannelUrls.size && index < iptvChannelNames.size) {
                    val newLogo = iptvChannelLogos?.getOrNull(index).takeIf { !it.isNullOrBlank() }
                    val baseLaunch = iptvLaunch ?: PlayerLaunch(
                        profileId = args.profileId,
                        title = iptvChannelNames[index],
                        sourceUrl = iptvChannelUrls[index],
                        streamTitle = iptvChannelNames[index],
                        providerName = args.providerName.ifBlank { "IPTV" },
                        parentMetaId = args.parentMetaId.ifBlank { "iptv" },
                        parentMetaType = args.parentMetaType.ifBlank { "iptv" },
                        logo = newLogo,
                        poster = newLogo,
                    )
                    val newLaunch = baseLaunch.copy(
                        title = iptvChannelNames[index],
                        sourceUrl = iptvChannelUrls[index],
                        streamTitle = iptvChannelNames[index],
                        logo = newLogo,
                        poster = newLogo,
                        initialPositionMs = 0L,
                        initialProgressFraction = null,
                        channelNames = iptvChannelNames,
                        channelUrls = iptvChannelUrls,
                        channelLogos = iptvChannelLogos,
                        channelIds = iptvChannelIds,
                        currentChannelIndex = index,
                        historyChannelNames = iptvHistoryNames,
                        historyChannelUrls = iptvHistoryUrls,
                        historyChannelLogos = iptvHistoryLogos,
                        historyChannelIds = iptvHistoryIds,
                    )
                    flushWatchProgress()
                    args.onSwitchIptvChannel?.invoke(PlayerLaunchStore.put(newLaunch))
                }
            },
        )
        if (showChannelMultiViewPicker && channelPickerUrl.isNotBlank()) {
            val mvName = channelPickerName
            val mvUrl = channelPickerUrl
            val mvLogo = channelPickerLogo
            MultiWindowPositionPicker(
                streamTitle = mvName,
                streamUrl = mvUrl,
                streamPoster = mvLogo,
                onDismiss = {
                    showChannelMultiViewPicker = false
                    channelPickerName = ""
                    channelPickerUrl = ""
                    channelPickerLogo = null
                },
                onSlotSelected = { slotIndex ->
                    MultiWindowStore.addStream(mvUrl, mvName, mvLogo, slotIndex)
                    showChannelMultiViewPicker = false
                    channelPickerName = ""
                    channelPickerUrl = ""
                    channelPickerLogo = null
                },
            )
        }
    }
}

@Composable
private fun PlayerScreenRuntime.RenderPlayerModals(displayedPositionMs: Long) {
    PlayerScreenModalHosts(
        pendingP2pSwitch = pendingP2pSwitch,
        onPendingP2pSwitchChanged = { pendingP2pSwitch = it },
        onP2pEpisodeStreamSelected = { stream, episode, isAutoPlay ->
            switchToP2pEpisodeStream(stream, episode, isAutoPlay)
        },
        onP2pSourceStreamSelected = { stream -> switchToP2pSourceStream(stream) },
        onNextEpisodeAutoPlaySearchingChanged = { nextEpisodeAutoPlaySearching = it },
        onNextEpisodeAutoPlayCountdownChanged = { nextEpisodeAutoPlayCountdown = it },
        onNextEpisodeAutoPlaySourceNameChanged = { nextEpisodeAutoPlaySourceName = it },
        showAudioModal = showAudioModal,
        audioTracks = audioTracks,
        selectedAudioIndex = selectedAudioIndex,
        onAudioTrackSelected = { index ->
            selectedAudioIndex = index
            persistAudioPreference(audioTracks.firstOrNull { it.index == index })
            playerController?.selectAudioTrack(index)
            scope.launch {
                kotlinx.coroutines.delay(200)
                showAudioModal = false
            }
        },
        onAudioModalDismissed = { showAudioModal = false },
        showSubtitleModal = showSubtitleModal,
        activeSubtitleTab = activeSubtitleTab,
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        addonSubtitles = visibleAddonSubtitles,
        selectedAddonSubtitleId = selectedAddonSubtitleId,
        isLoadingAddonSubtitles = isLoadingAddonSubtitles,
        subtitleStyle = subtitleStyle,
        subtitleDelayMs = subtitleDelayMs,
        selectedAddonSubtitle = selectedAddonSubtitle,
        subtitleAutoSyncState = subtitleAutoSyncState,
        onSubtitleTabSelected = { activeSubtitleTab = it },
        onBuiltInSubtitleTrackSelected = { index ->
            val wasCustom = useCustomSubtitles
            selectedSubtitleIndex = index
            selectedAddonSubtitleId = null
            useCustomSubtitles = false
            persistInternalSubtitlePreference(subtitleTracks.firstOrNull { it.index == index })
            if (wasCustom) {
                playerController?.clearExternalSubtitleAndSelect(index)
            } else {
                playerController?.selectSubtitleTrack(index)
            }
        },
        onAddonSubtitleSelected = { addon ->
            selectedAddonSubtitleId = addon.id
            selectedSubtitleIndex = -1
            useCustomSubtitles = true
            persistAddonSubtitlePreference(addon)
            playerController?.setSubtitleUri(addon.url)
        },
        onFetchAddonSubtitles = { fetchAddonSubtitlesForActiveItem() },
        onSubtitleStyleChanged = PlayerSettingsRepository::setSubtitleStyle,
        onSubtitleDelayChanged = { delayMs -> setSubtitleDelay(delayMs) },
        onSubtitleDelayReset = { setSubtitleDelay(0) },
        onAutoSyncCapture = { captureSubtitleAutoSyncTime() },
        onAutoSyncCueSelected = { cue -> applySubtitleAutoSyncCue(cue) },
        onAutoSyncReload = { loadSubtitleAutoSyncCues(force = true) },
        onSubtitleModalDismissed = { showSubtitleModal = false },
        showVideoSettingsModal = showVideoSettingsModal,
        playerSettings = playerSettingsUiState,
        onVideoSettingsChanged = {
            playerController?.configureIosVideoOutput(PlayerSettingsRepository.uiState.value)
        },
        onVideoSettingsModalDismissed = { showVideoSettingsModal = false },
        showQualitySelector = showQualitySelector,
        qualities = qualities,
        selectedQualityIndex = selectedQualityIndex,
        onQualitySelected = { index -> selectQuality(index) },
        showSourcesPanel = showSourcesPanel,
        sourceStreamsState = sourceStreamsState,
        activeSourceUrl = activeSourceUrl,
        activeStreamTitle = activeStreamTitle,
        onSourceFilterSelected = PlayerStreamsRepository::selectSourceFilter,
        onSourceStreamSelected = { stream -> switchToSource(stream) },
        onReloadSources = {
            val vid = activeVideoId
            if (vid != null) {
                PlayerStreamsRepository.loadSources(
                    type = contentType ?: parentMetaType,
                    videoId = vid,
                    season = activeSeasonNumber,
                    episode = activeEpisodeNumber,
                    forceRefresh = true,
                )
            }
        },
        onSourcesPanelDismissed = {
            showSourcesPanel = false
            controlsVisible = true
        },
        isSeries = isSeries,
        showEpisodesPanel = showEpisodesPanel,
        allEpisodes = playerMetaVideos,
        parentMetaType = parentMetaType,
        parentMetaId = parentMetaId,
        activeSeasonNumber = activeSeasonNumber,
        activeEpisodeNumber = activeEpisodeNumber,
        watchProgressByVideoId = watchProgressUiState.byVideoIdForContent(parentMetaId),
        watchedKeys = watchedUiState.watchedKeys,
        blurUnwatchedEpisodes = metaScreenSettingsUiState.blurUnwatchedEpisodes,
        episodeStreamsPanelState = episodeStreamsPanelState,
        episodeStreamsRepoState = episodeStreamsRepoState,
        onEpisodeSelectedForDownload = { episode ->
            selectDownloadedEpisodeForPlayback(
                parentMetaId = parentMetaId,
                episode = episode,
                onDownloadedEpisodeSelected = { item, video -> switchToDownloadedEpisode(item, video) },
            )
        },
        onEpisodeStreamsRequested = { episode ->
            PlayerStreamsRepository.loadEpisodeStreams(
                type = contentType ?: parentMetaType,
                videoId = episode.id,
                season = episode.season,
                episode = episode.episode,
            )
            episodeStreamsPanelState = EpisodeStreamsPanelState(showStreams = true, selectedEpisode = episode)
        },
        onEpisodeStreamFilterSelected = PlayerStreamsRepository::selectEpisodeStreamsFilter,
        onEpisodeStreamSelected = { stream, episode -> switchToEpisodeStream(stream, episode) },
        onBackToEpisodes = {
            episodeStreamsPanelState = EpisodeStreamsPanelState()
            PlayerStreamsRepository.clearEpisodeStreams()
        },
        onReloadEpisodeStreams = {
            val episode = episodeStreamsPanelState.selectedEpisode
            if (episode != null) {
                PlayerStreamsRepository.loadEpisodeStreams(
                    type = contentType ?: parentMetaType,
                    videoId = episode.id,
                    season = episode.season,
                    episode = episode.episode,
                    forceRefresh = true,
                )
            }
        },
        onEpisodesPanelDismissed = {
            showEpisodesPanel = false
            episodeStreamsPanelState = EpisodeStreamsPanelState()
            PlayerStreamsRepository.clearEpisodeStreams()
            controlsVisible = true
        },
        showSubmitIntroModal = showSubmitIntroModal,
        activeVideoId = activeVideoId,
        metaUiState = metaUiState,
        displayedPositionMs = displayedPositionMs,
        submitIntroSegmentType = submitIntroSegmentType,
        onSubmitIntroSegmentTypeChanged = { submitIntroSegmentType = it },
        submitIntroStartTimeStr = submitIntroStartTimeStr,
        onSubmitIntroStartTimeChanged = { submitIntroStartTimeStr = it },
        submitIntroEndTimeStr = submitIntroEndTimeStr,
        onSubmitIntroEndTimeChanged = { submitIntroEndTimeStr = it },
        onSubmitIntroDismissed = { showSubmitIntroModal = false },
        onSubmitIntroSuccess = {
            submitIntroStartTimeStr = "00:00"
            submitIntroEndTimeStr = "00:00"
            submitIntroSegmentType = "intro"
            showSubmitIntroModal = false
        },
    )

    // ─── Multi-View Picker ──────────────────────────────────────────
    if (showMultiViewPicker) {
        MultiWindowPositionPicker(
            streamTitle = activeStreamTitle.ifBlank { title },
            streamUrl = activeSourceUrl,
            streamPoster = poster ?: logo,
            onDismiss = { showMultiViewPicker = false },
            onSlotSelected = { slotIndex ->
                if (activeSourceUrl.isNotBlank()) {
                    MultiWindowStore.addStream(
                        url = activeSourceUrl,
                        title = activeStreamTitle.ifBlank { title },
                        poster = poster ?: logo,
                        slotIndex = slotIndex,
                    )
                }
                showMultiViewPicker = false
            },
        )
    }
}

@Composable
private fun rememberIptvEpgPrograms(channel: IptvChannel?): List<EpgProgram> {
    val key = channel?.id ?: ""
    var programs by remember(key) { mutableStateOf<List<EpgProgram>?>(null) }
    LaunchedEffect(key) {
        if (channel == null) return@LaunchedEffect
        programs = IptvRepository.getEpgProgramsForChannel(channel)
        while (isActive) {
            delay(30_000)
            if (!isActive) break
            programs = IptvRepository.getEpgProgramsForChannel(channel)
        }
    }
    return programs ?: emptyList()
}

@Composable
private fun IptvEpgOverlay(
    sourceUrl: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val channel = remember(sourceUrl) {
        IptvRepository.getAllChannels().find { it.url == sourceUrl }
    }
    val programs = rememberIptvEpgPrograms(channel)
    val now = TraktPlatformClock.nowEpochMs()
    val currentProg = programs.firstOrNull { now in it.startTime until it.endTime }
    val nextProg = programs.firstOrNull { it.startTime > now }
    if (currentProg == null && nextProg == null) return
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    currentProg?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            Text(formatEpgTime(it.startTime), color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(it.title, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    nextProg?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4DD0E1)))
                            Text(formatEpgTime(it.startTime), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(it.title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun formatEpgTime(ms: Long): String {
    val local = ms + TraktPlatformClock.localTimezoneOffsetMs()
    val hours = ((local / 3_600_000L) % 24L).toInt()
    val minutes = ((local / 60_000L) % 60L).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}
