package com.nuvio.app.features.hub

import kotlinx.serialization.Serializable

@Serializable
data class Podcast(
    val id: String,
    val name: String,
    val artist: String,
    val artwork: String? = null,
    val feedUrl: String = "",
    val genres: List<String> = emptyList(),
)

@Serializable
data class PodcastEpisode(
    val id: String,
    val title: String,
    val description: String = "",
    val pubDate: String = "",
    val duration: String = "",
    val audioUrl: String = "",
    val artwork: String? = null,
)

@Serializable
internal data class ItunesPodcastSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesPodcastItem> = emptyList(),
)

@Serializable
internal data class ItunesPodcastItem(
    val collectionId: Long = 0,
    val collectionName: String = "",
    val artistName: String = "",
    val artworkUrl600: String? = null,
    val feedUrl: String? = null,
    val genres: List<String> = emptyList(),
)