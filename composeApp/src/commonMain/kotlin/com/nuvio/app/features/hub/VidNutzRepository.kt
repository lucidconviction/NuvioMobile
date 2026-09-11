package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.sports.YouTubeStreamResolver
import com.nuvio.app.features.sports.StreamResult
import com.nuvio.app.features.sports.platformYouTubeSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object VidNutzRepository {

    private const val REQUEST_TIMEOUT_MS = 8_000L
    private const val CACHE_TTL_MS = 30 * 60 * 1000L
    private const val MAX_SEARCH_CACHE_SIZE = 50

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
    private var engineCache: MutableMap<VidNutzCategory, EngineCache> = mutableMapOf()

    private var categoryCache: MutableMap<VidNutzCategory, CachedCategory> = mutableMapOf()
    private var searchCache: MutableMap<String, CachedSearch> = mutableMapOf()
    private var hubCache: MutableMap<String, CachedHub> = mutableMapOf()
    private var cacheLoaded = false
    private var cacheSaveJob: Job? = null

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

    private fun loadCache() {
        if (cacheLoaded) return
        cacheLoaded = true
        VidNutzCacheStore.load()?.let { cachedJson ->
            try {
                val cache = json.decodeFromString<VidNutzCache>(cachedJson)
                categoryCache = cache.categories.mapKeys { VidNutzCategory.valueOf(it.key) }.toMutableMap()
                searchCache = cache.searches.toMutableMap()
                hubCache = cache.hubs.toMutableMap()
            } catch (_: Exception) {}
        }
    }

    private fun saveCache() {
        cacheSaveJob?.cancel()
        cacheSaveJob = kotlinx.coroutines.GlobalScope.launch {
            try {
                val cache = VidNutzCache(
                    categories = categoryCache.mapKeys { it.key.name },
                    searches = searchCache,
                    hubs = hubCache,
                )
                VidNutzCacheStore.save(json.encodeToString(cache))
            } catch (_: Exception) {}
        }
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
            } catch (_: Exception) {}
        }

        val offset = (page - 1) * 3
        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/feed/trending"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedTrendingResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items.filter { it.duration in 30..1800 }.take(28).map { it.toVidNutz() }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                }
            } catch (_: Exception) {}
        }

        for (instance in rotatedInvidious()) {
            try {
                val url = "$instance/api/v1/trending?type=video&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    return raw.filter { it.lengthSeconds in 30..1800 }.take(28).map { it.toVidNutz() }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    suspend fun search(query: String, page: Int = 1): List<VidNutzVideo> {
        if (query.isBlank()) return emptyList()
        loadCache()

        val cacheKey = "$query|$page"
        val cached = searchCache[cacheKey]
        if (cached != null && !cached.isExpired) {
            return cached.videos
        }

        if (page == 1) {
            try {
                val r = platformYouTubeSearch(query)
                if (r != null) {
                    val results = r.map { fromYouTubeVideo(it) }
                    cacheSearch(cacheKey, results)
                    return results
                }
            } catch (_: Exception) {}
        }

        val encodedQuery = encodeUrl(query)
        val offset = (page - 1) * 3

        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/search?q=${encodedQuery}&filter=videos&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    val results = parsed.items.filter { it.duration in 30..1800 }.take(28).map { it.toVidNutz() }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                    if (results.isNotEmpty()) {
                        cacheSearch(cacheKey, results)
                    }
                    return results
                }
            } catch (_: Exception) {}
        }

        for (instance in rotatedInvidious()) {
            try {
                val url = "$instance/api/v1/search?q=${encodedQuery}&type=video&sort=date&page=${page + offset}"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val raw = json.decodeFromString<List<InvidiousVideo>>(response)
                if (raw.isNotEmpty()) {
                    val results = raw.filter { it.lengthSeconds in 30..1800 }.take(28).map { it.toVidNutz() }
                    if (results.isNotEmpty()) {
                        cacheSearch(cacheKey, results)
                    }
                    return results
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    private fun cacheSearch(key: String, videos: List<VidNutzVideo>) {
        if (searchCache.size >= MAX_SEARCH_CACHE_SIZE) {
            val oldestKey = searchCache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { searchCache.remove(it) }
        }
        searchCache[key] = CachedSearch(videos = videos, timestamp = System.currentTimeMillis(), page = 1)
        saveCache()
    }

    suspend fun getSubVideos(subId: String, queries: List<String>, count: Int = 12): List<VidNutzVideo> {
        val base = queries.firstOrNull()?.takeIf { it.isNotBlank() } ?: "videos"
        return search(base, 1).distinctBy { it.videoId }.take(count).sortedByDescending { videoUploadTimestamp(it.uploadDate) }
    }

    suspend fun forceRefreshSubVideos(subId: String, queries: List<String>, count: Int = 12): List<VidNutzVideo> {
        loadCache()
        for (q in queries) {
            if (q.isNotBlank()) {
                val prefix = "$q|"
                searchCache.keys.removeAll { it.startsWith(prefix) }
            }
        }
        saveCache()
        return getSubVideos(subId, queries, count)
    }

    suspend fun getMoreSubVideos(subId: String, queries: List<String>, page: Int): List<VidNutzVideo> {
        val base = queries.firstOrNull()?.takeIf { it.isNotBlank() } ?: "videos"
        return search(base, page).sortedByDescending { videoUploadTimestamp(it.uploadDate) }
    }

    suspend fun refreshCategory(category: VidNutzCategory) {
        invalidateEngineCache()
        categoryCache.remove(category)
        VideoSuggestionEngine.resetCategory(categoryToEngineKey[category] ?: "Trending")
        saveCache()
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
                    }.take(28).map { it.toVidNutz(isLive = true) }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                    if (filtered.isNotEmpty()) return filtered
                }
            } catch (_: Exception) {}
        }
        for (instance in rotatedPiped()) {
            try {
                val url = "$instance/search?q=${encodeUrl("live stream")}&filter=videos&page=1"
                val response = withTimeout(REQUEST_TIMEOUT_MS) { httpGetText(url) }
                val parsed = json.decodeFromString<PipedSearchResponse>(response)
                if (parsed.items.isNotEmpty()) {
                    return parsed.items.filter { it.duration in 0..300 }.take(28).map { it.toVidNutz(isLive = true) }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                }
            } catch (_: Exception) {}
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
        loadCache()

        val memCache = engineCache[category]
        if (memCache != null) {
            val cached = memCache.getPage(page)
            if (cached.isNotEmpty()) return cached
        }

        val diskCache = categoryCache[category]
        if (diskCache != null && !diskCache.isExpired && diskCache.page >= page) {
            val cached = diskCache.videos
            val paged = cached.chunked(28).getOrElse(page - 1) { emptyList() }
            if (paged.isNotEmpty()) {
                if (page == 1) {
                    engineCache[category] = EngineCache(videos = cached, pageSize = 28)
                }
                return paged
            }
        }

        if (category != VidNutzCategory.TRENDING && (page == 1 || diskCache == null || diskCache.isExpired)) {
            try {
                val engineKey = categoryToEngineKey[category] ?: "Trending"
                val engineResults = VideoSuggestionEngine.suggest("", engineKey, 96)
                if (engineResults.isNotEmpty()) {
                    val mapped = engineResults.map { fromYouTubeVideo(it) }.distinctBy { it.videoId }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                    engineCache[category] = EngineCache(videos = mapped, pageSize = 28)
                    categoryCache[category] = CachedCategory(videos = mapped, timestamp = System.currentTimeMillis(), page = mapped.size / 28 + 1)
                    saveCache()
                    val paged = mapped.take(28)
                    if (paged.isNotEmpty()) return paged
                }
            } catch (_: Exception) {}
        }

        if (category == VidNutzCategory.TRENDING) {
            val results = fetchTrending(page)
            if (results.isNotEmpty() && page == 1) {
                categoryCache[category] = CachedCategory(videos = results, timestamp = System.currentTimeMillis(), page = 1)
                saveCache()
            }
            return results
        }

        if (category == VidNutzCategory.LIVE) {
            return fetchLive(page)
        }

        val query = category.displayName
        val results = search(query, page)
        if (results.isNotEmpty() && page == 1) {
            categoryCache[category] = CachedCategory(videos = results, timestamp = System.currentTimeMillis(), page = 1)
            saveCache()
        }
        if (results.isNotEmpty()) return results

        if (page == 1) {
            try {
                val platformResult = platformYouTubeSearch(query)
                if (platformResult != null) {
                    val mapped = platformResult.map { fromYouTubeVideo(it) }.sortedByDescending { videoUploadTimestamp(it.uploadDate) }
                    categoryCache[category] = CachedCategory(videos = mapped, timestamp = System.currentTimeMillis(), page = 1)
                    saveCache()
                    return mapped
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    suspend fun prefetchHub(hub: VidNutzHub): Map<String, List<VidNutzVideo>> {
        loadCache()
        val cached = hubCache[hub.id]
        if (cached != null && !cached.isExpired) {
            return cached.sections
        }
        return coroutineScope {
            val sections = hub.subs.map { sub ->
                async { sub.id to getSubVideos(sub.id, sub.queries, 12) }
            }.awaitAll().toMap()
            if (sections.values.any { it.isNotEmpty() }) {
                hubCache[hub.id] = CachedHub(sections = sections, timestamp = System.currentTimeMillis())
                saveCache()
            }
            sections
        }
    }

    fun getCachedCategory(category: VidNutzCategory): List<VidNutzVideo> {
        loadCache()
        val diskCache = categoryCache[category]
        if (diskCache != null && !diskCache.isExpired) {
            return diskCache.videos
        }
        return emptyList()
    }

    fun getCachedHub(hub: VidNutzHub): Map<String, List<VidNutzVideo>> {
        loadCache()
        val cached = hubCache[hub.id]
        if (cached != null && !cached.isExpired) {
            return cached.sections
        }
        return emptyMap()
    }

    suspend fun getHubSections(hub: VidNutzHub): Map<String, List<VidNutzVideo>> {
        loadCache()
        val cached = hubCache[hub.id]
        if (cached != null && !cached.isExpired) {
            return cached.sections
        }
        return coroutineScope {
            val sections = hub.subs.map { sub ->
                async { sub.id to getSubVideos(sub.id, sub.queries, 12) }
            }.awaitAll().toMap()
            if (sections.values.any { it.isNotEmpty() }) {
                hubCache[hub.id] = CachedHub(sections = sections, timestamp = System.currentTimeMillis())
                saveCache()
            }
            sections
        }
    }

    fun invalidateEngineCache() {
        engineCache = mutableMapOf()
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

    private fun videoUploadTimestamp(uploadDate: String): Long {
        if (uploadDate.isBlank()) return 0L
        val now = System.currentTimeMillis()
        val lower = uploadDate.lowercase()
        val rel = Regex("""(\d+)\s*(second|minute|hour|day|week|month|year)s?""").find(lower)
        if (rel != null) {
            val count = rel.groupValues[1].toLong()
            val unit = rel.groupValues[2]
            return when {
                unit == "second" -> now - count * 1000L
                unit == "minute" -> now - count * 60_000L
                unit == "hour" -> now - count * 3_600_000L
                unit == "day" -> now - count * 86_400_000L
                unit == "week" -> now - count * 604_800_000L
                unit == "month" -> now - count * 2_592_000_000L
                unit == "year" -> now - count * 31_536_000_000L
                else -> 0L
            }
        }
        return try {
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).parse(uploadDate)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

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

    data class PipedSearchItem(
        val url: String = "",
        val title: String = "",
        val uploader: String = "",
        val duration: Long = 0,
        val views: Long = 0,
        val uploadedDate: String = "",
    )

    data class PipedSearchResponse(
        val items: List<PipedSearchItem> = emptyList(),
    )

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