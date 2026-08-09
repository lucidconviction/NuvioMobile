package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class BoxingSceneEvent(
    val title: String,
    val date: String,
    val time: String = "",
    val location: String = "",
    val network: String = "",
)

object BoxingSceneClient {
    suspend fun fetchUpcomingEvents(): List<BoxingSceneEvent> {
        return try {
            val events = Sync2CalClient.getFilteredEvents("2fbb5375-c003-4a8c-82b0-1970a96356c0")
            events.map { ev ->
                val time = ev.startTime.substringAfter("T").take(5)
                BoxingSceneEvent(
                    title = ev.title.removePrefix("\uD83E\uDD4A").trim(),
                    date = ev.startTime.take(10),
                    time = time,
                    location = ev.location ?: "",
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
