package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.IptvChannel

/**
 * Region-aware broadcast filtering policy.
 *
 * Determines which IPTV channels are most relevant for a user based on their region,
 * then applies region boost/penalty to channel scores. Adapted from Tuvora's
 * SportsBroadcastRegionPolicy with singleton pattern for Nuvio's architecture.
 *
 * Flow:
 *  1. Detect region from available channels (UK/US/CA/Europe/Other)
 *  2. Boost channels from user's region
 *  3. Penalize channels from mismatched regions
 *  4. Filter out completely irrelevant regions if user has strong region preference
 */
enum class BroadcastRegion(val label: String, val code: String) {
    US("United States", "US"),
    UK("United Kingdom", "UK"),
    CA("Canada", "CA"),
    EUROPE("Europe", "EU"),
    OTHER("Other / International", "OTHER");

    companion object {
        fun fromCode(code: String): BroadcastRegion {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }
    }
}

object SportsBroadcastRegionPolicy {

    // Score adjustments applied on top of ChannelScorer base scores
    private const val REGION_MATCH_BOOST = 30
    private const val REGION_MISMATCH_PENALTY = -15
    private const val REGION_UNKNOWN_PENALTY = -5

    // Minimum number of channels needed to auto-detect region
    private const val MIN_CHANNELS_FOR_DETECTION = 5

    /**
     * Detect the dominant region from a list of IPTV channels.
     * Uses a simple voting system based on channel names and categories.
     */
    fun detectRegion(channels: List<IptvChannel>): BroadcastRegion {
        if (channels.size < MIN_CHANNELS_FOR_DETECTION) return BroadcastRegion.OTHER

        val regionVotes = mutableMapOf<BroadcastRegion, Int>()

        for (channel in channels) {
            val region = classifyChannelRegion(channel)
            if (region != BroadcastRegion.OTHER) {
                regionVotes[region] = (regionVotes[region] ?: 0) + 1
            }
        }

        // Need at least 30% of channels pointing to one region for auto-detection
        val threshold = (channels.size * 0.3).toInt()
        val dominant = regionVotes.maxByOrNull { it.value }

        return if (dominant != null && dominant.value >= threshold) {
            dominant.key
        } else {
            BroadcastRegion.OTHER
        }
    }

    /**
     * Classify a single channel's region based on its name and category.
     */
    fun classifyChannelRegion(channel: IptvChannel): BroadcastRegion {
        val name = channel.name.lowercase()
        val category = (channel.group ?: "").lowercase()
        val combined = "$name $category"

        // UK signals
        if (combined.containsAny("bbc", "itv", "channel 4", "channel 5", "sky sports",
            "bt sport", "tnt sport", "uk", "british", "premier league",
            "eurosport", "epl", "england")) {
            return BroadcastRegion.UK
        }

        // US signals
        if (combined.containsAny("fox sports", "espn", "cbs sports", "nbc sports",
            "nfl network", "nba tv", "mlb network", "nhl network",
            "usa network", "american", "mls", "ncaa", "big ten",
            "sec network", "acc network", "peacock", "paramount+")) {
            return BroadcastRegion.US
        }

        // Canadian signals
        if (combined.containsAny("cbc", "tsn", "sportsnet", "tva sports",
            "canada", "canadian", "toronto", "montreal", "vancouver",
            "rds", " RDS ")) {
            return BroadcastRegion.CA
        }

        // European signals
        if (combined.containsAny("dazn", "bein", "canal+", "movistar",
            "sky sport", "rai sport", "zdf", "ard", "tf1",
            "beinsports", "eurosport")) {
            return BroadcastRegion.EUROPE
        }

        return BroadcastRegion.OTHER
    }

    /**
     * Apply region boost/penalty to a list of channel scores.
     * Channels from the user's region get a boost, mismatched channels get a penalty.
     */
    fun applyRegionScores(
        scores: List<ChannelScore>,
        userRegion: BroadcastRegion,
    ): List<ChannelScore> {
        if (userRegion == BroadcastRegion.OTHER) return scores

        return scores.map { channelScore ->
            val channelRegion = classifyChannelRegion(channelScore.channel)
            val regionAdjustment = when {
                channelRegion == userRegion -> REGION_MATCH_BOOST
                channelRegion == BroadcastRegion.OTHER -> REGION_UNKNOWN_PENALTY
                else -> REGION_MISMATCH_PENALTY
            }

            val newScore = (channelScore.score + regionAdjustment).coerceAtLeast(0)
            val newReasons = channelScore.reasons.toMutableList()

            if (regionAdjustment > 0) {
                newReasons.add("region match (${userRegion.code})")
            } else if (regionAdjustment < 0) {
                newReasons.add("region mismatch (${channelRegion.code})")
            }

            channelScore.copy(score = newScore, reasons = newReasons)
        }
    }

    /**
     * Filter channels to only show those relevant to the user's region.
     * Returns all channels if region is OTHER or if filtering would leave too few results.
     */
    fun filterByRegion(
        scores: List<ChannelScore>,
        userRegion: BroadcastRegion,
        minResults: Int = 3,
    ): List<ChannelScore> {
        if (userRegion == BroadcastRegion.OTHER) return scores

        val regionMatched = scores.filter { classifyChannelRegion(it.channel) == userRegion }

        // If filtering leaves enough results, use filtered list
        // Otherwise fall back to all channels (with region scores still applied)
        return if (regionMatched.size >= minResults) regionMatched else scores
    }

    /**
     * Get a summary of channel distribution by region for display.
     */
    fun getRegionDistribution(channels: List<IptvChannel>): Map<BroadcastRegion, Int> {
        val distribution = mutableMapOf<BroadcastRegion, Int>()
        for (channel in channels) {
            val region = classifyChannelRegion(channel)
            distribution[region] = (distribution[region] ?: 0) + 1
        }
        return distribution
    }
}

private fun String.containsAny(vararg terms: String): Boolean {
    return terms.any { this.contains(it) }
}
