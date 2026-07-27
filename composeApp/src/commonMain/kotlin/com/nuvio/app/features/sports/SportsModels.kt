package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnNewsArticle
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel

data class TeamStanding(
    val teamName: String,
    val logo: String?,
    val record: String,
    val league: String,
    val sport: String,
    val rank: Int = 0,
    val netRating: String = "",
    val location: String = "",
)

data class HighlightVideo(
    val eventId: String,
    val video: YouTubeVideo,
    val sport: String = "",
)

data class SportLeague(
    val id: String,
    val name: String,
    val abbreviation: String,
    val slug: String,
    val logoUrl: String? = null,
)

data class SportEventVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val durationSeconds: Int,
    val category: String = "",
)

data class MatchedChannel(
    val channel: IptvChannel,
    val matchType: MatchType,
    val sourceName: String = "",
    val region: String = "",
)

enum class MatchType(val label: String) {
    LEAGUE("League"),
    TEAM("Team"),
    GENERAL_SPORTS("Sports"),
}

enum class EventTab(val label: String) {
    LIVE("Watch Live"),
    HIGHLIGHTS("Highlights"),
    PRE_MATCH("Pre-Match"),
}

data class SportsUiState(
    val events: List<EspnProcessedEvent> = emptyList(),
    val news: List<EspnNewsArticle> = emptyList(),
    val selectedSport: String? = null,
    val selectedDate: String = "",
    val selectedSeason: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val highlightVideos: List<HighlightVideo> = emptyList(),
    val standings: List<TeamStanding> = emptyList(),
    val trendingNewsVideos: List<YouTubeVideo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<YouTubeVideo> = emptyList(),
    val searchedEvents: List<EspnProcessedEvent> = emptyList(),
    val isSearching: Boolean = false,
    val selectedLeague: SportLeague? = null,
    val leagues: List<SportLeague> = emptyList(),
    val allLiveEvents: List<EspnProcessedEvent> = emptyList(),
    val allLiveLoading: Boolean = false,
    val selectedEvent: EspnProcessedEvent? = null,
    val matchedChannels: List<MatchedChannel> = emptyList(),
    val channelsLoading: Boolean = false,
    val sportEventVideos: List<SportEventVideo> = emptyList(),
    val sportVideosLoading: Boolean = false,
    val regionFilter: String = "ALL",
    val activeEventTab: EventTab = EventTab.LIVE,
    val daddyLiveEvents: List<DaddyLiveEvent> = emptyList(),
    val daddyLiveLoading: Boolean = false,
    val sync2CalEventsByLeague: Map<String, List<Sync2CalEvent>> = emptyMap(),
    val sync2CalLoading: Boolean = false,
    val sync2CalTvChannels: Map<Long, List<Sync2CalTvChannel>> = emptyMap(),
)

sealed class SportsTab {
    data object Scores : SportsTab()
    data object News : SportsTab()
    data object Highlights : SportsTab()
}
