package com.nuvio.app.features.iptv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.features.hub.MultiWindowStore
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

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
    val uiState by IptvRepository.uiState.collectAsStateWithLifecycle()
    var showAddSourceSheet by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pickerChannel by remember { mutableStateOf<IptvChannel?>(null) }

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
            )
        } else {
            IptvMobileMode(
                uiState = uiState,
                onPlayChannel = onPlayChannel,
                scrollToTopRequests = scrollToTopRequests,
                onAddSource = { showAddSourceSheet = true },
                onPickerChannel = { pickerChannel = it },
            )
        }
    }

    if (showAddSourceSheet) {
        AddSourceBottomSheet(
            onDismiss = { showAddSourceSheet = false },
            onSuccess = { showAddSourceSheet = false }
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
) {
    val now = TraktPlatformClock.nowEpochMs()
    val scrollState = rememberScrollState()
    val allChannels = uiState.m3uPlaylists.flatMap { it.channels } +
            uiState.xtreamAccounts.flatMap { it.channels } +
            uiState.stalkerAccounts.flatMap { it.channels }
    val favorites = IptvRepository.getFavoriteChannels()
    val history = IptvRepository.getHistoryChannels()
    val playlists = uiState.m3uPlaylists + uiState.xtreamAccounts.map { acc ->
        M3uPlaylist(id = acc.id, name = acc.name, url = acc.server, channels = acc.channels)
    } + uiState.stalkerAccounts.map { acc ->
        M3uPlaylist(id = acc.id, name = acc.name, url = acc.server, channels = acc.channels)
    }

    var showSearch by remember { mutableStateOf(false) }

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
            Icon(Icons.Filled.Notifications, "Notifications", tint = onSurface, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Filled.AccountCircle, "Account", tint = onSurface, modifier = Modifier.size(28.dp))
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
            val displayChannels = if (uiState.searchQuery.isNotBlank()) {
                allChannels.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        (it.group?.contains(uiState.searchQuery, ignoreCase = true) == true) }
            } else allChannels

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
                items(displayChannels.take(10), key = { "${it.id}_${it.sourceId}" }) { channel ->
                    TvChannelCard(
                        channel = channel,
                        isFavorite = channel.id in uiState.favoriteChannelIds,
                        now = now,
                        onPlay = { playChannel(channel, onPlayChannel) },
                        onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
                        onAddToMultiWindow = { onPickerChannel(channel) },
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Playlists Section
            PlaylistsTvSection(
                m3uPlaylists = uiState.m3uPlaylists,
                xtreamAccounts = uiState.xtreamAccounts,
                stalkerAccounts = uiState.stalkerAccounts,
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

            // Recently Watched (History)
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recently Watched", color = primary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(history.take(12), key = { i, ch -> "tv_hist_${i}_${ch.id}_${ch.sourceId}" }) { _, channel ->
                        HistoryTvCard(
                            channel = channel,
                            onPlay = { playChannel(channel, onPlayChannel) },
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
    val totalSeconds = ms / 1000L
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

@Composable
private fun TvChannelCard(
    channel: IptvChannel,
    isFavorite: Boolean,
    now: Long,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToMultiWindow: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceContainerHigh)
            .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay),
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(model = channel.logo, contentDescription = channel.name,
                modifier = Modifier.fillMaxSize().padding(24.dp), contentScale = ContentScale.Fit)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LiveTv, null, tint = onsurfaceContainerHigh.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f)))))
        Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935)))
        }
        Box(Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).align(Alignment.Center)) // hover overlay placeholder
        Box(Modifier.align(Alignment.Center)) {
            Icon(Icons.Filled.PlayArrow, "Play", tint = primary, modifier = Modifier.size(48.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Column {
                Text(channel.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.group?.uppercase() ?: "LIVE", color = onsurfaceContainerHigh, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun HistoryTvCard(channel: IptvChannel, onPlay: () -> Unit) {
    Box(
        modifier = Modifier.width(240.dp).aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceContainerHigh)
            .border(0.5.dp, outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay),
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
        Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(channel.name, color = primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 12.dp).fillMaxWidth(0.85f)) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(primary.copy(alpha = 0.2f))) {
                val pct = 0.7f // placeholder progress
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(primary))
            }
        }
    }
}

