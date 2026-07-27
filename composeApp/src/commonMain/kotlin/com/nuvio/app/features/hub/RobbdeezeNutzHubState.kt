package com.nuvio.app.features.hub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.StreamValidationStore
import com.nuvio.app.features.iptv.StreamValidator
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.sports.YouTubeStreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RobbdeezeNutzHubState {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var playerLoadingMessage by mutableStateOf<String?>(null)
    var validationProgress by mutableStateOf<String?>(null)
    var deadUrls by mutableStateOf<Set<String>>(emptySet())
    var showDeadStreams by mutableStateOf(false)
    var allIptvChannels by mutableStateOf<List<IptvChannel>>(emptyList())

    private var pendingValidation = false

    fun toggleShowDeadStreams() {
        showDeadStreams = !showDeadStreams
    }

    fun loadDeadUrls() {
        scope.launch {
            deadUrls = StreamValidationStore.getDeadUrls()
        }
    }

    fun requestValidation() {
        pendingValidation = true
    }

    fun runPendingValidation(channels: List<IptvChannel>) {
        if (!pendingValidation) return
        pendingValidation = false
        scope.launch {
            val urls = channels.map { it.url }.filter { it.isNotBlank() }
            if (urls.isEmpty()) return@launch
            validationProgress = "Validating 0/${urls.size}..."
            val dead = StreamValidator.validateUrls(urls) { done, total ->
                validationProgress = "Validating $done/$total..."
            }
            if (dead.isNotEmpty()) {
                StreamValidationStore.markDead(dead)
            }
            deadUrls = dead
            validationProgress = if (dead.isEmpty()) "All ${urls.size} streams working!" else "Found ${dead.size} dead of ${urls.size}"
            delay(5000)
            validationProgress = null
        }
    }

    fun playVideo(
        videoId: String,
        title: String,
        thumbnail: String?,
        onPlayChannel: (PlayerLaunch) -> Unit,
    ) {
        scope.launch {
            playerLoadingMessage = "Preparing..."
            try {
                val result = YouTubeStreamResolver.resolveStreamResult(videoId)
                if (result != null) {
                    val launch = PlayerLaunch(
                        profileId = 0,
                        title = title,
                        sourceUrl = result.videoUrl,
                        sourceAudioUrl = result.audioUrl,
                        streamTitle = title,
                        providerName = "VidNutz",
                        parentMetaId = videoId,
                        parentMetaType = "video",
                        logo = thumbnail,
                        poster = thumbnail,
                        videoId = videoId,
                    )
                    onPlayChannel(launch)
                }
            } catch (_: Exception) { }
            playerLoadingMessage = null
        }
    }

    fun refreshIptvSource(name: String, url: String, type: String) {
        scope.launch {
            val existing = IptvRepository.getAllSourceIds()
            IptvRepository.invalidateChannelCache(url)
            val playlistId = existing.firstOrNull { id ->
                IptvRepository.getAllSourceNames().zip(existing).any { (n, i) -> i == id && n == name }
            }
            if (playlistId != null) {
                if (type == "m3u") IptvRepository.refreshM3uChannels(playlistId)
            }
        }
    }

    fun loadAllIptvChannelsForQuick() {
        scope.launch {
            allIptvChannels = IptvRepository.getAllChannels()
        }
    }
}
