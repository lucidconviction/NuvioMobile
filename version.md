# Nuvio Mobile — Version History & Knowledge Base

## Build Commands
```bash
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full
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

## Modified Files (Phase 4–10)
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
