package com.nuvio.app.features.hub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.iptv.IptvScreen
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.sports.SportsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.runtime.snapshotFlow

// ─── Obsidian Media Hub Design Tokens ────────────────────────────────────
private val ObsidianBg = Color(0xFF131313)
private val SurfaceCard = Color(0xCC1F1F1F)
private val SurfaceCircle = Color(0x80353535)
private val OnSurface = Color(0xFFE2E2E2)
private val OnSurfaceVariant = Color(0xFFC4C7C8)
private val Outline = Color(0xFF8E9192)
private val OutlineVariant = Color(0xFF444748)
private val CardBorder = Color(0x1AFFFFFF)
private val Primary = Color(0xFFFDFDFC)

private enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, Music, Multi }

private data class HubItem(
    val title: String,
    val iconText: String,
    val target: HubSubScreen,
)

private val hubItems = listOf(
    HubItem("IPTVNutz Hub", "TV", HubSubScreen.Iptv),
    HubItem("SportNutz Hub", "SP", HubSubScreen.Sports),
    HubItem("VidNutz Hub", "VN", HubSubScreen.VidNutz),
    HubItem("MusicNutz Hub", "MU", HubSubScreen.Music),
    HubItem("MultiNutz Hub", "MW", HubSubScreen.Multi),
)

