package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class WatchFootyNewsResponse(
    val articles: List<WatchFootyNewsArticle> = emptyList(),
    val pagination: WatchFootyPagination? = null,
)

@Serializable
data class WatchFootyPagination(
    val limit: Int = 0,
    val offset: Int = 0,
    val nextOffset: Int = 0,
)

@Serializable
data class WatchFootyNewsArticle(
    val id: String = "",
    val headline: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val publishedAt: String = "",
    val editedAt: String? = null,
    val sport: String = "",
    val author: String = "",
    val url: String = "",
)
