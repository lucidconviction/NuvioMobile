package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object StalkerClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchChannels(server: String, macAddress: String, accountId: String): List<IptvChannel> {
        val baseUrl = server.trimEnd('/')
        val mac = macAddress.trim().uppercase()
        val token = getToken(baseUrl, mac) ?: return emptyList()
        val channelsJson = httpGetText("$baseUrl/stalker_portal/api/v1/channels?mac=$mac&token=$token&type=all")
        return parseChannels(channelsJson, accountId, baseUrl)
    }

    private suspend fun getToken(baseUrl: String, mac: String): String? {
        try {
            val response = httpGetText("$baseUrl/stalker_portal/api/v1/portal/init?mac=$mac")
            val obj = json.parseToJsonElement(response).jsonObject
            val token = obj["token"]?.jsonPrimitive?.content
            if (!token.isNullOrBlank()) return token
            val data = obj["data"]?.jsonObject
            return data?.let {
                val jsToken = it["js_token"]?.jsonPrimitive?.content
                val apiToken = it["api_token"]?.jsonPrimitive?.content
                jsToken ?: apiToken
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseChannels(jsonStr: String, sourceId: String, baseUrl: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val root = json.parseToJsonElement(jsonStr)
            val data = root.jsonObject["data"]?.jsonArray ?: root.jsonArray
            var counter = 0
            for (element in data) {
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content?.trim()
                    ?: obj["title"]?.jsonPrimitive?.content?.trim() ?: continue
                val cmd = obj["cmd"]?.jsonPrimitive?.content
                val url = extractUrl(cmd, baseUrl)
                if (url == null) continue
                counter++
                val chId = "stalker_${sourceId}_$counter"
                channels.add(IptvChannel(
                    id = chId,
                    name = name,
                    logo = obj["logo"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    group = obj["genres"]?.jsonPrimitive?.content?.trim() ?: "Other",
                    url = url,
                    epgChannelId = obj["epg_id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() },
                    sourceType = SourceType.Stalker,
                    sourceId = sourceId,
                ))
            }
        } catch (_: Exception) { }
        return channels
    }

    private fun extractUrl(cmd: String?, baseUrl: String): String? {
        if (cmd == null) return null
        val parts = cmd.split(" ")
        val uri = parts.lastOrNull() ?: return null
        return if (uri.startsWith("http://") || uri.startsWith("https://")) uri
        else "$baseUrl/$uri"
    }
}
