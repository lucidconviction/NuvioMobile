package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json

object EspnClient {
    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports"
    private val json = Json { ignoreUnknownKeys = true }

    private val sports = listOf(
        "football/nfl",
        "football/college-football",
        "basketball/nba",
        "basketball/mens-college-basketball",
        "baseball/mlb",
        "hockey/nhl",
        "soccer/usa.1",
        "soccer/eng.1",
        "fighting/ufc",
        "fighting/boxing",
        "fighting/pfl",
        "fighting/bellator",
    )

    suspend fun fetchAll(): List<EspnProcessedEvent> {
        val all = mutableListOf<EspnProcessedEvent>()
        for (sport in sports) {
            try {
                val url = "$BASE/$sport/scoreboard"
                val response = httpGetText(url)
                val parsed = json.decodeFromString<EspnResponse>(response)
                val processed = parsed.events.flatMap { event ->
                    processEvent(event, sport)
                }.filter { e ->
                    e.isLive || e.status != "STATUS_FINAL" || detailWithinHours(e.detail, 3)
                }
                all.addAll(processed)
            } catch (_: Exception) { }
        }
        return all
    }

    private fun detailWithinHours(detail: String, hours: Int): Boolean {
        if (detail.isBlank()) return false
        val lower = detail.lowercase()
        if (lower.contains("final") || lower.contains("end")) return false
        return true
    }

    private fun processEvent(event: EspnEvent, sportPath: String): List<EspnProcessedEvent> {
        val sport = sportPath.split("/").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Sport"
        val league = sportPath.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { it.uppercase() } ?: ""

        return event.competitions.mapNotNull { comp ->
            val home = comp.competitors.find { it.homeAway == "home" }
            val away = comp.competitors.find { it.homeAway == "away" }

            val channel = comp.broadcasts?.firstOrNull()
                ?.names?.firstOrNull() ?: ""

            val isPpv = channel.contains("PPV", ignoreCase = true) ||
                comp.notes?.any { it.type == "ppv" || it.headline.contains("PPV", ignoreCase = true) } == true

            val statusName = comp.status?.type?.name ?: ""
            val isLive = statusName == "STATUS_IN_PROGRESS"
            val detail = comp.status?.type?.detail ?: ""

            val dateStr = (event.date.takeIf { it.isNotBlank() } ?: comp.date).take(10)

            EspnProcessedEvent(
                id = event.id + "_" + comp.id,
                title = event.shortName.ifBlank { event.name },
                homeTeam = home?.team?.displayName ?: home?.team?.name ?: "",
                awayTeam = away?.team?.displayName ?: away?.team?.name ?: "",
                homeScore = home?.score,
                awayScore = away?.score,
                homeLogo = home?.team?.logo,
                awayLogo = away?.team?.logo,
                channel = channel,
                status = statusName,
                detail = detail,
                date = dateStr,
                sport = sport,
                league = league,
                isLive = isLive,
                isPpv = isPpv,
            )
        }
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

    internal fun matchSportEventsToChannels(
        sportEvents: List<SportEvent>,
        channels: List<IptvChannel>,
    ): List<MatchedSportEvent> {
        val matched = mutableListOf<MatchedSportEvent>()
        for (event in sportEvents) {
            if (event.strChannel.isBlank()) continue
            val apiName = event.strChannel.lowercase().trim()
            val apiWords = apiName.split(" ").filter { it.length > 2 }
            val found = channels.firstOrNull { ch ->
                val chName = ch.name.lowercase().trim()
                chName.contains(apiName) || apiName.contains(chName) ||
                apiWords.any { word -> chName.contains(word) } ||
                chName.split(" ").any { word -> word.length > 2 && apiName.contains(word) }
            }
            if (found != null) {
                matched.add(MatchedSportEvent(event = event, channel = found))
            }
        }
        return matched
    }
}
