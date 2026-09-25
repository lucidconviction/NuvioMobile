package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

object StreamEastClient {

    private val baseUrl = "https://streameast.mov"
    private val interestingCategories = setOf(
        "soccer", "football", "basketball", "baseball", "hockey", "tennis", "mma", "boxing",
        "rugby", "cricket", "motorsports", "f1", "motogp", "ufc", "nfl", "nba", "nhl", "mlb",
        "wwe", "darts", "snooker", "golf", "cycling", "volleyball", "handball",
        "olympics", "racing", "badminton", "table tennis", "water polo", "field hockey", "formula",
    )

    suspend fun fetchEvents(): List<DaddyLiveEvent> {
        val result = mutableListOf<DaddyLiveEvent>()
        try {
            val headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            val body = withTimeout(15_000) {
                httpGetTextWithHeaders("$baseUrl/api/events", headers)
            }
            val days = Json.parseToJsonElement(body).jsonArray
            val seen = mutableSetOf<String>()
            var eventCounter = 0

            for (dayElement in days) {
                val dayObj = dayElement.jsonObject
                val dayStr = dayObj["day"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
                val categories = dayObj["categories"]?.jsonObject ?: continue

                for (catKey in categories.keys) {
                    val lowerCat = catKey.lowercase().trim()
                    val isInteresting = interestingCategories.any { lowerCat.contains(it) || it.contains(lowerCat) }
                    if (!isInteresting) continue

                    val events = categories[catKey]?.jsonArray ?: continue
                    for (evElement in events) {
                        val ev = evElement.jsonObject
                        val eventName = ev["event"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
                        val eventTime = ev["time"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                        if (eventName.isEmpty()) continue

                        val channelsArr = ev["channels"]?.jsonArray ?: continue
                        val channels = mutableListOf<DaddyLiveChannel>()
                        for (chElement in channelsArr) {
                            val ch = chElement.jsonObject
                            val chName = ch["channel_name"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                            val chId = ch["channel_id"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                            val chUrl = ch["url"]?.jsonPrimitive?.contentOrNull?.trim()?.ifEmpty { null }
                            if (chName.isNotEmpty()) {
                                channels.add(DaddyLiveChannel(name = chName, channelId = chId, embedUrl = chUrl))
                            }
                        }
                        if (channels.isEmpty()) continue

                        val key = "$eventName|$eventTime|$dayStr"
                        if (seen.add(key)) {
                            result.add(DaddyLiveEvent(
                                id = "se_${eventCounter++}",
                                eventName = eventName,
                                category = catKey,
                                startTime = eventTime,
                                day = dayStr,
                                channels = channels,
                            ))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }
}