package com.nuvio.app.features.iptv

import kotlinx.serialization.Serializable

@Serializable
data class EspnNewsResponse(
    val articles: List<EspnNewsArticle> = emptyList(),
)

@Serializable
data class EspnNewsArticle(
    val headline: String = "",
    val description: String = "",
    val links: EspnNewsLinks? = null,
    val images: List<EspnNewsImage> = emptyList(),
    val published: String = "",
    val categories: List<EspnNewsCategory> = emptyList(),
    val byline: String = "",
)

@Serializable
data class EspnNewsLinks(val web: EspnNewsWebLink? = null)

@Serializable
data class EspnNewsWebLink(val href: String = "")

@Serializable
data class EspnNewsImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val alt: String = "",
)

@Serializable
data class EspnNewsCategory(
    val description: String = "",
    val type: String = "",
)
