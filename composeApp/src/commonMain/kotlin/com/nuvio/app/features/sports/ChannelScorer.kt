package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import kotlinx.serialization.Serializable

/**
 * Scored sport-channel matcher.
 *
 * Replaces the boolean keyword matching in [GameToChannelMatcher] with an evidence-weighted
 * score, modeled on RunTV's `_scoreChannelForGame`:
 *  - team presence (full name -> stripped -> first word -> last token) is the strongest signal
 *  - EPG current program containment is a strong booster
 *  - league + generic-sports keywords are weak but still surface candidates
 *  - ambiguous first words ("real", "city") are penalized
 */
@Serializable
data class ChannelScore(
    val channel: IptvChannel,
    val matchType: MatchType,
    val score: Int,
    val reasons: List<String>,
)

/** The two teams and league a channel is being tested against. */
data class MatchTarget(
    val homeTeam: String,
    val awayTeam: String,
    val league: String,
    val sport: String = "",
) {
    constructor(event: EspnProcessedEvent) : this(
        homeTeam = event.homeTeam,
        awayTeam = event.awayTeam,
        league = event.league,
        sport = event.sport,
    )
}

object ChannelScorer {

    // Event-level signal weights (tuned for real playlists)
    private const val BOTH_TEAMS = 100
    private const val ONE_TEAM = 40
    private const val EPG_BOTH_TEAMS = 60
    private const val EPG_ONE_TEAM = 25
    private const val LEAGUE_MATCH = 15
    private const val GENERIC_SPORTS = 8
    private const val AMBIGUOUS_PENALTY = -20

    /** Feeds a cached EPG lookup keyed by normalized channel name -> normalized current title. */
    fun buildEpgLookup(
        currentTitleByChannel: Map<String, String>,
    ): (String) -> String = { rawChannelName ->
        val key = ChannelText.channelNameKey(rawChannelName)
        currentTitleByChannel[key]
            ?: currentTitleByChannel.entries
                .firstOrNull { key.length >= 3 && key.contains(it.key) }
                ?.value
            ?: ""
    }

    private fun teamPresent(channelText: String, teamKeys: List<String>): Boolean {
        return teamKeys.any { it.isNotEmpty() && channelText.contains(it) }
    }

    private fun leagueMatch(
        target: MatchTarget,
        channelName: String,
        category: String,
    ): Boolean {
        val league = target.league.ifBlank { return false }
        return GameToChannelMatcher.matchesByLeagueKeywords(league, channelName, category)
    }

    private fun genericSportsMatch(channelName: String, category: String): Boolean {
        return GameToChannelMatcher.matchesGeneralSportsKeywords(channelName, category)
    }

    /**
     * Scores a single channel for [target]. `currentEpgTitle` is the pre-normalized current
     * program title for the channel (empty string when unavailable for M3U/Stalker sources).
     */
    fun scoreChannel(
        target: MatchTarget,
        channel: IptvChannel,
        currentEpgTitle: String? = null,
    ): ChannelScore? {
        if (GameToChannelMatcher.isNonSportsChannel(channel.name, channel.group ?: "")) return null

        val channelName = ChannelText.channelNameKey(channel.name)
        val category = ChannelText.channelNameKey(channel.group ?: "")
        val epgTitle = ChannelText.normalize(currentEpgTitle.orEmpty())
        val combined = "$channelName $category".trim()
        val combinedWithEpg = if (epgTitle.isNotEmpty()) "$combined $epgTitle" else combined

        if (combined.isEmpty()) return null

        val homeKeys = TeamNameKeys.expand(target.homeTeam, target.league)
        val awayKeys = TeamNameKeys.expand(target.awayTeam, target.league)

        val homeInName = teamPresent(combined, homeKeys)
        val awayInName = teamPresent(combined, awayKeys)
        val homeInEpg = teamPresent(epgTitle, homeKeys)
        val awayInEpg = teamPresent(epgTitle, awayKeys)

        val reasons = mutableListOf<String>()
        var score = 0

        if (homeInName && awayInName) {
            score += BOTH_TEAMS
            reasons.add("both teams")
        } else if (homeInName || awayInName) {
            score += ONE_TEAM
            reasons.add(if (homeInName) "home ${target.homeTeam}" else "away ${target.awayTeam}")
        }

        if (homeInEpg && awayInEpg) {
            score += EPG_BOTH_TEAMS
            reasons.add("EPG both teams")
        } else if (homeInEpg || awayInEpg) {
            score += EPG_ONE_TEAM
            reasons.add("EPG ${if (homeInEpg) target.homeTeam else target.awayTeam}")
        }

        if (leagueMatch(target, channelName, category)) {
            score += LEAGUE_MATCH
            reasons.add("league ${target.league}")
        } else if (genericSportsMatch(channelName, category)) {
            score += GENERIC_SPORTS
            reasons.add("sports")
        }

        // Ambiguity penalty: name is the team's first word being used generically.
        for (team in listOf(target.homeTeam, target.awayTeam)) {
            val first = ChannelText.normalize(team).split(" ").firstOrNull() ?: continue
            if (first.length >= 4 && TeamNameKeys.isAmbiguousFirstWord(first) &&
                combined.contains(first) && !combined.contains(first + " " + ChannelText.normalize(team.split(" ").getOrNull(1) ?: "").trim())
            ) {
                score += AMBIGUOUS_PENALTY
                reasons.add("ambiguous '$first'")
            }
        }

        if (score <= 0) return null

        val matchType = when {
            homeInName && awayInName -> MatchType.TEAM
            homeInName || awayInName -> MatchType.TEAM
            leagueMatch(target, channelName, category) -> MatchType.LEAGUE
            else -> MatchType.GENERAL_SPORTS
        }
        return ChannelScore(channel, matchType, score, reasons.distinct())
    }

