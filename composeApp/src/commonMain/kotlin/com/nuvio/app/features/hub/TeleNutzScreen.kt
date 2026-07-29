package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.player.PlayerLaunch
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ObsidianBg = Color(0xFF000000)
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFFFFFFF)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val TertiaryText = Color(0xFF888888)
private val BorderColor = Color(0xFF2A2A2A)
private val ChipBg = Color(0xFF2A2A2A)
private val ChipBgSelected = Color(0xFFFFFFFF)
private val ChipTextSelected = Color(0xFF000000)
private val AccentRed = Color(0xFFFF4444)
private val AccentGreen = Color(0xFF4CAF50)

@Composable
fun TeleNutzScreen(
    onPlayChannel: ((PlayerLaunch) -> Unit)? = null,
) {
    var uiState by remember {
        mutableStateOf(
            TeleNutzUiState(
                searchQuery = TeleNutzRepository.lastSearchQuery,
                searchResults = TeleNutzRepository.lastSearchResults,
            )
        )
    }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    fun refreshBookmarksAndDownloads() {
        uiState = uiState.copy(
            bookmarkedVideos = TeleNutzRepository.getBookmarkedVideos(),
            downloadedVideos = TeleNutzRepository.getDownloadedVideos(),
        )
    }

    LaunchedEffect(Unit) {
        TeleNutzRepository.start()
        refreshBookmarksAndDownloads()
        while (true) {
            val info = TeleNutzRepository.getAuthInfo()
            uiState = uiState.copy(authState = info.state, authQrUrl = info.qrCodeUrl, authError = info.error)
            if (info.state == TelegramAuthState.Ready || info.state == TelegramAuthState.Closed) break
            delay(500)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        val isTablet = maxWidth >= 768.dp
        val columns = if (isTablet) GridCells.Fixed(3) else GridCells.Fixed(2)

        if (uiState.needsAuth) {
            AuthScreen(uiState = uiState, onUpdate = { uiState = it }, scope = scope)
            return@BoxWithConstraints
        }

        val currentList = when (uiState.selectedTab) {
            TeleNutzTab.SEARCH -> uiState.searchResults
            TeleNutzTab.BOOKMARKS -> uiState.bookmarkedVideos
            TeleNutzTab.DOWNLOADS -> uiState.downloadedVideos
        }

        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                    Text("TeleNutz", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF444444))
                            .clickable { scope.launch { TeleNutzRepository.close(); TeleNutzRepository.start() } }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("Sign Out", color = Color(0xFFFF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }  // closes the title+signout Row

                    // Navigation Pills
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TabChip(
                            label = "Search",
                            isSelected = uiState.selectedTab == TeleNutzTab.SEARCH,
                            onClick = { uiState = uiState.copy(selectedTab = TeleNutzTab.SEARCH) },
                        )
                        TabChip(
                            label = "Bookmarks (${uiState.bookmarkedVideos.size})",
                            isSelected = uiState.selectedTab == TeleNutzTab.BOOKMARKS,
                            onClick = {
                                refreshBookmarksAndDownloads()
                                uiState = uiState.copy(selectedTab = TeleNutzTab.BOOKMARKS)
                            },
                        )
                        TabChip(
                            label = "Downloads (${uiState.downloadedVideos.size})",
                            isSelected = uiState.selectedTab == TeleNutzTab.DOWNLOADS,
                            onClick = {
                                refreshBookmarksAndDownloads()
                                uiState = uiState.copy(selectedTab = TeleNutzTab.DOWNLOADS)
                            },
                        )
                    }

                    if (uiState.selectedTab == TeleNutzTab.SEARCH) {
                        SearchField(
                            query = uiState.searchQuery,
                            onQueryChange = { uiState = uiState.copy(searchQuery = it) },
                            onSearch = {
                                val q = uiState.searchQuery.trim()
                                if (q.isNotBlank()) {
                                    uiState = uiState.copy(isSearching = true, searchResults = emptyList(), error = null)
                                    scope.launch {
                                        try {
                                            val results = TeleNutzRepository.searchVideos(q)
                                            uiState = uiState.copy(
                                                searchResults = results,
                                                isSearching = false,
                                                error = if (results.isEmpty()) "No videos found" else null,
                                            )
                                        } catch (e: Exception) {
                                            uiState = uiState.copy(isSearching = false, error = "Error: ${e.message}")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (uiState.selectedTab == TeleNutzTab.SEARCH && uiState.isSearching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = OnSurfaceVariant.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Searching Telegram...", color = OnSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            } else if (uiState.selectedTab == TeleNutzTab.SEARCH && uiState.error != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else if (currentList.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        val msg = when (uiState.selectedTab) {
                            TeleNutzTab.SEARCH -> "Search for Telegram videos above"
                            TeleNutzTab.BOOKMARKS -> "No bookmarked videos yet"
                            TeleNutzTab.DOWNLOADS -> "No downloaded/cached videos yet"
                        }
                        Text(msg, color = OnSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(currentList, key = { "${it.chatId}_${it.id}" }) { video ->
                    val isBookmarked = TeleNutzStore.isBookmarked(video.id, video.chatId)
                    val isDownloaded = TeleNutzStore.isDownloaded(video.id, video.chatId)
                    val downloadProgress = uiState.activeDownloads[video.fileId] ?: video.downloadProgress

                    var isResolvingPlayback by remember { mutableStateOf(false) }

                    TeleNutzVideoCard(
                        video = video.copy(
                            isBookmarked = isBookmarked,
                            isDownloaded = isDownloaded,
                            downloadProgress = if (isResolvingPlayback) 0.01f else downloadProgress,
                        ),
                        onPlay = {
                            if (onPlayChannel != null && !isResolvingPlayback) {
                                isResolvingPlayback = true
                                scope.launch {
                                    try {
                                        val launch = TeleNutzRepository.resolveVideoPlayback(video)
                                        if (launch != null) {
                                            val currentIndex = currentList.indexOfFirst { it.id == video.id && it.chatId == video.chatId }
                                            val queueUrls = currentList.map { v ->
                                                "telenutz://${v.chatId}/${v.id}/${v.fileId}/${v.chatTitle}?text=${v.text}"
                                            }
                                            val queueTitles = currentList.map { v ->
                                                v.text.ifBlank { v.chatTitle }
                                            }
                                            val launchWithQueue = launch.copy(
                                                autoPlayQueueUrls = queueUrls,
                                                autoPlayQueueTitles = queueTitles,
                                                autoPlayQueueIndex = if (currentIndex >= 0) currentIndex else 0,
                                            )
                                            onPlayChannel(launchWithQueue)
                                        }
                                    } finally {
                                        isResolvingPlayback = false
                                    }
                                }
                            }
                        },
                        onToggleBookmark = {
                            TeleNutzRepository.toggleBookmark(video)
                            refreshBookmarksAndDownloads()
                        },
                        onDownload = {
                            if (video.fileId > 0 && !isDownloaded && !uiState.activeDownloads.containsKey(video.fileId)) {
                                scope.launch {
                                    val downloadsMap = uiState.activeDownloads.toMutableMap()
                                    downloadsMap[video.fileId] = 0.01f
                                    uiState = uiState.copy(activeDownloads = downloadsMap)

                                    val result = TeleNutzRepository.downloadVideo(video) { p ->
                                        val map = uiState.activeDownloads.toMutableMap()
                                        map[video.fileId] = p
                                        uiState = uiState.copy(activeDownloads = map)
                                    }

                                    val map = uiState.activeDownloads.toMutableMap()
                                    map.remove(video.fileId)
                                    uiState = uiState.copy(activeDownloads = map)
                                    refreshBookmarksAndDownloads()
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                TeleNutzRepository.deleteDownload(video)
                                refreshBookmarksAndDownloads()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) ChipBgSelected else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (isSelected) ChipTextSelected else OnSurface,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search Telegram videos...", color = TertiaryText, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TertiaryText, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = OnSurfaceVariant)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
            focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
            cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
        ),
        modifier = modifier,
    )
}

@Composable
private fun TeleNutzVideoCard(
    video: TeleNutzVideo,
    onPlay: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(ChipBg)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.text,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            if (video.downloadProgress > 0f && video.downloadProgress < 1f) {
                LinearProgressIndicator(
                    progress = { video.downloadProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = AccentGreen,
                    trackColor = BorderColor,
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    video.chatTitle,
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (video.text.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        video.text,
                        color = OnSurface,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (video.date.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(video.date, color = TertiaryText, fontSize = 10.sp)
                }

                Spacer(Modifier.height(6.dp))

                // Card Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Bookmark toggle
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (video.isBookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (video.isBookmarked) AccentRed else OnSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Download / Cached Status
                        if (video.isDownloaded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("Cached", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        } else if (video.downloadProgress > 0f && video.downloadProgress < 1f) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = AccentGreen)
                        } else if (video.fileId > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChipBg)
                                    .clickable(onClick = onDownload)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text("Download", color = OnSurface, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(
    uiState: TeleNutzUiState,
    onUpdate: (TeleNutzUiState) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBg)) {
        val isWide = maxWidth >= 600.dp
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Telegram Auth", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))

            when (uiState.authState) {
                TelegramAuthState.None -> {
                    Text("Connecting...", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = OnSurfaceVariant, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                TelegramAuthState.WaitPhoneNumber -> {
                    Text("Enter your phone number", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.phoneInput,
                        onValueChange = { onUpdate(uiState.copy(phoneInput = it)) },
                        placeholder = { Text("+1234567890", color = TertiaryText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (uiState.phoneInput.isNotBlank()) scope.launch { TeleNutzRepository.setPhoneNumber(uiState.phoneInput) }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                            cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        ),
                        modifier = Modifier.fillMaxWidth(if (isWide) 0.5f else 1f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipBgSelected)
                            .clickable { if (uiState.phoneInput.isNotBlank()) scope.launch { TeleNutzRepository.setPhoneNumber(uiState.phoneInput) } }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) { Text("Next", color = ChipTextSelected, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(16.dp))
                    Text("- or -", color = TertiaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .clickable { scope.launch { TeleNutzRepository.requestQrCode() } }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) { Text("Login with QR Code", color = OnSurface) }
                }
                TelegramAuthState.WaitQrCode -> {
                    Text("Scan this QR code with your Telegram app", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    if (uiState.authQrUrl != null) {
                        Text(uiState.authQrUrl!!, color = TertiaryText, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("- or -", color = TertiaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Enter your phone number", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.phoneInput,
                        onValueChange = { onUpdate(uiState.copy(phoneInput = it)) },
                        placeholder = { Text("+1234567890", color = TertiaryText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (uiState.phoneInput.isNotBlank()) scope.launch { TeleNutzRepository.setPhoneNumber(uiState.phoneInput) }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                            cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        ),
                        modifier = Modifier.fillMaxWidth(if (isWide) 0.5f else 1f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipBgSelected)
                            .clickable { if (uiState.phoneInput.isNotBlank()) scope.launch { TeleNutzRepository.setPhoneNumber(uiState.phoneInput) } }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) { Text("Next", color = ChipTextSelected, fontWeight = FontWeight.Bold) }
                }
                TelegramAuthState.WaitCode -> {
                    Text("Enter the code sent to your phone", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.codeInput,
                        onValueChange = { onUpdate(uiState.copy(codeInput = it)) },
                        placeholder = { Text("12345", color = TertiaryText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (uiState.codeInput.isNotBlank()) scope.launch { TeleNutzRepository.checkAuthCode(uiState.codeInput) }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                            cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        ),
                        modifier = Modifier.fillMaxWidth(if (isWide) 0.5f else 1f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipBgSelected)
                            .clickable { if (uiState.codeInput.isNotBlank()) scope.launch { TeleNutzRepository.checkAuthCode(uiState.codeInput) } }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) { Text("Verify", color = ChipTextSelected, fontWeight = FontWeight.Bold) }
                }
                TelegramAuthState.WaitPassword -> {
                    Text("Enter your 2FA password", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.codeInput,
                        onValueChange = { onUpdate(uiState.copy(codeInput = it)) },
                        placeholder = { Text("Password", color = TertiaryText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (uiState.codeInput.isNotBlank()) scope.launch { TeleNutzRepository.checkPassword(uiState.codeInput) }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                            focusedBorderColor = BorderColor, unfocusedBorderColor = BorderColor,
                            cursorColor = OnSurface, focusedContainerColor = SurfaceCard, unfocusedContainerColor = SurfaceCard,
                        ),
                        modifier = Modifier.fillMaxWidth(if (isWide) 0.5f else 1f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChipBgSelected)
                            .clickable { if (uiState.codeInput.isNotBlank()) scope.launch { TeleNutzRepository.checkPassword(uiState.codeInput) } }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) { Text("Verify", color = ChipTextSelected, fontWeight = FontWeight.Bold) }
                }
                TelegramAuthState.Ready -> {
                    Text("Authenticated!", color = OnSurfaceVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = OnSurfaceVariant, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                TelegramAuthState.Closed -> Text("Disconnected", color = OnSurfaceVariant, fontSize = 14.sp)
            }
            if (uiState.authError != null) {
                Spacer(Modifier.height(12.dp))
                Text("Error: ${uiState.authError}", color = Color(0xFFFF4444), fontSize = 12.sp)
            }
        }
    }
}
