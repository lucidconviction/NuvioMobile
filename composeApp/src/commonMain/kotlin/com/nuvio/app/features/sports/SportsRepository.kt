package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.player.SportsNowStore
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SportsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null

    val leagues: List<SportLeague> = listOf(
        SportLeague("ufc", "UFC MMA", "UFC", "mma/ufc"),
        SportLeague("boxing", "Boxing", "BOX", "boxing/_"),
        SportLeague("pfl", "PFL MMA", "PFL", "mma/pfl"),
        SportLeague("nfl", "NFL Football", "NFL", "football/nfl"),
        SportLeague("nba", "NBA Basketball", "NBA", "basketball/nba"),
        SportLeague("mlb", "MLB Baseball", "MLB", "baseball/mlb"),
        SportLeague("nhl", "NHL Hockey", "NHL", "hockey/nhl"),
        SportLeague("soccer", "MLS Soccer", "MLS", "soccer/usa.1"),
    )

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.events.isEmpty(), error = null)
            val trendingVideos = fetchTrendingNewsVideos()
            _uiState.value = _uiState.value.copy(
                trendingNewsVideos = trendingVideos,
                isLoading = false,
            )
        }
    }

    fun loadLeagueEvents(league: SportLeague, date: String? = null) {
        refreshJob?.cancel()
        val parts = league.slug.split("/")
        if (parts.size < 2) return
        val sport = parts[0]
        val leagueName = parts[1]
        val queryDate = date ?: _uiState.value.selectedDate.ifBlank { null }

        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val events = EspnClient.fetchLeague(sport, leagueName, queryDate)
                val highlights = YouTubeHighlightClient.searchHighlights("${league.name} highlights")
                val highlightVideos = if (highlights.isNotEmpty()) {
                    listOf(HighlightVideo(eventId = "league_${league.id}", video = highlights.first(), sport = sport))
                } else emptyList()
                val trendingVids = if (_uiState.value.trendingNewsVideos.isEmpty()) fetchTrendingNewsVideos()
                    else _uiState.value.trendingNewsVideos

                _uiState.value = _uiState.value.copy(
                    events = events,
                    highlightVideos = highlightVideos,
                    trendingNewsVideos = trendingVids,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadAllLiveEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(allLiveLoading = true)
            val allEvents = EspnClient.fetchAll().filter { it.isLive }
            SportsNowStore.liveEvents = allEvents
            _uiState.value = _uiState.value.copy(allLiveEvents = allEvents, allLiveLoading = false)
        }
    }

    fun selectLeague(league: SportLeague?) {
        _uiState.value = _uiState.value.copy(selectedLeague = league)
    }

    fun selectEvent(event: EspnProcessedEvent?) {
        _uiState.value = _uiState.value.copy(selectedEvent = event)
        if (event != null) {
            loadMatchedChannels(event)
        }
    }

    private fun loadMatchedChannels(event: EspnProcessedEvent) {
        scope.launch {
            _uiState.value = _uiState.value.copy(channelsLoading = true)
            val allChannels = IptvRepository.getAllChannels()
            val matches = GameToChannelMatcher.matchChannels(event, allChannels)
                .map { it.copy(region = GameToChannelMatcher.detectRegion(it.channel)) }
            _uiState.value = _uiState.value.copy(matchedChannels = matches, channelsLoading = false)
        }
    }

    fun searchSportVideos(event: EspnProcessedEvent, isFuture: Boolean) {
        scope.launch {
            _uiState.value = _uiState.value.copy(sportVideosLoading = true, sportEventVideos = emptyList())
            val rawTitle = event.title.ifBlank { "${event.awayTeam} vs ${event.homeTeam}" }
            val matchName = rawTitle.replace(" @ ", " vs ").replace(" at ", " vs ")
            val queries = if (isFuture) listOf("$matchName preview", "$matchName predictions", "$matchName weigh in")
            else listOf("$matchName highlights", "$matchName full fight", "$matchName post fight analysis")
            val allVideos = mutableListOf<SportEventVideo>()
            val seen = mutableSetOf<String>()
            for (query in queries) {
                try {
                    val results = YouTubeHighlightClient.searchHighlights(query)
                    for (v in results) {
                        if (v.videoId !in seen) {
                            seen.add(v.videoId)
                            allVideos.add(SportEventVideo(
                                videoId = v.videoId, title = v.title, thumbnailUrl = v.thumbnail,
                                channelName = v.channelName, durationSeconds = v.durationSeconds,
                                category = query.substringAfterLast(" ").ifEmpty { "highlights" },
                            ))
                        }
                    }
                } catch (_: Exception) {}
                if (allVideos.size >= 30) break
            }
            _uiState.value = _uiState.value.copy(sportEventVideos = allVideos, sportVideosLoading = false)
        }
    }

    fun setRegionFilter(region: String) {
        _uiState.value = _uiState.value.copy(regionFilter = region)
    }

    fun setActiveEventTab(tab: EventTab) {
        _uiState.value = _uiState.value.copy(activeEventTab = tab)
    }

    fun clearSportSelection() {
        _uiState.value = _uiState.value.copy(
            selectedEvent = null, matchedChannels = emptyList(),
            sportEventVideos = emptyList(), regionFilter = "ALL", activeEventTab = EventTab.LIVE,
        )
    }

    fun startAutoRefresh(league: SportLeague) {
        autoRefreshJob?.cancel()
        autoRefreshJob = scope.launch {
            while (true) {
                delay(45_000)
                loadLeagueEvents(league)
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun searchSports(query: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = true)
            try {
                val videos = YouTubeHighlightClient.searchHighlights(query)
                val queryLower = query.lowercase()
                val matchedEvents = _uiState.value.events.filter { event ->
                    event.title.lowercase().contains(queryLower) ||
                    event.homeTeam.lowercase().contains(queryLower) ||
                    event.awayTeam.lowercase().contains(queryLower) ||
                    event.league.lowercase().contains(queryLower)
                }
                _uiState.value = _uiState.value.copy(searchResults = videos, searchedEvents = matchedEvents, isSearching = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "", searchResults = emptyList(), searchedEvents = emptyList(), isSearching = false)
    }

    fun loadStandings(league: SportLeague, season: Int? = null) {
        scope.launch {
            val parts = league.slug.split("/")
            if (parts.size < 2) return@launch
            val sport = parts[0]
            val leagueName = parts[1]
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val standings = EspnClient.fetchStandings(sport, leagueName, season)
                _uiState.value = _uiState.value.copy(standings = standings, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    val availableSeasons: List<Int> = (2020..2026).toList().reversed()

    fun selectSport(sport: String?) {
        _uiState.value = _uiState.value.copy(selectedSport = sport)
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        val league = _uiState.value.selectedLeague
        if (league != null && league.id != "now") {
            loadLeagueEvents(league, date)
        }
    }

    fun addDays(date: String, days: Int): String {
        var (y, m, d) = parseDate(date)
        var totalDays = daysToEpoch(y, m, d) + days
        if (totalDays < 0) totalDays = 0L
        val (ny, nm, nd) = epochToDate(totalDays)
        return "${ny}-${nm.toString().padStart(2, '0')}-${nd.toString().padStart(2, '0')}"
    }

    fun getToday(): String = formatToday()

    fun isToday(date: String): Boolean = date == formatToday()

    fun dateLabel(date: String): String {
        if (isToday(date)) return "Today"
        val yesterday = addDays(formatToday(), -1)
        if (date == yesterday) return "Yesterday"
        val tomorrow = addDays(formatToday(), 1)
        if (date == tomorrow) return "Tomorrow"
        val (y, m, day) = parseDate(date)
        val days = daysToEpoch(y, m, day)
        val dayOfWeek = ((days + 3) % 7 + 7).toInt() % 7
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayName = if (dayOfWeek in names.indices) names[dayOfWeek] else ""
        return "$dayName $day"
    }

    private fun parseDate(date: String): Triple<Int, Int, Int> {
        val parts = date.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 1970
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1
        return Triple(y, m, d)
    }

    private fun daysToEpoch(year: Int, month: Int, day: Int): Long {
        var y = year.toLong()
        var m = month
        if (m <= 2) { y--; m += 12 }
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    private fun epochToDate(days: Long): Triple<Int, Int, Int> {
        var z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val dayNum = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val monthNum = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val yearNum = if (monthNum <= 2) y.toInt() + 1 else y.toInt()
        return Triple(yearNum, monthNum, dayNum)
    }

    private fun formatToday(): String {
        val epochMs = TraktPlatformClock.nowEpochMs() + TraktPlatformClock.localTimezoneOffsetMs()
        val daysSinceEpoch = epochMs / 86_400_000L
        var y = 1970L
        var remaining = daysSinceEpoch
        while (true) {
            val daysInYear = if (isLeapYear(y)) 366 else 365
            if (remaining < daysInYear) break
            remaining -= daysInYear
            y++
        }
        val monthDays = if (isLeapYear(y)) intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        else intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var m = 0
        while (m < 12 && remaining >= monthDays[m]) { remaining -= monthDays[m]; m++ }
        val month = m + 1
        val day = remaining.toInt() + 1
        return "${y}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun isLeapYear(y: Long): Boolean = (y % 4 == 0L && y % 100 != 0L) || (y % 400 == 0L)

    private suspend fun fetchTrendingNewsVideos(): List<YouTubeVideo> {
        val queries = listOf("sports news today", "NFL highlights", "NBA highlights", "MLB highlights", "NHL highlights", "soccer highlights", "UFC news")
        for (query in queries) {
            try {
                val results = YouTubeHighlightClient.searchHighlights(query)
                if (results.isNotEmpty()) return results.take(8)
            } catch (_: Exception) {}
        }
        return emptyList()
    }
}