    /** Convenience overload that builds a [MatchTarget] from an [EspnProcessedEvent]. */
    fun scoreChannel(
        event: EspnProcessedEvent,
        channel: IptvChannel,
        currentEpgTitle: String? = null,
    ): ChannelScore? = scoreChannel(MatchTarget(event), channel, currentEpgTitle)

    /** Scores all channels and returns them sorted descending by score. */
    fun scoreChannels(
        target: MatchTarget,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<ChannelScore> {
        return channels
            .mapNotNull { scoreChannel(target, it, currentEpgTitleFor(it.name)) }
            .sortedWith(compareByDescending<ChannelScore> { it.score }.thenBy { it.channel.name })
            .distinctByUrl()
    }

    // Same stream URL can appear under multiple sources (M3U + Xtream + Stalker all
    // point at the same origin). Keep the higher-scoring copy so the picker isn't
    // full of duplicates of one feed. Input is already score-sorted, so keep first.
    private fun List<ChannelScore>.distinctByUrl(): List<ChannelScore> {
        val seen = mutableSetOf<String>()
        return filter { score ->
            val url = score.channel.url.trim().lowercase()
            if (url.isEmpty()) return@filter true
            seen.add(url)
        }
    }

    // Autoplay guardrails: only skip the picker when the best channel is clearly the
    // right one — otherwise "ESPN (league match)" would silently auto-play for any game.
    private const val AUTOPLAY_SCORE_FLOOR = 60
    private const val AUTOPLAY_GAP = 20

    /**
     * Returns the channel that may safely autoplay, or null when the picker should be
     * shown instead. Requires the top candidate to be a strong match (both teams in name,
     * or both teams in EPG) and clearly ahead of the runner-up.
     */
    fun autoplayCandidate(scored: List<ChannelScore>): ChannelScore? {
        val top = scored.firstOrNull() ?: return null
        if (top.score < AUTOPLAY_SCORE_FLOOR) return null
        val runnerUp = scored.getOrNull(1)
        if (runnerUp != null && top.score - runnerUp.score < AUTOPLAY_GAP) return null
        return top
    }

    /** Convenience overload that builds a [MatchTarget] from an [EspnProcessedEvent]. */
    fun scoreChannels(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<ChannelScore> = scoreChannels(MatchTarget(event), channels, currentEpgTitleFor)

    // ── Region-aware scoring pipeline ──────────────────────────────────────────

    /**
     * Full scoring pipeline with region awareness: base scores → region adjustments →
     * source ranking → user preferences. Returns sorted descending by composite score.
     *
     * This is the recommended entry point for new code. The non-region overloads
     * remain for backward compatibility.
     */
    fun scoreChannelsWithRegion(
        target: MatchTarget,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion = BroadcastRegion.OTHER,
        prefs: MatchPrefs = MatchPrefs(),
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<ChannelScore> {
        return scoreChannels(target, channels, currentEpgTitleFor)
            .let { SportsBroadcastRegionPolicy.applyRegionScores(it, userRegion) }
            .let { SportsChannelMatchPolicy.rankChannels(it, prefs) }
    }

    /** Convenience overload that builds a [MatchTarget] from an [EspnProcessedEvent]. */
    fun scoreChannelsWithRegion(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        userRegion: BroadcastRegion = BroadcastRegion.OTHER,
        prefs: MatchPrefs = MatchPrefs(),
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<ChannelScore> = scoreChannelsWithRegion(MatchTarget(event), channels, userRegion, prefs, currentEpgTitleFor)
}