package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Sync2CalClient {
    private const val BASE_URL = "https://www.sync2cal.com/api/v2"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchCategories(query: String, limit: Int = 10): List<Sync2CalCategory> {
        if (query.length < 2) return emptyList()
        return try {
            val response = httpGetText("$BASE_URL/categories/search?q=${encodeParam(query)}&limit=$limit")
            val root = json.parseToJsonElement(response).jsonObject
            root["results"]?.jsonArray?.mapNotNull { parseCategory(it) } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun lookupBySlug(slug: String): Sync2CalCategory? {
        return try {
            val response = httpGetText("$BASE_URL/categories/lookup?slug=${encodeParam(slug)}")
            val root = json.parseToJsonElement(response).jsonObject
            root["category"]?.let { parseCategory(it) }
        } catch (_: Exception) { null }
    }

    suspend fun lookupByUuid(uuid: String): Sync2CalCategory? {
        return try {
            val response = httpGetText("$BASE_URL/categories/lookup?uuid=$uuid")
            val root = json.parseToJsonElement(response).jsonObject
            root["category"]?.let { parseCategory(it) }
        } catch (_: Exception) { null }
    }

    suspend fun getFilteredEvents(uuid: String): List<Sync2CalEvent> {
        return try {
            val response = httpGetText("$BASE_URL/categories/$uuid/filtered-events")
            val root = json.parseToJsonElement(response).jsonObject
            root["events"]?.jsonArray?.mapNotNull { parseEvent(it) } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getNextEvents(ids: List<Int>): Map<Int, Sync2CalEvent?> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val idsParam = ids.joinToString(",")
            val response = httpGetText("$BASE_URL/categories/next-events?ids=$idsParam")
            val root = json.parseToJsonElement(response).jsonObject
            val events = root["next_events"]?.jsonObject ?: return emptyMap()
            ids.associateWith { id ->
                events[id.toString()]?.let { parseEvent(it) }
            }
        } catch (_: Exception) { emptyMap() }
    }

    suspend fun getRootCategories(): List<Sync2CalCategory> {
        return try {
            val response = httpGetText("$BASE_URL/categories/roots")
            val root = json.parseToJsonElement(response).jsonObject
            root["children"]?.jsonArray?.mapNotNull { parseCategory(it) }
                ?: root["categories"]?.jsonArray?.mapNotNull { parseCategory(it) }
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getChildren(parentUuid: String): List<Sync2CalCategory> {
        return try {
            val response = httpGetText("$BASE_URL/categories/$parentUuid/children")
            val root = json.parseToJsonElement(response).jsonObject
            root["children"]?.jsonArray?.mapNotNull { parseCategory(it) } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun parseCategory(element: JsonElement): Sync2CalCategory? {
        return try {
            val obj = element.jsonObject
            Sync2CalCategory(
                id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return null,
                uuid = obj["uuid"]?.jsonPrimitive?.contentOrNull ?: return null,
                slug = obj["slug"]?.jsonPrimitive?.contentOrNull ?: "",
                breadcrumb = obj["breadcrumb"]?.jsonPrimitive?.contentOrNull,
                parentName = obj["parent_name"]?.jsonPrimitive?.contentOrNull,
                addable = obj["addable"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
            )
        } catch (_: Exception) { null }
    }

    private fun parseEvent(element: JsonElement): Sync2CalEvent? {
        return try {
            val obj = element.jsonObject
            Sync2CalEvent(
                id = obj["id"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0,
                title = obj["title"]?.jsonPrimitive?.contentOrNull ?: return null,
                startTime = obj["start_time"]?.jsonPrimitive?.contentOrNull ?: "",
                endTime = obj["end_time"]?.jsonPrimitive?.contentOrNull ?: "",
                allDay = obj["all_day"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                location = obj["location"]?.jsonPrimitive?.contentOrNull,
                description = obj["description"]?.jsonPrimitive?.contentOrNull,
                ticketUrl = obj["ticket_url"]?.jsonPrimitive?.contentOrNull,
            )
        } catch (_: Exception) { null }
    }

    private fun encodeParam(s: String): String {
        return s.replace(" ", "+")
            .replace(",", "%2C")
            .replace("/", "%2F")
            .replace("&", "%26")
            .replace("?", "%3F")
            .replace("#", "%23")
    }
}
