package com.nuvio.app.features.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import kotlinx.coroutines.launch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.app.features.hub.HubReturnStore
import com.nuvio.app.features.player.openInWebViewPlayerLaunch

private val SurfaceBg = Color(0xFF131313)
private val surfaceContainer = Color(0xFF1F1F1F)
private val onSurface = Color(0xFFE2E2E2)
private val onSurfaceVariant = Color(0xFFC4C7C8)
private val primary = Color(0xFFFDFDFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XxxScreen(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<XxxVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<XxxCategory?>(null) }
    var categories by remember { mutableStateOf<List<XxxCategory>>(emptyList()) }
    var showLoading by remember { mutableStateOf(false) }
    fun loadVideos(query: String, category: XxxCategory? = null) {
        scope.launch {
            showLoading = true
            isLoading = true
            if (query.isNotBlank()) {
                videos = XxxClient.searchVideos(query)
            } else if (category != null) {
                videos = XxxClient.searchVideos(category.slug)
            } else {
                videos = XxxClient.getLatest()
            }
            isLoading = false
            showLoading = false
        }
    }

    LaunchedEffect(Unit) {
        categories = XxxClient.getCategories()
        selectedCategory = categories.firstOrNull()
        loadVideos("", selectedCategory)
    }

    LaunchedEffect(selectedCategory) {
            if (selectedCategory != null && searchQuery.isEmpty()) {
                loadVideos("", selectedCategory)
            } else if (searchQuery.isNotBlank()) {
                loadVideos(searchQuery)
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize().background(SurfaceBg),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("XXX", color = primary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { HubReturnStore.xxxScreen = "" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBg.copy(alpha = 0.9f),
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().background(SurfaceBg),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    if (q.length >= 3) {
                        selectedCategory = null
                        loadVideos(q)
                    }
                },
                placeholder = { Text("Search adult videos...", color = onSurfaceVariant.copy(alpha = 0.5f), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = onSurface,
                    unfocusedTextColor = onSurface,
                    focusedBorderColor = primary,
                    unfocusedBorderColor = onSurfaceVariant.copy(alpha = 0.3f),
                    cursorColor = primary,
                    focusedContainerColor = surfaceContainer,
                    unfocusedContainerColor = surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(8.dp))

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.take(15).forEach { cat ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                .background(if (selectedCategory == cat) primary.copy(alpha = 0.2f) else surfaceContainer)
                                .clickable { selectedCategory = cat; loadVideos("", cat) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(cat.name, color = if (selectedCategory == cat) primary else onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (isLoading || showLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            } else if (videos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos found", color = onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(videos, key = { it.id }) { video ->
                        XxxVideoCard(
                            video = video,
                            onPlay = {
                                if (video.embedUrl.isNotBlank()) {
                                    openInWebViewPlayerLaunch(video.embedUrl, video.title)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XxxVideoCard(
    video: XxxVideo,
    onPlay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = surfaceContainer),
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(video.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
            )
            if (video.duration.isNotBlank()) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(video.duration, color = primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFFE91E63).copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("XXX", color = primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = video.title,
                color = onSurface,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (video.rating > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2605", color = Color(0xFFFFD700), fontSize = 10.sp)
                    Spacer(Modifier.width(2.dp))
                    Text("${video.rating.toInt()}/5", color = onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}
