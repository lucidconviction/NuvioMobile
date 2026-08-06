# Wiring Guide: Adapting Debrify Patterns to Nuvio_Robbdeeze

> **Target:** `/Users/robbdeeze/Documents/projects/Nuvio_Robbdeeze`  
> **Reference:** `/Users/robbdeeze/Documents/projects/Nuv MObile/debrify_investigation.md`  
> **Date:** 2026-07-29

---

## Executive Summary

Nuvio_Robbdeeze has **two TV features that are visual shells with broken plumbing**:

| Feature | Debrify Equivalent | What Works | What's Broken |
|---------|-------------------|------------|---------------|
| **CinematicTuner** (NuvioNutz Hub) | Stremio TV | UI, rotation, catalog fetch | Stream resolution never called, `onPlayChannel` never wired |
| **DeezeNutz Hub** | Debrify TV | UI, recipe editor, store, cache service | `simulateSearch()` generates fake data, no real debrid/torrent search, no persistence |

Meanwhile, Nuvio already has:
- A **full debrid module** (18 files: `features/debrid/` — clients for Real-Debrid, Premiumize, TorBox)
- A **full stream resolution pipeline** (`features/player/PlayerScreenRuntimeSourceActions.kt` — `resolveDebridForPlayer`, `DirectDebridPlaybackResolver`)
- A **full stream model** (`features/streams/StreamItem` — `torrentMagnetUri`, `infoHash`, `fileIdx`, `p2pFileIdx`)
- A **full addon system** (`features/addons/AddonRepository` — manifest fetching, catalog browsing via `fetchCatalogPage`)
- A **full player** (`features/player/PlayerScreen` — ExoPlayer with torrent+P2P support)

This document maps **Debrify's working patterns** → **Nuvio's existing infrastructure** to fix the broken features.

---

## 1. CinematicTuner — Fixing Stream Resolution

### The Problem

`CinematicTunerViewModel.kt` (`composeApp/src/commonMain/.../features/hub/CinematicTunerViewModel.kt`)

The VM fetches catalog items via `fetchCatalogPage()`, creates `MetaPreview` objects, and rotates through them. But when the user presses play, **no stream resolution happens**. The `onPlayChannel` callback is received by `CinematicTunerScreen` but nothing ever calls it with a proper `PlayerLaunch`.

### The Missing Pipeline

```
User presses Play on catalog item
    ↓
Query streaming addons for /stream/{type}/{id}.json  ← MISSING
    ↓
Convert streams to StreamItem objects  ← MISSING
    ↓
Resolve through DirectDebridPlaybackResolver  ← MISSING
    ↓
Build PlayerLaunch with resolved URL  ← MISSING
    ↓
Call onPlayChannel(playerLaunch)  ← EXISTS but never invoked
```

### How Debrify Does It (Reference)

In Debrify's `StremioService.searchStreams()` (`lib/services/stremio_service.dart:491`):

1. Gets all enabled streaming addons via `getStreamingAddons()`
2. Filters addons by content type support and ID prefix
3. Calls each addon's `/stream/{type}/{id}.json` endpoint
4. Converts `StremioStream` objects to `Torrent` objects (infoHash, direct URL, external URL)
5. Deduplicates and sorts by seeders
6. Returns torrent list for the UI to present

### Wiring Fix for Nuvio

#### Add this method to `CinematicTunerViewModel`:

