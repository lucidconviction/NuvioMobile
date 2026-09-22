# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :androidApp:assembleFull -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

### v0.27.0 — Sports Screen Enhancements + Nett_Smutt_3.0 / XXX_2 M3U Playlists (September 21, 2026)

#### Sports Screen Enhancements
- **External streams** — `ExternalStreamsSection` with 2-column grid (`chunked(2)`), green "LIVE" tag on thumbnails when `event.isLive`, date/time overlay via `formatExternalEventDate()`, `onOpenExternalUrl` callback threaded through `RobbdeezeNutzHubScreen` → `SportsScreen` → `Page1Live`
- **SportNutz categories** — `SportNutzCategoriesSection` (2 columns per category row), refresh button calls `loadSportNutzCategories()`, "View More" routes to VidNutz via `HubReturnStore.subScreen = "VidNutz"` + `VidNutzPendingSearch.query`
- **Leagues section unified** — priority leagues (NFL, NBA, CBB, CFB, WNBA, NHL) always appear first in order, even when empty; all use identical expandable card layout with live count badge
- **UFC** — green live tag only when `event.isLive == true`; upcoming shows countdown; completed shows FINAL
- **Return-to-list** — `LaunchedEffect(isEnded)` in `PlayerScreenRuntimeEffects.kt`
- **UFC date range** — expanded to 180 days in `SportsRepository.loadAllLiveEvents()`
- **`loadSportNutzCategories()`** — called in `SportsRepository.warmupSports()`
- **Fixed brace mismatch** in `SportsScreen.kt` leagues section
- **`isLive` extension property** — moved from inside `ExternalStreamsClient` to top-level in `ExternalStreamsModels.kt`

#### IPTV — Nett_Smutt_3.0 & XXX_2 Playlists (opt-in)
- **12 Python scrapers pushed to GitHub** — crazyshit, usacrime, kaotic, livegore, theync, worldstar, youporn, daftporn, inhumanity, nothingtoxic, zfilmzoriginals, horriblevideos, efukt (with shared `core.py`)
- **`build_nett_smutt_3.py` / `build_xxx_2.py`** — runners emit `Nett_Smutt_3.0.m3u` and `XXX_2.m3u`
- **`.github/workflows/build-m3u.yml`** — daily `0 3 * * *` cron, commits generated M3U files
- **`IptvRepository.kt`** — `NETSMUTT_3_M3U_URL`/`XXX_2_M3U_URL` constants, `addNettSmutt3Playlist()`/`addXXX2Playlist()` + `has*()` helpers, `PREDEFINED_M3U_PLAYLISTS` list (opt-in, no auto-seed)
- **`IptvScreen.kt` PlaylistsSection** — "+ Nett Smutt 3.0" and "+ XXX_2" quick-add TextButtons (shown only if not already added)

### v0.26.0 — IPTV/ExoPlayer Overhaul, Quick Channels Rewrite, Auth Bypass, Sports TV Logos (September 17, 2026)

#### IPTV Navigation & Persistence (Items 1, 2, 8, 9, 10)
- **Prev/Next channel** — wired `onPrev`/`onNext` to `switchIptvChannel` when `parentMetaId == "iptv"`
- **Search persistence** — added `loadSearchQuery`/`saveSearchQuery`, `hasSeededDefaultM3u`/`markDefaultM3uSeeded`, `loadLastRefresh`/`saveLastRefresh` to `IptvStorage` (expect + android/ios actuals)
- **Background refresh** — `hydrateChannels` triggers background refresh; `scheduleRefresh` and `refreshM3uPlaylistInternal` added; `saveLastRefresh` called on M3U/Xtream/Stalker refresh
- **VOD URL resolution** — `buildVodUrl` returning `"tsUrl|m3u8Url"` and `resolveVodStream` in `XtreamClient.kt`; VOD/series URL construction uses `.m3u8` fallback
- **Default M3U seeding** — `NETSMUTT_M3U_URL`/`NETSMUTT_M3U_NAME` constants; `seedDefaultM3uIfNeeded` called from `ensureLoaded`

#### Cast & Download (Items 5, 6)
- **Cast** — `onCastClick` wired to `PlayerControlsShell`/`PlayerHeader`; `showCastSheet` state added to `PlayerScreenRuntime`; `CastBottomSheet` rendering in `RenderPlaybackOverlays`
- **Download** — `onDownloadClick` wired to `DownloadsRepository.enqueueFromStream` via `PlayerControlsShell`/`ProgressControls`

#### MultiNavFAB & Portal Tab Cleanup (Items 3, 4)
- **MultiNavFAB** — created `MultiNavFAB.kt` in hub package; added to all hub sub-screens; fixed alignment using `Box` with `contentAlignment`
- **Portal tab** — removed "RD NUTZ LIST" tab and `PortalForm` branch from `IptvScreen.kt`

#### Quick Channels Rewrite (Item 11 + Custom)
- **CustomQuickChannelStore** — new expect/actual store (Android/iOS) for persisting user-custom QuickChannels
- **QuickChannelList** — added `customChannels` list, `addCustomChannel()`, `removeCustomChannel()`, `getCustomChannels()`, `setCustomChannels()`; `all` getter now includes custom channels
- **QuickChannelsSection** — rewritten in `IptvScreen.kt`: removed "Quick Searches" saved terms pills, added "Custom" tab, add/delete custom channel UI with dialog
- **Cleanup** — removed all `ChannelQuickSearchStore` references from `MultiWindowCellOptions.kt`, `MultiWindowGrid.kt`, `PlayerPlaybackOverlays.kt`

#### Sports TV Layout & Final Score Search (Items 7, 11)
- **Final score search** — `navigateToVidNutzSearch` uses `"date + event name + game highlights"` format (e.g., `2026-09-16 Lakers vs Celtics game highlights`); keyword NOT selectable, background execution
- **Sports TV grid** — added Quick Channels section with channel logos to `SportsTvMode` via `QuickChannelsSection(showLogos = true)`
- **Nett Smutt** — added `addNetSmuttPlaylist()` and `hasNetSmuttPlaylist()`; "+ Nett Smutt" install button alongside "+ iptv-org"

#### Auth Bypass / Hide Nuvio Account
- **Removed Auth screen** — gate logic auto signs in anonymously (`AuthRepository.signInAnonymously()`) instead of showing `AuthScreen`
- **Removed** `AuthScreen` import, `AppGateScreen.Auth` enum value, and Auth case from render block in `App.kt`
- **Settings** — `showAccountSection = false` in both `settingsRootContent` calls; `SettingsPage.Account -> Unit` branches already present

#### Compilation Fixes
- Fixed `CompanionConnection` expect/actual: added `create()` factory method; implemented in Android and iOS actuals
- Fixed `IptvRepository.kt`: changed `a.url` → `a.server` for XtreamAccount/StalkerAccount
- Fixed `PlayerControls.kt`: added `onDownloadClickHandler` to `ProgressControls`; used `Icons.Rounded.Download` directly
- Fixed `PlayerScreenRuntimeUi.kt`: removed `playableDirectUrl` from StreamItem constructor; fixed `activeLogo` → `logo`; changed `CompanionConnection()` → `CompanionConnection.create()`
- Fixed `SettingsScreen.kt`: added `SettingsPage.Account -> Unit` branches in 3 places (tablet `when`, phone `when`, `openSearchTarget` inner `when`)

### v0.25.8 — Portal-Add Crash Fix, RdNutz Movies & Series Title Update (September 16, 2026)