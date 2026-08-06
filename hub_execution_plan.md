# Nuvio Hub Execution Plan

## Proposed execution order

| Phase | Effort | Risk | User value |
|---|---|---|---|
| 1 — Timezone | Low | Low | Subtle but pervasive fix |
| 2 — NuvioNutz EPG | Medium | Low | Core UX improvement |
| 3 — Multinutz windows | High | Medium | Major feature enhancement |
| 4 — MW overlay | Low | Low | Navigation improvement |
| 5 — UI cleanup | Low | Low | Visual polish |
| 6 — LiveGame sources | Medium | Low | Content expansion |

## Phase 1 — Timezone fixes

User: "Make sure all event times are based on device location." Six spots currently use UTC or hardcoded offsets:

| # | Location | File:line | Problem |
|---|---|---|---|
| 1 | ESPN time display | `EspnClient.kt:439-456` | UTC times shown as-is |
| 2 | EPG scheduler clock | `CinematicTunerModels.kt:229-241` | UTC-based clock & schedule |
| 3 | IPTV clock widget | `IptvScreen.kt:446-464` | Shows UTC not local |
| 4 | LiveGame today-filter | `PlayerPlaybackOverlays.kt:234-243` | UTC-based date boundary |
| 5 | DaddyLive time | `DaddyLiveClient.kt:80` | Hardcoded +4h ET, DST-broken |

All five use `TraktPlatformClock.nowEpochMs()` (UTC epoch) without adding `localTimezoneOffsetMs()`, which is already available in `TraktPlatformClock.android.kt:14-15`.

**Fix for all five:** Add `TraktPlatformClock.localTimezoneOffsetMs()` before formatting or computing `today`/`now`/slot boundaries.

---

## Phase 2 — NuvioNutz EPG improvements

**2.1 Device-local time for EPG** — Same as Phase 1 item #2 (`EpgScheduler`, `CinematicTunerModels.kt:229-241, 266-270`). Schedule should start at the next local hour, clock should show local time, playhead should compute against local `now`.

**2.2 Random episode for TV series** — `CinematicTunerViewModel.playCurrentItem` currently plays the current schedule item. For series, pick a random entry from the schedule instead of the current one.

**2.3 Swipe gestures** — Currently EPG navigation is via on-screen buttons (`CinematicTunerScreen.kt:462-469`). Add:
- `detectHorizontalDragGestures`: swipe left/right advances to next/previous program in the same channel
- `detectVerticalDragGestures`: swipe up/down switches to the next/previous channel

**Files:** `CinematicTunerViewModel.kt`, `CinematicTunerModels.kt`, `CinematicTunerScreen.kt`

---

## Phase 3 — Multinutz per-window features

User: "Each window should have auto-next, hideaway left/right arrows, channel overlays, and search bar."

Currently each window (`VideoCell` in `MultiWindowGrid.kt:324-505`) has: play/pause, volume, fullscreen, progress bar, and the "⋮" options button. **All four requested features are absent.**

**3.1 Auto-next:** Add `Player.Listener` to the ExoPlayer in `MultiWindowPlayerEngine.android.kt:27-59`. On `STATE_ENDED`, invoke a callback to advance to the next item in the window's queue. Store per-window queue and current index in `MultiWindowStore`.

**3.2 Hideaway left/right arrows:** Add left/right arrow buttons (semi-transparent, auto-hide after 3s) to each `VideoCell`. Arrow click triggers focus/move to adjacent window slot.

**3.3 Channel overlay (inline):** Extract the `ChannelOverlay` composable from `MultiWindowCellOptions.kt:302-435` into a per-window slide-up overlay anchored to each `VideoCell`.

**3.4 Search bar:** Add a text field to each `VideoCell` overlay (slides down from top). On submit, searches channels via the existing `ChannelOverlay` search logic and populates the window.

**Files:** `MultiWindowGrid.kt`, `MultiWindowStore.kt`, `MultiWindowPlayerEngine.android.kt`, `MultiWindowCellOptions.kt`, `MultiWindowPlayerEngine.kt`

---

## Phase 4 — MW overlay button → always Multinutz (from anywhere)

Currently the MW button at `App.kt:3779-3798` only sets `HubReturnStore.subScreen = "Multi"` but **cannot switch tabs** — `AppTabHost` has no tab-activation callback. On non-Hubz tabs, clicking the button does nothing visible.

**Fix:**
1. Add an `onOpenMultinutz: () -> Unit` param to `AppTabHost` (line 3617)
2. Wire it to a lambda in `MainAppContent` that calls `activateTab(AppScreenTab.RobbdeezeNutzHub)` + `HubReturnStore.subScreen = "Multi"`
3. For true "anywhere" coverage (including player/detail routes), hoist the overlay above `NavDisplay` (line 1844) so it survives route pushes

**Files:** `App.kt:3617, 3779-3798, 1844-1850`

---

## Phase 5 — UI cleanup

**5.1 Remove SportNutz pill menu buttons:** Delete `CategoryChipsBar` composable and its rendering sites at `SportsScreen.kt:284-288, 364-368, 902-908`.

**5.2 Remove VidNutz Live tab:** Remove `LIVE("Live")` from `VidNutzCategory` enum, delete `fetchLive()` from `VidNutzRepository.kt`, and remove the tab chip rendering.

**5.3 Optimize VidNutz first-load search:** Already partially done — `LaunchedEffect(selectedCategory)` at `VidNutzScreen.kt:121-132` fires on each tab change. The engine cache is in-memory only. Add `invalidateEngineCache` call when the screen composes to ensure fresh results.

**Files:** `SportsScreen.kt`, `VidNutzModels.kt`, `VidNutzRepository.kt`, `VidNutzScreen.kt`

---

## Phase 6 — LiveGame overlay from all sports sources

Currently the overlay (`PlayerPlaybackOverlays.kt:233-397`) only feeds from `SportsNowStore.liveEvents` (ESPN) and `daddyLiveEvents` (DaddyLive).

**Missing sources:**
- `Sync2Cal` events (`SportsRepository.kt:187-204`) — sports calendar events
- YouTube sports highlights (`SportsRepository.kt:232-259`, `searchSportVideos`) — channel video highlights

**Fix:** Add all three sources to `SportsNowStore`: populate from `loadAllSports()` fetch, filter to `isLive` and display in the overlay with source labels. Extend the 30-second heartbeat (`SportsRepository.kt:149-164`) to cover missing sources.

**Files:** `SportsNowStore.kt`, `SportsRepository.kt`, `PlayerPlaybackOverlays.kt`