```kotlin
// File: composeApp/src/commonMain/.../features/hub/CinematicTunerViewModel.kt

/**
 * Resolve a catalog item into playable streams and launch the player.
 *
 * Pattern matches Debrify's StremioService.searchStreams():
 * 1. Get streaming addons from AddonRepository
 * 2. Call each addon's /stream/{type}/{id}.json
 * 3. Filter usable streams (infoHash, direct URL, external URL)
 * 4. Resolve the first playable stream via DirectDebridPlaybackResolver
 * 5. Build PlayerLaunch and invoke onPlayChannel
 */
fun playCurrentItem(onPlayChannel: ((PlayerLaunch) -> Unit)?) {
    val item = getCurrentItem() ?: return
    
    scope.launch {
        // 1. Find streaming addons that support this content ID
        val streamAddons = AddonRepository.uiState.value.addons
            .filter { it.enabled && it.manifest?.streams?.isNotEmpty() == true }
            .filter { addon -> 
                item.id.startsWith("tt") && // IMDB prefix check (like Debrify's supportsContentId)
                (addon.manifest?.types?.isEmpty() != false || 
                 addon.manifest?.types?.contains(item.type) == true)
            }
        
        if (streamAddons.isEmpty()) {
            // Fallback: try direct playback from P2P if available
            onPlayChannel?.invoke(PlayerLaunch(
                profileId = 0,
                title = item.name,
                sourceUrl = item.id,
                streamTitle = item.name,
                providerName = "NuvioNutz",
                torrentInfoHash = null,
                torrentFileIdx = null,
            ))
            return@launch
        }
        
        // 2. Query streaming addons in parallel (matching Debrify's bounded concurrency)
        val streamResults = streamAddons.mapNotNull { addon ->
            try {
                val streamUrl = "${addon.manifestUrl.removeSuffix("/manifest.json")}/stream/${item.type}/${item.id}.json"
                val response = httpClient.get(streamUrl)
                val json = Json.decodeFromString<StreamResponse>(response.body)
                json.streams?.map { s -> 
                    StreamItem(
                        title = item.name,
                        streamUrl = s.url,
                        torrentMagnetUri = if (s.infoHash != null) 
                            "magnet:?xt=urn:btih:${s.infoHash}&dn=${item.name}" else null,
                        infoHash = s.infoHash,
                        fileIdx = s.fileIdx,
                        streamLabel = s.title ?: item.name,
                        addonId = addon.manifest?.id ?: "",
                    )
                }
            } catch (_: Exception) { null }
        }.flatten().filter { it.infoHash != null || it.streamUrl != null }
        
        if (streamResults.isEmpty()) return@launch
        
        // 3. Use existing DirectDebridPlaybackResolver (already in PlayerScreenRuntimeSourceActions)
        val firstStream = streamResults.first()
        val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
            stream = firstStream,
            season = null,
            episode = null,
        )
        
        if (resolved is DirectDebridPlayableResult.Success) {
            onPlayChannel?.invoke(PlayerLaunch(
                profileId = 0,
                title = item.name,
                sourceUrl = resolved.stream.playableDirectUrl ?: "",
                streamTitle = item.name,
                providerName = "NuvioNutz",
                poster = resolved.stream.poster ?: item.poster,
                torrentInfoHash = firstStream.infoHash,
                torrentFileIdx = firstStream.fileIdx,
                parentMetaId = item.id,
                parentMetaType = item.type,
            ))
        }
    }
}
```

#### Wire it in `CinematicTunerScreen.kt`:

Find where the user clicks/taps a catalog item (search for `onTap`, `clickable`, or `onClick` handlers on the meta preview cards). Replace the existing stub with:

```kotlin
// In the MetaPreview card click handler — replace whatever is there
onClick = {
    if (!isFullScreenView) {
        isFullScreenView = true
        // Start playing the current item through the stream resolution pipeline
        viewModel.playCurrentItem(onPlayChannel)
    }
}
```

---

## 2. DeezeNutz — Fixing Fake Data Generation

### The Problem

`DeezeNutzViewModel.kt` line 213-239 (`composeApp/src/commonMain/.../features/hub/DeezeNutzViewModel.kt`):

```kotlin
private suspend fun simulateSearch(recipe: DeezeRecipeExport): List<ChannelItem> {
    delay(600)  // Fake delay
    // ... ALL data is Random generated — fake hashes, fake magnets, fake seed counts
    val hash = Random.nextLong().toString(16).padStart(40, '0')
    // ...
}
```

