package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Probes a portalnutz entry's Xtream endpoint with its credentials to confirm it is
 * actually live and serving a channel catalog. Returns the parsed account info when
 * the portal responds, or null when it is dead / unreachable / misconfigured.
 */
object PortalProbe {
    private val json = Json { ignoreUnknownKeys = true }
    private const val TIMEOUT_MS = 8_000L
    private const val MAX_CONCURRENCY = 8

    data class Result(
        val entry: PortalNutzEntry,
        val alive: Boolean,
        val channelCount: Int? = null,
        val error: String? = null,
        val at: Long = TraktPlatformClock.nowEpochMs(),
    )

    suspend fun probe(entry: PortalNutzEntry): Result {
        val baseUrl = entry.url.trimEnd('/')
        val creds = "username=${entry.username}&password=${entry.password}"
        val headers = mapOf("User-Agent" to "VLC/3.0.20")
        val raw = runCatching {
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(TIMEOUT_MS) {
                    httpGetTextWithHeaders("$baseUrl/player_api.php?$creds", headers)
                }
            }
        }.getOrNull()
        if (raw.isNullOrBlank()) return Result(entry, alive = false, error = "No response")
        val obj = runCatching {
            json.decodeFromString<kotlinx.serialization.json.JsonObject>(raw)
        }.getOrNull() ?: return Result(entry, alive = false, error = "Not Xtream API")
        val userInfo = obj["user_info"]?.jsonObject
        val active = userInfo?.get("active_cons")?.toString()?.toIntOrNull()
        val max = userInfo?.get("max_connections")?.toString()?.toIntOrNull()
        val exp = userInfo?.get("exp_date")?.toString()?.toLongOrNull()
        val isTrial = (active != null && max != null && active >= max)
        return Result(
            entry = entry.copy(
                activeConnections = active ?: entry.activeConnections,
                maxConnections = max ?: entry.maxConnections,
                expDate = exp ?: entry.expDate,
                isTrial = isTrial,
            ),
            alive = true,
            channelCount = obj["channels"]?.jsonObject?.size,
        )
    }

    suspend fun probeBatch(
        entries: List<PortalNutzEntry>,
        onResult: (Result) -> Unit,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Result> {
        if (entries.isEmpty()) return emptyList()
        val semaphore = Semaphore(MAX_CONCURRENCY)
        val progressMutex = kotlinx.coroutines.sync.Mutex()
        var completed = 0
        return coroutineScope {
            entries.map { entry ->
                async {
                    val r = semaphore.withPermit { probe(entry) }
                    onResult(r)
                    progressMutex.withLock {
                        completed++
                        onProgress(completed, entries.size)
                    }
                    r
                }
            }.awaitAll()
        }
    }
}