package com.nuvio.app.features.magnutz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.player.PlayerLaunch

private val ObsidianBg = Color(0xFF131313)
private val SurfaceCard = Color(0xCC1F1F1F)
private val SurfaceCardOpaque = Color(0xFF1F1F1F)
private val SurfaceContainerLow = Color(0xFF1B1B1B)
private val OnSurface = Color(0xFFE2E2E2)
private val OnSurfaceVariant = Color(0xFFC4C7C8)
private val Primary = Color(0xFFFDFDFC)
private val OnPrimary = Color(0xFF2F3131)
private val Accent = Color(0xFF4A90D9)
private val AccentGreen = Color(0xFF4CAF50)
private val ErrorColor = Color(0xFFFF4444)
private val OutlineVariant = Color(0xFF444748)
private val SurfaceContainerHighest = Color(0xFF353535)

@Composable
fun MagNutzScreen(
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
) {
    MagNutzRepository.ensureLoaded()
    LaunchedEffect(Unit) { MagNutzRepository.consumePendingMagnet() }
    val uiState by MagNutzRepository.uiState.collectAsState()

    val filteredItems = uiState.items.filter { item ->
        val matchesFilter = when (uiState.filter) {
            MagNutzFilter.All -> true
            MagNutzFilter.Downloading -> item.status == MagNutzStatus.Downloading || item.status == MagNutzStatus.Queued
            MagNutzFilter.Seeding -> item.status == MagNutzStatus.Seeding
            MagNutzFilter.Completed -> item.status == MagNutzStatus.Completed
            MagNutzFilter.Failed -> item.status == MagNutzStatus.Failed
        }
        val matchesSearch = uiState.searchQuery.isBlank() ||
            item.title.contains(uiState.searchQuery, ignoreCase = true) ||
            item.infoHash.contains(uiState.searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(ObsidianBg)) {
        val isTablet = maxWidth >= 768.dp

        if (isTablet) {
            TabletMagNutzLayout(
                uiState = uiState,
                filteredItems = filteredItems,
                onPlayChannel = onPlayChannel,
            )
        } else {
            PhoneMagNutzLayout(
                uiState = uiState,
                filteredItems = filteredItems,
                onPlayChannel = onPlayChannel,
            )
        }
    }

    if (uiState.isAddingMagnet) {
        AddMagnetDialog(
            text = uiState.magnetInputText,
            onTextChange = { MagNutzRepository.setMagnetInputText(it) },
            onConfirm = {
                val lines = uiState.magnetInputText.trim().lines().filter { it.isNotBlank() }
                lines.forEach { MagNutzRepository.addMagnet(it.trim()) }
                MagNutzRepository.setAddingMagnet(false)
            },
            onDismiss = { MagNutzRepository.setAddingMagnet(false) },
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { MagNutzRepository.clearError() },
            title = { Text("Error", color = OnSurface) },
            text = { Text(error, color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { MagNutzRepository.clearError() }) {
                    Text("OK", color = Accent)
                }
            },
            containerColor = SurfaceCardOpaque,
        )
    }
}

@Composable
private fun PhoneMagNutzLayout(
    uiState: MagNutzUiState,
    filteredItems: List<MagNutzItem>,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    Column(Modifier.fillMaxSize()) {
        SearchAndAddBar(uiState = uiState)
        FilterChipsRow(uiState = uiState)
        SaveLocationRow()

        if (uiState.items.isEmpty()) {
            EmptyState(hasFilter = uiState.filter != MagNutzFilter.All)
        } else if (filteredItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching torrents", color = OnSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    MagNutzDownloadCard(
                        item = item,
                        onPlay = onPlayChannel?.let { { item.playWith(it) } },
                        onPause = { MagNutzRepository.pauseDownload(item.id) },
                        onResume = { MagNutzRepository.resumeDownload(item.id) },
                        onCancel = { MagNutzRepository.cancelDownload(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletMagNutzLayout(
    uiState: MagNutzUiState,
    filteredItems: List<MagNutzItem>,
    onPlayChannel: ((PlayerLaunch) -> Unit)?,
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    val selectedItem = filteredItems.find { it.id == selectedItemId }
    if (selectedItemId == null && filteredItems.isNotEmpty()) selectedItemId = filteredItems.first().id

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            SearchAndAddBar(uiState = uiState)
            FilterChipsRow(uiState = uiState)
            SaveLocationRow()

            if (uiState.items.isEmpty()) {
                EmptyState(hasFilter = uiState.filter != MagNutzFilter.All)
            } else if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching torrents", color = OnSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        MagNutzGridCard(
                            item = item,
                            isSelected = item.id == selectedItemId,
                            onClick = { selectedItemId = item.id },
                            onPlay = onPlayChannel?.let { { item.playWith(it) } },
                            onPause = { MagNutzRepository.pauseDownload(item.id) },
                            onResume = { MagNutzRepository.resumeDownload(item.id) },
                            onCancel = { MagNutzRepository.cancelDownload(item.id) },
                        )
                    }
                }
            }
        }

        if (selectedItem != null && uiState.items.isNotEmpty()) {
            DetailPanel(
                item = selectedItem,
                onPlay = onPlayChannel?.let { { selectedItem.playWith(it) } },
                onPause = { MagNutzRepository.pauseDownload(selectedItem.id) },
                onResume = { MagNutzRepository.resumeDownload(selectedItem.id) },
                onCancel = { MagNutzRepository.cancelDownload(selectedItem.id) },
            )
        }
    }
}

