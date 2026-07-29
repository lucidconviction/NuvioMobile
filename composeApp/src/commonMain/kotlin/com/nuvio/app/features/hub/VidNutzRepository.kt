package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.sports.YouTubeStreamResolver
import com.nuvio.app.features.sports.StreamResult
import com.nuvio.app.features.sports.platformYouTubeSearch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object VidNutzRepository {

    private const val REQUEST_TIMEOUT_MS = 8_000L

    private data class EngineCache(
        val videos: List<VidNutzVideo>,
        val pageSize: Int = 28,
    ) {
        fun getPage(page: Int): List<VidNutzVideo> {
            val start = (page - 1) * pageSize
            if (start >= videos.size) return emptyList()
            val end = minOf(start + pageSize, videos.size)
            return videos.subList(start, end)
        }
        val hasMore: Boolean get() = videos.size > 28
    }
    private var engineCache: Map<VidNutzCategory, EngineCache> = emptyMap()

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

    private var invidiousIndex = 0
    private var pipedIndex = 0

    private val json = Json { ignoreUnknownKeys = true }

    private fun rotatedInvidious(): List<String> {
        val current = invidiousIndex
        invidiousIndex = (invidiousIndex + 1) % 1_000_000
        val start = current % invidiousInstances.size
        return invidiousInstances.subList(start, invidiousInstances.size) +
            invidiousInstances.subList(0, start)
    }

    private fun rotatedPiped(): List<String> {
        val current = pipedIndex
        pipedIndex = (pipedIndex + 1) % 1_000_000
        val start = current % pipedInstances.size
        return pipedInstances.subList(start, pipedInstances.size) +
            pipedInstances.subList(0, start)
    }

    suspend fun fetchTrending(page: Int = 1): List<VidNutzVideo> {
        if (page == 1) {
            try {
                val qs = listOf("popular", "trending", "viral")
                val results = mutableSetOf<VidNutzVideo>()
                for (q in qs) {
                    val r = platformYouTubeSearch(q)?.map { fromYouTubeVideo(it) } ?: emptyList()
                    results.addAll(r)
                    if (results.size >= 28) break
                }
                if (results.isNotEmpty()) return results.take(28).toList()
            } catch (_: Exception) { }
        }

        val offset = (page - 1) * 3
        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/feed/trending"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedTrendingResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items.filter { it.duration in 30..1800 }.take(28).map { it.toVidNutz() }
                }
            } catch (_: Exception) { }
        }

        for (instance in rotatedInvidious()) {
            try {
                val url = "$instance/api/v1/trending?type=video&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return raw.filter { it.lengthSeconds in 30..1800 }.take(28).map { it.toVidNutz() }
                }
            } catch (_: Exception) { }
        }

        return emptyList()
    }

    suspend fun search(query: String, page: Int = 1): List<VidNutzVideo> {
        if (query.isBlank()) return emptyList()

        if (page == 1) {
            try {
                val r = platformYouTubeSearch(query)
                if (r != null) return r.map { fromYouTubeVideo(it) }
            } catch (_: Exception) { }
        }

        val encodedQuery = encodeUrl(query)
        val offset = (page - 1) * 3

        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/search?q=${encodedQuery}&filter=videos&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items.filter { it.duration in 30..1800 }.take(28).map { it.toVidNutz() }
                }
            } catch (_: Exception) { }
        }

        for (instance in rotatedInvidious()) {
            try {
                val url = "$instance/api/v1/search?q=${encodedQuery}&type=video&sort=relevance&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return raw.filter { it.lengthSeconds in 30..1800 }.take(28).map { it.toVidNutz() }
                }
            } catch (_: Exception) { }
        }

        return emptyList()
    }

    suspend fun refreshCategory(category: VidNutzCategory) {
        invalidateEngineCache()
        VideoSuggestionEngine.resetCategory(categoryToEngineKey[category] ?: "Trending")
    }

    suspend fun fetchLive(page: Int = 1): List<VidNutzVideo> {
        if (page > 1) return emptyList()
        val queries = listOf("live stream", "livestream", "live now")
        for (instance in rotatedInvidious()) {
            try {
                for (q in queries) {
                    val url = "$instance/api/v1/search?q=${encodeUrl(q)}&type=video&sort=relevance&date=all"
                    val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                    val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                    val filtered = raw.filter {
                        it.lengthSeconds in 0..60 || it.publishedText.contains("streaming", ignoreCase = true)
                    }.take(28).map { it.toVidNutz(isLive = true) }
                    if (filtered.isNotEmpty()) return filtered
                }
            } catch (_: Exception) { }
        }
        // Fallback: search "live" via Piped
        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/search?q=${encodeUrl("live stream")}&filter=videos&page=1"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items.filter { it.duration in 0..300 }.take(28).map { it.toVidNutz(isLive = true) }
                }
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    private val categoryToEngineKey = mapOf(
        VidNutzCategory.TRENDING to "Trending",
        VidNutzCategory.LIVE to "Live",
        VidNutzCategory.POLITICS to "Politics",
        VidNutzCategory.NEWS to "News",
        VidNutzCategory.MUSIC to "Music",
        VidNutzCategory.SPORTS to "Sports",
        VidNutzCategory.DOCUMENTARY to "Documentary",
        VidNutzCategory.TECHNOLOGY to "Technology",
        VidNutzCategory.ENTERTAINMENT to "Entertainment",
        VidNutzCategory.COMEDY to "Comedy",
        VidNutzCategory.SCIENCE to "Science",
        VidNutzCategory.TRUE_CRIME to "True Crime",
        VidNutzCategory.FOOD_DRINK to "Food & Drink",
    )

    suspend fun fetchByCategory(category: VidNutzCategory, page: Int = 1): List<VidNutzVideo> {
        val cache = engineCache[category]
        if (cache != null) {
            val cached = cache.getPage(page)
            if (cached.isNotEmpty()) return cached
        }

        if (category != VidNutzCategory.TRENDING && (page == 1 || cache == null)) {
            try {
                val engineKey = categoryToEngineKey[category] ?: "Trending"
                val engineResults = VideoSuggestionEngine.suggest("", engineKey, 96)
                if (engineResults.isNotEmpty()) {
                    val mapped = engineResults.map { fromYouTubeVideo(it) }.distinctBy { it.videoId }
                    engineCache = engineCache + (category to EngineCache(videos = mapped, pageSize = 28))
                    val paged = mapped.take(28)
                    if (paged.isNotEmpty()) return paged
                }
            } catch (_: Exception) {}
        }

        if (category == VidNutzCategory.TRENDING) {
            return fetchTrending(page)
        }

        if (category == VidNutzCategory.LIVE) {
            return fetchLive(page)
        }

        val query = category.displayName
        val results = search(query, page)
        if (results.isNotEmpty()) return results

        // Last-ditch: platform search
        if (page == 1) {
            try {
                val platformResult = platformYouTubeSearch(query)
                if (platformResult != null) {
                    return platformResult.map { fromYouTubeVideo(it) }
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    fun invalidateEngineCache() {
        engineCache = emptyMap()
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

    private fun InvidiousVideo.toVidNutz(isLive: Boolean = false) = VidNutzVideo(
        videoId = videoId,
        title = title,
        thumbnail = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
        channelName = author,
        durationSeconds = lengthSeconds,
        viewCount = viewCount,
        uploadDate = publishedText,
        isLive = isLive || (lengthSeconds in 0..5) || publishedText.contains("streaming", ignoreCase = true),
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

    @Serializable
    data class PipedTrendingResponse(
        val items: List<PipedSearchItem> = emptyList(),
    )

    private fun PipedSearchItem.toVidNutz(isLive: Boolean = false): VidNutzVideo {
        val videoId = url.removePrefix("/watch?v=")
        return VidNutzVideo(
            videoId = videoId,
            title = title,
            thumbnail = "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
            channelName = uploader,
            durationSeconds = duration.toInt(),
            viewCount = views,
            uploadDate = uploadedDate,
            isLive = isLive || (duration in 0..5) || uploadedDate.contains("streaming", ignoreCase = true),
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
