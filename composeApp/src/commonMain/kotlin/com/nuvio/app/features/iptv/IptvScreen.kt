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
private val GlassBg = Color(0xFF1E293B).copy(alpha = 0.7f)
private val NeonPurple = Color(0xFF7C3AED)
private val NeonPurpleLight = Color(0xFFD2BBFF)
private val ElectricBlue = Color(0xFF00A2E6)
private val ElectricBlueLight = Color(0xFF89CEFF)
private val SurfaceLow = Color(0xFF131B2E)
private val SurfaceVariant = Color(0xFF2D3449)
private val OnSurface = Color(0xFFDAE2FD)
private val OnSurfaceVariant = Color(0xFFCCC3D8)
private val OutlineVariant = Color(0xFF4A4455)
private val InputBg = Color(0xFF0F172A)
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
                title = {
                    Text("IPTV", color = NeonPurpleLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        CircularProgressIndicator(color = NeonPurple)
                    }
                }
                return@LazyColumn
            }

            item { SearchSection(searchQuery = uiState.searchQuery, onSearchQueryChange = { IptvRepository.setSearchQuery(it) }) }

            item { QuickAccessSection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { HistorySection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { UfcSection(uiState = uiState, onPlayChannel = onPlayChannel) }

            item { PlaylistsSection(uiState = uiState, onAddClick = { showAddSourceSheet = true }) }

            item { SourceChipsSection(uiState = uiState) }

            item { CategoryChipsSection(uiState = uiState) }

            if (uiState.channels.any { it.epgChannelId != null } && uiState.epgSources.isEmpty() && IptvRepository.hasPredefinedPlaylist()) {
                item {
                    Surface(
                        onClick = { showAddSourceSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricBlue.copy(alpha = 0.1f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📺", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add an EPG XMLTV source to see program guide data on channels",
                                color = OnSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

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
                                color = ElectricBlueLight,
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
                                            epgPrograms = channel.epgChannelId?.let { uiState.epgPrograms[it] } ?: emptyList(),
                                            epgProgramsByName = uiState.epgProgramsByName,
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
                                        epgPrograms = channel.epgChannelId?.let { uiState.epgPrograms[it] } ?: emptyList(),
                                        epgProgramsByName = uiState.epgProgramsByName,
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
            focusedBorderColor = NeonPurple,
            unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
            cursorColor = NeonPurple,
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
private fun NowOnTvSection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    Column {
        if (uiState.tvShows.isEmpty()) {
            if (!uiState.tvLoading) {
                Text("No TV matches found", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            return
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Now on TV", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.tvShows, key = { "tv_" + it.episode.id }) { matched ->
                TvShowCard(matched = matched, onPlay = { playChannel(matched.channel, onPlayChannel) })
            }
        }
    }
}

@Composable
private fun TvShowCard(matched: MatchedTvShow, onPlay: () -> Unit) {
    val ep = matched.episode
    val show = ep.show
    val networkName = show?.network?.name ?: show?.webChannel?.name ?: ""
    val parts = ep.airtime.split(":").map { it.toIntOrNull() ?: 0 }
    val epStart = parts.getOrElse(0) { 0 } * 60 + parts.getOrElse(1) { 0 }
    val nowMin = (TraktPlatformClock.nowEpochMs() / 60000).toInt() % 1440
    val epEnd = epStart + (ep.runtime ?: 30).coerceIn(15, 240)
    val isNow = nowMin in epStart until epEnd
    Card(
        modifier = Modifier.width(180.dp).clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (show?.image?.medium != null) {
                coil3.compose.AsyncImage(
                    model = show.image.medium,
                    contentDescription = show.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(
                if (show?.image?.medium != null)
                    Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.8f)))
                else
                    Brush.verticalGradient(listOf(SurfaceVariant.copy(alpha = 0.3f), SurfaceLow))
            ))
            Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = if (isNow) Color(0xFFFF0000) else NeonPurple) {
                        Text(ep.airtime.take(5), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (isNow) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF0000)) {
                            Text("NOW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(8.dp)) {
                Text(show?.name ?: "", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(networkName, color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (ep.name.isNotBlank()) {
                    Text("S${ep.season}E${ep.number ?: "?"} - ${ep.name.take(20)}", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun UfcSection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    val espnUfc = uiState.ufcEvents
    if (espnUfc.isEmpty()) return

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("🥊 UFC", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(espnUfc, key = { "ufc_" + it.id }) { event ->
                UfcCard(event = event, onPlay = {
                    val chName = event.channel
                    val found = IptvRepository.getLastFilteredChannels().firstOrNull { ch ->
                        ch.name.lowercase().contains(chName.lowercase())
                    }
                    if (found != null) playChannel(found, onPlayChannel)
                })
            }
        }
    }
}

@Composable
private fun UfcCard(event: EspnProcessedEvent, onPlay: () -> Unit) {
    Card(
        modifier = Modifier.width(220.dp).clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A0033), SurfaceLow))))
            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
                val isLive = event.isLive
                if (isLive) {
                    Text("LIVE", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                }
                Text(event.title.take(50), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.homeScore != null) {
                        Text("${event.homeTeam} vs ${event.awayTeam}", color = OnSurfaceVariant, fontSize = 11.sp)
                    } else {
                        Text(event.detail.take(20), color = ElectricBlueLight, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.channel.ifBlank { event.sport }, color = OnSurfaceVariant, fontSize = 10.sp)
                    if (event.isPpv) {
                        Spacer(Modifier.width(4.dp))
                        Text("PPV", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 8.sp,
                            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFFFF9800).copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSportsSection(
    uiState: IptvUiState,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    Column {
        if (uiState.sportLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonPurple, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Loading sports...", color = OnSurfaceVariant, fontSize = 12.sp)
            }
            return
        }
        if (uiState.sportEvents.isEmpty()) {
            if (!uiState.sportLoading) {
                Text("No sports match your channels", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF00)))
                Spacer(Modifier.width(8.dp))
                Text("Live Sports", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.sportEvents, key = { "sport_" + it.event.idEvent + it.channel.id }) { matched ->
                SportEventCard(matched = matched, onPlay = { playChannel(matched.channel, onPlayChannel) })
            }
        }
    }
}

@Composable
private fun SportEventCard(matched: MatchedSportEvent, onPlay: () -> Unit) {
    val event = matched.event
    val isPpv = event.strFilename == "PPV"
    val isLive = event.strStatus == "LIVE"
    Card(
        modifier = Modifier.width(220.dp).clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (!event.strThumb.isNullOrBlank()) {
                coil3.compose.AsyncImage(
                    model = event.strThumb,
                    contentDescription = event.strEvent,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(
                if (event.strThumb.isNullOrBlank())
                    Brush.verticalGradient(listOf(SurfaceVariant.copy(alpha = 0.3f), SurfaceLow))
                else
                    Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.7f)))
            ))
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isLive) {
                        Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF0000)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (isPpv) {
                        Text("PPV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF9800)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(8.dp),
            ) {
                Text(event.strSport, color = ElectricBlueLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(2.dp))
                Text(event.strEvent.take(40), color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val score = if (!event.intHomeScore.isNullOrBlank() && !event.intAwayScore.isNullOrBlank())
                        "${event.intHomeScore} - ${event.intAwayScore}"
                    else event.strTime.take(8)
                    Text(score ?: "", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(event.strChannel, color = OnSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
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
                    modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(NeonPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LiveTv, contentDescription = null, tint = NeonPurpleLight, modifier = Modifier.size(18.dp))
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                    .padding(8.dp),
            ) {
                Column {
                    Text(text = channel.name, color = OnSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = channel.group ?: "Live", color = ElectricBlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(6.dp).clip(CircleShape).background(ElectricBlue),
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
        color = if (selected) NeonPurple.copy(alpha = 0.2f) else SurfaceLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) NeonPurpleLight else OnSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$count",
                    color = if (selected) ElectricBlueLight else OnSurfaceVariant.copy(alpha = 0.5f),
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
        color = if (selected) ElectricBlue.copy(alpha = 0.2f) else SurfaceLow,
    ) {
        Text(
            text = label,
            color = if (selected) ElectricBlueLight else OnSurfaceVariant,
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
    epgPrograms: List<EpgProgram>,
    epgProgramsByName: Map<String, List<EpgProgram>> = emptyMap(),
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val resolvedPrograms = if (epgPrograms.isNotEmpty()) epgPrograms
        else epgProgramsByName[channel.name.lowercase().trim()] ?: emptyList()
    val currentProgram = resolvedPrograms.find { it.startTime <= now && it.endTime > now }
    val nextProgram = resolvedPrograms.find { it.startTime > now }
    val progress = if (currentProgram != null && currentProgram.endTime > currentProgram.startTime) {
        ((now - currentProgram.startTime).toFloat() / (currentProgram.endTime - currentProgram.startTime).toFloat()).coerceIn(0f, 1f)
    } else 0f

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
                        modifier = Modifier.size(36.dp).align(Alignment.Center).clip(CircleShape).background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.LiveTv, contentDescription = null, tint = NeonPurpleLight, modifier = Modifier.size(18.dp))
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))))
                        .padding(8.dp),
                ) {
                    Column {
                        Text(text = channel.name, color = OnSurface, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = channel.group ?: "Live", color = ElectricBlueLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue),
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

            if (currentProgram != null || nextProgram != null) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    if (currentProgram != null) {
                        Text(
                            text = currentProgram.title,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 3.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                                    .background(Brush.horizontalGradient(listOf(ElectricBlue, ElectricBlue.copy(alpha = 0.6f)))),
                            )
                        }
                    }
                    if (nextProgram != null && currentProgram == null) {
                        Text("Up Next: ${nextProgram.title}", color = NeonPurpleLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else if (channel.epgChannelId != null) {
                Text("No EPG now", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
                        Text("+ iptv-org", color = NeonPurpleLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Surface(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(12.dp),
                    color = NeonPurple,
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
                if (uiState.m3uPlaylists.isEmpty() && uiState.xtreamAccounts.isEmpty() && uiState.stalkerAccounts.isEmpty() && uiState.epgSources.isEmpty()) {
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
                            statusColor = if (playlist.channels.isNotEmpty()) ElectricBlue else OutlineVariant,
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
                            statusColor = if (account.channels.isNotEmpty()) ElectricBlue else OutlineVariant,
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
                            statusColor = if (account.channels.isNotEmpty()) ElectricBlue else OutlineVariant,
                            iconLabel = "SK",
                            isRefreshing = account.id in uiState.refreshingSourceIds,
                            onRefresh = { IptvRepository.refreshStalkerChannels(account.id) },
                            onDelete = { IptvRepository.removeStalkerAccount(account.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.epgSources.forEach { source ->
                        val matchInfo = if (!uiState.epgLoading && uiState.epgMatchCount > 0) {
                            "${uiState.epgMatchCount} channels matched"
                        } else if (!uiState.epgLoading && uiState.epgPrograms.isNotEmpty() && uiState.epgMatchCount == 0) {
                            "No channel matches"
                        } else ""
                        PlaylistCard(
                            name = source.name,
                            subtitle = if (matchInfo.isNotEmpty()) matchInfo else source.url,
                            status = if (!uiState.epgLoading && uiState.epgPrograms.isNotEmpty()) "Loaded" else if (uiState.epgLoading) "Loading..." else "EPG",
                            statusColor = if (uiState.epgLoading) OutlineVariant else if (uiState.epgPrograms.isNotEmpty()) NeonPurpleLight else OutlineVariant,
                            iconLabel = "EP",
                            isRefreshing = uiState.epgLoading,
                            onRefresh = { IptvRepository.refreshEpg() },
                            onDelete = { IptvRepository.removeEpgSource(source.id) },
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
                Text(iconLabel, color = NeonPurpleLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = OnSurfaceVariant.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NeonPurple,
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
                    color = if (mode == "xtreme") NeonPurple else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("XTREME", color = if (mode == "xtreme") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
                Surface(
                    onClick = { mode = "m3u" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "m3u") NeonPurple else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("M3U URL", color = if (mode == "m3u") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
                Surface(
                    onClick = { mode = "epg" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "epg") NeonPurple else Color.Transparent,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("EPG", color = if (mode == "epg") Color.White else OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                }
                Surface(
                    onClick = { mode = "stalker" },
                    shape = RoundedCornerShape(12.dp),
                    color = if (mode == "stalker") NeonPurple else Color.Transparent,
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
                "epg" -> EpgForm(onSuccess = onSuccess)
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
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
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
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            enabled = url.isNotBlank(),
        ) {
            Text("CONNECT SOURCE", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EpgForm(onSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column {
        InputField(label = "EPG SOURCE NAME", value = name, onValueChange = { name = it }, placeholder = "My EPG Guide")
        Spacer(Modifier.height(16.dp))
        InputField(label = "XMLTV URL", value = url, onValueChange = { url = it }, placeholder = "https://domain.com/epg.xmltv")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (url.isNotBlank() && name.isNotBlank()) {
                    IptvRepository.addEpgSource(name, url)
                    IptvRepository.refreshEpg()
                    onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            enabled = url.isNotBlank() && name.isNotBlank(),
        ) {
            Text("ADD EPG SOURCE", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
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
        Text(label, color = NeonPurpleLight, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
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
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = OutlineVariant.copy(alpha = 0.3f),
                cursorColor = NeonPurple,
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