### How Debrify Does It (Reference)

Debrify's `DebrifyTvCacheService` (`lib/services/debrify_tv_cache_service.dart`):

1. Takes keywords from the channel recipe
2. Searches all configured **debrid provider clouds** (Real-Debrid, TorBox, Premiumize, etc.)
3. Also queries **torrent indexers** (Jackett, Prowlarr, community engines)
4. Filters by quality, size, seeds, year
5. Checks each infoHash against the debrid provider's **instant availability** API
6. Caches results to SQLite
7. Rotates old entries out

### Nuvio Already Has the Infrastructure

| What's Needed | Where It Exists in Nuvio |
|---------------|-------------------------|
| Debrid API clients (TorBox, Premiumize, Real-Debrid) | `features/debrid/DebridApiClients.kt` |
| Torrent magnet building | `features/debrid/DebridMagnetBuilder.kt` |
| Stream formatting | `features/debrid/DebridStreamFormatter.kt` |
| Direct debrid resolution | `features/debrid/DirectDebridResolver.kt` |
| P2P streaming engine | `features/p2p/P2pStreamingEngine.kt` |
| Stream model with all fields | `features/streams/StreamModels.kt` |
| TorBox cloud library | `features/cloud/TorboxCloudLibraryProviderApi.kt` |
| MagNutz torrent engine | `features/magnutz/MagNutzRepository.kt` (expect) + `.android.kt` (actual) |

### Wiring Fix for DeezeNutz

#### Replace `simulateSearch()` with real debrid search:

