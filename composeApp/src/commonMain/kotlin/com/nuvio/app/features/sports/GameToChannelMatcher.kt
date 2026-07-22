package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel

object GameToChannelMatcher {

    private val leagueKeywords = mapOf(
        "NFL" to listOf("nfl", "football", "nfl network", "nfl redzone", "sunday night football", "monday night football", "thursday night football", "fox", "cbs", "nbc", "espn", "sky sports nfl", "nfl sunday", "gridiron", "espn2", "espnews", "nfln", "nfl network", "paramount", "peacock", "prime video", "amazon"),
        "NBA" to listOf("nba", "basketball", "nba tv", "tnt", "espn", "abc", "nba league pass", "sky sports nba", "bt sport nba", "tyson", "nba finals", "nbatv", "espn2", "nba tv", "sportsnet", "tsn"),
        "MLB" to listOf("mlb", "baseball", "mlb network", "espn", "fox", "fs1", "fs2", "tbs", "mlb.tv", "world series", "mlbn", "sportsnet", "tsn", "mlb network"),
        "NHL" to listOf("nhl", "hockey", "nhl network", "tnt", "espn", "abc", "nhl.tv", "stanley cup", "nhl center ice", "sportsnet", "cbc", "tsn", "nhl network", "hockey"),
        "MLS" to listOf("mls", "soccer", "fox soccer", "apple tv", "mls season pass", "football", "fs1", "fs2", "espn", "tsn"),
        "UFC" to listOf("ufc", "mma", "espn", "espn+", "espn2", "ufc fight pass", "tnt sports", "bt sport", "dazn", "fight night", "ufc ppv", "tnt sport 1", "tnt sport 2", "sky sports arena"),
        "BOX" to listOf("boxing", "fight", "espn", "dazn", "showtime", "sky sports boxing", "bt sport boxing", "matchroom", "top rank", "ppv", "title fight", "tnt sports", "dazn 1", "dazn 2", "boxing"),
        "PFL" to listOf("pfl", "mma", "espn2", "espn", "espn+", "dazn"),
        "F1" to listOf("f1", "formula 1", "formula one", "sky sports f1", "sky sports f1", "espn", "fox", "grand prix", "motorsport", "f1 tv", "dazn"),
        "MOTO" to listOf("motogp", "motorcycle", "motorsport", "dazn", "bt sport", "sport", "motogp"),
        "TEN" to listOf("tennis", "atp", "wta", "grand slam", "wimbledon", "us open", "australian open", "french open", "sky sports tennis", "tennis channel", "espn", "espn2", "eurosport"),
        "GOLF" to listOf("golf", "pga", "masters", "open championship", "sky sports golf", "golf channel", "nbc", "cbs", "espn", "golf"),
        "CFB" to listOf("college football", "ncaa football", "cfb", "espn", "fox", "abc", "cbs", "ncaa", "bowl game", "college game day", "espn2", "sec network", "acc network", "big ten network", "btn", "espnu"),
        "CBB" to listOf("college basketball", "ncaa basketball", "march madness", "cbb", "espn", "cbs", "tbs", "tru tv", "ncaa tournament", "final four", "espn2", "sec network", "big ten network", "btn"),
        "WNBA" to listOf("wnba", "women basketball", "espn", "espn2", "nba tv", "cbs", "abc"),
        "AFL" to listOf("afl", "australian football", "afl footy", "fox footy", "kayo", "seven", "afl"),
        "NRL" to listOf("nrl", "rugby league", "fox league", "kayo", "nine", "nrl"),
        "RUGBY" to listOf("rugby", "rugby union", "six nations", "world rugby", "premiership rugby", "united rugby", "sky sports", "bt sport", "tnt sports", "rugby"),
        "EPL" to listOf("premier league", "epl", "english football", "sky sports", "sky sports premier league", "sky sports main event", "bt sport", "tnt sports", "nbc sports", "usa network", "peacock", "epl football", "sky sports football"),
        "LALIGA" to listOf("la liga", "laliga", "spanish football", "espn", "espn2", "dazn", "sky sports", "premium sport", "barcelona", "real madrid", "la liga tv"),
        "SERIEA" to listOf("serie a", "italian football", "cbs", "paramount", "bt sport", "sky sport", "sky sport italia", "juventus", "milan", "inter", "serie a"),
        "BUNDES" to listOf("bundesliga", "german football", "espn", "fox", "sky sport", "sky sport bundesliga", "bayern", "dortmund", "bundesliga"),
        "LIGUE1" to listOf("ligue 1", "french football", "bein", "bt sport", "sky sport", "psg", "ligue 1"),
        "UCL" to listOf("champions league", "ucl", "uefa", "sky sports", "tnt sports", "bt sport", "bt sport espn", "cbs", "paramount", "espn", "tnt sport", "champions league"),
        "UEL" to listOf("europa league", "uefa", "sky sports", "tnt sports", "bt sport", "europa league"),
        "CRIC" to listOf("cricket", "ipl", "bbl", "ashes", "world cup", "sky sports", "fox cricket", "kayo", "willow", "espn cricket", "cricket"),
    )

