# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :androidApp:assembleFull -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

### v0.25.3 — Critical Bug Fixes, Stalker Source Chips, Debounced Validation Persistence (September 10, 2026)

#### P0 Critical Bug Fixes
- **Dead overlay removed** — leftover dead-stream overlay at `IptvScreen.kt:926` deleted; dead streams now hidden via `StreamValidationStore.isKnownDeadSync()` filtering only
- **Hardcoded progress bar removed** — fake progress bar at `IptvScreen.kt:989` deleted; real scan progress comes from `StreamValidationController.scans` state
- **Dead token check fixed** — `StreamValidator.kt:101` now checks first 256 chars always instead of skipping short tokens
- **Platform search caching** — `VidNutzRepository` now caches platform YouTube search results via `cacheSearch()` before returning
- **Cache invalidation fixed** — `forceRefreshSubVideos` now invalidates all cached pages by prefix (`"$q|"`) instead of only pages 1-2

#### Source Chips — Stalker Support
- **Stalker sources in source chips** — `SourceChipsSection` now includes `stalkerAccounts` in channel counts and chip iteration

#### Stream Validation — Performance
- **Debounced persistence** — `StreamValidationStore.remember()` now schedules disk writes with a 2-second debounce, batching rapid probe results
- **Batched scan UI updates** — `StreamValidationController.scanSource` now updates `scans` state every 5 results instead of per-URL, reducing Compose recomposition

#### Add Source Sheet — Scrollable Tabs
- **Scrollable tab row** — Add Source bottom sheet tabs now scroll horizontally when there are many source types; removed fixed `weight(1f)` so labels can't overflow

#### Files Modified
- `IptvScreen.kt` — dead overlay removal, progress bar removal, Stalker source chips, scrollable add-source tabs
- `StreamValidator.kt` — dead token check fix
- `StreamValidationStore.kt` — debounced persistence
- `StreamValidationController.kt` — batched scan UI updates
- `VidNutzRepository.kt` — platform search caching, prefix-based cache invalidation

### v0.24.0 — RdNutz Portal Search Inline View, Free-User Gating, Parallel Fetch (September 7, 2026)

#### RdNutz Portal Search — Rewritten as Inline View
- **Removed nested ModalBottomSheet** — RdNutz portal search is no longer a separate bottom sheet. It now renders inline inside the existing source sheets (`NameSearchSourcesSheet` and `QuickChannelSourcesSheet`).
- **"Search RDNutz List" button** — added to the header of both the Name Search and Quick Channel sheets, visible only when user has access (`LicenseStatus.VALID` or `LicenseStatus.GRACE`).
- **Back button (←)** — header includes a back arrow that returns to the source list view.
- **Auto-trigger on open** — when the RdNutz search view opens, it automatically starts scraping portals with the search term (no manual "Refresh" tap required).
- **Rotator progress messages** — animated quotes like "Scourin' the wild nutz networks..." and "Huntin' for hidden gems across the web..." while scraping.

#### Portal Speed & Limits
- **Parallel fetches reduced to 6** — `MAX_PARALLEL_FETCHES` and `MAX_COUNT_PARALLEL` both set to 6 (was 48) to reduce server load and avoid rate-limiting.
- **Free users get 5 portals** — `maxPortals = 5` for sports-only, `maxPortals = 5` general, `maxPortals = 10` sports-only mode.
- **Donors get unlimited** — `LicenseStatus.VALID`/`GRACE` → `Int.MAX_VALUE` portals returned.
- **Search-term filter** — results are filtered by domain name (lowercase contains match) so "Premier League" only returns relevant portals.

#### Access Gating
- **Free users locked out** — if `licenseStatus` is not VALID or GRACE, the "Search RDNutz List" button does not appear at all.
- **When accessed without license** — shows a locked state with 🔒 icon and message: "This feature requires an active RDNutz key. Donate to unlock access."
- **hasAccess check** — derived from `licenseStatus in listOf(LicenseStatus.VALID, LicenseStatus.GRACE)`.

#### Portal Dedup & Add Flow
- **Dedup by URL** — portals deduplicated using `entry.url` (was fragile name-based matching).
- **ADD button per portal** — each portal card shows an ADD button; tapped portals show "ADDED" with green border.
- **Account limit check** — respects `canAddPortal(license, existing)` before adding; free users limited to 10 portals total.
- **Portal info preserved** — on add, `PortalAccountInfo` (expDate, maxConnections, activeConnections, status, isTrial) stored alongside account.

#### Files Modified
- `IptvScreen.kt` — `RdNutzSearchView` composable, `NameSearchSourcesSheet`, `QuickChannelSourcesSheet` restructured
- `PortalNutzScraper.kt` — `MAX_PARALLEL_FETCHES=6`, `MAX_COUNT_PARALLEL=6`, `searchTerm` filter in `presentEntries`, `scrape()` no longer takes `maxPortalsOverride` param

### v0.25.2 — AI Channel Search Integration, freellmapi (September 9, 2026)

#### AI-Powered Channel Discovery (SportNutz → LIVE tab)
- **Inline AI search bar** — appears in SportNutz event detail → LIVE tab with placeholder: "AI: e.g. 'ESPN channels in HD', 'my region, no duplicates'"
- **Natural language filtering** — type queries like "ESPN channels", "my region HD", "no duplicates" → LLM returns explanation + filtered channel list
- **Explanation card** — shows AI reasoning (2-3 sentences), latency, and suggested portal badges (unique provider groups)
- **Channel count badge updates** — LIVE tab header shows filtered count after AI search
- **Auto-clear on tab switch** — query/results reset when leaving LIVE tab

#### freellmapi Integration
- **Unified endpoint** — `https://freellmapi.com/v1` (configurable via `AiSettingsStore`)
- **Auto-router model selection** — hardcoded `"model": "auto"` in all requests, no user-facing picker
- **Free tier** — 34 providers, 635 models, 7.4B tokens/month across all models
- **Network layer** — uses existing `httpPostJson` (OkHttp) from `AddonPlatform.android.kt`, no new HTTP library
- **Settings toggle** — AI enabled by default (`AiSettingsStore.isEnabled()`), base URL configurable

