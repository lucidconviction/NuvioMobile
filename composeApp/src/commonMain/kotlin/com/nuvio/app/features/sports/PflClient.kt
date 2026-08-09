package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class PflEvent(
    val title: String,
    val date: String,
    val time: String = "",
    val location: String = "",
    val fighter1: String = "",
    val fighter2: String = "",
)

object PflClient {
    private const val HOME_URL = "https://pflmma.com/"

    suspend fun fetchUpcomingEvents(): List<PflEvent> {
        return try {
            val html = httpGetText(HOME_URL)
            parseEvents(html)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEvents(html: String): List<PflEvent> {
        val events = mutableListOf<PflEvent>()

        val jsonLdMatch = Regex("""<script type="application/ld\+json"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL).find(html)
        if (jsonLdMatch != null) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(jsonLdMatch.groupValues[1])
                val items = root.jsonObject["itemListElement"]?.jsonArray
                items?.forEach { item ->
                    val name = item.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val vsMatch = Regex("""(.+?)\s+vs\.?\s+(.+)""").find(name)
                    if (vsMatch != null) {
                        val f1 = vsMatch.groupValues[1].trim()
                        val f2 = vsMatch.groupValues[2].trim()
                        val dateMatch = Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},?\s+\d{4}""").find(name)
                        val date = dateMatch?.value ?: ""
                        if (f1.isNotBlank() && f2.isNotBlank()) {
                            events.add(PflEvent(
                                title = "$f1 vs $f2", date = date,
                                fighter1 = f1, fighter2 = f2,
                            ))
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (events.isEmpty()) {
            val dateTimeMatch = Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2}\s*\|\s*(\d{1,2}:\d{2}\s*(am|pm|AM|PM))""").find(html)
            val eventNameMatch = Regex("""<h3[^>]*class="mb-2"[^>]*>(.*?)</h3>""").find(html)
            if (eventNameMatch != null && dateTimeMatch != null) {
                events.add(PflEvent(
                    title = eventNameMatch.groupValues[1].trim(),
                    date = dateTimeMatch.groupValues[1],
                    time = dateTimeMatch.groupValues[2],
                ))
            }
        }
        return events.distinctBy { it.title }.take(20)
    }
}
