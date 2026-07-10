package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.iptv.IptvScreen
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.sports.SportsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val ObsidianBg = Color(0xFF000000)
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)

private enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, Music }

@Composable
fun RobbdeezeNutzHubScreen(
    modifier: Modifier = Modifier,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    onTeamClick: ((teamName: String, teamLogo: String?, sport: String) -> Unit)? = null,
    iptvScrollToTopRequests: Flow<Unit> = emptyFlow(),
    sportsScrollToTopRequests: Flow<Unit> = emptyFlow(),
    resetTrigger: Int = 0,
) {
    var subScreen by remember { mutableStateOf(HubSubScreen.Hub) }

    LaunchedEffect(Unit) {
        val saved = HubReturnStore.subScreen
        subScreen = when (saved) {
            "Iptv" -> HubSubScreen.Iptv
            "Sports" -> HubSubScreen.Sports
            "VidNutz" -> HubSubScreen.VidNutz
            "Music" -> HubSubScreen.Music
            else -> HubSubScreen.Hub
        }
        if (subScreen != HubSubScreen.Hub) {
            HubReturnStore.subScreen = "Hub"
        }
    }

    LaunchedEffect(resetTrigger) {
        subScreen = HubSubScreen.Hub
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
        val topPad = if (isTablet) 96.dp else 32.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = topPad, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (subScreen != HubSubScreen.Hub) {
                    IconButton(onClick = { subScreen = HubSubScreen.Hub }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface)
                    }
                }
                Text("RobbdeezeNutz Hubz", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }

            when (subScreen) {
                HubSubScreen.Hub -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HubCard("IPTVNutz Hub", "Live TV channels and on-demand content from around the world.", "TV") { subScreen = HubSubScreen.Iptv }
                        HubCard("SportNutz Hub", "Real-time scores, highlights, and live match coverage.", "SP") { subScreen = HubSubScreen.Sports }
                        HubCard("VidNutz Hub", "Browse, search, and watch videos from across the web.", "VN") { subScreen = HubSubScreen.VidNutz }
                        HubCard("MusicNutz Hub", "Search and stream millions of tracks from across the web.", "MU") { subScreen = HubSubScreen.Music }
                        Spacer(Modifier.height(32.dp))
                    }
                }
                HubSubScreen.Iptv -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("IPTVNutz Hub", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                        Box(Modifier.fillMaxSize()) { IptvScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = onPlayChannelSave, scrollToTopRequests = iptvScrollToTopRequests) }
                    }
                }
                HubSubScreen.Sports -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("SportNutz Hub", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                        Box(Modifier.fillMaxSize()) { SportsScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = onPlayChannelSave, scrollToTopRequests = sportsScrollToTopRequests, onTeamClick = onTeamClick) }
                    }
                }
                HubSubScreen.VidNutz -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("VidNutz Hub", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                        Box(Modifier.fillMaxSize()) { VidNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
                HubSubScreen.Music -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("MusicNutz Hub", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                        Box(Modifier.fillMaxSize()) { MusicNutzScreen(onPlayChannel = onPlayChannelSave) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubCard(title: String, description: String, iconText: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(if (isFocused) Modifier.border(2.dp, Color.White, cardShape) else Modifier)
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) SurfaceCard.copy(alpha = 1.2f) else SurfaceCard)
            .padding(20.dp),
    ) {
        Column {
            Text(title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(description, color = OnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = if (isFocused) 0.15f else 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
