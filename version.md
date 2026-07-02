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
- **Logo changing on channel switch** — added `logo = newLogo, poster = newLogo` to `PlayerLaunch.copy()` in `onSwitchChannel` callback so the new channel's logo appears in the player loading splash
- **EPG name-based matching** — `EpgParser` now parses `<channel>` elements for display names; `refreshEpg()` builds both `epgPrograms` (by channel ID) and `epgProgramsByName` (by lowercase display name); `ChannelCard` tries ID match first, then falls back to name-based lookup
- **EPG match count** — repository counts matched channels (ID + name) and shows "X channels matched" on EPG source cards in the playlists section
- **Error logging** — removed empty `catch (_: Exception)` in `refreshEpg()`, now logs `println("EPG fetch error: ...")`  
- **Brace fix** — fixed missing closing `}` in IptvScreen that caused all private composable functions to be treated as local functions

## Modified Files (Phase 4–8)
- `PlayerModels.kt` — channel data, history fields, episode fields
- `PlayerScreenArgs.kt` — iptv params, history params, onToggleIptvFavorite
- `PlayerScreen.kt` — plumb all params
- `PlayerScreenRuntimeUi.kt` — read channel/history data, wire switching with logo copy
- `PlayerPlaybackOverlays.kt` — popup menu, OutlinedTextField, half-height, modes, ChannelListItem, PopupOption, timeout
- `App.kt` — channel switch nav, favorite + history wiring
- `IptvScreen.kt` — group headers, source badges, collapsible grids, history pass, refresh spinner, EPG states with name fallback, brace fix
- `IptvRepository.kt` — toggle methods, auto EPG refresh, addToHistory, getHistoryChannels, refreshingSourceIds, name-based EPG matching
- `IptvModels.kt` — channelsExpanded, favoritesExpanded, channelHistory, refreshingSourceIds, epgProgramsByName, epgMatchCount
- `EpgParser.kt` — changed return type to `EpgParseResult` with programsByChannelId + channelDisplayNames map; parses `<channel>` elements
