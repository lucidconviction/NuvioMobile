package com.nuvio.app.features.sports

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

object Sync2CalRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _eventsByLeague = MutableStateFlow<Map<String, List<Sync2CalEvent>>>(emptyMap())
    val eventsByLeague: StateFlow<Map<String, List<Sync2CalEvent>>> = _eventsByLeague.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tvChannelsByEvent = MutableStateFlow<Map<Long, List<Sync2CalTvChannel>>>(emptyMap())
    val tvChannelsByEvent: StateFlow<Map<Long, List<Sync2CalTvChannel>>> = _tvChannelsByEvent.asStateFlow()

    private val cache = mutableMapOf<String, CacheEntry>()
    private const val CACHE_TTL_MS = 300_000L

    private data class CacheEntry(
        val events: List<Sync2CalEvent>,
        val tvChannels: Map<Long, List<Sync2CalTvChannel>>,
        val timestamp: Long,
    )

    private val TV_LINE_REGEX = Regex("""TV:\s*([^\n]+)""", RegexOption.IGNORE_CASE)
    private val CHANNEL_SEPARATOR = Regex(""",\s*|\s*/\s*|\s+&\s+""")

    fun extractTvChannels(description: String?): List<Sync2CalTvChannel> {
        if (description.isNullOrBlank()) return emptyList()
        val tvLine = TV_LINE_REGEX.find(description)?.groupValues?.getOrNull(1) ?: return emptyList()
        return tvLine.split(CHANNEL_SEPARATOR)
            .map { it.trim().replace("^[^a-zA-Z0-9]+".toRegex(), "").takeIf { it.isNotBlank() } }
            .filterNotNull()
            .map { Sync2CalTvChannel(name = it) }
    }

    fun extractTvChannels(event: Sync2CalEvent): List<Sync2CalTvChannel> {
        return extractTvChannels(event.description)
    }

    fun loadAllEvents() {
        scope.launch {
            _isLoading.value = true
            val allResults = mutableMapOf<String, List<Sync2CalEvent>>()
            val allTvChannels = mutableMapOf<Long, List<Sync2CalTvChannel>>()

            val deferred = Sync2CalMappings.leagueMappings.map { mapping ->
                async {
                    val cacheKey = mapping.leagueId
                    val cached = cache[cacheKey]
                    if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
                        return@async Pair(cacheKey, cached)
                    }
                    try {
                        val category = withTimeout(10_000) {
                            Sync2CalClient.lookupBySlug(mapping.sync2calSlug)
                        }
                        if (category != null) {
                            val events = withTimeout(10_000) {
                                Sync2CalClient.getFilteredEvents(category.uuid)
                            }
                            val enriched = events.map { it.copy(leagueId = mapping.leagueId, leagueName = Sync2CalMappings.leagueNameFromId(mapping.leagueId)) }
                            val tvChannels = enriched.associate { ev -> ev.id to extractTvChannels(ev) }
                            Pair(cacheKey, CacheEntry(enriched, tvChannels, System.currentTimeMillis()))
                        } else {
                            Pair(cacheKey, null)
                        }
                    } catch (_: Exception) { Pair(cacheKey, null) }
                }
            }

            deferred.forEach { def ->
                try {
                    val (leagueId, entry) = def.await()
                    if (entry != null) {
                        cache[leagueId] = entry
                        allResults[leagueId] = entry.events
                        allTvChannels.putAll(entry.tvChannels)
                    }
                } catch (_: Exception) { }
            }

            _eventsByLeague.value = allResults
            _tvChannelsByEvent.value = allTvChannels
            _isLoading.value = false
        }
    }

    fun loadLeagueEvents(leagueId: String) {
        scope.launch {
            val mapping = Sync2CalMappings.leagueMappings.find { it.leagueId == leagueId } ?: return@launch
            val cacheKey = leagueId
            val cached = cache[cacheKey]
            if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
                _eventsByLeague.value = _eventsByLeague.value + (leagueId to cached.events)
                _tvChannelsByEvent.value = _tvChannelsByEvent.value + cached.tvChannels
                return@launch
            }
            try {
                val category = withTimeout(10_000) { Sync2CalClient.lookupBySlug(mapping.sync2calSlug) }
                if (category != null) {
                    val events = withTimeout(10_000) { Sync2CalClient.getFilteredEvents(category.uuid) }
                    val enriched = events.map { it.copy(leagueId = leagueId, leagueName = Sync2CalMappings.leagueNameFromId(leagueId)) }
                    val tvChannels = enriched.associate { ev -> ev.id to extractTvChannels(ev) }
                    cache[cacheKey] = CacheEntry(enriched, tvChannels, System.currentTimeMillis())
                    _eventsByLeague.value = _eventsByLeague.value + (leagueId to enriched)
                    _tvChannelsByEvent.value = _tvChannelsByEvent.value + tvChannels
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun searchEvents(query: String): List<Sync2CalEvent> {
        if (query.length < 2) return emptyList()
        val q = query.lowercase()
        return _eventsByLeague.value.values.flatten().filter { event ->
            event.title.lowercase().contains(q) ||
            event.leagueName.lowercase().contains(q) ||
            event.location?.lowercase()?.contains(q) == true
        }.take(50)
    }

    fun getEventsForLeague(leagueId: String): List<Sync2CalEvent> {
        return _eventsByLeague.value[leagueId] ?: emptyList()
    }

    fun getAllEvents(): List<Sync2CalEvent> {
        return _eventsByLeague.value.values.flatten()
    }

    fun getTvChannelsForEvent(eventId: Long): List<Sync2CalTvChannel> {
        return _tvChannelsByEvent.value[eventId] ?: emptyList()
    }

    fun clearCache() {
        cache.clear()
        _eventsByLeague.value = emptyMap()
        _tvChannelsByEvent.value = emptyMap()
    }
}
