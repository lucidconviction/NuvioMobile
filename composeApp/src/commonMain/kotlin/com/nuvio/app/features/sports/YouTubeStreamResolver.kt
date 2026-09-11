package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpPostJsonWithHeaders
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class PipedStreamItem(
    val url: String = "",
    val quality: String = "",
    val format: String = "",
)

@Serializable
data class PipedStreamsResponse(
    val title: String = "",
    val uploader: String = "",
    val duration: Long = 0,
    val thumbnailUrl: String = "",
    val hls: String? = null,
    val videoStreams: List<PipedStreamItem> = emptyList(),
    val audioStreams: List<PipedStreamItem> = emptyList(),
)

data class StreamResult(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val audioUrl: String? = null,
    val qualities: List<YoutubeQuality> = emptyList(),
)

object YouTubeStreamResolver {
    private const val TAG = "YouTubeStreamResolver"
    private const val FALLBACK_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val TIMEOUT_MS = 20_000L
    private const val CONFIG_TTL_MS = 3 * 60 * 60 * 1000L
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val PREFERRED_CLIENT = "android_vr"

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.lunar.icu",
        "https://piped-api.garudalinux.org",
    )

    private val json = Json { ignoreUnknownKeys = true }

    private data class CachedConfig(
        val apiKey: String,
        val visitorData: String?,
        val fetchedAt: Long = currentTimeMillis(),
    )
    private var cachedConfig: CachedConfig? = null
    private val resultCache = mutableMapOf<String, CacheEntry>()

    private data class CacheEntry(
        val result: YoutubeStreamResult,
        val fetchedAt: Long = currentTimeMillis(),
    )

    private val clientConfigs = listOf(
        ClientConfig(
            name = "android_vr",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12; en_US; Quest 3)",
            clientName = "ANDROID_VR", clientVersion = "1.56.21",
            deviceMake = "Oculus", deviceModel = "Quest 3",
            osName = "Android", osVersion = "12", androidSdkVersion = 32,
        ),
        ClientConfig(
            name = "android",
            userAgent = "com.google.android.youtube/20.10.35 (Linux; U; Android 14; en_US)",
            clientName = "ANDROID", clientVersion = "20.10.35",
            osName = "Android", osVersion = "14", androidSdkVersion = 34,
        ),
        ClientConfig(
            name = "ios",
            userAgent = "com.google.ios.youtube/20.10.1 (iPhone16,2; U; CPU iOS 17_4)",
            clientName = "IOS", clientVersion = "20.10.1",
            deviceModel = "iPhone16,2", osName = "iPhone", osVersion = "17.4.0",
        ),
    )

    private data class ClientConfig(
        val name: String,
        val userAgent: String,
        val clientName: String,
        val clientVersion: String,
        val deviceMake: String = "unknown",
        val deviceModel: String = "unknown",
        val osName: String = "Android",
        val osVersion: String = "14",
        val androidSdkVersion: Int = 34,
    )

    suspend fun resolveStream(videoId: String): StreamResult? {
        return resolveStreamResult(videoId)?.let {
            StreamResult(url = it.videoUrl, headers = mapOf("Referer" to "https://www.youtube.com"),
                audioUrl = it.audioUrl, qualities = it.qualities)
        }
    }

    suspend fun resolveStreamResult(videoId: String): YoutubeStreamResult? {
        if (videoId.isBlank() || !videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return null

        val now = currentTimeMillis()
        val cached = resultCache[videoId]
        if (cached != null && now - cached.fetchedAt < CACHE_TTL_MS) {
            return cached.result
        }

        val innerTubeResult = runCatching {
            withTimeout(TIMEOUT_MS) { resolveInnerTube(videoId, forceRefresh = false) }
        }.getOrNull() ?: runCatching {
            withTimeout(TIMEOUT_MS) { resolveInnerTube(videoId, forceRefresh = true) }
        }.getOrNull()

        val finalResult = if (innerTubeResult != null && innerTubeResult.audioUrl != null) {
            innerTubeResult
        } else {
            innerTubeResult ?: runCatching {
                withTimeout(TIMEOUT_MS) { resolvePiped(videoId) }
            }.getOrNull() ?: runCatching {
                withTimeout(TIMEOUT_MS) { resolvePlatform(videoId) }
            }.getOrNull()
        }

        if (finalResult != null) {
            resultCache[videoId] = CacheEntry(finalResult)
        }
        return finalResult
    }

    private suspend fun getWatchConfig(): CachedConfig {
        val now = currentTimeMillis()
        val current = cachedConfig
        if (current != null && now - current.fetchedAt < CONFIG_TTL_MS) {
            return current
        }

        return runCatching {
            val html = httpGetText("https://www.youtube.com/watch?v=dQw4w9WgXcQ&hl=en")
            val apiKey = API_KEY_REGEX.find(html)?.groupValues?.getOrNull(1) ?: FALLBACK_API_KEY
            val visitorData = VISITOR_DATA_REGEX.find(html)?.groupValues?.getOrNull(1)
            CachedConfig(apiKey = apiKey, visitorData = visitorData).also { cachedConfig = it }
        }.getOrElse {
            current ?: CachedConfig(apiKey = FALLBACK_API_KEY, visitorData = null)
        }
    }

    private suspend fun resolveInnerTube(videoId: String, forceRefresh: Boolean): YoutubeStreamResult? {
        if (forceRefresh) cachedConfig = null
        val config = getWatchConfig()

        val progressiveVideos = mutableListOf<Pair<Int, String>>()
        val adaptiveVideos = mutableListOf<Pair<Int, String>>()
        var allAudio: String? = null
        var hasLoginRequired = false
        var hlsUrl: String? = null

        for (clientCfg in clientConfigs) {
            try {
                val context = buildString {
                    append(""""client":{"clientName":"${clientCfg.clientName}","clientVersion":"${clientCfg.clientVersion}"""")
                    append(""","deviceMake":"${clientCfg.deviceMake}","deviceModel":"${clientCfg.deviceModel}"""")
                    append(""","osName":"${clientCfg.osName}","osVersion":"${clientCfg.osVersion}"""")
                    append(""","platform":"MOBILE","androidSdkVersion":${clientCfg.androidSdkVersion},"hl":"en","gl":"US"}""")
                }
                val payload = """{"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true,"context":{$context},"playbackContext":{"contentPlaybackContext":{"html5Preference":"HTML5_PREF_WANTS"}}}"""

                val endpoint = "https://www.youtube.com/youtubei/v1/player?key=${config.apiKey}"
                val headers = mutableMapOf(
                    "Content-Type" to "application/json",
                    "User-Agent" to clientCfg.userAgent,
                    "accept-language" to "en-US,en;q=0.9",
                    "origin" to "https://www.youtube.com",
                )
                val vd = config.visitorData
                if (!vd.isNullOrBlank()) headers["x-goog-visitor-id"] = vd

                val body = httpPostJsonWithHeaders(endpoint, payload, headers)
                val root = json.parseToJsonElement(body).jsonObject

                val playbackStatus = root["playabilityStatus"]?.jsonObject
                val status = playbackStatus?.get("status")?.jsonPrimitive?.contentOrNull
                if (status == "LOGIN_REQUIRED" || status == "UNPLAYABLE") {
                    if (status == "LOGIN_REQUIRED") hasLoginRequired = true
                    continue
                }

                val streamingData = root["streamingData"]?.jsonObject ?: continue

                if (hlsUrl == null) {
                    hlsUrl = streamingData["hlsManifestUrl"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                }

                streamingData["formats"]?.jsonArray?.forEach { fmt ->
                    val fmtObj = fmt.jsonObject
                    val mimeType = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                    val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    if (mimeType.startsWith("video/")) {
                        val height = fmtObj["height"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                        progressiveVideos.add(height to url)
                    } else if (mimeType.startsWith("audio/")) {
                        if (allAudio == null) allAudio = url
                    }
                }

                streamingData["adaptiveFormats"]?.jsonArray?.forEach { fmt ->
                    val fmtObj = fmt.jsonObject
                    val mimeType = fmtObj["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                    val url = fmtObj["url"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    if (mimeType.startsWith("video/")) {
                        val height = fmtObj["height"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                        adaptiveVideos.add(height to url)
                    } else if (mimeType.startsWith("audio/")) {
                        val bitrate = fmtObj["bitrate"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                        if (allAudio == null || bitrate > 0) allAudio = url
                    }
                }

                val hasEmbeddedAudio = hlsUrl != null || progressiveVideos.isNotEmpty()
                if ((progressiveVideos.isNotEmpty() || adaptiveVideos.isNotEmpty()) && (allAudio != null || hasEmbeddedAudio)) break
            } catch (_: Exception) { }
        }

        // Priority: HLS > Progressive (has audio) > Adaptive + separate audio
        if (hlsUrl != null) {
            val qualities = progressiveVideos.plus(adaptiveVideos).sortedByDescending { it.first }.mapNotNull { (h, url) ->
                if (h > 0) YoutubeQuality(height = h, videoUrl = url, audioUrl = null) else null
            }
            return YoutubeStreamResult(videoUrl = hlsUrl, audioUrl = null, qualities = qualities)
        }

        if (progressiveVideos.isNotEmpty()) {
            val best = progressiveVideos.sortedByDescending { it.first }.first().second
            val qualities = progressiveVideos.plus(adaptiveVideos).sortedByDescending { it.first }.mapNotNull { (h, url) ->
                if (h > 0) YoutubeQuality(height = h, videoUrl = url, audioUrl = allAudio) else null
            }
            return YoutubeStreamResult(videoUrl = best, audioUrl = null, qualities = qualities)
        }

        if (adaptiveVideos.isEmpty()) return null

        val sorted = adaptiveVideos.sortedByDescending { it.first }
        val best = sorted.first().second
        val qualities = sorted.mapNotNull { (h, url) ->
            if (h > 0) YoutubeQuality(height = h, videoUrl = url, audioUrl = allAudio) else null
        }
        return YoutubeStreamResult(videoUrl = best, audioUrl = allAudio, qualities = qualities)
    }

    private suspend fun tryParseHlsQualities(hlsUrl: String): List<Pair<Int, String>> {
        return try {
            val m3u8 = httpGetText(hlsUrl)
            val lines = m3u8.lines()
            val results = mutableListOf<Pair<Int, String>>()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF:")) {
                    val resolutionMatch = Regex("RESOLUTION=\\d+x(\\d+)").find(line)
                    val height = resolutionMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                    i++
                    if (i < lines.size) {
                        val url = lines[i].trim()
                        if (url.isNotBlank() && !url.startsWith("#")) {
                            results.add(height to (if (url.startsWith("http")) url else resolveRelativeUrl(hlsUrl, url)))
                        }
                    }
                }
                i++
            }
            results
        } catch (_: Exception) { emptyList() }
    }

    private fun resolveRelativeUrl(base: String, relative: String): String {
        return try {
            val baseUrl = base.substringBeforeLast("/")
            "$baseUrl/$relative"
        } catch (_: Exception) { relative }
    }

    private suspend fun resolvePiped(videoId: String): YoutubeStreamResult? {
        for (instance in pipedInstances) {
            try {
                val response = httpGetText("$instance/streams/$videoId")
                val parsed = json.decodeFromString<PipedStreamsResponse>(response)

                val useHls = parsed.hls?.takeIf { it.isNotBlank() }
                val videoUrl = useHls
                    ?: parsed.videoStreams
                        .sortedByDescending { extractQuality(it.quality) }
                        .firstOrNull()?.url
                        ?.takeIf { it.isNotBlank() } ?: continue

                val audioUrl = if (useHls != null) null else parsed.audioStreams
                    .sortedByDescending { extractQuality(it.quality) }
                    .firstOrNull()?.url?.takeIf { it.isNotBlank() }

                val qualities = parsed.videoStreams
                    .sortedByDescending { extractQuality(it.quality) }
                    .mapNotNull { item ->
                        val h = extractQuality(item.quality)
                        if (h > 0 && item.url.isNotBlank()) YoutubeQuality(height = h, videoUrl = item.url, audioUrl = audioUrl)
                        else null
                    }

                return YoutubeStreamResult(videoUrl = videoUrl, audioUrl = audioUrl, qualities = qualities)
            } catch (_: Exception) { }
        }
        return null
    }

    private suspend fun resolvePlatform(videoId: String): YoutubeStreamResult? {
        return try {
            val result = platformResolveYouTubeStream(videoId)
            result?.let {
                YoutubeStreamResult(videoUrl = it.url, qualities = emptyList())
            }
        } catch (_: Exception) { null }
    }

    private fun extractQuality(quality: String): Int {
        return quality.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    private val API_KEY_REGEX = Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"")
    private val VISITOR_DATA_REGEX = Regex("\"VISITOR_DATA\":\"([^\"]+)\"")

    private fun currentTimeMillis(): Long = com.nuvio.app.features.trakt.TraktPlatformClock.nowEpochMs()
}