@Composable
private fun PlaylistsTvSection(
    m3uPlaylists: List<M3uPlaylist>,
    xtreamAccounts: List<XtreamAccount>,
    stalkerAccounts: List<StalkerAccount>,
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
                subtitle = "${pl.channels.size} Channels",
                status = if (pl.channels.isNotEmpty()) "Connected" else "Pending",
                statusColor = if (pl.channels.isNotEmpty()) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = pl.id in refreshingIds,
                onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                onRefresh = { onRefreshM3u(pl.id) },
                onDelete = { onDeleteM3u(pl.id) },
            )
        }
        xtreamAccounts.forEach { acc ->
            val idx = allIds.indexOf(acc.id)
            TvPlaylistCard(
                iconLabel = "XT",
                name = acc.name,
                subtitle = "${acc.channels.size} Channels",
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
                subtitle = "${acc.channels.size} Channels",
                status = if (acc.channels.isNotEmpty()) "Connected" else "Pending",
                statusColor = if (acc.channels.isNotEmpty()) Color(0xFF4CAF50) else outlineVariant,
                isRefreshing = acc.id in refreshingIds,
                onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                onRefresh = { onRefreshStalker(acc.id) },
                onDelete = { onDeleteStalker(acc.id) },
            )
        }
    }
}

@Composable
private fun TvPlaylistCard(
    iconLabel: String,
    name: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    isRefreshing: Boolean,
    onClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
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
                Text(subtitle, color = onsurfaceContainerHigh, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
) {
    val listState = rememberLazyListState()
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
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

            item { QuickAccessSection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { HistorySection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item {
                val allCh = uiState.m3uPlaylists.flatMap { it.channels } +
                    uiState.xtreamAccounts.flatMap { it.channels } +
                    uiState.stalkerAccounts.flatMap { it.channels }
                QuickChannelsSection(allChannels = allCh, onPlayChannel = onPlayChannel)
            }

            item { PlaylistsSection(uiState = uiState, onAddClick = onAddSource) }

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
                    grouped.forEach { (group, chs) ->
                        val isExpanded = group in expandedGroups
                        item(key = "grp_$group") {
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
                            itemsIndexed(chunked, key = { index, row -> "${group}_${index}_" + row.joinToString("-") { "${it.id}_${it.sourceId}" } }) { index, rowChannels ->
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
                                                onPlay = { playChannel(channel, onPlayChannel) },
                                                onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
                                                onAddToMultiWindow = { onPickerChannel(channel) },
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

                val allFavorites = IptvRepository.getFavoriteChannels()
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
                        itemsIndexed(favChunked, key = { index, row -> "allfav_${index}_" + row.joinToString("-") { "${it.id}_${it.sourceId}" } }) { index, rowChannels ->
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
                                            onPlay = { playChannel(channel, onPlayChannel) },
                                            onToggleFavorite = { IptvRepository.toggleFavorite(channel.id) },
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
) {
    val favorites = IptvRepository.getFavoriteChannels()

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
                QuickAccessCard(channel = channel, onPlay = { playChannel(channel, onPlayChannel) })
            }
        }
    }
}

@Composable
private fun HistorySection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    val history = IptvRepository.getHistoryChannels()
    if (history.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(history.take(15), key = { index, channel -> "hist_${index}_${channel.id}_${channel.sourceId}" }) { index, channel ->
                QuickAccessCard(channel = channel, onPlay = { playChannel(channel, onPlayChannel) })
            }
        }
    }
}

@Composable
private fun QuickAccessCard(channel: IptvChannel, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable(onClick = onPlay),
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
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(6.dp).clip(CircleShape).background(onSurface),
            )
        }
    }
}

