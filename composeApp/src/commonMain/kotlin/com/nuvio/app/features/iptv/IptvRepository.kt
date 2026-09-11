package com.nuvio.app.features.iptv

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.addons.httpGetTextChunked
import com.nuvio.app.features.addons.httpGetTextWithHeaders
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object IptvRepository {
    private const val CHANNEL_CACHE_TTL_MS = 3_600_000L
    private const val MAX_EPG_CACHE_PROGRAMS = 150_000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var idCounter = 0L

    private var epgJob: Job? = null

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var settings = IptvPlaylistSettings()
    private var cachedAllChannels: List<IptvChannel> = emptyList()

    private fun updateCachedAllChannels() {
        val m3uChannels = settings.m3uPlaylists.flatMap { it.channels }
        val xtreamChannels = settings.xtreamAccounts.flatMap { it.channels }
        val stalkerChannels = settings.stalkerAccounts.flatMap { it.channels }
        cachedAllChannels = m3uChannels + xtreamChannels + stalkerChannels
    }

    private val IPTV_ORG_URL = "https://iptv-org.github.io/iptv/index.m3u"
    private val IPTV_ORG_NAME = "iptv-org"
    private val MJH_EPG_URL = "https://raw.githubusercontent.com/matthuisman/i.mjh.nz/master/all/epg.xml"
    private val MJH_EPG_NAME = "i.mjh.nz EPG"

    private fun nextId(prefix: String): String {
        return "${prefix}_${++idCounter}_${TraktPlatformClock.nowEpochMs()}"
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        try {
            loadFromStorage()
        } catch (e: Exception) {
            e.printStackTrace()
            settings = IptvPlaylistSettings()
            saveToStorage()
            refreshUi()
        }
    }

    private fun loadFromStorage() {
        val payload = try {
            IptvStorage.loadSettings()
        } catch (e: Exception) {
            e.printStackTrace()
            IptvStorage.saveSettings(json.encodeToString(StoredIptvSettings.fromSettings(IptvPlaylistSettings())))
            null
        }
        if (payload != null) {
            try {
                settings = hydrateChannels(json.decodeFromString<StoredIptvSettings>(payload).toSettings())
            } catch (_: Exception) {
                settings = IptvPlaylistSettings()
                IptvStorage.saveSettings(json.encodeToString(StoredIptvSettings.fromSettings(settings)))
            }
        }
        updateCachedAllChannels()
        refreshUi()
    }

    private fun saveToStorage() {
        val stripped = IptvPlaylistSettings(
            m3uPlaylists = settings.m3uPlaylists.map { it.copy(channels = emptyList()) },
            xtreamAccounts = settings.xtreamAccounts.map { it.copy(channels = emptyList()) },
            stalkerAccounts = settings.stalkerAccounts.map { it.copy(channels = emptyList()) },
            epgSources = settings.epgSources,
            favoriteChannelIds = settings.favoriteChannelIds,
            channelHistory = settings.channelHistory,
        )
        try {
            IptvStorage.saveSettings(json.encodeToString(StoredIptvSettings.fromSettings(stripped)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun m3uCacheKey(playlist: M3uPlaylist): String =
        if (playlist.url.isNotBlank()) playlist.url else "m3u:${playlist.id}"

    private fun xtreamCacheKey(account: XtreamAccount): String = "xtream:${account.id}"

    private fun stalkerCacheKey(account: StalkerAccount): String = "stalker:${account.id}"

    private fun loadChannelsFromCacheRaw(key: String): List<IptvChannel>? {
        return try {
            val cached = IptvStorage.loadChannelCache(key) ?: return null
            val data = json.decodeFromString<StoredChannelCache>(cached)
            data.channels.map { it.toChannel() }
        } catch (_: Exception) { null }
    }

    private fun hydrateChannels(s: IptvPlaylistSettings): IptvPlaylistSettings {
        return s.copy(
            m3uPlaylists = s.m3uPlaylists.map { p ->
                val key = m3uCacheKey(p)
                val cached = loadChannelsFromCacheRaw(key)
                if (cached != null && cached.isNotEmpty()) {
                    p.copy(channels = cached)
                } else if (p.channels.isNotEmpty()) {
                    saveChannelsToCache(key, p.channels)
                    p
                } else {
                    p
                }
            },
            xtreamAccounts = s.xtreamAccounts.map { a ->
                val key = xtreamCacheKey(a)
                val cached = loadChannelsFromCacheRaw(key)
                if (cached != null && cached.isNotEmpty()) {
                    a.copy(channels = cached)
                } else if (a.channels.isNotEmpty()) {
                    saveChannelsToCache(key, a.channels)
                    a
                } else {
                    a
                }
            },
            stalkerAccounts = s.stalkerAccounts.map { a ->
                val key = stalkerCacheKey(a)
                val cached = loadChannelsFromCacheRaw(key)
                if (cached != null && cached.isNotEmpty()) {
                    a.copy(channels = cached)
                } else if (a.channels.isNotEmpty()) {
                    saveChannelsToCache(key, a.channels)
                    a
                } else {
                    a
                }
            },
        )
    }

    fun addM3uPlaylist(name: String, url: String) {
        scope.launch {
            val id = nextId("m3u")
            val playlist = M3uPlaylist(id = id, name = name, url = url)
            settings = settings.copy(m3uPlaylists = settings.m3uPlaylists + playlist)
            if (url == IPTV_ORG_URL) {
                maybeAutoAddIptvOrgEpg()
            }
            saveToStorage()
            refreshUi()
            refreshM3uChannels(id)
        }
    }

    private fun maybeAutoAddIptvOrgEpg() {
        if (settings.epgSources.any { it.url == MJH_EPG_URL }) return
        val id = nextId("epg")
        settings = settings.copy(epgSources = settings.epgSources + EpgSource(id = id, name = MJH_EPG_NAME, url = MJH_EPG_URL))
        saveToStorage()
    }

    fun removeM3uPlaylist(id: String) {
        val playlist = settings.m3uPlaylists.find { it.id == id }
        settings = settings.copy(m3uPlaylists = settings.m3uPlaylists.filter { it.id != id })
        saveToStorage()
        playlist?.let { IptvStorage.invalidateChannelCache(m3uCacheKey(it)) }
        refreshUi()
    }

    fun invalidateChannelCache(url: String) {
        IptvStorage.invalidateChannelCache(url)
    }

    private fun loadChannelsFromCache(url: String): Pair<List<IptvChannel>, Boolean>? {
        return try {
            val cached = IptvStorage.loadChannelCache(url) ?: return null
            val data = json.decodeFromString<StoredChannelCache>(cached)
            val age = System.currentTimeMillis() - data.timestamp
            if (age in 0..CHANNEL_CACHE_TTL_MS) {
                data.channels.map { it.toChannel() } to true
            } else {
                null
            }
        } catch (_: Exception) { null }
    }

    private fun saveChannelsToCache(url: String, channels: List<IptvChannel>) {
        try {
            val data = json.encodeToString(StoredChannelCache(
                channels = channels.map { StoredIptvChannel.fromChannel(it) },
                timestamp = System.currentTimeMillis(),
            ))
            IptvStorage.saveChannelCache(url, data)
        } catch (_: Throwable) { }
    }

    fun refreshM3uChannels(id: String) {
        scope.launch {
            val playlist = settings.m3uPlaylists.find { it.id == id } ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, refreshingSourceIds = _uiState.value.refreshingSourceIds + id)

            val cached = loadChannelsFromCache(m3uCacheKey(playlist))
            if (cached != null) {
                val (channels, _) = cached
                val updated = playlist.copy(channels = channels)
                settings = settings.copy(
                    m3uPlaylists = settings.m3uPlaylists.map { if (it.id == id) updated else it }
                )
                saveToStorage()
                _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - id)
                refreshUi()
                return@launch
            }

            try {
                val m3uContent = httpGetTextWithHeaders(playlist.url, mapOf("Accept" to "*/*", "User-Agent" to "Nuvio/1.0"))
                val channels = M3uParser.parse(m3uContent, id)
                saveChannelsToCache(m3uCacheKey(playlist), channels)
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
            saveChannelsToCache(m3uCacheKey(playlist), channels)
            saveToStorage()
            refreshUi()
        }
    }

    /** Add an M3U source from a URL and wait for it to load. Returns the number of channels parsed. */
    suspend fun addM3uPlaylistAndLoad(name: String, url: String): Result<Int> {
        val id = nextId("m3u")
        val playlist = M3uPlaylist(id = id, name = name.ifBlank { url.take(30) }, url = url)
        settings = settings.copy(m3uPlaylists = settings.m3uPlaylists + playlist)
        if (url == IPTV_ORG_URL) maybeAutoAddIptvOrgEpg()
        saveToStorage()
        refreshUi()
        return loadM3uChannelsSuspended(playlist)
    }

    /** Add an M3U source from pasted file content. Returns the number of channels parsed. */
    suspend fun addM3uPlaylistFromFile(content: String, name: String): Result<Int> {
        return try {
            val id = nextId("m3u_up")
            val channels = M3uParser.parse(content, id)
            val playlist = M3uPlaylist(id = id, name = name.ifBlank { "Uploaded Playlist" }, url = "", channels = channels)
            settings = settings.copy(m3uPlaylists = settings.m3uPlaylists + playlist)
            saveChannelsToCache(m3uCacheKey(playlist), channels)
            saveToStorage()
            refreshUi()
            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadM3uChannelsSuspended(playlist: M3uPlaylist): Result<Int> {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            refreshingSourceIds = _uiState.value.refreshingSourceIds + playlist.id,
        )
        val cached = loadChannelsFromCache(m3uCacheKey(playlist))
        if (cached != null) {
            val (channels, _) = cached
            settings = settings.copy(
                m3uPlaylists = settings.m3uPlaylists.map { if (it.id == playlist.id) it.copy(channels = channels) else it }
            )
            saveToStorage()
            _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - playlist.id)
            refreshUi()
            return Result.success(channels.size)
        }
        return try {
            val m3uContent = httpGetTextWithHeaders(playlist.url, mapOf("Accept" to "*/*", "User-Agent" to "Nuvio/1.0"))
            val channels = M3uParser.parse(m3uContent, playlist.id)
            saveChannelsToCache(m3uCacheKey(playlist), channels)
            settings = settings.copy(
                m3uPlaylists = settings.m3uPlaylists.map { if (it.id == playlist.id) it.copy(channels = channels) else it }
            )
            saveToStorage()
            _uiState.value = _uiState.value.copy(refreshingSourceIds = _uiState.value.refreshingSourceIds - playlist.id)
            refreshUi()
            if (settings.epgSources.isNotEmpty()) refreshEpg()
            Result.success(channels.size)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load M3U: ${e.message}",
                refreshingSourceIds = _uiState.value.refreshingSourceIds - playlist.id,
            )
            Result.failure(e)
        }
    }

    fun addXtreamAccount(name: String, server: String, username: String, password: String, info: PortalAccountInfo? = null) {
        scope.launch {
            val id = nextId("xtream")
            val account = XtreamAccount(id = id, name = name, server = server, username = username, password = password, info = info)
            settings = settings.copy(xtreamAccounts = settings.xtreamAccounts + account)
            saveToStorage()
            refreshUi()
            refreshXtreamChannels(id)
        }
    }

    fun removeXtreamAccount(id: String) {
        val account = settings.xtreamAccounts.find { it.id == id }
        settings = settings.copy(xtreamAccounts = settings.xtreamAccounts.filter { it.id != id })
        saveToStorage()
        account?.let { IptvStorage.invalidateChannelCache(xtreamCacheKey(it)) }
        refreshUi()
    }

    fun portalAccountCount(): Int =
        settings.xtreamAccounts.count { PortalLicenseManager.isPortalName(it.name) }

    /** Removes all portal-added Xtream accounts. Returns how many were removed. */
    fun removePortalAccounts(): Int {
        val portals = settings.xtreamAccounts.filter { PortalLicenseManager.isPortalName(it.name) }
        if (portals.isEmpty()) return 0
        settings = settings.copy(xtreamAccounts = settings.xtreamAccounts.filterNot { PortalLicenseManager.isPortalName(it.name) })
        portals.forEach { IptvStorage.invalidateChannelCache(xtreamCacheKey(it)) }
        saveToStorage()
        refreshUi()
        return portals.size
    }

    fun refreshXtreamChannels(id: String) {
        scope.launch {
            val account = settings.xtreamAccounts.find { it.id == id } ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, refreshingSourceIds = _uiState.value.refreshingSourceIds + id)
            try {
                val baseUrl = account.server.trimEnd('/')
                val creds = "username=${account.username}&password=${account.password}"
                val headers = mapOf("User-Agent" to "VLC/3.0.20")

                val info = try {
                    XtreamClient.parseAccountInfo(httpGetTextWithHeaders("$baseUrl/player_api.php?$creds", headers))
                } catch (_: Exception) { null }

                var categoriesJson = try {
                    httpGetTextWithHeaders("$baseUrl/player_api.php?$creds&action=get_live_categories", headers)
                } catch (_: Exception) {
                    try { httpGetTextWithHeaders("$baseUrl/player_api.php?$creds&action=live_categories", headers) } catch (_: Exception) { "" }
                }
                val categories = XtreamClient.parseCategories(categoriesJson)

                var streamsJson = try {
                    httpGetTextWithHeaders("$baseUrl/player_api.php?$creds&action=get_live_streams", headers)
                } catch (_: Exception) {
                    try { httpGetTextWithHeaders("$baseUrl/player_api.php?$creds&action=live_streams", headers) } catch (_: Exception) { "" }
                }
                val channels = XtreamClient.parseChannels(streamsJson, id, account.server, account.username, account.password)

                val updated = account.copy(categories = categories, channels = channels, info = info ?: account.info)
                settings = settings.copy(
                    xtreamAccounts = settings.xtreamAccounts.map { if (it.id == id) updated else it }
                )
                saveChannelsToCache(xtreamCacheKey(account), channels)
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
        val account = settings.stalkerAccounts.find { it.id == id }
        settings = settings.copy(stalkerAccounts = settings.stalkerAccounts.filter { it.id != id })
        saveToStorage()
        account?.let { IptvStorage.invalidateChannelCache(stalkerCacheKey(it)) }
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
                saveChannelsToCache(stalkerCacheKey(account), channels)
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
        refreshEpg()
    }

    fun addUploadedEpgSource(name: String, content: String) {
        val id = nextId("epg_upload")
        IptvStorage.saveEpgSourceContent(id, content)
        settings = settings.copy(epgSources = settings.epgSources + EpgSource(id = id, name = name, url = "file://$id"))
        saveToStorage()
        refreshUi()
        refreshEpg()
    }

    fun removeEpgSource(id: String) {
        val source = settings.epgSources.find { it.id == id }
        settings = settings.copy(epgSources = settings.epgSources.filter { it.id != id })
        if (source?.url?.startsWith("file://") == true) {
            IptvStorage.deleteEpgSourceContent(id)
        }
        saveToStorage()
        refreshUi()
    }

    fun refreshEpg() {
        epgJob?.cancel()
        epgJob = scope.launch {
            _uiState.value = _uiState.value.copy(epgLoading = true, epgError = null)
            try {
                withTimeout(180_000L) {
                    val allPrograms = mutableMapOf<String, List<EpgProgram>>()
                    val programsByName = mutableMapOf<String, List<EpgProgram>>()
                    var lastError: String? = null
                    for (source in settings.epgSources) {
                        try {
                            val result = if (source.url.startsWith("file://")) {
                                val content = IptvStorage.loadEpgSourceContent(source.id)
                                if (content == null) {
                                    throw IllegalStateException("Local EPG file missing for \"${source.name}\"")
                                }
                                EpgParser.parseXmltv(content)
                            } else {
                                EpgParser.parseXmltvStream { emit ->
                                    httpGetTextChunked(
                                        source.url,
                                        mapOf("Accept" to "application/xml, text/xml, */*"),
                                    ) { chunk ->
                                        emit(chunk)
                                        true
                                    }
                                }
                            }
                            allPrograms.putAll(result.programsByChannelId)
                            for ((chId, progs) in result.programsByChannelId) {
                                val displayName = result.channelDisplayNames[chId]
                                if (displayName != null) {
                                    val key = normalizeIptvName(displayName)
                                    if (key.isNotEmpty()) {
                                        val existing = programsByName[key]
                                        programsByName[key] = if (existing == null) {
                                            progs
                                        } else {
                                            (existing + progs).sortedBy { it.startTime }
                                        }
                                    }
                                }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: OutOfMemoryError) {
                            lastError = "EPG source \"${source.name}\" is too large"
                        } catch (e: Exception) {
                            lastError = e.message ?: "Unknown EPG error"
                        }
                    }
                    val programsByNormId = buildNormalizedIdIndex(allPrograms)
                    val allCh = getAllChannels()
                    val matchedIds = allCh.count { ch ->
                        normalizeEpgId(ch.epgChannelId).let { it.isNotEmpty() && programsByNormId.containsKey(it) }
                    }
                    val matchedNames = allCh.count { ch ->
                        val byId = normalizeEpgId(ch.epgChannelId).let { it.isNotEmpty() && programsByNormId.containsKey(it) }
                        val byName = !byId && normalizeIptvName(ch.name).let { it.isNotEmpty() && programsByName.containsKey(it) }
                        byName
                    }
                    _uiState.value = _uiState.value.copy(
                        epgPrograms = allPrograms,
                        epgProgramsByName = programsByName,
                        epgProgramsByNormId = programsByNormId,
                        epgLoading = false,
                        epgError = lastError,
                        epgMatchCount = matchedIds + matchedNames,
                    )
                    saveEpgCache(allPrograms, programsByName, programsByNormId)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                _uiState.value = _uiState.value.copy(
                    epgLoading = false,
                    epgError = "EPG feed is too large to load",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    epgLoading = false,
                    epgError = if (e is kotlinx.coroutines.TimeoutCancellationException) "EPG loading timed out" else "EPG error: ${e.message}",
                )
            }
        }
    }

    fun clearSourceSelection() {
        _uiState.value = _uiState.value.copy(selectedSourceIds = emptySet())
        applyFilters()
    }

    fun toggleSourceSelection(index: Int) {
        val current = _uiState.value.selectedSourceIds
        val allSourceIds = getAllSourceIds()
        val sourceId = allSourceIds.getOrNull(index) ?: return
        val newIds = if (sourceId in current) current - sourceId else current + sourceId
        _uiState.value = _uiState.value.copy(selectedSourceIds = newIds)
        applyFilters()
    }

    fun selectSource(index: Int) {
        val allSourceIds = getAllSourceIds()
        val sourceId = allSourceIds.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(selectedSourceIds = setOf(sourceId))
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

    fun getFavoriteChannelIds(): Set<String> = settings.favoriteChannelIds

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
        } else {
            maybeAutoAddIptvOrgEpg()
            refreshEpg()
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
        _uiState.value = _uiState.value.copy(channelHistoryIds = settings.channelHistory)
    }

    fun clearHistory() {
        settings = settings.copy(channelHistory = emptyList())
        saveToStorage()
        _uiState.value = _uiState.value.copy(channelHistoryIds = emptyList())
        refreshUi()
    }

    fun getHistoryChannels(): List<IptvChannel> {
        return settings.channelHistory.mapNotNull { id ->
            getAllChannels().find { it.id == id }
        }
    }

    fun getLastFilteredChannels(): List<IptvChannel> = _uiState.value.channels

    fun getXtreamAccounts(): List<XtreamAccount> = settings.xtreamAccounts

    suspend fun getEpgProgramsForChannel(channel: IptvChannel): List<EpgProgram> {
        return when (channel.sourceType) {
            SourceType.Xtream -> {
                val account = settings.xtreamAccounts.find { it.id == channel.sourceId }
                if (account != null) ShortEpgCache.getOrLoad(account, channel, limit = 2)
                else emptyList()
            }
            SourceType.Stalker, SourceType.M3U -> {
                val byId = channel.epgChannelId
                    ?.let { id -> _uiState.value.epgProgramsByNormId[normalizeEpgId(id)] }
                    ?.takeIf { it.isNotEmpty() }
                byId ?: _uiState.value.epgProgramsByName[normalizeIptvName(channel.name)]
                    ?.takeIf { it.isNotEmpty() } ?: emptyList()
            }
        }
    }

    suspend fun getEpgNowNext(channelId: String?, channelName: String?): Pair<EpgProgram?, EpgProgram?> {
        val now = TraktPlatformClock.nowEpochMs()
        val programs = channelId?.let { id -> getAllChannels().find { it.id == id } }
            ?.let { ch -> getEpgProgramsForChannel(ch) }
            ?: channelName?.let { name -> _uiState.value.epgProgramsByName[normalizeIptvName(name)] }
                ?.takeIf { it.isNotEmpty() }
                ?: emptyList()
        val current = programs.firstOrNull { now in it.startTime until it.endTime }
        val next = programs.firstOrNull { it.startTime > now }
        return current to next
    }

    fun getAllChannels(): List<IptvChannel> {
        if (!hasLoaded) {
            ensureLoaded()
        }
        return cachedAllChannels
    }

    /**
     * Builds the map of normalized channel-name keys to the "current" EPG program title, using
     * the pre-loaded XMLTV cache. Empty when no EPG is loaded for that channel (M3U/Stalker
     * fallback), so the scorer can still boost channels whose program contains the teams.
     */
    fun buildCurrentEpgTitleMap(): Map<String, String> {
        val state = _uiState.value
        val now = TraktPlatformClock.nowEpochMs()
        val titlesByChannelKey = mutableMapOf<String, String>()
        for ((nameKey, programs) in state.epgProgramsByName) {
            val current = programs.firstOrNull { now in it.startTime until it.endTime }?.title
            if (!current.isNullOrBlank()) {
                titlesByChannelKey[com.nuvio.app.features.sports.ChannelText.channelNameKey(nameKey)] = current
            }
        }
        return titlesByChannelKey
    }

    /**
     * Builds a closure resolving the current EPG program title for a channel name from the
     * pre-loaded XMLTV cache. Empty string when no EPG is loaded (M3U/Stalker fallback), so the
     * scorer can boost channels whose "now" program contains both teams.
     */
    fun buildCurrentEpgTitleLookup(): (String) -> String =
        com.nuvio.app.features.sports.ChannelScorer.buildEpgLookup(buildCurrentEpgTitleMap())

    /**
     * Lazily and concurrently enriches the EPG lookup for the given candidate channels (the ones
     * that already scored > 0 on name/league/generic). Xtream candidates get their current program
     * fetched via [ShortEpgCache.getOrLoad]; M3U/Stalker candidates fall back to the XMLTV map.
     * Capped at [cap] channels so a per-candidate HTTP fan-out never stalls first paint.
     */
    suspend fun buildLazyEpgTitleLookup(candidates: List<IptvChannel>, cap: Int = 8): (String) -> String {
        val merged = buildCurrentEpgTitleMap().toMutableMap()
        if (candidates.isEmpty()) return com.nuvio.app.features.sports.ChannelScorer.buildEpgLookup(merged)

        val xtreamCandidates = candidates.filter { it.sourceType == SourceType.Xtream }.take(cap)
        val now = TraktPlatformClock.nowEpochMs()

        val fetched = kotlinx.coroutines.coroutineScope {
            val results = xtreamCandidates.map { channel ->
                async(kotlinx.coroutines.Dispatchers.Default) {
                    val account = settings.xtreamAccounts.find { it.id == channel.sourceId } ?: return@async null
                    try {
                        val programs = ShortEpgCache.getOrLoad(account, channel, limit = 2)
                        val current = programs.firstOrNull { now in it.startTime until it.endTime }?.title
                        if (current.isNullOrBlank()) null
                        else com.nuvio.app.features.sports.ChannelText.channelNameKey(channel.name) to current
                    } catch (_: Throwable) {
                        null
                    }
                }
            }.mapNotNull { it.await() }
            results
        }
        for ((key, title) in fetched) {
            if (title.isNotBlank()) merged[key] = title
        }
        return com.nuvio.app.features.sports.ChannelScorer.buildEpgLookup(merged)
    }

    private fun applyFilters() {
        try {
            updateCachedAllChannels()
            val state = _uiState.value
            val selectedIds = state.selectedSourceIds
            val query = state.searchQuery.trim().lowercase()
            val category = state.selectedCategory

            var filtered = cachedAllChannels

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
                epgProgramsByNormId = _uiState.value.epgProgramsByNormId,
                epgLoading = _uiState.value.epgLoading,
                epgMatchCount = _uiState.value.epgMatchCount,
                debugText = _uiState.value.debugText,
            )
        } catch (_: Exception) { }
    }

    private fun loadCachedEpg() {
        val cached = IptvStorage.loadEpgCache() ?: return
        try {
            val data = json.decodeFromString<EpgCacheData>(cached)
            val age = TraktPlatformClock.nowEpochMs() - data.timestamp
            if (data.timestamp > 0 && age in 0..3_600_000L) {
                val programsByNormId = buildNormalizedIdIndex(data.programsByChannelId)
                val allCh = getAllChannels()
                val matchedIds = allCh.count { ch ->
                    normalizeEpgId(ch.epgChannelId).let { it.isNotEmpty() && programsByNormId.containsKey(it) }
                }
                val matchedNames = allCh.count { ch ->
                    val byId = normalizeEpgId(ch.epgChannelId).let { it.isNotEmpty() && programsByNormId.containsKey(it) }
                    val byName = !byId && normalizeIptvName(ch.name).let { it.isNotEmpty() && data.programsByName.containsKey(it) }
                    byName
                }
                _uiState.value = _uiState.value.copy(
                    epgPrograms = data.programsByChannelId,
                    epgProgramsByName = data.programsByName,
                    epgProgramsByNormId = programsByNormId,
                    epgLoading = false,
                    epgMatchCount = matchedIds + matchedNames,
                )
            }
        } catch (_: Exception) { }
    }

    private fun saveEpgCache(programs: Map<String, List<EpgProgram>>, programsByName: Map<String, List<EpgProgram>>, programsByNormId: Map<String, List<EpgProgram>>) {
        try {
            val totalPrograms = programs.values.sumOf { it.size }
            if (totalPrograms > MAX_EPG_CACHE_PROGRAMS) return
            val data = json.encodeToString(EpgCacheData(
                programsByChannelId = programs,
                programsByName = programsByName,
                programsByNormId = programsByNormId,
                timestamp = TraktPlatformClock.nowEpochMs(),
            ))
            if (data.length > 8_000_000) return
            IptvStorage.saveEpgCache(data)
        } catch (_: Throwable) { }
    }

    private fun refreshUi() {
        applyFilters()
    }
}

@kotlinx.serialization.Serializable
private data class EpgCacheData(
    val programsByChannelId: Map<String, List<EpgProgram>>,
    val programsByName: Map<String, List<EpgProgram>>,
    val programsByNormId: Map<String, List<EpgProgram>> = emptyMap(),
    val timestamp: Long,
)

private fun buildNormalizedIdIndex(programsByChannelId: Map<String, List<EpgProgram>>): Map<String, List<EpgProgram>> {
    val result = mutableMapOf<String, List<EpgProgram>>()
    for ((id, progs) in programsByChannelId) {
        val key = normalizeEpgId(id)
        if (key.isEmpty()) continue
        val existing = result[key]
        result[key] = if (existing == null) progs else (existing + progs).sortedBy { it.startTime }
    }
    return result
}

private fun normalizeEpgId(id: String?): String {
    if (id == null) return ""
    var s = id.trim().lowercase()
    val at = s.indexOf('@')
    if (at >= 0) s = s.substring(0, at).trim()
    return s
}

private fun normalizeIptvName(name: String): String {
    if (name.isBlank()) return ""
    return name.lowercase()
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^\\]]*\\]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
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
    val info: StoredXtreamInfo? = null,
) {
    fun toAccount() = XtreamAccount(
        id = id, name = name, server = server, username = username, password = password,
        channels = channels.map { it.toChannel() },
        categories = categories.map { XtreamCategory(it.id, it.name) },
        info = info?.toInfo(),
    )

    companion object {
        fun fromAccount(a: XtreamAccount) = StoredXtreamAccount(
            id = a.id, name = a.name, server = a.server, username = a.username, password = a.password,
            channels = a.channels.map { StoredIptvChannel.fromChannel(it) },
            categories = a.categories.map { StoredXtreamCategory(it.id, it.name) },
            info = a.info?.let { StoredXtreamInfo.fromInfo(it) },
        )
    }
}

@Serializable
private data class StoredXtreamInfo(
    val expDate: Long? = null,
    val maxConnections: Int? = null,
    val activeConnections: Int? = null,
    val status: String? = null,
    val isTrial: Boolean? = null,
) {
    fun toInfo() = PortalAccountInfo(
        expDate = expDate,
        maxConnections = maxConnections,
        activeConnections = activeConnections,
        status = status,
        isTrial = isTrial,
    )

    companion object {
        fun fromInfo(i: PortalAccountInfo) = StoredXtreamInfo(
            expDate = i.expDate,
            maxConnections = i.maxConnections,
            activeConnections = i.activeConnections,
            status = i.status,
            isTrial = i.isTrial,
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
    val group: String? = null, val url: String, val audioUrl: String? = null,
    val epgChannelId: String? = null,
    val sourceType: String, val sourceId: String,
) {
    fun toChannel() = IptvChannel(
        id = id, name = name, logo = logo, group = group, url = url,
        audioUrl = audioUrl, epgChannelId = epgChannelId,
        sourceType = if (sourceType == "M3U") SourceType.M3U else SourceType.Xtream,
        sourceId = sourceId,
    )

    companion object {
        fun fromChannel(c: IptvChannel) = StoredIptvChannel(
            id = c.id, name = c.name, logo = c.logo, group = c.group, url = c.url,
            audioUrl = c.audioUrl, epgChannelId = c.epgChannelId,
            sourceType = c.sourceType.name, sourceId = c.sourceId,
        )
    }
}

@Serializable
private data class StoredChannelCache(
    val channels: List<StoredIptvChannel>,
    val timestamp: Long,
)
