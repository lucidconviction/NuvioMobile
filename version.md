# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

### v0.7.1 — ProGuard/R8 Fix for Full Release Build (July 2026)

#### Build Fixes
- **R8 missing classes fix** — Added `-dontwarn` rules for `java.beans.*` and `javax.script.ScriptEngineFactory` in `composeApp/proguard-rules.pro` to suppress R8 warnings when minifying the full release APK (Mozilla Rhino JavaScript engine references JVM-only classes not available on Android).

#### Latest Debug Build
```bash
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

To build release, set up release keystore in `local.properties`:
```
NUVIO_RELEASE_STORE_FILE=path/to/keystore.jks
NUVIO_RELEASE_STORE_PASSWORD=...
NUVIO_RELEASE_KEY_ALIAS=...
NUVIO_RELEASE_KEY_PASSWORD=...
```

### v0.7.0 — TeleNutz ExoPlayer Streaming, Auto-Next Queue, Persistent Search & SportNutz League Drawers (July 2026)

#### TeleNutz — Video Search, Auto-Play-Next & Persistent Results (`TeleNutzScreen.kt`, `TeleNutzRepository.kt`, `PlayerScreenRuntimeEffects.kt`)
- **ExoPlayer Video Streaming & Auto-Play-Next** — TeleNutz videos play directly inside the internal ExoPlayer. Automatically passes `autoPlayQueueUrls`, `autoPlayQueueTitles`, and `autoPlayQueueIndex` from search results, bookmarks, or downloads lists.
- **On-Demand Telegram Stream Resolution** — `PlayerScreenRuntimeEffects.kt` detects `telenutz://` queue URLs when advancing to the next video, downloading/resolving Telegram media on demand via TDLib before playback.
- **Persistent Search State** — `TeleNutzRepository` retains `lastSearchQuery` and `lastSearchResults`, preserving search results across tab switches and navigation until a new search is performed.
- **Bookmarks & Downloads** — Bookmark toggles, local file caching, download progress tracking, and file deletion.

#### SportNutz — Collapsible League Drawers & Text Cards (`SportsScreen.kt`, `GameToChannelMatcher.kt`)
- **Categorized Collapsible Drawers** — In the "ALL LEAGUES" tab, sports events are organized in expandable/collapsible drawers per league with match counts.
- **Clean Text Cards in Portrait View** — Upcoming/Live event cards in portrait view are simplified to clean text-only layouts showing team names, scores, and channel info.
- **Enhanced Broadcaster Channel Matcher** — Expanded league-to-channel matching logic to suggest relevant sports channels and reduce non-sports channel false matches.

#### MagNutz — Verification & Torrents (`MagNutzScreen.kt`, `MagNutzRepository.android.kt`)
- **Verified Torrent Operations** — Confirmed magnet parsing, TorrServer background engine integration, pause/resume/cancel actions, progress tracking, and ExoPlayer playback from local storage.

### v0.5.0 — MagNutz Torrents, MusicNutz Playlists/Downloads, MultiNutz Fullscreen, Auto-Play-Next, IPTV Polish (July 2026)

#### MagNutz — Torrent Download Manager (`MagNutzScreen.kt`, `MagNutzRepository.android.kt`)
- **New hub:** MagNutz — paste magnet links, download torrents via TorrServer, browse/add/remove
- **Magnet parsing** — `MagNutzMagnetParser` extracts `btih:`, trackers, display name from any magnet URI
- **TorrServer integration** — `P2pStreamingEngine.startTorrServer()` starts the native binary on demand; magnets submitted via TorrServer HTTP API; stats polling (speed/peers/seeds/progress) every 2s
- **File completion** — auto-detect when download finishes (size match or stalled), saves to user-chosen directory
- **Download management** — pause/resume/cancel per torrent; persistent state via SharedPreferences JSON; Delete button on each card
- **Save location picker** — SAF directory picker (`OpenDocumentTree`) with `takePersistableUriPermission`; Change button in UI
- **External magnet intents** — `<data android:scheme="magnet" />` in manifest; `MainActivity.handleIncomingAppIntent()` routes to MagNutz hub
- **Default trackers** — 9 UDP/HTTPS/WSS trackers appended to magnets without trackers for better peer discovery
- **Phone layout** — search bar + ADD MAGNET button + filter chips + scrollable download list with progress bars
- **Tablet layout** — two-panel: left card grid + right detail panel with full specs + Play/Pause/Remove buttons

#### MusicNutz — Playlists, Downloads, Saved Albums (`MusicNutzModels.kt`, `MusicNutzScreen.kt`, `MusicNutzStore.kt`)
- **Playlist system** — 5th mode chip "Playlists"; create named playlists, add tracks, view/manage/delete
- **Album-to-Playlist** — "Add All to Playlist" button on album detail; fetches album tracks, appends to selected playlist
- **Download system** — "Download" button on each track card; resolves YouTube/Deezer stream via OkHttp, saves to `filesDir/music/`
- **Downloads tab** — view downloaded tracks with album art, play, delete; progress spinner while downloading
- **Saved Albums** — bookmark icon on album cards; "Saved" mode chip shows bookmarked albums without downloading
- **Playback error display** — inline red error banner when `resolveStream()` fails; "Resolving stream..." overlay while loading
- **Auto-play-next for albums** — when playing from album detail view, pre-resolves all remaining track URLs, passes as `autoPlayQueueUrls`

#### MultiNutz — Layout Expansion & Fullscreen (`MultiWindowLayouts.kt`, `MultiWindowGrid.kt`)
- **30+ new window layouts** — Full set per user spec: 3-screen (1×3 columns, 2+1 vertical, 1+2 horizontal), 4-screen (1×4, 4 vert, 1+3, 3+1, 2-1-1, 1-1-2), 5-screen (1×5, 2+3, 1+2×2, 3+2v), 6-screen (1×6, 6 vert, 4+2, 3+3), 7-screen (3×2+1, 2×3+1, 1+3+3, 7 asym), 8-screen (3×2+2, 2×3+1×2, 4+2+1), 9-screen (3×3 center, 3×2+3, 2×3+3)
- **isTablet-aware layout selection** — phone gets orientation-appropriate layouts, tablet (≥600dp) gets full set
- **Fullscreen button per cell** — `⛶` icon at top-right of each VideoCell glass overlay; launches stream in full-screen player
- **Floating MW quick-nav** — translucent 52dp "MW" button at bottom-right when streams active and on different sub-screen
- **Double-tap gesture** — double-tap hub background navigates to MultiNutz when streams active

#### Auto-Play-Next — ExoPlayer Queue (`PlayerLaunch`, `PlayerScreenArgs`, `PlayerScreenRuntimeEffects.kt`)
- **Queue fields** — `autoPlayQueueUrls`, `autoPlayQueueTitles`, `autoPlayQueueIndex` added to `PlayerLaunch`
- **Auto-advance on end** — when ExoPlayer reaches `STATE_ENDED` and queue has remaining items, creates new `PlayerLaunch` with next URL + metadata, stores in `PlayerLaunchStore`, calls `onAutoPlayNext(launchId)` → navigates to fresh `PlayerScreen`
- **NutzScreen queue wiring** — MusicNutz album playback pre-resolves remaining tracks as queue; IPTV/VidNutz/Sports channel lists pass through automatically

#### SportNutz — Cache, New Leagues, Standings Fix (`SportsRepository.kt`, `GameToChannelMatcher.kt`, `SportsScreen.kt`)
- **League event cache** — in-memory cache per league+date combo (60s TTL); switching leagues loads instantly; cache fallback on network errors
- **20 leagues** (up from 8) — added EPL, La Liga, Serie A, Bundesliga, Ligue 1, UCL, F1, Tennis, Golf, CFB, CBB, WNBA, fixed MLS slug
- **Fixed Boxing standings** — slug changed from `boxing/_` (invalid ESPN) to `boxing/boxing`
- **Standings error display** — shows error + Retry button on failure (was silently failing before); ESPN API tries `?season=` first, falls back to no param
- **Expanded channel matcher** — 25+ league mappings with 100+ broadcaster keywords (F1, MotoGP, Tennis, Golf, Cricket, Rugby, NRL, AFL, College sports, soccer leagues per-broadcaster)
- **Upcoming card contrast** — match names/VS text changed from `OnSurfaceVariant` (gray) to `OnSurface` (white) in both phone and TV cards

