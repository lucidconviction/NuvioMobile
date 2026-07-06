package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnNewsArticle
import com.nuvio.app.features.iptv.EspnNewsClient
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.SportsClient
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SportsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    private var cachedEvents: List<EspnProcessedEvent> = emptyList()
    private var cachedNews: List<EspnNewsArticle> = emptyList()
    private var cachedHighlights: List<HighlightVideo> = emptyList()
    private var cachedStandings: List<TeamStanding> = emptyList()
    private var cachedTrendingVideos: List<YouTubeVideo> = emptyList()
    private var hasCachedData = false
    private val perDateEventCache = mutableMapOf<String, List<EspnProcessedEvent>>()

    fun refresh() {
        val currentDate = _uiState.value.selectedDate.ifBlank { formatToday() }
        refresh(currentDate)
    }

    fun refresh(date: String) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val isDateSwitch = _uiState.value.selectedDate.isNotBlank() && _uiState.value.selectedDate != date

            if (!isDateSwitch) {
                val hasData = _uiState.value.events.isNotEmpty()
                if (!hasData && hasCachedData) {
                    _uiState.value = _uiState.value.copy(
                        events = cachedEvents,
                        news = cachedNews,
                        highlightVideos = cachedHighlights,
                        standings = cachedStandings,
                        trendingNewsVideos = cachedTrendingVideos,
                        isLoading = false,
                    )
                }
                _uiState.value = _uiState.value.copy(isLoading = !hasData && !hasCachedData, error = null)
            }

            try {
                val espnDeferred = async {
                    perDateEventCache.getOrPut(date) { EspnClient.fetchAll(date) }
                }
                val newsDeferred = if (!isDateSwitch) async { EspnNewsClient.fetchNews() } else null
                val sportsDbDeferred = async { fetchSportsDbEvents(date) }
                val trendingDeferred = if (!isDateSwitch) async { fetchTrendingNewsVideos() } else null

                val espnEvents = espnDeferred.await()
                val sportsDbEvents = sportsDbDeferred.await()
                val news = newsDeferred?.await() ?: _uiState.value.news
                val trendingVideos = trendingDeferred?.await() ?: _uiState.value.trendingNewsVideos

                val espnIds = espnEvents.map { it.id }.toSet()
                val merged = espnEvents + sportsDbEvents.filter { it.id !in espnIds }

                val highlightVideos = fetchYouTubeHighlights(merged, date)
                val standings = buildStandings(merged)

                perDateEventCache[date] = merged
                cachedEvents = merged
                cachedNews = if (!isDateSwitch) news else cachedNews
                cachedHighlights = highlightVideos
                cachedStandings = standings
                cachedTrendingVideos = if (!isDateSwitch) trendingVideos else cachedTrendingVideos
                hasCachedData = true

                _uiState.value = _uiState.value.copy(
                    events = merged,
                    news = if (news.isNotEmpty()) news else _uiState.value.news,
                    highlightVideos = highlightVideos,
                    standings = standings,
                    trendingNewsVideos = trendingVideos,
                    isLoading = false,
                    selectedDate = date,
                )

                if (!isDateSwitch) {
                    launch {
                        val prev = addDays(date, -1)
                        val next = addDays(date, 1)
                        if (!perDateEventCache.containsKey(prev)) {
                            try { perDateEventCache[prev] = EspnClient.fetchAll(prev) } catch (_: Exception) { }
                        }
                        if (!perDateEventCache.containsKey(next)) {
                            try { perDateEventCache[next] = EspnClient.fetchAll(next) } catch (_: Exception) { }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isDateSwitch) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sports",
                    )
                }
            }
        }
    }

    private suspend fun fetchSportsDbEvents(date: String): List<EspnProcessedEvent> {
        return try {
            val raw = SportsClient.fetchTodaysEvents(date)
            raw.map { it.toEspnProcessedEvent() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchYouTubeHighlights(events: List<EspnProcessedEvent>, date: String): List<HighlightVideo> = coroutineScope {
        val candidates = events.filter {
            it.status.contains("FINAL") || it.isLive || it.homeLogo != null
        }.take(12)

        val eventResults = candidates.map { event ->
            async {
                try {
                    val query = buildHighlightQuery(event)
                    val videos = YouTubeHighlightClient.searchHighlights(query)
                    if (videos.isNotEmpty()) {
                        HighlightVideo(eventId = event.id, video = videos.first(), sport = event.sport)
                    } else null
                } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()

        val leagueQueries = buildLeagueQueries(events, date)
        val leagueResults = leagueQueries.map { query ->
            async {
                try {
                    YouTubeHighlightClient.searchHighlights(query).map { video ->
                        val sport = when {
                            query.contains("UFC", ignoreCase = true) -> "Fighting"
                            query.contains("PFL", ignoreCase = true) -> "Fighting"
                            query.contains("NBA", ignoreCase = true) -> "Basketball"
                            query.contains("NFL", ignoreCase = true) -> "Football"
                            query.contains("MLB", ignoreCase = true) -> "Baseball"
                            query.contains("NHL", ignoreCase = true) -> "Hockey"
                            query.contains("Premier", ignoreCase = true) -> "Soccer"
                            query.contains("F1", ignoreCase = true) -> "Racing"
                            else -> ""
                        }
                        HighlightVideo(
                            eventId = "league_${query.hashCode().toLong()}",
                            video = video,
                            sport = sport,
                        )
                    }
                } catch (_: Exception) { emptyList() }
            }
        }.awaitAll().flatten()

        (eventResults + leagueResults).distinctBy { it.video.videoId }
    }

    private fun buildHighlightQuery(event: EspnProcessedEvent): String {
        val base = if (event.title.isNotBlank()) event.title else "${event.homeTeam} ${event.awayTeam}"
        val league = when {
            event.league.contains("Ufc", ignoreCase = true) -> "UFC"
            event.league.contains("Pfl", ignoreCase = true) -> "PFL"
            event.league.contains("Nfl", ignoreCase = true) -> "NFL"
            event.league.contains("Nba", ignoreCase = true) -> "NBA"
            event.league.contains("Mlb", ignoreCase = true) -> "MLB"
            event.league.contains("Nhl", ignoreCase = true) -> "NHL"
            event.league.contains("Premier", ignoreCase = true) -> "Premier League"
            event.league.contains("Mens college", ignoreCase = true) -> "College Basketball"
            event.league.contains("College football", ignoreCase = true) -> "College Football"
            event.league.contains("F1", ignoreCase = true) -> "F1"
            else -> event.sport
        }
        return "$base $league highlights"
    }

    private fun buildLeagueQueries(events: List<EspnProcessedEvent>, date: String): List<String> {
        val today = formatToday()
        val dateLabel = when (date) {
            today -> "today"
            addDays(today, -1) -> "yesterday"
            else -> ""
        }
        val uniqueLeagues = events.mapNotNull { event ->
            when {
                event.league.contains("Ufc", ignoreCase = true) -> "UFC"
                event.league.contains("Pfl", ignoreCase = true) -> "PFL"
                event.league.contains("Nfl", ignoreCase = true) -> "NFL"
                event.league.contains("Nba", ignoreCase = true) -> "NBA"
                event.league.contains("Mlb", ignoreCase = true) -> "MLB"
                event.league.contains("Nhl", ignoreCase = true) -> "NHL"
                event.league.contains("Premier", ignoreCase = true) -> "Premier League"
                event.league.contains("F1", ignoreCase = true) -> "F1"
                else -> null
            }
        }.distinct()
        return uniqueLeagues.map { league ->
            if (dateLabel.isNotBlank()) "$league $dateLabel highlights" else "$league highlights"
        }
    }

    private suspend fun fetchTrendingNewsVideos(): List<YouTubeVideo> {
        val queries = listOf(
            "sports news today",
            "NFL highlights",
            "NBA highlights",
            "MLB highlights",
            "NHL highlights",
            "soccer highlights",
            "UFC news",
        )
        for (query in queries) {
            try {
                val results = YouTubeHighlightClient.searchHighlights(query)
                if (results.isNotEmpty()) return results.take(8)
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    private fun buildStandings(events: List<EspnProcessedEvent>): List<TeamStanding> {
        // Extract from event competitor records (each competitor has a "records" field)
        val seen = mutableSetOf<String>()
        return events.flatMap { event ->
            // We can't access raw competitors here since EspnProcessedEvent is flat
            // Instead, group teams by their win/loss patterns inferred from scores
            if (event.homeScore != null && event.awayScore != null &&
                event.status.contains("FINAL") && event.homeTeam.isNotBlank()
            ) {
                val homeWon = event.homeScore.toIntOrNull() ?: 0 > event.awayScore.toIntOrNull() ?: 0
                val key = "${event.sport}|${event.homeTeam}"
                if (seen.add(key)) {
                    listOf(
                        TeamStanding(
                            teamName = event.homeTeam,
                            logo = event.homeLogo,
                            record = if (homeWon) "W" else "L",
                            league = event.league,
                            sport = event.sport,
                        )
                    )
                } else emptyList()
            } else emptyList()
        }.take(30)
    }

    fun selectSport(sport: String?) {
        _uiState.value = _uiState.value.copy(selectedSport = sport)
    }

    fun selectDate(date: String) {
        val cached = perDateEventCache[date]
        if (cached != null) {
            _uiState.value = _uiState.value.copy(
                selectedDate = date,
                events = cached,
                isLoading = true,
            )
        } else {
            _uiState.value = _uiState.value.copy(selectedDate = date)
        }
        refresh(date)
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
                _uiState.value = _uiState.value.copy(
                    searchResults = videos,
                    searchedEvents = matchedEvents,
                    isSearching = false,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            searchedEvents = emptyList(),
            isSearching = false,
        )
    }

    fun addDays(date: String, days: Int): String {
        var (y, m, d) = parseDate(date)
        var totalDays = daysToEpoch(y, m, d) + days
        if (totalDays < 0) totalDays = 0L
        val (ny, nm, nd) = epochToDate(totalDays)
        return "${ny}-${nm.toString().padStart(2, '0')}-${nd.toString().padStart(2, '0')}"
    }

    fun dateRange(centerDate: String, range: Int): List<String> {
        return (-range..range).map { addDays(centerDate, it) }
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
        val monthDays = if (isLeapYear(y))
            intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        else
            intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var m = 0
        while (m < 12 && remaining >= monthDays[m]) {
            remaining -= monthDays[m]
            m++
        }
        val month = m + 1
        val day = remaining.toInt() + 1
        return "${y}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun isLeapYear(y: Long): Boolean =
        (y % 4 == 0L && y % 100 != 0L) || (y % 400 == 0L)
}

private fun com.nuvio.app.features.iptv.SportEvent.toEspnProcessedEvent(): EspnProcessedEvent {
    val teams = strEvent.split(" vs ", " @ ", " at ")
    val homeName = if (teams.size >= 2) teams.last().trim() else strHomeTeam
    val awayName = if (teams.size >= 2) teams.first().trim() else strAwayTeam
    val isLive = strStatus?.uppercase() == "LIVE"
    val isFinal = strStatus?.uppercase() == "FINAL"
    val status = when {
        isLive -> "STATUS_IN_PROGRESS"
        isFinal -> "STATUS_FINAL"
        else -> "STATUS_SCHEDULED"
    }
    return EspnProcessedEvent(
        id = "sdb_${idEvent}",
        title = strEvent,
        homeTeam = homeName,
        awayTeam = awayName,
        homeScore = intHomeScore,
        awayScore = intAwayScore,
        homeLogo = strThumb,
        awayLogo = null,
        channel = strChannel,
        status = status,
        detail = strTime,
        date = strDate.take(10),
        sport = when (strSport.lowercase()) {
            "american football" -> "Football"
            "baseball" -> "Baseball"
            "basketball" -> "Basketball"
            "ice hockey" -> "Hockey"
            "soccer" -> "Soccer"
            "mma", "ufc", "boxing" -> "Fighting"
            else -> strSport.replaceFirstChar { it.uppercase() }
        },
        league = strLeague,
        isLive = isLive,
        isPpv = strFilename?.uppercase() == "PPV",
    )
}
