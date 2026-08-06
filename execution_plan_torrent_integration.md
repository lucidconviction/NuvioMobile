# Execution Plan: Torrent Search Integration & Live Games Fix

## Current Problems

### 1. Live Games Overlay — Crash & Missing Channel Options
- **Force close on exit**: The `LaunchedEffect` with `while(true)` in `PlayerPlaybackOverlays.kt` continues running after dismiss, potentially causing state corruption when the user taps the background
- **No channel options shown**: `SportsNowStore.onSwitchToEvent` may be null if `SportsRepository.selectEvent()` hasn't been wired, causing taps to silently do nothing
- **NCH overlay conflict**: The new NCH channel surf overlay may overlap with the Live Games overlay, both using `Box(Modifier.fillMaxSize())`

### 2. DeezeNutz — No Real Torrent Search
- `realSearch()` only calls `searchViaAddons()` which fetches catalog items from Stremio addons — this returns metadata (posters, titles), not actual playable torrents
- No torrent indexer search, so channels have no real content to play
- `playChannelItem()` tries to resolve via `DirectDebridPlaybackResolver` but passes catalog item IDs as `infoHash`, which debrid can't resolve

### 3. MagNutz — TorrServer Only, No Native BitTorrent
- MagNutz uses TorrServer (an external binary) for torrent management
- TorrServer has reliability issues, no DHT support, crashes on some Android versions
- No searching capability

---

## Proposed Solution

### Library Assessment

| Library | What It Provides | Integration Effort | Risk |
|---------|-----------------|-------------------|------|
| **TorrentSearch** (prajwalch) | Multi-tracker torrent search API (1337x, PirateBay, RARBG, etc.) via predefined engines | Medium — pure Kotlin, just add dependency + API calls | Low — well-maintained, simple API |
| **libretorrent** (proninyaroslav) | Full BitTorrent engine (DHT, magnet links, sequential download for streaming) | High — requires Android service, notification integration, storage management | Medium — large library, Android-specific |

### Recommended Approach

Replace TorrServer with libretorrent engine AND add TorrentSearch for discovery:

```
New Flow:
User enters keywords → TorrentSearch queries 1337x/TBP/RARBG/etc
    → Returns magnet links + metadata (seeds, size, name)
    → DeezeNutz stores as ChannelItem with real magnetUri + infoHash
    → User taps Play → MagNutz (libretorrent) starts sequential download
    → DirectDebridPlaybackResolver checks RD/PM/Torbox first
    → If not cached, streams via libretorrent sequential mode
    → Player receives progressive stream URL
```

---

## Phase 1: Fix Live Games Overlay (P0)

### Changes

**File: `PlayerPlaybackOverlays.kt`**

1. Fix `onDismissLiveGames` not working:
   - Add `onDismissLiveGames` to the NCH overlay's background click to prevent both overlays showing simultaneously
   - Move `pickerEvent` state reset into `onDismissLiveGames`
   - Wrap the `while(true)` LaunchedEffect with `isActive` check

2. Fix channel options not showing:
   - Add non-null assertion/fallback for `SportsNowStore.onSwitchToEvent`
   - If no `onSwitchToEvent`, fall back to calling `onPlayChannel` with an IPTV `PlayerLaunch`

**Risk:** Low — self-contained changes to existing overlay

---

## Phase 2: Add TorrentSearch to DeezeNutz (P1)

### Steps

#### 2a. Add Dependency
**File: `composeApp/build.gradle.kts`**
```kotlin
dependencies {
    // Add TorrentSearch library
    implementation("com.github.prajwalch:TorrentSearch:1.0.0")
    // or use JitPack
    implementation("com.github.prajwalch.TorrentSearch:torrentsearch:main-SNAPSHOT")
}
```

**File: `settings.gradle.kts`**
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### 2b. Create TorrentSearch Wrapper
**New file: `composeApp/src/commonMain/.../features/hub/DeezeNutzTorrentSearch.kt`**

```kotlin
object DeezeNutzTorrentSearch {
    private val engines = listOf(
        TorrentSearch.Engine.THEPIRATEBAY,
        TorrentSearch.Engine.RARBG,
        TorrentSearch.Engine.LEETX,
        TorrentSearch.Engine.YTS,
        TorrentSearch.Engine.EZTV,
        TorrentSearch.Engine.TORRENTDOWNLOAD,
    )

    suspend fun search(query: String, limit: Int = 30): List<ChannelItem> {
        val results = mutableListOf<ChannelItem>()
        for (engine in engines.take(3)) {  // search first 3 engines in parallel
            try {
                val items = engine.search(query)
                for (item in items.take(10)) {
                    if (item.magnetUri != null) {
                        results.add(ChannelItem(
                            title = item.name,
                            torrentHash = extractInfoHash(item.magnetUri),
                            fileIndex = 0,
                            magnetUri = item.magnetUri,
                            seeds = item.seeds ?: 0,
                            sizeBytes = item.size ?: 0L,
                            isPlayable = item.magnetUri != null,
                            isBlocked = false,
                        ))
                    }
                }
            } catch (_: Exception) {}
        }
        return results.sortedByDescending { it.seeds }.take(limit)
    }

    private fun extractInfoHash(magnet: String): String {
        val regex = Regex("btih:([a-fA-F0-9]{40})")
        return regex.find(magnet)?.groupValues?.getOrNull(1) ?: ""
    }
}
```

