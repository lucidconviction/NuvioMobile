# Nuvio Mobile — Version History & Knowledge Base

## Latest Build
```bash
export JAVA_HOME=/usr/local/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk
./gradlew :androidApp:assembleFull -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`

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
