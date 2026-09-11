package com.nuvio.app.features.sports

import com.nuvio.app.features.iptv.EspnClient
import com.nuvio.app.features.iptv.EspnNewsArticle
import com.nuvio.app.features.iptv.EspnNewsClient
import com.nuvio.app.features.iptv.EspnProcessedEvent
import com.nuvio.app.features.iptv.IptvRepository
import com.nuvio.app.features.iptv.RssNewsClient
import com.nuvio.app.features.iptv.SportsClient
import com.nuvio.app.features.player.SportsNowStore
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SportsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var sync2CalCollectorJob: Job? = null

    private data class LeagueCache(
        val events: List<EspnProcessedEvent>,
        val highlights: List<HighlightVideo>,
        val timestamp: Long,
    )
    private const val MAX_LEAGUE_CACHE_ENTRIES = 50
    private val leagueCache = linkedMapOf<String, LeagueCache>()
    private const val CACHE_TTL_MS = 60_000L

    private fun putLeagueCache(key: String, cache: LeagueCache) {
        leagueCache[key] = cache
        while (leagueCache.size > MAX_LEAGUE_CACHE_ENTRIES) {
            val oldest = leagueCache.keys.firstOrNull() ?: break
            leagueCache.remove(oldest)
        }
    }

    // ── Persistent pre-cache (mirrors VidNutzCacheStore pattern) ────────────
    private val cacheJson = Json { ignoreUnknownKeys = true }
    @Volatile private var cacheLoaded = false
    private var cacheAllLiveEvents: List<EspnProcessedEvent> = emptyList()
    private val cacheLeagueByKey = mutableMapOf<String, CachedLeagueEvents>()
    private val cacheMatchedByEvent = mutableMapOf<String, List<MatchedChannel>>()
    private val cacheTimestamps = mutableMapOf<String, Long>()
    private var cacheSaveJob: Job? = null

    private fun loadCache() {
        if (cacheLoaded) return
        cacheLoaded = true
        SportsCacheStore.load()?.let { cachedJson ->
            try {
                val cache = cacheJson.decodeFromString<SportsCache>(cachedJson)
                cacheAllLiveEvents = cache.allLiveEvents
                cacheLeagueByKey.putAll(cache.leagueEvents)
                cacheMatchedByEvent.putAll(cache.matchedChannels)
                cacheTimestamps.putAll(cache.timestamps)
            } catch (_: Exception) {}
        }
    }

    private fun saveCache() {
        cacheSaveJob?.cancel()
        cacheSaveJob = scope.launch {
            try {
                val cache = SportsCache(
                    allLiveEvents = cacheAllLiveEvents,
                    leagueEvents = cacheLeagueByKey.toMap(),
                    matchedChannels = cacheMatchedByEvent.toMap(),
                    timestamps = cacheTimestamps.toMap(),
                )
                SportsCacheStore.save(cacheJson.encodeToString(cache))
            } catch (_: Exception) {}
        }
    }

    fun getCachedAllLiveEvents(): List<EspnProcessedEvent> {
        loadCache()
        return cacheAllLiveEvents
    }

    fun getCachedLeagueEvents(cacheKey: String): CachedLeagueEvents? {
        loadCache()
        return cacheLeagueByKey[cacheKey]
    }

    fun getCachedMatchedChannels(eventId: String): List<MatchedChannel>? {
        loadCache()
        return cacheMatchedByEvent[eventId]?.takeIf { it.isNotEmpty() }
    }

    /**
     * Load the persisted sports pre-cache into memory AND seed the UI state so the
     * screen renders content immediately (no blank), then kick off a background
     * network refresh for fresh data.
     */
    fun warmupSports() {
        loadCache()
        _uiState.value = _uiState.value.copy(
            allLiveEvents = cacheAllLiveEvents,
            refreshing = _uiState.value.events.isEmpty() && cacheAllLiveEvents.isEmpty(),
        )
        // Background refresh without blanking (all existing cache remains visible).
        refresh()
        loadAllLiveEvents()
    }

    private fun seedMatchedChannelsFromCache(event: EspnProcessedEvent) {
        loadCache()
        val cached = cacheMatchedByEvent[event.id]
        if (cached != null && cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(matchedChannels = cached, channelsLoading = false)
        }
    }


    val leagues: List<SportLeague> = listOf(
        SportLeague("nfl", "NFL Football", "NFL", "football/nfl", "https://a.espncdn.com/i/teamlogos/leagues/500/nfl.png"),
        SportLeague("nba", "NBA Basketball", "NBA", "basketball/nba", "https://a.espncdn.com/i/teamlogos/leagues/500/nba.png"),
        SportLeague("mlb", "MLB Baseball", "MLB", "baseball/mlb", "https://a.espncdn.com/i/teamlogos/leagues/500/mlb.png"),
        SportLeague("nhl", "NHL Hockey", "NHL", "hockey/nhl", "https://a.espncdn.com/i/teamlogos/leagues/500/nhl.png"),
        SportLeague("ufc", "UFC MMA", "UFC", "mma/ufc", "https://a.espncdn.com/i/teamlogos/leagues/500/ufc.png"),
        SportLeague("boxing", "Boxing", "BOX", "boxing/boxing", "https://a.espncdn.com/i/teamlogos/leagues/500/boxing.png"),
        SportLeague("pfl", "PFL MMA", "PFL", "mma/pfl", "https://a.espncdn.com/i/teamlogos/leagues/500/pfl.png"),
        SportLeague("bkfc", "BKFC", "BKFC", "mma/bkfc", "https://a.espncdn.com/i/teamlogos/leagues/500/bkfc.png"),
        SportLeague("powerslap", "PowerSlap", "SLAP", "mma/powerslap", "https://a.espncdn.com/i/teamlogos/leagues/500/powerslap.png"),
        SportLeague("mls", "MLS Soccer", "MLS", "soccer/usa.1", "https://a.espncdn.com/i/teamlogos/leagues/500/mls.png"),
        SportLeague("f1", "Formula 1", "F1", "racing/f1", "https://a.espncdn.com/i/teamlogos/leagues/500/f1.png"),
        SportLeague("tennis", "Tennis", "TEN", "tennis/atp", "https://a.espncdn.com/i/teamlogos/leagues/500/tennis.png"),
        SportLeague("golf", "Golf", "GOLF", "golf/pga", "https://a.espncdn.com/i/teamlogos/leagues/500/golf.png"),
        SportLeague("cfb", "College Football", "CFB", "football/college-football", "https://a.espncdn.com/i/teamlogos/leagues/500/cfb.png"),
        SportLeague("cbb", "College Basketball", "CBB", "basketball/mens-college-basketball", "https://a.espncdn.com/i/teamlogos/leagues/500/cbb.png"),
        SportLeague("wnba", "WNBA", "WNBA", "basketball/wnba", "https://a.espncdn.com/i/teamlogos/leagues/500/wnba.png"),
    )

    fun refresh() {
        loadCache()
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val hasContent = _uiState.value.events.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasContent,
                refreshing = hasContent,
                error = null,
            )
            if (_uiState.value.trendingNewsVideos.isEmpty()) {
                val trendingVideos = fetchTrendingNewsVideos()
                _uiState.value = _uiState.value.copy(
                    trendingNewsVideos = trendingVideos,
                    isLoading = _uiState.value.events.isEmpty(),
                    refreshing = _uiState.value.events.isNotEmpty(),
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = _uiState.value.events.isEmpty(),
                    refreshing = _uiState.value.events.isNotEmpty(),
                )
            }
        }
        loadSync2CalEvents()
        loadBkfcEvents()
        loadBoxingEvents()
        loadPflEvents()
        loadPowerSlapEvents()
    }

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1_000,
        block: suspend () -> T,
    ): T {
        var lastEx: Exception? = null
        var delayMs = initialDelayMs
        for (attempt in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastEx = e
                if (attempt < maxRetries) {
                    delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        throw lastEx ?: Exception("Retry failed")
    }

    fun loadLeagueEvents(league: SportLeague, date: String? = null, forceRefresh: Boolean = false) {
        refreshJob?.cancel()
        val parts = league.slug.split("/")
        if (parts.size < 2) return
        val sport = parts[0]
        val leagueName = parts[1]
        val queryDate = date ?: _uiState.value.selectedDate.ifBlank { null }
        val cacheKey = "${league.id}_${queryDate ?: "today"}"

        if (!forceRefresh) {
            val cached = leagueCache[cacheKey]
            if (cached != null && TraktPlatformClock.nowEpochMs() - cached.timestamp < CACHE_TTL_MS) {
                _uiState.value = _uiState.value.copy(
                    events = cached.events, highlightVideos = cached.highlights,
                    refreshing = false, isLoading = false, error = null,
                )
                return
            }
        }

        // Cold start / TTL expired: fall back to the persistent pre-cache so the
        // screen renders content immediately instead of blanking.
        loadCache()
        val persisted = cacheLeagueByKey[cacheKey]
        if (persisted != null && persisted.events.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                events = persisted.events,
                refreshing = true,
                isLoading = false,
                error = null,
            )
        }

        scope.launch {
            val hasContent = _uiState.value.events.isNotEmpty() || _uiState.value.allLiveEvents.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasContent,
                refreshing = hasContent,
                error = null,
            )
            try {
                val events = if (league.id == "bkfc" || league.id == "pfl" || league.id == "powerslap") {
                    val rawEvents = when (league.id) {
                        "bkfc" -> withTimeout(15_000) { BkfcClient.fetchUpcomingEvents() }.map { ev ->
                            val f1 = ev.fighter1.ifBlank { ev.mainEvent.substringBefore(" vs ") }
                            val f2 = ev.fighter2.ifBlank { ev.mainEvent.substringAfter(" vs ") }
                            val shortTitle = if (f1.isNotBlank() && f2.isNotBlank()) "$f1 vs $f2" else ev.title
                            EspnProcessedEvent(id = "bkfc_${ev.slug}", title = shortTitle, homeTeam = f2, awayTeam = f1,
                                homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
                                channel = "", status = "Scheduled", detail = ev.location, date = ev.date, rawDate = ev.date,
                                timeStr = ev.time.ifBlank { null }, sport = "Fighting", league = "BKFC", isLive = false, isPpv = false)
                        }
                        "pfl" -> withTimeout(15_000) { PflClient.fetchUpcomingEvents() }.map { ev ->
                            val shortTitle = if (ev.fighter1.isNotBlank()) "${ev.fighter1} vs ${ev.fighter2}" else ev.title
                            EspnProcessedEvent(id = "pfl_${ev.title.hashCode()}", title = shortTitle, homeTeam = ev.fighter2, awayTeam = ev.fighter1,
                                homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
                                channel = "", status = "Scheduled", detail = ev.location, date = ev.date, rawDate = ev.date,
                                timeStr = ev.time.ifBlank { null }, sport = "Fighting", league = "PFL", isLive = false, isPpv = false)
                        }
                        else -> withTimeout(15_000) { PowerSlapClient.fetchUpcomingEvents() }.map { ev ->
                            val shortTitle = if (ev.fighter1.isNotBlank()) "${ev.fighter1} vs ${ev.fighter2}" else ev.title
                            EspnProcessedEvent(id = "slap_${ev.title.hashCode()}", title = shortTitle, homeTeam = ev.fighter2, awayTeam = ev.fighter1,
                                homeScore = null, awayScore = null, homeLogo = null, awayLogo = null,
                                channel = "", status = "Scheduled", detail = ev.location, date = ev.date, rawDate = ev.date,
                                sport = "Fighting", league = "SLAP", isLive = false, isPpv = false)
                        }
                    }
                    rawEvents
                } else {
                    withTimeout(15_000) { retryWithBackoff { EspnClient.fetchLeague(sport, leagueName, queryDate) } }
                }
                val standingsDeferred = async { retryWithBackoff(maxRetries = 2) { EspnClient.fetchStandings(sport, leagueName, null) } }
                val standings = try { withTimeout(10_000) { standingsDeferred.await() } } catch (_: Exception) { emptyList() }
                val highlightQueries = if (league.id == "boxing") {
                    listOf("boxing highlights", "boxing knockouts", "boxing best fights", "boxing news")
                } else {
                    listOf(
                        "${league.name} highlights", "${league.abbreviation} highlights",
                        "${league.name} top plays", "${league.name} best moments",
                    )
                }
                val highlightVideos = highlightQueries.mapNotNull { query ->
                    try { withTimeout(8_000) { YouTubeHighlightClient.searchHighlights(query).take(3) } }
                    catch (_: Exception) { emptyList() }
                }.flatten().distinctBy { it.videoId }.take(12).mapIndexed { idx, video ->
                    HighlightVideo(eventId = "league_${league.id}_$idx", video = video, sport = sport)
                }

                putLeagueCache(cacheKey, LeagueCache(events, highlightVideos, TraktPlatformClock.nowEpochMs()))
                cacheLeagueByKey[cacheKey] = CachedLeagueEvents(events, TraktPlatformClock.nowEpochMs())
                cacheTimestamps[cacheKey] = TraktPlatformClock.nowEpochMs()
                saveCache()
                _uiState.value = _uiState.value.copy(
                    events = events,
                    highlightVideos = highlightVideos,
                    standings = standings,
                    refreshing = false,
                    isLoading = false,
                    error = null,
                )
                precacheMatchedChannels(events)
            } catch (e: Exception) {
                val cached = leagueCache[cacheKey] ?: cacheLeagueByKey[cacheKey]?.let {
                    LeagueCache(it.events, emptyList(), it.timestamp)
                }
                if (cached != null && cached.events.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        events = cached.events, highlightVideos = cached.highlights,
                        refreshing = false, isLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, refreshing = false, error = e.message)
                }
            }
        }
    }

    /** Smart heartbeat: uses per-data-type refresh intervals from LiveRefreshPolicy. */
    private var liveHeartbeatJob: Job? = null

    fun startLiveHeartbeat() {
        liveHeartbeatJob?.cancel()
        liveHeartbeatJob = scope.launch {
            while (true) {
                val state = _uiState.value
                if (state.selectedLeague == null) {
                    // Live mode: refresh live scores at LIVE_SCORES interval
                    if (LiveRefreshPolicy.shouldRefresh(RefreshInterval.LIVE_SCORES)) {
                        loadAllLiveEvents()
                        loadDaddyLiveEvents()
                        LiveRefreshPolicy.markFetched(RefreshInterval.LIVE_SCORES)
                    }
                }
                delay(5_000) // Check every 5s, actual refresh gated by policy
            }
        }
    }

    fun stopLiveHeartbeat() {
        liveHeartbeatJob?.cancel()
        liveHeartbeatJob = null
    }

    fun loadAllLiveEvents() {
        loadCache()
        // Cold start: seed from pre-cache so the Sports Now/Later view renders
        // immediately instead of blanking behind the spinner.
        if (_uiState.value.allLiveEvents.isEmpty() && cacheAllLiveEvents.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                allLiveEvents = cacheAllLiveEvents,
                allLiveLoading = false,
            )
        }
        scope.launch {
            // No-blank: keep any existing allLiveEvents visible while refreshing.
            // allLiveLoading stays true during the fetch so the UI can show the thin
            // strip; the screen renders cached/stale content whenever it is non-empty.
            _uiState.value = _uiState.value.copy(
                allLiveLoading = true,
                refreshing = !_uiState.value.events.isEmpty(),
            )
            try {
                val today = formatToday()
                val twoWeeks = addDays(today, 14)
                val dateRange = "${today.replace("-", "")}${twoWeeks.replace("-", "")}"
                var allEvents = withTimeout(40_000) { retryWithBackoff(maxRetries = 3, initialDelayMs = 2_000) { EspnClient.fetchAll(dateRange) } }
                if (allEvents.isEmpty()) {
                    // Fallback: per-league scoreboards -> live + next-24h games so the
                    // Sports Now/Later view is always populated.
                    val nowMs = TraktPlatformClock.nowEpochMs()
                    val day24Ms = nowMs + 86_400_000L
                    allEvents = leagues.flatMap { league ->
                        runCatching {
                            val parts = league.slug.split("/")
                            if (parts.size < 2) emptyList() else EspnClient.fetchLeague(parts[0], parts[1], today)
                        }.getOrDefault(emptyList())
                    }.filter { ev ->
                        val startMs = ev.rawDate?.let { runCatching { java.time.Instant.parse(it.trim()).toEpochMilli() }.getOrNull() }
                        ev.isLive || (startMs != null && startMs in (nowMs - 3_600_000L)..day24Ms)
                    }
                }
                cacheAllLiveEvents = allEvents
                cacheTimestamps["all_live"] = TraktPlatformClock.nowEpochMs()
                saveCache()
                SportsNowStore.liveEvents = allEvents
                _uiState.value = _uiState.value.copy(
                    allLiveEvents = allEvents,
                    allLiveLoading = false,
                    refreshing = _uiState.value.events.isNotEmpty(),
                )
                precacheMatchedChannels(allEvents)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    allLiveLoading = false,
                    refreshing = _uiState.value.events.isNotEmpty(),
                )
            }
        }
    }

    fun loadSync2CalEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(sync2CalLoading = true)
            Sync2CalRepository.loadAllEvents()
            sync2CalCollectorJob?.cancel()
            sync2CalCollectorJob = scope.launch {
                Sync2CalRepository.eventsByLeague.collect { events ->
                    Sync2CalRepository.tvChannelsByEvent.collect { channels ->
                        _uiState.value = _uiState.value.copy(
                            sync2CalEventsByLeague = events,
                            sync2CalTvChannels = channels,
                            sync2CalLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun selectLeague(league: SportLeague?) {
        _uiState.value = _uiState.value.copy(selectedLeague = league, selectedSport = league?.id)
        if (league != null) {
            loadLeagueEvents(league)
            loadLeagueNews(league)
        } else {
            loadAllLiveEvents()
        }
    }

    /** Load league-specific sports news headlines. */
    fun loadLeagueNews(league: SportLeague) {
        scope.launch {
            val leagueLower = league.id.lowercase()
            val news = when {
                leagueLower in setOf("ufc", "mma", "pfl", "bkfc", "powerslap", "slap", "bellator") ->
                    fetchRssNews(mmaNewsFeeds)
                leagueLower == "boxing" ->
                    fetchRssNews(mmaNewsFeeds + boxingNewsFeeds)
                else -> {
                    val parts = league.slug.split("/")
                    if (parts.size < 2) emptyList()
                    else runCatching {
                        withTimeout(10_000) { EspnNewsClient.fetchNewsForLeague(parts[0], parts[1]) }
                    }.getOrDefault(emptyList())
                }
            }
            _uiState.value = _uiState.value.copy(leagueNews = news)
        }
    }

    fun selectEvent(event: EspnProcessedEvent?) {
        _uiState.value = _uiState.value.copy(selectedEvent = event)
        if (event != null) {
            loadMatchedChannels(event)
        }
    }

    private fun loadMatchedChannels(event: EspnProcessedEvent) {
        loadCache()
        // No-blank: seed from pre-cache so the channel list renders immediately,
        // only showing a spinner when there is truly no channel data yet. If there
        // is no cache for THIS event, clear any stale channels from a previous
        // event so we never show the wrong game's channels.
        val cached = cacheMatchedByEvent[event.id]
        if (cached != null && cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                matchedChannels = cached,
                channelsLoading = false,
            )
        } else {
            _uiState.value = _uiState.value.copy(
                matchedChannels = emptyList(),
                channelsLoading = true,
            )
        }
        scope.launch {
            val hasChannels = _uiState.value.matchedChannels.isNotEmpty()
            _uiState.value = _uiState.value.copy(channelsLoading = !hasChannels)
            val allChannels = IptvRepository.getAllChannels()
            val epgLookup = IptvRepository.buildCurrentEpgTitleLookup()
            val region = _uiState.value.userRegion

            // Use region-aware matching pipeline
            val matches = AdvancedChannelMatcher.matchChannels(
                event = event,
                channels = allChannels,
                userRegion = region,
                currentEpgTitleFor = epgLookup,
            )

            persistMatchedChannels(event.id, matches)

            // Show every candidate immediately; then validate streams and drop the confirmed-dead
            _uiState.value = _uiState.value.copy(matchedChannels = matches, channelsLoading = false)
            val candidates = matches.map { it.channel }
            com.nuvio.app.features.iptv.StreamValidationController.resolveChannelsStatuses(candidates) { statuses ->
                _uiState.value = _uiState.value.copy(
                    aliveByUrl = statuses,
                    matchedChannels = matches.filter { statuses[it.channel.url] != false },
                )
            }
        }
    }

    private fun persistMatchedChannels(eventId: String, matches: List<MatchedChannel>) {
        if (matches.isNotEmpty()) {
            cacheMatchedByEvent[eventId] = matches
            cacheTimestamps["channels_$eventId"] = TraktPlatformClock.nowEpochMs()
            saveCache()
        }
    }

    /**
     * Background precompute of matched channels for a set of events (e.g. freshly
     * fetched live/league events). Reuses the same region-aware matcher as
     * [loadMatchedChannels], capped for cheapness, and persists the results so the
     * event detail page loads instantly from cache.
     */
    private fun precacheMatchedChannels(events: List<EspnProcessedEvent>) {
        if (events.isEmpty()) return
        val targets = events.take(6)  // keep it cheap
        loadCache()
        scope.launch(Dispatchers.Default) {
            try {
                val allChannels = IptvRepository.getAllChannels()
                if (allChannels.isEmpty()) return@launch
                val region = _uiState.value.userRegion
                val epgLookup = IptvRepository.buildCurrentEpgTitleLookup()
                var changed = false
                for (event in targets) {
                    // Skip events we've already pre-cached recently.
                    val lastTs = cacheTimestamps["channels_${event.id}"] ?: 0
                    if (lastTs > 0 && TraktPlatformClock.nowEpochMs() - lastTs < 5 * 60 * 1000) continue
                    runCatching {
                        val matches = AdvancedChannelMatcher.matchChannels(
                            event = event,
                            channels = allChannels,
                            userRegion = region,
                            currentEpgTitleFor = epgLookup,
                            cap = 8,
                        )
                        if (matches.isNotEmpty()) {
                            cacheMatchedByEvent[event.id] = matches
                            cacheTimestamps["channels_${event.id}"] = TraktPlatformClock.nowEpochMs()
                            changed = true
                        }
                    }
                }
                if (changed) saveCache()
            } catch (_: Exception) {}
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

    fun setUserRegion(region: BroadcastRegion) {
        _uiState.value = _uiState.value.copy(userRegion = region)
        // Re-match current event with new region if one is selected
        val event = _uiState.value.selectedEvent
        if (event != null) {
            loadMatchedChannels(event)
        }
    }

    /**
     * Auto-detect user's broadcast region from available IPTV channels.
     * Called once on first load, or manually by the user.
     */
    fun detectAndSetRegion() {
        scope.launch {
            val allChannels = IptvRepository.getAllChannels()
            val detected = AdvancedChannelMatcher.detectUserRegion(allChannels)
            _uiState.value = _uiState.value.copy(
                userRegion = detected,
                regionFilter = "ALL",
            )
        }
    }

    fun setActiveEventTab(tab: EventTab) {
        _uiState.value = _uiState.value.copy(activeEventTab = tab)
    }

    fun loadDaddyLiveEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(daddyLiveLoading = true)
            try {
                val events = withTimeout(15_000) { DaddyLiveClient.fetchEvents() }
                _uiState.value = _uiState.value.copy(daddyLiveEvents = events, daddyLiveLoading = false)
                SportsNowStore.daddyLiveEvents = events
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(daddyLiveLoading = false)
            }
        }
    }

    fun loadBkfcEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(bkfcLoading = true)
            try {
                val events = withTimeout(15_000) { BkfcClient.fetchUpcomingEvents() }
                _uiState.value = _uiState.value.copy(bkfcEvents = events, bkfcLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(bkfcLoading = false)
            }
        }
    }

    fun loadBoxingEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(boxingLoading = true)
            try {
                val events = withTimeout(15_000) { BoxingSceneClient.fetchUpcomingEvents() }
                _uiState.value = _uiState.value.copy(boxingEvents = events, boxingLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(boxingLoading = false)
            }
        }
    }

    fun loadPflEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(pflLoading = true)
            try {
                val events = withTimeout(15_000) { PflClient.fetchUpcomingEvents() }
                _uiState.value = _uiState.value.copy(pflEvents = events, pflLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(pflLoading = false)
            }
        }
    }

    fun loadPowerSlapEvents() {
        scope.launch {
            _uiState.value = _uiState.value.copy(powerSlapLoading = true)
            try {
                val events = withTimeout(15_000) { PowerSlapClient.fetchUpcomingEvents() }
                _uiState.value = _uiState.value.copy(powerSlapEvents = events, powerSlapLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(powerSlapLoading = false)
            }
        }
    }

    private val mmaNewsFeeds = listOf(
        "https://www.sherdog.com/rss/news.xml",
        "https://www.mmafighting.com/rss/index.xml",
        "https://www.mmamania.com/rss/index.xml",
    )

    private val boxingNewsFeeds = listOf(
        "https://www.boxingnews24.com/feed/",
        "https://boxinginsider.com/feed/",
    )

    fun loadEventNews(event: EspnProcessedEvent) {
        scope.launch {
            _uiState.value = _uiState.value.copy(eventNewsLoading = true)
            val leagueLower = event.league.lowercase()
            val news = when {
                leagueLower in setOf("ufc", "mma", "pfl", "bkfc", "powerslap", "slap", "bellator") ->
                    fetchRssNews(mmaNewsFeeds)
                leagueLower == "boxing" ->
                    fetchRssNews(mmaNewsFeeds + boxingNewsFeeds)
                else -> {
                    val path = "${event.sport.lowercase()}/${event.league.lowercase().replace(" ", "-")}"
                    val parts = path.split("/")
                    if (parts.size == 2) {
                        runCatching { withTimeout(10_000) { EspnNewsClient.fetchNewsForLeague(parts[0], parts[1]) } }.getOrDefault(emptyList())
                    } else emptyList()
                }
            }
            val homeKey = event.homeTeam.lowercase()
            val awayKey = event.awayTeam.lowercase()
            val relevant = if (homeKey.isNotBlank() || awayKey.isNotBlank()) {
                val filtered = news.filter { art ->
                    val hay = (art.headline + " " + art.description).lowercase()
                    (homeKey.isNotBlank() && hay.contains(homeKey)) || (awayKey.isNotBlank() && hay.contains(awayKey))
                }
                if (filtered.isNotEmpty()) filtered else news
            } else news
            _uiState.value = _uiState.value.copy(eventNews = relevant, eventNewsLoading = false)
        }
    }

    private suspend fun fetchRssNews(feeds: List<String>, limit: Int = 15): List<EspnNewsArticle> {
        for (feed in feeds) {
            val articles = runCatching { withTimeout(10_000) { RssNewsClient.fetchRss(feed) } }.getOrDefault(emptyList())
            if (articles.isNotEmpty()) return articles.take(limit)
        }
        return emptyList()
    }

    fun clearLeagueCache() { leagueCache.clear() }

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
                val hasLiveEvents = _uiState.value.events.any { it.isLive }
                val interval = if (hasLiveEvents) RefreshInterval.LIVE_SCORES else RefreshInterval.UPCOMING_GAMES
                val delayMs = if (LiveRefreshPolicy.shouldRefresh(interval)) {
                    LiveRefreshPolicy.markFetched(interval)
                    loadLeagueEvents(league, forceRefresh = true)
                    // After refresh, wait the full interval before next check
                    interval.ms
                } else {
                    // Not stale yet, wait until it is
                    LiveRefreshPolicy.msUntilNextRefresh(interval).coerceAtLeast(5_000)
                }
                delay(delayMs)
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
            if (parts.size < 2) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid league slug: ${league.slug}")
                return@launch
            }
            val sport = parts[0]
            val leagueName = parts[1]
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val standings = EspnClient.fetchStandings(sport, leagueName, season)
                if (standings.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        standings = emptyList(), isLoading = false,
                        error = "No standings available for ${league.name}",
                    )
                } else {
                    _uiState.value = _uiState.value.copy(standings = standings, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = "Failed to load standings: ${e.message?.take(60) ?: "unknown error"}",
                )
            }
        }
    }

    /** Load grouped (conference/division/table) standings with full stats for the adaptive screen. */
    fun loadStandingsGroups(league: SportLeague, season: Int? = null) {
        scope.launch {
            val parts = league.slug.split("/")
            if (parts.size < 2) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid league slug: ${league.slug}")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, standingsGroups = emptyList())
            try {
                var groups = EspnClient.fetchStandingsGroups(parts[0], parts[1], season)
                if (groups.isEmpty()) {
                    // Fallback: TheSportsDB standings (free tier, mainly soccer/football).
                    groups = SportsClient.fetchStandings(league.slug)
                }
                val flat = groups.flatMap { g -> g.rows }.mapIndexed { i, r ->
                    TeamStanding(
                        teamName = r.teamName, logo = r.logo,
                        record = listOf("wins", "losses", "ties").mapNotNull { r.stats[it] }.take(2).joinToString("-"),
                        league = league.id, sport = parts[0], rank = i + 1,
                        netRating = r.stats["netRating"] ?: r.stats["pointDifferential"] ?: "",
                    )
                }
                _uiState.value = _uiState.value.copy(
                    standingsGroups = groups, standings = flat, isLoading = false,
                    error = if (groups.isEmpty()) "No standings available for ${league.name}" else null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, error = "Failed to load standings: ${e.message?.take(60) ?: "unknown error"}",
                )
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
        if (league != null) {
            loadLeagueEvents(league, date)
        }
    }

    fun loadUpcomingFixtures(league: SportLeague, days: Int = 7) {
        scope.launch {
            _uiState.value = _uiState.value.copy(fixturesLoading = true)
            try {
                val parts = league.slug.split("/")
                if (parts.size >= 2) {
                    val fixtures = FixturesClient.getUpcomingFixtures(parts[0], parts[1], days)
                    _uiState.value = _uiState.value.copy(upcomingFixtures = fixtures, fixturesLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(fixturesLoading = false)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(fixturesLoading = false)
            }
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

    private suspend fun fetchTrendingNewsVideos(): List<YouTubeVideo> = coroutineScope {
        val queries = mutableListOf<String>()
        try {
            val today = formatToday()
            val oneWeek = addDays(today, 7)
            val dateRange = "${today.replace("-", "")}${oneWeek.replace("-", "")}"
            val allEvents = withTimeout(40_000) {
                retryWithBackoff(maxRetries = 3, initialDelayMs = 2_000) { EspnClient.fetchAll(dateRange) }
            }
            val nowMs = TraktPlatformClock.nowEpochMs()
            val weekEndMs = nowMs + 7L * 86_400_000L
            val relevant = allEvents.filter { ev ->
                val startMs = ev.rawDate?.let { runCatching { java.time.Instant.parse(it.trim()).toEpochMilli() }.getOrNull() }
                ev.isLive || (startMs != null && startMs in nowMs..weekEndMs)
            }.sortedWith(compareBy({ !it.isLive }, { it.rawDate ?: "" }))
            relevant.take(10).forEach { ev ->
                val name = ev.title.ifBlank { "${ev.awayTeam} vs ${ev.homeTeam}" }
                    .replace(" @ ", " vs ").replace(" at ", " vs ")
                queries.add(if (ev.isLive) "$name highlights" else "$name preview")
            }
        } catch (_: Exception) {}
        if (queries.isEmpty()) queries.add("sports news today")
        val results = mutableSetOf<YouTubeVideo>()
        queries.take(10).map { query ->
            async {
                try { withTimeout(12_000) { YouTubeHighlightClient.searchHighlights(query) } }
                catch (_: Exception) { emptyList() }
            }
        }.also { deferreds ->
            deferreds.forEach { def ->
                try { results.addAll(def.await().filter { it.videoId.isNotBlank() }) } catch (_: Exception) {}
            }
        }
        if (results.size < 12) {
            val fallback = try { YouTubeHighlightClient.searchHighlights("sports") } catch (_: Exception) { emptyList() }
            results.addAll(fallback.filter { it.videoId.isNotBlank() })
        }
        results.take(12)
    }
}
