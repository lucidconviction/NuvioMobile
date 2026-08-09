package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText

object BkfcClient {
    private const val EVENTS_URL = "https://www.bkfc.com/events"

    suspend fun fetchUpcomingEvents(): List<BkfcEvent> {
        return try {
            val html = httpGetText(EVENTS_URL)
            parseEvents(html)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEvents(html: String): List<BkfcEvent> {
        val events = mutableListOf<BkfcEvent>()
        val eventBlocks = html.split("BKFC ").drop(1)

        for (block in eventBlocks) {
            try {
                val title = "BKFC " + block.substringBefore("<").trim()
                val dateMatch = Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},\s+\d{4}""").find(block)
                val timeMatch = Regex("""\d{1,2}:\d{2}\s*(AM|PM)""").find(block)
                val vsMatch = Regex("""([A-Z\s\.]+)\s+vs\.?\s*([A-Z\s\.]+)""").find(title)

                val date = dateMatch?.value ?: ""
                val time = timeMatch?.value ?: ""

                val locationLines = block.split("\n").filter { it.contains(",") && it.uppercase() == it }
                val location = locationLines.firstOrNull { !it.contains(" VS ") && !it.contains(" vs ") }?.trim() ?: ""

                val (fighter1, fighter2) = if (vsMatch != null) {
                    val f1 = vsMatch.groupValues[1].trim()
                    val f2 = vsMatch.groupValues[2].trim()
                    f1 to f2
                } else {
                    "" to ""
                }

                val slug = Regex("""/events/([^"'\s]+)""").find(block)?.groupValues?.getOrNull(1) ?: ""

                if (title.isNotBlank() && date.isNotBlank()) {
                    events.add(BkfcEvent(
                        title = title,
                        date = date,
                        time = time,
                        location = location,
                        mainEvent = if (fighter1.isNotBlank()) "$fighter1 vs $fighter2" else title,
                        fighter1 = fighter1,
                        fighter2 = fighter2,
                        slug = slug,
                    ))
                }
            } catch (_: Exception) {}
        }
        return events.take(20)
    }

    suspend fun fetchPastEvents(): List<BkfcEvent> {
        return try {
            val html = httpGetText(EVENTS_URL)
            parsePastEvents(html)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePastEvents(html: String): List<BkfcEvent> {
        val pastIdx = html.indexOf("Past")
        if (pastIdx < 0) return emptyList()
        val pastSection = html.substring(pastIdx)
        val events = mutableListOf<BkfcEvent>()
        val eventBlocks = pastSection.split("BKFC ").drop(1)

        for (block in eventBlocks) {
            try {
                val title = "BKFC " + block.substringBefore("<").trim()
                val dateMatch = Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2},\s+\d{4}""").find(block)
                val vsMatch = Regex("""([A-Z\s\.]+)\s+vs\.?\s*([A-Z\s\.]+)""").find(title)
                val date = dateMatch?.value ?: ""

                val (fighter1, fighter2) = if (vsMatch != null) {
                    vsMatch.groupValues[1].trim() to vsMatch.groupValues[2].trim()
                } else {
                    "" to ""
                }

                if (title.isNotBlank() && date.isNotBlank()) {
                    events.add(BkfcEvent(
                        title = title,
                        date = date,
                        time = "",
                        location = "",
                        mainEvent = if (fighter1.isNotBlank()) "$fighter1 vs $fighter2" else title,
                        fighter1 = fighter1,
                        fighter2 = fighter2,
                    ))
                }
            } catch (_: Exception) {}
        }
        return events.take(20)
    }
}