#### IPTVNutz — Layout Polish (`IptvScreen.kt`)
- **Persistent search bar** — TV mode search bar + header row moved outside scrollable column; stays fixed at top while channels scroll
- **Top margin 5dp** — header padding reduced from 16dp to 5dp
- **Button text contrast fix** — changed `Color.White` to `onPrimary` (dark gray) on `primary` (near-white) backgrounds across source selector tabs, ADD button, CONNECT SOURCE buttons

### v0.6.1 — App Icon Refresh, Collapsible IPTV Groups, All Leagues Fix, VidNutz Pre-Cache (July 2026)

#### App Icon
- **All icons replaced** — Android launcher icons (5 densities × 3 variants), splash logo (1080×1080), and iOS app icon (1024×1024) all replaced with new icon art

#### IPTVNutz — Collapsible M3U Groups (`IptvScreen.kt`)
- **Group headers are now clickable** — each channel group header in mobile mode has an expand/collapse arrow. All groups collapsed by default. Tapping a header toggles that group's channels visibility.

#### SportNutz — All Leagues Fix (`SportsRepository.kt`, `SportsScreen.kt`)
- **ALL LEAGUES now fetches all events** — tapping the "ALL LEAGUES" chip calls `loadAllLiveEvents()` which scans all 20 leagues within a 14-day window. Previously it only showed stale single-league data.

#### VidNutz — Background Pre-Cache (`VidNutzScreen.kt`)
- **Next page pre-fetched** — after loading a category's first page (24 videos), the next page is fetched in the background to warm the cache, so tapping "LOAD MORE" is instant

### v0.6.0 — All Leagues, Channel Auto-Advance, Nav Overlay, Net Smutt Polish (July 2026)

#### SportNutz — All Leagues & Upcoming Cards (`SportsRepository.kt`, `EspnClient.kt`, `GameToChannelMatcher.kt`, `SportsScreen.kt`)
- **ALL LEAGUES button** — now fetches all upcoming events across all sports within 2 weeks (not just live). `EspnClient.fetchAll()` accepts date range `YYYYMMDD-YYYYMMDD`, skips the live-only filter for range queries. `SportsRepository.loadAllLiveEvents()` passes a 14-day window.
- **Upcoming match cards text-only** — removed team logos, VS separator, and SET ALERT button. Now shows time + away team + home team + league name as clean plain text.
- **Expanded channel matcher** — 2× more league-specific keywords per league (espn2, fs1, fs2, tsn, sportsnet, dazn1, dazn2, btn, sec network, acc network, nbcsn, paramount, peacock, prime video). `generalSportsKeywords` expanded from 15 → 40+ entries.

#### Auto-Advance — 5-Second Grace Period (`PlayerScreenRuntimeEffects.kt`)
- **Error-based debounce** — auto-next now waits 5 seconds after the stream ends before advancing. If playback resumes during the window (transient glitch), auto-advance is cancelled. Works across all content types (IPTV, MusicNutz, VidNutz).

#### Prev/Next Nav Overlay (`PlayerScreenRuntimeUi.kt`)
- **Floating ◀/▶ buttons** — two semi-transparent 52dp circle buttons positioned at vertical center of the player. Left edge (prev), right edge (next).
- **Auto-hide** — disappears after 3 seconds of no interaction. Reappears on screen tap alongside controls.
- **Works everywhere** — shows for any content with an auto-play queue (IPTV, MusicNutz, VidNutz). Button state disables at queue boundaries.

#### MusicNutz — Full Queue Support (`MusicNutzScreen.kt`)
- **Complete queue** — `playTrack()` now includes ALL tracks in the auto-play queue (not just remaining), with `autoPlayQueueIndex` set to the current track. Enables prev/next navigation and error-based auto-advance.
- **Grid queue** — playing from the main track grid now uses `uiState.tracks` as the full queue, so auto-advance works from any track, not just album detail.

#### VidNutz — Queue & Auto-Advance (`VidNutzScreen.kt`)
- **Video queue** — when playing a video, the full `displayVideos` list is set as `autoPlayQueueUrls`/`autoPlayQueueTitles` with the current index. Enables prev/next and auto-advance through the current category or search results.

#### VidNutz Load-More Fix (`VidNutzRepository.kt`)
- **Engine cache pagination** — `fetchByCategory` caches 96 results per category from `VideoSuggestionEngine` and paginates through them (24/page) before falling through to search/trending API. Added `EngineCache` data class with `hasMore` awareness.

#### Net Smutt (`VidNutzScreen.kt`)
- **Loading text** — changed from "Scraping videos..." to "Looking for Smutt, hang tight"

#### Full Feature Changelog
- `MagNutzModels.kt` — MagNutzItem, MagNutzStatus, MagNutzFilter, MagNutzMagnetParser
- `MagNutzStorage.kt` — expect/actual persistence (SharedPreferences Android, NSUserDefaults iOS)
- `MagNutzRepository.kt` — expect API surface + Android actual with TorrServer integration
- `MagNutzScreen.kt` — full UI phone/tablet layouts with search, add magnet, progress, pause/resume, delete
- `MusicNutzModels.kt` — MusicNutzPlaylist, MusicDownload, savedAlbums in UiState
- `MusicNutzStore.kt` — MusicNutzPlaylistStore, MusicDownloadStore, MusicNutzSavedAlbumsStore
- `MusicNutzScreen.kt` — 5 mode chips (Tracks/Albums/Playlists/Downloads/Saved), playlists CRUD, downloads with progress, saved albums, album-to-playlist, auto-play-next queue, error display
- `MultiWindowLayouts.kt` — 30+ new enum entries, isTablet-aware getValidLayouts, calculateSlots for each
- `MultiWindowGrid.kt` — onFullscreenCell callback, VideoCell fullscreen button, isTablet passed to layout resolver
- `MultiWindowCellOptions.kt` — onFullscreen parameter, "Fullscreen" row in options sheet
- `MultipleWindowStore.kt` — resolveLayout accepts isTablet
- `PlayerModels.kt` — autoPlayQueueUrls, autoPlayQueueTitles, autoPlayQueueIndex
- `PlayerScreenArgs.kt` — autoPlayQueue fields + onAutoPlayNext callback
- `PlayerScreen.kt` — autoPlayQueue + onAutoPlayNext parameters
- `PlayerScreenRuntimeEffects.kt` — LaunchedEffect for auto-advance on STATE_ENDED with queue
- `App.kt` — auto-play queue wiring in PlayerScreen call, MagnetLink deep link handling
- `P2pStreaming.kt` — startTorrServer() added to expect engine
- `P2pStreamingEngine.android.kt` — startTorrServer() actual (starts binary without stream)
- `P2pStreamingEngine.ios.kt` — startTorrServer() stub
- `MagNutzRepository.android.kt` — calls startTorrServer() before adding torrents, default trackers appended to magnets
- `MainActivity.kt` — MagNutzRepository.initialize(), MusicNutzDownloadStorage.downloadDirPath, magnet intent handling
- `AndroidManifest.xml` — magnet: scheme intent filter
- `AppUrlBridge.kt` — MagnetLink deep link type
- `RobbdeezeNutzHubScreen.kt` — MagNutz in HubSubScreen enum/hubItems/when branch/HubReturnStore
- `SportsRepository.kt` — leagueCache, 20 leagues, CACHE_TTL_MS, forceRefresh param, clearLeagueCache()
- `GameToChannelMatcher.kt` — 25+ league mappings with 100+ keywords
- `EspnClient.kt` — fetchStandings tries ?season= first, falls back to no param
- `IptvScreen.kt` — persistent search bar (TV mode), 5dp top padding, Color.White → onPrimary fix