@Composable
fun RobbdeezeNutzHubScreen(
    modifier: Modifier = Modifier,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
    iptvScrollToTopRequests: Flow<Unit> = emptyFlow(),
    sportsScrollToTopRequests: Flow<Unit> = emptyFlow(),
    resetTrigger: Int = 0,
) {
    var subScreen by remember(resetTrigger) {
        mutableStateOf(
            when (HubReturnStore.subScreen) {
                "Iptv" -> HubSubScreen.Iptv; "Sports" -> HubSubScreen.Sports
                "VidNutz" -> HubSubScreen.VidNutz; "Music" -> HubSubScreen.Music
                "Multi" -> HubSubScreen.Multi
                else -> HubSubScreen.Hub
            },
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { HubReturnStore.subScreen }
            .drop(1)
            .collect { saved ->
                if (saved != "Hub") {
                    val restored = when (saved) {
                        "Iptv" -> HubSubScreen.Iptv; "Sports" -> HubSubScreen.Sports
                        "VidNutz" -> HubSubScreen.VidNutz; "Music" -> HubSubScreen.Music
                        "Multi" -> HubSubScreen.Multi
                        else -> null
                    }
                    if (restored != null && restored != subScreen) {
                        subScreen = restored
                    }
                }
            }
    }

    val onPlayChannelSave: ((PlayerLaunch) -> Unit)? = onPlayChannel?.let { original ->
        { launch ->
            HubReturnStore.subScreen = subScreen.name
            original(launch)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        val isTablet = maxWidth >= 768.dp

        Column(modifier = Modifier.fillMaxSize()) {
            when (subScreen) {
                HubSubScreen.Hub -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 12.dp, end = 16.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("RobbdeezeNutz", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    HubGrid(onNavigate = { subScreen = it })
                }
                HubSubScreen.Iptv -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { HubReturnStore.subScreen = "Hub"; subScreen = HubSubScreen.Hub }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            IptvScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = onPlayChannelSave, scrollToTopRequests = iptvScrollToTopRequests, onMultiWindowAdded = { subScreen = HubSubScreen.Multi }, isTabletLayout = isTablet)
                        }
                    }
                }
                HubSubScreen.Sports -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { HubReturnStore.subScreen = "Hub"; subScreen = HubSubScreen.Hub }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            SportsScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = onPlayChannelSave, scrollToTopRequests = sportsScrollToTopRequests, onTeamClick = onTeamClick)
                        }
                    }
                }
                HubSubScreen.VidNutz -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { HubReturnStore.subScreen = "Hub"; subScreen = HubSubScreen.Hub }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) { VidNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Music -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { HubReturnStore.subScreen = "Hub"; subScreen = HubSubScreen.Hub }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) { MusicNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Multi -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { HubReturnStore.subScreen = "Hub"; subScreen = HubSubScreen.Hub }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            MultiWindowContent(
                                onSubScreenChange = { subScreen = it },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Hub Grid ─────────────────────────────────────────────────────────────

@Composable
private fun HubGrid(onNavigate: (HubSubScreen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(5.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(hubItems, key = { it.target.name }) { item ->
                HubGridCard(
                    title = item.title,
                    iconText = item.iconText,
                    onClick = { onNavigate(item.target) },
                )
            }
        }

        Text(
            text = "Version ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.VERSION_CODE})",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            color = OnSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Hub Grid Card ────────────────────────────────────────────────────────

@Composable
private fun HubGridCard(
    title: String,
    iconText: String,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 0.97f else 1f,
        label = "scale",
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(focusScale)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .then(
                if (isFocused) Modifier.border(2.dp, Primary, RoundedCornerShape(8.dp))
                else Modifier.border(0.5.dp, CardBorder, RoundedCornerShape(8.dp))
            )
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Circular icon container
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) Primary else SurfaceCircle),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = iconText,
                    color = if (isFocused) ObsidianBg else OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            // Title
            Text(
                text = title,
                color = OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Multi Window Content ────────────────────────────────────────────────

@Composable
private fun MultiWindowContent(
    onSubScreenChange: (HubSubScreen) -> Unit,
) {
        var selectedCell by remember { mutableStateOf<WindowStream?>(null) }
        var showBookmarks by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
            MultiWindowGrid(
                streams = MultiWindowStore.allStreams,
                onRemoveStream = { MultiWindowStore.remove(it) },
                onAddMore = { onSubScreenChange(HubSubScreen.Iptv) },
                onCellLongPress = { selectedCell = it },
                onCellVolumeToggle = { stream, active ->
                    MultiWindowStore.setVolume(stream.id, if (active) 1f else 0f)
                    if (active) MultiWindowStore.setAudioFocus(stream.id)
                },
                onBookmarksClick = { showBookmarks = true },
                onMuteAll = {
                    val allMuted = MultiWindowStore.allStreams.all { MultiWindowStore.getVolume(it.id) == 0f }
                    val targetVol = if (allMuted) 1f else 0f
                    MultiWindowStore.allStreams.forEach { s ->
                        MultiWindowStore.setVolume(s.id, targetVol)
                        val hid = MultiWindowStore.getPlayerHandleId(s.id)
                        if (hid != null) {
                            com.nuvio.app.features.hub.MultiWindowPlayerManager.setVolume(
                                com.nuvio.app.features.hub.PlayerHandle(hid), targetVol,
                            )
                        }
                    }
                },
                onRefreshAll = {
                    val snapshots = MultiWindowStore.allStreams.toList().map { it.channel to it.slotIndex }
                    snapshots.forEach { (_, slot) ->
                        val existing = MultiWindowStore.allStreams.find { it.slotIndex == slot }
                        if (existing != null) MultiWindowStore.remove(existing.id)
                    }
                    snapshots.forEach { (channel, slot) ->
                        MultiWindowStore.addToSlot(channel, slot)
                    }
                },
                onCloseAll = {
                    MultiWindowStore.allStreams.toList().forEach { s -> MultiWindowStore.remove(s.id) }
                },
                onPauseAll = {
                    val allPaused = MultiWindowStore.allStreams.all { MultiWindowStore.isPaused(it.id) }
                    MultiWindowStore.allStreams.toList().forEach { s ->
                        val hid = MultiWindowStore.getPlayerHandleId(s.id)
                        if (hid != null) {
                            if (allPaused) {
                                com.nuvio.app.features.hub.MultiWindowPlayerManager.resumePlayer(
                                    com.nuvio.app.features.hub.PlayerHandle(hid),
                                )
                                MultiWindowStore.setPaused(s.id, false)
                            } else {
                                com.nuvio.app.features.hub.MultiWindowPlayerManager.pausePlayer(
                                    com.nuvio.app.features.hub.PlayerHandle(hid),
                                )
                                MultiWindowStore.setPaused(s.id, true)
                            }
                        }
                    }
                },
            )
        }
        if (showBookmarks) {
            MultiWindowBookmarksSheet(
                bookmarks = MultiWindowBookmarkStore.all(),
                onSave = { MultiWindowBookmarkStore.save(it) },
                onLoad = { MultiWindowBookmarkStore.load(it) },
                onDelete = { MultiWindowBookmarkStore.delete(it) },
                onDismiss = { showBookmarks = false },
            )
        }
        selectedCell?.let { stream ->
            MultiWindowCellOptions(
                stream = stream,
                volume = MultiWindowStore.getVolume(stream.id),
                onVolumeChange = { vol ->
                    MultiWindowStore.setVolume(stream.id, vol)
                    val hid = MultiWindowStore.getPlayerHandleId(stream.id)
                    if (hid != null) {
                        com.nuvio.app.features.hub.MultiWindowPlayerManager.setVolume(
                            com.nuvio.app.features.hub.PlayerHandle(hid), vol,
                        )
                    }
                },
                onRefresh = {
                    val slot = stream.slotIndex
                    val channel = stream.channel
                    MultiWindowStore.remove(stream.id)
                    MultiWindowStore.addToSlot(channel, slot)
                    selectedCell = null
                },
                onSwap = { targetSlot ->
                    MultiWindowStore.swapSlots(stream.slotIndex, targetSlot)
                    selectedCell = null
                },
                onClose = {
                    MultiWindowStore.remove(stream.id)
                    selectedCell = null
                },
                onDismiss = { selectedCell = null },
            )
        }
    }
