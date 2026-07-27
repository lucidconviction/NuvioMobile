package com.nuvio.app.features.sports

import kotlinx.serialization.Serializable

data class YoutubeStreamResult(
    val videoUrl: String,
    val audioUrl: String? = null,
    val qualities: List<YoutubeQuality> = emptyList(),
)

@Serializable
data class YoutubeQuality(
    val height: Int,
    val videoUrl: String,
    val audioUrl: String? = null,
)
