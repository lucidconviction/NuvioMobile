package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.IptvRepository

object GameToChannelMatcher {

    internal val leagueKeywords = mapOf(
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

    internal val generalSportsKeywords = listOf("sports", "espn", "espn2", "espn3", "espnu", "espnews", "fox sports", "fs1", "fs2", "cbs sports", "cbsn", "nbc sports", "tnt", "tbs", "tru tv", "dazn", "bein", "bein sport", "sky sports", "sky sport", "bt sport", "eurosport", "eurosport 1", "eurosport 2", "sport tv", "premium sport", "sport 1", "sport 2", "sport 3", "sport 4", "sport 5", "tnt sport", "tnt sports", "paramount", "peacock", "abc", "cbs", "nbc", "fox", "usa network", "golf channel", "nfl network", "nba tv", "mlb network", "nhl network", "tennis channel", "olympic", "sportsnet", "tsn", "rds", "cbc", "ctv", "kayo", "fox footy", "fox league", "seven", "nine", "willow", "sky racing")

    internal val genericNetworks = setOf("fox", "abc", "cbs", "nbc", "cbc", "ctv", "seven", "nine", "paramount", "peacock")

    internal val sportsIndicators = listOf("sport", "sports", "fs1", "fs2", "espn", "nfl", "nba", "mlb", "nhl", "ufc", "fight", "game", "league", "pass", "network", "tv", "stream", "live")

    fun isNonSportsChannel(channelName: String, categoryName: String): Boolean {
        if (nonSportsChannelKeywords.any { channelName.contains(it) }) return true
        val isNonSportsCategory = nonSportsGroupKeywords.any { categoryName.contains(it) }
        val isSportsCategory = sportsCategoryIndicators.any { categoryName.contains(it) }
        if (isNonSportsCategory && !isSportsCategory) return true
        return false
    }

    fun matchChannels(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        sourceName: String = "",
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<MatchedChannel> {
        return ChannelScorer.scoreChannels(event, channels, currentEpgTitleFor)
            .map {
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

    /**
     * Two-pass variant for the detail-panel list: fast name/league/generic score first, then lazily
     * fetch current-program EPG for the top [cap] candidates and re-score so a live program's
     * matchup outranks an unrelated generic sports channel — without blocking on EPG for every channel.
     */
    suspend fun matchChannelsWithLazyEpg(
        event: EspnProcessedEvent,
        channels: List<IptvChannel>,
        sourceName: String = "",
        currentEpgTitleFor: (String) -> String = { "" },
        cap: Int = 8,
    ): List<MatchedChannel> {
        val fast = ChannelScorer.scoreChannels(event, channels, currentEpgTitleFor)
        val candidates = fast.filter { it.score > 0 }.map { it.channel }
        val lazyLookup = IptvRepository.buildLazyEpgTitleLookup(candidates, cap)
        val combined: (String) -> String = { name ->
            val lazy = lazyLookup(name)
            if (lazy.isNotEmpty()) lazy else currentEpgTitleFor(name)
        }
        return ChannelScorer.scoreChannels(event, channels, combined)
            .map {
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

    fun matchesByLeagueKeywords(league: String, channelName: String, category: String): Boolean {
        if (league.isBlank()) return false
        val key = leagueKey(league)
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

    /**
     * Maps the league value carried on an event to the keyword-table key. Events arrive with ESPN
     * path IDs like "Eng.1" (Premier League), so alias them onto the readable table keys.
     */
    internal fun leagueKey(league: String): String {
        val upper = league.trim().uppercase()
        return when (upper) {
            "ENG.1", "PREMIER LEAGUE", "EPL" -> "EPL"
            "ESP.1", "LALIGA" -> "LALIGA"
            "ITA.1", "SERIE A" -> "SERIEA"
            "GER.1", "BUNDESLIGA" -> "BUNDES"
            "FRA.1", "LIGUE 1" -> "LIGUE1"
            "USA.1", "MLS" -> "MLS"
            "ENGLISH-PREMIERSHIP", "ENGLISH PREMIERSHIP" -> "RUGBY"
            "COLLEGE-FOOTBALL", "COLLEGE FOOTBALL" -> "CFB"
            "MENS-COLLEGE-BASKETBALL", "NCAA BASKETBALL" -> "CBB"
            "ATP", "WTA" -> "TEN"
            "PGA", "PGATOUR" -> "GOLF"
            "F1", "FORMULA 1" -> "F1"
            else -> upper.ifBlank { "" }
        }
    }

    fun matchesGeneralSportsKeywords(channelName: String, category: String): Boolean {
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

    }