@Composable
private fun SourceChipsSection(uiState: IptvUiState) {
    val sourceNames = IptvRepository.getAllSourceNames()
    val sourceIds = IptvRepository.getAllSourceIds()
    val allChannels = uiState.m3uPlaylists.flatMap { it.channels } + uiState.xtreamAccounts.flatMap { it.channels }
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
                    else -> emptyList()
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

@Composable
private fun ChannelCard(
    channel: IptvChannel,
    now: Long,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
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
            .clickable(onClick = onPlay)
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
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(onSurface),
                )

                Row(Modifier.align(Alignment.TopEnd).padding(end = 4.dp)) {
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
private fun PlaylistsSection(uiState: IptvUiState, onAddClick: () -> Unit) {
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
                if (uiState.m3uPlaylists.isEmpty() && uiState.xtreamAccounts.isEmpty() && uiState.stalkerAccounts.isEmpty()) {
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
                            subtitle = "${playlist.channels.size} Channels",
                            status = if (playlist.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (playlist.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "M3U",
                            isRefreshing = playlist.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshM3uChannels(playlist.id) },
                            onDelete = { IptvRepository.removeM3uPlaylist(playlist.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.xtreamAccounts.forEach { account ->
                        val idx = allIds.indexOf(account.id)
                        PlaylistCard(
                            name = account.name,
                            subtitle = "${account.channels.size} Channels",
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "XT",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshXtreamChannels(account.id) },
                            onDelete = { IptvRepository.removeXtreamAccount(account.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.stalkerAccounts.forEach { account ->
                        val idx = allIds.indexOf(account.id)
                        PlaylistCard(
                            name = account.name,
                            subtitle = "${account.channels.size} Channels",
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) onSurface else outlineVariant,
                            iconLabel = "SK",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onClick = { if (idx >= 0) IptvRepository.selectSource(idx) },
                            onRefresh = { IptvRepository.refreshStalkerChannels(account.id) },
                            onDelete = { IptvRepository.removeStalkerAccount(account.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    name: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    iconLabel: String,
    isRefreshing: Boolean = false,
    onClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                Text(subtitle, color = onsurfaceContainerHigh.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Icon(Icons.Filled.CheckCircle, contentDescription = status, tint = statusColor, modifier = Modifier.size(20.dp))
        }
    }
}

private fun playChannel(channel: IptvChannel, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    IptvRepository.addToHistory(channel.id)
    val channels = IptvRepository.getLastFilteredChannels()
    val channelIndex = channels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId }
    val history = IptvRepository.getHistoryChannels()
    val channelUrls = channels.map { it.url }
    val channelNames = channels.map { it.name }
    val startIdx = if (channelIndex >= 0) channelIndex else 0
    val launch = PlayerLaunch(
        profileId = 0,
        title = channel.name,
        sourceUrl = channel.url,
        streamTitle = channel.name,
        providerName = "IPTV",
        parentMetaId = "iptv",
        parentMetaType = "tv",
        logo = channel.logo,
        channelNames = channelNames,
        channelUrls = channelUrls,
        channelLogos = channels.map { it.logo ?: "" },
        channelIds = channels.map { it.id },
        currentChannelIndex = startIdx,
        historyChannelNames = history.map { it.name },
        historyChannelUrls = history.map { it.url },
        historyChannelLogos = history.map { it.logo ?: "" },
        historyChannelIds = history.map { it.id },
        autoPlayQueueUrls = channelUrls,
        autoPlayQueueTitles = channelNames,
        autoPlayQueueIndex = startIdx,
    )
    val id = PlayerLaunchStore.put(launch)
    PlayerLaunchStore.get(id)?.let { onPlayChannel?.invoke(it) }
}

// ── Add Source Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceBottomSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
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

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surfaceContainerLow).padding(4.dp),
            ) {
                listOf("xtreme" to "XTREME", "m3u" to "M3U URL", "m3ufile" to "M3U FILE", "stalker" to "STALKER", "portal" to "PORTAL").forEach { (id, label) ->
                    Surface(
                        onClick = { mode = id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (mode == id) primary else Color.Transparent,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = if (mode == id) onPrimary else onsurfaceContainerHigh,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            when (mode) {
                "xtreme" -> XtreamForm(onSuccess = onSuccess)
                "m3u" -> M3uForm(onSuccess = onSuccess)
                "m3ufile" -> M3uFileForm(onSuccess = onSuccess)
                "stalker" -> StalkerForm(onSuccess = onSuccess)
                "portal" -> PortalForm(onSuccess = onSuccess)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun XtreamForm(onSuccess: () -> Unit) {
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
            enabled = name.isNotBlank() && server.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun M3uForm(onSuccess: () -> Unit) {
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
            enabled = url.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun M3uFileForm(onSuccess: () -> Unit) {
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }

    val pickFile = rememberFilePickerLauncher { name, content ->
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
            enabled = !isParsing,
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
private fun StalkerForm(onSuccess: () -> Unit) {
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
            enabled = name.isNotBlank() && server.isNotBlank() && macAddress.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PortalForm(onSuccess: () -> Unit) {
    var englishOnly by remember { mutableStateOf(true) }
    var noAdult by remember { mutableStateOf(true) }
    var sportsOnly by remember { mutableStateOf(false) }
    var adultOnly by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PortalNutzEntry>?>(null) }
    var progressMsg by remember { mutableStateOf("") }
    val repoUiState by IptvRepository.uiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose { PortalNutzScraper.cancel() }
    }

    Column {
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
            Text("SEARCH PORTALS", color = onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))

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

        results?.let { portals ->
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                portals.forEach { entry ->
                    val isAdded = repoUiState.xtreamAccounts.any { it.name == entry.label || it.name.lowercase().startsWith("portal${entry.label.filter { it.isDigit() }}") }
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
                            Text("${entry.channelCount} channels", color = onsurfaceContainerHigh, fontSize = 12.sp)
                        }
                        if (isAdded) {
                            Text("ADDED", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Button(
                                onClick = {
                                    IptvRepository.addXtreamAccount(entry.label, entry.url, entry.username, entry.password)
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

@Composable
private fun QuickChannelsSection(
    allChannels: List<IptvChannel>,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    var qcRegion by remember { mutableStateOf("All") }
    var selectedQc by remember { mutableStateOf<QuickChannel?>(null) }
    val qcTabs = listOf("All", "US", "UK", "CA", "Bay Area", "Premium", "Sports", "News")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Quick Channels", color = onSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(qcTabs) { tab ->
                val isActive = qcRegion == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) primary else surfaceContainer)
                        .clickable { qcRegion = tab }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        tab,
                        color = if (isActive) onPrimary else onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        val filtered = QuickChannelList.all.filter { qc ->
            when (qcRegion) {
                "All" -> true; "US" -> "US" in qc.regions; "UK" -> "UK" in qc.regions
                "CA" -> "CA" in qc.regions; "Bay Area" -> "bay-area" in qc.regions
                "Premium" -> "premium" in qc.tags
                "Sports" -> "sports" in qc.tags; "News" -> "news" in qc.tags
                else -> true
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { it.displayName }) { qc ->
                Card(
                    onClick = { selectedQc = qc },
                    modifier = Modifier.width(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                qc.displayName,
                                color = onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            Text("▸ select", color = primary.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
    var loadState by remember { mutableStateOf<QcSheetState>(QcSheetState.Loading) }

    val sourceNameForId = remember(sourceNames, sourceIds) {
        sourceIds.zip(sourceNames).toMap()
    }

    LaunchedEffect(quickChannel) {
        loadState = QcSheetState.Loading
        loadState = try {
            val matches = withContext(Dispatchers.Default) {
                allChannels.filter { ch ->
                    ch.name.contains(quickChannel.displayName, ignoreCase = true) ||
                    quickChannel.aliases.any { ch.name.contains(it, ignoreCase = true) }
                }
            }
            if (matches.isEmpty()) {
                QcSheetState.Error("No sources found for \"${quickChannel.displayName}\"")
            } else {
                QcSheetState.Success(matches)
            }
        } catch (e: Exception) {
            QcSheetState.Error(e.message ?: "Failed to load sources")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        when (val state = loadState) {
            is QcSheetState.Loading -> {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Resolving ${quickChannel.displayName}...", color = onSurface, fontSize = 14.sp)
                    Text("Scanning IPTV playlists", color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(Modifier.height(48.dp))
                }
            }
            is QcSheetState.Error -> {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠", color = errorColor, fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(state.message, color = onSurface, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(primary).clickable {
                        loadState = QcSheetState.Loading
                    }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                        Text("Retry", color = onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
            is QcSheetState.Success -> {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().height(440.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(quickChannel.displayName, color = onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${state.channels.size} source${if (state.channels.size != 1) "s" else ""}", color = onsurfaceContainerHigh, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.channels) { ch ->
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(provider, color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        if (ch.group != null) {
                                            Text(ch.group, color = onsurfaceContainerHigh.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class QcSheetState {
    data object Loading : QcSheetState()
    data class Success(val channels: List<IptvChannel>) : QcSheetState()
    data class Error(val message: String) : QcSheetState()
}
