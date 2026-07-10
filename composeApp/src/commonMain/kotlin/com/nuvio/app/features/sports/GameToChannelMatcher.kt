package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel

object GameToChannelMatcher {

    private val leagueKeywords = mapOf(
        "NFL" to listOf("nfl", "football", "nfl network", "nfl redzone", "sunday night football", "monday night football", "thursday night football"),
        "NBA" to listOf("nba", "basketball", "nba tv", "tnt", "espn"),
        "MLB" to listOf("mlb", "baseball", "mlb network", "espn"),
        "NHL" to listOf("nhl", "hockey", "nhl network", "tnt"),
        "MLS" to listOf("mls", "soccer", "fox soccer"),
        "UFC" to listOf("ufc", "mma", "espn", "espn+"),
        "BOX" to listOf("boxing", "fight", "espn", "dazn"),
        "PFL" to listOf("pfl", "mma", "espn2"),
    )

    private val generalSportsKeywords = listOf("sports", "espn", "fox sports", "cbs sports", "nbc sports")

    fun matchChannels(event: EspnProcessedEvent, channels: List<IptvChannel>, sourceName: String = ""): List<MatchedChannel> {
        val matches = mutableListOf<MatchedChannel>()

        for (channel in channels) {
            val channelName = channel.name.lowercase()
            val categoryName = (channel.group ?: "").lowercase()

            val match = when {
                matchesByLeagueAbbreviation(event.league, channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.LEAGUE, sourceName)
                matchesByTeamName(event.awayTeam, event.homeTeam, channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.TEAM, sourceName)
                matchesGeneralSports(channelName, categoryName) ->
                    MatchedChannel(channel, MatchType.GENERAL_SPORTS, sourceName)
                else -> null
            }
            if (match != null) {
                matches.add(match)
            }
        }

        return matches.sortedBy { it.matchType.ordinal }
    }

    private fun matchesByLeagueAbbreviation(league: String, channelName: String, category: String): Boolean {
        if (league.isBlank()) return false
        val key = league.uppercase()
        val keywords = leagueKeywords[key] ?: listOf(league.lowercase())
        return keywords.any { kw -> channelName.contains(kw) || category.contains(kw) }
    }

    private fun matchesByTeamName(homeTeam: String, awayTeam: String, channelName: String, category: String): Boolean {
        val teamTokens = (homeTeam.split(" ") + awayTeam.split(" "))
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && it !in commonSkipWords }
        return teamTokens.any { token ->
            channelName.contains(token) || category.contains(token)
        }
    }

    private fun matchesGeneralSports(channelName: String, category: String): Boolean {
        return generalSportsKeywords.any { kw ->
            channelName.contains(kw) || category.contains(kw)
        }
    }

    fun detectRegion(channel: IptvChannel): String {
        val cn = channel.name.lowercase()
        val cat = (channel.group ?: "").lowercase()
        return when {
            cn.contains("bbc") || cn.contains("itv") || cn.contains("channel 4") || cn.contains("sky sports") ||
            cn.contains("bt sport") || cn.contains("uk") || cn.contains("british") ||
            cat.contains("uk") || cat.contains("united kingdom") -> "UK"
            cn.contains("cbc") || cn.contains("tsn") || cn.contains("sportsnet") ||
            cn.contains("canada") || cn.contains("toronto") ||
            cat.contains("canada") || cat.contains("canadian") -> "CA"
            cn.contains("fox") || cn.contains("espn") || cn.contains("cbs") || cn.contains("nbc") ||
            cn.contains("abc") || cn.contains("nfl network") || cn.contains("nba tv") ||
            cn.contains("mlb network") || cn.contains("nhl network") ||
            cn.contains("us") || cn.contains("american") ||
            cat.contains("us") || cat.contains("usa") -> "US"
            else -> "Other"
        }
    }

    private val commonSkipWords = setOf("the", "and", "for", "fc", "utd", "vs", "at", "de", "los", "las", "san", "real", "city", "united", "team", "club")
}
