package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class SportSRCMatch(
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val league: String,
    val startTime: Long,
    val score: String,
)

object SportSRCClient {

    private val baseUrl = "https://api.sportsrc.org"
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

    suspend fun fetchSports(): List<String> {
        val arr = fetchJson("?data=sports") ?: return emptyList()
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

    suspend fun fetchMatches(category: String): List<SportSRCMatch> {
        val encoded = category.replace(" ", "%20")
        val arr = fetchJson("?data=matches&category=$encoded") ?: return emptyList()
        val result = mutableListOf<SportSRCMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    SportSRCMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["home_team"]?.jsonPrimitive?.contentOrNull
                            ?: obj["home"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["away_team"]?.jsonPrimitive?.contentOrNull
                            ?: obj["away"]?.jsonPrimitive?.contentOrNull ?: "",
                        league = obj["league"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                            ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        score = obj["score"]?.jsonPrimitive?.contentOrNull ?: "",
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchResults(): List<SportSRCMatch> {
        val arr = fetchJson("?data=results") ?: return emptyList()
        val result = mutableListOf<SportSRCMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    SportSRCMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        homeTeam = obj["home_team"]?.jsonPrimitive?.contentOrNull
                            ?: obj["home"]?.jsonPrimitive?.contentOrNull ?: "",
                        awayTeam = obj["away_team"]?.jsonPrimitive?.contentOrNull
                            ?: obj["away"]?.jsonPrimitive?.contentOrNull ?: "",
                        league = obj["league"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                            ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        score = obj["score"]?.jsonPrimitive?.contentOrNull ?: "",
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchMatchDetail(id: String, category: String): SportSRCMatch? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl?data=detail&id=$id&category=$category",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                val obj = Json.parseToJsonElement(body).jsonObject
                SportSRCMatch(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: id,
                    homeTeam = obj["home_team"]?.jsonPrimitive?.contentOrNull
                        ?: obj["home"]?.jsonPrimitive?.contentOrNull ?: "",
                    awayTeam = obj["away_team"]?.jsonPrimitive?.contentOrNull
                        ?: obj["away"]?.jsonPrimitive?.contentOrNull ?: "",
                    league = obj["league"]?.jsonPrimitive?.contentOrNull ?: "",
                    startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                    score = obj["score"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    data class StreamSource(
        val id: String,
        val embedUrl: String,
        val language: String,
        val hd: Boolean,
    )

    suspend fun fetchMatchStreams(id: String, category: String): List<StreamSource> {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl?data=detail&id=$id&category=$category",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                val obj = Json.parseToJsonElement(body).jsonObject
                val sources = obj["sources"]?.jsonArray
                val result = mutableListOf<StreamSource>()
                if (sources != null) {
                    for (el in sources) {
                        val s = el.jsonObject
                        val url = s["embedUrl"]?.jsonPrimitive?.contentOrNull
                        if (!url.isNullOrBlank()) {
                            result.add(StreamSource(
                                id = s["id"]?.jsonPrimitive?.contentOrNull ?: "",
                                embedUrl = url,
                                language = s["language"]?.jsonPrimitive?.contentOrNull ?: "",
                                hd = s["hd"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                            ))
                        }
                    }
                }
                result
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}