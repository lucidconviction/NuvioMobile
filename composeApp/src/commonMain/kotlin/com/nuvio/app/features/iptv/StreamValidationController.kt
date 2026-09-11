package com.nuvio.app.features.iptv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class SourceScanState(
    val active: Boolean,
    val total: Int,
    val checked: Int,
    val alive: Int,
    val dead: Int,
)

object StreamValidationController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var scans by mutableStateOf<Map<String, SourceScanState>>(emptyMap())
        private set

    fun scanSource(sourceId: String, channels: List<IptvChannel>, force: Boolean = false) {
        val urls = channels.map { it.url }.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty()) return
        scope.launch {
            StreamValidationStore.ensureLoaded()
            val toCheck = if (force) urls
            else urls.filter { StreamValidationStore.needsRecheck(it) }
            val total = toCheck.size
            if (total == 0) {
                scans = scans + (sourceId to SourceScanState(active = false, total = urls.size, checked = urls.size, alive = urls.size, dead = 0))
                return@launch
            }
            scans = scans + (sourceId to SourceScanState(active = true, total = total, checked = 0, alive = 0, dead = 0))
            val results = mutableMapOf<String, Boolean>()
            var checked = 0
            var aliveCount = 0
            var deadCount = 0
            StreamValidator.checkBatch(toCheck, onResult = { url, alive ->
                StreamValidationStore.updateStatus(url, alive)
                synchronized(results) { results[url] = alive }
                checked++
                if (alive) aliveCount++ else deadCount++
                if (checked % 5 == 0 || checked == total) {
                    scans = scans + (sourceId to SourceScanState(active = true, total = total, checked = checked, alive = aliveCount, dead = deadCount))
                }
            })
            if (results.isNotEmpty()) StreamValidationStore.remember(results)
            scans = scans + (sourceId to SourceScanState(active = false, total = total, checked = checked, alive = aliveCount, dead = deadCount))
        }
    }

    fun scanAll(sources: List<Pair<String, List<IptvChannel>>>, force: Boolean = false) {
        sources.forEach { (id, channels) -> scanSource(id, channels, force) }
    }

    suspend fun resolveCachedStatuses(
        matches: List<IptvChannel>,
        onUpdate: (Map<String, Boolean>) -> Unit,
    ) {
        StreamValidationStore.ensureLoadedSuspend()
        onUpdate(StreamValidationStore.getAliveMap())
    }

    suspend fun resolveChannelsStatuses(
        matches: List<IptvChannel>,
        onUpdate: (Map<String, Boolean>) -> Unit,
    ) {
        StreamValidationStore.ensureLoadedSuspend()
        val allUrls = matches.map { it.url }.filter { it.isNotBlank() }.distinct()
        val cachedMap = StreamValidationStore.getAliveMap()
        val urlsToCheck = allUrls.filter { StreamValidationStore.needsRecheck(it) }
        if (urlsToCheck.isEmpty()) {
            onUpdate(allUrls.associateWith { cachedMap[it] ?: false })
            return
        }
        val results = mutableMapOf<String, Boolean>()
        StreamValidator.checkBatch(urlsToCheck, onResult = { url, alive ->
            StreamValidationStore.updateStatus(url, alive)
            synchronized(results) { results[url] = alive }
        })
        if (results.isNotEmpty()) StreamValidationStore.remember(results)
        onUpdate(allUrls.associateWith { cachedMap[it] ?: results[it] ?: false })
    }
}