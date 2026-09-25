package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class EmbedSportexMatch(
    val id: String,
    val title: String,
    val category: String,
    val startTime: Long,
    val embedUrl: String,
    val isLive: Boolean,
)

object EmbedSportexClient {

    private val baseUrl = "https://api.esportex.site"
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

    suspend fun fetchLiveMatches(): List<EmbedSportexMatch> {
        val arr = fetchJson("/matches/live") ?: return emptyList()
        val result = mutableListOf<EmbedSportexMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    EmbedSportexMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        title = obj["title"]?.jsonPrimitive?.contentOrNull
                            ?: "${obj["home"]?.jsonPrimitive?.contentOrNull ?: ""} vs ${obj["away"]?.jsonPrimitive?.contentOrNull ?: ""}",
                        category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                            ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        embedUrl = obj["embed_url"]?.jsonPrimitive?.contentOrNull
                            ?: obj["stream_url"]?.jsonPrimitive?.contentOrNull ?: "",
                        isLive = true,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchUpcomingMatches(): List<EmbedSportexMatch> {
        val arr = fetchJson("/matches/upcoming") ?: return emptyList()
        val result = mutableListOf<EmbedSportexMatch>()
        try {
            for (el in arr) {
                val obj = el.jsonObject
                result.add(
                    EmbedSportexMatch(
                        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                        title = obj["title"]?.jsonPrimitive?.contentOrNull
                            ?: "${obj["home"]?.jsonPrimitive?.contentOrNull ?: ""} vs ${obj["away"]?.jsonPrimitive?.contentOrNull ?: ""}",
                        category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "",
                        startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                            ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                        embedUrl = obj["embed_url"]?.jsonPrimitive?.contentOrNull
                            ?: obj["stream_url"]?.jsonPrimitive?.contentOrNull ?: "",
                        isLive = false,
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchCategories(): List<String> {
        val arr = fetchJson("/categories") ?: return emptyList()
        val result = mutableListOf<String>()
        try {
            for (el in arr) {
                val name = el.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    ?: el.jsonObject["category"]?.jsonPrimitive?.contentOrNull
                if (!name.isNullOrBlank()) result.add(name)
            }
        } catch (_: Exception) {}
        return result
    }

    suspend fun fetchMatchById(id: String): EmbedSportexMatch? {
        return try {
            withTimeout(15_000) {
                val body = httpGetTextWithHeaders(
                    "$baseUrl/matches/$id",
                    mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"),
                )
                val obj = Json.parseToJsonElement(body).jsonObject
                EmbedSportexMatch(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: id,
                    title = obj["title"]?.jsonPrimitive?.contentOrNull
                        ?: "${obj["home"]?.jsonPrimitive?.contentOrNull ?: ""} vs ${obj["away"]?.jsonPrimitive?.contentOrNull ?: ""}",
                    category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "",
                    startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                        ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
                    embedUrl = obj["embed_url"]?.jsonPrimitive?.contentOrNull
                        ?: obj["stream_url"]?.jsonPrimitive?.contentOrNull ?: "",
                    isLive = obj["is_live"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}