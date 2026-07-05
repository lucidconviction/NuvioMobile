package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SportsClient {
    private const val BASE = "https://www.thesportsdb.com/api/v1/json/123"
    private const val API_KEY = "123"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchTodaysEvents(date: String): List<SportEvent> {
        return try {
            val url = "$BASE/eventstv.php?d=$date"
            println("SportsClient: fetching $url")
            val response = httpGetText(url)
            println("SportsClient: response length=${response.length}")
            val root = json.parseToJsonElement(response).jsonObject
            val eventsArray = root["tvevents"]?.jsonArray ?: root["events"]?.jsonArray
            if (eventsArray == null) {
                println("SportsClient: no events array found, keys=${root.keys}")
                return emptyList()
            }
            println("SportsClient: found ${eventsArray.size} events")
            val parsed = eventsArray.mapNotNull { element ->
                try {
                    json.decodeFromJsonElement<SportEvent>(element)
                } catch (e: Exception) {
                    println("SportsClient: parse error: ${e.message}")
                    null
                }
            }
            println("SportsClient: parsed ${parsed.size} events")
            parsed
        } catch (e: Exception) {
            println("SportsClient: fetch error: ${e.message}")
            emptyList()
        }
    }

}
