package com.nuvio.app.features.iptv

import com.nuvio.app.features.sports.ChannelScore
import com.nuvio.app.features.sports.ChannelScorer
import com.nuvio.app.features.sports.MatchTarget
import com.nuvio.app.features.sports.TeamStanding
import com.nuvio.app.features.sports.StandingsGroup
import com.nuvio.app.features.sports.StandingsRow
import com.nuvio.app.features.trakt.TraktPlatformClock
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
        "mma/bkfc",
        "fighting/bkfc",
        "mma/powerslap",
        "fighting/powerslap",
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
        "mma/bkfc", "fighting/bkfc",
        "mma/powerslap", "fighting/powerslap",
        "mma/bellator", "fighting/bellator",
        "football/nfl", "basketball/nba", "baseball/mlb", "hockey/nhl",
        "football/college-football", "basketball/mens-college-basketball",
        "soccer/usa.1",
        "basketball/wnba",
        "racing/f1", "golf/pga", "tennis/atp", "tennis/wta", "rugby/english-premiership",
    )

    private const val PER_SPORT_TIMEOUT_MS = 5_000L
    private const val EARLY_BAIL_EVENT_COUNT = 40
    private const val WIKI_API = "https://en.wikipedia.org/api/rest_v1/page/summary"

    private val wikiThumbnailCache = mutableMapOf<String, String?>()
    private val fetchedWikiPages = mutableSetOf<String>()

    suspend fun fetchLeague(sport: String, league: String, date: String? = null): List<EspnProcessedEvent> {
        val dateParam = if (!date.isNullOrBlank()) "?dates=${date.replace("-", "")}" else ""
        val url = "$BASE/$sport/$league/scoreboard$dateParam"
        return try {
            val response = httpGetText(url)
            val parsed = json.decodeFromString<EspnResponse>(response)
            val events = parsed.events.flatMap { event -> processEvent(event, "$sport/$league") }
            enrichEventImages(events)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchAll(date: String? = null): List<EspnProcessedEvent> {
        val dateParam = if (!date.isNullOrBlank()) {
            // Support both single date "YYYYMMDD" and range "YYYYMMDD-YYYYMMDD"
            val cleaned = date.replace("-", "").let { d ->
                if (d.length == 8) "?dates=$d" else "?dates=${d.take(8)}-${d.drop(8)}"
            }
            cleaned
        } else ""
        val isRange = date?.replace("-", "")?.let { it.length > 8 } ?: false
        val skipFilter = !date.isNullOrBlank() && isRange
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
                        isRange || e.isLive || e.status != "STATUS_FINAL" || detailWithinHours(e.detail, 3) || skipFilter
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

        // ── Fighting sports: consolidate all competitions into one event card ──
        if (sport == "Fighting") {
            val firstComp = event.competitions.firstOrNull() ?: return emptyList()
            val mainHome = firstComp.competitors.find { it.homeAway == "home" } ?: firstComp.competitors.firstOrNull()
            val mainAway = firstComp.competitors.find { it.homeAway == "away" } ?: firstComp.competitors.getOrNull(1)

            val channel = firstComp.broadcasts?.firstOrNull()
                ?.names?.firstOrNull() ?: ""

            val isPpv = channel.contains("PPV", ignoreCase = true) ||
                firstComp.notes?.any { it.type == "ppv" || it.headline.contains("PPV", ignoreCase = true) } == true

            val anyLive = event.competitions.any { it.status?.type?.name == "STATUS_IN_PROGRESS" }
            val statusName = if (anyLive) "STATUS_IN_PROGRESS" else (firstComp.status?.type?.name ?: "")
            val detail = firstComp.status?.type?.detail ?: ""

            val rawDate = (event.date.takeIf { it.isNotBlank() } ?: firstComp.date).let { it.ifBlank { null } }
            val dateStr = rawDate?.take(10) ?: ""
            val timeStr = rawDate?.let { extractTime12h(it) }

            // Use the first competition's logo, fall back to event thumbnail
            val eventLogo = firstComp.logos
                ?.maxByOrNull { it.width * it.height }
                ?.href
                ?.takeIf { it.isNotBlank() }
                ?: event.thumbnail?.takeIf { it.isNotBlank() }

            val wikiPage = if (eventLogo == null) {
                guessWikipediaPage(event.name.ifBlank { event.shortName })
            } else null

            // Build title: event name with fight count
            val fightCount = event.competitions.size
            val titleSuffix = if (fightCount > 1) " (${fightCount} fights)" else ""
            val title = (event.shortName.ifBlank { event.name }) + titleSuffix

            val mainEventName = if (mainHome != null && mainAway != null) {
                "${mainAway.team?.displayName ?: "TBD"} vs ${mainHome.team?.displayName ?: "TBD"}"
            } else event.name

            return listOf(EspnProcessedEvent(
                id = event.id,
                title = title,
                homeTeam = mainHome?.team?.displayName ?: mainHome?.team?.name ?: "",
                awayTeam = mainAway?.team?.displayName ?: mainAway?.team?.name ?: "",
                homeScore = mainHome?.score,
                awayScore = mainAway?.score,
                homeLogo = eventLogo,   // event logo as card image (shown once)
                awayLogo = null,
                eventImage = eventLogo,
                rawDate = rawDate,
                timeStr = timeStr,
                channel = channel,
                status = statusName,
                detail = detail,
                date = dateStr,
                sport = sport,
                league = league,
                isLive = anyLive,
                isPpv = isPpv,
                wikipediaPage = wikiPage,
                subEventCount = if (fightCount > 1) fightCount else null,
            ))
        }

        // ── Non-fighting sports: one card per competition (existing logic) ──
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

            val rawDate = (event.date.takeIf { it.isNotBlank() } ?: comp.date).let { it.ifBlank { null } }
            val dateStr = rawDate?.take(10) ?: ""
            val timeStr = rawDate?.let { extractTime12h(it) }

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
                rawDate = rawDate,
                timeStr = timeStr,
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

    // ── Standings ───────────────────────────────────────────────────────────
    suspend fun fetchStandings(sport: String, league: String, season: Int? = null): List<TeamStanding> {
        val seasonParam = if (season != null) "?season=$season" else "?season=${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}"
        val urls = listOf(
            "$BASE/$sport/$league/standings$seasonParam",
            "$BASE/$sport/$league/standings",
        )
        for (url in urls) {
            try {
                val response = httpGetText(url)
                val parsed = json.decodeFromString<EspnStandingsResponse>(response)
                val standings = parseStandingsResponse(parsed, sport, league)
                if (standings.isNotEmpty()) return standings
            } catch (_: Exception) { continue }
        }
        return emptyList()
    }

    private fun parseStandingsResponse(response: EspnStandingsResponse, sport: String, league: String): List<TeamStanding> {
        val result = mutableListOf<TeamStanding>()

        fun extractTeams(entries: List<EspnStandingEntry>, prefix: String = "") {
            for (entry in entries) {
                val team = entry.team ?: continue
                val stats = entry.stats?.associate { it.name to it.displayValue } ?: emptyMap()
                val wins = stats["wins"] ?: "0"
                val losses = stats["losses"] ?: "0"
                val ties = stats["ties"]?.takeIf { it != "0" }?.let { "-$it" } ?: ""
                val record = "$wins-$losses$ties"
                val rank = result.size + 1
                result.add(TeamStanding(
                    teamName = team.displayName,
                    logo = team.logo,
                    record = record,
                    league = league,
                    sport = sport,
                    rank = rank,
                    netRating = stats["netRating"] ?: stats["pointDifferential"] ?: "",
                    location = team.location,
                ))
            }
        }

        // Flatten the standings structure — handles both flat and grouped responses
        for (container in response.standings) {
            if (container.entries.isNotEmpty()) {
                extractTeams(container.entries, container.name)
            }
            container.groups?.forEach { group ->
                if (group.entries.isNotEmpty()) {
                    extractTeams(group.entries, "${container.name} - ${group.name}")
                }
            }
        }
        return result
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

    /** Fetch standings preserving group/table structure and full stats per row. */
    suspend fun fetchStandingsGroups(sport: String, league: String, season: Int? = null): List<StandingsGroup> {
        if (season != null) {
            return fetchStandingsGroupsForSeason(sport, league, season)
        }
        // Auto: try the current season, then walk back up to 3 years when the
        // current season has no tables published yet.
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        for (year in 0..3) {
            val groups = fetchStandingsGroupsForSeason(sport, league, currentYear - year)
            if (groups.isNotEmpty()) return groups
        }
        return emptyList()
    }

    private suspend fun fetchStandingsGroupsForSeason(sport: String, league: String, year: Int): List<StandingsGroup> {
        val urls = listOf(
            "$BASE/$sport/$league/standings?season=$year",
            "$BASE/$sport/$league/standings",
        )
        for (url in urls) {
            try {
                val parsed = json.decodeFromString<EspnStandingsResponse>(httpGetText(url))
                val groups = parsed.standings.mapNotNull { container ->
                    val entries = mutableListOf<StandingsRow>()
                    container.entries.forEachIndexed { i, e ->
                        val team = e.team ?: return@forEachIndexed
                        entries.add(StandingsRow(
                            teamId = team.id, teamName = team.displayName.ifBlank { team.name },
                            abbreviation = team.abbreviation, logo = team.logo, color = team.color,
                            rank = i + 1,
                            stats = e.stats?.associate { it.name to it.displayValue } ?: emptyMap(),
                        ))
                    }
                    container.groups?.forEach { g ->
                        g.entries.forEachIndexed { i, e ->
                            val team = e.team ?: return@forEachIndexed
                            entries.add(StandingsRow(
                                teamId = team.id, teamName = team.displayName.ifBlank { team.name },
                                abbreviation = team.abbreviation, logo = team.logo, color = team.color,
                                rank = i + 1,
                                stats = e.stats?.associate { it.name to it.displayValue } ?: emptyMap(),
                            ))
                        }
                    }
                    if (entries.isEmpty()) null else StandingsGroup(name = container.name, rows = entries)
                }
                if (groups.isNotEmpty()) return groups
            } catch (_: Exception) { continue }
        }
        return emptyList()
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
                strTime = e.detail.ifBlank { e.timeStr ?: "" },
                strThumb = e.homeLogo ?: e.awayLogo,
                strChannel = e.channel,
                intHomeScore = e.homeScore,
                intAwayScore = e.awayScore,
                strStatus = if (e.isLive) "LIVE" else if (e.status.contains("FINAL")) "FINAL" else "SCHEDULED",
                strFilename = if (e.isPpv) "PPV" else null,
            )
        }
    }

    fun findAllMatchingChannels(
        event: SportEvent,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<IptvChannel> {
        return findScoredMatchingChannels(event, channels, currentEpgTitleFor).map { it.channel }
    }

    /**
     * Scored variant of [findAllMatchingChannels] for the picker UI: every candidate plus its
     * score and the reasons it matched, sorted descending so the picker can show confidence.
     */
    fun findScoredMatchingChannels(
        event: SportEvent,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<ChannelScore> {
        if (event.strHomeTeam.isBlank() && event.strAwayTeam.isBlank()) return emptyList()
        val target = MatchTarget(
            homeTeam = event.strHomeTeam,
            awayTeam = event.strAwayTeam,
            league = event.strLeague,
            sport = event.strSport,
        )
        return ChannelScorer.scoreChannels(target, channels, currentEpgTitleFor)
    }

    /**
     * Two-pass scored variant for the picker/autoplay path: first score on name/league/generic,
     * then lazily fetch current-program EPG for the top [cap] candidates and re-score, so a channel
     * whose EPG shows the matchup outranks an unrelated generic sports channel. Never blocks the
     * main list on EPG for every channel.
     */
    suspend fun findScoredMatchingChannelsWithLazyEpg(
        event: SportEvent,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
        cap: Int = 8,
    ): List<ChannelScore> {
        if (event.strHomeTeam.isBlank() && event.strAwayTeam.isBlank()) return emptyList()
        val target = MatchTarget(
            homeTeam = event.strHomeTeam,
            awayTeam = event.strAwayTeam,
            league = event.strLeague,
            sport = event.strSport,
        )
        val fast = ChannelScorer.scoreChannels(target, channels, currentEpgTitleFor)
        val candidates = fast.filter { it.score > 0 }.map { it.channel }
        val lazyLookup = IptvRepository.buildLazyEpgTitleLookup(candidates, cap)
        return ChannelScorer.scoreChannels(target, channels, currentEpgTitleFor = { name ->
            val lazy = lazyLookup(name)
            if (lazy.isNotEmpty()) lazy else currentEpgTitleFor(name)
        })
    }

    internal fun matchSportEventsToChannels(
        sportEvents: List<SportEvent>,
        channels: List<IptvChannel>,
        currentEpgTitleFor: (String) -> String = { "" },
    ): List<MatchedSportEvent> {
        val matched = mutableListOf<MatchedSportEvent>()
        for (event in sportEvents) {
            if (event.strHomeTeam.isBlank() && event.strAwayTeam.isBlank()) continue
            val target = MatchTarget(
                homeTeam = event.strHomeTeam,
                awayTeam = event.strAwayTeam,
                league = event.strLeague,
                sport = event.strSport,
            )
            val top = ChannelScorer.scoreChannels(target, channels, currentEpgTitleFor).firstOrNull()
            if (top != null) {
                matched.add(MatchedSportEvent(event = event, channel = top.channel))
            }
        }
        return matched
    }

    /**
     * Extracts time from an ISO 8601 timestamp and formats to 12-hour AM/PM.
     * ESPN returns UTC times like "2026-07-11T19:00Z" or "2026-07-11T23:00:00Z".
     */
    private fun extractTime12h(iso: String): String? {
        val timePart = iso.substringAfter("T", "").substringBefore("Z").substringBefore("+").substringBeforeLast("-")
            .takeWhile { it.isDigit() || it == ':' }
        if (timePart.isBlank()) return null
        val parts = timePart.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        // ESPN often returns midnight (00:00) as a placeholder for fighting events — skip it
        if (hour == 0 && minute == 0) return null
        val offsetMinutes = (TraktPlatformClock.localTimezoneOffsetMs() / 60_000L).toInt()
        val localTotalMinutes = (hour * 60 + minute + offsetMinutes) % (24 * 60)
        val localHour = (localTotalMinutes / 60 + 24) % 24
        val localMinute = localTotalMinutes % 60
        val amPm = if (localHour < 12) "AM" else "PM"
        val hour12 = when {
            localHour == 0 -> 12
            localHour > 12 -> localHour - 12
            else -> localHour
        }
        val minStr = localMinute.toString().padStart(2, '0')
        return "$hour12:$minStr $amPm"
    }
}

// (SportBroadcasterMap retired: its league/sport broadcaster terms are covered by
//  GameToChannelMatcher leagueKeywords + ChannelScorer general-sports matching, so keeping
//  a second term map would reintroduce the dual-implementation drift the scorer removed.)
