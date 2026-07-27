package com.nuvio.app.features.iptv

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object StreamValidationStore {
    private const val TAG = "StreamValidation"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getDeadUrls(): Set<String> {
        val raw = IptvStorage.loadDeadUrls() ?: return emptySet()
        return try {
            json.decodeFromString<List<String>>(raw).toSet()
        } catch (_: Exception) { emptySet() }
    }

    suspend fun markDead(urls: Set<String>) {
        val current = getDeadUrls()
        val updated = current + urls
        IptvStorage.saveDeadUrls(json.encodeToString(updated.toList()))
    }

    suspend fun markAlive(url: String) {
        val current = getDeadUrls()
        if (url !in current) return
        val updated = current - url
        IptvStorage.saveDeadUrls(json.encodeToString(updated.toList()))
    }

    suspend fun clearAll() {
        IptvStorage.saveDeadUrls(json.encodeToString(emptyList<String>()))
    }
}
