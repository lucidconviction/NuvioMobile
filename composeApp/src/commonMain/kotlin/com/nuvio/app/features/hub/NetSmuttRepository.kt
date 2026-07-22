package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object NetSmuttRepository {

    data class NetSmuttVideo(
        val id: String,
        val title: String,
        val url: String,
        val thumbnail: String = "",
        val source: String,
    )

    private var cachedVideos: List<NetSmuttVideo> = emptyList()
    private var cacheTimestamp = 0L
    private var isScraping = false

    suspend fun fetchPage(page: Int = 1, pageSize: Int = 20): List<NetSmuttVideo> {
        val now = currentTimeMillis()
        if (cachedVideos.isEmpty() || now - cacheTimestamp > 300_000L) {
            if (!isScraping) {
                isScraping = true
                try {
                    cachedVideos = scrapeAllParallel()
                    cacheTimestamp = now
                } finally {
                    isScraping = false
                }
            } else {
                kotlinx.coroutines.delay(500)
                if (cachedVideos.isEmpty()) return emptyList()
            }
        }
        val startIdx = (page - 1) * pageSize
        if (startIdx >= cachedVideos.size) return emptyList()
        val endIdx = minOf(startIdx + pageSize, cachedVideos.size)
        return cachedVideos.subList(startIdx, endIdx)
    }

    private suspend fun scrapeAllParallel(): List<NetSmuttVideo> = coroutineScope {
        val seenUrls = mutableSetOf<String>()
        val urlToId = mutableMapOf<String, String>()
        val idToUrl = mutableMapOf<String, String>()

        fun addUnique(url: String, title: String, source: String): NetSmuttVideo? {
            if (url.isBlank() || seenUrls.contains(url)) return null
            seenUrls.add(url)
            val id = "ns_${seenUrls.size}_${url.hashCode()}"
            urlToId[url] = id
            idToUrl[id] = url
            return NetSmuttVideo(id = id, title = title, url = url, source = source)
        }

        val def1 = async { scrapeKaoticInternal(::addUnique) }
        val def2 = async { scrapeVidmaxInternal(::addUnique) }
        val def3 = async { scrapeWorldstarInternal(::addUnique) }

        val all = mutableListOf<NetSmuttVideo>()
        all.addAll(def1.await())
        all.addAll(def2.await())
        all.addAll(def3.await())

        all.shuffled()
    }

    fun resolveUrl(videoId: String): String? {
        val video = cachedVideos.firstOrNull { it.id == videoId }
        return video?.url
    }

    private suspend fun scrapeKaoticInternal(addUnique: (String, String, String) -> NetSmuttVideo?): List<NetSmuttVideo> {
        val results = mutableListOf<NetSmuttVideo>()
        try {
            for (page in 1..3) {
                val pageUrl = if (page == 1) "https://kaotic.com/" else "https://kaotic.com/?page=$page"
                val html = httpGetText(pageUrl)
                val linkRegex = Regex("""<a\s+href="(https://kaotic\.com/video/[a-f0-9_t]+)"[^>]*title="([^"]*)"[^>]*>""", RegexOption.IGNORE_CASE)
                for (match in linkRegex.findAll(html)) {
                    val videoUrl = match.groupValues[1].split("?")[0]
                    val title = match.groupValues[2].replace(Regex("[<>]"), "").trim()
                    val resolved = resolveKaoticUrl(videoUrl)
                    if (resolved != null) {
                        addUnique(resolved, title, "Kaotic")?.let { results.add(it) }
                    }
                    if (results.size >= 60) return results
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private suspend fun resolveKaoticUrl(videoUrl: String): String? {
        return try {
            val html = httpGetText(videoUrl)
            val sourceMatch = Regex("""<source\s+src="([^"]+\.mp4[^"]*)"\s+type=['"]video/mp4['"]>""", RegexOption.IGNORE_CASE).find(html)
            if (sourceMatch != null) return sourceMatch.groupValues[1]
            val ogMatch = Regex("""<meta\s+[^>]*property=["']og:video:url["'][^>]*content=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE).find(html)
            ogMatch?.groupValues?.getOrNull(1)
        } catch (_: Exception) { null }
    }

    private suspend fun scrapeVidmaxInternal(addUnique: (String, String, String) -> NetSmuttVideo?): List<NetSmuttVideo> {
        val results = mutableListOf<NetSmuttVideo>()
        try {
            val pages = listOf(
                "https://vidmax.com/",
                "https://vidmax.com/page2.html",
                "https://vidmax.com/page3.html",
            )
            for (pageUrl in pages) {
                val html = httpGetText(pageUrl)
                val linkRegex = Regex("""<a[^>]*href="(https://vidmax\.com/video/\d+[^"]*)"[^>]*title="([^"]*)"[^>]*>""", RegexOption.IGNORE_CASE)
                for (match in linkRegex.findAll(html)) {
                    val videoUrl = match.groupValues[1].split("?")[0]
                    val title = match.groupValues[2].replace(Regex("&#\\d+;"), "").replace("&amp;", "&").trim()
                    val resolved = resolveVidmaxUrl(videoUrl)
                    if (resolved != null) {
                        addUnique(resolved, title, "Vidmax")?.let { results.add(it) }
                    }
                    if (results.size >= 50) return results
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private suspend fun resolveVidmaxUrl(videoUrl: String): String? {
        return try {
            val html = httpGetText(videoUrl)
            val sourceMatch = Regex("""<source\s+src="([^"]+\.mp4[^"]*)"\s+type=['"]video/mp4['"]>""", RegexOption.IGNORE_CASE).find(html)
            if (sourceMatch != null) return sourceMatch.groupValues[1].split("?")[0]
            val altMatch = Regex("""src=["']([^"']+\.mp4[^"']*)["']""", RegexOption.IGNORE_CASE).find(html)
            altMatch?.groupValues?.getOrNull(1)?.split("?")?.getOrNull(0)
        } catch (_: Exception) { null }
    }

    private suspend fun scrapeWorldstarInternal(addUnique: (String, String, String) -> NetSmuttVideo?): List<NetSmuttVideo> {
        val results = mutableListOf<NetSmuttVideo>()
        try {
            val html = httpGetText("https://worldstarhiphop.com/videos/")
            val nextDataMatch = Regex("""(?s)<script id="__NEXT_DATA__" type="application/json">(.+?)</script>""").find(html)
            if (nextDataMatch != null) {
                val jsonStr = nextDataMatch.groupValues[1]
                try {
                    val root = Json.parseToJsonElement(jsonStr).jsonObject
                    val props = root?.get("props")?.jsonObject?.get("pageProps")?.jsonObject
                    val videoData = props?.get("videoData")?.jsonObject
                    if (videoData != null) {
                        val title = videoData["title"]?.jsonPrimitive?.content ?: ""
                        val mp4Url = videoData["desktopVideoFile"]?.jsonPrimitive?.content ?: ""
                        addUnique(mp4Url, title, "Worldstar")?.let { results.add(it) }
                    }
                    val trendingList = props?.get("trendingVideosList")?.jsonObject?.get("result")
                    if (trendingList is JsonArray) {
                        for (item in trendingList) {
                            val obj = item.jsonObject ?: continue
                            val title = obj["title"]?.jsonPrimitive?.content ?: ""
                            val mp4Url = obj["desktopVideoFile"]?.jsonPrimitive?.content ?: ""
                            addUnique(mp4Url, title, "Worldstar")?.let { results.add(it) }
                        }
                    }
                    val nextVideos = props?.get("nextVideosInitialResponse")?.jsonObject?.get("result")
                    if (nextVideos is JsonArray) {
                        for (item in nextVideos) {
                            val obj = item.jsonObject ?: continue
                            val title = obj["title"]?.jsonPrimitive?.content ?: ""
                            val mp4Url = obj["desktopVideoFile"]?.jsonPrimitive?.content ?: ""
                            addUnique(mp4Url, title, "Worldstar")?.let { results.add(it) }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return results.take(100)
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}