```kotlin
// File: composeApp/src/commonMain/.../features/hub/DeezeNutzViewModel.kt

/**
 * Real search using Nuvio's existing debrid infrastructure.
 * 
 * Pattern matches Debrify's approach:
 * 1. Normalize keywords into search queries
 * 2. Search each configured debrid provider's cloud for matching torrents
 * 3. Also search via MagNutzRepository for fresh torrent indexer results
 * 4. Filter results by quality/size/seeds/year from the recipe
 * 5. Check instant availability on the configured debrid provider
 * 6. Return deduplicated, sorted ChannelItem list
 */
private suspend fun realSearch(recipe: DeezeRecipeExport): List<ChannelItem> {
    val keywords = recipe.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    
    // 1. Search debrid provider clouds via existing API clients
    val cloudResults = searchDebridClouds(keywords, recipe)
    
    // 2. Search torrent indexers via MagNutz
    val indexerResults = if (cloudResults.size < recipe.maxQueueItems) {
        searchTorrentIndexers(keywords, recipe)
    } else emptyList()
    
    // 3. Merge, deduplicate, filter, and sort
    return (cloudResults + indexerResults)
        .distinctBy { it.torrentHash }
        .filter { passesFilters(it, recipe) }
        .sortedByDescending { it.seeds }
        .take(recipe.maxQueueItems.coerceIn(5, 100))
}

private suspend fun searchDebridClouds(
    keywords: List<String>,
    recipe: DeezeRecipeExport,
): List<ChannelItem> {
    val results = mutableListOf<ChannelItem>()
    
    when (recipe.provider) {
        "torbox" -> {
            // Use existing TorboxCloudLibraryProviderApi
            val library = TorboxCloudLibraryProviderApi.getTorrentList()
            for (torrent in library) {
                val matches = keywords.any { 
                    torrent.name.contains(it, ignoreCase = true) 
                }
                if (matches) {
                    // Check instant availability
                    val check = DirectDebridPlaybackResolver.checkInstantAvailability(
                        infoHash = torrent.hash,
                    )
                    results.add(ChannelItem(
                        title = torrent.name,
                        torrentHash = torrent.hash,
                        fileIndex = torrent.fileIndex,
                        magnetUri = "magnet:?xt=urn:btih:${torrent.hash}",
                        seeds = torrent.seeds ?: 0,
                        sizeBytes = torrent.size ?: 0L,
                        isPlayable = check.isAvailable,
                        isBlocked = !check.isAvailable,
                    ))
                }
            }
        }
        "premiumize" -> {
            // Use Premiumize API client 
            val transfers = PremiumizeApiClient.getTransfers()
            for (t in transfers) {
                // ... similar pattern
            }
        }
        "real_debrid" -> {
            // Use Real-Debrid API client
            val torrents = RealDebridApiClient.getUserTorrents()
            for (t in torrents) {
                // ... similar pattern
            }
        }
    }
    
    return results
}

private suspend fun searchTorrentIndexers(
    keywords: List<String>,
    recipe: DeezeRecipeExport,
): List<ChannelItem> {
    // Use MagNutzRepository which already has TorrServer integration
    val query = keywords.joinToString(" ")
    val magnetResults = MagNutzRepository.search(query)
    
    return magnetResults.mapNotNull { magnet ->
        // Check if the debrid provider already has this cached
        val check = DirectDebridPlaybackResolver.checkInstantAvailability(
            infoHash = magnet.infoHash,
        )
        ChannelItem(
            title = magnet.name,
            torrentHash = magnet.infoHash,
            fileIndex = magnet.fileIndex ?: 0,
            magnetUri = magnet.magnetUri,
            seeds = magnet.seeds ?: 0,
            sizeBytes = magnet.sizeBytes ?: 0L,
            isPlayable = check.isAvailable,
            isBlocked = !check.isAvailable,
        )
    }
}

private fun passesFilters(item: ChannelItem, recipe: DeezeRecipeExport): Boolean {
    val filters = recipe.filters
    
    // Quality filter (parse from title or size)
    if (filters.quality != "Any") {
        val qualityOk = when (filters.quality) {
            "4K" -> item.sizeBytes >= 20_000_000_000L  // ~20GB+
            "1080p" -> item.sizeBytes >= 4_000_000_000L  // ~4GB+
            "720p" -> item.sizeBytes >= 1_500_000_000L   // ~1.5GB+
            else -> true
        }
        if (!qualityOk) return false
    }
    
    // Size filter
    val sizeGb = item.sizeBytes / 1_073_741_824.0
    if (sizeGb < filters.minSize || sizeGb > filters.maxSize) return false
    
    // Seed filter
    if (item.seeds < filters.minSeeds) return false
    
    return true
}
```

#### Fix DeezeNutzStore Persistence:

```kotlin
// File: composeApp/src/commonMain/.../features/hub/DeezeNutzStore.kt

// Replace the in-memory _channels with DataStore/Settings persistence
// Pattern from other Nuvio stores (e.g., MultiWindowStorage, IptvStorage)

object DeezeNutzStore {
    // Use the existing storage infrastructure
    private const val STORAGE_KEY = "deeze_nutz_channels"
    
    fun loadChannels(): List<DeezeChannel> {
        // Use existing DebridSettingsStorage or IptvStorage pattern
        // These use DataStore/settings persistence
        val json = DebridSettingsStorage.getString(STORAGE_KEY) ?: return emptyList()
        return try {
            deezeJson.decodeFromString<List<DeezeChannel>>(json)
        } catch (_: Exception) { emptyList() }
    }
    
    fun saveChannels(channels: List<DeezeChannel>) {
        val json = deezeJson.encodeToString(channels)
        DebridSettingsStorage.setString(STORAGE_KEY, json)
    }
    
    // ... rest stays the same but now persists
}
```

#### Wire `addChannel` to call `realSearch` instead of `simulateSearch`:

