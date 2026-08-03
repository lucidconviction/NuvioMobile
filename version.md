# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :androidApp:assembleFull -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

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