    private val nonSportsGroupKeywords = listOf(
        "news", "movie", "movies", "cinema", "entertainment", "kids", "family", "music",
        "comedy", "drama", "documentary", "documentaries", "radio", "adult", "xx", "xxx",
        "general", "shopping", "reality", "local news", "series", "animation", "lifestyle",
    )

    private val sportsCategoryIndicators = listOf(
        "sport", "sports", "nfl", "nba", "mlb", "nhl", "ufc", "football", "basketball",
        "baseball", "hockey", "soccer", "fight", "racing", "f1", "golf", "tennis",
        "espn", "dazn", "bein", "arena", "match", "league", "ppv", "box", "super sport",
    )

    private val nonSportsChannelKeywords = listOf(
        "fox news", "fox business", "abc news", "cbs news", "nbc news", "sky news",
        "bbc news", "cnn", "msnbc", "bloomberg", "hbo", "starz", "cinemax", "disney",
        "nickelodeon", "cartoon network", "mtv", "vh1", "discovery", "history channel",
        "national geographic", "tlc", "hgtv", "food network", "hallmark", "weather channel",
        "qvc", "hsn", "cbs drama", "cbs reality", "cbs justice", "paramount network",
    )

    private val generalSportsKeywords = listOf("sports", "espn", "espn2", "espn3", "espnu", "espnews", "fox sports", "fs1", "fs2", "cbs sports", "cbsn", "nbc sports", "tnt", "tbs", "tru tv", "dazn", "bein", "bein sport", "sky sports", "sky sport", "bt sport", "eurosport", "eurosport 1", "eurosport 2", "sport tv", "premium sport", "sport 1", "sport 2", "sport 3", "sport 4", "sport 5", "tnt sport", "tnt sports", "paramount", "peacock", "abc", "cbs", "nbc", "fox", "usa network", "golf channel", "nfl network", "nba tv", "mlb network", "nhl network", "tennis channel", "olympic", "sportsnet", "tsn", "rds", "cbc", "ctv", "kayo", "fox footy", "fox league", "seven", "nine", "willow", "sky racing")

    private val genericNetworks = setOf("fox", "abc", "cbs", "nbc", "cbc", "ctv", "seven", "nine", "paramount", "peacock")

    private val sportsIndicators = listOf("sport", "sports", "fs1", "fs2", "espn", "nfl", "nba", "mlb", "nhl", "ufc", "fight", "game", "league", "pass", "network", "tv", "stream", "live")

    fun isNonSportsChannel(channelName: String, categoryName: String): Boolean {
        if (nonSportsChannelKeywords.any { channelName.contains(it) }) return true
        val isNonSportsCategory = nonSportsGroupKeywords.any { categoryName.contains(it) }
        val isSportsCategory = sportsCategoryIndicators.any { categoryName.contains(it) }
        if (isNonSportsCategory && !isSportsCategory) return true
        return false
    }

    fun matchChannels(event: EspnProcessedEvent, channels: List<IptvChannel>, sourceName: String = ""): List<MatchedChannel> {
        val matches = mutableListOf<MatchedChannel>()

        for (channel in channels) {
            val channelName = channel.name.lowercase().trim()
            val categoryName = (channel.group ?: "").lowercase().trim()

            if (isNonSportsChannel(channelName, categoryName)) continue

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
        return keywords.any { kw ->
            val kwLower = kw.lowercase()
            if (kwLower in genericNetworks) {
                (channelName.contains(kwLower) || category.contains(kwLower)) &&
                        sportsIndicators.any { channelName.contains(it) || category.contains(it) }
            } else {
                channelName.contains(kwLower) || category.contains(kwLower)
            }
        }
    }

    private fun matchesByTeamName(homeTeam: String, awayTeam: String, channelName: String, category: String): Boolean {
        val teamTokens = (homeTeam.split(" ") + awayTeam.split(" "))
            .map { it.lowercase().trim() }
            .filter { it.length >= 4 && it !in commonSkipWords }
        if (teamTokens.isEmpty()) return false
        return teamTokens.any { token ->
            channelName.contains(token) || category.contains(token)
        }
    }

    private fun matchesGeneralSports(channelName: String, category: String): Boolean {
        return generalSportsKeywords.any { kw ->
            if (kw in genericNetworks) {
                (channelName.contains(kw) || category.contains(kw)) &&
                        sportsIndicators.any { channelName.contains(it) || category.contains(it) }
            } else {
                channelName.contains(kw) || category.contains(kw)
            }
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

    private val commonSkipWords = setOf(
        "the", "and", "for", "fc", "utd", "vs", "at", "de", "los", "las", "san", "real", "city",
        "united", "team", "club", "fc", "cf", "ac", "ssc", "sc", "sv", "bv", "vfl", "tsv", "1.", "2.", "3.",
        "red", "blue", "white", "black", "gold", "golden", "bay", "st", "new", "york", "angels",
        "north", "south", "east", "west", "central", "state", "port", "green", "grand", "la", "el", "al"
    )
}

