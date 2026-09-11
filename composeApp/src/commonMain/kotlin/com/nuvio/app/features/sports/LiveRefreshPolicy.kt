package com.nuvio.app.features.sports

import com.nuvio.app.features.trakt.TraktPlatformClock

/**
 * Per-data-type refresh intervals for sports data.
 *
 * Replaces the hardcoded 30s heartbeat with intelligent refresh rates.
 * Each data type has its own interval based on how frequently it changes.
 */
enum class RefreshInterval(val ms: Long, val label: String) {
    LIVE_SCORES(15_000, "Live Scores"),
    CHANNEL_MATCH(20_000, "Channel Match"),
    UPCOMING_GAMES(300_000, "Upcoming Games"),
    STANDINGS(600_000, "Standings"),
    HIGHLIGHTS(900_000, "Highlights"),
    NEWS(1_800_000, "News"),
    ENDED_GAMES(3_600_000, "Ended Games"),
}

/**
 * Tracks last-fetch timestamps per data type and determines
 * whether a refresh is needed based on elapsed time.
 */
object LiveRefreshPolicy {

    private val lastFetchTimes = mutableMapOf<RefreshInterval, Long>()

    /**
     * Returns true if the data type is stale and should be refreshed.
     */
    fun shouldRefresh(dataType: RefreshInterval): Boolean {
        val lastFetch = lastFetchTimes[dataType] ?: 0L
        return now() - lastFetch >= dataType.ms
    }

    /**
     * Returns true if the data type is stale, using a custom threshold override.
     */
    fun shouldRefresh(dataType: RefreshInterval, overrideMs: Long): Boolean {
        val lastFetch = lastFetchTimes[dataType] ?: 0L
        return now() - lastFetch >= overrideMs
    }

    /**
     * Mark a data type as freshly fetched.
     */
    fun markFetched(dataType: RefreshInterval) {
        lastFetchTimes[dataType] = now()
    }

    /**
     * Get milliseconds until the next refresh is due for a data type.
     * Returns 0 if already due.
     */
    fun msUntilNextRefresh(dataType: RefreshInterval): Long {
        val lastFetch = lastFetchTimes[dataType] ?: 0L
        val elapsed = now() - lastFetch
        return (dataType.ms - elapsed).coerceAtLeast(0)
    }

    /**
     * Reset all timestamps (e.g., on logout or full reload).
     */
    fun reset() {
        lastFetchTimes.clear()
    }

    /**
     * Reset timestamps for a specific data type.
     */
    fun reset(dataType: RefreshInterval) {
        lastFetchTimes.remove(dataType)
    }

    private fun now(): Long = TraktPlatformClock.nowEpochMs()
}
