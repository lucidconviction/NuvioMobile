package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText

data class PowerSlapEvent(
    val title: String,
    val date: String,
    val location: String = "",
    val fighter1: String = "",
    val fighter2: String = "",
)

object PowerSlapClient {
    private const val EVENTS_URL = "https://www.powerslap.com/events/"

    suspend fun fetchUpcomingEvents(): List<PowerSlapEvent> {
        return try {
            val html = httpGetText(EVENTS_URL)
            parseEvents(html)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEvents(html: String): List<PowerSlapEvent> {
        val events = mutableListOf<PowerSlapEvent>()
        val parts = html.split("Power Slap").drop(1)

        for (part in parts) {
            try {
                val number = Regex("""(\d+)""").find(part)?.groupValues?.getOrNull(1) ?: continue
                val title = "Power Slap $number"

                val dateMatch = Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2}""").find(part)
                val date = dateMatch?.value ?: ""
                val year = Regex("""202\d""").find(part)?.value ?: "2026"
                val fullDate = if (date.isNotBlank()) "$date, $year" else ""

                val vsMatch = Regex("""([A-Za-z]+)\s+vs\.?\s*([A-Za-z]+)""").find(part)
                val (f1, f2) = if (vsMatch != null) {
                    vsMatch.groupValues[1].trim() to vsMatch.groupValues[2].trim()
                } else "" to ""

                val locMatch = Regex("""([A-Za-z\s\.]+),\s*([A-Za-z\s\.]+)\s*\d{5}""").find(part)
                val location = locMatch?.value ?: ""

                if (title.isNotBlank()) {
                    events.add(PowerSlapEvent(
                        title = title, date = fullDate,
                        location = location,
                        fighter1 = f1, fighter2 = f2,
                    ))
                }
            } catch (_: Exception) {}
        }
        return events.distinctBy { it.title }.take(10)
    }
}
