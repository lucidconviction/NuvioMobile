package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetTextWithHeaders
import kotlinx.coroutines.withTimeout

data class XxxVideo(
    val id: String,
    val title: String,
    val duration: String,
    val views: Int,
    val rating: Double,
    val thumbnail: String,
    val embedUrl: String,
    val videoUrl: String,
    val categories: List<String>,
)

data class XxxCategory(
    val name: String,
    val slug: String,
    val count: Int,
)

object XxxClient {
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.9",
    )

    suspend fun searchVideos(query: String, page: Int = 1): List<XxxVideo> {
        return try {
            val url = "https://www.eporner.com/search/?q=${query.replace(" ", "+")}&page=$page"
            val response = withTimeout(15_000) { httpGetTextWithHeaders(url, headers) }
            parseVideoList(response)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getLatest(page: Int = 1): List<XxxVideo> {
        return try {
            // Page 1 uses /latest/ (no page number), subsequent pages use /latest/N/
            val url = if (page <= 1) "https://www.eporner.com/latest/" else "https://www.eporner.com/latest/$page/"
            val response = withTimeout(15_000) { httpGetTextWithHeaders(url, headers) }
            parseVideoList(response)
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getCategories(): List<XxxCategory> {
        return listOf(
            XxxCategory("Teen", "teen", 0),
            XxxCategory("MILF", "milf", 0),
            XxxCategory("Big Tits", "big+tits", 0),
            XxxCategory("BBW", "bbw", 0),
            XxxCategory("Anal", "anal", 0),
            XxxCategory("Lesbian", "lesbian", 0),
            XxxCategory("Gay", "gay", 0),
            XxxCategory("Trans", "trans", 0),
            XxxCategory("HD", "hd", 1080),
            XxxCategory("Creampie", "creampie", 0),
            XxxCategory("Young Teen", "young+teen", 0),
            XxxCategory("Petite", "petite", 0),
            XxxCategory("Threesome", "threesome", 0),
            XxxCategory("Mature", "mature", 0),
            XxxCategory("Brunette", "brunette", 0),
        )
    }

    private fun parseVideoList(html: String): List<XxxVideo> {
        val results = mutableListOf<XxxVideo>()
        // Split by video card divs
        val cards = html.split("""<div class="mb hdy""").drop(1)

        for (card in cards) {
            // Extract data-id
            val idMatch = """data-id="(\d+)"""".toRegex().find(card)
            val id = idMatch?.groupValues?.get(1) ?: continue

            // Extract thumbnail src
            val thumbMatch = """<img[^>]*src="([^"]+)"""".toRegex().find(card)
            val thumbnail = thumbMatch?.groupValues?.get(1) ?: ""

            // Extract video path and title from mbtit link
            val titleMatch = """<a href="(/video-[^"]+)"[^>]*>([^<]+)</a>""".toRegex().find(card)
            val path = titleMatch?.groupValues?.get(1) ?: ""
            val title = decodeHtmlEntities(titleMatch?.groupValues?.get(2)?.trim() ?: "")

            // Extract slug from path
            val slugMatch = """/video-([A-Za-z0-9]+)/""".toRegex().find(path)
            val slug = slugMatch?.groupValues?.get(1) ?: id

            // Extract duration
            val durMatch = """title="Duration">(\d+:\d+)""".toRegex().find(card)
            val duration = durMatch?.groupValues?.get(1) ?: ""

            // Extract views
            val viewsMatch = """title="Views">([\d,]+)""".toRegex().find(card)
            val viewsStr = viewsMatch?.groupValues?.get(1)?.replace(",", "") ?: "0"
            val views = viewsStr.toIntOrNull() ?: 0

            // Extract rating
            val ratingMatch = """title="Rating">(\d+)%""".toRegex().find(card)
            val ratingStr = ratingMatch?.groupValues?.get(1) ?: "0"
            val rating = ratingStr.toDoubleOrNull()?.div(100.0) ?: 0.0

            results.add(XxxVideo(
                id = slug,
                title = title,
                duration = duration,
                views = views,
                rating = rating,
                thumbnail = thumbnail,
                embedUrl = "https://www.eporner.com/embed/$slug/",
                videoUrl = "https://www.eporner.com$path",
                categories = emptyList(),
            ))
        }
        return results
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace("&apos;", "'")
    }
}