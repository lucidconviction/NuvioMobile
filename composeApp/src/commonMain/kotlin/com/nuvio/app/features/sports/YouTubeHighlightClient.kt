package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class InvidiousVideo(
    val title: String = "",
    val videoId: String = "",
    val author: String = "",
    val lengthSeconds: Int = 0,
    val viewCount: Long = 0,
    val publishedText: String = "",
)

@Serializable
data class InvidiousSearchResponse(
    val items: List<InvidiousVideo> = emptyList(),
)

data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val durationSeconds: Int,
)

object YouTubeHighlightClient {
    private val instances = listOf(
        "https://inv.nadeko.net",
        "https://vid.puffyan.us",
        "https://yewtu.be",
        "https://inv.skyn3t.in",
        "https://invidious.snopyta.org",
    )
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.lunar.icu",
        "https://piped-api.garudalinux.org",
    )
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchHighlights(query: String): List<YouTubeVideo> {
        val platformResult = platformYouTubeSearch(query)
        if (platformResult != null) return platformResult

        val encodedQuery = encodeUrl(query)

        for (instance in instances) {
            val result = tryInvidious(instance, encodedQuery)
            if (result != null) return result
        }

        for (instance in pipedInstances) {
            val result = tryPiped(instance, encodedQuery)
            if (result != null) return result
        }

        try {
            val simpleQuery = encodedQuery.split("+").take(3).joinToString("+")
            return tryInvidious(instances.first(), simpleQuery) ?: emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private suspend fun tryInvidious(instance: String, query: String): List<YouTubeVideo>? {
        return try {
            val url = "$instance/api/v1/search?q=${query}+highlights&type=video&sort=relevance&date=month"
            val response = httpGetText(url)
            val raw = json.decodeFromString<List<InvidiousVideo>>(response)
            if (raw.isEmpty()) return null
            raw.filter { it.lengthSeconds in 30..600 }
                .take(6)
                .map { v ->
                    YouTubeVideo(
                        videoId = v.videoId,
                        title = v.title,
                        thumbnail = "https://img.youtube.com/vi/${v.videoId}/mqdefault.jpg",
                        channelName = v.author,
                        durationSeconds = v.lengthSeconds,
                    )
                }.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    @Serializable
    data class PipedSearchItem(
        val url: String = "",
        val title: String = "",
        val uploader: String = "",
        val duration: Long = 0,
        val views: Long = 0,
        val uploadedDate: String = "",
    )

    @Serializable
    data class PipedSearchResponse(
        val items: List<PipedSearchItem> = emptyList(),
    )

    private suspend fun tryPiped(instance: String, query: String): List<YouTubeVideo>? {
        return try {
            val url = "$instance/search?q=${query}+highlights&filter=videos"
            val response = httpGetText(url)
            val parsed = json.decodeFromString<PipedSearchResponse>(response)
            val items = parsed.items
            if (items.isEmpty()) return null
            items.filter { it.duration in 30..600 }
                .take(6)
                .map { item ->
                    val videoId = item.url.removePrefix("/watch?v=")
                    YouTubeVideo(
                        videoId = videoId,
                        title = item.title,
                        thumbnail = "https://img.youtube.com/vi/${videoId}/mqdefault.jpg",
                        channelName = item.uploader,
                        durationSeconds = item.duration.toInt(),
                    )
                }.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    private fun encodeUrl(s: String): String {
        return s.replace(" ", "+")
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replace("&", "%26")
            .replace("?", "%3F")
            .replace("#", "%23")
    }
}
