package com.nuvio.app.features.ai

import com.nuvio.app.features.addons.httpPostJson
import com.nuvio.app.features.sports.BroadcastRegion
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.sports.MatchedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private val CHANNEL_SEARCH_SYSTEM_PROMPT = """
You are a sports channel discovery assistant for an IPTV app. Given a sporting event, 
a user's IPTV channels, their matched channels, a natural language query, and their region,
explain which channels best match the query and why. Return a JSON object with:
- "explanation": 2-3 sentences explaining the match
- "suggested_portals": list of unique provider group names from the channels (max 5)
Do NOT return channel URLs or full channel lists — the app already has those.
Keep explanations concise and practical.
""".trimIndent()

actual object AiChannelSearchStore {
    actual fun searchChannels(
        event: EspnProcessedEvent,
        allChannels: List<IptvChannel>,
        matchedChannels: List<MatchedChannel>,
        query: String,
        region: BroadcastRegion,
    ): Flow<AiChannelSearchResult> = flow {
        val regionName = when (region) {
            BroadcastRegion.US -> "United States"
            BroadcastRegion.UK -> "United Kingdom"
            BroadcastRegion.CA -> "Canada"
            BroadcastRegion.EUROPE -> "Europe"
            else -> "Global"
        }

        val context = buildString {
            append("Event: ${event.homeTeam} vs ${event.awayTeam} in ${event.league}\n")
            append("Region: $regionName\n")
            append("Query: $query\n")
            if (matchedChannels.isNotEmpty()) {
                append("Available channels (${matchedChannels.size}): ")
                append(matchedChannels.take(10).joinToString(", ") { "${it.channel.name} (${it.providerGroup.label})" })
                if (matchedChannels.size > 10) append("... and ${matchedChannels.size - 10} more")
            } else {
                append("No IPTV channels matched this event.\n")
            }
            append("Suggested portals from channel groups: ")
            val portalGroups = matchedChannels.mapNotNull { it.providerGroup.label }.distinct()
            append(if (portalGroups.isNotEmpty()) portalGroups.joinToString(", ") else "none available")
        }

        val start = System.currentTimeMillis()
        var explanation = "No AI results available."
        var suggestedPortals = emptyList<String>()

        try {
            val body = """
                {
                    "model": "auto",
                    "messages": [
                        {"role": "system", "content": "$CHANNEL_SEARCH_SYSTEM_PROMPT"},
                        {"role": "user", "content": "$context"}
                    ],
                    "temperature": 0.5,
                    "max_tokens": 512
                }
            """.trimIndent()

            val raw = httpPostJson("${AiClient.DEFAULT_BASE_URL}/chat/completions", body)
            val response = runCatching { json.decodeFromString<AiChatCompletionResponse>(raw) }
            val text = response.getOrNull()?.choices?.firstOrNull()?.message?.content ?: ""

            if (text.isNotBlank()) {
                val jsonStart = text.indexOf('{')
                val jsonEnd = text.lastIndexOf('}') + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val jsonStr = text.substring(jsonStart, jsonEnd)
                    val parsed = runCatching {
                        json.decodeFromString<Map<String, Any>>(jsonStr)
                    }
                    if (parsed.isSuccess) {
                        explanation = (parsed.getOrNull()?.get("explanation") as? String) ?: text
                        val portals = (parsed.getOrNull()?.get("suggested_portals") as? List<*>)
                            ?.filterIsInstance<String>()
                            ?: emptyList()
                        suggestedPortals = portals.take(5)
                    } else {
                        explanation = text
                    }
                } else {
                    explanation = text
                }
            }
        } catch (_: Exception) {
            // Fall back to empty result
        }

        val latencyMs = System.currentTimeMillis() - start
        emit(AiChannelSearchResult(
            query = query,
            explanation = explanation,
            filteredChannels = matchedChannels,
            suggestedPortals = suggestedPortals,
            latencyMs = latencyMs,
        ))
    }
}
