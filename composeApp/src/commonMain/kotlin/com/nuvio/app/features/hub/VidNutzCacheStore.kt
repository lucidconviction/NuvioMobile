package com.nuvio.app.features.hub

import kotlinx.serialization.Serializable

internal expect object VidNutzCacheStore {
    fun load(): String?
    fun save(json: String)
}

@Serializable
data class VidNutzCache(
    val categories: Map<String, CachedCategory> = emptyMap(),
    val searches: Map<String, CachedSearch> = emptyMap(),
    val hubs: Map<String, CachedHub> = emptyMap(),
)

@Serializable
data class CachedCategory(
    val videos: List<VidNutzVideo>,
    val timestamp: Long,
    val page: Int = 1,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > 30 * 60 * 1000
}

@Serializable
data class CachedSearch(
    val videos: List<VidNutzVideo>,
    val timestamp: Long,
    val page: Int = 1,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > 30 * 60 * 1000
}

@Serializable
data class CachedHub(
    val sections: Map<String, List<VidNutzVideo>>,
    val timestamp: Long,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > 30 * 60 * 1000
}