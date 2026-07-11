package com.nuvio.app.features.hub

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val Accent = Color(0xFF4A90D9)
private val SurfaceLow = Color(0xFF111111)
private val OnSurface = Color(0xFFE0E0E0)

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
) {
    if (streams.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No streams added", color = OnSurfaceVariant, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceCard).clickable(onClick = onAddMore).padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("Browse IPTV Channels", color = Accent, fontWeight = FontWeight.Bold)
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
        val layout = MultiWindowStore.resolveLayout(count, isPortrait)
        val slots = layout.calculateSlots(count)
        val totalRows = (slots.maxOfOrNull { it.row + it.rowSpan } ?: 1).coerceAtLeast(1)
        val totalCols = (slots.maxOfOrNull { it.col + it.colSpan } ?: 1).coerceAtLeast(1)

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val validLayouts = getValidLayouts(count, isPortrait)
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (!MultiWindowStore.isLayoutLocked()) Accent else SurfaceCard).clickable { MultiWindowStore.setAutoLayout() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text("Auto", color = if (!MultiWindowStore.isLayoutLocked()) Color.White else OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                validLayouts.forEach { l ->
                    val isSelected = MultiWindowStore.currentLayout() == l || (!MultiWindowStore.isLayoutLocked() && defaultLayout(count, isPortrait) == l)
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSelected) Accent else SurfaceCard).clickable { MultiWindowStore.setLayout(l) }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(l.label, color = if (isSelected) Color.White else OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onBookmarksClick != null) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable(onClick = onBookmarksClick).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("★", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onMuteAll != null && streams.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable(onClick = onMuteAll).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("Mute All", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onPauseAll != null && streams.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable(onClick = onPauseAll).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("Pause All", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onCloseAll != null && streams.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(SurfaceCard).clickable(onClick = onCloseAll).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("Close All", color = Color(0xFFFF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until totalCols) {
                            val slot = slots.find { it.row == row && it.col == col }
                            if (slot != null) {
                                val stream = streams.getOrNull(slot.index)
                                if (stream != null) {
                                    key(stream.id) {
                                        VideoCell(stream = stream, onRemove = { onRemoveStream(stream.id) }, onLongPress = { onCellLongPress(stream) }, onVolumeToggle = { active -> onCellVolumeToggle(stream, active) }, modifier = Modifier.weight((slot.colSpan * totalRows).toFloat()).padding(4.dp))
                                    }
                                } else {
                                    Box(Modifier.weight((slot.colSpan * totalRows).toFloat()).padding(4.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceLow))
                                }
                            }
                        }
                    }
                }
                if (streams.size < maxSlots) {
                    Box(Modifier.fillMaxWidth().height(48.dp).padding(4.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceLow).clickable(onClick = onAddMore), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, "Add", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Channel", color = OnSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCell(stream: WindowStream, onRemove: () -> Unit, onLongPress: () -> Unit, onVolumeToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val playerHandle = remember(stream.id) {
        MultiWindowPlayerManager.createPlayer(stream.channel.url, emptyMap()).also { MultiWindowStore.storePlayerHandle(stream.id, it.id) }
    }
    DisposableEffect(stream.id) {
        MultiWindowPlayerManager.setVolume(playerHandle, MultiWindowStore.getVolume(stream.id))
        onDispose { MultiWindowPlayerManager.releasePlayer(playerHandle) }
    }
    var isAudioActive by remember { mutableStateOf(MultiWindowStore.getVolume(stream.id) > 0f) }
    var isPlaying by remember { mutableStateOf(true) }
    val borderModifier = if (isAudioActive) Modifier.border(2.dp, Accent, RoundedCornerShape(12.dp)) else Modifier

    val currentResizeMode = MultiWindowStore.getResizeMode(stream.id)

    Box(modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceCard).then(borderModifier)) {
        MultiWindowVideoSurface(handle = playerHandle, modifier = Modifier.fillMaxSize(), resizeMode = currentResizeMode)
        Box(Modifier.align(Alignment.TopStart).padding(3.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
            Text("${stream.slotIndex + 1} ${stream.channel.name}", color = Color.White, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable { isPlaying = !isPlaying }, contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(18.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.7f)).clickable {
            isAudioActive = !isAudioActive; val vol = if (isAudioActive) 1f else 0f
            MultiWindowPlayerManager.setVolume(playerHandle, vol); MultiWindowStore.setVolume(stream.id, vol)
            if (isAudioActive) { MultiWindowPlayerManager.setAudioFocus(playerHandle.id); MultiWindowStore.setAudioFocus(stream.id) }
            onVolumeToggle(isAudioActive)
        }, contentAlignment = Alignment.Center) {
            Icon(if (isAudioActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff, "Audio", tint = if (isAudioActive) Accent else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(10.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).padding(3.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onLongPress).padding(horizontal = 6.dp, vertical = 4.dp)) {
            Text("⋮", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
