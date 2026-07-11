# TV Multi-View Instructions — Phase 17 Port

This document translates every multi-window change from the mobile Phase 17 / 17b / 17c into TV-appropriate guidance. The core logic ports verbatim; only the UI layer (Compose bottom sheets → Leanback fragments / TV overlays) and input model (touch → D-pad) differ.

## Phase 17 — MultiNutz Hub Vol. 2 for TV

When porting to Android TV / Fire TV, apply these changes:

### 1. Real-Time Scaling Fix

**Mobile fix:** `AndroidView` `update` lambda in `MultiWindowPlayerEngine.android.kt` sets `view.resizeMode = rm` on every recomposition instead of only at factory time.

**TV port:**
- If using `VideoSurfaceView` / `SurfaceView` in a Leanback `Presenters` or `DetailsOverviewRow`, make sure `resizeMode` is applied **every time** the bound data changes (not just in `onBindViewHolder`).
- If using Compose for TV (`androidx.tv.material3`), same `AndroidView` `update` pattern works.
- **Critical test:** Change scaling from Fit → Fill → 16:9 while the stream is playing. If the video doesn't reshape immediately, your scaling isn't reactive.

### 2. Screen Size / Orientation Freeze Fix

**Mobile fix:** Wrapped `AndroidView` in `key(handle.id)` so phone↔tablet layout transitions don't dispose and recreate `PlayerView`.

**TV port:**
- TV orientation doesn't change, but **layout changes** (e.g. going from 2×2 → 1+2+1 or 2→4 streams) must not recreate player surfaces.
- Wrap each video surface composable in a **stable key** — use the stream's unique `id`, not its slot index.
  ```kotlin
  // DO THIS:
  key(stream.id) { VideoCell(...) }
  // NOT THIS:
  key(slotIndex) { VideoCell(...) }
  ```
- If any layout change causes even a 1-frame black flash on any tile, the `key()` is wrong. The key must be unique per stream and survive slot moves.

### 3. Default Video Fit

**Mobile fix:** `getResizeMode()` default changed from `RESIZE_FILL` to `RESIZE_FIT`.

**TV port:**
- Set default resize mode to `RESIZE_FIT` everywhere — player init, fresh streams, clear-all reset.
- Users expect to see the full frame, not a cropped one. Let them opt into Fill manually via scaling pills.

### 4. Volume Discrete Step Dots (Replace Slider)

**Mobile fix:** Removed `Slider` (thumb track). Replaced with 7 pill buttons: Mute / 15 / 30 / 45 / 70 / 85 / Max. Reactive via `mutableStateMapOf`.

**TV port:**
- **Do NOT use a slider on TV.** D-pad slider navigation is miserable (hold D-pad right for 3 seconds to go 0→100).
- Use discrete step pills instead. On TV, they should be focusable buttons in a horizontal `Row`:
  - Mute (0%)
  - 15%
  - 30%
  - 45%
  - 70%
  - 85%
  - Max (100%)
- D-pad left/right navigates between pills. Select/OK sets the volume.
- Active step gets a highlight/accent ring.
- Store volumes in a reactive map (same as mobile). When audio focus changes, the new active cell reads its volume from this map and all other cells are muted.

### 5. Stream Refresh

**Mobile fix:** "Refresh Stream" row in cell options releases current player and re-creates with same channel at same slot.

