package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class WeStreamMatch(
    val id: String,
    val title: String,
    val sport: String,
    val homeTeam: String,
    val awayTeam: String,
    val startTime: Long,
    val streamUrl: String,
)

object WeStreamClient {

    private val baseUrl = "https://westream.su"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchJson(path: String): JsonArray? {
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

    suspend fun fetchLiveMatches(): List<WeStreamMatch> {
        val arr = fetchJson("/api/matches/live") ?: return emptyList()
        val result = mutableListOf<WeStreamMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    WeStreamMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                        sport = obj["sport"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["home"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["away"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        streamUrl = obj["stream_url"]?.jsonPrimitive?.contentOrNull
                            ?: obj["embed_url"]?.jsonPrimitive?.contentOrNull ?: "",
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchMatchesBySport(sport: String): List<WeStreamMatch> {
        val encoded = sport.replace(" ", "%20")
        val arr = fetchJson("/api/matches?sport=$encoded") ?: return emptyList()
        val result = mutableListOf<WeStreamMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    WeStreamMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                        sport = obj["sport"]?.jsonPrimitive?.contentOrNull ?: sport,
                        homeTeam = obj["home"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["away"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        streamUrl = obj["stream_url"]?.jsonPrimitive?.contentOrNull
                            ?: obj["embed_url"]?.jsonPrimitive?.contentOrNull ?: "",
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchAvailableSports(): List<String> {
        val arr = fetchJson("/api/sports") ?: return emptyList()
        val result = mutableListOf<String>()
        try {
            for (el in arr) {
                val name = el.jsonObject["sport"]?.jsonPrimitive?.contentOrNull
                    ?: el.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                if (!name.isNullOrBlank()) result.add(name)
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchStreamUrl(source: String, id: String): String? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl/api/stream/$source/$id",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                val obj = Json.parseToJsonElement(body).jsonObject
                obj["stream_url"]?.jsonPrimitive?.contentOrNull
                    ?: obj["embed_url"]?.jsonPrimitive?.contentOrNull
            }
        } catch (_: Exception) {
            null
        }
    }
}