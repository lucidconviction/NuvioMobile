package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnNewsArticle
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvChannel
import com.nuvio.app.features.iptv.SourceType

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

/** A single standings table row with a stat map keyed by ESPN stat name. */
data class StandingsRow(
    val teamId: String = "",
    val teamName: String = "",
    val abbreviation: String = "",
    val logo: String? = null,
    val color: String? = null,
    val rank: Int = 0,
    val stats: Map<String, String> = emptyMap(),
) {
    fun stat(name: String): String = stats[name] ?: "-"
}

/** One grouped standings table (conference/division/poll/championship). */
data class StandingsGroup(
    val name: String = "",
    val rows: List<StandingsRow> = emptyList(),
)

enum class StandingsLayout { STANDARD_TEAM, SOCCER, COLLEGE, COMBAT, INDIVIDUAL, MOTORSPORT, NONE }

/** Classify a league into a Standings & Rankings layout model. */
fun detectStandingsLayout(league: SportLeague): StandingsLayout = when (league.id) {
    "nfl", "nba", "mlb", "nhl", "wnba" -> StandingsLayout.STANDARD_TEAM
    "mls", "epl", "laliga", "seriea", "bundes", "ligue1", "ucl" -> StandingsLayout.SOCCER
    "cfb", "cbb" -> StandingsLayout.COLLEGE
    "ufc", "pfl", "bkfc", "powerslap", "boxing" -> StandingsLayout.COMBAT
    "tennis", "golf" -> StandingsLayout.INDIVIDUAL
    "f1" -> StandingsLayout.MOTORSPORT
    else -> StandingsLayout.NONE
}

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

@kotlinx.serialization.Serializable
data class MatchedChannel(
    val channel: IptvChannel,
    val matchType: MatchType,
    val sourceName: String = "",
    val region: String = "",
    val providerGroup: SourceType,
    val score: Int = 0,
    val reasons: List<String> = emptyList(),
)

enum class MatchType(val label: String) {
    LEAGUE("League"),
    TEAM("Team"),
    GENERAL_SPORTS("Sports"),
}

enum class EventTab(val label: String) {
    LIVE("Watch Live"),
    HIGHLIGHTS("Highlights"),
    NEWS("News"),
    PRE_MATCH("Pre-Match"),
}

data class SportsUiState(
    val events: List<EspnProcessedEvent> = emptyList(),
    val news: List<EspnNewsArticle> = emptyList(),
    val selectedSport: String? = null,
    val selectedDate: String = "",
    val selectedSeason: Int = 0,
    val isLoading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val highlightVideos: List<HighlightVideo> = emptyList(),
    val standings: List<TeamStanding> = emptyList(),
    val standingsGroups: List<StandingsGroup> = emptyList(),
    val trendingNewsVideos: List<YouTubeVideo> = emptyList(),
    val leagueNews: List<EspnNewsArticle> = emptyList(),
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
    val aliveByUrl: Map<String, Boolean> = emptyMap(),
    val sportEventVideos: List<SportEventVideo> = emptyList(),
    val sportVideosLoading: Boolean = false,
    val regionFilter: String = "ALL",
    val userRegion: BroadcastRegion = BroadcastRegion.OTHER,
    val activeEventTab: EventTab = EventTab.LIVE,
    val daddyLiveEvents: List<DaddyLiveEvent> = emptyList(),
    val daddyLiveLoading: Boolean = false,
    val bkfcEvents: List<BkfcEvent> = emptyList(),
    val bkfcLoading: Boolean = false,
    val boxingEvents: List<BoxingSceneEvent> = emptyList(),
    val boxingLoading: Boolean = false,
    val pflEvents: List<PflEvent> = emptyList(),
    val pflLoading: Boolean = false,
    val powerSlapEvents: List<PowerSlapEvent> = emptyList(),
    val powerSlapLoading: Boolean = false,
    val sync2CalEventsByLeague: Map<String, List<Sync2CalEvent>> = emptyMap(),
    val sync2CalLoading: Boolean = false,
    val sync2CalTvChannels: Map<Long, List<Sync2CalTvChannel>> = emptyMap(),
    val upcomingFixtures: List<EspnProcessedEvent> = emptyList(),
    val fixturesLoading: Boolean = false,
    val eventNews: List<EspnNewsArticle> = emptyList(),
    val eventNewsLoading: Boolean = false,
)

sealed class SportsTab {
    data object Scores : SportsTab()
    data object News : SportsTab()
    data object Highlights : SportsTab()
}
