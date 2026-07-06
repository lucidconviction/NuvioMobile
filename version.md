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
