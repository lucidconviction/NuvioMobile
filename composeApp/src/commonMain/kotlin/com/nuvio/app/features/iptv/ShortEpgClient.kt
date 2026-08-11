package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalEncodingApi::class)
object ShortEpgClient {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeBase64(raw: String): String? {
        val u = raw.replace(Regex("[^A-Za-z0-9+/=]"), "")
        if (u.isEmpty() || u.length % 4 == 1) return null
        val padded = u + "=".repeat((4 - u.length % 4) % 4)
        return try {
            Base64.decode(padded).decodeToString()
        } catch (_: Exception) { null }
    }

    private fun decodeField(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val direct = raw.trim()
        if (direct.length < 40 && !direct.contains("+") && !direct.contains("/") && !direct.contains("=")) {
            return direct
        }
        return decodeBase64(direct) ?: direct
    }

    private fun parseTimestamp(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val trimmed = raw.trim()
        if (trimmed.all { it.isDigit() || it.isWhitespace() }) {
            val value = trimmed.toLongOrNull() ?: return 0L
            return if (value < 1_000_000_000_000L) value * 1000L else value
        }
        val m = Regex("""(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?""").find(trimmed)
            ?: return 0L
        val g = m.groupValues
        val year = g[1].toIntOrNull() ?: return 0L
        val month = g[2].toIntOrNull() ?: return 0L
        val day = g[3].toIntOrNull() ?: return 0L
        val hour = g[4].toIntOrNull() ?: 0
        val minute = g[5].toIntOrNull() ?: 0
        val second = g[6].toIntOrNull() ?: 0
        return epochMillis(year, month, day, hour, minute, second)
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        var y = if (month <= 2) year - 1 else year
        var m = if (month <= 2) month + 12 else month
        y = if (y < 0) y + 1 else y
        val daysSinceEpoch = 365L * y + y / 4 - y / 100 + y / 400 + (153 * m - 457) / 5 + day - 306 - 719162
        return daysSinceEpoch * 86400_000L + hour * 3600_000L + minute * 60_000L + second * 1_000L
    }

    private fun isGarbageTimestamp(ms: Long, nowMs: Long): Boolean {
        if (ms <= 0L) return true
        return ms < nowMs - (24L * 3600L * 1000L) || ms > nowMs + (24L * 90L * 3600L * 1000L)
    }

    suspend fun fetchShortEpg(
        account: XtreamAccount,
        channel: IptvChannel,
        limit: Int,
    ): List<EpgProgram> {
        val server = account.server.trimEnd('/')
        val url = "$server/player_api.php?username=${account.username}&password=${account.password}" +
            "&action=get_short_epg&stream_id=${channel.id}&limit=$limit"
        val body = try {
            httpGetTextWithHeaders(url, mapOf("User-Agent" to "VLC/3.0.20"))
        } catch (_: Exception) {
            return emptyList()
        }
        if (body.isBlank()) return emptyList()

        val listings = try {
            val root = json.parseToJsonElement(body)
            val rootObj = root as? JsonObject ?: return emptyList()
            (rootObj["epg_listings"] as? JsonArray) ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        val nowMs = System.currentTimeMillis()
        val programs = mutableListOf<EpgProgram>()
        for (element in listings) {
            val obj = element as? JsonObject ?: continue
            val start = parseTimestamp(obj["start"]?.jsonPrimitive?.contentOrNull)
            if (isGarbageTimestamp(start, nowMs)) continue
            val stopRaw = obj["stop"]?.jsonPrimitive?.contentOrNull
            var stop = parseTimestamp(stopRaw)
            if (stop <= start) stop = start + 3600_000L
            if (isGarbageTimestamp(stop, nowMs)) continue

            val title = decodeField(obj["title"]?.jsonPrimitive?.contentOrNull) ?: "Unknown"
            val description = decodeField(obj["description"]?.jsonPrimitive?.contentOrNull)
            programs.add(
                EpgProgram(
                    channelId = channel.id,
                    title = title.ifBlank { "Unknown" },
                    description = description,
                    startTime = start,
                    endTime = stop,
                )
            )
        }

        programs.sortBy { it.startTime }
        return programs
    }
}