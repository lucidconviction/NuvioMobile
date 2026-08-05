package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.QuickChannel
import com.nuvio.app.features.iptv.QuickChannelList

private val SurfaceCard = Color(0xCC1F1F1F)
private val OnSurface = Color(0xFFE2E2E2)
private val Primary = Color(0xFF7C3AED)

@Composable
fun HomeQuickChannelsSection(
    modifier: Modifier = Modifier,
    sectionPadding: Dp = 0.dp,
    onChannelClick: ((IptvChannel) -> Unit)? = null,
) {
    var qcRegion by remember { mutableStateOf("All") }
    val qcTabs = listOf("All", "US", "UK", "CA", "Bay Area", "Premium", "Sports", "News")

    val filtered = remember(qcRegion) {
        QuickChannelList.all.filter { qc ->
            when (qcRegion) {
                "All" -> true; "US" -> "US" in qc.regions; "UK" -> "UK" in qc.regions
                "CA" -> "CA" in qc.regions; "Bay Area" -> "bay-area" in qc.regions
                "Premium" -> "premium" in qc.tags
                "Sports" -> "sports" in qc.tags; "News" -> "news" in qc.tags
                else -> true
            }
        }
    }

    Column(modifier = modifier.padding(horizontal = sectionPadding)) {
        Text(
            "Quick Channels",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(qcTabs) { tab ->
                val isActive = qcRegion == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) Primary else SurfaceCard)
                        .clickable { qcRegion = tab }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Text(
                        tab,
                        color = if (isActive) Color.White else OnSurface,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.displayName }) { qc ->
                QuickChannelCard(
                    qc = qc,
                    onClick = {
                        val matches = IptvRepository.getAllChannels().filter { ch ->
                            ch.name.contains(qc.displayName, ignoreCase = true) ||
                            qc.aliases.any { ch.name.contains(it, ignoreCase = true) }
                        }
                        matches.firstOrNull()?.let { onChannelClick?.invoke(it) }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickChannelCard(
    qc: QuickChannel,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    qc.displayName,
                    color = OnSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "▸ play",
                    color = Primary.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
