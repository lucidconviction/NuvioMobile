package com.nuvio.app.features.iptv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch

@Composable
fun StreamThumbnail(
    modifier: Modifier = Modifier,
    streamUrl: String,
    providedLogo: String? = null,
    providedPoster: String? = null,
    headers: Map<String, String> = emptyMap(),
    contentDescription: String? = null,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    placeholderColor: Color = Color(0xFF1A1A1A),
    onThumbnailGenerated: ((String) -> Unit)? = null,
) {
    val extractor = remember { StreamThumbnailExtractorFactory.create() }
    var extractedThumbnail by remember { mutableStateOf<String?>(null) }
    var extractionAttempted by remember { mutableStateOf(false) }

    val effectiveLogo = providedLogo?.takeIf { it.isNotBlank() }
    val effectivePoster = providedPoster?.takeIf { it.isNotBlank() }

    LaunchedEffect(streamUrl, effectiveLogo, effectivePoster) {
        if (effectiveLogo != null || effectivePoster != null) return@LaunchedEffect
        if (extractionAttempted) return@LaunchedEffect
        extractionAttempted = true

        try {
            val result = extractor.extractThumbnail(streamUrl, headers)
            if (result.thumbnailUri != null) {
                extractedThumbnail = result.thumbnailUri
                onThumbnailGenerated?.invoke(result.thumbnailUri!!)
            }
        } catch (e: Exception) {
            // Extraction failed, fall back to placeholder
        }
    }

    val imageSource = effectiveLogo ?: effectivePoster ?: extractedThumbnail
    val showPlaceholder = imageSource == null

    Box(
        modifier = modifier
            .clip(shape)
            .background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            androidx.compose.material3.Text(
                text = "▶",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageSource)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun ChannelLogo(
    modifier: Modifier = Modifier,
    channel: IptvChannel,
    headers: Map<String, String> = emptyMap(),
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    placeholderColor: Color = Color(0xFF1A1A1A),
) {
    StreamThumbnail(
        modifier = modifier,
        streamUrl = channel.url,
        providedLogo = channel.logo,
        headers = headers,
        contentDescription = channel.name,
        shape = shape,
        placeholderColor = placeholderColor,
    )
}