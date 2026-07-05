package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object IptvRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var idCounter = 0L

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var settings = IptvPlaylistSettings()

    private val IPTV_ORG_URL = "https://iptv-org.github.io/iptv/index.m3u"
    private val IPTV_ORG_NAME = "iptv-org"

    private fun nextId(prefix: String): String {
        return "${prefix}_${++idCounter}_${TraktPlatformClock.nowEpochMs()}"
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val payload = IptvStorage.loadSettings()
        if (payload != null) {
            try {
                settings = json.decodeFromString<StoredIptvSettings>(payload).toSettings()
            } catch (_: Exception) {
                settings = IptvPlaylistSettings()
            }
        }
        refreshUi()
        refreshSports()
    }

    private fun saveToStorage() {
        IptvStorage.saveSettings(json.encodeToString(StoredIptvSettings.fromSettings(settings)))
    }

    fun addM3uPlaylist(name: String, url: String) {
        scope.launch {
            val id = nextId("m3u")
            val playlist = M3uPlaylist(id = id, name = name, url = url)
            settings = settings.copy(m3uPlaylists = settings.m3uPlaylists + playlist)
            saveToStorage()
            refreshUi()
            refreshM3uChannels(id)
            refreshSports()
        }
    }

    fun removeM3uPlaylist(id: String) {
        settings = settings.copy(m3uPlaylists = settings.m3uPlaylists.filter { it.id != id })
        saveToStorage()
        refreshUi()
    }

    fun refreshM3uChannels(id: String) {
        scope.launch {
            val playlist = settings.m3uPlaylists.find { it.id == id } ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, refreshingSourceIds = _uiState.value.refreshingSourceIds + id)
            try {
                val m3uContent = httpGetText(playlist.url)
                val channels = M3uParser.parse(m3uContent, id)
                val updated = playlist.copy(channels = channels)
                settings = settings.copy(
                    m3uPlaylists = settings.m3uPlaylists.map { if (it.id == id) updated else it }
                )
                saveToStorage()
                _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
                refreshUi()
                if (settings.epgSources.isNotEmpty()) {
                    refreshEpg()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load M3U: ${e.message}", refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
            }
        }
    }

    fun parseM3uContent(content: String, name: String) {
        scope.launch {
            val id = nextId("m3u_up")
            val channels = M3uParser.parse(content, id)
            val playlist = M3uPlaylist(id = id, name = name, url = "", channels = channels)
            settings = settings.copy(m3uPlaylists = settings.m3uPlaylists + playlist)
            saveToStorage()
            refreshUi()
        }
    }

    fun addXtreamAccount(name: String, server: String, username: String, password: String) {
        scope.launch {
            val id = nextId("xtream")
            val account = XtreamAccount(id = id, name = name, server = server, username = username, password = password)
            settings = settings.copy(xtreamAccounts = settings.xtreamAccounts + account)
            saveToStorage()
            refreshUi()
            refreshXtreamChannels(id)
            refreshSports()
        }
    }

    fun removeXtreamAccount(id: String) {
        settings = settings.copy(xtreamAccounts = settings.xtreamAccounts.filter { it.id != id })
        saveToStorage()
        refreshUi()
    }

    fun refreshXtreamChannels(id: String) {
        scope.launch {
            val account = settings.xtreamAccounts.find { it.id == id } ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, refreshingSourceIds = _uiState.value.refreshingSourceIds + id)
            try {
                val baseUrl = account.server.trimEnd('/')
                val creds = "username=${account.username}&password=${account.password}"

                val categoriesJson = httpGetText("$baseUrl/player_api.php?$creds&action=live_categories")
                val categories = XtreamClient.parseCategories(categoriesJson)

                val streamsJson = httpGetText("$baseUrl/player_api.php?$creds&action=live_streams")
                val channels = XtreamClient.parseChannels(streamsJson, id, account.server, account.username, account.password)

                val updated = account.copy(categories = categories, channels = channels)
                settings = settings.copy(
                    xtreamAccounts = settings.xtreamAccounts.map { if (it.id == id) updated else it }
                )
                saveToStorage()
                _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
                refreshUi()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load Xtream: ${e.message}", refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
            }
        }
    }

    fun addStalkerAccount(name: String, server: String, macAddress: String) {
        scope.launch {
            val id = nextId("stalker")
            val account = StalkerAccount(id = id, name = name, server = server, macAddress = macAddress)
            settings = settings.copy(stalkerAccounts = settings.stalkerAccounts + account)
            saveToStorage()
            refreshUi()
            refreshStalkerChannels(id)
        }
    }

    fun removeStalkerAccount(id: String) {
        settings = settings.copy(stalkerAccounts = settings.stalkerAccounts.filter { it.id != id })
        saveToStorage()
        refreshUi()
    }

    fun refreshStalkerChannels(id: String) {
        scope.launch {
            val account = settings.stalkerAccounts.find { it.id == id } ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, refreshingSourceIds = _uiState.value.refreshingSourceIds + id)
            try {
                val channels = StalkerClient.fetchChannels(account.server, account.macAddress, id)
                val updated = account.copy(channels = channels)
                settings = settings.copy(
                    stalkerAccounts = settings.stalkerAccounts.map { if (it.id == id) updated else it }
                )
                saveToStorage()
                _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
                refreshUi()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load Stalker: ${e.message}", refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
            }
        }
    }

    fun addEpgSource(name: String, url: String) {
        val id = nextId("epg")
        settings = settings.copy(epgSources = settings.epgSources + EpgSource(id = id, name = name, url = url))
        saveToStorage()
        refreshUi()
    }

    fun removeEpgSource(id: String) {
        settings = settings.copy(epgSources = settings.epgSources.filter { it.id != id })
        saveToStorage()
        refreshUi()
    }

    fun refreshEpg() {
        scope.launch {
            _uiState.value = _uiState.value.copy(epgLoading = true)
            val allPrograms = mutableMapOf<String, List<EpgProgram>>()
            val programsByName = mutableMapOf<String, List<EpgProgram>>()
            for (source in settings.epgSources) {
                try {
                    val xmlContent = httpGetText(source.url)
                    val result = EpgParser.parseXmltv(xmlContent)
                    allPrograms.putAll(result.programsByChannelId)
                    for ((chId, progs) in result.programsByChannelId) {
                        val displayName = result.channelDisplayNames[chId]
                        if (displayName != null) {
                            val key = displayName.lowercase().trim()
                            programsByName.merge(key, progs) { old, new -> (old + new).sortedBy { it.startTime } }
                        }
                    }
                } catch (e: Exception) {
                    println("EPG fetch error: ${e.message}")
                }
            }
            val allCh = getAllChannels()
            val matchedIds = allCh.count { ch -> ch.epgChannelId != null && allPrograms.containsKey(ch.epgChannelId) }
            val matchedNames = allCh.count { ch ->
                ch.epgChannelId == null || !allPrograms.containsKey(ch.epgChannelId)
            }.let { total ->
                allCh.count { ch ->
                    val byId = ch.epgChannelId != null && allPrograms.containsKey(ch.epgChannelId)
                    val byName = !byId && programsByName.containsKey(ch.name.lowercase().trim())
                    byName
                }
            }
            _uiState.value = _uiState.value.copy(
                epgPrograms = allPrograms,
                epgProgramsByName = programsByName,
                epgLoading = false,
                epgMatchCount = matchedIds + matchedNames,
            )
        }
    }

    fun clearSourceSelection() {
        _uiState.value = _uiState.value.copy(selectedSourceIds = emptySet())
        applyFilters()
    }

    fun toggleSourceSelection(index: Int) {
        val current = _uiState.value.selectedSourceIds
        val allSourceIds = settings.m3uPlaylists.map { it.id } + settings.xtreamAccounts.map { it.id }
        val sourceId = allSourceIds.getOrNull(index) ?: return
        val newIds = if (sourceId in current) current - sourceId else current + sourceId
        _uiState.value = _uiState.value.copy(selectedSourceIds = newIds)
        applyFilters()
    }

    fun selectCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun getAllCategories(): List<String> {
        return getAllChannels().mapNotNull { it.group }.distinct().sorted()
    }

    fun getAllSourceNames(): List<String> {
        return settings.m3uPlaylists.map { it.name } + settings.xtreamAccounts.map { it.name } + settings.stalkerAccounts.map { it.name }
    }

    fun getAllSourceIds(): List<String> {
        return settings.m3uPlaylists.map { it.id } + settings.xtreamAccounts.map { it.id } + settings.stalkerAccounts.map { it.id }
    }

    fun toggleFavorite(channelId: String) {
        val current = settings.favoriteChannelIds
        settings = settings.copy(
            favoriteChannelIds = if (channelId in current) current - channelId else current + channelId
        )
        saveToStorage()
        _uiState.value = _uiState.value.copy(favoriteChannelIds = settings.favoriteChannelIds)
    }

    fun isFavorite(channelId: String): Boolean = channelId in settings.favoriteChannelIds

    fun getFavoriteChannels(): List<IptvChannel> {
        return getAllChannels().filter { it.id in settings.favoriteChannelIds }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun togglePlaylistsExpanded() {
        _uiState.value = _uiState.value.copy(playlistsExpanded = !_uiState.value.playlistsExpanded)
    }

    fun toggleChannelsExpanded() {
        _uiState.value = _uiState.value.copy(channelsExpanded = !_uiState.value.channelsExpanded)
    }

    fun toggleFavoritesExpanded() {
        _uiState.value = _uiState.value.copy(favoritesExpanded = !_uiState.value.favoritesExpanded)
    }

    fun addPredefinedPlaylist() {
        val exists = settings.m3uPlaylists.any { it.url == IPTV_ORG_URL }
        if (!exists) {
            addM3uPlaylist(IPTV_ORG_NAME, IPTV_ORG_URL)
        }
    }

    fun hasPredefinedPlaylist(): Boolean =
        settings.m3uPlaylists.any { it.url == IPTV_ORG_URL }

    fun addToHistory(channelId: String) {
        val current = settings.channelHistory.toMutableList()
        current.remove(channelId)
        current.add(0, channelId)
        settings = settings.copy(channelHistory = current.take(15))
        saveToStorage()
    }

    fun getHistoryChannels(): List<IptvChannel> {
        return settings.channelHistory.mapNotNull { id ->
            getAllChannels().find { it.id == id }
        }
    }

    fun getLastFilteredChannels(): List<IptvChannel> = _uiState.value.channels

    fun refreshSports() {
        scope.launch {
            var dbg = ""
            _uiState.value = _uiState.value.copy(sportLoading = true)
            val allCh = getAllChannels()
            dbg += "Channels: ${allCh.size}\n"

            try {
                val espnProcessed = EspnClient.fetchAll()
                dbg += "ESPN events: ${espnProcessed.size}\n"
                val espnSportEvents = EspnClient.toSportEvents(espnProcessed)
                val espnMatched = EspnClient.matchSportEventsToChannels(espnSportEvents, allCh)
                dbg += "ESPN matches: ${espnMatched.size}\n"

                val today = TraktPlatformClock.nowEpochMs()
                val totalSeconds = today / 1000L
                val rawDays = totalSeconds / 86400L
                var y = 1970
                var remaining = rawDays
                while (remaining >= 365 + (if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 1 else 0)) {
                    remaining -= 365 + (if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 1 else 0)
                    y++
                }
                val daysInMonth = listOf(31, if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
                var m = 0
                var d = remaining
                while (m < 12 && d >= daysInMonth[m]) { d -= daysInMonth[m]; m++ }
                m++
                d++
                val dateStr = "${y}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                val tdbEvents = SportsClient.fetchTodaysEvents(dateStr)
                dbg += "SportsDB: ${tdbEvents.size}\n"
                val tdbMatched = EspnClient.matchSportEventsToChannels(tdbEvents, allCh)
                dbg += "SportsDB matches: ${tdbMatched.size}\n"

                try {
                    val tvEpisodes = TvMazeClient.fetchSchedule("US")
                    dbg += "TV episodes: ${tvEpisodes.size}\n"
                    val tvMatched = TvMazeClient.matchToChannels(tvEpisodes, allCh)
                    dbg += "TV matches: ${tvMatched.size}\n"
                    val ufc = espnProcessed.filter { it.sport == "Fighting" && it.league.contains("ufc", ignoreCase = true) }
                    dbg += "UFC events: ${ufc.size}\n"

                    _uiState.value = _uiState.value.copy(
                        sportEvents = espnMatched + tdbMatched,
                        espnEvents = espnProcessed,
                        tvShows = tvMatched,
                        ufcEvents = ufc,
                        sportLoading = false,
                        tvLoading = false,
                        debugText = dbg,
                    )
                } catch (e: Exception) {
                    dbg += "TV/UFC error: ${e.message}\n"
                    _uiState.value = _uiState.value.copy(sportLoading = false, debugText = dbg)
                }
            } catch (e: Exception) {
                dbg += "Sports error: ${e.message}\n"
                _uiState.value = _uiState.value.copy(sportLoading = false, debugText = dbg)
            }
        }
    }

    private fun getAllChannels(): List<IptvChannel> {
        val m3uChannels = settings.m3uPlaylists.flatMap { it.channels }
        val xtreamChannels = settings.xtreamAccounts.flatMap { it.channels }
        val stalkerChannels = settings.stalkerAccounts.flatMap { it.channels }
        return m3uChannels + xtreamChannels + stalkerChannels
    }

    private fun applyFilters() {
        try {
            val state = _uiState.value
            val selectedIds = state.selectedSourceIds
            val query = state.searchQuery.trim().lowercase()
            val category = state.selectedCategory

            var filtered = getAllChannels()

            if (selectedIds.isNotEmpty()) {
                filtered = filtered.filter { it.sourceId in selectedIds }
            } else {
                val allIds = getAllSourceIds().toSet()
                filtered = filtered.filter { it.sourceId in allIds }
            }

            if (query.isNotEmpty()) {
                filtered = filtered.filter { it.name.lowercase().contains(query) }
            }

            if (category != null) {
                filtered = filtered.filter { it.group == category }
            }

            _uiState.value = _uiState.value.copy(
                m3uPlaylists = settings.m3uPlaylists,
                xtreamAccounts = settings.xtreamAccounts,
                stalkerAccounts = settings.stalkerAccounts,
                epgSources = settings.epgSources,
                favoriteChannelIds = settings.favoriteChannelIds,
                channels = filtered,
                isLoading = false,
                error = null,
                epgPrograms = _uiState.value.epgPrograms,
                epgProgramsByName = _uiState.value.epgProgramsByName,
                epgLoading = _uiState.value.epgLoading,
                epgMatchCount = _uiState.value.epgMatchCount,
                sportEvents = _uiState.value.sportEvents,
                espnEvents = _uiState.value.espnEvents,
                sportLoading = _uiState.value.sportLoading,
                tvShows = _uiState.value.tvShows,
                tvLoading = _uiState.value.tvLoading,
                ufcEvents = _uiState.value.ufcEvents,
                debugText = _uiState.value.debugText,
            )
        } catch (_: Exception) { }
    }

    private fun refreshUi() {
        applyFilters()
    }
}

@Serializable
private data class StoredIptvSettings(
    val m3uPlaylists: List<StoredM3uPlaylist> = emptyList(),
    val xtreamAccounts: List<StoredXtreamAccount> = emptyList(),
    val stalkerAccounts: List<StoredStalkerAccount> = emptyList(),
    val epgSources: List<StoredEpgSource> = emptyList(),
    val favoriteChannelIds: List<String> = emptyList(),
    val channelHistory: List<String> = emptyList(),
) {
    fun toSettings() = IptvPlaylistSettings(
        m3uPlaylists = m3uPlaylists.map { it.toPlaylist() },
        xtreamAccounts = xtreamAccounts.map { it.toAccount() },
        stalkerAccounts = stalkerAccounts.map { it.toAccount() },
        epgSources = epgSources.map { EpgSource(id = it.id, name = it.name, url = it.url) },
        favoriteChannelIds = favoriteChannelIds.toSet(),
        channelHistory = channelHistory,
    )

    companion object {
        fun fromSettings(s: IptvPlaylistSettings) = StoredIptvSettings(
            m3uPlaylists = s.m3uPlaylists.map { StoredM3uPlaylist.fromPlaylist(it) },
            xtreamAccounts = s.xtreamAccounts.map { StoredXtreamAccount.fromAccount(it) },
            stalkerAccounts = s.stalkerAccounts.map { StoredStalkerAccount.fromAccount(it) },
            epgSources = s.epgSources.map { StoredEpgSource(id = it.id, name = it.name, url = it.url) },
            favoriteChannelIds = s.favoriteChannelIds.toList(),
            channelHistory = s.channelHistory,
        )
    }
}

@Serializable
private data class StoredM3uPlaylist(
    val id: String,
    val name: String,
    val url: String,
    val channels: List<StoredIptvChannel> = emptyList(),
) {
    fun toPlaylist() = M3uPlaylist(id = id, name = name, url = url, channels = channels.map { it.toChannel() })

    companion object {
        fun fromPlaylist(p: M3uPlaylist) = StoredM3uPlaylist(
            id = p.id, name = p.name, url = p.url,
            channels = p.channels.map { StoredIptvChannel.fromChannel(it) },
        )
    }
}

@Serializable
private data class StoredXtreamAccount(
    val id: String,
    val name: String,
    val server: String,
    val username: String,
    val password: String,
    val channels: List<StoredIptvChannel> = emptyList(),
    val categories: List<StoredXtreamCategory> = emptyList(),
) {
    fun toAccount() = XtreamAccount(
        id = id, name = name, server = server, username = username, password = password,
        channels = channels.map { it.toChannel() },
        categories = categories.map { XtreamCategory(it.id, it.name) },
    )

    companion object {
        fun fromAccount(a: XtreamAccount) = StoredXtreamAccount(
            id = a.id, name = a.name, server = a.server, username = a.username, password = a.password,
            channels = a.channels.map { StoredIptvChannel.fromChannel(it) },
            categories = a.categories.map { StoredXtreamCategory(it.id, it.name) },
        )
    }
}

@Serializable
private data class StoredStalkerAccount(
    val id: String,
    val name: String,
    val server: String,
    val macAddress: String,
    val channels: List<StoredIptvChannel> = emptyList(),
) {
    fun toAccount() = StalkerAccount(
        id = id, name = name, server = server, macAddress = macAddress,
        channels = channels.map { it.toChannel() },
    )

    companion object {
        fun fromAccount(a: StalkerAccount) = StoredStalkerAccount(
            id = a.id, name = a.name, server = a.server, macAddress = a.macAddress,
            channels = a.channels.map { StoredIptvChannel.fromChannel(it) },
        )
    }
}

@Serializable
private data class StoredXtreamCategory(val id: String, val name: String)

@Serializable
private data class StoredEpgSource(val id: String, val name: String, val url: String)

@Serializable
private data class StoredIptvChannel(
    val id: String, val name: String, val logo: String? = null,
    val group: String? = null, val url: String, val epgChannelId: String? = null,
    val sourceType: String, val sourceId: String,
) {
    fun toChannel() = IptvChannel(
        id = id, name = name, logo = logo, group = group, url = url,
        epgChannelId = epgChannelId,
        sourceType = if (sourceType == "M3U") SourceType.M3U else SourceType.Xtream,
        sourceId = sourceId,
    )

    companion object {
        fun fromChannel(c: IptvChannel) = StoredIptvChannel(
            id = c.id, name = c.name, logo = c.logo, group = c.group, url = c.url,
            epgChannelId = c.epgChannelId, sourceType = c.sourceType.name, sourceId = c.sourceId,
        )
    }
}
