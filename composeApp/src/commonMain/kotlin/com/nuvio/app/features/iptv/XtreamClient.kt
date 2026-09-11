package com.nuvio.app.features.iptv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object XtreamClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseAccountInfo(response: String): PortalAccountInfo? {
        try {
            val element = json.parseToJsonElement(response)
            if (element !is JsonObject) return null
            val info = element["user_info"]?.jsonObject ?: element
            if (!element.containsKey("user_info") && info["auth"]?.jsonPrimitive?.contentOrNull != "1") return null

            fun str(key: String): String? = info[key]?.jsonPrimitive?.contentOrNull
            fun parseExpDate(raw: String): Long? {
                if (raw.isBlank() || raw == "null") return null
                val asLong = raw.trim().toLongOrNull()
                if (asLong != null) {
                    return if (asLong > 10_000_000_000L) asLong else asLong * 1000L
                }
                return null
            }
            return PortalAccountInfo(
                expDate = str("exp_date")?.let { parseExpDate(it) },
                maxConnections = str("max_connections")?.toIntOrNull(),
                activeConnections = str("active_connections")?.toIntOrNull(),
                status = str("status"),
                isTrial = str("is_trial")?.let { it == "1" || it.equals("true", true) },
            )
        } catch (_: Exception) { }
        return null
    }

    fun parseCategories(response: String): List<XtreamCategory> {
        val categories = mutableListOf<XtreamCategory>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val id = obj["category_id"]?.jsonPrimitive?.content ?: continue
                val name = obj["category_name"]?.jsonPrimitive?.content ?: continue
                categories.add(XtreamCategory(id = id, name = name))
            }
        } catch (_: Exception) { }
        return categories
    }

    fun parseChannels(
        response: String,
        sourceId: String,
        server: String,
        username: String,
        password: String,
    ): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val arr = json.decodeFromString<JsonArray>(response)
            for (element in arr) {
                val obj = element.jsonObject
                val streamId = obj["stream_id"]?.jsonPrimitive?.content
                    ?: obj["stream_id"]?.jsonPrimitive?.content?.toLongOrNull()?.toString()
                    ?: continue
                val name = obj["name"]?.jsonPrimitive?.content ?: obj["stream_type"]?.jsonPrimitive?.content ?: "Unknown"
                val logo = obj["stream_icon"]?.jsonPrimitive?.content
                val categoryId = obj["category_id"]?.jsonPrimitive?.content
                val epgChannelId = obj["epg_channel_id"]?.jsonPrimitive?.content

                val url = buildXtreamUrl(server, username, password, streamId)

                channels.add(
                    IptvChannel(
                        id = streamId,
                        name = name,
                        logo = logo?.ifBlank { null },
                        group = categoryId,
                        url = url,
                        epgChannelId = epgChannelId?.ifBlank { null },
                        sourceType = SourceType.Xtream,
                        sourceId = sourceId,
                    )
                )
            }
        } catch (_: Exception) { }
        return channels
    }

    private fun buildXtreamUrl(server: String, username: String, password: String, streamId: String): String {
        val base = server.trimEnd('/')
        return "$base/live/$username/$password/$streamId.ts"
    }
}
