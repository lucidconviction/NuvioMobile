package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository

/**
 * Advanced channel matching pipeline with region awareness and source preferences.
 *
 * This is the high-level entry point that orchestrates the full matching pipeline:
 *  1. Fast name/league score (via [ChannelScorer])
 *  2. Region boost/penalty (via [SportsBroadcastRegionPolicy])
 *  3. Source ranking + user preferences (via [SportsChannelMatchPolicy])
 *  4. Lazy EPG enrichment for top candidates
 *  5. Region-filtered output
 *
 * Replaces direct calls to [GameToChannelMatcher.matchChannels] for new code.
 * The old matcher remains for backward compatibility.
 */
object AdvancedChannelMatcher {

    /**
     * Two-pass matching with region awareness.
     *
     * Pass 1: Fast name/league scoring without EPG
     * Pass 2: Lazy EPG fetch for top [cap] candidates, re-score with full pipeline
     *
     * @param event The sports event to match against
     * @param channels All available IPTV channels
     * @param userRegion User's broadcast region for filtering
     * @param prefs User's channel preferences (providers, source types)
     * @param currentEpgTitleFor Fast EPG lookup (pre-fetched)
     * @param cap Max channels to fetch EPG for (performance limit)
     * @return Sorted list of matched channels with scores and reasons
     */
    suspend fun matchChannels(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion = BroadcastRegion.OTHER,
        prefs: MatchPrefs = MatchPrefs(),
        sourceName: String = "",
        currentEpgTitleFor: (String) -> String = { "" },
        cap: Int = 12,
    ): List<MatchedChannel> {
        val target = MatchTarget(event)

        // Pass 1: Fast scoring without EPG
        val fastScores = ChannelScorer.scoreChannels(target, channels, currentEpgTitleFor)

        // Pre-filter to positive-score candidates
        val candidates = fastScores.filter { it.score > 0 }.map { it.channel }

        // Pass 2: Lazy EPG for top candidates
        val lazyLookup = IptvRepository.buildLazyEpgTitleLookup(candidates, cap)
        val combined: (String) -> String = { name ->
            val lazy = lazyLookup(name)
            if (lazy.isNotEmpty()) lazy else currentEpgTitleFor(name)
        }

        // Full scoring pipeline: base → region → source → preferences
        val fullScores = ChannelScorer.scoreChannelsWithRegion(
            target, channels, userRegion, prefs, combined
        )

        return fullScores.mapToMatchedChannels(sourceName)
    }

    /**
     * Region-filtered matching for when you want only channels from a specific region.
     * Falls back to all channels if too few region-matched results.
     */
    suspend fun matchChannelsRegionFiltered(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion,
        prefs: MatchPrefs = MatchPrefs(),
        sourceName: String = "",
        currentEpgTitleFor: (String) -> String = { "" },
        cap: Int = 12,
        minRegionResults: Int = 3,
    ): List<MatchedChannel> {
        val allMatches = matchChannels(
            event, channels, userRegion, prefs, sourceName, currentEpgTitleFor, cap
        )

        // Apply region filter
        return SportsBroadcastRegionPolicy.filterByRegion(
            allMatches.map { it.toChannelScore() },
            userRegion,
            minRegionResults
        ).mapToMatchedChannels(sourceName)
    }

    /**
     * Batch matching for multiple events.
     * Returns a map of eventId → matched channels, with region awareness applied.
     */
    suspend fun matchChannelsBatch(
        events: List<EspnProcessedEvent>,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion = BroadcastRegion.OTHER,
        prefs: MatchPrefs = MatchPrefs(),
        sourceName: String = "",
        currentEpgTitleFor: (String) -> String = { "" },
        cap: Int = 8,
    ): Map<String, List<MatchedChannel>> {
        return events.associate { event ->
            event.id to matchChannels(
                event, channels, userRegion, prefs, sourceName, currentEpgTitleFor, cap
            )
        }
    }

    /**
     * Get the best match (autoplay candidate) for an event.
     * Returns null if no channel scores high enough for autoplay.
     */
    suspend fun getAutoplayCandidate(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion = BroadcastRegion.OTHER,
        prefs: MatchPrefs = MatchPrefs(),
        currentEpgTitleFor: (String) -> String = { "" },
    ): MatchedChannel? {
        val scores = ChannelScorer.scoreChannelsWithRegion(
            MatchTarget(event), channels, userRegion, prefs, currentEpgTitleFor
        )
        val autoplay = ChannelScorer.autoplayCandidate(scores) ?: return null
        return MatchedChannel(
            channel = autoplay.channel,
            matchType = autoplay.matchType,
            sourceName = "",
            providerGroup = autoplay.channel.sourceType,
            score = autoplay.score,
            reasons = autoplay.reasons,
        )
    }

    /**
     * Detect the user's broadcast region from their available channels.
     */
    fun detectUserRegion(channels: List<IptvChannel>): BroadcastRegion {
        return SportsBroadcastRegionPolicy.detectRegion(channels)
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun List<ChannelScore>.mapToMatchedChannels(sourceName: String): List<MatchedChannel> {
        return map {
            MatchedChannel(
                channel = it.channel,
                matchType = it.matchType,
                sourceName = sourceName,
                providerGroup = it.channel.sourceType,
                score = it.score,
                reasons = it.reasons,
            )
        }
    }

    private fun MatchedChannel.toChannelScore(): ChannelScore {
        return ChannelScore(
            channel = channel,
            matchType = matchType,
            score = score,
            reasons = reasons,
        )
    }
}