```kotlin
// In DeezeNutzViewModel.kt — replace the addChannel method
fun addChannel(recipe: DeezeRecipeExport) {
    scope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val channelId = nextId()
            val realItems = realSearch(recipe)  // ← was simulateSearch()
            val channel = DeezeChannel(
                id = channelId,
                name = recipe.name,
                keywords = recipe.keywords,
                debridProvider = recipe.provider,
                qualityFilter = recipe.filters.quality,
                // ... rest of fields
                queue = realItems,
            )
            DeezeNutzStore.addChannel(channel)
            // ...
        }
    }
}
```

---

## 3. Other Broken Features (from execution_plan.md)

### 3.1 MagNutz — Torrent Engine (execution plan 1.2)

**Current state:** `MagNutzRepository` is `expect/actual` — the Android `actual` exists at `composeApp/src/androidMain/.../features/magnutz/MagNutzRepository.android.kt` but uses TorrServer, not a real BitTorrent engine.

**Fix (Option A — libtorrent4j from LibreTorrent):**

```kotlin
// File: composeApp/build.gradle.kts — add dependency
dependencies {
    androidFullImplementation("org.libtorrent4j:libtorrent4j:1.3.0-20")
}
```

Nuvio already has LibreTorrent source at `/Users/robbdeeze/Documents/projects/Nuv MObile/libretorrent-master/`. The libtorrent4j pattern:

```kotlin
// File: composeApp/src/androidMain/.../features/magnutz/MagNutzRepository.android.kt
actual object MagNutzRepository {
    private val session = SessionManager()
    
    actual fun addTorrent(magnetUri: String): Boolean {
        val params = AddTorrentParams(magnetUri)
        val handle = session.addTorrent(params)
        return handle.isValid
    }
    
    actual fun getTorrents(): List<MagNutzItem> {
        return session.torrents.map { handle ->
            val status = handle.status()
            MagNutzItem(
                infoHash = status.infoHash.toString(),
                name = status.name,
                progress = status.progress,
                speed = status.downloadRate,
                status = when (status) {
                    is TorrentStatus.Downloading -> MagNutzStatus.Downloading
                    is TorrentStatus.Seeding -> MagNutzStatus.Seeding
                    is TorrentStatus.Finished -> MagNutzStatus.Completed
                    else -> MagNutzStatus.Paused
                }
            )
        }
    }
}
```

### 3.2 TeleNutz — TDLib Init Order (execution plan 1.1)

**Current state:** `TelegramTdEngine.kt` creates the TDLib `Client` before `SetTdlibParameters` is sent.

**Fix:** Already documented in execution_plan.md. Key change:

```kotlin
// File: composeApp/src/androidMain/.../features/hub/TelegramTdEngine.kt

// Current (broken):
// Client.create(clientHandler)  ← called too early

// Fix — wait for authorizationStateWaitTdlibParameters first:
override fun onUpdate(update: TdApi.Update) {
    when (update) {
        is TdApi.UpdateAuthorizationState -> {
            when (update.authorizationState) {
                is TdApi.AuthorizationStateWaitTdlibParameters -> {
                    val params = TdApi.SetTdlibParameters(
                        useTestDc = false,
                        databaseDirectory = "${filesDir}/tdlib",
                        apiId = TelegramConfig.API_ID,
                        apiHash = TelegramConfig.API_HASH,
                        systemLanguageCode = "en",
                        deviceModel = Build.MODEL,
                        systemVersion = Build.VERSION.RELEASE,
                        applicationVersion = BuildConfig.VERSION_NAME,
                    )
                    client.send(params, this)
                }
                // ... handle other auth states
            }
        }
    }
}

// Client.create() is already done in init — just ensure the handler
// sends SetTdlibParameters on the WaitTdlibParameters state
```

### 3.3 MusicNutz — Playlist Population (execution plan 1.3)

**Current state:** `MusicNutzStore.kt` uses in-memory `mutableStateListOf` with no persistence, and `addAlbumToPlaylist()` may not iterate all tracks.

**Fix pattern from Debrify** — Debrify uses `sqflite` for persistent storage. For Nuvio, use DataStore or the existing `MusicNutzStorage` expect/actual. The key is ensuring `mutableStateListOf` triggers recomposition by replacing the list reference:

