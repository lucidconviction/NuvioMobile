package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class OpenLigaDBMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val startTime: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val isLive: Boolean,
)

object OpenLigaDBClient {

    private val baseUrl = "https://openligadb.github.io/api"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchArray(path: String): JsonArray? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl$path",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                Json.parseToJsonElement(body).jsonArray
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchLiveMatches(): List<OpenLigaDBMatch> {
        val arr = fetchArray("/matchdata/live") ?: return emptyList()
        val result = mutableListOf<OpenLigaDBMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    OpenLigaDBMatch(
                        id = obj["MatchID"]?.jsonPrimitive?.contentOrNull
                            ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["Team1"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                            ?: obj["homeTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["Team2"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                            ?: obj["awayTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["MatchDateTimeUTC"]?.jsonPrimitive?.contentOrNull
                            ?: obj["startTime"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeScore = obj["Score1"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        awayScore = obj["Score2"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        isLive = obj["isLive"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchMatchById(id: String): OpenLigaDBMatch? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl/matchdata/$id",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                val obj = Json.parseToJsonElement(body).jsonObject
                OpenLigaDBMatch(
                    id = obj["MatchID"]?.jsonPrimitive?.contentOrNull
                        ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: id,
                    homeTeam = obj["Team1"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                        ?: obj["homeTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                    awayTeam = obj["Team2"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                        ?: obj["awayTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                    startTime = obj["MatchDateTimeUTC"]?.jsonPrimitive?.contentOrNull
                        ?: obj["startTime"]?.jsonPrimitive?.contentOrNull ?: "",
                    homeScore = obj["Score1"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    awayScore = obj["Score2"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    isLive = obj["isLive"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchLeagueMatches(seasonId: String): List<OpenLigaDBMatch> {
        val arr = fetchArray("/matchdata/?germanBundesliga/$seasonId") ?: return emptyList()
        val result = mutableListOf<OpenLigaDBMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    OpenLigaDBMatch(
                        id = obj["MatchID"]?.jsonPrimitive?.contentOrNull
                            ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["Team1"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                            ?: obj["homeTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["Team2"]?.jsonObject?.get("TeamName")?.jsonPrimitive?.contentOrNull
                            ?: obj["awayTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["MatchDateTimeUTC"]?.jsonPrimitive?.contentOrNull
                            ?: obj["startTime"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeScore = obj["Score1"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        awayScore = obj["Score2"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        isLive = obj["isLive"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchTeams(): List<String> {
        val arr = fetchArray("/teams") ?: return emptyList()
        val result = mutableListOf<String>()
        try {
            for (el in arr) {
                val name = el.jsonObject["TeamName"]?.jsonPrimitive?.contentOrNull
                    ?: el.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                if (!name.isNullOrBlank()) result.add(name)
            }
        } catch (_: Exception) {}
        return result
    }
}