#### Files Created
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/ai/AiModels.kt` — data classes (AiMessage, AiResponse, AiChannelSearchResult, etc.)
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/ai/AiSettingsStore.kt` — expect/actual for AI enabled + base URL (SharedPreferences/NSUserDefaults)
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/ai/AiClient.kt` — chat completion, streaming, connection test
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/ai/AiStore.kt` — global AI chat state (conversation history, rate limiting)
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/ai/AiChannelSearchStore.kt` — expect/actual for channel search
- `composeApp/src/androidMain/kotlin/com/nuvio/app/features/ai/AiSettingsStore.android.kt` — SharedPreferences impl
- `composeApp/src/androidMain/kotlin/com/nuvio/app/features/ai/AiChannelSearchStore.android.kt` — freellmapi call with event/channel context
- `composeApp/src/iosMain/kotlin/com/nuvio/app/features/ai/AiSettingsStore.ios.kt` — NSUserDefaults impl
- `composeApp/src/iosMain/kotlin/com/nuvio/app/features/ai/AiChannelSearchStore.ios.kt` — stub (to implement)
- `AI_INTEGRATION_PLAN.md` — full phased implementation plan

#### Files Modified
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/sports/SportsScreen.kt` — AI search bar + explanation card in LIVE tab; `userRegion` parameter; `allChannels` passed to AI for full context

### v0.25.1 — Quick Search Save/Delete, Dead Stream Filtering, Overlay Search Terms (September 9, 2026)

#### Quick Search Management
- **Save search terms** — SearchSection now has a bookmark icon in trailing icon; tapping it saves the current query to `ChannelQuickSearchStore`
- **Delete saved searches** — Each quick search chip now shows a ✕ button to remove it from saved terms
- **Visual save indicator** — Saved search terms show a filled bookmark icon in SearchSection; unsaved shows outline

#### Stream Validation
- **Hide known-dead streams** — All channel lists now filter out streams marked as dead by `StreamValidationStore.isKnownDeadSync()`
  - Main channel grid (Recommended Channels)
  - Quick Channel popup overlays (`QuickChannelSourcesSheet`)
  - MultiWindowCellOptions channel picker (`ChannelOverlay`)
  - MultiWindowCellOptions quick channel overlay (`QuickChannelMatchOverlay`)
- **Multi-token search** — Channel overlay search now matches against both channel name AND group/category

#### Overlay Menu Enhancements
- **Saved search terms in QuickChannelMatchOverlay** — Quick search chips displayed above search field; tapping one auto-fills search and filters results
- **Saved search terms in ChannelOverlay** — When browsing channels, saved search terms appear as chips above search field for quick filtering

#### Files Modified
- `IptvScreen.kt` — `SearchSection` with save button; `QuickChannelsSection` with delete on chips; dead stream filtering in display channels
- `MultiWindowCellOptions.kt` — Saved search chips in `QuickChannelMatchOverlay` and `ChannelOverlay`; dead stream filtering in `filtered` computations

### v0.25.0 — Portal Search Crash Fix, UI Labels, Quick Search Terms (September 9, 2026)

#### Bug Fixes
- **IPTV portal search NPE crash** — fixed null pointer exception in `IptvScreen.kt` when initializing remaining time state for portal license; changed from unsafe `remember(license) { mutableStateOf(...) }` to safe initialization in `LaunchedEffect` with null check

#### UI Changes
- **"PORTAL" → "RD NUTZ LIST"** — label in Add Source bottom sheet updated for consistency with RdNutz branding
- **"RD NUTZ LISTS ACTIVE" → "RdNutz Lists Active"** — license status banner text refined

#### New Features
- **Quick Search Terms** — saved search terms from `ChannelQuickSearchStore` now displayed as chips under Quick Channels section; tapping a chip triggers a channel search via `IptvRepository.setSearchQuery()`

#### Files Modified
- `IptvScreen.kt` — `QuickChannelsSection` extended with saved search terms UI; portal search NPE fix; label renames

### v0.24.1 — MusicNutz Download Removal, SportNutz Portal Search, Deeper/Faster Search (September 8, 2026)

#### MusicNutz — Downloads Removed
- **Removed download feature entirely** — deleted `MusicDownload` model, `MusicDownloadStore`, `MusicNutzDownloadStorage`, and all download UI from `MusicNutzScreen.kt`
- **Removed "Downloads" mode chip** — no longer appears in MusicNutz mode selector
- **Removed download buttons** — "Download All" from album detail, "DL" button from track rows, download options from playlist detail
- **Removed `MusicNutzDownloadStorage.downloadDirPath`** assignment from `MainActivity.kt`
- **Removed unused imports** — `okio`, `okhttp3`, `java.io`, `LinearProgressIndicator` from MusicNutzScreen

#### MusicNutz — Search Bar UX
- **Enter/Go key triggers search** — `KeyboardOptions(imeAction = ImeAction.Search)` + `KeyboardActions(onSearch)` added to search `OutlinedTextField` in both mobile and TV layouts
- **Instant search on Enter** — debounce reduced to 200ms (from 400ms) when user explicitly presses search key

#### SportNutz — "Search Portals" Button
- **Always visible** — "Search Portals" button added to LIVE tab header row (right side), visible even when channels exist for the event
- **Empty state button retained** — also shown in the "No IPTV channels found" empty state below the channel list
- **Fast path to VidNutz** — clicking sets `HubReturnStore.subScreen = "VidNutz"` and `VidNutzPendingSearch.query = "${awayTeam} vs ${homeTeam}"`, then opens VidNutz hub
- **Auto-save from SportNutz** — when a pending search arrives from SportNutz, it auto-saves to VidNutz saved searches (deduped by case-insensitive match) so the query appears in the saved searches list
- **`isFromSportNutz` flag** — added to `VidNutzPendingSearch` to distinguish portal-triggered searches from manual searches

