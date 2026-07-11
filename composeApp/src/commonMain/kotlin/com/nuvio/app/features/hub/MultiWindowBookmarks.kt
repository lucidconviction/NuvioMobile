package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository

data class MultiWindowBookmark(
    val id: String,
    val name: String,
    val layoutName: String,
    val slotChannels: Map<Int, ChannelRef>,
)

data class ChannelRef(val id: String, val sourceId: String, val url: String)

object MultiWindowBookmarkStore {
    private val bookmarks = mutableListOf<MultiWindowBookmark>()
    private var idCounter = 0L

    fun all(): List<MultiWindowBookmark> = bookmarks.toList()

    fun save(name: String): MultiWindowBookmark {
        val layout = MultiWindowStore.currentLayout()
        val streams = MultiWindowStore.allStreams
        val slotChannels = streams.associate {
            it.slotIndex to ChannelRef(
                id = it.channel.id,
                sourceId = it.channel.sourceId,
                url = it.channel.url,
            )
        }
        val bm = MultiWindowBookmark(
            id = "bm_${++idCounter}",
            name = name,
            layoutName = layout?.name ?: "",
            slotChannels = slotChannels,
        )
        bookmarks.add(bm)
        return bm
    }

    fun load(bm: MultiWindowBookmark) {
        MultiWindowStore.clear()
        val allChannels = IptvRepository.getAllChannels()
        bm.slotChannels.forEach { (slotIndex, ref) ->
            val channel = allChannels.find { ch -> ch.id == ref.id && ch.sourceId == ref.sourceId }
            if (channel != null) {
                MultiWindowStore.addToSlot(channel, slotIndex)
            }
        }
    }

    fun delete(id: String) { bookmarks.removeAll { it.id == id } }
}

private val SurfaceBg = Color(0xFF000000)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceLow = Color(0xFF111111)
private val Accent = Color(0xFF4A90D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWindowBookmarksSheet(
    bookmarks: List<MultiWindowBookmark>,
    onSave: (String) -> Unit,
    onLoad: (MultiWindowBookmark) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSaveDialog by remember { mutableStateOf(false) }
    var loadConfirm by remember { mutableStateOf<MultiWindowBookmark?>(null) }
    var bookmarkName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Bookmarks", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${bookmarks.size}", color = OnSurfaceVariant, fontSize = 13.sp)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showSaveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Save Current Layout", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))
            Spacer(Modifier.height(8.dp))

            if (bookmarks.isEmpty()) {
                Text("No bookmarks yet", color = OnSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(300.dp)) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).clickable { loadConfirm = bm }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bm.name, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${bm.slotChannels.size} streams", color = OnSurfaceVariant, fontSize = 11.sp)
                                    Text("•", color = OnSurfaceVariant, fontSize = 11.sp)
                                    Text(bm.layoutName, color = OnSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceLow).clickable { onDelete(bm.id) }.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("✕", color = Color(0xFFFF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Save Bookmark", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = bookmarkName,
                    onValueChange = { bookmarkName = it },
                    label = { Text("Bookmark name", color = OnSurfaceVariant) },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookmarkName.isNotBlank()) {
                            onSave(bookmarkName)
                            bookmarkName = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = OnSurfaceVariant) } },
        )
    }

    loadConfirm?.let { bm ->
        AlertDialog(
            onDismissRequest = { loadConfirm = null },
            containerColor = SurfaceCard,
            title = { Text("Load Layout", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Replace current grid with \"${bm.name}\"?", color = OnSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { onLoad(bm); loadConfirm = null; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { loadConfirm = null }) { Text("Cancel", color = OnSurfaceVariant) } },
        )
    }
}