#### 2c. Update DeezeNutzViewModel
**File: `DeezeNutzViewModel.kt`**
- Replace `searchViaAddons()` in `realSearch()` with `DeezeNutzTorrentSearch.search()`
- Keep `searchViaAddons()` as fallback for metadata

#### 2d. Update playChannelItem
- Update to use real torrent hashes from TorrentSearch, which `DirectDebridPlaybackResolver` can actually resolve

**Risk:** Medium — TorrentSearch may need custom engine config; some engines may be blocked by region

---

## Phase 3: Integrate libretorrent Engine (P1)

### Steps

#### 3a. Add libretorrent Dependency
**File: `composeApp/build.gradle.kts`**
```kotlin
dependencies {
    implementation("com.github.proninyaroslav:libretorrent:1.0.0")
}
```

#### 3b. Create MagNutzEngine Wrapper
**New file: `composeApp/src/androidMain/.../features/magnutz/MagNutzEngine.kt`**

```kotlin
class MagNutzEngine(private val context: Context) {
    private var session: TorrentSession? = null

    fun start() {
        session = TorrentSession(context, TorrentSessionConfig(
            downloadDir = File(context.filesDir, "magnutz/downloads"),
            maxActiveDownloads = 3,
            enableDht = true,
        ))
    }

    suspend fun addMagnet(magnet: String): TorrentHandle? {
        val params = AddTorrentParams(magnet)
        params.sequentialDownload = true  // enable streaming
        return session?.addTorrent(params)
    }

    suspend fun addTorrentFile(file: File): TorrentHandle? {
        val params = AddTorrentParams(file)
        return session?.addTorrent(params)
    }

    fun getStreamUrl(handle: TorrentHandle): String? {
        // Returns file:// URL for the first video file once enough is downloaded
        return handle.firstVideoFile?.let { "file://${it.absolutePath}" }
    }

    fun getProgress(handle: TorrentHandle): Float = handle.progress

    suspend fun stop() {
        session?.stop()
    }
}
```

#### 3c. Update MagNutzRepository
**File: `MagNutzRepository.android.kt`**
- Replace TorrServer calls with libretorrent `MagNutzEngine` calls
- Keep same public API (`addMagnet`, `pauseDownload`, `resumeDownload`, etc.)
- Add `search()` method that uses TorrentSearch library

**Risk:** High — libretorrent is a large library; sequential download mode may not work for all torrents; streaming requires sufficient download speed

---

## Phase 4: Wire DeezeNutz + MagNutz (P2)

### Changes

**File: `DeezeNutzViewModel.kt`**
- `playChannelItem()` calls `MagNutzRepository.addMagnet()` if `DirectDebridPlaybackResolver` fails
- Waits for `MagNutzRepository` to buffer enough data, then plays from local file

**File: `MagNutzRepository.android.kt`**
- Expose `streamTorrent(magnetUri: String, onProgress: (Float) -> Unit): String?` that starts sequential download and returns a streaming URL when buffer is ready

---

## Phase 5: Add RealMagnet Search to DeezeNutz UI (P2)

- Add "Use Torrents" toggle in recipe editor
- When enabled, `realSearch()` prioritizes torrent search results over addon catalogs
- Show seed count and size in channel queue items
- Allow users to select multiple torrent sources (cached on debrid + fresh torrent search)

---

## Priority & Effort

| Phase | Description | Effort | Priority | Depends On |
|-------|-------------|--------|----------|------------|
| 1 | Fix Live Games overlay | Small | P0 (crash) | None |
| 2 | Add TorrentSearch to DeezeNutz | Medium | P1 (feature) | None |
| 3 | Integrate libretorrent engine | Large | P1 (feature) | Phase 2 |
| 4 | Wire DeezeNutz + MagNutz | Medium | P2 | Phase 2, 3 |
| 5 | UI for torrent search | Medium | P2 | Phase 2 |

---

## Immediate Fix (Before Plan Approval)

The Live Games overlay crash — quick fix:

**File: `PlayerPlaybackOverlays.kt`**
1. Add `onDismissChannelSurf` call to the NCH overlay's background click to prevent overlap
2. The crash on exit is from the `while(true)` loop in `LaunchedEffect(showLiveGamesOverlay)` — need to check `isActive` and add a try/catch around the `onDismissLiveGames?.invoke()` call at line 264

**File: `PlayerScreenRuntimeUi.kt`**
3. Ensure `SportsNowStore.onSwitchToEvent` is initialized in the player screen runtime effects

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| TorrentSearch engines get blocked/change domains | Abstract engine list into config, add fallback engines |
| libretorrent sequential download is slow | Start download on item selection, show progress, play once buffered |
| libretorrent APK size increase (~15MB) | Only include required ABIs via `jniFilters` |
| Legal concerns with torrent search | Make it opt-in via settings toggle; clearly label "Torrent Search" in UI |
| Android storage permissions | Use app-specific directory (`filesDir/magnutz`) to avoid SAF/permissions |
