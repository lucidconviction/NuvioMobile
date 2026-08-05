# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :androidApp:assembleFull -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

### v0.13.0 — MagNutz Removal, Quick Channels, MultiWindow Overhaul, BKFC, Timezone Fixes (August 4, 2026)

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
