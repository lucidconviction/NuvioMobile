package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.iptv.IptvChannel

private val SurfaceBg = Color(0xFF000000)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceLow = Color(0xFF111111)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val Accent = Color(0xFF4A90D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWindowPositionPicker(
    channel: IptvChannel,
    onDismiss: () -> Unit,
    onSlotSelected: (Int) -> Unit,
    onSlotSelectedAndOpenHub: ((Int) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSlotIndex by remember { mutableStateOf<Int?>(null) }
    val isSelectionMode = onSlotSelectedAndOpenHub != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Add to MultiWindow", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(channel.name, color = OnSurfaceVariant, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isTablet = maxWidth >= 600.dp
                val slotCount = if (isTablet) 9 else 6
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().height(((slotCount + 2) / 3 * 110).dp),
                ) {
                    items((0 until slotCount).toList()) { slotIndex ->
                    val existing = MultiWindowStore.getStreamsForSlot(slotIndex)
                    val isOccupied = existing != null
                    val isSelected = selectedSlotIndex == slotIndex

                    val borderMod = when {
                        isSelected -> Modifier.border(2.dp, Accent, RoundedCornerShape(12.dp))
                        isOccupied -> Modifier.border(1.dp, Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        else -> Modifier
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> Accent.copy(alpha = 0.25f)
                                    isOccupied -> SurfaceCard
                                    else -> SurfaceLow
                                }
                            )
                            .then(borderMod)
                            .clickable {
                                if (isSelectionMode) {
                                    selectedSlotIndex = if (isSelected) null else slotIndex
                                } else {
                                    onSlotSelected(slotIndex)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isOccupied) {
                                if (!existing!!.channel.logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = existing.channel.logo,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Text(existing!!.channel.name, color = OnSurface, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                                Text("Slot ${slotIndex + 1}", color = OnSurfaceVariant, fontSize = 8.sp)
                            } else {
                                Text("Slot ${slotIndex + 1}", color = OnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Empty", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
                            }
                        }
                    }
                    }
                }
            }

            if (isSelectionMode) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Add to slot button
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedSlotIndex != null) Accent else Accent.copy(alpha = 0.3f))
                            .clickable(enabled = selectedSlotIndex != null) {
                                selectedSlotIndex?.let { onSlotSelected(it) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add to Slot",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    // Add & Open Hub button
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedSlotIndex != null) Accent.copy(alpha = 0.8f) else Accent.copy(alpha = 0.15f))
                            .clickable(enabled = selectedSlotIndex != null) {
                                selectedSlotIndex?.let { onSlotSelectedAndOpenHub?.invoke(it) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add & Open Hub",
                            color = Color.White.copy(alpha = if (selectedSlotIndex != null) 1f else 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
