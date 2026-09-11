package com.nuvio.app.features.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.hub.VideoSelectionFeedback
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.iptv.EspnClient

private val SurfaceBg = Color(0xFF000000)
private val SurfaceContainer = Color(0xFF111111)
private val SurfaceContainerHigh = Color(0xFF1A1A1A)
private val SurfaceContainerHighest = Color(0xFF252525)
private val OnSurface = Color(0xFFDAE2FD)
private val OnSurfaceVariant = Color(0xFFB9CACB)
private val Primary = Color(0xFFDBFCFF)
private val Secondary = Color(0xFFC3F400)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamName: String,
    teamLogo: String?,
    sport: String,
    onBack: () -> Unit,
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by SportsRepository.uiState.collectAsStateWithLifecycle()
    val teamEvents = uiState.events.filter { event ->
        event.homeTeam == teamName || event.awayTeam == teamName
    }
    val recentResults = teamEvents
        .filter { it.status.contains("FINAL") }
        .sortedByDescending { it.date }
        .take(10)
    val upcoming = teamEvents
        .filter { !it.isLive && !it.status.contains("FINAL") }
        .sortedBy { it.date }
        .take(5)

    Scaffold(
        modifier = modifier.fillMaxSize().background(SurfaceBg),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(teamName, color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg.copy(alpha = 0.9f)),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Team Header ──
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!teamLogo.isNullOrBlank()) {
                            AsyncImage(model = teamLogo, contentDescription = teamName, contentScale = ContentScale.Fit, modifier = Modifier.size(84.dp))
                        } else {
                            Text(teamName.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(teamName, color = OnSurface, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    if (sport.isNotBlank()) {
                        Text(sport, color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            // ── Recent Results ──
            if (recentResults.isNotEmpty()) {
                Text("Recent Results", color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                recentResults.forEach { event ->
                    TeamGameRow(event = event, onPlayChannel = onPlayChannel)
                }
            }

            // ── Upcoming ──
            if (upcoming.isNotEmpty()) {
                Text("Upcoming", color = Primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                upcoming.forEach { event ->
                    TeamGameRow(event = event, onPlayChannel = onPlayChannel)
                }
            }

            if (recentResults.isEmpty() && upcoming.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No game data available", color = OnSurfaceVariant, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TeamGameRow(event: EspnProcessedEvent, onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    val scope = rememberCoroutineScope()
    val isLive = event.isLive
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWon = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWon = homeScore != null && awayScore != null && awayScore > homeScore

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLive) SurfaceContainer.copy(alpha = 0.85f) else SurfaceContainer)
            .clickable { playOrShowPicker(scope, event, onPlayChannel) }
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!event.homeLogo.isNullOrBlank()) AsyncImage(model = event.homeLogo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp))
                        else Text(event.homeTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
                    }
                    Text(event.homeTeam, color = if (homeWon) Secondary else OnSurface, fontSize = 14.sp, fontWeight = if (homeWon) FontWeight.ExtraBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(4.dp))
                    Text(event.homeScore ?: "-", color = if (homeWon) Secondary else OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        if (!event.awayLogo.isNullOrBlank()) AsyncImage(model = event.awayLogo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp))
                        else Text(event.awayTeam.take(2).uppercase(), color = OnSurfaceVariant, fontSize = 9.sp)
                    }
                    Text(event.awayTeam, color = if (awayWon) Secondary else OnSurface, fontSize = 14.sp, fontWeight = if (awayWon) FontWeight.ExtraBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(4.dp))
                    Text(event.awayScore ?: "-", color = if (awayWon) Secondary else OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            if (isLive) {
                Text("LIVE", color = Color(0xFFFFB4AB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

private fun playOrShowPicker(
    scope: CoroutineScope,
    event: EspnProcessedEvent,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    if (onPlayChannel == null) return
    val sportEvents = EspnClient.toSportEvents(listOf(event))
    if (sportEvents.isEmpty()) return
    val se = sportEvents.first()
    val allChannels = IptvRepository.getAllChannels()
    VideoSelectionFeedback.start(event.title, "Sports")
    scope.launch {
        try {
            val scored = EspnClient.findScoredMatchingChannelsWithLazyEpg(
                se, allChannels, IptvRepository.buildCurrentEpgTitleLookup()
            ).filterNot { com.nuvio.app.features.iptv.StreamValidationStore.isKnownDeadSync(it.channel.url) }
            if (scored.isEmpty()) return@launch
            val channel = ChannelScorer.autoplayCandidate(scored)?.channel ?: scored.first().channel
            val channelIndex = allChannels.indexOfFirst { it.id == channel.id && it.sourceId == channel.sourceId }
            val history = IptvRepository.getHistoryChannels()
            val launch = PlayerLaunch(
                profileId = 0, title = channel.name, sourceUrl = channel.url, streamTitle = channel.name,
                providerName = "Sports", parentMetaId = "iptv", parentMetaType = "tv", logo = channel.logo,
                channelNames = allChannels.map { it.name }, channelUrls = allChannels.map { it.url },
                channelLogos = allChannels.map { it.logo ?: "" }, channelIds = allChannels.map { it.id },
                currentChannelIndex = if (channelIndex >= 0) channelIndex else 0,
                historyChannelNames = history.map { it.name }, historyChannelUrls = history.map { it.url },
                historyChannelLogos = history.map { it.logo ?: "" }, historyChannelIds = history.map { it.id },
            )
            val id = PlayerLaunchStore.put(launch)
            PlayerLaunchStore.get(id)?.let { onPlayChannel(it) }
        } finally {
            VideoSelectionFeedback.stop()
        }
    }
}
