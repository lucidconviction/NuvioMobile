package com.nuvio.app.features.sports

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkHttpRequest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.concurrent.TimeUnit

private class NewPipeDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody("application/octet-stream".toMediaType())
        val okRequest = OkHttpRequest.Builder()
            .url(request.url())
            .method(request.httpMethod(), body)
            .apply {
                request.headers().forEach { (key, values) ->
                    values.forEach { value -> addHeader(key, value) }
                }
            }
            .build()
        val okResponse = client.newCall(okRequest).execute()
        val responseBody = okResponse.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until okResponse.headers.size) {
            val name = okResponse.headers.name(i)
            val value = okResponse.headers.value(i)
            responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
        }
        return Response(
            okResponse.code,
            okResponse.message,
            responseHeaders,
            responseBody,
            okResponse.request.url.toString()
        )
    }
}

actual suspend fun platformYouTubeSearch(query: String): List<YouTubeVideo>? {
    return withContext(Dispatchers.IO) {
        try {
            withTimeout(10_000L) {
                if (NewPipe.getDownloader() == null) {
                    NewPipe.init(NewPipeDownloader())
                }
                val extractor = ServiceList.YouTube.getSearchExtractor(query, emptyList(), "")
                extractor.fetchPage()
                val page = extractor.initialPage
                page.items?.mapNotNull { item ->
                    if (item is StreamInfoItem) {
                        val url = item.url ?: return@mapNotNull null
                        val videoId = url.removePrefix("https://www.youtube.com/watch?v=")
                            .substringBefore("&")
                        YouTubeVideo(
                            videoId = videoId,
                            title = item.name ?: "",
                            thumbnail = "https://img.youtube.com/vi/${videoId}/mqdefault.jpg",
                            channelName = item.uploaderName ?: "",
                            durationSeconds = (item.duration ?: 0L).toInt(),
                        )
                    } else null
                }?.filter { it.durationSeconds in 30..1800 }?.take(28)?.ifEmpty { null }
            }
        } catch (_: Exception) { null }
    }
}

actual suspend fun platformResolveYouTubeStream(videoId: String): StreamResult? {
    return withContext(Dispatchers.IO) {
        try {
            withTimeout(15_000L) {
                if (NewPipe.getDownloader() == null) {
                    NewPipe.init(NewPipeDownloader())
                }
                val extractor = ServiceList.YouTube.getStreamExtractor(
                    "https://www.youtube.com/watch?v=$videoId"
                )
                extractor.fetchPage()

                val videoStreams = extractor.videoStreams
                val best = videoStreams
                    .filter { it.isVideoOnly.not() }
                    .sortedByDescending { extractResolution(it.resolution) }
                    .firstOrNull()
                val streamUrl = best?.url
                if (streamUrl != null) {
                    StreamResult(
                        url = streamUrl,
                        headers = mapOf("Referer" to "https://www.youtube.com"),
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) { null }
    }
}

private fun extractResolution(resolution: String?): Int {
    if (resolution == null) return 0
    return resolution.filter { it.isDigit() }.toIntOrNull() ?: 0
}
