package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.sports.YouTubeStreamResolver
import com.nuvio.app.features.sports.StreamResult
import com.nuvio.app.features.sports.platformYouTubeSearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object VidNutzRepository {

    private val invidiousInstances = listOf(
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

    suspend fun fetchTrending(page: Int = 1): List<VidNutzVideo> {
        if (page == 1) {
            val platformResult = platformYouTubeSearch("trending")
            if (platformResult != null) {
                return platformResult.map { fromYouTubeVideo(it) }
            }
        }

        for (instance in invidiousInstances) {
            try {
                val url = "$instance/api/v1/trending?type=video&page=$page"
                val response = httpGetText(url)
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return raw.filter { it.lengthSeconds in 30..1800 }
                        .take(20)
                        .map { it.toVidNutz() }
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    suspend fun search(query: String, page: Int = 1): List<VidNutzVideo> {
        if (query.isBlank()) return emptyList()

        if (page == 1) {
            val platformResult = platformYouTubeSearch(query)
            if (platformResult != null) {
                return platformResult.map { fromYouTubeVideo(it) }
            }
        }

        val encodedQuery = encodeUrl(query)

        for (instance in invidiousInstances) {
            try {
                val url = "$instance/api/v1/search?q=${encodedQuery}&type=video&sort=relevance&page=$page"
                val response = httpGetText(url)
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return raw.filter { it.lengthSeconds in 30..1800 }
                        .take(20)
                        .map { it.toVidNutz() }
                }
            } catch (_: Exception) {}
        }

        for (instance in pipedInstances) {
            try {
                val url = "$instance/search?q=${encodedQuery}&filter=videos&page=$page"
                val response = httpGetText(url)
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items
                        .filter { it.duration in 30..1800 }
                        .take(20)
                        .map { it.toVidNutz() }
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    suspend fun fetchByCategory(category: VidNutzCategory, page: Int = 1): List<VidNutzVideo> {
        if (category == VidNutzCategory.TRENDING) {
            return fetchTrending(page)
        }
        val query = when (category) {
            VidNutzCategory.POLITICS -> "politics news today"
            VidNutzCategory.NEWS -> "breaking news today"
            VidNutzCategory.MUSIC -> "music videos"
            VidNutzCategory.SPORTS -> "sports highlights"
            VidNutzCategory.DOCUMENTARY -> "documentary"
            VidNutzCategory.TECHNOLOGY -> "technology tech review"
            VidNutzCategory.ENTERTAINMENT -> "entertainment"
            VidNutzCategory.COMEDY -> "comedy standup"
            VidNutzCategory.SCIENCE -> "science"
            VidNutzCategory.TRUE_CRIME -> "true crime documentary"
            VidNutzCategory.FOOD_DRINK -> "food drink cooking"
        }
        return search(query, page)
    }

    suspend fun resolveStream(videoId: String): StreamResult? {
        return YouTubeStreamResolver.resolveStream(videoId)
    }

    private fun encodeUrl(s: String): String {
        return s.replace(" ", "+")
            .replace(",", "%2C")
            .replace(":", "%3A")
            .replace("&", "%26")
            .replace("?", "%3F")
            .replace("#", "%23")
    }

    @Serializable
    data class InvidiousVideo(
        val title: String = "",
        val videoId: String = "",
        val author: String = "",
        val lengthSeconds: Int = 0,
        val viewCount: Long = 0,
        val publishedText: String = "",
    )

    private fun InvidiousVideo.toVidNutz() = VidNutzVideo(
        videoId = videoId,
        title = title,
        thumbnail = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
        channelName = author,
        durationSeconds = lengthSeconds,
        viewCount = viewCount,
        uploadDate = publishedText,
    )

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

    private fun PipedSearchItem.toVidNutz(): VidNutzVideo {
        val videoId = url.removePrefix("/watch?v=")
        return VidNutzVideo(
            videoId = videoId,
            title = title,
            thumbnail = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
            channelName = uploader,
            durationSeconds = duration.toInt(),
            viewCount = views,
            uploadDate = uploadedDate,
        )
    }

    private fun fromYouTubeVideo(v: com.nuvio.app.features.sports.YouTubeVideo) = VidNutzVideo(
        videoId = v.videoId,
        title = v.title,
        thumbnail = v.thumbnail,
        channelName = v.channelName,
        durationSeconds = v.durationSeconds,
    )
}