#### IPTV — Deeper & Faster Search
- **Multi-token AND search** — search terms like "ESPN HD" now require ALL tokens to match (channel name or group/category), not just one
- **Group/category search** — text search now matches against both channel name AND IPTV group, doubling discoverable channels
- **QuickChannel enhanced matching** — `QuickChannelList.matches()` now splits displayName/aliases into keywords and matches any significant token (>= 2 chars) against channel name or group
- **Fuzzy keyword matching** — region bundles (US/UK/CA) and individual quick channels (ESPN, HBO, etc.) now find channels with partial matches like "ESPN HD", "Fox Sports 1", etc.

#### Portal Search — Fast Results & Background Continuation
- **Progressive results** — RdNutz portal search now shows results as they arrive during verification instead of waiting for all results
- **Background search continuation** — if user dismisses the portal search sheet before results complete, the search continues in the background
- **Auto-re-pop on reopen** — when user reopens the portal search for the same term, completed results appear immediately
- **Status indicator** — "X found · searching more..." banner with spinner shows when background search is still running

### v0.23.0 — IPTVNutz Playlist Channel Search (September 3, 2026)

#### "Your Playlists" Search
- The search bar in **Your Playlists** now searches **channels**, not just playlist cards.
- Typing filters the selected playlists' channels by **name or group** and shows a "Results in playlists (N)" list of tappable channel rows (logo, name, group, play arrow).
- Elements respect the currently **selected playlist(s)** (`selectedSourceIds`) when set; otherwise search across all installed m3u/xtream/stalker playlists.
- **Tap a row to play** the channel directly via the same `playChannel` path used by the channel grid.
- Reuses the existing single search field — this replaces the previous card-name-only filtering.
- Implemented for both **mobile** (`PlaylistsSection`) and **TV** (`PlaylistsTvSection`) layouts, sharing the new compact `PlaylistChannelRow` composable.

### v0.22.0 — VidNutz Caching, Search UI Consistency (September 1, 2026)

#### VidNutz Performance
- **Disk cache with TTL** — categories, searches, and hubs now cached to SharedPreferences/NSUserDefaults with 30-minute expiry; switching categories no longer reloads on every switch
- **Search cache** — up to 50 recent searches cached; repeat queries return instantly
- **Parallel hub loading** — hub sections loaded concurrently via `async/awaitAll` instead of sequentially
- **Pre-fetch adjacent hubs** — `VidNutzScreen` pre-fetches neighbor hub sections while current hub is visible
- **Instant category switching** — cached results shown immediately; background refresh when cache expired

#### UI Consistency
- **Tab labels unified** — "Saved" → "Saved Searches" across all three channel menus (MultiWindowGrid, PlayerPlaybackOverlays, IptvScreen)
- **Empty state wording** — "No saved searches" → "No saved searches yet" for consistency
- **IptvScreen search section** — now has full 5-tab layout (Channels, Quick Channels, Saved Searches, Favorites, History) matching TV mode

#### Fixes
- **VidNutzRepository compilation** — fixed mutable cache maps, `Json` import path, expect/actual for cache store (Android SharedPreferences + iOS NSUserDefaults)

### v0.21.0 — RdNutz Rebrand, Portal Keys/Limits, Auto-Updates, Addon Persistence (August 25, 2026)

#### Deployed (2026-08-25)
- Latest debug build `Nuvio-Mobile-2026-08-25.apk` published to **apps.rdnutz.us**.
- All prior mobile APKs removed from the site; Cloudflare cache purged. The auto-updater now points at this build.

#### Rebrand & Cleanup
- **PortalNutz → RdNutz TV** — the portal search tab is now labeled "RD NUTZ TV"; individual portal results/accounts are shown as **List 1, List 2** etc. (`listN` labels, backward-compatible `isPortalName` detection).
- **TeleNutz removed** — the TeleNutz feature and all its wiring (hub tile, navigation, `telenutz://` player routing, tdlib engine usage) were deleted.

#### Portal donation keys — fixed & hardened
- **Key format fixed** — keys are now the full base64url of `payload.signature`; the previous truncated 16-char keys never verified. Expiry `exp` is now **milliseconds** (seconds-based keys showed 1970 / "Expired").
- **Admin keys** — generator `--admin` flag produces unlimited-portal, effectively non-expiring keys (`isAdmin` in payload).
- **3-portal limit** for user keys; admin keys are unlimited. Hard expiry **erases all installed portals**.
- **Donation popup** — pressing ACTIVATE now shows payment info (USDT, PayPal, $8/month, "include email with donation") plus the key field.

#### Auto-updates
- Updater now pulls from **`https://apps.rdnutz.us/`** (dated-APK directory listing, `Nuvio-Mobile-YYYY-MM-DD.apk`), selecting the newest by date.
- **Last-alerted-date tracking** — users are only prompted when a newer-dated build exists than the last one they were told about (no repeated nagging).
- Removed the GitHub release API + DI providers.

#### Addon/plugin persistence
- Default addon set on first launch now matches the TV app (cinemeta, opensubtitles, hdhub, personalized MediaFusion token, webstream, torrentio, live-sport, torrentsdb, pengu, ytztvio, nodebrid).
- **Plugin repositories seeded by default** on first launch so plugins persist across installs.

#### Misc
- Removed version number footer from the RobbdeezeHubz screen.
- Removed `@RNutzNuvioChat` / `@RNutzNuvioBugs` from the About section (kept `@RnutzNuvioUpdates`).
- Admin reference added: `ADMIN CHEATSHEET.md` (bot info, key instructions, plans).

### v0.20.0 — Portal Paywall / Donation Key System (August 25, 2026)