### v0.4.0 — PortalNutz, DaddyLive Sports, VideoSuggestionEngine, VidNutz/Sports Enhancements (July 2026)
- **PortalNutz (Feature 2)** — 4th "PORTAL" tab in AddSourceBottomSheet. Scrapes Xtream credentials from GitHub/Telegram/AMZ, verifies via player_api.php, returns top 5. Sheet stays open for multi-add with reactive ADDED labels. Uses existing Xtream account flow.
- **DaddyLiveClient (Feature 3)** — fetches live/upcoming sports events from daddylive mirrors (3 parallel, 6s timeout). ET→local time conversion. Integrated into SportsScreen mobile + TV as collapsible "Sports Now/Later" section showing channel names, LIVE/UPCOMING badges, channel picker for multiple matches.
- **VideoSuggestionEngine (Feature 4)** — query rotation per 12 categories + seen-ID dedup + random affixes. KMP-safe with Mutex. Integrated into VidNutzRepository for first-page category variety.
- **SportNutz Performance (Feature 5)** — parallel async fetches, withTimeout on all HTTP (6-20s), cached trending videos (reduced 16→4 queries), skip re-fetch if cached.
- **VidNutz Enhancement (Feature 6)** — fetchByCategory uses VideoSuggestionEngine for first-page results before falling back to Invidious/Piped.
- **Player Live Games Overlay (Feature 9)** — existing overlay expanded to show both ESPN DaddyLive events with LIVE/UPCOMING badges and channel names. SportsNowStore.daddyLiveEvents auto-populated.
- **Playlist source selection fix** — clicking any playlist card now selects that source (selectSource instead of toggleSourceSelection). Works in both TV and mobile modes.
- **Playlist cards clickable** — TvPlaylistCard, PlaylistCard, MobilePlaylistRow all accept onClick to select source and show its channels.
- **Performance fixes** — refreshXtreamChannels uses User-Agent VLC/3.0.20 + get_live_streams fallback. DaddyLive mirrors probed in parallel. Trending news reduced to 4 parallel queries with 8s timeout per query.
- **Error display** — uiState.error now visible in IPTV screen TV + mobile modes.

### v0.3.0 — Backup/Restore, Hub Header Tuning, Multiwindow Fixes (July 2026)
- **Backup & Restore** — export/import all profiles, library, watched status, progress, collections, IPTV, settings, auth, identity via clipboard or file picker
- **NuvioSync backup import** — import `.json` backups from the original NuvioSync format (library, watched, progress, addons) with automatic conversion
- **Hub header tunings** — smaller compact headers (14sp, reduced padding), consistent across all 4 hubs, persistent back buttons, translucent hub cards
- **VidNutz randomization** — each category uses random page offset + rotating search query variants so different videos show per category
- **SportNutz grid** — changed live events to 4-column grid, aggregates trending news across 16 sports queries
- **MusicNutz randomization** — each genre category uses random page offset + query variants
- **D-pad focusable** — `NuvioPosterCard` (home catalog), `ChannelCard` (IPTV) all focusable with scale + border highlight for TV remote
- **IPTV search toggle** — search bar hidden by default, tap search icon to show
- **Multiwindow crash fix** — removed `!!` force-unwraps in slot picker, try-catch around `addToSlot`
- **Multiwindow auto-layout** — layout no longer locks when adding streams (stays auto by default)
- **Spacing** — added 20dp between header and grid in hub screen, 32dp between header and pills in SportNutz
- **File picker** — "Pick File" button in Backup → Import NuvioSync opens Android file picker for `.json` files
- **Restart reminder** — restore success message now says "Restart app to see changes"
- **Upstream cherry-picks** — `60cde302` (disable predictive back), `fe1bc880` (triage automation), `a6e19eab` (version bump). Skipped `cc593875` (conflicts with custom AppUpdater/Settings), `0f155c75` (Italian strings conflict), `409a2e9b` (already removed in our codebase)

