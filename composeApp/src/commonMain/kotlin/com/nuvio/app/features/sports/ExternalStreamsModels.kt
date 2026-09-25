package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout
import kotlin.time.TimeSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@Serializable
data class ExternalLiveEvent(
    val id: String,
    val title: String,
    val category: String,
    val date: Long,
    val poster: String? = null,
    val provider: String,
    val streamUrl: String,
    val homeTeam: String? = null,
    val awayTeam: String? = null,
    val tag: String? = null,
    val sourceTag: String? = null,
)

val ExternalLiveEvent.isLive: Boolean
    get() = date == 0L || (date / 1000L <= (TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds / 1000) + 3600)

object ExternalStreamsClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")

    suspend fun fetchAll(): List<ExternalLiveEvent> {
        val results = mutableListOf<ExternalLiveEvent>()
        results.addAll(fetchStreamedPkAll())
        results.addAll(fetchPpvStAll())
        return results.sortedWith(compareBy({ !it.isLive }, { it.date }))
    }

    // ── streamed.pk ──────────────────────────────────────────────────────────
    private suspend fun fetchStreamedPkAll(): List<ExternalLiveEvent> {
        val sports = listOf("football", "american-football", "hockey", "basketball", "fight")
        val results = mutableListOf<ExternalLiveEvent>()
        for (sport in sports) {
            try {
                val url = "https://streamed.pk/api/matches/$sport"
                val response = withTimeout(10_000) { httpGetTextWithHeaders(url, headers) }
                val arr = json.parseToJsonElement(response) as JsonArray
                arr.mapNotNull { el ->
                    try {
                        val obj = el as JsonObject
                        val sources = obj["sources"] as JsonArray? ?: return@mapNotNull null
                        val matchId = (obj["id"] as JsonPrimitive?)?.content ?: ""
                        val title = (obj["title"] as JsonPrimitive?)?.content ?: ""
                        val category = (obj["category"] as JsonPrimitive?)?.content ?: sport
                        val date = (obj["date"] as JsonPrimitive?)?.content?.toLongOrNull() ?: 0L
                        val poster = (obj["poster"] as JsonPrimitive?)?.content
                        val homeTeam = (obj["teams"] as JsonObject?)?.get("home")?.jsonObject?.get("name")?.let { it as JsonPrimitive }?.content
                        val awayTeam = (obj["teams"] as JsonObject?)?.get("away")?.jsonObject?.get("name")?.let { it as JsonPrimitive }?.content

                        val firstSource = sources.firstOrNull() as JsonObject? ?: return@mapNotNull null
                        val src = firstSource["source"] as JsonPrimitive? ?: return@mapNotNull null
                        val sid = firstSource["id"] as JsonPrimitive? ?: return@mapNotNull null
                        val embedUrl = try {
                            val streamUrl = "https://streamed.pk/api/stream/${src.content}/${sid.content}"
                            val streamResp = withTimeout(8_000) { httpGetTextWithHeaders(streamUrl, headers) }
                            val streamArr = json.parseToJsonElement(streamResp) as JsonArray
                            (streamArr.firstOrNull() as? JsonObject)?.get("embedUrl")?.let {
                                (it as JsonPrimitive).content
                            } ?: ""
                        } catch (_: Exception) { "" }

                        if (embedUrl.isBlank()) return@mapNotNull null

                        ExternalLiveEvent(
                            id = matchId,
                            title = title,
                            category = category,
                            date = date,
                            poster = poster?.let { "https://streamed.pk$it" },
                            provider = "streamed.pk",
                            streamUrl = embedUrl,
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                        )
                    } catch (e: Exception) { null }
                }.also { results.addAll(it) }
            } catch (e: Exception) { /* skip this sport */ }
        }
        return results
    }

    // ── ppv.st (api.ppv.st) ──────────────────────────────────────────────────
    private suspend fun fetchPpvStAll(): List<ExternalLiveEvent> {
        return try {
            val url = "https://api.ppv.st/api/streams"
            val response = withTimeout(20_000) { httpGetTextWithHeaders(url, headers) }
            val root = json.parseToJsonElement(response) as JsonObject
            val categories = root["streams"] as JsonArray? ?: return emptyList()
            categories.mapNotNull { catEl ->
                try {
                    val cat = catEl as JsonObject
                    val catName = (cat["category"] as JsonPrimitive?)?.content ?: ""
                    val streams = cat["streams"] as JsonArray? ?: return@mapNotNull null
                    streams.mapNotNull { sEl ->
                        try {
                            val s = sEl as JsonObject
                            val name = (s["name"] as JsonPrimitive?)?.content ?: ""
                            val tag = (s["tag"] as JsonPrimitive?)?.content ?: ""
                            val poster = (s["poster"] as JsonPrimitive?)?.content
                            val iframe = (s["iframe"] as JsonPrimitive?)?.content ?: ""
                            val alwaysLiveStr = (s["always_live"] as JsonPrimitive?)?.content ?: "0"
                            val alwaysLive = alwaysLiveStr == "1" || alwaysLiveStr.equals("true", ignoreCase = true)
                            val startsAt = (s["starts_at"] as JsonPrimitive?)?.content?.toLongOrNull() ?: 0L
                            val endsAt = (s["ends_at"] as JsonPrimitive?)?.content?.toLongOrNull() ?: 0L
                            val sourceTag = (s["source_tag"] as JsonPrimitive?)?.content ?: ""

                            if (iframe.isBlank()) return@mapNotNull null

                            ExternalLiveEvent(
                                id = (s["id"] as JsonPrimitive?)?.content ?: "",
                                title = name,
                                category = catName,
                                date = if (startsAt > 0) startsAt * 1000 else 0,
                                poster = poster,
                                provider = "ppv.st",
                                streamUrl = iframe,
                                homeTeam = null,
                                awayTeam = null,
                                tag = tag,
                                sourceTag = sourceTag,
                            )
                        } catch (_: Exception) { null }
                    }
                } catch (_: Exception) { null }
            }.flatten()
        } catch (e: Exception) { emptyList() }
    }
}