package com.nuvio.app.features.hub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.iptv.IptvScreen
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.features.sports.SportsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.runtime.snapshotFlow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.hub_iptv
import nuvio.composeapp.generated.resources.hub_multi
import nuvio.composeapp.generated.resources.hub_music
import nuvio.composeapp.generated.resources.hub_pod
import nuvio.composeapp.generated.resources.hub_sport
import nuvio.composeapp.generated.resources.hub_vid
import nuvio.composeapp.generated.resources.rdnutz_banner
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
private val GlassBg = Color(0x991E1E1E)

private enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, Music, Pod, Multi }

private data class HubItem(
    val title: String,
    val iconText: String,
    val target: HubSubScreen,
    val bgRes: DrawableResource? = null,
)

private val hubItems = listOf(
    HubItem("IPTVNutz Hub", "TV", HubSubScreen.Iptv, Res.drawable.hub_iptv),
    HubItem("SportNutz Hub", "SP", HubSubScreen.Sports, Res.drawable.hub_sport),
    HubItem("VidNutz Hub", "VN", HubSubScreen.VidNutz, Res.drawable.hub_vid),
    HubItem("MusicNutz Hub", "MU", HubSubScreen.Music, Res.drawable.hub_music),
    HubItem("PodNutz Hub", "PO", HubSubScreen.Pod, Res.drawable.hub_pod),
    HubItem("MultiNutz Hub", "MW", HubSubScreen.Multi, Res.drawable.hub_multi),
)

private const val TELEGRAM_HANDLE = "@RnutzNuvioUpdates"
private const val TELEGRAM_URL = "https://t.me/RnutzNuvioUpdates"
private const val RD_TV_USDT = "0xf42b556E240b5a3820365414cE91BCaCd5bBA287"
private const val RD_TV_PAYPAL = "https://paypal.me/robbdeeze"

private val hubDescriptions = mapOf(
    HubSubScreen.Iptv to "Live TV & sources",
    HubSubScreen.Sports to "Scores, standings & fights",
    HubSubScreen.VidNutz to "Video search & playlists",
    HubSubScreen.Music to "Music streaming",
    HubSubScreen.Pod to "Podcasts with resume",
    HubSubScreen.Multi to "Multi-window streaming",
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
                "Pod" -> HubSubScreen.Pod
                "Multi" -> HubSubScreen.Multi
                else -> HubSubScreen.Hub
            },
        )
    }
    var backStack by remember(resetTrigger) { mutableStateOf<List<HubSubScreen>>(emptyList()) }

    fun pushNav(next: HubSubScreen) {
        if (next == subScreen) return
        if (subScreen != HubSubScreen.Hub) {
            backStack = backStack + subScreen
        }
        subScreen = next
        HubReturnStore.subScreen = next.name
    }

    fun navigateBack() {
        val prev = backStack.lastOrNull()
        backStack = if (backStack.isEmpty()) backStack else backStack.dropLast(1)
        subScreen = prev ?: HubSubScreen.Hub
        HubReturnStore.subScreen = subScreen.name
    }

    LaunchedEffect(Unit) {
        snapshotFlow { HubReturnStore.subScreen }
            .drop(1)
            .collect { saved ->
                if (saved != "Hub") {
                    val restored = when (saved) {
                        "Iptv" -> HubSubScreen.Iptv; "Sports" -> HubSubScreen.Sports
                        "VidNutz" -> HubSubScreen.VidNutz; "Music" -> HubSubScreen.Music
                        "Pod" -> HubSubScreen.Pod
                        "Multi" -> HubSubScreen.Multi
                        else -> null
                    }
                    if (restored != null && restored != subScreen) {
                        pushNav(restored)
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

    PlatformBackHandler(enabled = subScreen != HubSubScreen.Hub) {
        navigateBack()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        val isTablet = maxWidth >= 768.dp

        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (MultiWindowStore.allStreams.isNotEmpty() && subScreen != HubSubScreen.Multi) {
                            pushNav(HubSubScreen.Multi)
                        }
                    },
                )
            },
        ) {
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
                    HubPageHeader()
                    Spacer(Modifier.height(8.dp))
                    HubGrid(modifier = Modifier.weight(1f), onNavigate = { pushNav(it) })
                }
                HubSubScreen.Iptv -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            IptvScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = onPlayChannelSave, scrollToTopRequests = iptvScrollToTopRequests, onMultiWindowAdded = { pushNav(HubSubScreen.Multi) }, isTabletLayout = isTablet)
                        }
                    }
                }
                HubSubScreen.Sports -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateBack() }) {
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
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) { VidNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Music -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) { MusicNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Pod -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) { PodNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Multi -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            MultiWindowContent(
                                onSubScreenChange = { pushNav(it) },
                                onPlayChannel = onPlayChannelSave,
                            )
                        }
                    }
                }
            }
            }

        }
    }
}

