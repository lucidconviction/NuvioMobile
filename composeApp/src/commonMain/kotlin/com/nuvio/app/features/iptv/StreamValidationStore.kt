package com.nuvio.app.features.iptv

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class StreamCheckRecord(
    val url: String,
    val alive: Boolean,
    val at: Long,
)

object StreamValidationStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val DEAD_COOLDOWN_MS = 10 * 60 * 1000L

    private val cache = mutableMapOf<String, StreamCheckRecord>()
    private val lock = Any()
    private var loaded = false
    private var preloadScheduled = false

    /**
     * Compose-observable snapshot of url -> alive status, kept in sync with the cache and
     * updated live as per-URL validation results stream in. Read keys directly in composables.
     */
    val statuses = mutableStateMapOf<String, Boolean>()

    private fun snapshot() {
        synchronized(lock) {
            statuses.clear()
            cache.forEach { (url, record) -> statuses[url] = record.alive }
        }
    }

    private var preloadJob: kotlinx.coroutines.Job? = null

    fun preload() {
        if (loaded || preloadScheduled) return
        preloadScheduled = true
        preloadJob = backgroundScope.launch { doLoad() }
    }

    suspend fun ensureLoadedSuspend() {
        if (loaded) return
        preload()
        preloadJob?.join()
    }

    private suspend fun doLoad() {
        val raw = kotlinx.coroutines.withContext(Dispatchers.IO) {
            IptvStorage.loadDeadUrls()
        } ?: return
        synchronized(lock) {
            try {
                json.decodeFromString<List<StreamCheckRecord>>(raw).forEach { record ->
                    cache[record.url] = record
                }
            } catch (_: Exception) {
                val now = System.currentTimeMillis()
                json.decodeFromString<List<String>>(raw).forEach { url ->
                    cache[url] = StreamCheckRecord(url, alive = false, at = now)
                }
            }
            loaded = true
        }
        snapshot()
    }

    fun ensureLoaded() {
        if (loaded) return
        preload()
    }

    /** Records a single result for live UI updates without triggering a disk write per probe. */
    fun updateStatus(url: String, alive: Boolean) {
        statuses[url] = alive
    }

    fun isKnownDeadSync(url: String): Boolean {
        ensureLoaded()
        return cache[url]?.let { !it.alive } ?: false
    }

    fun isKnownAliveSync(url: String): Boolean {
        ensureLoaded()
        val record = cache[url] ?: return false
        return record.alive && System.currentTimeMillis() - record.at <= StreamValidator.TTL_MS
    }

    /**
     * True when this URL deserves a fresh probe: never seen, an alive result older than the
     * TTL, or a dead result older than the dead cooldown. Freshly-failed streams are skipped
     * so repeat scans don't waste time re-hammering dead endpoints.
     */
    fun needsRecheck(url: String): Boolean {
        ensureLoaded()
        val record = cache[url] ?: return true
        val age = System.currentTimeMillis() - record.at
        return age >= if (record.alive) StreamValidator.TTL_MS else DEAD_COOLDOWN_MS
    }

    fun remember(results: Map<String, Boolean>) {
        ensureLoaded()
        if (results.isEmpty()) return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            results.forEach { (url, alive) ->
                cache[url] = StreamCheckRecord(url, alive, now)
            }
        }
        snapshot()
        persist()
    }

    fun rememberAll(results: Collection<Pair<String, Boolean>>) = remember(results.toMap())

    fun getDeadUrls(): Set<String> {
        ensureLoaded()
        return cache.values.filter { !it.alive }.map { it.url }.toSet()
    }

    fun getAliveMap(): Map<String, Boolean> {
        ensureLoaded()
        return cache.values.associate { it.url to it.alive }
    }

    fun markDead(urls: Set<String>) = remember(urls.associateWith { false })

    fun markAlive(url: String) = remember(mapOf(url to true))

    fun clearAll() {
        synchronized(lock) { cache.clear() }
        statuses.clear()
        persist()
    }

    private fun persist() {
        runCatching {
            IptvStorage.saveDeadUrls(json.encodeToString(cache.values.toList()))
        }
    }
}