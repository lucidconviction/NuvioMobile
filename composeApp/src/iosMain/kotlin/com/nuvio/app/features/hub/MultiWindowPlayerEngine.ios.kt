package com.nuvio.app.features.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

actual object MultiWindowPlayerManager {
    actual fun createPlayer(sourceUrl: String, headers: Map<String, String>): PlayerHandle = PlayerHandle(0)
    actual fun setVolume(handle: PlayerHandle, volume: Float) {}
    actual fun setAudioFocus(handleId: Int) {}
    actual fun pausePlayer(handle: PlayerHandle) {}
    actual fun resumePlayer(handle: PlayerHandle) {}
    actual fun releasePlayer(handle: PlayerHandle) {}
    actual fun releaseAll() {}
}

@Composable
actual fun MultiWindowVideoSurface(handle: PlayerHandle?, modifier: Modifier, resizeMode: Int) {
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text("MW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    }
}
