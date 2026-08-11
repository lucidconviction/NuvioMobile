package com.nuvio.app.features.iptv

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ShortEpgCache {

    private const val TTL_MS = 5 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val entries = mutableMapOf<String, CacheEntry>()

    private data class CacheEntry(
        var fetchedAtMs: Long,
        var job: Job,
        var deferred: CompletableDeferred<List<EpgProgram>>,
    )

    fun clear() {
        scope.launch {
            mutex.withLock { entries.clear() }
        }
    }

    private fun keyFor(account: XtreamAccount, channel: IptvChannel, limit: Int): String =
        "${account.server}|${channel.id}|$limit"

    suspend fun getOrLoad(
        account: XtreamAccount,
        channel: IptvChannel,
        limit: Int = 2,
    ): List<EpgProgram> {
        val key = keyFor(account, channel, limit)

        val cached = mutex.withLock {
            val now = System.currentTimeMillis()
            val existing = entries[key]
            if (existing != null && (now - existing.fetchedAtMs < TTL_MS || !existing.deferred.isCompleted)) {
                existing.deferred
            } else {
                val deferred = CompletableDeferred<List<EpgProgram>>()
                val freshJob = scope.launch(Dispatchers.Default) {
                    val programs = try {
                        ShortEpgClient.fetchShortEpg(account, channel, limit)
                    } catch (_: Throwable) {
                        emptyList()
                    }
                    deferred.complete(programs)
                    mutex.withLock {
                        entries[key]?.takeIf { it.deferred === deferred }?.fetchedAtMs = System.currentTimeMillis()
                    }
                }
                entries[key] = CacheEntry(System.currentTimeMillis(), freshJob, deferred)
                deferred
            }
        }

        return cached.await()
    }
}