#### Portal Paywall
- **Donation key gate** — Portal auto-discovery feature now requires a valid donation key to access. Users enter a `NVIO-XXXX-XXXX-XXXX` key after donating.
- **HMAC-SHA256 verification** — keys are signed with a shared secret (`Rdnutz`) and verified client-side. Invalid or tampered keys are rejected.
- **Device binding** — each key is locked to 1 device via a stable device fingerprint (UUID stored in SharedPreferences/NSUserDefaults). Transferring keys between devices is blocked.
- **Duration-based expiry** — keys expire after the purchased duration (default 30 days). Hard cut at expiry.
- **24-hour grace period** — keys show a warning 24 hours before expiry but remain functional.
- **Key input UI** — new "PORTAL ACCESS" section in the Portal tab of Add Source sheet with key input field, "ACTIVATE" button, and descriptive error messages.
- **License status display** — active keys show a green "Portal Access Active" banner with expiry date and remaining time. Grace period shows a yellow "Expiring Soon" warning.
- **Error messages** — clear messages for invalid format, expired keys, wrong device, and successful activation.

#### Key Generator (Admin)
- **`scripts/generate_portal_key.py`** — standalone Python script for generating donation keys. Supports custom durations, bulk generation, and environment-based secret override.
- **Key format** — `NVIO-XXXX-XXXX-XXXX` (19 characters, human-readable).
- **Admin cheatsheet** — `Docs/admin_cheatsheet.md` with full usage instructions and security notes.

#### New Files
- `PortalLicenseModels.kt` — `PortalLicenseKey`, `LicenseStatus` enum
- `DeviceFingerprint.kt` (expect) + Android/iOS actuals — stable per-device UUID
- `PlatformCrypto.android.kt` / `PlatformCrypto.ios.kt` — HMAC-SHA256 + base64url decode
- `PortalLicenseManager.kt` — key verification, storage, status checks
- `scripts/generate_portal_key.py` — CLI key generator

#### Modified Files
- `IptvStorage.kt` — added `loadLicense/saveLicense/loadFingerprint/saveFingerprint`
- `IptvStorage.android.kt` / `IptvStorage.ios.kt` — implemented license + fingerprint storage
- `IptvScreen.kt` — PortalForm gated behind license check
- `MainActivity.kt` — DeviceFingerprint initialization

### v0.19.0 — Unified Scored Sport Channel Matching + SportNutz Stream Validation (August 15, 2026)

#### Unified Scorer
- **One scorer, both entry points** — new `ChannelScorer` replaces the two divergent boolean matchers. The detail-panel "Watch Live" (`GameToChannelMatcher.matchChannels`) and the picker/autoplay (`EspnClient.findScoredMatchingChannels`) both score every candidate with the same weighted evidence model, so results never diverge.
- **Scored evidence, not first-predicate-wins** — team presence (full name → stripped → first word → US-league last token) is the strongest signal; EPG current program is a strong booster; league + generic-sports keywords surface weak hits; ambiguous words are penalized.
- **Weight table (`ChannelScorer`)** — both teams in name +100, one team +40, both teams in EPG +60, one team in EPG +25, league match +15, generic sports +8, ambiguous first word −20. `MatchType` (TEAM/LEAGUE/GENERAL_SPORTS) is derived from the top reason.
- **Normalizer + team expansion** — `ChannelText` (accent folding, `SD/HD/4K` strip) and `TeamNameKeys` (club prefix/suffix strip, guarded first word, `fc`/`ac`/`utd` maps).
- **Real-event aliases** — `TeamNameKeys` manual alias map so well-known shorthand matches the real broadcaster: Manchester Utd/Man City, Wolves, Spurs, Red Sox, Lakers, Yankees, Inter/AC Milan, Dortmund, Bayern, PSG, etc.
- **League-ID aliasing** — events arrive with ESPN path IDs (`eng.1`, `esp.1`, `ita.1`) while the keyword tables are keyed `EPL`/`SERIEA`; `GameToChannelMatcher.leagueKey()` maps ID → table key so real games get their league bonus (without it a live Premier League game scored like a generic channel).
- **`SportBroadcasterMap` retired** — deleted; its league/sport terms are covered by the scorer's keyword tables, removing dual-implementation drift.

#### EPG current-program signal
- **`buildCurrentEpgTitleLookup()`** — `channelKey → current title` from the loaded XMLTV cache, handed to the scorer via `currentEpgTitleFor`; a channel whose "now" program shows the matchup outranks an unrelated generic ESPN.
- **Lazy concurrent short-EPG** — `buildLazyEpgTitleLookup(candidates, cap=8)` plus two-pass `matchChannelsWithLazyEpg` / `findScoredMatchingChannelsWithLazyEpg`: score on name/league/generic first, then concurrently fetch current-program EPG (`ShortEpgCache.getOrLoad`) for the top ~8 candidates and re-score. Never blocks first paint on EPG for every channel.

#### Aggregation, dedup & provider grouping
- **URL dedup** — `scoreChannels` dedups by lowercased stream URL, keeping the highest-scoring copy; the same feed under M3U + Xtream + Stalker shows once.
- **Provider group** — `MatchedChannel.providerGroup` (`SourceType.label`: M3U/Xstream/Stalker) surfaced as a badge in the detail list and picker, keeping sources visually grouped while the region filter is preserved.

#### Picker & autoplay guardrails
- **`ChannelScorer.autoplayCandidate`** — only autoplays when the top score is ≥ 60 (both teams in name or both in EPG) and ≥ 20 ahead of the runner-up; otherwise the picker opens instead of silently playing a weak "ESPN (league-only)" hit.
- **`strChannel.isBlank()` no longer skips** — a fight night with no broadcaster hint still scores normally (league/generic/team evidence) and surfaces candidates.

#### SportNutz stream validation
- **Validate + hide dead streams when an event is chosen** — opening an event's detail shows all candidates immediately, then `StreamValidationController.resolveChannelsStatuses` live-probes them and drops the confirmed-dead; rows show **✓ Live / checking / offline** via new `SportsUiState.aliveByUrl`.
- **Known-dead filtered from picker/play** — `playOrShowPicker` (Sports + TeamDetail) and the live-games overlay filter out previously-verified-dead channels before autoplay/picker (`StreamValidationStore.isKnownDeadSync`).

#### Verification
- 29 sport-scoring unit tests (ChannelScorer/ChannelText/TeamNameKeys) including real-event cases that caught the alias + league-ID gaps; full suite 407 tests, 0 failures; fresh `assembleFullDebug` APK built and installed.

