# Execution Plan: IPTV Channel Intelligence & TeleNutz Enhancements

## Phase Overview

| Phase | Effort | Risk | Value | Status |
|-------|--------|------|-------|--------|
| **1 — Channel Classifier** | Low | Low | Fills missing group data | Approved |
| **2 — Customizable Quick Channels** | Low | Low | User customization | Approved |
| **3 — TeleNutz UX** | Medium | Low | Better TeleNutz experience | Approved |
| **4 — EPG Grid View** | High | Medium | Major UI feature | Deferred |
| **5 — TeleNutz Subscriptions + Background Audio** | High | Medium | Content discovery | Deferred |

---

## Phase 1: Channel Classifier

**Problem:** M3U `group-title` is often missing, inconsistent, or in a foreign language. Channels with no group don't appear in any category chip and are lost in the "Other" bucket.

**Solution:** `ChannelClassifier.kt` exists. Wire it into `IptvRepository.getAllCategories()` and the display layer.

**Files:**
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/iptv/ChannelClassifier.kt` (already exists)

**Changes:**
1. Add `IptvChannel.resolvedGroup` extension that returns `group ?: ChannelClassifier.classify(name)`
2. Use `resolvedGroup` in `IptvRepository.getAllCategories()` so missing-group channels appear in correct category chips
3. Use `resolvedGroup` in IptvScreen group-by display

**Logo fallback — Deferred.** `iptv-org` logo URLs change frequently. Hardcoding 200+ URLs would create maintenance burden. Instead, improve the mini-card fallback to show initials more prominently.

---

## Phase 2: Customizable Quick Channels

**Scope reduction:** Skip storage persistence for this phase. Quick channels are session-scoped.

### 2.1 Pin as Quick Channel

**Files:**
- `QuickChannelList.kt` — Add in-memory add/remove methods
- `IptvScreen.kt` — Add "Pin as Quick Channel" / "Unpin" action in channel context menu (long-press or "…" menu)

**Changes:**
1. Add `mutableStateListOf<QuickChannel>` for user-pinned channels in `QuickChannelList`
2. Add `pin(qc: QuickChannel)` / `unpin(displayName: String)` methods
3. Add "Pin as Quick Channel" context menu item on any IPTV channel card
4. `HomeQuickChannelsSection` and IPTV screen's `QuickChannelsSection` show pinned + default channels merged

### 2.2 Custom User Groups — Deferred

Requires storage persistence and drag-to-group UI. Revisit after TeleNutz UX phase.

---

## Phase 3: TeleNutz UX

### Priority: 3.1 → 3.2 → 3.3

### 3.1 Saved/Bookmarked Tab (High Priority)

**Files:**
- `TeleNutzScreen.kt` — Add "Saved" tab

**Changes:**
1. Add a new tab in the TeleNutz screen filter row: "Saved"
2. Show `TeleNutzStore.getBookmarks()` items in a grid/list when tab is active
3. Each item shows thumbnail, chat title, date, and "Play" / "Unbookmark" actions

`TeleNutzStore.getBookmarks()` already exists — this is a pure UI wiring task.

### 3.2 Download Management Tab (Medium Priority, after Saved tab)

**Files:**
- `TeleNutzScreen.kt` — Add "Downloads" tab

**Changes:**
1. Add "Downloads" tab next to "Saved"
2. Show `TeleNutzStore.getDownloads()` items with progress bars, status badges (Downloading/Downloaded/Error)
3. Add pause/resume/delete actions per item
4. Show total download count in tab label

### 3.3 Better Search UX (Lower Priority)

**Files:**
- `TeleNutzScreen.kt` — Search history dropdown, search suggestions

**Changes:**
1. Show recent search terms as chips below the search field
2. Add debounced search-as-you-type suggestions
3. Tap a history chip to re-search

---
