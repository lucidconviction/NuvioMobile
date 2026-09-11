package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.SourceType

/**
 * Channel quality/source preferences policy.
 *
 * Ranks channels by source type (official broadcasts > IPTV sources), applies
 * user preferences for preferred providers and quality thresholds. Adapted from
 * Tuvora's SportsChannelMatchPolicy for Nuvio's singleton architecture.
 *
 * This policy runs AFTER [ChannelScorer] base scoring and [SportsBroadcastRegionPolicy]
 * region adjustments, providing a final ranking pass.
 */
data class MatchPrefs(
    /** Preferred source types in priority order (e.g., M3U first, then Xtream) */
    val preferredSourceTypes: List<SourceType> = emptyList(),
    /** Preferred provider keywords (e.g., "ESPN", "Sky Sports", "DAZN") */
    val preferredProviders: List<String> = emptyList(),
    /** Minimum quality tag to accept (null = accept all) */
    val minQuality: String? = null,
    /** Prefer official/broadcast channels over generic IPTV */
    val preferOfficial: Boolean = true,
)

object SportsChannelMatchPolicy {

    // Score adjustments for source type ranking
    private const val OFFICIAL_SOURCE_BOOST = 15
    private const val PREFERRED_SOURCE_BOOST = 10
    private const val PREFERRED_PROVIDER_BOOST = 8
    private const val UNKNOWN_SOURCE_PENALTY = -5

    /**
     * Rank channels by source quality.
     * Official broadcast sources get a boost over generic IPTV sources.
     */
    fun rankBySource(scores: List<ChannelScore>): List<ChannelScore> {
        return scores.map { channelScore ->
            val sourceBoost = when (channelScore.channel.sourceType) {
                SourceType.Xtream -> OFFICIAL_SOURCE_BOOST  // Xtream often has official streams
                SourceType.M3U -> 0                          // Neutral — mixed quality
                SourceType.Stalker -> -3                      // Often lower quality
            }

            val newScore = (channelScore.score + sourceBoost).coerceAtLeast(0)
            val newReasons = channelScore.reasons.toMutableList()

            if (sourceBoost > 0) {
                newReasons.add("source: ${channelScore.channel.sourceType.label}")
            }

            channelScore.copy(score = newScore, reasons = newReasons)
        }
    }

    /**
     * Apply user preferences to channel ranking.
     * Boosts channels matching preferred providers and source types.
     */
    fun applyPreferences(
        scores: List<ChannelScore>,
        prefs: MatchPrefs,
    ): List<ChannelScore> {
        if (prefs.preferredSourceTypes.isEmpty() && prefs.preferredProviders.isEmpty()) {
            return scores
        }

        return scores.map { channelScore ->
            var boost = 0
            val newReasons = channelScore.reasons.toMutableList()

            // Boost preferred source types
            if (prefs.preferredSourceTypes.isNotEmpty()) {
                val sourceIndex = prefs.preferredSourceTypes.indexOf(channelScore.channel.sourceType)
                if (sourceIndex >= 0) {
                    boost += PREFERRED_SOURCE_BOOST * (prefs.preferredSourceTypes.size - sourceIndex)
                    newReasons.add("preferred source (${channelScore.channel.sourceType.label})")
                }
            }

            // Boost preferred providers
            if (prefs.preferredProviders.isNotEmpty()) {
                val channelName = channelScore.channel.name.lowercase()
                val matchesProvider = prefs.preferredProviders.any { provider ->
                    channelName.contains(provider.lowercase())
                }
                if (matchesProvider) {
                    boost += PREFERRED_PROVIDER_BOOST
                    newReasons.add("preferred provider")
                }
            }

            val newScore = (channelScore.score + boost).coerceAtLeast(0)
            channelScore.copy(score = newScore, reasons = newReasons)
        }
    }

    /**
     * Full pipeline: source ranking + user preferences.
     * Call this after ChannelScorer + SportsBroadcastRegionPolicy.
     */
    fun rankChannels(
        scores: List<ChannelScore>,
        prefs: MatchPrefs = MatchPrefs(),
    ): List<ChannelScore> {
        return scores
            .let { rankBySource(it) }
            .let { applyPreferences(it, prefs) }
            .sortedByDescending { it.score }
    }

    /**
     * Source type summary for display.
     * Returns breakdown of how many channels per source type.
     */
    fun getSourceDistribution(scores: List<ChannelScore>): Map<SourceType, Int> {
        val distribution = mutableMapOf<SourceType, Int>()
        for (score in scores) {
            val type = score.channel.sourceType
            distribution[type] = (distribution[type] ?: 0) + 1
        }
        return distribution
    }
}
