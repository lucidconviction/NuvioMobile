package com.nuvio.app.features.hub

import com.nuvio.app.features.addons.httpGetText
import kotlinx.serialization.json.Json

/**
 * Podcast discovery via the Apple/iTunes Search API + episode parsing from RSS feeds.
 * No API keys required. KMP-safe (no Android/xmlpull dependencies).
 */
object PodNutzRepository {
    private const val ITUNES_SEARCH = "https://itunes.apple.com/search"
    private const val MAX_FEED_CHARS = 6_000_000
    private val json = Json { ignoreUnknownKeys = true }

    private val featuredTerms = listOf("true crime", "comedy", "news", "sports", "technology", "history")

    suspend fun fetchFeatured(): List<Podcast> {
        val seen = LinkedHashMap<String, Podcast>()
        for (term in featuredTerms) {
            for (pod in searchPodcasts(term)) {
                if (pod.id !in seen) seen[pod.id] = pod
                if (seen.size >= 24) return seen.values.toList()
            }
        }
        return seen.values.toList()
    }

    suspend fun searchPodcasts(term: String): List<Podcast> {
        if (term.isBlank()) return emptyList()
        val url = "$ITUNES_SEARCH?term=${encodeUrl(term)}&media=podcast&limit=25"
        return runCatching {
            val body = httpGetText(url)
            val response = json.decodeFromString<ItunesPodcastSearchResponse>(body)
            response.results.filter { !it.feedUrl.isNullOrBlank() }.map {
                Podcast(
                    id = it.collectionId.toString(),
                    name = it.collectionName,
                    artist = it.artistName,
                    artwork = it.artworkUrl600?.takeIf { a -> a.isNotBlank() },
                    feedUrl = it.feedUrl.orEmpty(),
                    genres = it.genres,
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun fetchEpisodes(feedUrl: String): List<PodcastEpisode> {
        if (feedUrl.isBlank()) return emptyList()
        return runCatching {
            val body = httpGetText(feedUrl)
            parseEpisodes(body)
        }.getOrElse { emptyList() }
    }

    private fun parseEpisodes(body: String): List<PodcastEpisode> {
        val text = if (body.length > MAX_FEED_CHARS) {
            val cut = body.substring(0, MAX_FEED_CHARS)
            val lastEnd = cut.lastIndexOf("</item>", ignoreCase = true)
            if (lastEnd >= 0) cut.substring(0, lastEnd + "</item>".length) else cut
        } else {
            body
        }
        val episodes = mutableListOf<PodcastEpisode>()
        var idx = 0
        while (true) {
            val start = text.indexOf("<item", idx, ignoreCase = true)
            if (start < 0) break
            val end = text.indexOf("</item>", start, ignoreCase = true)
            if (end < 0) break
            val block = text.substring(start, end + "</item>".length)
            parseEpisodeItem(block)?.let { episodes.add(it) }
            idx = end + "</item>".length
        }
        return episodes.filter { it.audioUrl.isNotBlank() }
    }

    private fun parseEpisodeItem(block: String): PodcastEpisode? {
        val title = extractTag(block, "title") ?: "Episode"
        val description = extractTag(block, "description").orEmpty()
        val pubDate = extractTag(block, "pubdate").orEmpty()
        val duration = extractTag(block, "itunes:duration")
            ?: extractTag(block, "media:duration")
            ?: extractTag(block, "duration")
            .orEmpty()
        val audioUrl = extractEnclosureUrl(block)
        val artwork = extractArtwork(block)
        if (audioUrl.isBlank()) return null
        return PodcastEpisode(
            id = audioUrl.hashCode().toString(),
            title = title,
            description = description,
            pubDate = pubDate,
            duration = duration,
            audioUrl = audioUrl,
            artwork = artwork,
        )
    }

    private fun extractTag(block: String, tag: String): String? {
        val regex = Regex("""<$tag\b[^>]*>([\s\S]*?)</$tag>""", RegexOption.IGNORE_CASE)
        val match = regex.find(block) ?: return null
        return cleanText(match.groupValues[1])
    }

    private fun extractEnclosureUrl(block: String): String {
        val regex = Regex("""<enclosure\b[^>]*?url="([^"]+)"""", RegexOption.IGNORE_CASE)
        return regex.find(block)?.groupValues?.get(1)?.trim().orEmpty()
    }

    private fun extractArtwork(block: String): String? {
        val itunes = Regex("""<itunes:image\b[^>]*?href="([^"]+)"""", RegexOption.IGNORE_CASE)
            .find(block)?.groupValues?.get(1)?.trim()
        if (!itunes.isNullOrBlank()) return itunes
        val generic = Regex("""<image\b[^>]*?href="([^"]+)"""", RegexOption.IGNORE_CASE)
            .find(block)?.groupValues?.get(1)?.trim()
        return generic?.takeIf { it.isNotBlank() }
    }

    private fun cleanText(raw: String): String {
        var s = raw.trim()
        val cdata = Regex("""<!\[CDATA\[([\s\S]*?)\]\]>""", RegexOption.IGNORE_CASE).find(s)
        if (cdata != null) s = cdata.groupValues[1]
        return s
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun encodeUrl(s: String): String =
        s.replace(" ", "+")
            .replace(",", "%2C")
            .replace("&", "%26")
            .replace("?", "%3F")
            .replace("#", "%23")
}