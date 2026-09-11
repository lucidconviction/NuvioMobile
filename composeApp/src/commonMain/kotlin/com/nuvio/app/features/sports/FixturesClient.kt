package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnProcessedEvent

/**
 * Fetches upcoming fixtures (future games) from ESPN scoreboard API.
 * Uses date-range queries to get games for the next N days.
 */
object FixturesClient {

    /**
     * Fetch upcoming fixtures for a specific sport/league over the next N days.
     * Returns deduplicated events sorted by date.
     */
    suspend fun getUpcomingFixtures(
        sport: String,
        league: String,
        days: Int = 7,
    ): List<EspnProcessedEvent> {
        val today = formatToday()
        val allEvents = mutableListOf<EspnProcessedEvent>()

        for (dayOffset in 1..days) {
            val targetDate = addDays(today, dayOffset)
            val dateStr = targetDate.replace("-", "")
            try {
                val events = EspnClient.fetchLeague(sport, league, dateStr)
                allEvents.addAll(events)
            } catch (_: Exception) {
                // Skip failed days silently
            }
        }

        return allEvents.distinctBy { it.id }.sortedBy { it.rawDate }
    }

    /**
     * Fetch fixtures for a specific date range (inclusive).
     * Both dates should be in "YYYY-MM-DD" format.
     */
    suspend fun getFixturesInRange(
        sport: String,
        league: String,
        startDate: String,
        endDate: String,
    ): List<EspnProcessedEvent> {
        val dateRange = "${startDate.replace("-", "")}${endDate.replace("-", "")}"
        return try {
            EspnClient.fetchLeague(sport, league, dateRange)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Date utilities matching SportsRepository pattern
    private fun formatToday(): String {
        val nowMs = com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs() +
                com.nuvio.app.features.trakt.TraktPlatformClock.localTimezoneOffsetMs()
        val seconds = nowMs / 1000
        val days = seconds / 86400L
        val (y, m, d) = epochToDate(days)
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    private fun addDays(date: String, days: Int): String {
        var (y, m, d) = parseDate(date)
        var totalDays = daysToEpoch(y, m, d) + days
        if (totalDays < 0) totalDays = 0L
        val (ny, nm, nd) = epochToDate(totalDays)
        return "$ny-${nm.toString().padStart(2, '0')}-${nd.toString().padStart(2, '0')}"
    }

    private fun parseDate(date: String): Triple<Int, Int, Int> {
        val parts = date.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 1970
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return Triple(y, m, d)
    }

    private fun daysToEpoch(year: Int, month: Int, day: Int): Long {
        var y = year.toLong()
        var m = month.toLong()
        var d = day.toLong()
        // Adjust for Jan/Feb
        if (m <= 2) { y--; m += 12 }
        val era = y / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    private fun epochToDate(days: Long): Triple<Int, Int, Int> {
        val ld = days + 719468
        val era = (if (ld >= 0) ld else ld - 146096) / 146097
        val doe = ld - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        val finalY = if (m <= 2) y + 1 else y
        return Triple(finalY.toInt(), m.toInt(), d.toInt())
    }
}
