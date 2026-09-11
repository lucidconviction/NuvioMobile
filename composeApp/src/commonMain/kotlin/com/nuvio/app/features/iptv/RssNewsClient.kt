package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText

object RssNewsClient {
    private val itemRegex = Regex("<item>(.*?)</item>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    private fun field(xml: String, name: String): String {
        val re = Regex("<$name[^>]*>(.*?)</$name>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val m = re.find(xml) ?: return ""
        var v = m.groupValues[1]
        v = v.replace(Regex("""<!\[CDATA\[(.*?)\]\]>""", setOf(RegexOption.DOT_MATCHES_ALL))) { it.groupValues[1] }
        v = v.replace(Regex("<[^>]+>"), " ")
        v = v.replace(Regex("""\s+"""), " ").trim()
        return v
    }

    private fun enclosureUrl(xml: String): String {
        val enc = Regex("""<enclosure[^>]*url=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE).find(xml)
        if (enc != null) return enc.groupValues[1]
        val media = Regex("""<media:content[^>]*url=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE).find(xml)
        return media?.groupValues?.get(1) ?: ""
    }

    suspend fun fetchRss(url: String): List<EspnNewsArticle> {
        return try {
            val xml = httpGetText(url)
            itemRegex.findAll(xml).mapNotNull { item ->
                val title = field(item.value, "title")
                if (title.isBlank()) return@mapNotNull null
                val link = field(item.value, "link")
                val description = field(item.value, "description")
                val published = field(item.value, "pubDate")
                val imageUrl = enclosureUrl(item.value)
                EspnNewsArticle(
                    headline = title,
                    description = description,
                    links = if (link.isNotBlank()) EspnNewsLinks(EspnNewsWebLink(link)) else null,
                    images = if (imageUrl.isNotBlank()) listOf(EspnNewsImage(url = imageUrl, width = 0, height = 0)) else emptyList(),
                    published = published,
                    byline = "",
                )
            }.toList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}