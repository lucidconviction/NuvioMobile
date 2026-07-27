package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.QuickChannel
import com.nuvio.app.features.iptv.QuickChannelList
import com.nuvio.app.features.player.PlayerLaunch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SurfaceBg = Color(0xFF000000)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceLow = Color(0xFF111111)
private val Accent = Color(0xFF4A90D9)

private val scaleLabels = listOf("Fill", "Fit", "16:9", "4:3", "Zoom")
private val scaleModes = listOf(RESIZE_FILL, RESIZE_FIT, RESIZE_FIXED_WIDTH, RESIZE_FIXED_HEIGHT, RESIZE_ZOOM)

private val volumeSteps = listOf(
    "Mute" to 0f, "15" to 0.15f, "30" to 0.30f, "45" to 0.45f,
    "70" to 0.70f, "85" to 0.85f, "Max" to 1.0f,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWindowCellOptions(
    stream: WindowStream,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSwap: ((Int) -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onFullscreen: (() -> Unit)? = null,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var overlayMode by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        when (overlayMode) {
            "swap" -> SwapPositionOverlay(
                currentSlotIndex = stream.slotIndex,
                onSelect = { targetSlot ->
                    onSwap?.invoke(targetSlot)
                    overlayMode = null
                    onDismiss()
                },
                onBack = { overlayMode = null },
            )
            "channels", "history", "favorites" -> ChannelOverlay(
                mode = overlayMode!!,
                currentSlotIndex = stream.slotIndex,
                onSelect = {
                    MultiWindowStore.addToSlot(it, stream.slotIndex)
                    overlayMode = null
                    onDismiss()
                },
                onBack = { overlayMode = null },
            )
            "quick" -> QuickChannelOverlay(
                currentSlotIndex = stream.slotIndex,
                onSelect = {
                    MultiWindowStore.addToSlot(it, stream.slotIndex)
                    overlayMode = null
                    onDismiss()
                },
                onBack = { overlayMode = null },
            )
            else -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stream.channel.name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Slot ${stream.slotIndex + 1}", color = OnSurfaceVariant, fontSize = 13.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CH" to "channels", "History" to "history", "Fav" to "favorites", "Quick" to "quick").forEach { (label, mode) ->
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable { overlayMode = mode }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                Text(label, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onSwap != null) {
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable { overlayMode = "swap" }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                Text("Swap", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))

                    Text("Volume", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        volumeSteps.forEach { (label, level) ->
                            val isActive = volume == level
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (isActive) Accent else SurfaceCard)
                                    .clickable { onVolumeChange(level) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    color = if (isActive) Color.White else OnSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))

                    Text("Scaling", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        scaleLabels.zip(scaleModes).forEach { (label, mode) ->
                            val currentMode = MultiWindowStore.getResizeMode(stream.id)
                            val isActive = currentMode == mode
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isActive) Accent else SurfaceCard).clickable { MultiWindowStore.setResizeMode(stream.id, mode) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(label, color = if (isActive) Color.White else OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))

                    if (onRefresh != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).clickable(onClick = onRefresh).padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Refresh Stream", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("↻", color = Accent, fontSize = 16.sp)
                        }
                    }

                    if (onFullscreen != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).clickable(onClick = onFullscreen).padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Fullscreen", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("⛶", color = Accent, fontSize = 16.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).clickable(onClick = onClose).padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Close Channel", color = Color(0xFFFF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("✕", color = Color(0xFFFF4444), fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SwapPositionOverlay(currentSlotIndex: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val maxSlots = 9
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text("Swap Slot", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("Select target slot to swap with:", color = OnSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (slot in 0 until maxSlots) {
                if (slot == currentSlotIndex) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${slot + 1}", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val hasStream = MultiWindowStore.getStreamsForSlot(slot) != null
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (hasStream) Accent else SurfaceLow).clickable(enabled = hasStream) { onSelect(slot) }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${slot + 1}", color = if (hasStream) Color.White else OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ChannelOverlay(mode: String, currentSlotIndex: Int, onSelect: (IptvChannel) -> Unit, onBack: () -> Unit) {
    val allChannels = remember { IptvRepository.getAllChannels() }
    val sourceNames = remember { IptvRepository.getAllSourceNames() }
    val sourceIds = remember { IptvRepository.getAllSourceIds() }
    val channels = remember(mode) {
        when (mode) {
            "history" -> IptvRepository.getHistoryChannels()
            "favorites" -> IptvRepository.getFavoriteChannels()
            else -> allChannels
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(-1) }
    var selectedGroup by remember { mutableStateOf("") }

    val filtered = remember(channels, searchQuery, selectedSource, selectedGroup) {
        var result = channels
        if (selectedSource >= 0 && selectedSource < sourceIds.size) {
            val sid = sourceIds[selectedSource]
            result = result.filter { it.sourceId == sid }
        }
        if (selectedGroup.isNotBlank()) {
            result = result.filter { it.group == selectedGroup }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }
        result
    }

    val groups = remember(channels) {
        channels.map { it.group ?: "Other" }.distinct().sorted()
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(480.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                when (mode) {
                    "history" -> "History"
                    "favorites" -> "Favorites"
                    else -> "All Channels"
                },
                color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("${filtered.size}", color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        if (mode == "channels") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search channels...", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                    focusedBorderColor = Accent, unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.3f),
                    cursorColor = Accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selectedSource < 0) Accent else SurfaceCard).clickable { selectedSource = -1 }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("All", color = if (selectedSource < 0) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                sourceIds.forEachIndexed { idx, _ ->
                    val isSel = selectedSource == idx
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSel) Accent else SurfaceCard).clickable { selectedSource = idx }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(sourceNames[idx].take(12), color = if (isSel) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selectedGroup.isBlank()) Accent else SurfaceCard).clickable { selectedGroup = "" }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("All Groups", color = if (selectedGroup.isBlank()) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                groups.take(20).forEach { g ->
                    val isSel = selectedGroup == g
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSel) Accent else SurfaceCard).clickable { selectedGroup = g }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(g.take(16), color = if (isSel) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filtered) { ch ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceLow).clickable { onSelect(ch) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!ch.logo.isNullOrBlank()) {
                        AsyncImage(model = ch.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceCard))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, color = OnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (ch.group != null && selectedGroup.isBlank()) {
                            Text(ch.group, color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Slot ${currentSlotIndex + 1}", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text("No ${mode} channels match", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
        }
    }
}

private val qcTabs = listOf("All", "US", "UK", "CA", "Premium", "Sports", "News")

private sealed class QcLoadState {
    data object Idle : QcLoadState()
    data object Loading : QcLoadState()
    data class Success(val channels: List<IptvChannel>) : QcLoadState()
    data class Error(val message: String) : QcLoadState()
}

@Composable
private fun QuickChannelOverlay(
    currentSlotIndex: Int,
    onSelect: (IptvChannel) -> Unit,
    onBack: () -> Unit,
) {
    val allChannels = remember { IptvRepository.getAllChannels() }
    val sourceNames = remember { IptvRepository.getAllSourceNames() }
    val sourceIds = remember { IptvRepository.getAllSourceIds() }
    var qcFilter by remember { mutableStateOf("All") }
    var selectedQc by remember { mutableStateOf<QuickChannel?>(null) }
    var loadState by remember { mutableStateOf<QcLoadState>(QcLoadState.Idle) }

    val filtered = remember(qcFilter) {
        QuickChannelList.all.filter { qc ->
            when (qcFilter) {
                "All" -> true; "US" -> "US" in qc.regions; "UK" -> "UK" in qc.regions
                "CA" -> "CA" in qc.regions; "Premium" -> "premium" in qc.tags
                "Sports" -> "sports" in qc.tags; "News" -> "news" in qc.tags
                else -> true
            }
        }
    }

    LaunchedEffect(selectedQc) {
        val qc = selectedQc ?: return@LaunchedEffect
        loadState = QcLoadState.Loading
        loadState = try {
            val matches = withContext(Dispatchers.Default) {
                allChannels.filter { ch ->
                    ch.name.contains(qc.displayName, ignoreCase = true) ||
                    qc.aliases.any { ch.name.contains(it, ignoreCase = true) }
                }
            }
            if (matches.isEmpty()) {
                QcLoadState.Error("No sources found for \"${qc.displayName}\"")
            } else {
                QcLoadState.Success(matches)
            }
        } catch (e: Exception) {
            QcLoadState.Error(e.message ?: "Failed to load sources")
        }
    }

    val onRetry: () -> Unit = {
        val current = selectedQc
        selectedQc = null
        selectedQc = current
    }

    when (val state = loadState) {
        is QcLoadState.Loading -> {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(480.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.align(Alignment.Start).clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = { selectedQc = null; loadState = QcLoadState.Idle }).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(color = Accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text("Resolving ${selectedQc?.displayName}...", color = OnSurfaceVariant, fontSize = 14.sp)
                Text("Scanning IPTV playlists for matching sources", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
        is QcLoadState.Error -> {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(480.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.align(Alignment.Start).clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = { selectedQc = null; loadState = QcLoadState.Idle }).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(48.dp))
                Text("⚠", color = Color(0xFFFF4444), fontSize = 36.sp)
                Spacer(Modifier.height(12.dp))
                Text(state.message, color = OnSurface, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(16.dp))
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Accent).clickable(onClick = onRetry).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        is QcLoadState.Success -> {
            QuickChannelMatchOverlay(
                quickChannel = selectedQc!!,
                matchedChannels = state.channels,
                sourceNames = sourceNames,
                sourceIds = sourceIds,
                currentSlotIndex = currentSlotIndex,
                onSelect = onSelect,
                onBack = { selectedQc = null; loadState = QcLoadState.Idle },
            )
        }
        QcLoadState.Idle -> {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(480.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Quick Channels", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size}", color = OnSurfaceVariant, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(qcTabs) { tab ->
                        val isActive = qcFilter == tab
                        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isActive) Accent else SurfaceCard).clickable { qcFilter = tab }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(tab, color = if (isActive) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.displayName }) { qc ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceLow).clickable { selectedQc = qc }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(qc.displayName, color = OnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("▸", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text("No quick channels match filter", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChannelMatchOverlay(
    quickChannel: QuickChannel,
    matchedChannels: List<IptvChannel>,
    sourceNames: List<String>,
    sourceIds: List<String>,
    currentSlotIndex: Int,
    onSelect: (IptvChannel) -> Unit,
    onBack: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(-1) }
    var selectedGroup by remember { mutableStateOf("") }

    val sourceNameForId = remember(sourceNames, sourceIds) {
        sourceIds.zip(sourceNames).toMap()
    }

    val filtered = remember(matchedChannels, searchQuery, selectedSource, selectedGroup) {
        var result = matchedChannels
        if (selectedSource >= 0 && selectedSource < sourceIds.size) {
            val sid = sourceIds[selectedSource]
            result = result.filter { it.sourceId == sid }
        }
        if (selectedGroup.isNotBlank()) {
            result = result.filter { it.group == selectedGroup }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }
        result
    }

    val groups = remember(matchedChannels) {
        matchedChannels.map { it.group ?: "Other" }.distinct().sorted()
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(480.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = onBack).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("← Back", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(quickChannel.displayName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.weight(1f))
            Text("${filtered.size}", color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search channels...", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                focusedBorderColor = Accent, unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.3f),
                cursorColor = Accent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
        )

        if (sourceIds.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selectedSource < 0) Accent else SurfaceCard).clickable { selectedSource = -1 }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("All", color = if (selectedSource < 0) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                sourceIds.forEachIndexed { idx, _ ->
                    val isSel = selectedSource == idx
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSel) Accent else SurfaceCard).clickable { selectedSource = idx }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(sourceNames[idx].take(12), color = if (isSel) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selectedGroup.isBlank()) Accent else SurfaceCard).clickable { selectedGroup = "" }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("All Groups", color = if (selectedGroup.isBlank()) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                groups.take(20).forEach { g ->
                    val isSel = selectedGroup == g
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSel) Accent else SurfaceCard).clickable { selectedGroup = g }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(g.take(16), color = if (isSel) Color.White else OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(filtered) { ch ->
                val providerName = sourceNameForId[ch.sourceId] ?: "Unknown"
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceLow).clickable { onSelect(ch) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!ch.logo.isNullOrBlank()) {
                        AsyncImage(model = ch.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceCard))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ch.name, color = OnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(providerName, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (ch.group != null && selectedGroup.isBlank()) {
                                Text(ch.group, color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (currentSlotIndex >= 0) {
                        Text("Slot ${currentSlotIndex + 1}", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Select", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text("No channels match \"${quickChannel.displayName}\"", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickChannelsSheet(
    onAddToSlot: (IptvChannel, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        QuickChannelOverlay(
            currentSlotIndex = -1,
            onSelect = { channel ->
                val nextSlot = (0 until 9).firstOrNull { MultiWindowStore.isSlotAvailable(it) } ?: 0
                onAddToSlot(channel, nextSlot)
                onDismiss()
            },
            onBack = onDismiss,
        )
    }
}
