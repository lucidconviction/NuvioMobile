# Nuvio Mobile — Version History & Knowledge Base

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
