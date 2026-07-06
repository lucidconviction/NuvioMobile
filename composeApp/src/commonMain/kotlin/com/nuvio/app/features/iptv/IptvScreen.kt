package com.nuvio.app.features.iptv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LiveTv
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val ObsidianBg = Color(0xFF000000)
private val GlassBg = Color(0xFF1A1A1A).copy(alpha = 0.7f)
private val SurfaceLow = Color(0xFF111111)
private val SurfaceVariant = Color(0xFF252525)
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val OutlineVariant = Color(0xFF3A3A3A)
private val InputBg = Color(0xFF0F0F0F)
private val AccentGray = Color(0xFFCCCCCC)
private val FavoriteRed = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvScreen(
    modifier: Modifier = Modifier,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
) {
    val uiState by IptvRepository.uiState.collectAsStateWithLifecycle()
    var showAddSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        IptvRepository.ensureLoaded()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequests) {
        scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(ObsidianBg),
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
                        CircularProgressIndicator(color = AccentGray)
                    }
                }
                return@LazyColumn
            }

            item { SearchSection(searchQuery = uiState.searchQuery, onSearchQueryChange = { IptvRepository.setSearchQuery(it) }) }

            item { QuickAccessSection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { HistorySection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { PlaylistsSection(uiState = uiState, onAddClick = { showAddSourceSheet = true }) }

            item { SourceChipsSection(uiState = uiState) }

            item { CategoryChipsSection(uiState = uiState) }

            val channels = uiState.channels
            if (channels.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No channels found", color = OnSurfaceVariant)
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { IptvRepository.toggleChannelsExpanded() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${channels.size} Channel${if (channels.size == 1) "" else "s"}",
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Icon(
                            if (uiState.channelsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (uiState.channelsExpanded) "Collapse" else "Expand",
                            tint = OnSurfaceVariant,
                        )
                    }
                }

                if (uiState.channelsExpanded) {
                    val now = TraktPlatformClock.nowEpochMs()
                    val grouped = channels.groupBy { it.group ?: "Other" }.toSortedMap()
                    val gridItems = mutableListOf<Pair<String?, List<IptvChannel>>>()
                    grouped.forEach { (group, chs) ->
                        gridItems.add(group to chs)
                    }
                    gridItems.forEach { (group, chs) ->
                        item(key = "grp_$group") {
                            Text(
                                text = group ?: "Other",
                                color = OnSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        val chunked = chs.chunked(2)
                        items(chunked, key = { row -> "${group}_" + row.joinToString("-") { it.id } }) { rowChannels ->
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

            val allFavorites = IptvRepository.getFavoriteChannels()
            if (allFavorites.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { IptvRepository.toggleFavoritesExpanded() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("All Favorites", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Icon(
                            if (uiState.favoritesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (uiState.favoritesExpanded) "Collapse" else "Expand",
                            tint = OnSurfaceVariant,
                        )
                    }
                }
                if (uiState.favoritesExpanded) {
                    val nowFav = TraktPlatformClock.nowEpochMs()
                    val favChunked = allFavorites.chunked(2)
                    items(favChunked, key = { row -> "allfav_" + row.joinToString("-") { it.id + it.sourceId } }) { rowChannels ->
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
                    Surface(shape = RoundedCornerShape(8.dp), color = GlassBg) {
                        Text(uiState.debugText, color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 9.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showAddSourceSheet) {
        AddSourceBottomSheet(
            onDismiss = { showAddSourceSheet = false },
            onSuccess = { showAddSourceSheet = false }
        )
    }
    }
}

@Composable
private fun SearchSection(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search channels...", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 14.sp) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = OnSurfaceVariant) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(color = OnSurface, fontSize = 14.sp),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGray,
            unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
            cursorColor = AccentGray,
            focusedContainerColor = InputBg,
            unfocusedContainerColor = InputBg,
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
            Text("Favorites", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBg),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap the heart icon on any channel to add it here",
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
            return
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(favorites.take(10), key = { "fav_" + it.id + it.sourceId }) { channel ->
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
            Text("History", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(history.take(15), key = { "hist_" + it.id + it.sourceId }) { channel ->
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
        colors = CardDefaults.cardColors(containerColor = SurfaceLow),
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
                        Brush.verticalGradient(listOf(SurfaceVariant.copy(alpha = 0.3f), SurfaceLow))
                    else
                        Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f)))
                ),
            )
            if (channel.logo.isNullOrBlank()) {
                Box(
                    modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(AccentGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LiveTv, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                    .padding(8.dp),
            ) {
                Column {
                    Text(text = channel.name, color = OnSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = channel.group ?: "Live", color = OnSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(6.dp).clip(CircleShape).background(OnSurface),
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
        color = if (selected) AccentGray.copy(alpha = 0.2f) else SurfaceLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) OnSurfaceVariant else OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$count",
                    color = if (selected) OnSurface else OnSurfaceVariant.copy(alpha = 0.5f),
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
        color = if (selected) OnSurface.copy(alpha = 0.2f) else SurfaceLow,
    ) {
        Text(
            text = label,
            color = if (selected) OnSurface else OnSurfaceVariant,
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
) {

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow),
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
                                Brush.verticalGradient(listOf(SurfaceVariant.copy(alpha = 0.3f), SurfaceLow))
                            else
                                Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f)))
                        ),
                )
                if (channel.logo.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(AccentGray.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                        .padding(8.dp),
                ) {
                    Column {
                        Text(text = channel.name, color = OnSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = channel.group ?: "Live", color = OnSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OnSurface),
                )

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                        tint = if (isFavorite) FavoriteRed else OnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
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
            Text("Your Playlists", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!IptvRepository.hasPredefinedPlaylist()) {
                    TextButton(onClick = { IptvRepository.addPredefinedPlaylist() }) {
                        Text("+ iptv-org", color = OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    color = AccentGray,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ADD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = { IptvRepository.togglePlaylistsExpanded() }) {
                    Icon(
                        if (uiState.playlistsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (uiState.playlistsExpanded) "Collapse" else "Expand",
                        tint = OnSurfaceVariant,
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
                        colors = CardDefaults.cardColors(containerColor = GlassBg),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No playlists added yet", color = OnSurfaceVariant)
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    uiState.m3uPlaylists.forEach { playlist ->
                        PlaylistCard(
                            name = playlist.name,
                            subtitle = "${playlist.channels.size} Channels",
                            status = if (playlist.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (playlist.channels.isNotEmpty()) OnSurface else OutlineVariant,
                            iconLabel = "M3U",
                            isRefreshing = playlist.id in uiState.refreshingSourceIds,
                            onRefresh = { IptvRepository.refreshM3uChannels(playlist.id) },
                            onDelete = { IptvRepository.removeM3uPlaylist(playlist.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.xtreamAccounts.forEach { account ->
                        PlaylistCard(
                            name = account.name,
                            subtitle = "${account.channels.size} Channels",
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) OnSurface else OutlineVariant,
                            iconLabel = "XT",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onRefresh = { IptvRepository.refreshXtreamChannels(account.id) },
                            onDelete = { IptvRepository.removeXtreamAccount(account.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.stalkerAccounts.forEach { account ->
                        PlaylistCard(
                            name = account.name,
                            subtitle = "${account.channels.size} Channels",
                            status = if (account.channels.isNotEmpty()) "Connected" else "Pending",
                            statusColor = if (account.channels.isNotEmpty()) OnSurface else OutlineVariant,
                            iconLabel = "SK",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
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
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconLabel, color = OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = OnSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AccentGray,
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = OnSurfaceVariant)
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
    val launch = PlayerLaunch(
        profileId = 0,
        title = channel.name,
        sourceUrl = channel.url,
        streamTitle = channel.name,
        providerName = "IPTV",
        parentMetaId = "iptv",
        parentMetaType = "tv",
        logo = channel.logo,
        channelNames = channels.map { it.name },
        channelUrls = channels.map { it.url },
        channelLogos = channels.map { it.logo ?: "" },
        channelIds = channels.map { it.id },
        currentChannelIndex = if (channelIndex >= 0) channelIndex else 0,
        historyChannelNames = history.map { it.name },
        historyChannelUrls = history.map { it.url },
        historyChannelLogos = history.map { it.logo ?: "" },
        historyChannelIds = history.map { it.id },
    )
    val id = PlayerLaunchStore.put(launch)
    PlayerLaunchStore.get(id)?.let { onPlayChannel?.invoke(it) }
}

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
        containerColor = GlassBg,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { Box(Modifier.width(48.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(OutlineVariant.copy(alpha = 0.4f))) },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Add Source", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                IconButton(onClick = onDismiss) {
                    Text("✕", color = OnSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLow)
                    .padding(4.dp),
            ) {
                Surface(
                    onClick = { mode = "xtreme" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "xtreme") AccentGray else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("XTREME", color = if (mode == "xtreme") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
                Surface(
                    onClick = { mode = "m3u" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "m3u") AccentGray else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("M3U URL", color = if (mode == "m3u") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
                Surface(
                    onClick = { mode = "stalker" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "stalker") AccentGray else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("STALKER", color = if (mode == "stalker") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            when (mode) {
                "xtreme" -> XtreamForm(onSuccess = onSuccess)
                "m3u" -> M3uForm(onSuccess = onSuccess)
                "stalker" -> StalkerForm(onSuccess = onSuccess)
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
            InputField(label = "PASSWORD", value = password, onValueChange = { password = it }, placeholder = "••••••••", isPassword = true, modifier = Modifier.weight(1f))
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
            colors = ButtonDefaults.buttonColors(containerColor = AccentGray),
            enabled = name.isNotBlank() && server.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            colors = ButtonDefaults.buttonColors(containerColor = AccentGray),
            enabled = url.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            colors = ButtonDefaults.buttonColors(containerColor = AccentGray),
            enabled = name.isNotBlank() && server.isNotBlank() && macAddress.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
        Text(label, color = OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = OnSurface, fontSize = 14.sp),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGray,
                unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
                cursorColor = AccentGray,
                focusedContainerColor = InputBg,
                unfocusedContainerColor = InputBg,
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
