package com.nuvio.app.features.iptv

import kotlinx.coroutines.flow.Flow

interface StreamThumbnailExtractor {
    suspend fun extractThumbnail(streamUrl: String, headers: Map<String, String> = emptyMap()): ThumbnailResult
}

data class ThumbnailResult(
    val thumbnailUri: String? = null,
    val error: String? = null,
) {
    companion object {
        fun success(thumbnailUri: String) = ThumbnailResult(thumbnailUri = thumbnailUri)
        fun failure(error: String) = ThumbnailResult(error = error)
    }
}

expect class StreamThumbnailExtractorFactory {
    companion object {
        fun create(): StreamThumbnailExtractor
    }
}