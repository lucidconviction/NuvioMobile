package com.nuvio.app.features.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.layout.onSizeChanged
import co.touchlab.kermit.Logger
import com.nuvio.app.features.player.NuvioPlayerBridgeFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents

actual object MultiWindowPlayerManager {
    private val bridges = mutableMapOf<Int, com.nuvio.app.features.player.NuvioPlayerBridge>()
    private val completionCallbacks = mutableMapOf<Int, (() -> Unit)?>()
    private var nextId = 1
    private const val MAX_PLAYERS = 9

    actual fun createPlayer(sourceUrl: String, headers: Map<String, String>, onCompletion: (() -> Unit)?): PlayerHandle {
        if (bridges.size >= MAX_PLAYERS) {
            val oldest = bridges.keys.first()
            bridges[oldest]?.destroy()
            bridges.remove(oldest)
            completionCallbacks.remove(oldest)
        }
        val id = nextId++
        val bridge = NuvioPlayerBridgeFactory.create()
            ?: run { Logger.w(TAG) { "MultiWindow: no bridge available for id=$id, url=$sourceUrl" }; return PlayerHandle(-1) }
        bridges[id] = bridge
        completionCallbacks[id] = onCompletion
        bridge.loadFile(sourceUrl)
        bridge.play()
        return PlayerHandle(id)
    }

    actual fun setVolume(handle: PlayerHandle, volume: Float) {
        bridges[handle.id]?.setMuted(volume <= 0.001f)
    }

    actual fun setAudioFocus(handleId: Int) {
        bridges.forEach { (id, bridge) ->
            bridge.setMuted(id != handleId)
        }
    }

    actual fun pausePlayer(handle: PlayerHandle) {
        bridges[handle.id]?.pause()
    }

    actual fun resumePlayer(handle: PlayerHandle) {
        bridges[handle.id]?.play()
    }

    actual fun releasePlayer(handle: PlayerHandle) {
        bridges.remove(handle.id)?.destroy()
        completionCallbacks.remove(handle.id)
    }

    actual fun releaseAll() {
        bridges.values.forEach { it.destroy() }
        bridges.clear()
        completionCallbacks.clear()
    }
}

private const val TAG = "NuvioMultiWindow"

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MultiWindowVideoSurface(handle: PlayerHandle?, modifier: Modifier, resizeMode: Int) {
    if (handle == null) return
    val bridge = remember(handle.id) { MultiWindowPlayerManager.bridges[handle.id] }
    if (bridge == null) return
    LaunchedEffect(handle.id, resizeMode) {
        val mpvMode = when (resizeMode) {
            RESIZE_FIT -> 0
            RESIZE_ZOOM -> 2
            else -> 1 // RESIZE_FILL, RESIZE_FIXED_WIDTH, RESIZE_FIXED_HEIGHT
        }
        bridge.setResizeMode(mpvMode)
    }
    UIKitViewController(
        factory = { bridge.createPlayerViewController() },
        modifier = modifier.onSizeChanged { s ->
            bridge.syncVideoSurfaceLayout(width = s.width.toDouble(), height = s.height.toDouble())
        },
        onResize = { vc, rect ->
            rect.useContents {
                bridge.syncVideoSurfaceLayout(width = size.width, height = size.height)
            }
        },
        interactive = false,
    )
    // Poll for end-of-stream to trigger completion callback
    LaunchedEffect(handle.id) {
        while (true) {
            kotlinx.coroutines.delay(500L)
            if (!kotlinx.coroutines.isActive) break
            if (bridge.getIsEnded()) {
                val cb = MultiWindowPlayerManager.completionCallbacks[handle.id]
                if (cb != null) cb()
                break
            }
        }
    }
}
