package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.features.hub.MultiWindowStore

@Composable
fun MultiNavFAB(onClick: () -> Unit) {
    val mwCount = MultiWindowStore.allStreams.size
    if (mwCount <= 0) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 16.dp, bottom = 72.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = CircleShape,
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MW", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$mwCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}