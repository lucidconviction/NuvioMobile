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
            StreamValidator.checkBatch(toCheck, onResult = { url, alive ->
                StreamValidationStore.updateStatus(url, alive)
                synchronized(results) { results[url] = alive }
                val prev = scans[sourceId]
                if (prev != null) {
                    scans = scans + (sourceId to prev.copy(
                        checked = prev.checked + 1,
                        alive = prev.alive + (if (alive) 1 else 0),
                        dead = prev.dead + (if (alive) 0 else 1),
                    ))
                }
            })
            if (results.isNotEmpty()) StreamValidationStore.remember(results)
            val final = scans[sourceId]
            if (final != null) {
                scans = scans + (sourceId to final.copy(active = false))
            }
        }
    }

    fun scanAll(sources: List<Pair<String, List<IptvChannel>>>, force: Boolean = false) {
        sources.forEach { (id, channels) -> scanSource(id, channels, force) }
    }

    suspend fun resolveChannelsStatuses(
        matches: List<IptvChannel>,
        onUpdate: (Map<String, Boolean>) -> Unit,
    ) {
        StreamValidationStore.ensureLoaded()
        val initial = StreamValidationStore.getAliveMap()
        onUpdate(initial)
        val urlsToCheck = matches.map { it.url }
            .filter { it.isNotBlank() && StreamValidationStore.needsRecheck(it) }
            .distinct()
        if (urlsToCheck.isEmpty()) return
        val results = mutableMapOf<String, Boolean>()
        StreamValidator.checkBatch(urlsToCheck, onResult = { url, alive ->
            StreamValidationStore.updateStatus(url, alive)
            synchronized(results) { results[url] = alive }
            onUpdate(initial + results)
        })
        if (results.isNotEmpty()) StreamValidationStore.remember(results)
    }
}