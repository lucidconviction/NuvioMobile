package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class DaddyLiveChannel(
    val name: String,
    val channelId: String,
    val embedUrl: String?,
)

data class DaddyLiveEvent(
    val id: String,
    val eventName: String,
    val category: String,
    val startTime: String,
    val day: String,
    val channels: List<DaddyLiveChannel>,
) {
    val startEpochMs: Long by lazy { parseEventTime(day, startTime) }

    val isLive: Boolean
        get() {
            if (startEpochMs == 0L) return false
            val now = TraktPlatformClock.nowEpochMs()
            return now >= startEpochMs - 3600000 && now < startEpochMs + 7200000
        }

    val localTime: String
        get() {
            if (startEpochMs == 0L) return startTime
            val localEpochMs = startEpochMs + TraktPlatformClock.localTimezoneOffsetMs()
            val totalSeconds = localEpochMs / 1000L
            val h24 = ((totalSeconds / 3600) % 24).toInt()
            val m = ((totalSeconds / 60) % 60).toInt()
            val amPm = if (h24 < 12) "AM" else "PM"
            val h12 = when { h24 % 12 == 0 -> 12; else -> h24 % 12 }
            return "$h12:${m.toString().padStart(2, '0')} $amPm"
        }

    val localDate: String
        get() {
            if (startEpochMs == 0L) return day
            val localEpochMs = startEpochMs + TraktPlatformClock.localTimezoneOffsetMs()
            val daysSinceEpoch = localEpochMs / 86400000L
            val y = daysSinceEpochToYear(daysSinceEpoch)
            val (m, d) = daysSinceEpochToMonthDay(daysSinceEpoch, y)
            return "$m/$d"
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseEventTime(dayStr: String, timeStr: String): Long {
            try {
                val cleanDay = dayStr.replace("Full Schedule ", "").replace("Schedule ", "").trim()
                val parts = cleanDay.split("/")
                val month = parts.getOrNull(0)?.toIntOrNull() ?: return 0
                val day = parts.getOrNull(1)?.toIntOrNull() ?: return 0
                val year = parts.getOrNull(2)?.toIntOrNull() ?: return 0

                val tParts = timeStr.split(" ")
                val hm = tParts.getOrNull(0)?.split(":") ?: return 0
                val hour = hm.getOrNull(0)?.toIntOrNull() ?: return 0
                val minute = hm.getOrNull(1)?.toIntOrNull() ?: return 0
                val isPM = tParts.getOrNull(1)?.lowercase() == "pm"
                val h24 = when { isPM && hour != 12 -> hour + 12; !isPM && hour == 12 -> 0; else -> hour }

                val days = daysFromEpoch(year, month, day)
                return (days * 86400L + h24 * 3600L + minute * 60L) * 1000L + TraktPlatformClock.localTimezoneOffsetMs()
            } catch (_: Exception) { return 0 }
        }

        private fun isLeapYear(y: Int) = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)

        private fun daysFromEpoch(year: Int, month: Int, day: Int): Int {
            var total = 0
            for (y in 1970 until year) total += if (isLeapYear(y)) 366 else 365
            val dim = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            for (m in 0 until month - 1) total += dim[m]
            return total + day - 1
        }

        private fun daysSinceEpochToYear(days: Long): Int {
            var remaining = days
            var y = 1970
            while (true) {
                val daysInYear = if (isLeapYear(y)) 366 else 365
                if (remaining < daysInYear) break
                remaining -= daysInYear
                y++
            }
            return y
        }

        private fun daysSinceEpochToMonthDay(days: Long, year: Int): Pair<Int, Int> {
            var remaining = days.toInt()
            val dim = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            for (m in 0 until 12) {
                if (remaining < dim[m]) return (m + 1) to (remaining + 1)
                remaining -= dim[m]
            }
            return 12 to 31
        }
    }
}

object DaddyLiveClient {

    private val mirrors = listOf(
        "https://daddylive.li",
        "https://daddylive.eu",
        "https://streameast.mov",
    )

    private val interestingCategories = setOf(
        "soccer", "football", "basketball", "baseball", "hockey", "tennis", "mma", "boxing",
        "rugby", "cricket", "motorsports", "f1", "motogp", "ufc", "nfl", "nba", "nhl", "mlb",
        "wwe", "darts", "snooker", "golf", "cycling", "volleyball", "handball",
        "olympics", "racing", "badminton", "table tennis", "water polo", "field hockey", "formula",
    )

    suspend fun fetchActiveDomain(): String? {
        val headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
        return coroutineScope {
            mirrors.map { mirror ->
                async {
                    try {
                        withTimeout(6_000) {
                            val body = httpGetTextWithHeaders("$mirror/api/events", headers)
                            val arr = Json.parseToJsonElement(body).jsonArray
                            if (arr.isNotEmpty()) mirror else null
                        }
                    } catch (_: Exception) { null }
                }
            }.firstNotNullOfOrNull { it.await() }
        }
    }

    suspend fun fetchEvents(): List<DaddyLiveEvent> {
        val domain = fetchActiveDomain() ?: return emptyList()
        val result = mutableListOf<DaddyLiveEvent>()
        try {
            val headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            val body = httpGetTextWithHeaders("$domain/api/events", headers)
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
                                id = "dl_${eventCounter++}",
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