### v0.18.3 — Instant Portal Search + Portal Account Info (August 13, 2026)

#### Portal Nutz Search — Much Faster
- **Goal-oriented verify** — `PortalNutzScraper.verifyGoalOriented` uses a parallel worker-pool (24 concurrent) that stops the instant `maxPortals` (5 normal / 10 sports) portals are confirmed alive, instead of testing the whole pool and blocking on channel counts. Zero wasted work.
- **Present-fast, then fill** — results now appear immediately with `channelCount = 0`; channel counts are fetched in the background (`countPortals`, 48 parallel) and re-emitted so cards update in place with real numbers.
- **1-hour verified cache** — confirmed-working portals cached (max 100, LRU, TTL 60 min) keyed by `domain|user|pass`. Repeat searches within the hour are instant — no re-scraping, no re-verifying.
- **Bigger candidate pool** — shuffled pool raised 50 → 80 (`CANDIDATE_POOL_SIZE`).
- **Progress messages** — live "Testing nutz… X/Y" and "Counting channels… X/Y" updates during each phase.

#### Portal Account Info (expiration, connections, status)
- **`user_info` parsed on the same cheap verify call** — `verifyViaPlayerApi` now pulls `exp_date`, `max_connections`, `active_connections`, `status`, and `is_trial` with no extra HTTP request.
- **New `PortalAccountInfo` model** persisted on each `XtreamAccount` — survives restarts (`StoredXtreamAccount.info`), kept when channels are stripped for storage.
- **`refreshXtreamChannels` refreshes account info** — every channel refresh re-checks `player_api.php` (no `action`) and updates expiry/connections so cards stay fresh.
- **Portal search cards** now show a monospace line under the channel count: `Exp 12d · 2/4 conns` (red "Expired" when past due).
- **Playlist cards (TV + mobile)** for Xtream accounts show the same account-info line in primary color, turning red on `Expired`.

#### Crash Fixes
- **ANR fix — main thread CPU hog moved off thread** — `allQuickMatches` (200+ Quick Channels × 1000+ channels regex matching) moved from synchronous `remember{}` on the main thread to `LaunchedEffect` with `withContext(Dispatchers.Default)`. Was causing 107% CPU spikes that blocked input for >5s and triggered ANR kill.
- **NPE fix — `selectedQc!!` force-unwrap** — `MultiWindowCellOptions.kt` crash on rapid tap/dismiss of Quick Channel sheet. Replaced with `val qc = selectedQc ?: return`.
- **IndexOutOfBounds fix — `entries[idx]`** — safe access via `entries.getOrNull(idx)` in `PlayerPlaybackOverlays.kt` channel list rendering.
- **File I/O crash fix** — `IptvStorage.android.kt` `file.readText()`/`file.writeText()` wrapped in try-catch with Kermit logging.
- **EPG poll leak fix** — `rememberIptvEpgPrograms` loop guarded with `isActive` check.

#### Performance
- **QuickChannelMatchCache (singleton)** — match results cached app-wide so QC selection is instant (zero filtering) on subsequent taps, even across sheet dismissals.
- **StreamValidationStore async preload** — `dead_urls.json` loaded on `Dispatchers.IO` via `preload()` + `ensureLoadedSuspend()` instead of blocking the main thread. Background preload triggered early when Quick Channels sheet opens.
- **Batched status updates** — `resolveChannelsStatuses` calls `onUpdate` only twice (initial + final) instead of once per URL probe.
- **`resolveCachedStatuses` — zero network probes** — Quick Channels now use cached alive/dead status from `StreamValidationStore` without hitting any URLs. Fresh probes only happen on explicit retry.

#### Stability
- **StreamValidationStore `ensureLoaded()` made non-blocking** — synchronous `file.readText()` replaced with background coroutine on `Dispatchers.IO`. Callers get empty data on first access; real data populates when I/O completes.

### v0.18.1 — EPG Now/Next in Overlay, Instant Quick Channels, Overhauled Channel Switcher (August 10, 2026)

#### Player Overlay
- **EPG now/next on every channel row** — history, quick, favorites, and full channel list items in the overlay now show a red **NOW** + cyan **NEXT** strip below the channel name (Xtream channels only)
- **Favorites rendering fix** — the favorites-only mode (tap the star tab) now uses correct channel indices for URLs, logos, and switching (was showing wrong data for non-first pages)
- **Quick channels instant** — precomputed eagerly on playlist load so switching to the Quick tab shows results immediately with zero loading state
- **"Loading guide…" placeholder** — items show a subtle placeholder while EPG data fetches, then the now/next strip appears
- **Channel list auto-scroll to current** — overlay automatically scrolls the currently-playing channel into view on open

#### IPTV Everywhere
- **Channel nav arrows (◀ ▶) on all launches** — the hideaway prev/next buttons now appear when IPTV is launched from any screen (was previously limited to home tab only)
- **Auto-hide nav arrows** — arrows appear with controls, auto-hide after 3s of inactivity; each tap resets the timer
- **Clean channel switch** — switching channels now resets playback position so streams start fresh