**TV port:**
- Add a "Refresh" button/action to the cell context menu (invoked by Menu button on remote).
- On TV, show a brief loading spinner overlay on just that cell while the player re-connects (so the user doesn't think it crashed).
- Logic: `playerManager.releasePlayer(handle)` → `playerManager.createPlayer(channel.url)` → assign the new handle to the same stream ID.

### 6. Swap Positions

**Mobile fix:** "Swap" pill opens slot picker showing occupied slots; tapping swaps two streams via `MultiWindowStore.swapSlots()`.

**TV port:**
- Same slot picker UI, but shown as an overlay panel (not bottom sheet).
- When user selects "Swap" from cell menu → show grid of occupied slots → navigate with D-pad → select target slot → streams swap.
- `swapSlots()` logic is pure data — swap the `slotIndex` values of two `WindowStream` objects. Copy verbatim.

### 7. Mute All / Close All / Pause All

**Mobile fix:** Three pills in the grid layout row.

**TV port:**
- Place these in a toolbar row above or below the grid (not overlapping video tiles).
- D-pad navigation: these are focusable buttons. Left/Right between them, Up to the layout pills, Down to the grid.
- **Mute All:** Sets all volumes in `volumes` map to 0f. Each cell's `DisposableEffect` or recomposition picks up the new value.
- **Close All:** Iterates all streams, releases each player via `playerManager.releasePlayer(handle)`, clears `streams` list.
- **Pause All:** Iterates all streams, calls `playerManager.pause(handle)` for each. Do NOT release — resume via a single "Play All" or per-cell play toggle.

### 8. Push-to-Multi from Player

**Mobile fix:** "Multi" pill in IPTV player controls. Uses `MultiWindowPushStore.pendingChannel` (set before player launch) to find channel without URL lookup. Adds to first empty slot, pops back.

**TV port:**
- During `ExoPlayer` full-screen playback, add a "Send to Multi-View" action:
  - In the Leanback `PlaybackOverlayFragment`, add a `PlaybackControlRow` action or custom `PlaybackCardAction`.
  - When triggered: read `MultiWindowPushStore.pendingChannel` → add to first empty slot in `MultiWindowStore` → navigate back to the multi-view grid.
- `MultiWindowPushStore` is a simple singleton:
  ```kotlin
  object MultiWindowPushStore {
      var pendingChannel: IptvChannel? = null
  }
  ```
  Set it in the channel browser just before navigating to the player:
  ```kotlin
  MultiWindowPushStore.pendingChannel = channel
  navigateToPlayer(channel)
  ```

### 9. Layout Bookmarks

**Mobile fix:** ★ pill opens `MultiWindowBookmarksSheet` with save/load/delete. `MultiWindowBookmark` stores layout + slot→channel map.

**TV port:**
- Show a "Bookmarks" button in the toolbar row (near Mute/Close/Pause All).
- On TV, use a simple dialog overlay (Leanback `AlertDialog`-style) showing the bookmark list + Save/Delete actions.
- Each bookmark needs:
  - `name: String` (user-editable, pre-filled with "Bookmark N")
  - `layoutName: String` (e.g. "3×3")
  - `slotChannelMap: Map<Int, ChannelIdentifier>` (maps slot index → channel ID/URL)
- **Save:** Serializes current layout + all slot assignments to local storage (SharedPreferences / DataStore / Room).
- **Load:** Restores `slotChannelMap` entries to `MultiWindowStore.streams` with fresh `WindowStream` objects (new unique IDs). The `key()` wrapper ensures new players are created.
- **Delete:** Removes from storage.

### 10. 2×1 Layout

**Mobile fix:** Added `V2_STACK("2×1")` for portrait stacking.

**TV port:**
- Add to your `LayoutPreset` enum.
- On TV, 2×1 is useful for side-by-side (sports + stats overlay, or main event + alternate angle).
- `calculateSlots(2, V2_STACK)` → row 0: col 0-1, single cell. Row 1: col 0, cell. Row 1: col 1, cell. (Or whatever visual makes sense — 2×1 on a 16:9 TV is just 2 equal columns.)
- Register in `getValidLayouts(2, isPortrait=false)` → returns list including `V2_STACK`.

### 11. Reactive Layout Pills

**Mobile fix:** `_currentLayout` and `_layoutLocked` changed from non-reactive vars to `mutableStateOf`.

**TV port:**
- Use reactive state for layout selection. In Compose for TV, use `mutableStateOf`. In Views, use `LiveData` / `StateFlow` and observe in the fragment/activity.
- When the user D-pad navigates to a layout pill and presses OK, the grid must recompose immediately — no stale layout shown even for a frame.

## Phase 17b — Channel Overlay Overhaul for TV

### Source Filter Pills

- Show "All" + each M3U/Xtream/Stalker source by name as focusable pill buttons.
- **TV UX:** Horizontal row at top of the channel overlay panel. D-pad left/right to navigate, Up to search field, Down to group pills.
- On TV, the channel overlay should be a **side panel** (slides in from right), not a bottom sheet. Full-height, 50–60% screen width.

### Group Filter Pills

- Channels grouped by `group` field. Scrollable horizontal pills below source pills.
- On TV, `LazyRow` is fine. Each pill is focusable.
- When a group is selected, only channels in that group show.
- When no group is selected, show all channels with group name as subtitle text.

### Search Field

- `OutlinedTextField` at top of overlay. On TV, use a standard `EditText` or `TextField` with D-pad keyboard input.
- Real-time filter: as user types, channels list filters by name match.
- **TV consideration:** Use Leanback's `SearchFragment` or a simple inline text field. Avoid Android TV's full-screen search (too heavyweight for an overlay).

### Swap Position Overlay

- In cell options, "Swap" opens a slot grid showing which slots are occupied.
- On TV, this replaces the current overlay content with a grid of square slots. D-pad navigate to select target slot, press OK to swap.

## Phase 17c — Sports Hub Fixes for TV

### "Live/Upcoming" Dual Header

- Show both active game count (with pulsing dot) and upcoming game count.
- On TV Leanback, use a standard `HeaderItem` or a custom header row.

### Highlight / Prematch Video Streaming Fix

- Ensure `onPlayPlayerLaunch` callback is wired from `VideoCardSmall` → outer handler → channel player.
- On TV, Leanback `VideoCardView` or custom `CardPresenter` must have an `onClick` that triggers player launch. Test: tap a highlight/prematch → must actually play video, not silently drop the intent.

### Event Images for UFC / Fighting Sports

- Extract largest logo from `competition.logos` → `eventImage` on `EspnProcessedEvent`.
- Show as banner in the score/results card when both team logos are null (always the case for UFC fighters).
- On TV, use a full-width banner image at the top of the event detail row.

### `SportsAsyncImage` Fallback

- Handle null/blank URLs: show a `?` placeholder.
- On TV Leanback, use `ImageCardView` with fallback drawable.

### Back Navigation Preserved

- `SportsRepository` singleton state persists across player lifecycle.
- On TV, the same principle applies — keep selected event ID, loaded videos, active tab in a singleton so pressing Back from the player returns to exactly where the user was.

### MultiWindowPushStore

- Same as described in Phase 17 item 8 above. Copy the pattern.