## Build Commands
```bash
./gradlew :androidApp:assembleDebug -Pnuvio.android.distribution=full
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## IPTV Progress

### Phase 1 — Core
IPTV tab, M3U/Xtream/EPG support, channel playback

### Phase 2 — Polish
Category chips, EPG source management bottom sheet, ExoPlayer low-latency live buffering

### Phase 3 — UX Overhaul
Removed Live Guide, EPG inline on cards, search bar, multi-source toggle chips, favorites (persisted heart toggles), collapsible playlists, iptv-org preset (7000+ channels), true black bg, title "IPTV", fixed ADD button touch interception

### Phase 4 — In-Player Channel Overlay
Ghost CH button (transparent bg, accent on tap), slide-up channel list with all playlist channels, auto-scrolls to currently playing, close via backdrop tap / X button / 30s timeout, see-through list (alpha 0.3), channel logos per row (numbered fallback), one-tap channel switching via new PlayerLaunch + nav replace

### Phase 5 — Overlay Search, Favorites, Collapsible Grids & EPG Fix
- Search bar in channel overlay, favorites section pinned at top, heart toggle on each row
- Collapsible channel grid + favorites grid (clickable headers with arrows)
- Auto-refresh EPG after loading M3U channels
- `channelIds`, `iptvFavoriteIds`, `onToggleIptvFavorite` wired through args

### Phase 6 — Popup Menu, Real Search, Half-Height, Channel History
- Popup menu on CH button (List/Favorites/History options), 30s auto-dismiss timeout
- Real search input (OutlinedTextField), half-screen height (fillMaxHeight 0.5f)
- Channel history (last 15, persisted), favorites mode, hearts in all modes

### Phase 7 — Group Headers, Source Badges, EPG State, Popup Timeout, Grid Fix
- Group/category headers, channel count badges, EPG empty state, popup timeout, grid collapse fix, refresh spinner, popup transparency

### Phase 8 — Logo Switch Fix, EPG Name Matching, Brace Fix
- Logo fix, EPG name matching, EPG match count, error logging, brace fix

### Phase 9 — Stalker Portal Support & Full-Screen Overlay
- Stalker Portal as 4th source type (XTREME / M3U / EPG / STALKER), full-screen overlay

### Phase 10 — Sports Integration, History Bar, Full-Bleed Logos, Timeouts
- **Live Sports section** — fetches today's events from TheSportsDB free API (`eventsday.php`), matches broadcast channel names to user's IPTV channels via `SportsClient.matchEventsToChannels()`
- `SportsClient.kt` — HTTP client for `thesportsdb.com/api/v1/json/123/`, parses `SportEvent` JSON (teams, scores, time, channel, thumbnail)
- `SportsModels.kt` — `SportEvent` and `MatchedSportEvent` data classes
- **Sports cards** — horizontal scroll row with green live dot, event thumbnail, team names, live scores (green), channel name, tap to play
- **Auto-refresh** — `IptvRepository.refreshSports()` called on load, channel name matching via `SportsClient.matchEventsToChannels()`
- **History bar** — horizontal scroll of last 15 channels under Favorites
- **Full-bleed logos** — channel logos now fill entire card as `ContentScale.Crop` background with dark gradient overlay; fallback to gradient + LiveTv icon when no logo
- **5s overlay timeout** — popup and channel list both auto-dismiss after 5s instead of 30s

### Phase 12 — Sports Hub Maturation & NewPipeExtractor
- **Date navigation** — horizontal date pill picker (3 days back, today + 6 ahead, "Yesterday"/"Today"/"Tomorrow" labels), all APIs refetched on tap via `?dates=YYYYMMDD`
- **Team detail pages** — `TeamDetailScreen` with logo, name, recent results, upcoming games, tap-to-play; `TeamDetailRoute` in NavHost, team name clickable from scores & standings
- **YouTube highlights hardened** — 5 Invidious + 3 Piped API instances as fallback chain; retry with simplified 3-keyword query; WebView player tuned (user-agent, load settings)
- **Dual MMA paths** — both `mma/*` and `fighting/*` queried for UFC/PFL/Bellator/boxing; fallback competitor matching when `homeAway` empty
- **Background refresh** — `isLoading` only on first load (kept data stays visible during auto-refresh)
- **Timezone fix** — `localTimezoneOffsetMs()` in `TraktPlatformClock` shows correct local dates
- **Trending News** — always visible, preserves previous news on fetch failure, placeholder card when empty
- **Fixed LazyColumn sluggishness** — `if/else` chain instead of `return@LazyColumn` early exits for stable indices
- **Fixed StandaloneCoroutine cancel** — `CancellationException` caught before generic handler
- **NewPipeExtractor integration** — `com.github.TeamNewPipe:NewPipeExtractor:v0.26.3` added via JitPack; Android uses `SearchExtractor` with OkHttp `Downloader` to search YouTube directly (no API key); iOS falls back to Invidious/Piped

### Phase 11 — EPG Crash Fix, Touch-Friendly Overlay & Sports Tab Redesign
- **EPG OOM crash fix** — streaming XML parser (`parseXmltvStream`) using `httpGetTextChunked` with <200KB buffer, replacing the 26MB String load that caused OOM; `withTimeout(180s)` wrapper; stale job cancellation before re-fetch
- **EPG file-based cache** — `IptvStorage.saveEpgCache()` / `loadEpgCache()` using platform file APIs (not SharedPreferences, which has 2MB limit); 1hr TTL, loaded on startup so EPG is available immediately
- **Channel overlay bigger & touch-friendly** — bump logos 36dp, list items 44dp+, font 13→15sp, popup 14dp padding, CH pill button larger
- **Sports tab rewrite (MMA-first)**:
  - Full ESPN API integration: 12 sports via `EspnClient.fetchAll()`, news via `EspnNewsClient.fetchNews()`
  - MMA section first (UFC/boxing/PFL/Bellator grouped under "Fighting")
  - Live scores horizontal scroll (pulse animation, neon green winners)
  - Trending news feed (ESPN API, thumbnails, category chips per article)
  - Highlights bento grid (featured 16:9 + two square tiles)
  - Stats & Standings section per league
  - Tap-to-play: score cards and highlight tiles match events to IPTV channels via `EspnClient.matchSportEventsToChannels()` → `PlayerLaunch` → `onPlayChannel`
  - Auto-refresh every 60s; refresh button in top bar
  - Standalone Sports tab in bottom navigation (not inside IPTV)
- **Fix MMA API paths** — changed `fighting/ufc` → `mma/ufc` etc. (ESPN returns 400 for `fighting/*`); `mma` → `Fighting` label mapping
- **Sports tab icon** — new `sidebar_sports.xml` (trophy) replaces shared `sidebar_iptv` icon
- **Channel picker on tap** — `EspnClient.findAllMatchingChannels()` returns ALL matching channels; single match plays directly, multiple matches show `ChannelPickerDialog`
- **Multi-source aggregation** — fetches from both ESPN `fetchAll()` and TheSportsDB `fetchTodaysEvents()` in parallel, merges deduplicated by ID; `SportEvent.toEspnProcessedEvent()` converter
- **US/UK/CA broadcaster map** — `SportBroadcasterMap` maps each league/sport to known broadcasters across all three regions (NFL→ESPN/FOX/NBC/Sky/TSN, PL→Sky/TNT/BBC/NBC/TSN, etc.); channel matcher checks all variants so "ESPN+" matches "ESPN Plus HD" or "ESPN+ UK"
- **Black background** — `SurfaceBg` changed from `#0B1326` to `#000000`, surface cards tuned to `#111` / `#1A1A1A` for OLED contrast

### Upstream Merge — Trakt Sync, iOS Now Playing, Sentry, OKHttp (July 2026)
- **Trakt credential sync** — tokens sync across devices, progress watched precedence fixed
- **iOS Now Playing** — lock screen controls with skip precision fix
- **Sentry diagnostics** — crash/error reporting integration
- **OKHttp client** — Android networking replaced Ktor with OKHttp
- **Open downloads directory** — system file picker for downloaded content
- **Additional auto-play hours** — expanded scheduling options
- **Pagination for watched movies** — fixes Trakt watched state for large libraries
- **Disable backup in AndroidManifest** — security hardening
- **Greek translations** — 854 new UI strings

### Phase 13b — Source Labels Removed, Date Range -1/+5, Sports Colors Theme-Cohesive, IPTV Grayscale, EPG Removed, Infinite Play (removed)
- **Source labels removed** — `YouTubeVideo.sourceLabel` field and all UI badges removed
- **Date range** — changed from `-3..6` to `-1..5`
- **Sports colors** — hardcoded hex constants replaced with `@Composable get()` delegates to `MaterialTheme.colorScheme.*`
- **IPTV grayscale theme** — replaced all purple/neon colors (`NeonPurple`/`ElectricBlue`) with grayscale (`AccentGray`/`OnSurface`/`SurfaceVariant`)
- **EPG removed** — EPG promotion banner, program display, source list, EPG tab, and EpgForm all removed from IPTV
- **Infinite Play (removed)** — experimental auto-loop feature was removed after testing

### Planned — NutzTube (YouTube-style tab)
- **Concept** — Full YouTube browsing tab using NewPipeExtractor (same engine as PipePipe)
- **Design** — Strictly black, grey, and white palette (no colors). Pure black background (#000000), charcoal surfaces (#121212, #1A1A1A, #252525), white text (#FFFFFF), muted gray metadata (#B0B0B0). Geist font for UI, JetBrains Mono for metadata.
- **Screens** — (1) Main browse feed with 2-column video card grid + category chips row (Trending, Music, Gaming, News, Sports), (2) Search results with dark search bar, (3) Full-screen video player with playback controls and "More Like This" row, (4) Category-filtered browse view
- **Feed** — Trending/popular video grid with category chips
- **Search** — YouTube search with results in video card grid
- **Player** — Reuses existing ExoPlayer + NewPipeExtractor stream resolution
- **Stitch design prompt** — written to `stitch_nuvio_sports_hub/nutz_tube/DESIGN.md`
- **Status** — Not yet implemented

### Hotfix — Trakt Credentials, Branding, Sports Performance, IPTV Stability
- **Trakt login fixed** — OAuth client ID/secret configured, users can now sign in to Trakt from Settings
- **"by RobbdeezeNutz" branding** — added to auth screen and loading screen under Nuvio logo
- **Sports loading optimized** — switched from 28 parallel requests to sequential prioritized requests with 5s timeout per sport, bails out early after 40 events collected
- **IPTV scroll stability** — fixed crash when scrolling through channel list
- **Screenshots added** — README now shows Sports Hub, IPTV, player controls, channel overlay, and Trakt login

## Modified Files (Phase 4–13b)
- `PlayerModels.kt` — channel data, history fields
- `PlayerScreenArgs.kt` — iptv + history params
- `PlayerScreen.kt` — plumb params
- `PlayerScreenRuntimeUi.kt` — logo copy, channel/history wiring
- `PlayerPlaybackOverlays.kt` — popup, search, half-height, modes, ChannelListItem, 5s timeout
- `App.kt` — nav, favorites, history
- `IptvScreen.kt` — group headers, source badges, collapsible grids, EPG states, Stalker tab/form, Live Sports section, History section, full-bleed logos
- `IptvRepository.kt` — stalker CRUD, name-based EPG, history, toggle methods, refreshSports
- `IptvModels.kt` — stalkerAccounts, sportEvents, sportLoading, expanded states, epgProgramsByName
- `EpgParser.kt` — EpgParseResult with channelDisplayNames
- `StalkerClient.kt` — Stalker Portal API client
- `SportsClient.kt` — NEW: TheSportsDB API client
- `SportsModels.kt` — NEW: SportEvent/MatchedSportEvent models
- `EpgParser.kt` — NEW: streaming `parseXmltvStream()` method (<200KB buffer)
- `AddonPlatform.kt` — NEW: `httpGetTextChunked()` suspend fun (Android: OkHttp streaming, iOS: Ktor `bodyAsChannel`)
- `IptvStorage.kt` — NEW: `saveEpgCache()` / `loadEpgCache()` platform file I/O
- `EspnClient.kt` — NEW: ESPN sports API client (12 leagues, matchSportEventsToChannels)
- `EspnNewsClient.kt` — NEW: ESPN news API client
- `EspnNewsModels.kt` — NEW: EspnNewsArticle / EspnNewsResponse models
- `SportsScreen.kt` — NEW: full sports hub UI (scores, news, highlights, tap-to-play)
- `SportsRepository.kt` — NEW: sports data layer
- `SportsModels.kt` — NEW: SportsUiState
- `App.kt` — SportsTab in AppScreenTab, onPlayChannel wiring
- `IptvRepository.kt` — `getAllChannels()` made public
- `sidebar_sports.xml` — NEW: trophy icon for Sports tab
- `EspnClient.kt` — `findAllMatchingChannels()`, `SportBroadcasterMap` (US/UK/CA per-league)
- `SportsRepository.kt` — TheSportsDB integration, parallel fetch, date formatting, merge/dedup

### Phase 13 — Search, Date-Switch Optimization, Trending Videos, Cache, Pull-to-Refresh
- **League-level highlight searches** — `buildLeagueQueries()` extracts unique leagues from events and generates queries like "NFL today highlights" / "UFC yesterday highlights", searched alongside event-specific queries; results merged and deduplicated by videoId
- **Pull-to-refresh** — replaced top-bar Refresh button with `PullToRefreshBox` wrapping the LazyColumn; pull down triggers `SportsRepository.refresh()`
- **ESPN response cache** — in-memory `cachedEvents`/`cachedNews`/`cachedHighlights`/`cachedStandings`/`cachedTrendingVideos`; initial load shows cached snapshot instantly while network fetch runs in background
- **Highlight sport filter** — `HighlightVideosSection` checks both `eventMap[eventId]?.sport` and `highlight.sport` (for league-level entries without event mapping) against the selected sport chip
- **Source labels on highlights** — `YouTubeVideo.sourceLabel` set to `"NewPipe"`, `"Invidious"`, or `"Piped"` based on which source returned results; displayed as badge next to channel name
- **Trending News → YouTube videos** — replaced ESPN text article news with video cards from YouTube (7 sports news queries, first to return results wins); cards match highlight card style with thumbnail, play overlay, title, channel, source label; tapping plays in ExoPlayer via `YouTubeStreamResolver`
- **Date-switching performance** — on date switch: (1) stale data stays visible (no spinner flash), (2) date-independent data (news/trending videos) is NOT re-fetched, (3) `perDateEventCache` map stores events per date so revisiting is instant, (4) after loading a date, adjacent dates D-1 and D+1 are pre-fetched in background
- **Sports search bar** — `OutlinedTextField` below date pills with search/close icons; searches YouTube via `YouTubeHighlightClient.searchHighlights(query)` AND filters loaded ESPN events by team name/league/title; results shown as video cards + matched event cards in a dedicated `SearchResultsSection`; clear button resets results

### Tab Restructure — RobbdeezeNutzHub (July 2026)
- **Removed standalone IPTV and Sports tabs** — replaced with single **RobbdeezeNutzHub** tab
- **RobbdeezeNutzHubScreen** — hub screen with 4 glass-style cards: IPTVNutz Hub, SportNutz Hub, VidNutz Hub, MusicNutz Hub
- **Persistent "RobbdeezeNutz Hubz" title** — stays visible across all sub-screens; back button shown inline within the persistent title row when on a sub-screen
- **Tapping a hub card** — opens the sub-screen inline with compact header (sub-name) + content fills remaining space
- **sidebar_hub.xml** — new dashboard-style icon for the hub tab
- **NativeTabBridge updated** — `publishTabTitles()` signature changed from (home, search, library, profile, iptv, sports) to (home, search, library, profile, hub)
- **SportsScreen** — stripped of standalone Scaffold/TopAppBar (hub provides the header)

### Phase 14 — VidNutz Hub (YouTube Video Browser)
- **VidNutz Hub created** — replaces disabled placeholder card; inline sub-screen with back button
- **VidNutzModels.kt** — `VidNutzVideo`, `VidNutzCategory` (12 entries: Trending, Politics, News, Music, Sports, Documentary, Technology, Entertainment, Comedy, Science, True Crime, Food & Drink), `VidNutzUiState` with pagination fields
- **VidNutzRepository.kt** — singleton with `fetchTrending()`, `search()`, `fetchByCategory()`, `resolveStream()`; 5 Invidious + 3 Piped API instances + NewPipeExtractor platform fallback; platform-first for page 1 (fast), Invidious for page 2+
- **VidNutzScreen.kt** — persistent `OutlinedTextField` search bar with 400ms debounce, 12 pill category chips, 1-column `LazyVerticalGrid` with 16:9 thumbnail cards (12dp radius, play overlay, duration badge, title, channel, views/date), tap-to-play via `PlayerLaunch` → ExoPlayer
- **Monochrome design** — #000000 background, black/grey/white palette, JetBrains Mono for metadata
- **D-pad focus indicators** — `focusable()` + `onFocusChanged` on chips and cards; white focus ring on focused items
- **Swipe left/right** — `detectHorizontalDragGestures` on video grid; 80dp threshold; wraps around; disabled while searching
- **"Load More" button** — explicit button at bottom of grid; spinner shown while loading
- **Sub-screen state fix** — `remember` → `rememberSaveable` + `HubSubScreen` enum made `Serializable`

### Phase 15 — MusicNutz Hub (Deezer + YouTube Music Player)
- **MusicNutz Hub created** — replaces disabled placeholder; full inline sub-screen with back button
- **MusicNutzModels.kt** — `MusicTrack`, `MusicAlbum`, `MusicNutzCategory` (12 genres: Trending, New Releases, Rock, Hip-Hop, Electronic, Pop, R&B, Jazz, Classical, Country, Metal, Indie), `MusicNutzUiState` with tracks/albums/album-detail state, `MusicNutzMode` enum (TRACKS / ALBUMS)
- **MusicNutzRepository.kt** — Deezer public API (free, no key) for track search, album search, album track lists, trending/chart endpoints; YouTube fallback via `platformYouTubeSearch` + `YouTubeStreamResolver` for full-length audio playback
- **MusicNutzScreen.kt** — persistent search bar (adapts placeholder per mode), scrollable category chips, Tracks/Albums toggle pill row, 2-column `LazyVerticalGrid` with square (1:1) album art cards
- **Album detail view** — full-screen inline view with 200dp album art, title, artist, release year, track count, scrollable track list with D-pad focus, tap to play
- **Playback** — Deezer 30s MP3 preview (if available) → YouTube full audio fallback via NewPipeExtractor; album art shown as `poster` in ExoPlayer; subtitle shows "Artist · Album"
- **Swipe, Load More, D-pad focus** — same patterns as VidNutz v2
- **All 4 hubs live** — IPTVNutz, SportNutz, VidNutz, MusicNutz all enabled and working

### MusicNutz — Planned Enhancements (saved for later)
- **Synced lyrics** — display synchronized lyrics during playback (similar to PlayTorrioV2)
- **Download tracks** — save music for offline playback
- **Playlists** — create, manage, and persist custom playlists
- **Queue management** — full player controls with shuffle, repeat, queue reorder
- **Favorites** — like songs and save albums with persisted state
- **Playback speed control** — variable speed for audio playback
- **Sleep timer** — auto-stop playback after a set duration

### Planned Enhancements (from yesnt10/NuvioMobile-Enhanced)
- **Working EPG** — implement using their simpler approach: parse EPG URLs from M3U header (`url-tvg`, `x-tvg-url`), use regex-based parser (not streaming — small per-provider files), store only current program per channel (not full schedule), match by `tvg-id`, load in background coroutine. Avoids the OOM crashes from i.mjh.nz 50MB+ aggregate files
- **AI Assistant** — Gemini, OpenRouter, Cerebras, and Groq support with grounded web search and formatted markdown replies. Add `ai/` module with models, service layer, settings storage, and web search service
- **Premium Release Calendar** — integrate into Library screen with better status handling for current and future entries, safer month transitions, and less UI flicker in calendar-driven views

## New Files
- `VidNutzModels.kt` — VidNutzVideo, VidNutzCategory (12 entries), VidNutzUiState with pagination
- `VidNutzRepository.kt` — singleton, Invidious + Piped + NewPipeExtractor, fetchTrending/search/fetchByCategory/resolveStream
- `VidNutzScreen.kt` — monochrome UI, persistent search bar, category chips, 1-column grid, Load More, D-pad focus, swipe
- `MusicNutzModels.kt` — MusicTrack, MusicAlbum, MusicNutzCategory (12 genres), MusicNutzUiState with tracks/albums/mode
- `MusicNutzRepository.kt` — Deezer public API + YouTube/NewPipeExtractor fallback
- `MusicNutzScreen.kt` — monochrome UI, persistent search bar, Tracks/Albums toggle, 2-column grid, album detail view, swipe, Load More, D-pad focus
- `RobbdeezeNutzHubScreen.kt` — hub screen with persistent title, 4 glass cards, inline sub-screens with back navigation
- `sidebar_hub.xml` — dashboard icon for the hub tab

### Multi-Window Hub — Live IPTV Grid (July 2026)

**Why:** Users wanted to watch multiple live IPTV streams at the same time (e.g. watching 4 NFL games simultaneously on a tablet, or monitoring all security cameras on one screen). A single full-screen player can only show one channel. The multi-window hub solves this by letting you tile up to 9 live streams in a grid, each with its own ExoPlayer instance, with audio from only one cell at a time.

#### Core Architecture — What You Need to Build

**MultiWindowStore** — a singleton that holds ALL runtime state for the multi-window feature. In your TV app, create this as a class or object in your DI container. It stores:
- `streams`: a reactive list of `WindowStream` objects (channel + slotIndex + unique ID)
- `volumes`: per-stream float 0–1
- `playerHandleIds`: maps stream ID to player handle (used to wire the volume slider to the correct `ExoPlayer`)
- `resizeModes`: per-stream scaling mode (Fill/Fit/16:9/4:3/Zoom)
- `audioFocusId`: which stream currently owns audio (all others are muted)
- `currentLayout` / `layoutLocked`: which grid layout is active and whether auto-switch is locked

**WindowStream** — data class: `id: String`, `channel: IptvChannel`, `slotIndex: Int`, `isPlaying: Boolean`. On TV, replace `IptvChannel` with your channel model.

**PlayerManager** — an ExoPlayer pool. Key details:
- Cap at `MAX_PLAYERS` (9 in our case). When adding a new stream at capacity, evict the oldest (LRU-style).
- Track players by integer handle ID in a `Map<Int, ExoPlayer>`.
- `createPlayer(url)` builds an `ExoPlayer` with `MediaItem`, calls `prepare()` and `playWhenReady = true`.
- `setVolume(handle, 0f..1f)` sets volume on that player only.
- `setAudioFocus(handleId)` sets one player's volume to 1f and all others to 0f. This is how only one cell has audio at a time.
- `releasePlayer(handle)` stops and releases the player, removes from pool.
- On TV, you can likely skip the pool pattern and create/destroy players per cell since TV has more memory slack.

#### Layout System — How Grid Positions Are Calculated

The layout system replaces a `LazyVerticalGrid` with manual `Row`/`Column` positioning. You MUST do the same on TV because standard grids can't support non-uniform layouts (e.g. "1 cell wide on top, 3 cells below").

**SlotPos** — data class mapping a stream index to a grid position: `(index, row, col, rowSpan, colSpan)`. The rendering loop:
1. Compute `totalRows = maxOf(slots.row + rowSpan)` and `totalCols = maxOf(slots.col + colSpan)`.
2. For each row 0..totalRows-1, create a `Row` with equal weight.
3. For each col 0..totalCols-1, find the `SlotPos` at `(row, col)`. If found, render the stream at that index as a cell, with width proportional to `colSpan`. If no slot, render an empty spacer.
4. Column spanning only (`colSpan > 1`). Vertical spanning (`rowSpan > 1`) is not supported — all current layouts use `rowSpan = 1`.

**Supported Layouts (18 presets):**

| Count | Portrait | Landscape |
|-------|----------|-----------|
| 2 | `1×2` | — |
| 3 | `1+2` (top 1 wide, bottom 2 equal), `3 vert` (stacked) | `2+1` (left 2, right 1 tall), `3 vert` |
| 4 | `2×2`, `1-2-1` (top 1 wide, middle 2, bottom 1 wide) | — |
| 5 | `1+4` (top 1 wide, bottom 2×2), `4+1` (4 grid + bottom 1 wide) | `3+2` (left 3 stacked, right 2 stacked), `4+1` |
| 6 | `3×2` (3 rows × 2 cols), `1-2-2-1` (1-2-2-1 vertical) | `2×3` (2 rows × 3 cols), `1-2-2-1` |
| 7 | `1-3-3` (top 1 wide, middle 3 equal, bottom 3 equal), `1+6` (top 1 wide, bottom 3 rows × 2 cols) | `1-3-3` |
| 8 | `4×2` (4 rows × 2 cols), `1-3-3-1` (1-3-3-1 vertical) | `2×4` (2 rows × 4 cols), `4×2` |
| 9 | `3×3` | — |

**Auto-selection:** `getValidLayouts(count, isPortrait)` returns layouts for a given stream count and orientation. `defaultLayout()` picks the first one. When the stream count or orientation changes, the grid autocalls `resolveLayout()` which returns the manual override if set, otherwise the default.

**On TV:** You can add more presets (e.g. 4×3 for 12 streams, picture-in-picture for 2, a 5+1 sports layout). Just add enum entries, add cases to `calculateSlots()`, and add to `getValidLayouts()`.

#### Grid UI (`MultiWindowGrid.kt`) — The Visual Shell

**Layout pills row** — a horizontally scrollable row of pills above the grid. Shows "Auto" + all valid layouts for current count/orientation. "Auto" resets to automatic mode. Tapping a layout locks it (prevents auto-switch when count/orientation changes). Active pill in accent color, inactive in surface card color.

**Grid body** — the Row/Column loop described above. Each cell is either a `VideoCell` or an empty spacer. **Critical:** wrap each `VideoCell` in `key(stream.id)` so Compose preserves the composable across layout changes. Without this, changing layouts would dispose and recreate all players (causing freeze/restart).

**"Add Channel" button** — a row at the bottom when under max capacity (9 on tablet, 6 on phone). Tapping navigates to the IPTV channel browser. On TV, this could open a side panel channel picker.

#### VideoCell — The Individual Stream Tile

Each cell is a self-contained unit:
1. **Player creation:** `remember(stream.id) { playerManager.createPlayer(channel.url) }`. The `key()` wrapper ensures this `remember` survives layout reordering.
2. **Lifecycle:** `DisposableEffect(stream.id)` sets initial volume on composition and releases the player on disposal. When the cell is disposed (stream removed or count drops), the player is cleaned up automatically.
3. **Video surface:** A full-size video renderer. On Android, this is `PlayerView` with `useController = false`. Pass `resizeMode` from the store so scaling pills work.
4. **Overlays (drawn on top of video):**
   - **Slot label** (top-left): semi-transparent black background, shows "1 Channel Name". 8sp font, 3dp padding. Helps identify which slot is which.
   - **Play/Pause** (center): 28dp circle at 60% black, toggles `isPlaying` state. On TV, use D-pad select instead of click.
   - **Volume toggle** (bottom-right): 18dp rounded square. Tapping toggles mute/unmute. When unmuted: sets volume to 1f, calls `setAudioFocus(thisHandleId)` which mutes ALL other cells, draws a 2dp accent border around the active cell. Only ONE cell can have audio at a time.
   - **⋮ menu** (bottom-left): opens the cell options bottom sheet. On TV, map to the "Menu" button on the remote.

#### Slot Picker (`MultiWindowPositionPicker.kt`) — Adding Streams

Triggered by long-pressing an IPTV channel in the channel browser. Shows a bottom sheet with a 3-column grid of square cells:
- Each cell represents a slot (numbered 1–9 on tablet, 1–6 on phone)
- Occupied slots show the current channel logo + name
- Empty slots show "Slot N" / "Empty"
- Tapping a slot adds the channel there via `MultiWindowStore.addToSlot(channel, slotIndex)` and dismisses

**Why stays on IPTV:** Previously, picking a slot navigated to the Multi hub automatically. Users found this jarring — they want to batch-add several channels before going to the grid. Now it stays on the IPTV screen so you can keep adding.

**On TV:** Replace the bottom sheet with a side panel or overlay. Long-press can be the "OK" hold on the remote. Show the grid as a floating overlay rather than a sheet.

#### Cell Options (`MultiWindowCellOptions.kt`) — Per-Stream Controls

A bottom sheet with four sections. On TV, use a side panel or dialog instead.

**CH / History / Fav buttons** — three pill buttons. Tapping any one replaces the sheet content with a scrollable channel list (`LazyColumn`):
- "CH" shows all IPTV channels (from `IptvRepository.getAllChannels()`)
- "History" shows recently watched channels (from `getHistoryChannels()`)
- "Fav" shows favorite channels (from `getFavoriteChannels()`)
- Each row shows the channel logo + name + "Slot N" label
- Tapping a channel calls `MultiWindowStore.addToSlot(channel, currentSlotIndex)` which replaces the current slot's channel and dismisses the sheet
- "← Back" button returns to the main options

**Why this exists:** Without it, the only way to change what's playing in a cell is to close it and re-add from IPTV. This is tedious. The channel overlay lets you hot-swap any cell's channel from the full list, history, or favorites — same UX as the in-player channel overlay.

**Volume slider** — a `Slider` from 0% to 100%, with labels on each side. Wrapped in `animateFloatAsState(targetValue, tween(300))` so programmatic volume changes animate smoothly instead of jumping. Active track in accent color.

**Scaling pills** — a horizontal scrollable row: Fill / Fit / 16:9 / 4:3 / Zoom. Each corresponds to a `RESIZE_*` constant. Tapping stores the mode in `MultiWindowStore.resizeModes` (which uses `SnapshotStateMap` so the grid cell recomposes). The video surface reads this mode and applies it to `PlayerView.resizeMode`.

**Why scaling matters:** Different IPTV channels have different aspect ratios. A 4:3 security camera feed should not be stretched to Fill. A 16:9 sports stream should not have black bars. Per-cell scaling lets the user optimize each tile.

**Close Channel** — red row. Calls `MultiWindowStore.remove(stream.id)` → removes from grid, releases player, dismisses sheet.

#### Navigation — Back from Full-Screen Player

**Critical bug found:** The hub's `onPlayChannel` was calling BOTH `onIptvPlayChannel` AND `onSportsPlayChannel`. Both do `navController.navigate(PlayerRoute(id))`. This pushed TWO `PlayerRoute` entries onto the nav stack. The user had to press back TWICE — the first press popped to the duplicate player (flicker), the second popped back to the hub.

**Fix:** Only call `onIptvPlayChannel?.invoke(launch)`. Both handlers do the same thing (navigate to the same player), so the second call was redundant.

**On TV:** Your navigation may use a different pattern (fragment transactions, `startActivity`, or a custom router). The key takeaway: the player launch source should only fire once. If you have multiple "play channel" handlers, make sure only one navigates.

#### Tablet Detection

`BoxWithConstraints(width >= 600.dp)` in both the grid and the slot picker:
- Phone: max 6 streams, picker shows 6 slots (2 rows × 3 cols)
- Tablet: max 9 streams, picker shows 9 slots (3 rows × 3 cols)

**Why 600dp:** This is the standard Material Design breakpoint for "compact" vs "medium" screens. At 600dp+ (7-inch tablet portrait, 10-inch landscape), there's enough room for 9 tiles. On phones, 9 tiles would be too small to be usable.

**On TV:** TV screens are always large enough for 9 tiles. You can set `MAX_PLAYERS = 9` unconditionally and skip the phone/tablet gate. You might even go to 12+ tiles since TV screens are much larger.

#### What's NOT in the Mobile Version (for TV Considerations)

| Feature | Mobile | TV Suggestion |
|---------|--------|---------------|
| Bottom sheets | Used for picker + cell options | Replace with side panels / floating dialogs / on-screen overlays |
| Long-press for picker | Works with touch | On TV, use "Menu" button or a dedicated "Add to Multi" action in the channel context menu |
| Touch overlays (play/pause, volume, ⋮) | Click targets over video | On TV, use D-pad navigation between tiles + a system overlay bar at the bottom |
| `BoxWithConstraints` tablet gate | 600dp break | Not needed — TV is always large. Use a higher `MAX_PLAYERS` (12–16) and add more layouts |
| Swipeable layout pills | Horizontal scroll | On TV, use digital channel navigation (left/right on D-pad) |
| `animateFloatAsState` for volume | Nice-to-have animation | Optional — TV can use direct slider value |
| Audio focus (one cell unmuted) | Mutes all others when one is active | Same concept on TV — only one tile has audio at a time |
| Player pool (MAX=9, evict oldest) | Memory management | TV may not need pooling — create/destroy per cell freely |
| `SnapshotStateList` / `SnapshotStateMap` | Compose reactivity | If using Jetpack Compose for TV, same approach works. If using a different UI framework, use your framework's reactive state (StateFlow, LiveData, etc.) |

#### Files — What to Create on TV

| File | Purpose | Notes for TV Port |
|------|---------|-------------------|
| `MultiWindowStore.kt` | Reactive state singleton | Use `mutableStateListOf`/`mutableStateMapOf` (Compose TV) or `StateFlow` (non-Compose) |
| `MultiWindowLayouts.kt` | Layout enum + slot calculator | Pure logic — copy verbatim. Add more presets for TV screen size |
| `MultiWindowGrid.kt` | Grid composable + VideoCell | Replace overlays with D-pad-friendly UI. Use `key()` wrapper for player stability |
| `MultiWindowPositionPicker.kt` | Slot picker | Replace bottom sheet with overlay panel. 3×3 grid works for TV too |
| `MultiWindowCellOptions.kt` | Per-cell controls | Replace bottom sheet with side panel. Keep CH/History/Fav, volume, scaling, close |
| `MultiWindowPlayerEngine.kt` | Common expect/actual | Define player interface. Add more resize constants if needed |
| `MultiWindowPlayerEngine.android.kt` | ExoPlayer pool | `MAX_PLAYERS = 12+` for TV. Use `PlayerView` same as mobile |
| `MultiWindowPlayerEngine.ios.kt` | iOS stub | Not needed for Android TV |

### Phase 16 — TV Sports Hub Features Ported to Mobile (July 2026)
- **Orientation fix** — `LockPlayerToLandscape` now checks device rotation first; only locks to sensor landscape if user is *already* in landscape (no forced orientation switch)
- **TV-style league chips** — combat sports first (`⚡ Sports Now`, UFC, Boxing, PFL, PPV) ahead of NFL/NBA/MLB/NHL/MLS; tapping a chip filters events or triggers special views
- **"Sports Now" live aggregation** — scans all ESPN leagues for in-progress events, displays in 2-column grid, stores in `SportsNowStore` singleton for player overlay
- **PPV / Special Events league** — bypasses ESPN, searches YouTube for PPV content (UFC, boxing, WrestleMania)
- **GameToChannelMatcher** — 3-tier matching engine (LEAGUE → TEAM → GENERAL_SPORTS) with region detection (US/UK/CA)
- **"Find Channel" button** — on live score cards, taps select event and open detail panel
- **Event detail panel** — full-screen with 3-tab system: LIVE (matched channels + region filter chips), HIGHLIGHTS (YouTube grid), PRE-MATCH (preview videos); "Watch Highlights Instead" fallback
- **45s auto-refresh** — polling for selected league
- **Live Games overlay** — "LIVE" pill button in IPTV player controls (when live events exist), slide-up sheet with scores + Switch
- **SportsNowStore** — singleton bridge for live events between sports repo and player
- **New/Modified files:** `GameToChannelMatcher.kt`, `SportsNowStore.kt`, `SportsModels.kt`, `SportsRepository.kt`, `SportsScreen.kt`, `EspnClient.kt`, `PlayerPlatformEffects.android.kt`, `PlayerScreenRuntimeState.kt`, `PlayerPlaybackOverlays.kt`, `PlayerControls.kt`, `PlayerScreenRuntimeUi.kt`

### Phase 17 — MultiNutz Hub Vol. 2: Scaling, Swap, Overhaul & Bugfixes (July 2026)
- **Real-time scaling fix** — `AndroidView` in `MultiWindowPlayerEngine.android.kt` now has `update = { view -> view.resizeMode = rm }` so scaling changes apply immediately instead of being frozen at factory time
- **Phone↔tablet freeze fix** — wrapped `AndroidView` in `key(handle.id)` so layout transitions (phone 6-slot ↔ tablet 9-slot) don't dispose and recreate `PlayerView`, keeping the player surface alive across orientation/size changes
- **Default video fit** — `getResizeMode()` default changed from `RESIZE_FILL` to `RESIZE_FIT` so streams don't crop by default
- **Volume discrete step dots** — removed `Slider`, replaced with 7 pill buttons: Mute / 15 / 30 / 45 / 70 / 85 / Max. Active step highlights in accent color. Reactive via `mutableStateMapOf` for `volumes`.
- **Stream refresh** — "Refresh Stream" row in cell options releases the current player and re-creates it with the same channel at the same slot
- **Swap positions** — "Swap" pill in cell options opens slot picker showing occupied slots; tapping swaps the two streams via `MultiWindowStore.swapSlots()`
- **Mute All / Close All / Pause All** — three pills in the grid layout row. Mute All sets all volumes to 0. Close All removes all streams. Pause All releases all player handles.
- **Push-to-multi from ExoPlayer** — "Multi" pill button in IPTV player controls (shown when `parentMetaId == "iptv"`). Uses `MultiWindowPushStore.pendingChannel` (set before player launch in `IptvScreen.playChannel()`) to find the channel without URL lookup. Adds to first empty slot, sets `HubReturnStore.subScreen = "Multi"`, pops back.
- **MultiNutz rename** — "MultiWindow Hub" → "MultiNutz Hub" in hub card and sub-screen header
- **Layout bookmarks** — ★ pill opens `MultiWindowBookmarksSheet` with save/load/delete. `MultiWindowBookmark` stores layout + slot→channel map. Load restores all channels to their slots.
- **2×1 layout** — added `V2_STACK("2×1")` layout for portrait stacking
- **Layout pills reactive** — `_currentLayout` and `_layoutLocked` changed to `mutableStateOf` so tapping a layout pill triggers immediate grid recomposition

### Phase 17b — Channel Overlay Overhaul: Sources, Groups, Search
- **Source filter pills** — channel overlay now shows "All" + each M3U/Xtream/Stalker source by name as filter pills at the top
- **Group filter pills** — channels grouped by `group` field, shown as scrollable filter pills below sources
- **Search field** — `OutlinedTextField` at top of channel overlay filters channels by name in real-time
- **Group subtitle** — each channel row shows its group name as a smaller subtitle when no group filter is active
- **Swap position overlay** — new overlay in cell options showing slot grid; tapping an occupied slot swaps the two streams

### CloudNutz Removed (July 2026)
- **CloudStream plugin system removed** — after extensive attempts (PathClassLoader, Plugin bridge, code_cache DEX loading, app init, logging), CloudStream `.cs3` plugin loading was abandoned due to:
  - Android 14+ blocking DEX loading from writable app directories
  - Missing `Plugin` class in `cloudstream:library` (requires cloudstream-runtime-api AAR)
  - `NiceHttp` HTTP client dependency not available
  - CloudStream's `app.get()` network client initialization issues
- **10 files deleted** — all `CloudNutz*`, `CloudNutzRepositoryManager*`, `Plugin.kt` bridge removed
- **Dependencies removed** — `cloudstream:library:master-SNAPSHOT` from libs.versions.toml and build.gradle.kts
- **JitPack repository retained** — still used by `TeamNewPipe:NewPipeExtractor`

### Hub Grid Redesign (July 2026)
- **RobbdeezeNutzHub home redesigned** — replaced vertical `HubCard` list with responsive `LazyVerticalGrid` (`GridCells.Adaptive(minSize=160dp)`)
- **Square grid cards** — aspect-ratio 1:1, `Color(0xFF1F1F1F)` surface, 80dp circular icon containers (turn white on focus), 32sp bold 2-letter badges (TV/SP/VN/MU/MW)
- **Header simplified** — removed "Explore Hubz" headline and subtitle, grid starts directly below title bar
- **Compact top padding** — reduced from 96dp→48dp (tablet) and 32dp→12dp (mobile)
- **All navigation preserved** — 5 hub cards still navigate to sub-screens, back button works

### Multi-View "MV" Button in Player (July 2026)
- **Send any video to MultiNutz** — new "MV" button (Dashboard icon) in player controls action bar
- **Works with ALL content** — IPTV, movies, shows, sports, music — anything playing in ExoPlayer
- **Generic stream support** — `WindowStream` now has `playerUrl`, `playerTitle`, `playerPoster` fields
- **`MultiWindowStore.addStream()`** — new function that creates a stream without needing an `IptvChannel`
- **`MultiWindowPositionPicker`** — now accepts generic `streamTitle?`/`streamUrl?`/`streamPoster?` alongside `channel?`
- **Slot selection flow** — tap MV → picker shows slots → select slot → stream added → picker closes
- **IptvChannel dummy** — generic streams wrapped in minimal dummy channel for existing grid rendering

### Upstream Merge (July 2026)
- **NuvioMedia/NuvioMobile cmp-rewrite merged** — 68 commits from upstream
- **Navigation3 migration** — replaced old tab navigation (`AppScreenTab`, `selectedTab`) with `navigation3` (`AppRoute` sealed interface, `navBackStack`, `entry<>{}` composable pattern)
- **Routes refactored** — all `AppRoute` implementations moved to `navigation/Routes.kt`, `TeamDetailRoute` added there
- **`TeamDetailRoute`** — preserved as a Robbdeeze-specific route, re-added with `entry<>{}` pattern
- **`PlayerControls.kt`** — updated to upstream's new signature (nullable subtitle/audio callbacks, `NuvioLoadingIndicator`, `showDeviceStatusOverlay`, `qualityLabel`, `onRandomEpisodeClick`, shuffle/tune/tv/wifi icons)
- **`PlayerScreen.kt`** — added `onOpenExternalUrl` parameter from upstream
- **`PlayerSubtitleCueParser.kt`** — regex updated to upstream version
- **Merge conflicts resolved** — 5 in App.kt, 2 player files, 1 subtitle parser; all resolved without breaking our custom features
- **All hub/MV/IPTV features preserved** — grid layout, MV button, IPTV tokens, hub sub-screens all work post-merge

### Enhanced Fork Review & .AAR Discovery (July 2026)
- **Full analysis of `yesnt10/NuvioMobile-Enhanced`** — saved to `_bmad-output/enhanced-fork-review-cloudstream-ai.md`
- **CloudStream approach** — uses local AAR (`cloudstream-runtime-api-4.8.0-3496e5f.aar` NOT in git) + bridged source files + hand-written adapters (no DEX loading at runtime)
- **AI Assistant** — 4 providers (Cerebras, Groq, Gemini, OpenRouter) with auto-fallback + Tavily web search
- **Critical discovery: .AAR not needed** — the enhanced fork's adapter code (`CloudStreamModels.kt`, `CloudStreamRepository.kt`, `CloudStreamNuvioAdapter.kt`, etc.) **never imports from the `.aar`**. It defines its own DTOs and uses `kotlinx.serialization` (already a dependency). The `.aar` only provides classes for the CloudStream plugin runtime (`BasePlugin`, `Plugin`, `MainAPI`), which we don't use because we don't load `.cs3` DEX files.
- **Implementation path** — can copy 25+ adapter source files directly (~4 hours effort). No `.aar`, no `.cs3` loading, no `PathClassLoader`, no Android 14+ restrictions. The adapters are pure Kotlin compiled into the app using standard HTTP + JSON. Only covers the 1-2 providers the enhanced fork wrote adapters for (KickTR + maybe 1 more), but we can write additional adapters for any website with an API.
- **Not a merge blocker** — the `.aar` was thought to block the merge, but we can integrate the adapter code without it. Full details in `_bmad-output/enhanced-fork-review-cloudstream-ai.md#5-critical-discovery-we-dont-need-the-aar`

### Phase 17c — Sports Hub Fixes (July 2026)
- **"Live Now" → "Live/Upcoming"** — header shows both active game count with pulse dot AND upcoming count
- **Highlight/prematch video streaming fix** — `VideoCardSmall.onClick` in `SportEventDetailPanel` was silently creating a `PlayerLaunch` and dropping it. Added `onPlayPlayerLaunch` callback wired to the outer `onPlayChannel`, so tapping a highlight/prematch video now actually plays.
- **Event images for UFC/fighting sports** — added `eventImage` field to `EspnProcessedEvent` extracted from `competition.logos` (picks largest logo). Added `thumbnail` fallback from `EspnEvent.thumbnail`. Renders event image as a banner in `ScoreCard` when both team logos are null (the case for UFC/boxing fighters who have no team logo).
- **`SportsAsyncImage` fallback** — handles null/blank URLs with a `?` placeholder. Wraps `AsyncImage` in `Box` for stable sizing.
- **ESPN models** — added `EspnLogo` data class, `logos` field to `EspnCompetition`, `thumbnail` field to `EspnEvent`
- **Back navigation preserved** — `SportsRepository` singleton state (selected event, videos, active tab) persists across player navigation lifecycle. `HubReturnStore.subScreen` restores Sports sub-screen on player exit.
- **MultiWindowPushStore** — stores the `IptvChannel` before player launch so the push-to-multi callback can find the channel without a fragile URL-based lookup
