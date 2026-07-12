package com.nuvio.app.features.hub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class PlayerHandle(val id: Int)

const val RESIZE_FILL = 0
const val RESIZE_FIT = 1
const val RESIZE_FIXED_WIDTH = 2
const val RESIZE_FIXED_HEIGHT = 3
const val RESIZE_ZOOM = 4

expect object MultiWindowPlayerManager {
    fun createPlayer(sourceUrl: String, headers: Map<String, String>): PlayerHandle
    fun setVolume(handle: PlayerHandle, volume: Float)
    fun setAudioFocus(handleId: Int)
    fun pausePlayer(handle: PlayerHandle)
    fun resumePlayer(handle: PlayerHandle)
    fun releasePlayer(handle: PlayerHandle)
    fun releaseAll()
}

@Composable
expect fun MultiWindowVideoSurface(handle: PlayerHandle?, modifier: Modifier = Modifier, resizeMode: Int = RESIZE_FILL)
