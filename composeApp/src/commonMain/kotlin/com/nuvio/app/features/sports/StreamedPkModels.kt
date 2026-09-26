package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class StreamedPkMatch(
    val id: String,
    val title: String,
    val category: String,
    val date: Long,
    val poster: String? = null,
    val popular: Boolean = false,
    val teams: StreamedPkTeams? = null,
    val sources: List<StreamedPkSource> = emptyList(),
) {
    val startEpochMs: Long = date
    val isLive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now >= startEpochMs - 3600000 && now < startEpochMs + 7200000
        }
}

@Serializable
data class StreamedPkTeams(
    val home: StreamedPkTeam? = null,
    val away: StreamedPkTeam? = null,
)

@Serializable
data class StreamedPkTeam(
    val name: String,
    val badge: String? = null,
)

@Serializable
data class StreamedPkSource(
    val source: String,
    val id: String,
)

@Serializable
data class StreamedPkStream(
    val id: String,
    val streamNo: Int,
    val language: String,
    val hd: Boolean,
    val embedUrl: String,
    val source: String,
)

object StreamedPkClient {

    private val baseUrl = "https://streamed.pk"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun fetchJsonArray(path: String): JsonArray? {
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
        val arr = fetchJsonArray("/api/sports") ?: return emptyList()
        val result = mutableListOf<String>()
        for (el in arr) {
            val obj = el.jsonObject
            val name = obj["id"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
            if (!name.isNullOrBlank()) result.add(name)
        }
        return result
    }

    suspend fun fetchMatches(sport: String): List<StreamedPkMatch> {
        val encoded = sport.replace(" ", "%20")
        val arr = fetchJsonArray("/api/matches/$encoded") ?: return emptyList()
        val result = mutableListOf<StreamedPkMatch>()
        for (el in arr) {
            try {
                result.add(json.decodeFromJsonElement(StreamedPkMatch.serializer(), el))
            } catch (_: Exception) {}
        }
        return result
    }

    suspend fun fetchStreams(source: String, id: String): List<StreamedPkStream> {
        val arr = fetchJsonArray("/api/stream/$source/$id") ?: return emptyList()
        val result = mutableListOf<StreamedPkStream>()
        for (el in arr) {
            try {
                result.add(json.decodeFromJsonElement(StreamedPkStream.serializer(), el))
            } catch (_: Exception) {}
        }
        return result
    }
}