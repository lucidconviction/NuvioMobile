package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class BdlGame(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val startTime: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val isLive: Boolean,
)

object BallDontLieClient {

    private val baseUrl = "https://api.balldontlie.io/v1"
    private val apiKey = "eaff4f7e-29e9-47b3-93bc-4b44bc2e7f14"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchArray(path: String): JsonArray? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl$path",
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
                        "X-API-Key" to apiKey,
                    ),
                )
                Json.parseToJsonElement(body).jsonObject["data"]?.jsonArray
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchGames(
        dates: List<String> = emptyList(),
        seasons: List<Int> = emptyList(),
    ): List<BdlGame> {
        val params = mutableListOf<String>()
        dates.forEach { params.add("dates[]=$it") }
        seasons.forEach { params.add("seasons[]=$it") }
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val arr = fetchArray("/games$query") ?: return emptyList()
        val result = mutableListOf<BdlGame>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    BdlGame(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["home_team"]?.jsonObject?.get("full_name")?.jsonPrimitive?.contentOrNull
                            ?: obj["homeTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["away_team"]?.jsonObject?.get("full_name")?.jsonPrimitive?.contentOrNull
                            ?: obj["awayTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull
                            ?: obj["date"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeScore = obj["home_team_score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        awayScore = obj["away_team_score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        isLive = obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()?.contains("live") ?: false,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchGameById(id: String): BdlGame? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl/games/$id",
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36",
                        "X-API-Key" to apiKey,
                    ),
                )
                val obj = Json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
                if (obj == null) return@withTimeout null
                BdlGame(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: id,
                    homeTeam = obj["home_team"]?.jsonObject?.get("full_name")?.jsonPrimitive?.contentOrNull
                        ?: obj["homeTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                    awayTeam = obj["away_team"]?.jsonObject?.get("full_name")?.jsonPrimitive?.contentOrNull
                        ?: obj["awayTeam"]?.jsonPrimitive?.contentOrNull ?: "",
                    startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull
                        ?: obj["date"]?.jsonPrimitive?.contentOrNull ?: "",
                    homeScore = obj["home_team_score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    awayScore = obj["away_team_score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                    isLive = obj["status"]?.jsonPrimitive?.contentOrNull?.lowercase()?.contains("live") ?: false,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPlayers(): List<String> {
        val arr = fetchArray("/players") ?: return emptyList()
        val result = mutableListOf<String>()
        try {
            for (el in arr) {
                val name = el.jsonObject["first_name"]?.jsonPrimitive?.contentOrNull?.plus(" ")
                    ?.plus(el.jsonObject["last_name"]?.jsonPrimitive?.contentOrNull ?: "")
                    ?: el.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                if (!name.isNullOrBlank()) result.add(name)
            }
        } catch (_: Exception) {}
        return result
    }
}