@Composable
private fun HubPageHeader() {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(10.dp))
                .border(0.5.dp, CardBorder, RoundedCornerShape(10.dp)),
        ) {
            Image(
                painter = painterResource(Res.drawable.rdnutz_banner),
                contentDescription = "RD Nutz TV",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text("RD Nutz TV Playlist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Unlock everything with an $8/month per device donation:", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        "⚡ 5,000+ Premium Channels — every major network in crystal HD",
                        "🔥 XXX & Adult Networks",
                        "🏆 All Live Sports — every league, every game",
                        "🥊 PPV Events & Big Fights",
                        "🎬 Unlimited Movies & Series",
                        "🧒 Dedicated Kids Networks",
                    ).forEach { perk ->
                        Text(perk, color = Color.White.copy(alpha = 0.92f), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Questions? Ask us at Telegram: ", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
                    Text(
                        TELEGRAM_HANDLE,
                        color = Color(0xFF4A90D9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { uriHandler.openUri(TELEGRAM_URL) },
                    )
                }
            }
        }
    }
}

// ─── Hub Grid ─────────────────────────────────────────────────────────────

@Composable
private fun HubGrid(onNavigate: (HubSubScreen) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
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
                    description = hubDescriptions[item.target] ?: "",
                    backgroundImage = item.bgRes,
                    onClick = { onNavigate(item.target) },
                )
            }
        }

    }
}

// ─── Hub Grid Card ────────────────────────────────────────────────────────

@Composable
private fun HubGridCard(
    title: String,
    iconText: String,
    description: String,
    backgroundImage: DrawableResource?,
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
        if (backgroundImage != null) {
            Image(
                painter = painterResource(backgroundImage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
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
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    color = OnSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Multi Window Content ────────────────────────────────────────────────

@Composable
private fun MultiWindowContent(
    onSubScreenChange: (HubSubScreen) -> Unit,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
) {
        var selectedCell by remember { mutableStateOf<WindowStream?>(null) }
        var showBookmarks by remember { mutableStateOf(false) }
        var showQuickChannels by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
            MultiWindowGrid(
                streams = MultiWindowStore.allStreams,
                onRemoveStream = { MultiWindowStore.remove(it) },
                onAddMore = { onSubScreenChange(HubSubScreen.Iptv) },
                onQuickChannelsClick = { showQuickChannels = true },
                onCellLongPress = { selectedCell = it },
                onFullscreenCell = onPlayChannel?.let { cb -> { stream ->
                    val url = stream.playerUrl ?: stream.channel.url
                    val title = stream.playerTitle ?: stream.channel.name
                    val poster = stream.playerPoster ?: stream.channel.logo
                    cb(PlayerLaunch(
                        profileId = 0,
                        title = title,
                        sourceUrl = url,
                        streamTitle = title,
                        providerName = "MultiNutz",
                        parentMetaId = "multiview",
                        parentMetaType = "tv",
                        logo = poster,
                        poster = poster,
                    ))
                } },
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
                onFullscreen = onPlayChannel?.let { cb -> {
                    val url = stream.playerUrl ?: stream.channel.url
                    val title = stream.playerTitle ?: stream.channel.name
                    val poster = stream.playerPoster ?: stream.channel.logo
                    cb(PlayerLaunch(
                        profileId = 0,
                        title = title,
                        sourceUrl = url,
                        streamTitle = title,
                        providerName = "MultiNutz",
                        parentMetaId = "multiview",
                        parentMetaType = "tv",
                        logo = poster,
                        poster = poster,
                    ))
                    selectedCell = null
                } },
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
        if (showQuickChannels) {
            QuickChannelsSheet(
                onAddToSlot = { channel, slot ->
                    MultiWindowStore.addToSlot(channel, slot)
                },
                onDismiss = { showQuickChannels = false },
            )
        }
    }