```kotlin
// Fix in MusicNutzStore.kt:
fun addAlbumToPlaylist(playlistId: String, album: Album) {
    val playlist = playlists.find { it.id == playlistId } ?: return
    val trackIds = album.tracks.map { it.id }
    val newTrackIds = playlist.trackIds.toMutableList()  // new list reference
    newTrackIds.addAll(trackIds)
    val index = playlists.indexOf(playlist)
    playlists[index] = playlist.copy(trackIds = newTrackIds)  // replace, don't mutate
    persistPlaylists()  // save to storage
}
```

---

## 4. Data Flow Maps

### CinematicTuner — Corrected Data Flow

```
[AddonRepository] ──manifests──▶ [CinematicTunerViewModel]
                                      │
                                      ▼
                              fetchCatalogPage()
                              (returns MetaPreview list)
                                      │
                                      ▼
                              User presses Play
                                      │
                                      ▼
                              getStreamingAddons()
                              (from AddonRepository)
                                      │
                              ┌───────┼───────┐
                              ▼       ▼       ▼
                       Addon 1   Addon 2   Addon 3
                       /stream/  /stream/  /stream/
                              │
                              ▼
                     [StreamItem objects]
                      (infoHash, magnetUri, etc.)
                              │
                              ▼
                  [DirectDebridPlaybackResolver]
                      resolveToPlayableStream()
                              │
                              ▼
                   [PlayerLaunch with resolved URL]
                              │
                              ▼
                     onPlayChannel(playerLaunch)
                              │
                              ▼
                      [PlayerScreen / ExoPlayer]
```

### DeezeNutz — Corrected Data Flow

```
[User creates recipe] ──keywords──▶ [DeezeNutzViewModel.realSearch()]
                                          │
                          ┌───────────────┼───────────────┐
                          ▼               ▼               ▼
                 [Debrid Clouds]    [Torrent Indexers]  [MagNutz]
                 TorBox/Premiumize  Jackett/Prowlarr    TorrServer
                          │               │               │
                          └───────────────┼───────────────┘
                                          ▼
                              [DirectDebridPlaybackResolver]
                              checkInstantAvailability(infoHash)
                                          │
                                          ▼
                                 [ChannelItem list]
                              filtered by recipe quality/size/seeds
                                          │
                                          ▼
                                 [DeezeNutzStore]
                              (persisted via DataStore/Settings)
                                          │
                                          ▼
                              User taps Play ──▶ PlayerLaunch
                              (uses same DirectDebridPlaybackResolver
                               to get final streaming URL)
```

---

## 5. Priority Order for Fixes

| Priority | Feature | Effort | Why | Key Files |
|----------|---------|--------|-----|-----------|
| **P0** | CinematicTuner — Wire stream resolution | Medium | Core feature is a visual shell; add-on catalogs fetch but never play | `CinematicTunerViewModel.kt`, `CinematicTunerScreen.kt` |
| **P0** | DeezeNutz — Replace `simulateSearch` with real debrid/torrent search | Medium | Entirely fake data, channels produce non-playable items | `DeezeNutzViewModel.kt`, `DeezeNutzStore.kt` |
| **P0** | TeleNutz — Fix TDLib init order | Medium | App crashes on launch per execution plan | `TelegramTdEngine.kt`, `TelegramConfig.kt` |
| **P1** | MagNutz — Wire real BitTorrent engine (libtorrent4j) | Large | `expect` has no real `actual`- currently TorrServer only | `MagNutzRepository.android.kt`, `build.gradle.kts` |
| **P1** | DeezeNutz — Add persistence | Small | Channels disappear on restart | `DeezeNutzStore.kt` |
| **P2** | MusicNutz — Fix playlist persistence | Small | Playlists vanish on restart | `MusicNutzStore.kt` |
| **P2** | VidNutz — Add loading animations | Small | Missing polish per execution plan | `VidNutzScreen.kt` |

