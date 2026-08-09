package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var selectedQc by remember { mutableStateOf<QuickChannel?>(null) }
    var qcMatches by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }

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
                            QuickChannelList.matches(qc, ch)
                        }
                        qcMatches = matches
                        selectedQc = qc
                    },
                )
            }
        }
    }

    selectedQc?.let { qc ->
        ModalBottomSheet(
            onDismissRequest = { selectedQc = null; qcMatches = emptyList() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1C1B1B),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().heightIn(max = 500.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(qc.displayName, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    Text("${qcMatches.size} source${if (qcMatches.size != 1) "s" else ""}", color = OnSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                if (qcMatches.isEmpty()) {
                    Text("No sources found", color = OnSurface.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qcMatches) { ch ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2A2A2A))
                                    .clickable { onChannelClick?.invoke(ch); selectedQc = null; qcMatches = emptyList() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!ch.logo.isNullOrBlank()) {
                                    coil3.compose.AsyncImage(
                                        model = ch.logo, contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1F1F1F)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ch.name, color = OnSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (ch.group != null) {
                                        Text(ch.group, color = OnSurface.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Play", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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