#### Quick Channels Expansion
- **~60 new entries** — News (Newsmax, NewsNation, Fox Business, OAN, Real America's Voice, Newsy, Cheddar, ABC News Live, CBS News 24/7, NBC News Now, Fox Weather, CBN, i24, CGTN, NHK World Japan, Dubai One, GB News, TalkTV, LBC, STV News, Sky News/Fox News/ABC News Australia), Sports (NHL Network, Fox Soccer Plus, GolTV, Willow Cricket, TVG, Stadium, Pac-12, Fight Network, ESPN Deportes, Fox Deportes, TUDN, Viaplay, Eleven, Setanta, Sport Klub, Sky Sport Italia/DE, RAI, RMC, Canal+ Sport, Match TV, Nova Sports, Star/Sony India, ESPN Caribbean, Sportsnet World), Premium (FXM, Sony Movies, Movies!, AXN, Warner TV, Criterion, MUBI, Film4, Great! Movies), US Broadcast (ION, MeTV, Cozi, Buzzr, This TV, Decades, Rewind, Antenna TV, Grit, Bounce, UPtv, Pop, TV Land, Game Show Network, Telemundo, UniMas, TruTV, A&E, OWN, Freeform, E!, BBC America, VICE, CMT, Universal Kids), UK (Sky Arts/Crime/History/Witness, W, Dave, Gold, Drama, Yesterday, Challenge, Talking Pictures TV, Blaze, ITVBe, Movies4Men, London Live, 4Music, Now 80s, Kerrang!, Magic, The Box, Clubland, CBeebies, Milkshake!, POP), Canada (CPAC, TVA, Noovo, Vrak, AMI, ABC Spark, CTV Comedy/Drama, Discovery Science, Much, Stingray, YTV, Treehouse, Family, TVOKids), International (Zee, Colors, Star Plus, Sony, Hum, Geo, ARY, Aaj Tak, Times Now, Republic, NDTV, Asianet, Sun, Vijay, ETV, Gemini, Dunya, SAMAA, Bollywood Movies), Bay Area/Sacramento Locals (KMAX, KOFY, KTNC, KXTV, KCRA, KOVR, KTXL, KVIE, KFVT, KFSF)
- **New region tokens** — `LA` (Latin America) and `AU` (Australia)

#### IPTV Screen
- **EPG sources in "Your Playlists"** — added EPG guides show as cards with status (Loading/Loaded/Pending), refresh, and delete
- **Add EPG source UX** — new "EPG" tab in Add Source sheet: URL sub-tab for XML URL, FILE sub-tab for local file upload
- **Long-press EPG sheet** — long-press any channel card to open bottom sheet with up to 8 upcoming programs (time, title, description); currently-playing program highlighted
- **History clear button** — TV mode now has a "Clear" button next to "Recently Watched"

#### IPTV Engine
- **Faster channel switching** — buffer reduced from 50MB→24MB with tighter time thresholds (3s/30s forward, 1s/3s back); prioritizes time over byte thresholds
- **Telegram playback reliability** — `TdlibGrowingFileDataSource` waits up to 15s for downloaded files to become readable; `TeleNutzRepository` waits for 64KB minimum before returning URI
- **IPTV buffer tuning** — faster zapping between IPTV channels

### v0.18.0 — Live EPG, IPTV Persistence Overhaul, Portal Nutz Speedup, Stream Validation, Quick Channel Expansion (August 10, 2026)

#### Live EPG (`get_short_epg`)
- **`ShortEpgClient`** — fetches per-portal `player_api.php?action=get_short_epg&stream_id=…`; decodes base64 titles/descriptions and handles both unix-seconds and `yyyy-MM-dd HH:mm:ss` timestamps
- **`ShortEpgCache`** — in-memory `async` dedup keyed by `server|streamId` so N cards on one stream make a single HTTP call; scoped to the active account
- **Now/Next strips** — `EpgNowNextRow` shows a red **NOW** + cyan **NEXT** line on TV, mobile, and history channel cards plus home Quick Channel tiles (Xtream channels only)
- **EPG bottom sheet** — long-press a channel card opens a sheet with up to 8 upcoming programs (time, title, description)
- Renders nothing while loading or when a portal ships no EPG — never breaks the grid
- **EPG sources in "Your Playlists"** — added EPG guides now show as EPG cards (with refresh/delete) in both mobile `PlaylistsSection` and TV `PlaylistsTvSection`

#### EPG Stability
- **Stream buffer bound** — `EpgParser.parseXmltvStream` caps the working buffer (8MB) and trims it during the channel-header phase too, so oversized/garbage feeds fail gracefully instead of `OutOfMemoryError`
- **OOM guardrails** — `refreshEpg` catches `OutOfMemoryError` per-source and globally; `saveEpgCache` skips persisting when the feed exceeds 150k programs or an 8MB JSON payload (was crashing the app during `encodeToString`)

#### IPTV Persistence
- **Channel cache separation** — channel lists stripped out of SharedPreferences settings and stored in per-source cache files (M3U keyed by URL, Xtream/Stalker by account id); rehydrated on settings load so large playlists no longer stall startup
- **Cache invalidation** — caches cleared when a playlist/account is deleted; flushed after refresh and local-file import
- **Auto EPG on iptv-org** — adding the iptv-org M3U playlist now auto-adds the MJH EPG source and refreshes EPG
- **Uploaded EPG files** — add a local EPG file (`file://` sources) parsed via `EpgParser.parseXmltv`; removing the source deletes the stored file

#### Portal Nutz
- **Faster portal search** — 4s fetch timeouts, up to 24 parallel fetches, and byte caps prevent slow/rogue hosts from hanging the scan
- **Dedup by credentials** — portals deduplicated on the `url|username|password` triple across all sources
- **New sources** — Telegram channel posts, Reddit subreddit RSS, paste services (pastebin/pastes.dev/rentry raw), base64-encoded portal strings, and GitHub world-repo fallback channel files

#### Stream Validation
- **`StreamValidationController`/`StreamValidator`/`StreamValidationStore`** — multi-source scans with alive/dead tracking, 10-minute dead cooldown, and a persisted dead-URL list
- **Quick Channel status** — home Quick Channel sheets and MW channel sources resolve each match's live state (✓ Live / checking / Play) with working counts and group filtering + search

#### Quick Channels
- **~60 new entries** — News (Newsmax, NewsNation, Fox Business, OAN, Real America's Voice, Newsy, Cheddar, ABC News Live, CBS News 24/7, NBC News Now, Fox Weather, CBN, i24, CGTN, NHK World Japan, Dubai One, GB News, TalkTV, LBC, STV News, Sky News/Fox News/ABC News Australia), Sports (NHL Network, Fox Soccer Plus, GolTV, Willow Cricket, TVG, Stadium, Pac-12, Fight Network, ESPN Deportes, Fox Deportes, TUDN, Viaplay, Eleven, Setanta, Sport Klub, Sky Sport Italia/DE, RAI, RMC, Canal+ Sport, Match TV, Nova Sports, Star/Sony India, ESPN Caribbean, Sportsnet World)
- **New region tokens** — `LA` (Latin America) and `AU` (Australia) added to Quick Channel region filtering

#### Player
- **Quick Channels in switcher overlay** — new Quick tab resolves matches against installed playlists with live statuses; tap to zap to the first working source
- **IPTV nav arrows everywhere** — hideaway ◀ ▶ channel arrows now appear when IPTV is launched from any screen, not just the home tab

#### Docs
- **`Docs/short-epg-plan.md`** — implementation plan for the live `get_short_epg` EPG path

### v0.17.0 — BKFC/PFL/PowerSlap Scrapers, Quick Channel Region Bundles, SportNutz Overhaul, Prev/Next Player Arrows (August 8, 2026)

#### New Features
- **BKFC scraper** — `BkfcClient` scrapes `bkfc.com/events` for upcoming Bare Knuckle FC events; wired into BKFC collapsible tab + pill tab
- **PFL scraper** — `PflClient` scrapes `pflmma.com` homepage JSON-LD for PFL fight cards; wired into PFL collapsible tab + pill tab
- **PowerSlap scraper** — `PowerSlapClient` scrapes `powerslap.com/events` for PowerSlap events; new PowerSlap league pill tab added
- **Boxing scraper** — `BoxingSceneClient` fetches boxing schedule from Sync2Cal API; wired into BOXING collapsible tab + pill tab

#### SportNutz Overhaul
- **Featured Highlights grid** — merged trending news + league-specific highlights into a single grid (TV: 4-col, Mobile: adaptive), up to 12 videos
- **30-day Sync2Cal schedule window** — upcoming events filtered to next 30 days with proper sorting by start time
- **Real team names on Sync2Cal tap** — `splitEventTeams()` parses event titles for real home/away team names; video search uses actual names
- **LIVE/UPCOMING/ENDED badges** — collapsible tab headers show accurate counts per status; individual event cards tagged with LIVE/UPCOMING/ENDED
- **League-specific highlights** — each league tab fetches 4 highlight queries (e.g. "MLB highlights", "MLB top plays") and shows up to 12 videos
- **Fighting tabs first** — collapsible section order: UFC, PFL, BKFC, BOXING, PowerSlap, then rest alphabetically
- **Soccer leagues filtered** — USA.1, ESP.1, ITA.1, FRA.1, ENG.1 removed from collapsible display
- **10 trending highlights** — trending news section capped at 10 videos with diverse query coverage

#### Player Screen
- **Prev/Next video arrows** — SkipPrevious/SkipNext buttons added to center control bar; overlay ◀▶ arrows on left/right edges for queue navigation
- **Auto-advance queue** — `yt://` protocol for YouTube video queues; resolves stream URLs on-the-fly when navigating prev/next/auto-advance
- **All SportNutz video queues** — featured highlights, search results, event detail videos all pass queue for prev/next navigation

#### Quick Channels
- **Region sub-channel bundles** — "US Channels", "CA Channels", "UK Channels" entries with word-boundary regex matching on channel name + category
- **`QuickChannelList.matches()`** — dedicated matching function with region token lists and `\b` word-boundary for short tokens
- **All callers updated** — HomeQuickChannelsSection, IptvScreen, MultiWindowCellOptions now use `QuickChannelList.matches()`

#### Default Addons
- **First-launch seeding** — `AddonRepository.initialize()` seeds cinemeta, opensubtitles, hdhub, mediafusion, torrentio on first ever launch
- **First-launch flag** — `AddonStorage.hasSeededDefaultAddons()`/`markDefaultAddonsSeeded()` prevents re-seeding

#### TeleNutz
- **QR code login removed** — login via QR code option removed from auth screen; WaitQrCode state handled as empty

#### v0.13.0 — MagNutz Removal, Quick Channels, MultiWindow Overhaul, BKFC, Timezone Fixes (August 4, 2026)

#### Hub Changes
- **MagNutz removed** — entire torrent download manager deleted (TorrServer-dependent, unreliable)
- **MultiWindow auto-next** — `STATE_ENDED` listener on each ExoPlayer auto-removes completed streams
- **MultiWindow hideaway arrows** — left/right arrow buttons per cell swap content with adjacent occupied slot
- **MultiWindow channel picker** — "CH" button on each cell opens inline channel browser bottom sheet
- **Quick Channels on home screen** — new `HomeQuickChannelsSection` with region filter tabs (All, US, UK, CA, Bay Area, Premium, Sports, News); taps resolve against IPTV channels via alias matching
- **IPTV Favorites removed from home screen** — replaced by Quick Channels row

#### SportNutz
- **BKFC added** — Bare Knuckle Fighting Championship added to ESPN API sports, repository leagues, and quick league pills (next to UFC)
- **Live tab refinements** — league group badges show live count only; `MatchCard` properly tags LIVE / UPCOMING / FINAL

#### Timezone Fixes
- **ESPN event times** — `extractTime12h` now applies device timezone offset to UTC timestamps
- **IPTV clock widget** — `TvClock` shows device-local time
- **LiveGame today-filter** — date computation shifted from UTC epoch to local time
- **DaddyLive times** — hardcoded +4h ET replaced with `TraktPlatformClock.localTimezoneOffsetMs()`

#### Quick Channel List
- **Reorganized** — 187 entries sorted into 10 section headers (News, US Sports, UK Sports, CA Sports, Multi-Region Sports, US Premium Movies, UK Networks, CA Networks, US Entertainment, SF Bay Area Local)

#### Quick Channels
- **Channel source popup** — tapping a Quick Channel on home screen now opens a bottom sheet with matching IPTV channel sources instead of playing the first match directly
- **Bay Area expanded** — 16 individual Bay Area local channels with full call signs and aliases (KTVU, KPIX, KGO, KRON, KNTV, KQED, KBCW, KICU, KDTV, KTSF, KTVU Plus, NBC Bay Area News, KPJK, KRCB, KCSM, KEMO)

#### MultiWindow
- **Tabbed channel picker** — "CH" button on VideoCell now opens Channels/History/Favs/Quick tabbed overlay with search
- **Quick Channels tab** — region-filtered Quick Channels directly in the MW channel picker

#### SportNutz
- **DaddyLive channel popup** — tapping a DaddyLive event now shows a bottom sheet with available channel sources instead of playing directly
- **BKFC league tab** — accessible from quick league pills next to UFC

#### Player Screen
- **IPTV channel navigation arrows** — hideaway ◀ ▶ buttons on player screen when watching IPTV from home screen (auto-hide with controls)
- **CH & History overlay fix** — channel/history buttons now work when launching IPTV from home screen; added `IptvRepository` fallback for missing channel list data
- **Live Games overlay** — only shows live events (DaddyLive filtered to `it.isLive`); channel picker now properly switches to selected IPTV channel

#### Pin to Quick Channels
- **Pin button** — star (★/☆) button on TV and mobile channel cards; pinned channels appear at top of Quick Channels list (session-scoped)

#### SportNutz Highlights
- **Past highlights** — up to 8 highlight videos shown in all-live mode (was 3); TV mode shows up to 8 (was 2)
- **Trending highlights** — already visible in league tabs at bottom of Page1Live

#### Bug Fixes
- **TorrServer execution on Android 13** — uses `/system/bin/linker64` to bypass `noexec` mount on app data directories
- **IPTV player channel data** — fallback to `IptrRepository.getAllChannels()` when `PlayerLaunch` lacks channel list

#### Settings
- **Telegram groups updated** — footer now shows @RnutzNuvioUpdates, @RNutzNuvioChat, @RNutzNuvioBugs (removed GitHub link)
- **Version display** — simplified to show version name only

#### Internal
- **Telegram API credentials updated** — new api_id/api_hash for TeleNutz
- **Backup module removed** — TeleBackup, DVR, SitBack, DeezeNutz, CinematicTuner all deleted

### v0.12.0 — TeleNutz Restore, Channel Intelligence, Custom Groups, SportNutz Polish, Global MW Overlay (August 2, 2026)

#### Hub Changes
- **TeleNutz restored, CinematicTuner removed** — `HubSubScreen` enum/`HubItem`/`when` branches reverted to show TeleNutz instead of CinematicTuner
- **TeleNutz all-videos-not-playing fixed** — removed `DisposableEffect` that closed TDLib engine on screen dispose (was cancelling progressive downloads mid-playback); engine `start()` made idempotent via `closed` flag

#### Quick Channels & PPV
- **PPV category added** — new "PPV" tab in Quick Channels filter (both Home Screen and IPTV Control Center). 11 PPV channels added covering US/UK/CA (Sky Sports Box Office, BT Sport Box Office, UFC PPV, WWE PPV, Boxing PPV, DAZN PPV, PPV 1-5)
- **Bay Area tab synced** — Home Screen Quick Channels now also has "Bay Area" tab (was missing vs IPTV Control Center)

#### Channel Intelligence (Phase 1)
- **Smart auto-categorization** — `ChannelClassifier` with 105+ keyword rules across 12 categories (News, Sports, Premium, Entertainment, Kids, Music, Documentary, PPV, Local, Shopping, Religious, Adult). Channels missing M3U `group-title` now appear under the correct category chip via `IptvChannel.resolvedGroup`
- **Logo text fallback** — channel cards show a styled first-letter avatar when no logo URL exists (instead of generic LiveTv icon)

#### Custom User Groups & Pinned Channels (Phase 2)
- **Pinned Quick Channels** — pin button on every channel card (both TV and mobile modes). Pinned channels appear in a dedicated horizontal row below Quick Channels on both Home Screen and IPTV Control Center. Persisted via `IptvStorage`
- **Custom User Groups** — "My Groups" section with create/rename/delete. Add channels to groups via overflow menu on channel cards or "+ Add channel" sheet inside each group. Group channels display as expandable sections
- **Auto-refresh** — `IptvRepository.dataVersion` StateFlow ensures pin and group changes appear immediately
- **Delete confirmation** — both TV and Mobile playlist sections show a confirmation dialog before deleting any M3U/Xtream/Stalker playlist

#### TeleNutz UX (Phase 3)
- **Search history** — last 10 searches saved to `TeleNutzStorage`, shown as dropdown when search field is focused. "Clear history" option included
- **Cancel download** — active downloads now show progress percentage + red "Cancel" button
- **Bookmarks/Downloads tabs** — existing tabs persist and work

#### MultiNutz
- **Menu bar hideaway** — header + pill row auto-hides after 5 seconds. Tap the grid area to show it again without blocking video cell clicks
- **Global MW overlay** — floating red "MW N" pill at bottom-center visible on ALL screens (player, settings, hub, etc.). Navigates to MultiNutz on tap

#### Settings
- **GitHub removed, Telegram added** — settings footer shows app version + "Connect with us on Telegram" with clickable @RnutzNuvioUpdates, @RNutzNuvioChat, @RNutzNuvioBugs
- **Hub version footer removed** — version text removed from RobbdeezeNutz hub grid

#### Update Checker
- **Repo changed** — `AppUpdater` now checks `robbdeeze/nuvio` releases instead of `lucidconviction/NuvioMobile`

#### SportNutz
- **BKFC added** — Bare Knuckle Fighting Championship added to leagues list and quick league pills
- **Finished events filtered** — `STATUS_FINAL` events excluded from Live tab. Only live and upcoming events shown
- **Duplicate menu bar removed** — bottom `CategoryChipsBar` in mobile mode removed; only the top league pill bar remains
- **Standings auto-load** — `Page3Standings` initializes from `uiState.selectedLeague` so standings load when navigating from a league pill

#### Fixes
- **MW add-to-slot crash** — navigation to Multi view deferred via `LaunchedEffect` instead of calling `onMultiWindowAdded` inside the slot callback

### v0.11.0 — Hub Cleanup, Broken Hub Fixes, EPG Timezone & Gestures, Multinutz Features, Sync2Cal, Favorite Button (July 31, 2026)

[Previous entries remain unchanged...]
