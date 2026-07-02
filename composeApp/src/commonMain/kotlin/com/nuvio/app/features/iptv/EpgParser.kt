package com.nuvio.app.features.iptv

data class EpgParseResult(
    val programsByChannelId: Map<String, List<EpgProgram>>,
    val channelDisplayNames: Map<String, String>,
)

object EpgParser {
    fun parseXmltv(xmlContent: String): EpgParseResult {
        val channelNames = mutableMapOf<String, String>()
        val channelRegex = Regex("""<channel\s+id="([^"]+)"[^>]*>""")
        val displayNameRegex = Regex("""<display-name>([^<]*)</display-name>""")
        var chIdx = 0
        while (true) {
            val chMatch = channelRegex.find(xmlContent, chIdx) ?: break
            val chId = chMatch.groupValues[1]
            val blockStart = chMatch.range.last + 1
            val blockEnd = xmlContent.indexOf("</channel>", blockStart)
            val block = if (blockEnd >= 0) xmlContent.substring(blockStart, blockEnd) else ""
            val displayName = displayNameRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
            if (displayName != null) {
                channelNames[chId] = displayName
            }
            chIdx = if (blockEnd >= 0) blockEnd + 10 else chMatch.range.last + 1
        }
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()

        val programmeRegex = Regex(
            """<programme\s+[^>]*start="([^"]+)"[^>]*stop="([^"]+)"[^>]*channel="([^"]+)"[^>]*>"""
        )
        val titleRegex = Regex("""<title>([^<]*)</title>""")
        val descRegex = Regex("""<desc>([^<]*)</desc>""")
        val iconRegex = Regex("""<icon\s+src="([^"]+)"""")

        var currentIndex = 0
        val text = xmlContent

        while (true) {
            val progMatch = programmeRegex.find(text, currentIndex) ?: break
            val startStr = progMatch.groupValues[1]
            val stopStr = progMatch.groupValues[2]
            val channelId = progMatch.groupValues[3]

            val blockStart = progMatch.range.last + 1
            val blockEnd = text.indexOf("</programme>", blockStart)
            val block = if (blockEnd >= 0) text.substring(blockStart, blockEnd) else ""

            val title = titleRegex.find(block)?.groupValues?.getOrNull(1)?.trim() ?: "Unknown"
            val description = descRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
            val icon = iconRegex.find(block)?.groupValues?.getOrNull(1)

            val startTime = parseXmltvDate(startStr)
            val endTime = parseXmltvDate(stopStr)

            if (startTime > 0 && endTime > 0) {
                val program = EpgProgram(
                    channelId = channelId,
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    icon = icon,
                )
                programs.getOrPut(channelId) { mutableListOf() }.add(program)
            }

            currentIndex = if (blockEnd >= 0) blockEnd + 12 else progMatch.range.last + 1
        }

        for ((_, list) in programs) {
            list.sortBy { it.startTime }
        }

        return EpgParseResult(programsByChannelId = programs, channelDisplayNames = channelNames)
    }

    internal fun parseXmltvDate(dateStr: String): Long {
        val cleaned = dateStr.trim()

        val datePart: String
        val tzOffsetMinutes: Int

        val spaceIdx = cleaned.indexOf(' ')
        if (spaceIdx > 0) {
            datePart = cleaned.substring(0, spaceIdx)
            val tzStr = cleaned.substring(spaceIdx).trim()
            tzOffsetMinutes = parseTimezoneOffset(tzStr)
        } else {
            datePart = cleaned
            tzOffsetMinutes = 0
        }

        if (datePart.length < 14) return 0L

        try {
            val year = datePart.substring(0, 4).toInt()
            val month = datePart.substring(4, 6).toInt()
            val day = datePart.substring(6, 8).toInt()
            val hour = datePart.substring(8, 10).toInt()
            val minute = datePart.substring(10, 12).toInt()
            val second = datePart.substring(12, 14).toInt()

            return epochMillis(year, month, day, hour, minute, second) - (tzOffsetMinutes * 60_000L)
        } catch (_: Exception) {
            return 0L
        }
    }

    private fun parseTimezoneOffset(tz: String): Int {
        if (tz.length < 3) return 0
        val sign = if (tz[0] == '-') -1 else 1
        val hours = tz.substring(1, 3).toIntOrNull() ?: return 0
        val minutes = if (tz.length >= 5) tz.substring(3, 5).toIntOrNull() ?: 0 else 0
        return sign * (hours * 60 + minutes)
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        var y = if (month <= 2) year - 1 else year
        var m = if (month <= 2) month + 12 else month
        val era = if (y >= 0) 1 else 0
        y = if (y >= 0) y else y + 1

        val daysSinceEpoch = (365L * y + y / 4 - y / 100 + y / 400 + (153 * m - 457) / 5 + day - 306 - 719162)
        return daysSinceEpoch * 86400_000L + hour * 3600_000L + minute * 60_000L + second * 1_000L
    }
}
