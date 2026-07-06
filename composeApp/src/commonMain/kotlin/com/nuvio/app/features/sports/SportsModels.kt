package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnNewsArticle
import com.nuvio.app.features.iptv.EspnProcessedEvent

data class TeamStanding(
    val teamName: String,
    val logo: String?,
    val record: String,
    val league: String,
    val sport: String,
)

data class HighlightVideo(
    val eventId: String,
    val video: YouTubeVideo,
    val sport: String = "",
)

data class SportsUiState(
    val events: List<EspnProcessedEvent> = emptyList(),
    val news: List<EspnNewsArticle> = emptyList(),
    val selectedSport: String? = null,
    val selectedDate: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val highlightVideos: List<HighlightVideo> = emptyList(),
    val standings: List<TeamStanding> = emptyList(),
    val trendingNewsVideos: List<YouTubeVideo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<YouTubeVideo> = emptyList(),
    val searchedEvents: List<EspnProcessedEvent> = emptyList(),
    val isSearching: Boolean = false,
)

sealed class SportsTab {
    data object Scores : SportsTab()
    data object News : SportsTab()
    data object Highlights : SportsTab()
}
