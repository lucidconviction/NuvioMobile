package com.nuvio.app.features.iptv

object M3uParser {
    fun parse(content: String, sourceId: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val lines = content.lines()
        var i = 0
        var channelIdCounter = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val infoLine = line.removePrefix("#EXTINF:")

                val tvgId = extractTag(infoLine, "tvg-id")
                val tvgName = extractTag(infoLine, "tvg-name")
                val tvgLogo = extractTag(infoLine, "tvg-logo")
                val groupTitle = extractTag(infoLine, "group-title")

                val commaIndex = infoLine.lastIndexOf(',')
                val channelName = if (commaIndex >= 0) infoLine.substring(commaIndex + 1).trim() else "Unknown"

                i++
                while (i < lines.size && lines[i].trim().isEmpty()) i++

                if (i < lines.size) {
                    val url = lines[i].trim()
                    if (url.isNotBlank() && !url.startsWith("#")) {
                        channelIdCounter++
                        val id = tvgId.ifBlank { "ch_$channelIdCounter" }
                        channels.add(
                            IptvChannel(
                                id = id,
                                name = tvgName.ifBlank { channelName },
                                logo = tvgLogo.ifBlank { null },
                                group = groupTitle.ifBlank { null },
                                url = url,
                                epgChannelId = tvgId.ifBlank { null },
                                sourceType = SourceType.M3U,
                                sourceId = sourceId,
                            )
                        )
                    }
                }
            }
            i++
        }
        return channels
    }

    private fun extractTag(line: String, tag: String): String {
        val regex = Regex("""$tag="([^"]*)"""")
        return regex.find(line)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