---

## 6. Key Files Reference (Nuvio_Robbdeeze)

| File | Purpose |
|------|---------|
| `features/hub/CinematicTunerViewModel.kt` | Stremio TV VM — needs stream resolution wiring |
| `features/hub/CinematicTunerScreen.kt` | Stremio TV UI — needs play button to call VM |
| `features/hub/DeezeNutzViewModel.kt` | Debrify TV VM — needs real search, remove fake data |
| `features/hub/DeezeNutzStore.kt` | Debrify TV store — needs persistence |
| `features/hub/DeezeNutzModels.kt` | Debrify TV data models |
| `features/hub/DeezeNutzCacheService.kt` | Played-item tracking cache |
| `features/addons/AddonRepository.kt` | Stremio addon manifest management |
| `features/addons/ManagedAddon.kt` | Addon model with manifest, catalogs, streams |
| `features/catalog/fetchCatalog.kt` | Catalog page fetching |
| `features/debrid/DebridApiClients.kt` | Debrid provider API clients (TorBox, Premiumize, Real-Debrid) |
| `features/debrid/DirectDebridResolver.kt` | Direct debrid stream resolution |
| `features/debrid/DirectDebridStreamPreparer.kt` | Stream preparation pipeline |
| `features/player/PlayerScreenRuntimeSourceActions.kt` | `resolveDebridForPlayer` — the missing bridge between streams and player |
| `features/player/PlayerModels.kt` | `PlayerLaunch` with `torrentInfoHash`, `torrentFileIdx` |
| `features/streams/StreamModels.kt` | `StreamItem` with full torrent/debrid fields |
| `features/magnutz/MagNutzRepository.kt` | Torrent engine expect (Android actual needs libtorrent4j) |
| `features/p2p/P2pStreamingEngine.kt` | P2P/TorrServer streaming |
| `features/hub/TelegramTdEngine.kt` | TDLib init (broken order) |
| `features/hub/MusicNutzStore.kt` | Music playlist store (needs persistence fix) |
| `features/hub/VidNutzScreen.kt` | YouTube video screen (needs loading animations) |

---

## 7. Summary of Debrify Patterns to Port

| Debrify Pattern | Debrify Location | Nuvio Equivalent | What to Wire |
|----------------|-----------------|------------------|-------------|
| Stream search from addons | `StremioService.searchStreams()` at `lib/services/stremio_service.dart:491` | `AddonRepository` + `fetchCatalogPage` → need new `/stream/` endpoint fetcher | Call streaming addons' `/stream/{type}/{id}.json` endpoints, filter by content ID prefix |
| Stream → Torrent conversion | `StremioService._convertToTorrents()` at `lib/services/stremio_service.dart:1157` | `StreamItem` model in `features/streams/StreamModels.kt` | Already exists — just need to populate from addon stream responses |
| Debrid resolution pipeline | Various `{provider}_service.dart` files | `DirectDebridPlaybackResolver` in `features/debrid/` | Already exists — plug into `playCurrentItem()` |
| Keyword → debrid cloud search | `DebrifyTvCacheService` in `lib/services/debrify_tv_cache_service.dart` | `DebridApiClients` + `TorboxCloudLibraryProviderApi` | Replace `simulateSearch()` with actual API calls |
| Quality/size/seeds filtering | `debrify_tv_filters.dart` filters | `DeezeRecipeFilters` already defined | Models exist — just wire to real data |
| SQLite caching | `debrify_tv_database.dart`, `tv_cached_torrents` table | Use DataStore/SharedPreferences | Persist `DeezeChannel.queue` to storage |
| RD-blocked torrent filtering | `rd_blocked_filter.dart` | `LocalDebridAvailabilityService` | Already exists — populate `isBlocked` from `checkInstantAvailability()` |
