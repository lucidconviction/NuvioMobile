package com.nuvio.app.features.iptv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.nuvio.app.features.hub.HubReturnStore
import com.nuvio.app.features.hub.MultiWindowStore
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nuvio.app.navigation.PageStateStore
import com.nuvio.app.navigation.PageState

// ─── Obsidian Media Hub Design Tokens ────────────────────────────────────
private val ObsidianBg = Color(0xFF131313)
private val surfaceContainerLowest = Color(0xFF0E0E0E)
private val surfaceContainerLow = Color(0xFF1B1B1B)
private val surfaceContainer = Color(0xFF1F1F1F)
private val surfaceContainerHigh = Color(0xFF2A2A2A)
private val surfaceContainerHighest = Color(0xFF353535)
private val surfaceVariant = Color(0xFF353535)
private val onSurface = Color(0xFFE2E2E2)
private val onsurfaceContainerHigh = Color(0xFFC4C7C8)
private val outline = Color(0xFF8E9192)
private val outlineVariant = Color(0xFF444748)
private val primary = Color(0xFFFDFDFC)
private val onPrimary = Color(0xFF2F3131)
private val errorColor = Color(0xFFFFB4AB)
private val onError = Color(0xFF690005)
private val errorContainer = Color(0xFF93000A)
private val FavoriteRed = Color(0xFFE91E63)
private val EpgNowColor = Color(0xFFE53935)
private val EpgNextColor = Color(0xFF4DD0E1)
private val tvMargin = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvScreen(
    modifier: Modifier = Modifier,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onMultiWindowAdded: (() -> Unit)? = null,
    isTabletLayout: Boolean = false,
) {
    // Restore IptvScreen state when navigating back to this screen
    val restoredState = remember { PageStateStore.restore("IptvScreen") }
    
    // Hoist state up so we can save/restore scroll position and UI state
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val uiState by IptvRepository.uiState.collectAsStateWithLifecycle()
    var showAddSourceSheet by remember { 
        mutableStateOf(
            restoredState?.extra?.get("showAddSourceSheet") as? Boolean 
                ?: false
        ) 
    }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pickerChannel by remember { mutableStateOf<IptvChannel?>(null) }
    
    // Save IptvScreen state when navigating away from this screen
    DisposableEffect(Unit) {
        onDispose {
            // Save current state before leaving
            val stateToSave = PageState(
                searchQuery = uiState.searchQuery,
                selectedPage = "IptvScreen", // Using this field to identify the screen type
                scrollIndex = listState.firstVisibleItemIndex,
                scrollOffset = listState.firstVisibleItemScrollOffset,
                extra = mutableMapOf<String, Any>().apply {
                    put("showAddSourceSheet", showAddSourceSheet)
                    uiState.selectedCategory?.let { put("selectedCategory", it) }
                    put("selectedSourceIds", uiState.selectedSourceIds)
                    put("playlistsExpanded", uiState.playlistsExpanded)
                    put("channelsExpanded", uiState.channelsExpanded)
                    put("favoritesExpanded", uiState.favoritesExpanded)
                }
            )
            PageStateStore.save("IptvScreen", stateToSave)
        }
    }

    // Restore scroll position after layout if we have a saved state
    LaunchedEffect(restoredState, listState, scrollState) {
        if (restoredState != null && restoredState.scrollIndex >= 0) {
            runCatching {
                listState.scrollToItem(restoredState.scrollIndex, restoredState.scrollOffset)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            IptvRepository.ensureLoaded()
        } catch (e: Exception) {
            e.printStackTrace()
            loadError = e.message ?: "Failed to load IPTV"
        }
    }

    if (loadError != null) {
        Box(Modifier.fillMaxSize().background(ObsidianBg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Failed to load IPTV", color = onsurfaceContainerHigh, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(surfaceContainer).clickable {
                    loadError = null
                    try { IptvRepository.ensureLoaded() } catch (e: Exception) { loadError = e.message }
                }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("Retry", color = primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    pickerChannel?.let { ch ->
        com.nuvio.app.features.hub.MultiWindowPositionPicker(
            channel = ch,
            onDismiss = { pickerChannel = null },
            onSlotSelected = { slot ->
                try {
                    com.nuvio.app.features.hub.MultiWindowStore.addToSlot(ch, slot)
                } catch (e: Exception) {
                    co.touchlab.kermit.Logger.e(e) { "Failed to add to multiwindow slot" }
                }
                pickerChannel = null
            },
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
        val isTvMode = maxWidth >= 1024.dp

        if (isTvMode) {
            IptvTvMode(
                uiState = uiState,
                onPlayChannel = onPlayChannel,
                scrollToTopRequests = scrollToTopRequests,
                onAddSource = { showAddSourceSheet = true },
                onPickerChannel = { pickerChannel = it },
                scrollState = scrollState,
                restoredState = restoredState
            )
        } else {
            IptvMobileMode(
                uiState = uiState,
                onPlayChannel = onPlayChannel,
                scrollToTopRequests = scrollToTopRequests,
                onAddSource = { showAddSourceSheet = true },
                onPickerChannel = { pickerChannel = it },
                listState = listState,
                restoredState = restoredState
            )
        }
    }

    if (showAddSourceSheet) {
        val license = PortalLicenseManager.getSavedLicense()
        val portalCount = IptvRepository.portalAccountCount()
        val canAdd = PortalLicenseManager.canAddPortal(license, portalCount)
        AddSourceBottomSheet(
            onDismiss = { showAddSourceSheet = false },
            onSuccess = { showAddSourceSheet = false },
            canAddPortal = canAdd,
            licenseStatus = PortalLicenseManager.checkStatus(license),
        )
    }
}

// ── TV Mode ──────────────────────────────────────────────────────────────

@Composable
private fun IptvTvMode(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    scrollToTopRequests: Flow<Unit>,
    onAddSource: () -> Unit,
    onPickerChannel: (IptvChannel) -> Unit,
    scrollState: ScrollState,
    restoredState: PageState?,
) {
    val now = TraktPlatformClock.nowEpochMs()
    var showSearch by remember {
        mutableStateOf(
            restoredState?.extra?.get("showSearch") as? Boolean
                ?: false
        )
    }
    var epgSheetChannel: IptvChannel? by remember { mutableStateOf(null) }
    val xtreamAccountById = remember(uiState.xtreamAccounts) { uiState.xtreamAccounts.associateBy { it.id } }
    val allChannels = remember(uiState.m3uPlaylists, uiState.xtreamAccounts, uiState.stalkerAccounts) {
        uiState.m3uPlaylists.flatMap { it.channels } +
            uiState.xtreamAccounts.flatMap { it.channels } +
            uiState.stalkerAccounts.flatMap { it.channels }
    }
    val favorites = remember(uiState.favoriteChannelIds, allChannels) { IptvRepository.getFavoriteChannels() }
    val history = remember(uiState.channelHistoryIds, allChannels) { IptvRepository.getHistoryChannels() }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        // ── Fixed Top Bar (persistent search bar) ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = tvMargin, end = tvMargin, top = 5.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { /* parent handles back */ }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = onSurface)
            }
            Text("IPTVNutz Hub", color = primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.width(16.dp))
            if (showSearch) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { IptvRepository.setSearchQuery(it) },
                    placeholder = { Text("Search channels, groups, or sources...", color = onsurfaceContainerHigh.copy(alpha = 0.5f), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = onsurfaceContainerHigh, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { IptvRepository.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, "Clear", tint = onsurfaceContainerHigh)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = onSurface, unfocusedTextColor = onSurface,
                        focusedBorderColor = primary, unfocusedBorderColor = outlineVariant.copy(alpha = 0.5f),
                        cursorColor = primary,
                        focusedContainerColor = surfaceContainerLow,
                        unfocusedContainerColor = surfaceContainerLow,
                    ),
                    modifier = Modifier.weight(1f).height(44.dp),
                )
            } else {
                IconButton(onClick = { showSearch = true }) {
                    Icon(Icons.Filled.Search, "Search", tint = onSurface, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = { HubReturnStore.xxxScreen = "Xxx" }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Lock, null, tint = onSurface, modifier = Modifier.size(26.dp))
                    Text("XXX", color = onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        // ── Scrollable Content (everything else scrolls) ──
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                .padding(horizontal = tvMargin),
        ) {
            uiState.error?.let { err ->
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(errorContainer.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(err, color = errorColor, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
            // Source Chips Row
            SourceChipsTvRow(uiState = uiState)

            Spacer(Modifier.height(16.dp))

            // Category Chips Row
            CategoryChipsTvRow(uiState = uiState)

            Spacer(Modifier.height(24.dp))

            // Bento Featured Grid
            if (favorites.isNotEmpty()) {
                FeaturedBentoSection(
                    channels = favorites.take(3),
                    now = now,
                    onPlay = { playChannel(it, onPlayChannel) },
                    isTablet = false,
                )
                Spacer(Modifier.height(32.dp))
            }

            // Quick Channels
            QuickChannelsSection(
                allChannels = allChannels,
                onPlayChannel = onPlayChannel,
            )
            Spacer(Modifier.height(32.dp))

            // Recommended Channels Grid
            val displayChannels = remember(allChannels, uiState.searchQuery) {
                if (uiState.searchQuery.isNotBlank()) {
                    allChannels.filter { (it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            (it.group?.contains(uiState.searchQuery, ignoreCase = true) == true)) &&
                            !StreamValidationStore.isKnownDeadSync(it.url) }
                } else allChannels.filter { !StreamValidationStore.isKnownDeadSync(it.url) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recommended Channels", color = primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text("VIEW ALL", color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                modifier = Modifier.fillMaxWidth().height(360.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                userScrollEnabled = false,
            ) {
                itemsIndexed(displayChannels.take(10), key = { index, channel -> "rec_${index}_${channel.id}_${channel.sourceId}" }) { _, channel ->
                    TvChannelCard(
                        channel = channel,
                        isFavorite = channel.id in uiState.favoriteChannelIds,
                        isQuickPinned = com.nuvio.app.features.iptv.QuickChannelList.isPinned(channel.name),
                        now = now,
                        epgAccount = xtreamAccountById[channel.sourceId],
                        onLongPress = { epgSheetChannel = channel },
                        onPlay = { playChannel(channel, onPlayChannel) },
                        onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
                        onToggleQuickPin = { com.nuvio.app.features.iptv.QuickChannelList.togglePin(channel.name) },
                        onAddToMultiWindow = { sendToNextMultiWindowSlot(channel, onPickerChannel) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Playlists Section
            PlaylistsTvSection(
                m3uPlaylists = uiState.m3uPlaylists,
                xtreamAccounts = uiState.xtreamAccounts,
                stalkerAccounts = uiState.stalkerAccounts,
                epgSources = uiState.epgSources,
                epgLoading = uiState.epgLoading,
                epgMatchCount = uiState.epgMatchCount,
                refreshingIds = uiState.refreshingSourceIds,
                onAddClick = onAddSource,
                onRefreshM3u = { IptvRepository.refreshM3uChannels(it) },
                onDeleteM3u = { IptvRepository.removeM3uPlaylist(it) },
                onRefreshXtream = { IptvRepository.refreshXtreamChannels(it) },
                onDeleteXtream = { IptvRepository.removeXtreamAccount(it) },
                onRefreshStalker = { IptvRepository.refreshStalkerChannels(it) },
                onDeleteStalker = { IptvRepository.removeStalkerAccount(it) },
            )

            Spacer(Modifier.height(32.dp))

            VodSeriesSection(uiState = uiState, onPlayChannel = onPlayChannel)

            // Recently Watched (History)
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recently Watched", color = primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(
                        "✕ Clear",
                        color = onsurfaceContainerHigh,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { IptvRepository.clearHistory() },
                    )
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(history.take(12), key = { i, ch -> "tv_hist_${i}_${ch.id}_${ch.sourceId}" }) { _, channel ->
                        HistoryTvCard(
                            channel = channel,
                            now = now,
                            epgAccount = xtreamAccountById[channel.sourceId],
                            onLongPress = { epgSheetChannel = channel },
                            onPlay = { playChannel(channel, onPlayChannel) },
                            onAddToMultiWindow = { sendToNextMultiWindowSlot(channel, onPickerChannel) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }

    // ── Status Bar ──
    Row(
        modifier = Modifier.fillMaxWidth().background(surfaceContainerLow.copy(alpha = 0.9f))
            .border(0.5.dp, outlineVariant.copy(alpha = 0.5f))
            .padding(horizontal = tvMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                Text("SERVER STATUS: OPTIMAL", color = onsurfaceContainerHigh, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            }
            Box(Modifier.width(1.dp).height(16.dp).background(outlineVariant.copy(alpha = 0.5f)))
            Text("LATENCY: 42ms", color = onsurfaceContainerHigh, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val upNext = if (history.isNotEmpty()) history.first().name else ""
            Text("UPNEXT: ${upNext.uppercase()}", color = primary, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            TvClock()
        }
    }

    epgSheetChannel?.let { channel ->
        EpgProgramSheet(
            channel = channel,
            account = xtreamAccountById[channel.sourceId],
            onDismiss = { epgSheetChannel = null },
        )
    }
}

@Composable
private fun TvClock() {
    var ms by remember { mutableLongStateOf(TraktPlatformClock.nowEpochMs()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            ms = TraktPlatformClock.nowEpochMs()
        }
    }
    val localMs = ms + TraktPlatformClock.localTimezoneOffsetMs()
    val totalSeconds = localMs / 1000L
    val h = ((totalSeconds / 3600) % 24).toInt()
    val m = ((totalSeconds / 60) % 60).toInt()
    val s = (totalSeconds % 60).toInt()
    Text(
        "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}",
        color = onsurfaceContainerHigh, fontSize = 11.sp,
        fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun SourceChipsTvRow(uiState: IptvUiState) {
    val sourceNames = IptvRepository.getAllSourceNames()
    val sourceIds = IptvRepository.getAllSourceIds()
    val allChannels = uiState.m3uPlaylists.flatMap { it.channels } +
            uiState.xtreamAccounts.flatMap { it.channels } +
            uiState.stalkerAccounts.flatMap { it.channels }

    Column {
        Text("ACTIVE SOURCES", color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val allSelected = uiState.selectedSourceIds.isEmpty()
            TvSourceChip(
                selected = allSelected,
                onClick = { if (!allSelected) IptvRepository.clearSourceSelection() },
                label = "ALL SOURCES",
                count = allChannels.size,
                chipStyle = "filled",
            )
            sourceNames.forEachIndexed { index, name ->
                val id = sourceIds.getOrNull(index) ?: return@forEachIndexed
                val count = when {
                    index < uiState.m3uPlaylists.size -> uiState.m3uPlaylists[index].channels.size
                    index < uiState.m3uPlaylists.size + uiState.xtreamAccounts.size -> {
                        val xi = index - uiState.m3uPlaylists.size
                        uiState.xtreamAccounts.getOrNull(xi)?.channels?.size ?: 0
                    }
                    else -> 0
                }
                TvSourceChip(
                    selected = id in uiState.selectedSourceIds,
                    onClick = { IptvRepository.toggleSourceSelection(index) },
                    label = name.uppercase(),
                    count = count,
                    chipStyle = "outlined",
                )
            }
        }
    }
}

@Composable
private fun TvSourceChip(selected: Boolean, onClick: () -> Unit, label: String, count: Int, chipStyle: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) primary else surfaceContainerLow)
            .border(if (chipStyle == "outlined" && !selected) 1.dp else 0.dp,
                outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = if (selected) onPrimary else onsurfaceContainerHigh,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("$count", color = if (selected) onPrimary.copy(alpha = 0.7f) else onsurfaceContainerHigh.copy(alpha = 0.5f),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CategoryChipsTvRow(uiState: IptvUiState) {
    val categories = IptvRepository.getAllCategories()
    if (categories.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvCategoryChip(
            selected = uiState.selectedCategory == null,
            onClick = { IptvRepository.selectCategory(null) },
            label = "ALL",
        )
        categories.forEach { cat ->
            TvCategoryChip(
                selected = uiState.selectedCategory == cat,
                onClick = { IptvRepository.selectCategory(cat) },
                label = cat.uppercase(),
            )
        }
    }
}

@Composable
private fun TvCategoryChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) primary else surfaceContainerLow)
            .border(if (!selected) 1.dp else 0.dp, outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (selected) onPrimary else onsurfaceContainerHigh,
            fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FeaturedBentoSection(
    channels: List<IptvChannel>,
    now: Long,
    onPlay: (IptvChannel) -> Unit,
    isTablet: Boolean,
) {
    val hero = channels.getOrNull(0)
    val side1 = channels.getOrNull(1)
    val side2 = channels.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Hero (8-col equivalent)
        Box(
            modifier = Modifier.weight(2f).fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceContainerHigh)
                .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable { hero?.let(onPlay) },
        ) {
            if (hero != null) {
                if (!hero.logo.isNullOrBlank()) {
                    AsyncImage(model = hero.logo, contentDescription = hero.name,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.9f)))))
                Box(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(errorColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val pulseAlpha = animateToAlpha()
                                Box(Modifier.size(6.dp).clip(CircleShape).background(primary.copy(alpha = pulseAlpha)))
                                Text("LIVE", color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(ObsidianBg.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("4K ULTRA HD", color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Column {
                        Text(hero.group?.uppercase() ?: "PREMIUM", color = onsurfaceContainerHigh, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(hero.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(primary).clickable { onPlay(hero) }
                                .padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.PlayArrow, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                                    Text("WATCH NOW", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ObsidianBg.copy(alpha = 0.4f))
                                .border(0.5.dp, outlineVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text("STATS & INFO", color = primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No featured channels", color = onsurfaceContainerHigh)
                }
            }
        }

        // Stacked side cards (4-col equivalent)
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            listOfNotNull(side1, side2).forEach { ch ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceContainerHigh)
                        .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onPlay(ch) },
                ) {
                    if (!ch.logo.isNullOrBlank()) {
                        AsyncImage(model = ch.logo, contentDescription = ch.name,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        ChannelLogo(
                            modifier = Modifier.fillMaxSize(),
                            channel = ch,
                        )
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f)))))
                    Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Column {
                            Text("${ch.group?.take(3)?.uppercase() ?: "LIVE"} · LIVE",
                                color = errorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(ch.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun animateToAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    return infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    ).value
}

private const val StatusAliveColor = 0xFF4CAF50L
private const val StatusDeadColor = 0xFFE53935L

private fun sendToNextMultiWindowSlot(channel: IptvChannel, onPicker: (IptvChannel) -> Unit) {
    val slot = com.nuvio.app.features.hub.MultiWindowStore.nextAvailableSlot()
    if (slot != null) {
        com.nuvio.app.features.hub.MultiWindowStore.addToSlot(channel, slot)
    } else {
        onPicker(channel)
    }
}

@Composable
private fun StreamStatusDot(url: String, size: Dp) {
    val color = when (StreamValidationStore.statuses[url]) {
        true -> Color(StatusAliveColor)
        false -> Color(StatusDeadColor)
        null -> onSurface.copy(alpha = 0.25f)
    }
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
internal fun rememberEpgSteps(account: XtreamAccount?, channel: IptvChannel, limit: Int): List<EpgProgram> {
    var programs by remember(channel.sourceId, channel.id, account?.server, limit) {
        mutableStateOf<List<EpgProgram>?>(null)
    }
    LaunchedEffect(channel.sourceId, channel.id, account?.server, limit) {
        programs = if (account != null && channel.sourceType == SourceType.Xtream) {
            ShortEpgCache.getOrLoad(account, channel, limit)
        } else {
            emptyList()
        }
    }
    return programs ?: emptyList()
}

@Composable
internal fun EpgNowNextRow(programs: List<EpgProgram>, now: Long) {
    if (programs.isEmpty()) return
    val current = programs.firstOrNull { now in it.startTime until it.endTime }
    val next = programs.firstOrNull { it.startTime > now }
    if (current == null && next == null) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        current?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(EpgNowColor))
                Spacer(Modifier.width(5.dp))
                Text("${formatTime(it.startTime)} ${it.title}", color = onSurface,
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        next?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(EpgNextColor))
                Spacer(Modifier.width(5.dp))
                Text("${formatTime(it.startTime)} ${it.title}", color = onsurfaceContainerHigh,
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpgProgramSheet(
    channel: IptvChannel,
    account: XtreamAccount?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var programs by remember(channel.sourceId, channel.id, account?.server) { mutableStateOf<List<EpgProgram>?>(null) }
    LaunchedEffect(channel.sourceId, channel.id, account?.server) {
        programs = if (account != null && channel.sourceType == SourceType.Xtream) {
            ShortEpgCache.getOrLoad(account, channel, 8)
        } else {
            emptyList()
        }
    }
    val now = TraktPlatformClock.nowEpochMs()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                channel.name,
                color = onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            when {
                programs == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
                programs.isNullOrEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No EPG available for this channel", color = onsurfaceContainerHigh, fontSize = 13.sp)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    itemsIndexed(programs ?: emptyList(), key = { index, program -> "${index}_${program.startTime}_${program.title}" }) { _, program ->
                        val isNow = now in program.startTime until program.endTime
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(52.dp)) {
                                Text(formatTime(program.startTime), color = if (isNow) EpgNowColor else onsurfaceContainerHigh,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Box(Modifier.width(10.dp)) {
                                if (isNow) Box(Modifier.size(8.dp).clip(CircleShape).background(EpgNowColor))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(program.title, color = if (isNow) onSurface.copy(alpha = 0.95f) else onsurfaceContainerHigh,
                                    fontSize = 13.sp, fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (!program.description.isNullOrBlank()) {
                                    Text(program.description, color = onsurfaceContainerHigh.copy(alpha = 0.6f),
                                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TvChannelCard(
    channel: IptvChannel,
    isFavorite: Boolean,
    isQuickPinned: Boolean,
    now: Long,
    epgAccount: XtreamAccount? = null,
    onLongPress: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleQuickPin: () -> Unit,
    onAddToMultiWindow: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceContainerHigh)
            .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .then(
                if (onLongPress != null)
                    Modifier.combinedClickable(onClick = onPlay, onLongClick = onLongPress)
                else
                    Modifier.clickable(onClick = onPlay)
            ),
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(model = channel.logo, contentDescription = channel.name,
                modifier = Modifier.fillMaxSize().padding(24.dp), contentScale = ContentScale.Fit)
        } else {
            ChannelLogo(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                channel = channel,
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f)))))
        Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
            StreamStatusDot(url = channel.url, size = 8.dp)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onToggleQuickPin, modifier = Modifier.size(28.dp)) {
                    Text(if (isQuickPinned) "\u2605" else "\u2606", color = if (isQuickPinned) primary else onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (isFavorite) "Unfavorite" else "Favorite",
                        tint = if (isFavorite) FavoriteRed else onsurfaceContainerHigh.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onAddToMultiWindow, modifier = Modifier.size(28.dp)) {
                    Text("\u2295", color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 16.sp)
                }
            }
        }
        Box(Modifier.align(Alignment.Center)) {
            Icon(Icons.Filled.PlayArrow, "Play", tint = primary, modifier = Modifier.size(48.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Column {
                Text(channel.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.group?.uppercase() ?: "LIVE", color = onsurfaceContainerHigh, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                EpgNowNextRow(programs = rememberEpgSteps(epgAccount, channel, limit = 2), now = now)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryTvCard(
    channel: IptvChannel,
    now: Long,
    epgAccount: XtreamAccount? = null,
    onLongPress: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onAddToMultiWindow: () -> Unit,
) {
    Box(
        modifier = Modifier.width(240.dp).aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceContainerHigh)
            .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .then(
                if (onLongPress != null)
                    Modifier.combinedClickable(onClick = onPlay, onLongClick = onLongPress)
                else
                    Modifier.clickable(onClick = onPlay)
            ),
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(model = channel.logo, contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LiveTv, null, tint = onsurfaceContainerHigh.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f)))))
        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            IconButton(onClick = onAddToMultiWindow, modifier = Modifier.size(28.dp)) {
                Text("\u2295", color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 16.sp)
            }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Column {
                Text(channel.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                EpgNowNextRow(programs = rememberEpgSteps(epgAccount, channel, limit = 2), now = now)
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).fillMaxWidth(0.85f))
    }
}

@Composable
private fun PlaylistsTvSection(
    m3uPlaylists: List<M3uPlaylist>,
    xtreamAccounts: List<XtreamAccount>,
    stalkerAccounts: List<StalkerAccount>,
    epgSources: List<EpgSource> = emptyList(),
    epgLoading: Boolean = false,
    epgMatchCount: Int = 0,
    refreshingIds: Set<String>,
    onAddClick: () -> Unit,
    onRefreshM3u: (String) -> Unit,
    onDeleteM3u: (String) -> Unit,
    onRefreshXtream: (String) -> Unit,
    onDeleteXtream: (String) -> Unit,
    onRefreshStalker: (String) -> Unit,
    onDeleteStalker: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Your Playlists", color = primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(primary).clickable(onClick = onAddClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Add, null, tint = onPrimary, modifier = Modifier.size(16.dp))
                Text("ADD", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    val m3uIds = m3uPlaylists.map { it.id }
    val xtreamIds = xtreamAccounts.map { it.id }
    val stalkerIds = stalkerAccounts.map { it.id }
    val allIds = m3uIds + xtreamIds + stalkerIds

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        m3uPlaylists.forEach { pl ->
            val idx = allIds.indexOf(pl.id)
            TvPlaylistCard(
                iconLabel = "M3U",
                name = pl.name,
                channelCount = pl.channels.size,
                scanState = StreamValidationController.scans[pl.id],
                status = if (pl.channels.isNotEmpty()) "Connected" else "Pending",
                statusColor = if (pl.channels.isNotEmpty()) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = pl.id in refreshingIds,
                onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                onRefresh = { onRefreshM3u(pl.id) },
                onDelete = { onDeleteM3u(pl.id) },
            )
        }
        xtreamAccounts.filter { it.channels.isNotEmpty() }.forEach { acc ->
            val idx = allIds.indexOf(acc.id)
            TvPlaylistCard(
                iconLabel = "XT",
                name = acc.name,
                channelCount = acc.channels.size,
                scanState = StreamValidationController.scans[acc.id],
                status = if (acc.channels.isNotEmpty()) "Connected" else "Pending",
                statusColor = if (acc.channels.isNotEmpty()) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = acc.id in refreshingIds,
                onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                onRefresh = { onRefreshXtream(acc.id) },
                onDelete = { onDeleteXtream(acc.id) },
            )
        }
        stalkerAccounts.forEach { acc ->
            val idx = allIds.indexOf(acc.id)
            TvPlaylistCard(
                iconLabel = "SK",
                name = acc.name,
                channelCount = acc.channels.size,
                scanState = StreamValidationController.scans[acc.id],
                status = if (acc.channels.isNotEmpty()) "Connected" else "Pending",
                statusColor = if (acc.channels.isNotEmpty()) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = acc.id in refreshingIds,
                onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                onRefresh = { onRefreshStalker(acc.id) },
                onDelete = { onDeleteStalker(acc.id) },
            )
        }
        epgSources.forEach { source ->
            TvPlaylistCard(
                iconLabel = "EPG",
                name = source.name,
                channelCount = epgMatchCount,
                scanState = null,
                status = when {
                    epgLoading -> "Loading"
                    epgMatchCount > 0 -> "Loaded"
                    else -> "Pending"
                },
                statusColor = if (epgLoading || epgMatchCount > 0) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = epgLoading,
                onRefresh = { IptvRepository.refreshEpg() },
                onDelete = { IptvRepository.removeEpgSource(source.id) },
            )
        }
    }
}

@Composable
private fun TvPlaylistCard(
    iconLabel: String,
    name: String,
    channelCount: Int,
    scanState: SourceScanState?,
    status: String,
    statusColor: Color,
    isRefreshing: Boolean,
    onClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val scanSubtitle = when {
        scanState == null || scanState.checked == 0 -> null
        scanState.active -> "Scanning ${scanState.checked}/${scanState.total}..."
        else -> "${scanState.alive} OK · ${scanState.dead} bad"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(surfaceContainer)
            .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Text(iconLabel, color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = primary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Text("$channelCount Channels", color = onsurfaceContainerHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (scanSubtitle != null) {
                    Text("·", color = onsurfaceContainerHigh.copy(alpha = 0.4f))
                    Text(scanSubtitle, color = onsurfaceContainerHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Text("·", color = onsurfaceContainerHigh.copy(alpha = 0.4f))
                Text(status.uppercase(), color = onsurfaceContainerHigh, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primary, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = onsurfaceContainerHigh, modifier = Modifier.size(20.dp))
                }
            }
IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, "Delete", tint = errorColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            Icon(Icons.Filled.CheckCircle, null, tint = statusColor, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Mobile Mode (existing layout) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IptvMobileMode(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    scrollToTopRequests: Flow<Unit>,
    onAddSource: () -> Unit,
    onPickerChannel: (IptvChannel) -> Unit,
    listState: LazyListState,
    restoredState: PageState?,
) {
    var expandedGroups by remember { 
        mutableStateOf(
            restoredState?.extra?.get("expandedGroups") as? Set<String> 
                ?: setOf()
        ) 
    }
    var epgSheetChannel: IptvChannel? by remember { mutableStateOf(null) }
    val xtreamAccountById = remember(uiState.xtreamAccounts) { uiState.xtreamAccounts.associateBy { it.id } }
    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }

    val allChannels = remember(uiState.m3uPlaylists, uiState.xtreamAccounts, uiState.stalkerAccounts) {
        uiState.m3uPlaylists.flatMap { it.channels } +
            uiState.xtreamAccounts.flatMap { it.channels } +
            uiState.stalkerAccounts.flatMap { it.channels }
    }
    val allFavorites = remember(uiState.favoriteChannelIds, allChannels) { IptvRepository.getFavoriteChannels() }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("IPTVNutz Hub", color = primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { HubReturnStore.xxxScreen = "Xxx" }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Lock, null, tint = onSurface, modifier = Modifier.size(26.dp))
                            Text("XXX", color = onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg.copy(alpha = 0.8f),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading && uiState.channels.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }
                return@LazyColumn
            }

            uiState.error?.let { err ->
                item {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(errorContainer.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(err, color = errorColor, fontSize = 13.sp)
                    }
                }
            }

            item { SearchSection(searchQuery = uiState.searchQuery, onSearchQueryChange = { IptvRepository.setSearchQuery(it) }) }

            item { QuickAccessSection(
                uiState = uiState,
                onPlayChannel = onPlayChannel,
                onAddToMultiWindow = { sendToNextMultiWindowSlot(it, onPickerChannel) },
            ) }

            item { HistorySection(
                uiState = uiState,
                onPlayChannel = onPlayChannel,
                onAddToMultiWindow = { sendToNextMultiWindowSlot(it, onPickerChannel) },
            ) }

            item {
                QuickChannelsSection(allChannels = allChannels, onPlayChannel = onPlayChannel)
            }

            item { PlaylistsSection(uiState = uiState, onAddClick = onAddSource) }

            item { VodSeriesSection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { SourceChipsSection(uiState = uiState) }

            item { CategoryChipsSection(uiState = uiState) }

            val channels = uiState.channels
            if (channels.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No channels found", color = onsurfaceContainerHigh)
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { IptvRepository.toggleChannelsExpanded() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${channels.size} Channel${if (channels.size == 1) "" else "s"}",
                            color = onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Icon(
                            if (uiState.channelsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (uiState.channelsExpanded) "Collapse" else "Expand",
                            tint = onsurfaceContainerHigh,
                        )
                    }
                }

                if (uiState.channelsExpanded) {
                    val now = TraktPlatformClock.nowEpochMs()
                    val grouped = channels.groupBy { it.group ?: "Other" }
                        .toList()
                        .sortedBy { it.first }
                    grouped.forEachIndexed { groupIndex, (group, chs) ->
                        val isExpanded = group in expandedGroups
                        item(key = "grp_${groupIndex}_$group") {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        expandedGroups = if (isExpanded) expandedGroups - group
                                        else expandedGroups + group
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = group,
                                    color = onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                                Icon(
                                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = onsurfaceContainerHigh,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                        }
                        if (isExpanded) {
                            val chunked = chs.chunked(2)
                            itemsIndexed(chunked, key = { index, _ -> "grp_${groupIndex}_row_$index" }) { index, rowChannels ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowChannels.forEach { channel ->
                                        Box(modifier = Modifier.weight(1f)) {
                                        ChannelCard(
                                            channel = channel,
                                            now = now,
                                            isFavorite = channel.id in uiState.favoriteChannelIds,
                                            isQuickPinned = com.nuvio.app.features.iptv.QuickChannelList.isPinned(channel.name),
                                            epgAccount = xtreamAccountById[channel.sourceId],
                                            onLongPress = { epgSheetChannel = channel },
                                            onPlay = { playChannel(channel, onPlayChannel) },
                                            onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
                                            onToggleQuickPin = { com.nuvio.app.features.iptv.QuickChannelList.togglePin(channel.name) },
                                            onAddToMultiWindow = { sendToNextMultiWindowSlot(channel, onPickerChannel) },
                                        )
                                        }
                                    }
                                    if (rowChannels.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (allFavorites.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { IptvRepository.toggleFavoritesExpanded() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("All Favorites", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            Icon(
                                if (uiState.favoritesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (uiState.favoritesExpanded) "Collapse" else "Expand",
                                tint = onsurfaceContainerHigh,
                            )
                        }
                    }
                    if (uiState.favoritesExpanded) {
                        val nowFav = TraktPlatformClock.nowEpochMs()
                        val favChunked = allFavorites.chunked(2)
                        itemsIndexed(favChunked, key = { index, _ -> "allfav_row_$index" }) { index, rowChannels ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowChannels.forEach { channel ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ChannelCard(
                                            channel = channel,
                                            now = nowFav,
                                            isFavorite = channel.id in uiState.favoriteChannelIds,
                                            epgAccount = xtreamAccountById[channel.sourceId],
                                            onLongPress = { epgSheetChannel = channel },
                                            onPlay = { playChannel(channel, onPlayChannel) },
                                            onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
                                            onAddToMultiWindow = { sendToNextMultiWindowSlot(channel, onPickerChannel) },
                                        )
                                    }
                                }
                                if (rowChannels.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (uiState.debugText.isNotBlank()) {
                    item {
                        Surface(shape = RoundedCornerShape(8.dp), color = surfaceContainer) {
                            Text(uiState.debugText, color = onsurfaceContainerHigh.copy(alpha = 0.5f), fontSize = 9.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    epgSheetChannel?.let { channel ->
        EpgProgramSheet(
            channel = channel,
            account = xtreamAccountById[channel.sourceId],
            onDismiss = { epgSheetChannel = null },
        )
    }
}

// ── Shared Mobile Components (unchanged from original) ─────────────────────

@Composable
private fun SearchSection(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search channels...", color = onsurfaceContainerHigh.copy(alpha = 0.4f), fontSize = 14.sp) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = onsurfaceContainerHigh) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = onsurfaceContainerHigh)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 14.sp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primary,
            unfocusedBorderColor = outlineVariant.copy(alpha = 0.3f),
            cursorColor = primary,
            focusedContainerColor = surfaceContainerLowest,
            unfocusedContainerColor = surfaceContainerLowest,
        ),
    )
}

@Composable
private fun QuickAccessSection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onAddToMultiWindow: (IptvChannel) -> Unit,
) {
    val favorites = IptvRepository.getFavoriteChannels()
    val now = TraktPlatformClock.nowEpochMs()
    val xtreamAccountById = remember(uiState.xtreamAccounts) { uiState.xtreamAccounts.associateBy { it.id } }
    var epgSheetChannel: IptvChannel? by remember { mutableStateOf(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Favorites", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = onsurfaceContainerHigh.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the heart icon on any channel to add it here",
                        color = onsurfaceContainerHigh,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
            return
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(favorites.take(10), key = { index, channel -> "fav_${index}_${channel.id}_${channel.sourceId}" }) { index, channel ->
                QuickAccessCard(
                    channel = channel,
                    now = now,
                    epgAccount = xtreamAccountById[channel.sourceId],
                    onLongPress = { epgSheetChannel = channel },
                    onPlay = { playChannel(channel, onPlayChannel) },
                    onAddToMultiWindow = { onAddToMultiWindow(channel) },
                )
            }
        }
    }

    epgSheetChannel?.let { channel ->
        EpgProgramSheet(
            channel = channel,
            account = xtreamAccountById[channel.sourceId],
            onDismiss = { epgSheetChannel = null },
        )
    }
}

@Composable
private fun HistorySection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onAddToMultiWindow: (IptvChannel) -> Unit,
) {
    val history = IptvRepository.getHistoryChannels()
    if (history.isEmpty()) return
    val now = TraktPlatformClock.nowEpochMs()
    val xtreamAccountById = remember(uiState.xtreamAccounts) { uiState.xtreamAccounts.associateBy { it.id } }
    var epgSheetChannel: IptvChannel? by remember { mutableStateOf(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(
                "✕ Clear",
                color = onsurfaceContainerHigh,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { IptvRepository.clearHistory() },
            )
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(history.take(15), key = { index, channel -> "hist_${index}_${channel.id}_${channel.sourceId}" }) { index, channel ->
                QuickAccessCard(
                    channel = channel,
                    now = now,
                    epgAccount = xtreamAccountById[channel.sourceId],
                    onLongPress = { epgSheetChannel = channel },
                    onPlay = { playChannel(channel, onPlayChannel) },
                    onAddToMultiWindow = { onAddToMultiWindow(channel) },
                )
            }
        }
    }

    epgSheetChannel?.let { channel ->
        EpgProgramSheet(
            channel = channel,
            account = xtreamAccountById[channel.sourceId],
            onDismiss = { epgSheetChannel = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickAccessCard(
    channel: IptvChannel,
    now: Long,
    epgAccount: XtreamAccount? = null,
    onLongPress: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onAddToMultiWindow: () -> Unit,
) {
    Card(
        modifier = Modifier.width(140.dp).then(
            if (onLongPress != null)
                Modifier.combinedClickable(onClick = onPlay, onLongClick = onLongPress)
            else
                Modifier.clickable(onClick = onPlay)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            } else {
                ChannelLogo(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    channel = channel,
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    if (channel.logo.isNullOrBlank())
                        Brush.verticalGradient(listOf(surfaceContainerHigh.copy(alpha = 0.3f), surfaceContainerLow))
                    else
                        Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f)))
                ),
            )
            if (channel.logo.isNullOrBlank()) {
                Box(
                    modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LiveTv, contentDescription = null, tint = onsurfaceContainerHigh, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                    .padding(8.dp),
            ) {
                Column {
                    Text(text = channel.name, color = onSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = channel.group ?: "Live", color = onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    EpgNowNextRow(programs = rememberEpgSteps(epgAccount, channel, limit = 2), now = now)
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(6.dp).clip(CircleShape).background(onSurface),
            )
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(onClick = onAddToMultiWindow, modifier = Modifier.size(28.dp)) {
                    Text("\u2295", color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SourceChipsSection(uiState: IptvUiState) {
    val sourceNames = IptvRepository.getAllSourceNames()
    val sourceIds = IptvRepository.getAllSourceIds()
    val allChannels = uiState.m3uPlaylists.flatMap { it.channels } +
        uiState.xtreamAccounts.flatMap { it.channels } +
        uiState.stalkerAccounts.flatMap { it.channels }
    if (sourceNames.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val allSelected = uiState.selectedSourceIds.isEmpty()
            SourceChip(
                selected = allSelected,
                onClick = {
                    if (!allSelected) {
                        IptvRepository.clearSourceSelection()
                    }
                },
                label = "All",
                count = allChannels.size,
            )
            sourceNames.forEachIndexed { index, name ->
                val id = sourceIds.getOrNull(index) ?: return@forEachIndexed
                val sourceChannels = when {
                    index < uiState.m3uPlaylists.size -> uiState.m3uPlaylists[index].channels
                    index < uiState.m3uPlaylists.size + uiState.xtreamAccounts.size -> {
                        val xtrIdx = index - uiState.m3uPlaylists.size
                        uiState.xtreamAccounts.getOrNull(xtrIdx)?.channels ?: emptyList()
                    }
                    else -> {
                        val skIdx = index - uiState.m3uPlaylists.size - uiState.xtreamAccounts.size
                        uiState.stalkerAccounts.getOrNull(skIdx)?.channels ?: emptyList()
                    }
                }
                SourceChip(
                    selected = id in uiState.selectedSourceIds,
                    onClick = { IptvRepository.toggleSourceSelection(index) },
                    label = name,
                    count = sourceChannels.size,
                )
            }
        }
    }
}

@Composable
private fun SourceChip(selected: Boolean, onClick: () -> Unit, label: String, count: Int = 0) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) primary.copy(alpha = 0.2f) else surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) onsurfaceContainerHigh else onsurfaceContainerHigh,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$count",
                    color = if (selected) onSurface else onsurfaceContainerHigh.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun CategoryChipsSection(uiState: IptvUiState) {
    val categories = IptvRepository.getAllCategories()
    if (categories.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(
            selected = uiState.selectedCategory == null,
            onClick = { IptvRepository.selectCategory(null) },
            label = "All",
        )
        categories.forEach { cat ->
            CategoryChip(
                selected = uiState.selectedCategory == cat,
                onClick = { IptvRepository.selectCategory(cat) },
                label = cat,
            )
        }
    }
}

@Composable
private fun CategoryChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) onSurface.copy(alpha = 0.2f) else surfaceContainerLow,
    ) {
        Text(
            text = label,
            color = if (selected) onSurface else onsurfaceContainerHigh,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelCard(
    channel: IptvChannel,
    now: Long,
    isFavorite: Boolean,
    isQuickPinned: Boolean = false,
    epgAccount: XtreamAccount? = null,
    onLongPress: (() -> Unit)? = null,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleQuickPin: (() -> Unit)? = null,
    onAddToMultiWindow: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 0.97f else 1f,
        label = "scale",
    )
    Card(
        modifier = Modifier.fillMaxWidth().scale(focusScale)
            .then(if (isFocused) Modifier.border(2.dp, primary, RoundedCornerShape(12.dp)) else Modifier)
            .then(
                if (onLongPress != null)
                    Modifier.combinedClickable(onClick = onPlay, onLongClick = onLongPress)
                else
                    Modifier.clickable(onClick = onPlay)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (!channel.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    )
                } else {
                    ChannelLogo(
                        modifier = Modifier.fillMaxSize().align(Alignment.Center),
                        channel = channel,
                    )
                }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (channel.logo.isNullOrBlank())
                            Brush.verticalGradient(listOf(surfaceContainerHigh.copy(alpha = 0.3f), surfaceContainerLow))
                        else
                            Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f)))
                    ),
            )
            if (channel.logo.isNullOrBlank()) {
                Box(
                    modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LiveTv, contentDescription = null, tint = onsurfaceContainerHigh, modifier = Modifier.size(18.dp))
                }
            }
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                        .padding(8.dp),
                ) {
                    Column {
                        Text(text = channel.name, color = onSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = channel.group ?: "Live", color = onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        EpgNowNextRow(programs = rememberEpgSteps(epgAccount, channel, limit = 2), now = now)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(6.dp),
                ) {
                    StreamStatusDot(url = channel.url, size = 6.dp)
                }

                Row(Modifier.align(Alignment.TopEnd).padding(end = 4.dp)) {
                    if (onToggleQuickPin != null) {
                        IconButton(onClick = onToggleQuickPin, modifier = Modifier.size(28.dp)) {
                            Text(if (isQuickPinned) "\u2605" else "\u2606", color = if (isQuickPinned) primary else onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                    }
                    if (onAddToMultiWindow != null) {
                        IconButton(onClick = onAddToMultiWindow, modifier = Modifier.size(28.dp)) {
                            Text("\u2295", color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = if (isFavorite) FavoriteRed else onsurfaceContainerHigh.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    uiState: IptvUiState,
    onAddClick: () -> Unit,
    onPickerChannel: (IptvChannel) -> Unit = {},
) {
    val hasPlaylists = uiState.m3uPlaylists.isNotEmpty() || uiState.xtreamAccounts.isNotEmpty() || uiState.stalkerAccounts.isNotEmpty() || uiState.epgSources.isNotEmpty()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Your Playlists", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!IptvRepository.hasPredefinedPlaylist()) {
                    TextButton(onClick = { IptvRepository.addPredefinedPlaylist() }) {
                        Text("+ iptv-org", color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                if (!IptvRepository.hasNetSmuttPlaylist()) {
                    TextButton(onClick = { IptvRepository.addNetSmuttPlaylist() }) {
                        Text("+ Nett Smutt", color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    color = primary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ADD", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = { IptvRepository.togglePlaylistsExpanded() }) {
                    Icon(
                        if (uiState.playlistsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (uiState.playlistsExpanded) "Collapse" else "Expand",
                        tint = onsurfaceContainerHigh,
                    )
                }
            }
        }

        AnimatedVisibility(visible = uiState.playlistsExpanded) {
            Column {
                if (uiState.m3uPlaylists.isEmpty() && uiState.xtreamAccounts.isEmpty() && uiState.stalkerAccounts.isEmpty() && uiState.epgSources.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No playlists added yet", color = onsurfaceContainerHigh)
                        }
                    }
                } else {
                    val m3uIds = uiState.m3uPlaylists.map { it.id }
                    val xtreamIds = uiState.xtreamAccounts.map { it.id }
                    val stalkerIds = uiState.stalkerAccounts.map { it.id }
                    val allIds = m3uIds + xtreamIds + stalkerIds

                    Spacer(Modifier.height(8.dp))
                    uiState.m3uPlaylists.forEach { playlist ->
                        val idx = allIds.indexOf(playlist.id)
                        PlaylistCard(
                            name = playlist.name,
                            channelCount = playlist.channels.size,
                            scanState = StreamValidationController.scans[playlist.id],
                            status = if (playlist.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (playlist.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "M3U",
                            isRefreshing = playlist.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshM3uChannels(playlist.id) },
                            onDelete = { IptvRepository.removeM3uPlaylist(playlist.id) },
                            channels = playlist.channels,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.xtreamAccounts.forEach { account ->
                        val idx = allIds.indexOf(account.id)
                        PlaylistCard(
                            name = account.name,
                            channelCount = account.channels.size,
                            scanState = StreamValidationController.scans[account.id],
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "XT",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshXtreamChannels(account.id) },
                            onDelete = { IptvRepository.removeXtreamAccount(account.id) },
                            channels = account.channels,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.stalkerAccounts.forEach { account ->
                        val idx = allIds.indexOf(account.id)
                        PlaylistCard(
                            name = account.name,
                            channelCount = account.channels.size,
                            scanState = StreamValidationController.scans[account.id],
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "SK",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshStalkerChannels(account.id) },
                            onDelete = { IptvRepository.removeStalkerAccount(account.id) },
                            channels = account.channels,
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.epgSources.forEach { source ->
                        PlaylistCard(
                            name = source.name,
                            channelCount = uiState.epgMatchCount,
                            scanState = null,
                            status = when {
                                uiState.epgLoading -> "Loading"
                                uiState.epgMatchCount > 0 -> "Loaded"
                                else -> "Pending"
                            },
                            statusColor = if (uiState.epgLoading || uiState.epgMatchCount > 0) onSurface else outlineVariant,
                            iconLabel = "EPG",
                            isRefreshing = uiState.epgLoading,
                            onClick = {},
                            onRefresh = { IptvRepository.refreshEpg() },
                            onDelete = { IptvRepository.removeEpgSource(source.id) },
                            channels = emptyList(),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VodSeriesSection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    val xtreamAccounts = uiState.xtreamAccounts
    val allMovies = xtreamAccounts.flatMap { it.movies }
    val allSeries = xtreamAccounts.flatMap { it.series }
    val vodLoading = uiState.vodLoading
    val vodError = uiState.vodError
    val vodLoadedIds = uiState.vodLoadedAccountIds
    val hasAnyXtream = xtreamAccounts.isNotEmpty()

    LaunchedEffect(Unit) {
        if (hasAnyXtream && vodLoadedIds.isEmpty() && !vodLoading) {
            for (acc in xtreamAccounts) {
                IptvRepository.refreshXtreamVod(acc.id)
            }
        }
    }

    if (!hasAnyXtream) return

    var tab by remember { mutableStateOf("movies") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val movies = if (selectedCategory != null) allMovies.filter { it.categoryName == selectedCategory } else allMovies
    val series = if (selectedCategory != null) allSeries.filter { it.categoryName == selectedCategory } else allSeries

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("RdNutz Movies & Series", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            if (vodError != null) {
                TextButton(onClick = {
                    for (acc in xtreamAccounts) IptvRepository.refreshXtreamVod(acc.id)
                }) {
                    Text("Retry", color = primary, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (allMovies.isEmpty() && allSeries.isEmpty()) {
            if (vodLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = primary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Loading Movies & Series...", color = onsurfaceContainerHigh, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = { for (acc in xtreamAccounts) IptvRepository.refreshXtreamVod(acc.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceContainer),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Load Movies & Series", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                if (vodError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(vodError ?: "", color = errorColor, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (tab == "movies") primary else surfaceContainer)
                    .clickable { tab = "movies"; selectedCategory = null }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Movies${if (allMovies.isNotEmpty()) " (${allMovies.size})" else ""}", color = if (tab == "movies") onPrimary else onSurface, fontSize = 13.sp, fontWeight = if (tab == "movies") FontWeight.Bold else FontWeight.Normal)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (tab == "series") primary else surfaceContainer)
                    .clickable { tab = "series"; selectedCategory = null }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Series${if (allSeries.isNotEmpty()) " (${allSeries.size})" else ""}", color = if (tab == "series") onPrimary else onSurface, fontSize = 13.sp, fontWeight = if (tab == "series") FontWeight.Bold else FontWeight.Normal)
            }
        }

        val cats = if (tab == "movies") allMovies.mapNotNull { it.categoryName }.filter { it.isNotBlank() }.distinct() else allSeries.mapNotNull { it.categoryName }.filter { it.isNotBlank() }.distinct()
        if (cats.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selectedCategory == null) primary.copy(alpha = 0.3f) else surfaceContainerLow)
                        .clickable { selectedCategory = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("All", color = if (selectedCategory == null) primary else onsurfaceContainerHigh, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                cats.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedCategory == cat) primary else surfaceContainerLow)
                            .clickable { selectedCategory = if (selectedCategory == cat) null else cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(cat, color = if (selectedCategory == cat) onPrimary else onsurfaceContainerHigh, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (tab == "movies") {
            val filtered = if (selectedCategory != null) movies else allMovies
            if (filtered.isEmpty()) {
                Text("No movies available", color = onsurfaceContainerHigh, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered.take(50)) { movie ->
                        val sub = listOf(movie.year, movie.genre).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(surfaceContainer)
                                .clickable {
if (!movie.videoUrl.isNullOrBlank()) {
                                         val ch = IptvChannel(
                                             id = movie.id,
                                             name = movie.name,
                                             logo = movie.cover?.ifBlank { null },
                                             url = movie.videoUrl!!,
                                             sourceType = SourceType.Xtream,
                                             sourceId = movie.id,
                                         )
                                        playChannel(ch, onPlayChannel)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
model = movie.cover,
                                 contentDescription = movie.name,
                                modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(movie.name, color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sub.isNotBlank()) {
                                    Text(sub, color = onsurfaceContainerHigh, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        } else {
            val filtered = if (selectedCategory != null) series else allSeries
            if (filtered.isEmpty()) {
                Text("No series available", color = onsurfaceContainerHigh, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered.take(50)) { show ->
val totalEp = show.seasons.sumOf { it.episodes.size }
                         val sub = if (totalEp > 0) "$totalEp episodes" else ""
                         val seriesStreamUrl = show.seasons.flatMap { it.episodes }.firstOrNull()?.videoUrl ?: ""
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clip(RoundedCornerShape(10.dp))
                                 .background(surfaceContainer)
                                 .clickable {
                                     if (seriesStreamUrl.isNotBlank()) {
                                         val ch = IptvChannel(
                                             id = show.id,
                                             name = show.name,
                                             logo = show.cover?.ifBlank { null },
                                             url = seriesStreamUrl,
                                             sourceType = SourceType.Xtream,
                                             sourceId = show.id,
                                         )
                                         playChannel(ch, onPlayChannel)
                                     }
                                 }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
model = show.cover,
                                 contentDescription = show.name,
                                modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(show.name, color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sub.isNotBlank()) {
                                    Text(sub, color = onsurfaceContainerHigh, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(vodError) {
        if (vodError != null) {
            NuvioToastController.show(vodError)
        }
    }
}

@Composable
private fun PlaylistChannelRow(channel: IptvChannel, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(surfaceContainer)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            ChannelLogo(
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)),
                channel = channel,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!channel.group.isNullOrBlank()) {
                Text(channel.group!!, color = onsurfaceContainerHigh, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    name: String,
    channelCount: Int,
    scanState: SourceScanState?,
    status: String,
    statusColor: Color,
    iconLabel: String,
    isRefreshing: Boolean = false,
    onClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    channels: List<IptvChannel> = emptyList(),
) {
    var expanded by remember { mutableStateOf(false) }
    val scanSubtitle = when {
        scanState == null || scanState.checked == 0 -> null
        scanState.active -> "Scanning ${scanState.checked}/${scanState.total}..."
        else -> "${scanState.alive} OK · ${scanState.dead} bad"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(iconLabel, color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (scanSubtitle != null) "$channelCount Channels · $scanSubtitle" else "$channelCount Channels",
                        color = onsurfaceContainerHigh.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = onsurfaceContainerHigh)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = onsurfaceContainerHigh,
                    )
                }
                Icon(Icons.Filled.CheckCircle, contentDescription = status, tint = statusColor, modifier = Modifier.size(20.dp))
            }

            AnimatedVisibility(visible = expanded) {
                if (channels.isEmpty()) {
                    Text(
                        "No channels in this playlist",
                        color = onsurfaceContainerHigh,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(channels) { ch ->
                            val isDead = StreamValidationStore.isKnownDeadSync(ch.url)
                            val isValidating = StreamValidationController.scans[ch.sourceId]?.active == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaceContainerLow)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!ch.logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ch.logo,
                                        contentDescription = ch.name,
                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (!ch.group.isNullOrBlank()) {
                                        Text(ch.group!!, color = onsurfaceContainerHigh, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (isDead) {
                                    Text("DEAD", color = Color.Red.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun playChannel(channel: IptvChannel, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    IptvRepository.addToHistory(channel.id)
    val channels = IptvRepository.getLastFilteredChannels()
    val channelIndex = channels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId }
    val history = IptvRepository.getHistoryChannels()
    val liveChannels = channels.filter { !StreamValidationStore.isKnownDeadSync(it.url) }
    val liveIndex = if (channelIndex >= 0) liveChannels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId } else -1
    val channelUrls = liveChannels.map { it.url }
    val channelNames = liveChannels.map { it.name }
    val startIdx = if (liveIndex >= 0) liveIndex else 0
    // Resolve VOD stream URLs (pipe-separated .ts|.m3u8) to a single usable URL
    val resolvedUrl = if (channel.url.contains('|')) {
        val parts = channel.url.split('|')
        parts.firstOrNull { it.isNotBlank() } ?: channel.url
    } else {
        channel.url
    }
    val launch = PlayerLaunch(
        profileId = 0,
        title = channel.name,
        sourceUrl = resolvedUrl,
        streamTitle = channel.name,
        providerName = "IPTV",
        parentMetaId = "iptv",
        parentMetaType = "tv",
        logo = channel.logo,
        channelNames = channelNames,
        channelUrls = channelUrls,
        channelLogos = liveChannels.map { it.logo ?: "" },
        channelIds = liveChannels.map { it.id },
        currentChannelIndex = startIdx,
        historyChannelNames = history.map { it.name },
        historyChannelUrls = history.map { it.url },
        historyChannelLogos = history.map { it.logo ?: "" },
        historyChannelIds = history.map { it.id },
    )
    onPlayChannel?.invoke(launch)
}

// ── Add Source Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceBottomSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    canAddPortal: Boolean,
    licenseStatus: LicenseStatus,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf("xtreme") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { Box(Modifier.width(48.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(outlineVariant.copy(alpha = 0.4f))) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Add Source", color = onSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                IconButton(onClick = onDismiss) {
                    Text("\u2715", color = onsurfaceContainerHigh)
                }
            }
            Spacer(Modifier.height(24.dp))

            if (!canAddPortal) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Lock, "Locked", tint = errorColor, modifier = Modifier.size(20.dp))
                            Text("PORTAL LIMIT REACHED", color = errorColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (licenseStatus) {
                                LicenseStatus.NOT_ACTIVATED -> "Add up to ${PortalLicenseManager.MAX_PORTALS} IPTV sources for free. Activate an RdNutz key for unlimited portals."
                                LicenseStatus.EXPIRED -> "Your RdNutz key has expired. Renew to add more portals."
                                LicenseStatus.GRACE -> "Your RdNutz key is expiring soon. Renew for unlimited portals."
                                LicenseStatus.WRONG_DEVICE -> "This key is registered to another device."
                                LicenseStatus.INVALID -> "Invalid key format."
                                else -> "Portal limit reached (${PortalLicenseManager.MAX_PORTALS} max for free users)."
                            },
                            color = onsurfaceContainerHigh,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surfaceContainerLow).padding(4.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                listOf("xtreme" to "XTREME", "m3u" to "M3U URL", "m3ufile" to "M3U FILE", "stalker" to "STALKER", "epg" to "EPG", "portal" to "PORTAL").forEach { (id, label) ->
                    Surface(
                        onClick = { mode = id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (mode == id) primary else Color.Transparent,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = if (mode == id) onPrimary else onsurfaceContainerHigh,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            when (mode) {
                "xtreme" -> XtreamForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
                "m3u" -> M3uForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
                "m3ufile" -> M3uFileForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
                "stalker" -> StalkerForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
                "epg" -> EpgForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
                "portal" -> PortalForm(onSuccess = onSuccess)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun XtreamForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column {
        InputField(label = "ACCOUNT NAME", value = name, onValueChange = { name = it }, placeholder = "My Provider")
        Spacer(Modifier.height(16.dp))
        InputField(label = "PORTAL URL", value = server, onValueChange = { server = it }, placeholder = "http://provider-url.com:8080")
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InputField(label = "USERNAME", value = username, onValueChange = { username = it }, placeholder = "User123", modifier = Modifier.weight(1f))
            InputField(label = "PASSWORD", value = password, onValueChange = { password = it }, placeholder = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", isPassword = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (name.isNotBlank() && server.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                    IptvRepository.addXtreamAccount(name, server, username, password)
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = name.isNotBlank() && server.isNotBlank() && username.isNotBlank() && password.isNotBlank() && canAddPortal,
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun M3uForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column {
        InputField(label = "M3U PLAYLIST URL", value = url, onValueChange = { url = it }, placeholder = "https://domain.com/playlist.m3u")
        Spacer(Modifier.height(16.dp))
        InputField(label = "PLAYLIST NAME (OPTIONAL)", value = name, onValueChange = { name = it }, placeholder = "My Favorite Channels")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (url.isNotBlank()) {
                    IptvRepository.addM3uPlaylist(name.ifBlank { url.take(30) }, url)
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = url.isNotBlank() && canAddPortal,
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun M3uFileForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }

    val pickFile = rememberFilePickerLauncher { name, content ->
        if (!canAddPortal) return@rememberFilePickerLauncher
        selectedFileName = name
        parseError = null
        isParsing = true
        try {
            IptvRepository.parseM3uContent(content, name.removeSuffix(".m3u").take(30))
            onSuccess()
        } catch (e: Exception) {
            parseError = e.message ?: "Failed to parse M3U file"
        }
        isParsing = false
    }

    Column {
        Text("SELECT M3U FILE", color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { pickFile() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = !isParsing && canAddPortal,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Add, null, tint = onPrimary, modifier = Modifier.size(20.dp))
                Text("CHOOSE FILE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        if (selectedFileName != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                Text(selectedFileName!!, color = onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (parseError != null) {
            Spacer(Modifier.height(8.dp))
            Text(parseError!!, color = errorColor, fontSize = 13.sp)
        }

        if (isParsing) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primary, strokeWidth = 2.dp)
                Text("Loading playlist...", color = onsurfaceContainerHigh, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EpgForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var subMode by remember { mutableStateOf("url") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surfaceContainerLow).padding(4.dp),
        ) {
            listOf("url" to "URL", "file" to "FILE").forEach { (id, label) ->
                Surface(
                    onClick = { subMode = id },
                    shape = RoundedCornerShape(12.dp),
                    color = if (subMode == id) primary else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (subMode == id) onPrimary else onsurfaceContainerHigh,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (subMode) {
            "url" -> EpgUrlForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
            "file" -> EpgFileForm(onSuccess = onSuccess, canAddPortal = canAddPortal)
        }
    }
}

@Composable
private fun EpgUrlForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column {
        Text(
            "Add an EPG guide by URL. The iptv-org EPG is added automatically when you add the iptv-org playlist.",
            color = onsurfaceContainerHigh,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        InputField(label = "EPG XML URL", value = url, onValueChange = { url = it }, placeholder = "https://domain.com/epg.xml")
        Spacer(Modifier.height(16.dp))
        InputField(label = "GUIDE NAME (OPTIONAL)", value = name, onValueChange = { name = it }, placeholder = "My EPG Guide")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (url.isNotBlank()) {
                    IptvRepository.addEpgSource(name.ifBlank { "EPG Guide" }, url)
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = url.isNotBlank() && canAddPortal,
        ) {
            Text("ADD EPG GUIDE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EpgFileForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val pickFile = rememberFilePickerLauncher { name, content ->
        if (!canAddPortal) return@rememberFilePickerLauncher
        selectedFileName = name
        parseError = null
        isUploading = true
        try {
            IptvRepository.addUploadedEpgSource(name.removeSuffix(".xml").removeSuffix(".gz").take(30).ifBlank { "EPG Guide" }, content)
            onSuccess()
        } catch (e: Exception) {
            parseError = e.message ?: "Failed to read EPG file"
        }
        isUploading = false
    }

    Column {
        Text("SELECT EPG XML FILE", color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { pickFile() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = !isUploading && canAddPortal,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Add, null, tint = onPrimary, modifier = Modifier.size(20.dp))
                Text("CHOOSE FILE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        if (selectedFileName != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                Text(selectedFileName!!, color = onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (parseError != null) {
            Spacer(Modifier.height(8.dp))
            Text(parseError!!, color = errorColor, fontSize = 13.sp)
        }

        if (isUploading) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primary, strokeWidth = 2.dp)
                Text("Uploading guide...", color = onsurfaceContainerHigh, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StalkerForm(onSuccess: () -> Unit, canAddPortal: Boolean) {
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }

    Column {
        InputField(label = "ACCOUNT NAME", value = name, onValueChange = { name = it }, placeholder = "My Stalker Portal")
        Spacer(Modifier.height(16.dp))
        InputField(label = "PORTAL URL", value = server, onValueChange = { server = it }, placeholder = "http://portal-url.com")
        Spacer(Modifier.height(16.dp))
        InputField(label = "MAC ADDRESS", value = macAddress, onValueChange = { macAddress = it }, placeholder = "00:1A:79:XX:XX:XX")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (name.isNotBlank() && server.isNotBlank() && macAddress.isNotBlank()) {
                    IptvRepository.addStalkerAccount(name, server, macAddress)
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            enabled = name.isNotBlank() && server.isNotBlank() && macAddress.isNotBlank() && canAddPortal,
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PortalForm(onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var englishOnly by remember { mutableStateOf(true) }
    var noAdult by remember { mutableStateOf(true) }
    var sportsOnly by remember { mutableStateOf(false) }
    var adultOnly by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PortalNutzEntry>?>(null) }
    var progressMsg by remember { mutableStateOf("") }
    var autoLoaded by remember { mutableStateOf(false) }
    val repoUiState by IptvRepository.uiState.collectAsStateWithLifecycle()
    val installedKeys by PortalInstallStore.installed.collectAsStateWithLifecycle()

    val license = PortalLicenseManager.getSavedLicense()
    val licenseStatus = PortalLicenseManager.checkStatus(license)
    val hasAccess = licenseStatus == LicenseStatus.VALID || licenseStatus == LicenseStatus.GRACE
    var remainingTime by remember { mutableStateOf("") }
    LaunchedEffect(license) {
        if (license != null) {
            remainingTime = PortalLicenseManager.getRemainingTime(license)
        } else {
            remainingTime = ""
        }
    }

    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var keySuccess by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { PortalNutzScraper.cancel() }
    }

    Column {
        if (!hasAccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, "Locked", tint = errorColor, modifier = Modifier.size(20.dp))
                        Text("RD NUTZ LISTS LOCKED", color = errorColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (licenseStatus) {
                            LicenseStatus.NOT_ACTIVATED -> "This feature requires an active RdNutz key. Donate to unlock access."
                            LicenseStatus.EXPIRED -> "Your RdNutz key has expired. Donate to renew access."
                            LicenseStatus.GRACE -> "Your RdNutz key is expiring soon. Renew to continue."
                            LicenseStatus.WRONG_DEVICE -> "This key is registered to another device."
                            LicenseStatus.INVALID -> "Invalid key format. Please check and try again."
                            else -> "This feature requires an active RdNutz key."
                        },
                        color = onsurfaceContainerHigh,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it; keyError = null },
                        placeholder = { Text("NVIO-XXXX-XXXX-XXXX", color = onsurfaceContainerHigh.copy(alpha = 0.5f), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = outlineVariant.copy(alpha = 0.3f),
                            cursorColor = primary,
                            focusedContainerColor = surfaceContainerLowest,
                            unfocusedContainerColor = surfaceContainerLowest,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (keyError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(keyError!!, color = errorColor, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val trimmed = keyInput.trim()
                            if (trimmed.isBlank()) {
                                keyError = "Enter your RdNutz key"
                                return@Button
                            }
                            val result = PortalLicenseManager.verifyKey(trimmed)
                            when (result) {
                                is LicenseResult.Success -> {
                                    PortalLicenseManager.saveActivation(result.license)
                                    keySuccess = true
                                    keyInput = ""
                                    remainingTime = PortalLicenseManager.getRemainingTime(result.license)
                                }
                                is LicenseResult.Failure -> {
                                    keyError = result.message
                                    keySuccess = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                    ) {
                        Text("ACTIVATE KEY", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    if (keySuccess) {
                        Spacer(Modifier.height(8.dp))
                        Text("Key activated! You now have access to RdNutz Lists.", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        "Active",
                        tint = if (licenseStatus == LicenseStatus.GRACE) Color(0xFFFFA000) else Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "RdNutz Lists Active",
                        color = if (licenseStatus == LicenseStatus.GRACE) Color(0xFFFFA000) else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(remainingTime, color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (hasAccess) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "English" to englishOnly to { englishOnly = !englishOnly },
                    "No XXX" to noAdult to { noAdult = !noAdult },
                    "Sports" to sportsOnly to { sportsOnly = !sportsOnly },
                    "XXX" to adultOnly to { adultOnly = !adultOnly },
                ).forEach { (pair, toggle) ->
                    val (label, selected) = pair
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (selected) primary else surfaceContainerLow)
                            .border(if (!selected) 1.dp else 0.dp, outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable(onClick = toggle)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(label, color = if (selected) onPrimary else onsurfaceContainerHigh,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

// Auto-load results on first access
    LaunchedEffect(hasAccess) {
        if (hasAccess && !autoLoaded && results == null && !searching) {
            autoLoaded = true
            searching = true
                    searchError = null
                    results = null
                    progressMsg = ""
                    autoLoaded = true
                    PortalNutzScraper.scrape(
                        englishOnly = englishOnly,
                        noAdult = noAdult,
                        sportsOnly = sportsOnly,
                        adultOnly = adultOnly,
                        excludeServers = repoUiState.xtreamAccounts.map { it.server }.toSet(),
                        onEvent = { event ->
                            when (event) {
                                is PortalNutzScraper.ScrapeEvent.Progress -> progressMsg = event.message
                                is PortalNutzScraper.ScrapeEvent.Result -> {
                                    results = event.portals
                                    searching = false
                                    progressMsg = ""
                                }
                                is PortalNutzScraper.ScrapeEvent.Error -> {
                                    searchError = event.message
                                    searching = false
                                    progressMsg = ""
                                }
                            }
                        },
                    )
                }
            }

            if (searching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primary, strokeWidth = 2.dp)
                    Text(progressMsg, color = onsurfaceContainerHigh, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            searchError?.let { err ->
                Text(err, color = errorColor, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            // Refresh button
            Button(
                onClick = {
                    searching = true
                    searchError = null
                    results = null
                    progressMsg = ""
                    PortalNutzScraper.scrape(
                        englishOnly = englishOnly,
                        noAdult = noAdult,
                        sportsOnly = sportsOnly,
                        adultOnly = adultOnly,
                        excludeServers = repoUiState.xtreamAccounts.map { it.server }.toSet(),
                        onEvent = { event ->
                            when (event) {
                                is PortalNutzScraper.ScrapeEvent.Progress -> progressMsg = event.message
                                is PortalNutzScraper.ScrapeEvent.Result -> {
                                    results = event.portals
                                    searching = false
                                    progressMsg = ""
                                }
                                is PortalNutzScraper.ScrapeEvent.Error -> {
                                    searchError = event.message
                                    searching = false
                                    progressMsg = ""
                                }
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                enabled = !searching,
            ) {
                Text("REFRESH NUTZ", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(12.dp))

results?.let { raw ->
                val filtered = raw
                    .sortedByDescending { portalHealthScore(it) }
                    .filter { it.channelCount > 0 }
                if (filtered.isEmpty() && results!!.isNotEmpty()) {
                    Text("No working portals found. Try adjusting filters.", color = onsurfaceContainerHigh, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { entry ->
                        val isAdded = installedKeys.contains("${entry.url}|${entry.username}")
                        val isExpiringSoon = entry.expDate != null && entry.expDate - TraktPlatformClock.nowEpochMs() < 7L * 24 * 60 * 60 * 1000L
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(surfaceContainer)
                                .border(if (isAdded) 1.dp else 0.dp, if (isAdded) Color(0xFF4CAF50) else outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text(entry.label.replace("portal", "P"), color = primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.domain, color = primary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${entry.channelCount} channels", color = onsurfaceContainerHigh, fontSize = 12.sp)
                                    entry.activeConnections?.let { ac ->
                                        Text("•", color = onsurfaceContainerHigh.copy(alpha = 0.4f), fontSize = 12.sp)
                                        Text("$ac active", color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                    if (isExpiringSoon) {
                                        Text("•", color = onsurfaceContainerHigh.copy(alpha = 0.4f), fontSize = 12.sp)
                                        Text("Expiring", color = Color(0xFFFFA000), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        entry.expDate?.let { exp ->
                                            Text("•", color = onsurfaceContainerHigh.copy(alpha = 0.4f), fontSize = 12.sp)
                                            Text(formatExpDate(exp), color = Color(0xFFFFA000), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            if (isAdded) {
                                Text("ADDED", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            } else {
                                Button(
                                    onClick = {
                                        IptvRepository.addXtreamAccount(entry.label, entry.url, entry.username, entry.password)
                                        PortalInstallStore.markInstalled(entry.url, entry.username)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                ) {
                                    Text("ADD", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, color = onsurfaceContainerHigh, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = onsurfaceContainerHigh.copy(alpha = 0.4f), fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = onSurface, fontSize = 14.sp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primary,
                unfocusedBorderColor = outlineVariant.copy(alpha = 0.3f),
                cursorColor = primary,
                focusedContainerColor = surfaceContainerLowest,
                unfocusedContainerColor = surfaceContainerLowest,
            ),
        )
    }
}

private fun formatTime(epochMs: Long): String {
    val totalSeconds = epochMs / 1000L
    val hours = ((totalSeconds / 3600) % 24).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun formatExpDate(epochMs: Long): String {
    if (epochMs <= 0L) return "Never"
    val daysRemaining = (epochMs - com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs()) / 86400000L
    return when {
        daysRemaining < 0 -> "Expired"
        daysRemaining == 0L -> "Today"
        daysRemaining == 1L -> "1 day left"
        daysRemaining < 365 -> "$daysRemaining days left"
        else -> "${daysRemaining / 365} yr left"
    }
}

/**
 * Health score for a portalnutz result. Higher = more usable. Sorts results by it.
 * - channelCount dominates (more channels = better catalog).
 * - activeConnections ratio penalizes overloaded portals.
 * - expDate proximity rewards portals with longer remaining life.
 */
private fun portalHealthScore(e: PortalNutzEntry): Double {
    val now = TraktPlatformClock.nowEpochMs()
    val channels = e.channelCount.coerceAtLeast(0).toDouble()
    val active = (e.activeConnections ?: 0).toDouble()
    val max = (e.maxConnections ?: 0).toDouble()
    val loadPenalty = if (max > 0 && active > max) active / max else 0.0
    val expBonus = when {
        e.expDate == null -> 0.0
        e.expDate <= now -> -100.0
        else -> {
            val daysLeft = (e.expDate - now) / (24 * 60 * 60 * 1000.0)
            if (daysLeft > 365) 100.0 else daysLeft / 3.65
        }
    }
    return channels * 10.0 - loadPenalty * 5.0 + expBonus
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickChannelSourcesSheet(
    quickChannel: QuickChannel,
    allChannels: List<IptvChannel>,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sourceNames = remember { IptvRepository.getAllSourceNames() }
    val sourceIds = remember { IptvRepository.getAllSourceIds() }
    val sourceNameForId = remember(sourceNames, sourceIds) {
        sourceIds.zip(sourceNames).toMap()
    }
    val xtreamAccountById = remember { IptvRepository.getXtreamAccounts().associateBy { it.id } }
    val now = TraktPlatformClock.nowEpochMs()

    var matches by remember { mutableStateOf<List<IptvChannel>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var aliveByUrl by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(quickChannel, reload) {
        matches = null
        error = null
        aliveByUrl = emptyMap()
        val found = withContext(Dispatchers.Default) {
            allChannels.filter { ch -> QuickChannelList.matches(quickChannel, ch) }
        }
        if (found.isEmpty()) {
            error = "No sources found for \"${quickChannel.displayName}\""
            matches = emptyList()
            return@LaunchedEffect
        }
        matches = found
        StreamValidationController.resolveChannelsStatuses(found) { statuses ->
            aliveByUrl = statuses
        }
    }

    val visible = matches?.filter { aliveByUrl[it.url] != false && !StreamValidationStore.isKnownDeadSync(it.url) } ?: emptyList()
    val checkingCount = matches?.count { aliveByUrl[it.url] == null } ?: 0
    val workingCount = matches?.count { aliveByUrl[it.url] == true } ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        when {
            matches == null -> {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Resolving ${quickChannel.displayName}...", color = onSurface, fontSize = 14.sp)
                    Text("Scanning IPTV playlists", color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.height(48.dp))
                }
            }
            error != null && matches?.isEmpty() == true -> {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠", color = errorColor, fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(error ?: "", color = onSurface, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(primary).clickable { reload++ }
                        .padding(horizontal = 24.dp, vertical = 10.dp)) {
                        Text("Retry", color = onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
            else -> {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().height(440.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(quickChannel.displayName, color = onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.weight(1f))
                        if (checkingCount > 0) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            "${workingCount} working${if (checkingCount > 0) " · checking $checkingCount" else ""}",
                            color = onsurfaceContainerHigh,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (visible.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (checkingCount > 0) "Checking ${quickChannel.displayName} streams..." else "No working sources for \"${quickChannel.displayName}\"",
                                color = onsurfaceContainerHigh.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                            itemsIndexed(visible, key = { index, ch -> "src_${index}_${ch.id}_${ch.sourceId}" }) { _, ch ->
                                val alive = aliveByUrl[ch.url]
                                val provider = sourceNameForId[ch.sourceId] ?: "Unknown"
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(surfaceContainer).clickable { playChannel(ch, onPlayChannel); onDismiss() }.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (!ch.logo.isNullOrBlank()) {
                                        AsyncImage(model = ch.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(surfaceContainerLow))
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ch.name, color = onSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(provider, color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            if (ch.group != null) {
                                                Text(ch.group, color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                                            }
                                            if (alive == null) {
                                                Spacer(Modifier.width(4.dp))
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = primary.copy(alpha = 0.6f), strokeWidth = 2.dp)
                                            }
                                        }
                                        EpgNowNextRow(programs = rememberEpgSteps(xtreamAccountById[ch.sourceId], ch, limit = 2), now = now)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (alive == true) {
                                        Text("✓ Live", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else if (alive == null) {
                                        Text("checking", color = onsurfaceContainerHigh.copy(alpha = 0.5f), fontSize = 11.sp)
                                    } else {
                                        Text("Play", color = primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
@Composable
fun QuickChannelsSection(
    allChannels: List<IptvChannel>,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
    showLogos: Boolean = false,
) {
    var qcRegion by remember { mutableStateOf("All") }
    var selectedQc by remember { mutableStateOf<QuickChannel?>(null) }
    val qcTabs = listOf("All", "US", "UK", "CA", "Bay Area", "Premium", "Sports", "News", "Custom")
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAliases by remember { mutableStateOf("") }
    var newRegions by remember { mutableStateOf("") }
    var newTags by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Quick Channels", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(qcTabs) { tab ->
                val isActive = qcRegion == tab
                Card(
                    onClick = { qcRegion = tab },
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) primary else surfaceContainer,
                    ),
                ) {
                    Box(
                        modifier = Modifier.height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                tab,
                                color = if (isActive) onPrimary else onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        val filtered = remember(qcRegion) {
            when (qcRegion) {
                "All" -> QuickChannelList.all
                "US" -> QuickChannelList.getChannelsForCategory("us")
                "UK" -> QuickChannelList.getChannelsForCategory("uk")
                "CA" -> QuickChannelList.getChannelsForCategory("ca")
                "Bay Area" -> QuickChannelList.getChannelsForCategory("bay-area")
                "Premium" -> QuickChannelList.getChannelsForCategory("premium")
                "Sports" -> QuickChannelList.getChannelsForCategory("sports")
                "News" -> QuickChannelList.getChannelsForCategory("news")
                "Custom" -> QuickChannelList.getCustomChannels()
                else -> QuickChannelList.all
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(filtered, key = { index, qc -> "qc_${index}_${qc.displayName}" }) { _, qc ->
                val matchedChannel = if (showLogos) {
                    remember(qc, allChannels) {
                        allChannels.find { QuickChannelList.matches(qc, it) }
                    }
                } else null
                Card(
                    onClick = { selectedQc = qc },
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showLogos && matchedChannel?.logo?.isNotBlank() == true) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalPlatformContext.current)
                                        .data(matchedChannel.logo)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = qc.displayName,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                qc.displayName,
                                color = onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            if (!showLogos) {
                                Text("▸ select", color = primary.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (qcRegion == "Custom") {
            Spacer(Modifier.height(8.dp))
            val customChannels = QuickChannelList.getCustomChannels()
            if (customChannels.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(customChannels, key = { index, qc -> "cust_qc_${index}_${qc.displayName}" }) { _, qc ->
                        Card(
                            modifier = Modifier.width(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (showLogos && qc.displayName.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                                .data("") // no logo for custom, could add later
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = qc.displayName,
                                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Text(
                                        qc.displayName,
                                        color = onSurface,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(top = 4.dp)) {
                                        IconButton(onClick = { QuickChannelList.removeCustomChannel(qc.displayName) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = errorColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = {
                    newName = ""
                    newAliases = ""
                    newRegions = ""
                    newTags = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = primary.copy(alpha = 0.15f), contentColor = primary),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Text("Add Custom Channel", color = primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Custom Quick Channel", color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Display Name", color = onsurfaceContainerHigh.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newAliases,
                            onValueChange = { newAliases = it },
                            placeholder = { Text("Aliases (comma separated)", color = onsurfaceContainerHigh.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newRegions,
                            onValueChange = { newRegions = it },
                            placeholder = { Text("Regions (comma separated: US, UK, CA, bay-area)", color = onsurfaceContainerHigh.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newTags,
                            onValueChange = { newTags = it },
                            placeholder = { Text("Tags (comma separated: news, sports, premium, etc.)", color = onsurfaceContainerHigh.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = newName.trim()
                        if (name.isNotBlank()) {
                            val aliases = newAliases.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val regions = newRegions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val tags = newTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val channel = QuickChannel(
                                displayName = name,
                                aliases = if (aliases.isEmpty()) listOf(name) else aliases,
                                regions = if (regions.isEmpty()) listOf("US") else regions,
                                tags = tags,
                            )
                            QuickChannelList.addCustomChannel(channel)
                            CustomQuickChannelStore.addChannel(channel)
                        }
                        showAddDialog = false
                    }) {
                        Text("Add", color = primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = onsurfaceContainerHigh)
                    }
                },
            )
        }

    selectedQc?.let { qc ->
        QuickChannelSourcesSheet(
            quickChannel = qc,
            allChannels = allChannels,
            onPlayChannel = onPlayChannel,
            onDismiss = { selectedQc = null },
        )
    }
}
}

