package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object StreamValidator {

    private const val TIMEOUT_MS = 5_000
    private const val BATCH_SIZE = 20

    suspend fun validateUrls(
        urls: List<String>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Set<String> = withContext(Dispatchers.Default) {
        val dead = mutableSetOf<String>()
        var completed = 0
        val total = urls.size
        coroutineScope {
            urls.chunked(BATCH_SIZE).forEach { batch ->
                batch.map { url ->
                    async {
                        if (!isUrlReachable(url)) {
                            synchronized(dead) { dead.add(url) }
                        }
                        synchronized(this@withContext) {
                            completed++
                            onProgress(completed, total)
                        }
                    }
                }.awaitAll()
            }
        }
        dead
    }

    private suspend fun isUrlReachable(url: String): Boolean {
        return runCatching {
            httpGetTextWithHeaders(
                url = url,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
                    "Range" to "bytes=0-0",
                ),
            )
            true
        }.getOrDefault(false)
    }
}
