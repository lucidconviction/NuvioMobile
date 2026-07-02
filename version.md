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

### Phase 9 — Stalker Portal Support
- **Stalker Portal** as a 4th source type in Add Source bottom sheet (XTREME / M3U / EPG / STALKER)
- `StalkerAccount` model with `id`, `name`, `server`, `macAddress`, `channels`
- `StalkerClient.kt` — HTTP handshake: get token → authenticate MAC → fetch channels JSON
- Form fields: Account Name, Portal URL, MAC Address
- Full CRUD: `addStalkerAccount`, `removeStalkerAccount`, `refreshStalkerChannels` with refresh spinner
- Persisted via JSON, shows as "SK" source cards in playlists
- `SourceType.Stalker` enum variant, auto-included in all filtering/persistence
- **Full-screen overlay** — changed from `fillMaxHeight(0.5f)` to `fillMaxSize()` for better landscape scrolling
- **Stalker Portal** as a 4th source type in Add Source bottom sheet (XTREME / M3U / EPG / STALKER)
- `StalkerAccount` model with `id`, `name`, `server`, `macAddress`, `channels`
- `StalkerClient.kt` — HTTP handshake: get token → authenticate MAC → fetch channels JSON
- Form fields: Account Name, Portal URL, MAC Address
- Full CRUD: `addStalkerAccount`, `removeStalkerAccount`, `refreshStalkerChannels` with refresh spinner
- Persisted via JSON, shows as "SK" source cards in playlists
- `SourceType.Stalker` enum variant, auto-included in all filtering/persistence

## Modified Files (Phase 4–9)
- `PlayerModels.kt` — channel data, history fields
- `PlayerScreenArgs.kt` — iptv + history params
- `PlayerScreen.kt` — plumb params
- `PlayerScreenRuntimeUi.kt` — logo copy, channel/history wiring
- `PlayerPlaybackOverlays.kt` — popup, search, half-height, modes, ChannelListItem
- `App.kt` — nav, favorites, history
- `IptvScreen.kt` — group headers, source badges, collapsible grids, EPG states, Stalker tab + form, stalker cards in playlists
- `IptvRepository.kt` — stalker CRUD, name-based EPG, history, toggle methods
- `IptvModels.kt` — stalkerAccounts, channelsExpanded, favoritesExpanded, channelHistory, epgProgramsByName
- `EpgParser.kt` — EpgParseResult with channelDisplayNames
- `StalkerClient.kt` — NEW: Stalker Portal API client
