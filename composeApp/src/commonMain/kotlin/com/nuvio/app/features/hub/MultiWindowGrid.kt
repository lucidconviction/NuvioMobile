package com.nuvio.app.features.hub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ── Digital Kinetic color tokens (matching design system) ──
private val Primary = Color(0xFFadc6ff)
private val PrimaryContainer = Color(0xFF4b8eff)
private val OnPrimaryContainer = Color(0xFF00285c)
private val SurfaceContainer = Color(0xFF201f1f)
private val SurfaceContainerLow = Color(0xFF1c1b1b)
private val SurfaceContainerHigh = Color(0xFF2a2a2a)
private val SurfaceContainerHighest = Color(0xFF353534)
private val OnSurface = Color(0xFFe5e2e1)
private val OnSurfaceVariant = Color(0xFFc1c6d7)
private val OutlineVariant = Color(0xFF414755)
private val Outline = Color(0xFF8b90a0)
private val ErrorColor = Color(0xFFffb4ab)
private val ErrorContainerAlpha = Color(0x33ffb4ab) // error/20
private val ErrorBorderAlpha = Color(0x4dffb4ab)    // error/30
private val GlassBg = Color(0x991E1E1E)             // rgba(30,30,30,0.6)

@Composable
fun MultiWindowGrid(
    streams: List<WindowStream>,
    onRemoveStream: (String) -> Unit,
    onAddMore: () -> Unit,
    onCellLongPress: (WindowStream) -> Unit,
    onCellVolumeToggle: (WindowStream, Boolean) -> Unit,
    onBookmarksClick: (() -> Unit)? = null,
    onMuteAll: (() -> Unit)? = null,
    onCloseAll: (() -> Unit)? = null,
    onPauseAll: (() -> Unit)? = null,
    onRefreshAll: (() -> Unit)? = null,
    onFullscreenCell: ((WindowStream) -> Unit)? = null,
) {
    if (streams.isEmpty()) {
        // ── Empty State (render-match) ──
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("MW", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text("No streams added", color = OnSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(9999.dp)).background(PrimaryContainer)
                        .clickable(onClick = onAddMore).padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text("Browse IPTV Channels", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                if (onBookmarksClick != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp)).background(SurfaceContainer)
                            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable(onClick = onBookmarksClick).padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text("★ Saved Layouts", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isPortrait = maxWidth < maxHeight
        val isTablet = maxWidth >= 600.dp
        val maxSlots = if (isTablet) 9 else 6
        val count = streams.size
        val layout = MultiWindowStore.resolveLayout(count, isPortrait, isTablet)
        val slots = layout.calculateSlots(count)
        val totalRows = (slots.maxOfOrNull { it.row + it.rowSpan } ?: 1).coerceAtLeast(1)
        val totalCols = (slots.maxOfOrNull { it.col + it.colSpan } ?: 1).coerceAtLeast(1)

        Column(Modifier.fillMaxSize()) {
            // ── Header: Title + Active Count Badge ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("MultiNutz", color = Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = -0.3.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Active count badge (pulsing red dot)
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp)).background(ErrorContainerAlpha)
                            .border(0.5.dp, ErrorBorderAlpha, RoundedCornerShape(9999.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(ErrorColor))
                            Text("${streams.size} ACTIVE", color = ErrorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }

            // ── Pill Row (render-match) ──
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val validLayouts = getValidLayouts(count, isPortrait, isTablet)
                val isAuto = !MultiWindowStore.isLayoutLocked()

                // Auto pill
                Box(
                    Modifier.clip(RoundedCornerShape(9999.dp))
                        .background(if (isAuto) PrimaryContainer else SurfaceContainer)
                        .border(if (isAuto) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                        .clickable { MultiWindowStore.setAutoLayout() }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text("Auto", color = if (isAuto) Color.White else OnSurfaceVariant,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                validLayouts.forEach { l ->
                    val isSelected = MultiWindowStore.currentLayout() == l || (isAuto && defaultLayout(count, isPortrait, isTablet) == l)
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) PrimaryContainer else SurfaceContainer)
                            .border(if (isSelected) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable { MultiWindowStore.setLayout(l) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        Text(l.label, color = if (isSelected) Color.White else OnSurfaceVariant,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                // Divider + Bookmarks pill
                Box(Modifier.width(0.5.dp).height(16.dp).background(OutlineVariant.copy(alpha = 0.3f)))
                if (onBookmarksClick != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp)).background(SurfaceContainer)
                            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable(onClick = onBookmarksClick)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("★ Bookmarks", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                // Mute All
                if (onMuteAll != null && streams.isNotEmpty()) {
                    val allMuted = streams.all { MultiWindowStore.getVolume(it.id) == 0f }
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp))
                            .background(if (allMuted) SurfaceContainer else PrimaryContainer)
                            .border(if (!allMuted) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable(onClick = onMuteAll)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(if (allMuted) "Sound All" else "Mute All",
                            color = if (allMuted) OnSurfaceVariant else Color.White,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                // Pause All
                if (onPauseAll != null && streams.isNotEmpty()) {
                    val allPaused = streams.all { MultiWindowStore.isPaused(it.id) }
                    val pauseLabel = if (allPaused) "Play All" else "Pause All"
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp))
                            .background(if (allPaused) SurfaceContainer else PrimaryContainer)
                            .border(if (!allPaused) 0.dp else 0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable(onClick = onPauseAll)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(pauseLabel, color = if (allPaused) OnSurfaceVariant else Color.White,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                // Refresh All
                if (onRefreshAll != null && streams.isNotEmpty()) {
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp)).background(SurfaceContainer)
                            .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                            .clickable(onClick = onRefreshAll)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("Refresh All", color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                // Close All
                if (onCloseAll != null && streams.isNotEmpty()) {
                    Box(
                        Modifier.clip(RoundedCornerShape(9999.dp)).background(SurfaceContainer)
                            .border(0.5.dp, ErrorBorderAlpha, RoundedCornerShape(9999.dp))
                            .clickable(onClick = onCloseAll)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("Close All", color = ErrorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            // ── Video Grid ──
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                var gridPos = 0
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until totalCols) {
                            val slot = slots.find { it.row == row && it.col == col }
                            if (slot != null) {
                                val stream = streams.getOrNull(slot.index)
                                if (stream != null) {
                                    gridPos++
                                    val isAudioFocused = MultiWindowStore.isAudioFocused(stream.id)
                                    key(stream.id) {
                                        VideoCell(
                                            stream = stream,
                                            gridPosition = gridPos,
                                            isAudioFocused = isAudioFocused,
                                            onRemove = { onRemoveStream(stream.id) },
                                            onLongPress = { onCellLongPress(stream) },
                                            onVolumeToggle = { active -> onCellVolumeToggle(stream, active) },
                                            onFullscreen = onFullscreenCell?.let { { it(stream) } },
                                            modifier = Modifier.weight((slot.colSpan * totalRows).toFloat()).padding(4.dp),
                                        )
                                    }
                                } else {
                                    // Empty slot (dashed border style)
                                    Box(
                                        Modifier.weight((slot.colSpan * totalRows).toFloat()).padding(4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceContainerLow.copy(alpha = 0.5f))
                                            .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
                // Add Channel button
                if (streams.size < maxSlots) {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow.copy(alpha = 0.3f))
                            .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onAddMore),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(SurfaceContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Add, "Add", tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                            Text("Add Channel", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCell(
    stream: WindowStream,
    gridPosition: Int,
    isAudioFocused: Boolean,
    onRemove: () -> Unit,
    onLongPress: () -> Unit,
    onVolumeToggle: (Boolean) -> Unit,
    onFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val playerHandle = remember(stream.id) {
        MultiWindowPlayerManager.createPlayer(stream.channel.url, emptyMap()).also { MultiWindowStore.storePlayerHandle(stream.id, it.id) }
    }
    DisposableEffect(stream.id) {
        MultiWindowPlayerManager.setVolume(playerHandle, MultiWindowStore.getVolume(stream.id))
        onDispose { MultiWindowPlayerManager.releasePlayer(playerHandle) }
    }
    var isAudioActive by remember { mutableStateOf(MultiWindowStore.getVolume(stream.id) > 0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsInteractionTrigger by remember { mutableStateOf(0) }

    // Audio focus gets 2dp primary border + glow
    val borderModifier = if (isAudioFocused) {
        Modifier.border(2.dp, Primary, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    }

    val currentResizeMode = MultiWindowStore.getResizeMode(stream.id)

    // Auto-hide controls after 3.5s of inactivity
    LaunchedEffect(controlsVisible, controlsInteractionTrigger) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    fun onInteraction() {
        controlsInteractionTrigger++
        controlsVisible = true
    }

    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "multiWindowControlsAlpha",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .then(borderModifier)
            .clickable { onInteraction() },
    ) {
        MultiWindowVideoSurface(handle = playerHandle, modifier = Modifier.fillMaxSize(), resizeMode = currentResizeMode)

        // Glass overlay (fade in/out)
        Box(
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { alpha = controlsAlpha },
        ) {
            // Glass background
            Box(Modifier.fillMaxSize().background(GlassBg))

            // Top-left: slot + channel name badge (primary bg)
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp)
                    .clip(RoundedCornerShape(4.dp)).background(PrimaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("$gridPosition | ${stream.channel.name}", color = Color.White,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Top-right: fullscreen button
            if (onFullscreen != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(28.dp).clip(CircleShape)
                        .background(SurfaceContainerHighest.copy(alpha = 0.8f))
                        .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), CircleShape)
                        .clickable { onFullscreen(); onInteraction() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⛶", color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Center: play/pause button
            Box(
                Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape)
                    .background(PrimaryContainer.copy(alpha = 0.2f))
                    .border(0.5.dp, Primary.copy(alpha = 0.4f), CircleShape)
                    .clickable { isPlaying = !isPlaying; onInteraction() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play/Pause", tint = Primary, modifier = Modifier.size(18.dp))
            }

            // Bottom-right: volume toggle chip
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .clip(RoundedCornerShape(9999.dp)).background(SurfaceContainerHighest.copy(alpha = 0.8f))
                    .border(0.5.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                    .clickable {
                        isAudioActive = !isAudioActive
                        val vol = if (isAudioActive) 1f else 0f
                        MultiWindowPlayerManager.setVolume(playerHandle, vol)
                        MultiWindowStore.setVolume(stream.id, vol)
                        if (isAudioActive) {
                            MultiWindowPlayerManager.setAudioFocus(playerHandle.id)
                            MultiWindowStore.setAudioFocus(stream.id)
                        }
                        onVolumeToggle(isAudioActive)
                        onInteraction()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(if (isAudioActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        "Audio", tint = if (isAudioActive) Primary else OnSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Text("${(MultiWindowStore.getVolume(stream.id) * 100).toInt()}%",
                        color = if (isAudioActive) OnSurface else OnSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom-left: options menu (⋮)
            Box(
                Modifier.align(Alignment.BottomStart).padding(6.dp)
                    .clip(RoundedCornerShape(6.dp)).background(SurfaceContainerHighest.copy(alpha = 0.8f))
                    .clickable(onClick = { onLongPress(); onInteraction() })
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("⋮", color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
