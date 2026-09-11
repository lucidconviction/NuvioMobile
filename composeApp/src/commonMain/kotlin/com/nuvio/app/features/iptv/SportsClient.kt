package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.sports.StandingsGroup
import com.nuvio.app.features.sports.StandingsRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SportsClient {
    private const val BASE = "https://www.thesportsdb.com/api/v1/json/3"
    private const val API_KEY = "3"
    private val json = Json { ignoreUnknownKeys = true }

    /** TheSportsDB league ids for leagues we surface in the app. */
    private val leagueIds = mapOf(
        "epl" to 4328, "mls" to 4359, "laliga" to 4335, "seriea" to 4332,
        "bundes" to 4331, "ligue1" to 4334, "ucl" to 4337, "nfl" to 4391,
        "nba" to 4387, "mlb" to 4429, "nhl" to 4380,
    )

    fun getLeagueId(leagueSlug: String): Int? {
        val key = leagueSlug.substringAfterLast("/", leagueSlug).lowercase()
        return leagueIds[key] ?: leagueIds[leagueSlug.lowercase()]
    }

    /** Standings table for a league (current season) — free tier, mainly soccer. */
    suspend fun fetchStandings(leagueSlug: String): List<StandingsGroup> {
        val id = getLeagueId(leagueSlug) ?: return emptyList()
        return try {
            val url = "$BASE/lookuptable.php?id=$id"
            val root = json.parseToJsonElement(httpGetText(url)).jsonObject
            val rows = root["table"]?.jsonArray?.mapNotNull { el ->
                val o = el.jsonObject
                StandingsRow(
                    teamId = o["idStanding"]?.jsonPrimitive?.content ?: "",
                    teamName = o["strTeam"]?.jsonPrimitive?.content ?: "",
                    abbreviation = o["strTeamShort"]?.jsonPrimitive?.content ?: "",
                    logo = o["strBadge"]?.jsonPrimitive?.content ?: "",
                    rank = o["intRank"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    stats = buildMap {
                        o["intPlayed"]?.let { put("played", it.jsonPrimitive.content) }
                        o["intWin"]?.let { put("wins", it.jsonPrimitive.content) }
                        o["intDraw"]?.let { put("draws", it.jsonPrimitive.content) }
                        o["intLoss"]?.let { put("losses", it.jsonPrimitive.content) }
                        o["intGoalsFor"]?.let { put("goalsFor", it.jsonPrimitive.content) }
                        o["intGoalsAgainst"]?.let { put("goalsAgainst", it.jsonPrimitive.content) }
                        o["intGoalDifference"]?.let { put("goalDifference", it.jsonPrimitive.content) }
                        o["intPoints"]?.let { put("points", it.jsonPrimitive.content) }
                    },
                )
            } ?: emptyList()
            if (rows.isEmpty()) emptyList() else listOf(StandingsGroup(name = "Standings", rows = rows))
        } catch (e: Exception) {
            println("SportsClient: fetchStandings error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchTodaysEvents(date: String): List<SportEvent> {
        return try {
            val url = "$BASE/eventstv.php?d=$date"
            println("SportsClient: fetching $url")
            val response = httpGetText(url)
            println("SportsClient: response length=${response.length}")
            val root = json.parseToJsonElement(response).jsonObject
            val eventsArray = root["tvevents"]?.jsonArray ?: root["events"]?.jsonArray
            if (eventsArray == null) {
                println("SportsClient: no events array found, keys=${root.keys}")
                return emptyList()
            }
            println("SportsClient: found ${eventsArray.size} events")
            val parsed = eventsArray.mapNotNull { element ->
                try {
                    json.decodeFromJsonElement<SportEvent>(element)
                } catch (e: Exception) {
                    println("SportsClient: parse error: ${e.message}")
                    null
                }
            }
            println("SportsClient: parsed ${parsed.size} events")
            parsed
        } catch (e: Exception) {
            println("SportsClient: fetch error: ${e.message}")
            emptyList()
        }
    }

}
