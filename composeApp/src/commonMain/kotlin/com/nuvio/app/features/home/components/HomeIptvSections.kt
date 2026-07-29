package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.iptv.IptvChannel

private val SurfaceCard = Color(0xCC1F1F1F)
private val OnSurface = Color(0xFFE2E2E2)
private val OnSurfaceVariant = Color(0xFFC4C7C8)
private val TertiaryText = Color(0xFF888888)

@Composable
fun HomeIptvHistorySection(
    channels: List<IptvChannel>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp = 0.dp,
    onChannelClick: ((IptvChannel) -> Unit)? = null,
) {
    if (channels.isEmpty()) return
    Column(modifier = modifier.padding(horizontal = sectionPadding)) {
        Text(
            "IPTV Channel History",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(channels.take(20), key = { "${it.sourceId}_${it.id}" }) { channel ->
                IptvMiniCard(
                    channel = channel,
                    onClick = { onChannelClick?.invoke(channel) },
                )
            }
        }
    }
}

@Composable
fun HomeIptvFavoritesSection(
    channels: List<IptvChannel>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp = 0.dp,
    onChannelClick: ((IptvChannel) -> Unit)? = null,
) {
    if (channels.isEmpty()) return
    Column(modifier = modifier.padding(horizontal = sectionPadding)) {
        Text(
            "IPTV Favorite Channels",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(channels.take(20), key = { "${it.sourceId}_${it.id}" }) { channel ->
                IptvMiniCard(
                    channel = channel,
                    onClick = { onChannelClick?.invoke(channel) },
                )
            }
        }
    }
}

@Composable
private fun IptvMiniCard(
    channel: IptvChannel,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            if (!channel.logo.isNullOrBlank()) {
                coil3.compose.AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )
            } else {
                Text(
                    channel.name.take(2).uppercase(),
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            channel.name,
            color = OnSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!channel.group.isNullOrBlank()) {
            Text(
                channel.group!!.take(20),
                color = TertiaryText,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
