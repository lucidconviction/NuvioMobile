package com.nuvio.app.features.hub

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

actual object MultiWindowPlayerManager {
    internal val players = mutableMapOf<Int, ExoPlayer>()
    private var nextId = 1
    private const val MAX_PLAYERS = 9

    private lateinit var appContext: android.content.Context

    fun initialize(context: android.content.Context) {
        appContext = context.applicationContext
    }

    actual fun createPlayer(sourceUrl: String, headers: Map<String, String>, onCompletion: (() -> Unit)?): PlayerHandle {
        if (players.size >= MAX_PLAYERS) {
            val oldest = players.keys.first()
            releasePlayer(PlayerHandle(oldest))
        }
        val id = nextId++
        val isIptvStream = sourceUrl.contains("get.php") ||
            sourceUrl.contains("playlist.m3u8") ||
            sourceUrl.contains("chunklist") ||
            sourceUrl.matches(Regex(".*:\\d{4,5}/.*")) ||
            sourceUrl.contains("live.ts") ||
            sourceUrl.contains("stream.ts") ||
            sourceUrl.endsWith(".m3u8", ignoreCase = true)
        val loadControl = if (isIptvStream) {
            DefaultLoadControl.Builder()
                .setTargetBufferBytes(50 * 1024 * 1024)
                .setBufferDurationsMs(10_000, 60_000, 3_000, 5_000)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build()
        } else {
            DefaultLoadControl()
        }
        val player = ExoPlayer.Builder(appContext)
            .setLoadControl(loadControl)
            .build().apply {
            val mediaItem = MediaItem.Builder().setUri(sourceUrl).build()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            if (onCompletion != null) {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onCompletion()
                        }
                    }
                })
            }
        }
        players[id] = player
        return PlayerHandle(id)
    }

    actual fun setVolume(handle: PlayerHandle, volume: Float) {
        players[handle.id]?.volume = volume.coerceIn(0f, 1f)
    }

    private val savedVolumes = mutableMapOf<Int, Float>()

    actual fun setAudioFocus(handleId: Int) {
        players.forEach { (pid, player) ->
            if (pid == handleId) {
                player.volume = savedVolumes.remove(pid) ?: 1f
            } else {
                savedVolumes.putIfAbsent(pid, player.volume)
                player.volume = 0f
            }
        }
    }

    actual fun pausePlayer(handle: PlayerHandle) {
        players[handle.id]?.pause()
    }

    actual fun resumePlayer(handle: PlayerHandle) {
        players[handle.id]?.play()
    }

    actual fun releasePlayer(handle: PlayerHandle) {
        players.remove(handle.id)?.run { stop(); release() }
    }

    actual fun releaseAll() {
        players.values.forEach { it.stop(); it.release() }
        players.clear()
    }
}

private fun mapResizeMode(mode: Int): Int {
    return when (mode) {
        RESIZE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        RESIZE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        RESIZE_FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
        RESIZE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
}

@Composable
actual fun MultiWindowVideoSurface(handle: PlayerHandle?, modifier: Modifier, resizeMode: Int) {
    val rm = mapResizeMode(resizeMode)
    if (handle == null) return
    val player = remember(handle.id) { MultiWindowPlayerManager.players[handle.id] }
    key(handle.id) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    this.resizeMode = rm
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view -> view.resizeMode = rm },
            modifier = modifier,
        )
    }
}
