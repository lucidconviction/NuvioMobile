package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetTextChunked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object StreamValidator {

    const val TTL_MS = 30 * 60 * 1000L

    private const val TIMEOUT_MS = 3_000L
    private const val MAX_CONCURRENCY = 24
    private const val SAMPLE_SIZE = 1024
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"

    private val htmlPagePrefixes = listOf(
        "<!doctype html", "<!doctype", "<html", "<!", "<?xml",
    )
    private val deadPageTokens = listOf(
        "404", "403", "401", "not found", "forbidden", "unauthorized",
        "invalid user", "invalid username", "account disabled", "account suspended",
        "credits expired", "expired", "offline", "service unavailable",
        "bad gateway", "internal server error", "error", "failed to open stream",
    )

    suspend fun checkUrl(url: String): Boolean {
        var sample = probeSample(url, "bytes=0-${SAMPLE_SIZE - 1}")
        if (sample.isNullOrBlank()) {
            sample = probeSample(url, null)
        }
        return sample?.let { looksAlive(it) } ?: false
    }

    suspend fun checkBatch(
        urls: List<String>,
        onResult: suspend (String, Boolean) -> Unit,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        if (urls.isEmpty()) return
        val semaphore = Semaphore(MAX_CONCURRENCY)
        val progressMutex = Mutex()
        var completed = 0
        coroutineScope {
            urls.map { url ->
                async {
                    val alive = semaphore.withPermit { checkUrl(url) }
                    onResult(url, alive)
                    progressMutex.withLock {
                        completed++
                        onProgress(completed, urls.size)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun validateUrls(
        urls: List<String>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Set<String> {
        val dead = mutableSetOf<String>()
        checkBatch(urls, onResult = { url, alive ->
            StreamValidationStore.updateStatus(url, alive)
            if (!alive) dead.add(url)
        }, onProgress = onProgress)
        return dead
    }

    private suspend fun probeSample(url: String, range: String?): String? {
        val headers = buildList {
            add("User-Agent" to USER_AGENT)
            if (range != null) add("Range" to range)
        }
        return runCatching {
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(TIMEOUT_MS) {
                    val sb = StringBuilder()
                    httpGetTextChunked(url, headers.toMap()) { chunk ->
                        if (sb.length < SAMPLE_SIZE) sb.append(chunk)
                        sb.length < SAMPLE_SIZE
                    }
                    sb.toString()
                }
            }
        }.getOrNull()
    }

    private fun looksAlive(sample: String): Boolean {
        val trimmed = sample.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (trimmed.isEmpty()) return false
        val lower = trimmed.lowercase()
        if (htmlPagePrefixes.any { lower.startsWith(it) }) return false
        if (lower.length < 256 && deadPageTokens.any { trimmed.contains(it, ignoreCase = true) }) return false
        return true
    }
}