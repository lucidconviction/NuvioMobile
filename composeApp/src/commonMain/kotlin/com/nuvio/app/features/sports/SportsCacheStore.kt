package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import kotlinx.serialization.Serializable

internal expect object SportsCacheStore {
    fun load(): String?
    fun save(json: String)
}

/**
 * Persistent pre-cache for SportsNutz, persisted as a single JSON blob via
 * platform SharedPreferences/NSUserDefaults (mirrors VidNutzCacheStore).
 *
 * Holds:
 *  - all live events (Sports Now/Later view)
 *  - league events keyed by cacheKey ("<leagueId>_<date>")
 *  - matched channels keyed by event id
 *  - a generic timestamp map for each persisted section
 */
@Serializable
data class SportsCache(
    val allLiveEvents: List<EspnProcessedEvent> = emptyList(),
    val leagueEvents: Map<String, CachedLeagueEvents> = emptyMap(),
    val matchedChannels: Map<String, List<MatchedChannel>> = emptyMap(),
    val timestamps: Map<String, Long> = emptyMap(),
)

@Serializable
data class CachedLeagueEvents(
    val events: List<EspnProcessedEvent> = emptyList(),
    val timestamp: Long = 0,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > 10 * 60 * 1000
}
