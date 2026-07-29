package com.nuvio.app.features.hub

data class VidNutzVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val durationSeconds: Int,
    val viewCount: Long = 0,
    val uploadDate: String = "",
    val isLive: Boolean = false,
)

enum class VidNutzCategory(val displayName: String) {
    TRENDING("Trending"),
    LIVE("Live"),
    POLITICS("Politics"),
    NEWS("News"),
    MUSIC("Music"),
    SPORTS("Sports"),
    DOCUMENTARY("Documentary"),
    TECHNOLOGY("Technology"),
    ENTERTAINMENT("Entertainment"),
    COMEDY("Comedy"),
    SCIENCE("Science"),
    TRUE_CRIME("True Crime"),
    FOOD_DRINK("Food & Drink"),
}

data class VidNutzUiState(
    val selectedCategory: VidNutzCategory = VidNutzCategory.TRENDING,
    val videos: List<VidNutzVideo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<VidNutzVideo>? = null,
    val searchCurrentPage: Int = 1,
    val searchHasMore: Boolean = true,
    val resolvingVideoId: String? = null,
    val recentSearches: List<String> = emptyList(),
)
