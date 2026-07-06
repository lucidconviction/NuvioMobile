package com.nuvio.app.features.sports

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
)

object YouTubeStreamResolver {
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.lunar.icu",
        "https://piped-api.garudalinux.org",
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolveStream(videoId: String): StreamResult? {
        val platformResult = platformResolveYouTubeStream(videoId)
        if (platformResult != null) return platformResult

        for (instance in pipedInstances) {
            val result = tryPipedStream(instance, videoId)
            if (result != null) {
                return StreamResult(
                    url = result,
                    headers = mapOf("Referer" to "https://www.youtube.com"),
                )
            }
        }
        return null
    }

    private suspend fun tryPipedStream(instance: String, videoId: String): String? {
        return try {
            withTimeout(10_000L) {
                val response = httpGetText("$instance/streams/$videoId")
                val parsed = json.decodeFromString<PipedStreamsResponse>(response)

                parsed.hls?.takeIf { it.isNotBlank() }
                    ?: parsed.videoStreams
                        .sortedByDescending { extractQuality(it.quality) }
                        .firstOrNull()?.url
                        ?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) { null }
    }

    private fun extractQuality(quality: String): Int {
        return quality.filter { it.isDigit() }.toIntOrNull() ?: 0
    }
}
