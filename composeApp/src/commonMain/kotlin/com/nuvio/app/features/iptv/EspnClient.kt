package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object EspnClient {
    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports"
    private val json = Json { ignoreUnknownKeys = true }

    private val sports = listOf(
        "football/nfl",
        "football/college-football",
        "basketball/nba",
        "basketball/mens-college-basketball",
        "basketball/wnba",
        "baseball/mlb",
        "hockey/nhl",
        "soccer/usa.1",
        "soccer/eng.1",
        "soccer/esp.1",
        "soccer/ita.1",
        "soccer/ger.1",
        "soccer/fra.1",
        "mma/ufc",
        "fighting/ufc",
        "mma/pfl",
        "fighting/pfl",
        "mma/bellator",
        "fighting/bellator",
        "mma/boxing",
        "fighting/boxing",
        "racing/f1",
        "golf/pga",
        "tennis/atp",
        "tennis/wta",
        "rugby/english-premiership",
    )

    private val prioritizedSports = listOf(
        "mma/ufc", "fighting/ufc",
        "mma/pfl", "fighting/pfl",
        "mma/boxing", "fighting/boxing",
        "mma/bellator", "fighting/bellator",
        "football/nfl", "basketball/nba", "baseball/mlb", "hockey/nhl",
        "football/college-football", "basketball/mens-college-basketball",
        "soccer/eng.1", "soccer/usa.1", "soccer/esp.1", "soccer/ita.1", "soccer/ger.1", "soccer/fra.1",
        "basketball/wnba",
        "racing/f1", "golf/pga", "tennis/atp", "tennis/wta", "rugby/english-premiership",
    )

    private const val PER_SPORT_TIMEOUT_MS = 5_000L
    private const val EARLY_BAIL_EVENT_COUNT = 40
    private const val WIKI_API = "https://en.wikipedia.org/api/rest_v1/page/summary"

    private val wikiThumbnailCache = mutableMapOf<String, String?>()
    private val fetchedWikiPages = mutableSetOf<String>()

    suspend fun fetchLeague(sport: String, league: String): List<EspnProcessedEvent> {
        val url = "$BASE/$sport/$league/scoreboard"
        return try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<EspnResponse>(response)
            val events = parsed.events.flatMap { event -> processEvent(event, "$sport/$league") }
            enrichEventImages(events)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchAll(date: String? = null): List<EspnProcessedEvent> {
        val dateParam = if (!date.isNullOrBlank()) "?dates=${date.replace("-", "")}" else ""
        val skipFilter = !date.isNullOrBlank()
        val results = mutableListOf<EspnProcessedEvent>()

        for (sport in prioritizedSports) {
            if (results.size >= EARLY_BAIL_EVENT_COUNT && date.isNullOrBlank()) break
            val events = withTimeoutOrNull(PER_SPORT_TIMEOUT_MS) {
                try {
                    val url = "$BASE/$sport/scoreboard$dateParam"
                    val response = httpGetText(url)
                    val parsed = json.decodeFromString<EspnResponse>(response)
                    parsed.events.flatMap { event ->
                        processEvent(event, sport)
                    }.filter { e ->
                        e.isLive || e.status != "STATUS_FINAL" || detailWithinHours(e.detail, 3) || skipFilter
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            results.addAll(events)
            if (results.size >= EARLY_BAIL_EVENT_COUNT && date.isNullOrBlank()) break
        }
        return enrichEventImages(results)
    }

    private fun detailWithinHours(detail: String, hours: Int): Boolean {
        if (detail.isBlank()) return false
        val lower = detail.lowercase()
        if (lower.contains("final") || lower.contains("end")) return false
        return true
    }

    fun processEvent(event: EspnEvent, sportPath: String): List<EspnProcessedEvent> {
        val sport = when (val raw = sportPath.split("/").firstOrNull() ?: "Sport") {
            "mma" -> "Fighting"
            else -> raw.replaceFirstChar { it.uppercase() }
        }
        val league = sportPath.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercase() } ?: ""

        return event.competitions.mapNotNull { comp ->
            val home = comp.competitors.find { it.homeAway == "home" } ?: comp.competitors.firstOrNull()
            val away = comp.competitors.find { it.homeAway == "away" } ?: comp.competitors.getOrNull(1)

            val channel = comp.broadcasts?.firstOrNull()
                ?.names?.firstOrNull() ?: ""

            val isPpv = channel.contains("PPV", ignoreCase = true) ||
                comp.notes?.any { it.type == "ppv" || it.headline.contains("PPV", ignoreCase = true) } == true

            val statusName = comp.status?.type?.name ?: ""
            val isLive = statusName == "STATUS_IN_PROGRESS"
            val detail = comp.status?.type?.detail ?: ""

            val dateStr = (event.date.takeIf { it.isNotBlank() } ?: comp.date).take(10)

            val eventImage = comp.logos
                ?.maxByOrNull { it.width * it.height }
                ?.href
                ?.takeIf { it.isNotBlank() }
                ?: event.thumbnail?.takeIf { it.isNotBlank() }

            val wikiPage = if (eventImage == null && sport == "Fighting") {
                guessWikipediaPage(event.name.ifBlank { event.shortName })
            } else null

            EspnProcessedEvent(
                id = event.id + "_" + comp.id,
                title = event.shortName.ifBlank { event.name },
                homeTeam = home?.team?.displayName ?: home?.team?.name ?: "",
                awayTeam = away?.team?.displayName ?: away?.team?.name ?: "",
                homeScore = home?.score,
                awayScore = away?.score,
                homeLogo = home?.team?.logo,
                awayLogo = away?.team?.logo,
                eventImage = eventImage,
                channel = channel,
                status = statusName,
                detail = detail,
                date = dateStr,
                sport = sport,
                league = league,
                isLive = isLive,
                isPpv = isPpv,
                wikipediaPage = wikiPage,
            )
        }
    }

    suspend fun enrichEventImages(events: List<EspnProcessedEvent>): List<EspnProcessedEvent> {
        val needWiki = events.filter { it.eventImage == null && it.wikipediaPage != null }
        if (needWiki.isEmpty()) return events

        for (e in needWiki) {
            val page = e.wikipediaPage ?: continue
            if (page in wikiThumbnailCache) continue
            if (page in fetchedWikiPages) continue
            fetchedWikiPages.add(page)
            val url = withTimeoutOrNull(3_000L) {
                try {
                    val resp = httpGetText("$WIKI_API/${urlEncode(page)}")
                    val parsed = json.decodeFromString<WikipediaPageSummary>(resp)
                    parsed.thumbnail?.source
                } catch (_: Exception) { null }
            }
            wikiThumbnailCache[page] = url
        }

        return events.map { e ->
            val wikiUrl = e.wikipediaPage?.let { wikiThumbnailCache[it] }
            if (wikiUrl != null && e.eventImage == null) e.copy(eventImage = wikiUrl) else e
        }
    }

    private fun guessWikipediaPage(title: String): String? {
        if (title.isBlank()) return null
        // Try "UFC 313" pattern first — most reliable
        val ufcMatch = Regex("""UFC\s+\d+""").find(title)
        if (ufcMatch != null) return ufcMatch.value.replace(" ", "_")
        // Try "PFL \d+" or "PFL (year)"
        val pflMatch = Regex("""PFL\s+\d+""").find(title)
        if (pflMatch != null) return pflMatch.value.replace(" ", "_")
        val pflYearMatch = Regex("""PFL\s+\d{4}""").find(title)
        if (pflYearMatch != null) return pflYearMatch.value.replace(" ", "_")
        // Try "Bellator \d+"
        val bellatorMatch = Regex("""Bellator\s+\d+""", RegexOption.IGNORE_CASE).find(title)
        if (bellatorMatch != null) return bellatorMatch.value.replace(" ", "_")
        // For boxing PPV: try the full event name (e.g. "Canelo_Álvarez_vs._John_Ryder")
        val segments = title.split(" vs ", " Vs ", " VS ", " vs. ", " Vs. ")
        if (segments.size >= 2) {
            val first = segments.first().trim()
            // If the first segment is longer than "UFC 313" style, use full title
            if (first.length <= 25) return null // too short to be useful alone
        }
        return null
    }

    private fun urlEncode(s: String): String {
        return s.map { c ->
            when {
                c == ' ' -> "%20"
                c == '_' -> "_"
                c == '/' -> "%2F"
                c == '?' -> "%3F"
                c == '&' -> "%26"
                c == '#' -> "%23"
                c == '%' -> "%25"
                c == '\'' -> "%27"
                c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '.' || c == '~' -> c.toString()
                else -> "%" + c.code.toString(16).uppercase().padStart(2, '0')
            }
        }.joinToString("")
    }

    fun toSportEvents(processed: List<EspnProcessedEvent>): List<SportEvent> {
        return processed.map { e ->
            val teams = e.title.split(" vs ", " @ ", " at ")
            val homeName = if (teams.size >= 2) teams.last().trim() else e.homeTeam
            val awayName = if (teams.size >= 2) teams.first().trim() else e.awayTeam
            SportEvent(
                idEvent = e.id,
                strEvent = e.title,
                strSport = e.sport,
                strLeague = e.league,
                strHomeTeam = homeName,
                strAwayTeam = awayName,
                strDate = e.date,
                strTime = e.detail,
                strThumb = e.homeLogo ?: e.awayLogo,
                strChannel = e.channel,
                intHomeScore = e.homeScore,
                intAwayScore = e.awayScore,
                strStatus = if (e.isLive) "LIVE" else if (e.status.contains("FINAL")) "FINAL" else "SCHEDULED",
                strFilename = if (e.isPpv) "PPV" else null,
            )
        }
    }

    fun findAllMatchingChannels(event: SportEvent, channels: List<IptvChannel>): List<IptvChannel> {
        if (event.strChannel.isBlank()) return emptyList()
        val searchTerms = SportBroadcasterMap.expandSearchTerms(event.strChannel, event.strSport, event.strLeague)
        val matched = mutableSetOf<IptvChannel>()
        for (term in searchTerms) {
            if (term.length < 3) continue
            val lowerTerm = term.lowercase().trim()
            for (ch in channels) {
                val chName = ch.name.lowercase().trim()
                if (chName.contains(lowerTerm) || lowerTerm.contains(chName)) {
                    matched.add(ch)
                }
            }
        }
        return matched.toList()
    }

    internal fun matchSportEventsToChannels(
        sportEvents: List<SportEvent>,
        channels: List<IptvChannel>,
    ): List<MatchedSportEvent> {
        val matched = mutableListOf<MatchedSportEvent>()
        for (event in sportEvents) {
            if (event.strChannel.isBlank()) continue
            val searchTerms = SportBroadcasterMap.expandSearchTerms(event.strChannel, event.strSport, event.strLeague)
            var found: IptvChannel? = null
            for (term in searchTerms) {
                if (term.length < 3) continue
                val lowerTerm = term.lowercase().trim()
                found = channels.firstOrNull { ch ->
                    val chName = ch.name.lowercase().trim()
                    chName.contains(lowerTerm) || lowerTerm.contains(chName)
                }
                if (found != null) break
            }
            if (found != null) {
                matched.add(MatchedSportEvent(event = event, channel = found))
            }
        }
        return matched
    }
}

// ── Region-specific broadcaster mapping for channel matching ──────────────
object SportBroadcasterMap {
    private val leagueBroadcasters = mapOf(
        "nfl" to listOf(
            "ESPN", "ABC", "FOX", "CBS", "NBC", "NFL Network", "NFLN",
            "Amazon Prime", "Prime Video", "Peacock", "Paramount+",
            "Sky Sports", "Sky Sports NFL", "BBC", "BBC One", "BBC Two",
            "ITV", "ITV1", "Channel 4",
            "TSN", "TSN1", "TSN2", "TSN3", "TSN4", "TSN5", "CTV", "RDS", "DAZN",
        ),
        "college-football" to listOf(
            "ESPN", "ABC", "FOX", "CBS", "NBC", "SEC Network", "ACC Network",
            "Big Ten Network", "BTN", "ESPN2", "ESPNU",
            "TSN", "TSN2",
        ),
        "nba" to listOf(
            "ESPN", "ABC", "TNT", "NBA TV", "NBATV",
            "Sky Sports", "Sky Sports Arena",
            "TSN", "TSN1", "TSN2", "TSN3", "TSN4", "Sportsnet", "Sportsnet One",
            "DAZN",
        ),
        "mens-college-basketball" to listOf(
            "ESPN", "ABC", "CBS", "TNT", "TBS", "truTV", "ESPN2", "ESPNU",
            "SEC Network", "Big Ten Network", "TSN",
        ),
        "mlb" to listOf(
            "ESPN", "ABC", "FOX", "FS1", "TBS", "MLB Network", "MLBN",
            "Apple TV+", "Peacock",
            "Sky Sports", "BT Sport", "TNT Sports",
            "TSN", "Sportsnet", "Sportsnet One", "RDS",
        ),
        "nhl" to listOf(
            "ESPN", "ABC", "TNT", "TBS", "NHL Network",
            "Sportsnet", "Sportsnet One", "CBC", "TSN", "TSN1", "TSN2",
            "TSN3", "TSN4", "TSN5", "CTV", "RDS", "TVA Sports",
            "Sky Sports", "BBC",
        ),
        "usa.1" to listOf(
            "ESPN", "FOX", "FS1", "Apple TV+", "MLS Season Pass",
            "TSN", "TSN2", "RDS", "Sky Sports",
        ),
        "eng.1" to listOf(
            "Sky Sports", "Sky Sports Premier League", "Sky Sports Main Event",
            "TNT Sports", "BT Sport", "BBC", "BBC One",
            "Amazon Prime", "Prime Video",
            "NBC", "USA Network", "Peacock",
            "TSN", "TSN2", "RDS", "DAZN",
        ),
        "ufc" to listOf(
            "ESPN", "ESPN+", "ABC",
            "TNT Sports", "BT Sport", "Sky Sports Arena",
            "TSN", "RDS", "DAZN",
        ),
        "pfl" to listOf("ESPN", "ESPN2", "ESPN+", "DAZN"),
        "bellator" to listOf("HBO Max", "MAX", "DAZN"),
        "boxing" to listOf(
            "ESPN", "ESPN+", "ABC",
            "Sky Sports", "Sky Sports Box Office", "TNT Sports", "BT Sport",
            "DAZN", "Showtime", "TSN", "RDS",
        ),
    )

    private val sportBroadcasters = mapOf(
        "Football" to listOf(
            "ESPN", "ABC", "FOX", "CBS", "NBC", "NFL Network",
            "Sky Sports", "BBC", "ITV", "TSN", "CTV", "DAZN",
        ),
        "Basketball" to listOf(
            "ESPN", "ABC", "TNT", "NBA TV", "Sky Sports", "TSN", "Sportsnet",
        ),
        "Baseball" to listOf(
            "ESPN", "FOX", "FS1", "TBS", "MLB Network", "TSN", "Sportsnet",
        ),
        "Hockey" to listOf(
            "ESPN", "ABC", "TNT", "Sportsnet", "CBC", "TSN", "CTV", "RDS",
        ),
        "Soccer" to listOf(
            "Sky Sports", "TNT Sports", "BT Sport", "BBC",
            "ESPN", "FOX", "FS1", "ABC", "NBC", "USA Network", "TSN", "DAZN",
        ),
        "Fighting" to listOf(
            "ESPN", "ESPN+", "ABC", "TNT Sports", "BT Sport",
            "Sky Sports", "DAZN", "TSN", "RDS",
        ),
    )

    fun getBroadcastersForLeague(league: String): List<String> {
        val key = league.lowercase().replace(" ", "-").replace("_", "-")
        return leagueBroadcasters[key] ?: emptyList()
    }

    fun getBroadcastersForSport(sport: String): List<String> {
        return sportBroadcasters[sport] ?: emptyList()
    }

    fun expandSearchTerms(eventChannel: String, sport: String, league: String): List<String> {
        val terms = mutableListOf(eventChannel)
        if (eventChannel.isNotBlank()) {
            terms.add(eventChannel.lowercase().trim())
        }
        terms.addAll(getBroadcastersForLeague(league))
        terms.addAll(getBroadcastersForSport(sport))
        val fragments = terms.flatMap { it.split(" ").filter { w -> w.length > 2 } }
        terms.addAll(fragments)
        return terms.distinct()
    }
}