@Composable
private fun SearchAndAddBar(uiState: MagNutzUiState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { MagNutzRepository.setSearchQuery(it) },
            placeholder = { Text("Search torrents...", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                { IconButton(onClick = { MagNutzRepository.setSearchQuery("") }) { Icon(Icons.Default.Clear, "Clear", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) } }
            } else null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                focusedBorderColor = OutlineVariant, unfocusedBorderColor = OutlineVariant,
                cursorColor = OnSurface, focusedContainerColor = SurfaceContainerLow, unfocusedContainerColor = SurfaceContainerLow,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
            modifier = Modifier.weight(1f),
        )

        Surface(
            onClick = { MagNutzRepository.setAddingMagnet(true) },
            shape = RoundedCornerShape(12.dp),
            color = Primary,
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Add, null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                Text("ADD MAGNET", color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilterChipsRow(uiState: MagNutzUiState) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MagNutzFilter.entries.forEach { filter ->
            val selected = uiState.filter == filter
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (selected) Primary else SurfaceCard)
                    .border(if (selected) 0.dp else 1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .clickable { MagNutzRepository.setFilter(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    filter.label,
                    color = if (selected) OnPrimary else OnSurfaceVariant,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SaveLocationRow() {
    val path = remember { MagNutzRepository.getSaveLocationDisplayPath() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Save to:", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(
            path.substringAfterLast("/").take(20),
            color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceCard)
                .clickable { MagNutzRepository.requestPickSaveLocation() }
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text("Change", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceCard),
                contentAlignment = Alignment.Center,
            ) {
                Text("MG", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (hasFilter) "No torrents match this filter" else "No torrents yet",
                color = OnSurfaceVariant, fontSize = 14.sp,
            )
            if (!hasFilter) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(9999.dp)).background(Accent)
                        .clickable { MagNutzRepository.setAddingMagnet(true) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text("Add Magnet Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MagNutzDownloadCard(
    item: MagNutzItem,
    onPlay: (() -> Unit)?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text("MG", color = OnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(ErrorColor.copy(alpha = 0.2f)).clickable { onCancel() }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("Delete", color = ErrorColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                when (item.status) {
                    MagNutzStatus.Downloading, MagNutzStatus.Queued -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { item.progressFraction },
                                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Accent,
                                trackColor = SurfaceContainerHighest,
                            )
                            Text("${(item.progressFraction * 100).toInt()}%", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatSize(item.totalBytes), color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text("↓${formatSpeed(item.downloadSpeed)}", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text("${item.seeds} seeds", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                    MagNutzStatus.Paused -> {
                        Text("Paused — ${formatSize(item.totalBytes)}", color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                    MagNutzStatus.Seeding -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Seeding", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Ratio ${round2(item.ratio)}", color = OnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    MagNutzStatus.Completed -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Completed", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(formatSize(item.totalBytes), color = OnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    MagNutzStatus.Failed -> {
                        Text(item.errorMessage ?: "Failed", color = ErrorColor, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            ActionButton(item = item, onPlay = onPlay, onPause = onPause, onResume = onResume, onCancel = onCancel)
        }
    }
}

@Composable
private fun MagNutzGridCard(
    item: MagNutzItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlay: (() -> Unit)?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SurfaceCardOpaque else SurfaceContainerLow)
            .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) Primary else OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(12.dp),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text("MG", color = OnSurfaceVariant.copy(alpha = 0.3f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (item.status == MagNutzStatus.Downloading || item.status == MagNutzStatus.Queued) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                        color = Accent, trackColor = SurfaceContainerHighest,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(item.status)
                Text(formatSize(item.totalBytes), color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ActionButton(item = item, onPlay = onPlay, onPause = onPause, onResume = onResume, onCancel = onCancel)
            }
        }
    }
}

@Composable
private fun DetailPanel(
    item: MagNutzItem,
    onPlay: (() -> Unit)?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier.width(300.dp).fillMaxSize()
            .background(SurfaceCard)
            .border(0.5.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            .padding(20.dp),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text("MG", color = OnSurfaceVariant.copy(alpha = 0.3f), fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(item.title, color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow("Status", item.status.name)
            InfoRow("Size", formatSize(item.totalBytes))
            InfoRow("Downloaded", formatSize(item.downloadedBytes))
            InfoRow("Progress", "${(item.progressFraction * 100).toInt()}%")
            InfoRow("Speed", formatSpeed(item.downloadSpeed))
            InfoRow("Peers", "${item.peers}")
            InfoRow("Seeds", "${item.seeds}")
            InfoRow("Ratio", round2(item.ratio))
            if (item.errorMessage != null) InfoRow("Error", item.errorMessage!!)
            Spacer(Modifier.height(16.dp))

            if (item.status == MagNutzStatus.Downloading || item.status == MagNutzStatus.Queued) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Accent, trackColor = SurfaceContainerHighest,
                )
                Spacer(Modifier.height(16.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    MagNutzStatus.Downloading, MagNutzStatus.Queued -> {
                        Button(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest)) {
                            Text("Pause", color = OnSurface, fontSize = 13.sp)
                        }
                    }
                    MagNutzStatus.Paused -> {
                        Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                            Text("Resume", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    MagNutzStatus.Completed -> {
                        if (onPlay != null) {
                            Button(onClick = onPlay, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                                Text("Play", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                    MagNutzStatus.Failed -> {
                        Button(onClick = onResume, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                            Text("Retry", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    MagNutzStatus.Seeding -> {}
                }
                Button(onClick = onCancel, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest)) {
                    Text("Remove", color = ErrorColor, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: MagNutzStatus) {
    val (color, text) = when (status) {
        MagNutzStatus.Downloading -> Accent to "D/L"
        MagNutzStatus.Queued -> OnSurfaceVariant to "QUE"
        MagNutzStatus.Paused -> OnSurfaceVariant to "PAU"
        MagNutzStatus.Seeding -> AccentGreen to "SEE"
        MagNutzStatus.Completed -> AccentGreen to "DON"
        MagNutzStatus.Failed -> ErrorColor to "ERR"
    }
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(
    item: MagNutzItem,
    onPlay: (() -> Unit)?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val tint: Color
    val action: () -> Unit
    when (item.status) {
        MagNutzStatus.Downloading, MagNutzStatus.Queued -> {
            icon = Icons.Default.Pause; tint = OnSurface; action = onPause
        }
        MagNutzStatus.Paused -> {
            icon = Icons.Default.PlayArrow; tint = Accent; action = onResume
        }
        MagNutzStatus.Completed -> {
            icon = Icons.Default.PlayArrow; tint = AccentGreen; action = onPlay ?: {}
        }
        MagNutzStatus.Failed -> {
            icon = Icons.Default.Refresh; tint = Accent; action = onResume
        }
        MagNutzStatus.Seeding -> {
            icon = Icons.Default.Clear; tint = OnSurfaceVariant; action = onCancel
        }
    }
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerHighest)
            .clickable(onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label  ", color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddMagnetDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Magnet Link", color = OnSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Paste magnet link(s) below:", color = OnSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("magnet:?xt=urn:btih:...", color = OnSurfaceVariant.copy(alpha = 0.4f), fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                        focusedBorderColor = Accent, unfocusedBorderColor = OutlineVariant,
                        cursorColor = Accent, focusedContainerColor = SurfaceContainerLow, unfocusedContainerColor = SurfaceContainerLow,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                enabled = text.isNotBlank()) {
                Text("Add & Download", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurfaceVariant) }
        },
        containerColor = SurfaceCardOpaque,
    )
}

private fun MagNutzItem.playWith(onPlayChannel: (PlayerLaunch) -> Unit) {
    MagNutzRepository.playDownload(this)?.let(onPlayChannel)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${round1(bytes / 1_073_741_824.0)} GB"
    bytes >= 1_048_576L -> "${round1(bytes / 1_048_576.0)} MB"
    bytes >= 1_024L -> "${bytes / 1_024} KB"
    else -> "$bytes B"
}

private fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec >= 1_048_576L -> "${round1(bytesPerSec / 1_048_576.0)} MB/s"
    bytesPerSec >= 1_024L -> "${bytesPerSec / 1_024} KB/s"
    else -> "$bytesPerSec B/s"
}

private fun round2(value: Float): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val whole = rounded.toInt()
    val frac = ((rounded - whole) * 100.0).toInt()
    return "$whole.${if (frac < 10) "0" else ""}$frac"
}

private fun round1(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    val whole = rounded.toLong()
    val frac = ((rounded - whole) * 10.0).toInt()
    return "$whole.$frac"
}
