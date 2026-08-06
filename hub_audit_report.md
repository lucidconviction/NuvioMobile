# Nuvio Hub Audit Report & Execution Plan

Date: 2026-07-30
Scope: MagNutz, DeezeNutz, NuvioNutz (CinematicTuner), TeleNutz + in-app resources (addons, plugins, debrid)

---

## 1. Executive Summary

The four hubs are **fully wired to the app's player and navigation**, but none of them can reliably deliver a playable URL today. The root causes fall into three buckets:

1. **A shared playback-contract bug** — the debrid resolver returns `Success` even when no debrid is configured and no URL exists. Both NuvioNutz and DeezeNutz treat that as "playable" and launch the player with an **empty `sourceUrl`**, which produces a player error overlay. This is the #1 reason the hubs "don't work."
2. **Zero in-app content sources on a fresh install** — the app ships with **no built-in addons, no plugin repositories, and no working torrent engines**. Home, NuvioNutz, and DeezeNutz are empty shells until the user manually adds sources. Nothing is seeded.
3. **The only self-contained playback backend (MagNutz/TorrServer) is unreliable and unverified** — TorrServer add/stream fails at runtime; the fallback paths that depend on it (DeezeNutz) silently time out for 30 s and give up.

There is no single "killer bug"; the hubs need (a) the playback-contract fix, (b) seeded/real content sources, and (c) one working torrent→stream path. The plan below addresses all three.

---

## 2. How Playback Currently Works (reference)

`PlayerLaunch` (PlayerModels.kt) → `App.kt` stores it and navigates to `PlayerRoute` → `PlatformPlayerSurface` (PlayerEngine.android.kt:83).

- Player engine: **ExoPlayer (Media3 1.8.0)** with automatic fallback to **libmpv** (mpv-android 0.1.12). Engine picked by `androidPlaybackEngine` setting (Auto/ExoPlayer/Libmpv).
- URL schemes: `http(s)://` (direct, via OkHttp), `file://`, `torrent://` (sentinel → resolved to `http://127.0.0.1:8091/stream?...` via TorrServer), `telenutz://` (queue sentinel → re-resolved via TeleNutzRepository), HLS/DASH/SS/RTSP via mime inference.
- `torrentInfoHash` on a launch triggers the P2P effect (`PlayerScreenRuntimeEffects.kt:85-140`) which starts TorrServer and sets `p2pResolvedSourceUrl` to the localhost stream URL.
- **No guard for blank `sourceUrl`**: if a hub launches with `""`, ExoPlayer prepares an empty MediaItem, fails, Auto-falls-back to libmpv with `""`, and the error overlay shows. This is the failure mode you're seeing from the hubs.

---

## 3. Hub-by-Hub Analysis

### 3.1 MagNutz (torrent downloads) — Root cause of failure

**What it does:** Add magnet → start TorrServer binary → `addTorrent` to `http://127.0.0.1:8091/torrents` → poll stats → download to `filesDir/downloads/magnutz/<id>` → play local file. Also `streamTorrent()` (sequential, returns stream URL after 5% or 5 MB) and `searchTorrents()` (delegates to DeezeNutzTorrentSearch).

**Evidence that the infra is valid:**
- `libtorrserver.so` is a real statically-linked ARM64 ELF (41 MB), present in all 4 ABIs, packaged in the APK. Binary is fine.
- `P2pStreamingEngine.start()` copies it to `filesDir/torrserver/libtorrserver`, `setExecutable(true)`, launches via `ProcessBuilder`, and health-checks `/echo` on port 8091. Android 10+ W^X: app-private `filesDir` is still executable, so this approach is correct in principle.

**Likely failure points (need logcat to confirm — plan step 0):**
- **`addTorrent` HTTP contract mismatch.** `TorrServerApi.addTorrent` POSTs `{"action":"add","link":...,"save_to_db":false}` to `/torrents` and expects `{"hash":...}`. If the running TorrServer build expects the v3 API shape or rejects `save_to_db`, the call fails. The same shape is used everywhere (MagNutzRepository.android.kt:336, P2pStreamingEngine.android.kt:506), so a single wrong contract breaks all torrent paths.
- **`streamTorrent` bug:** `addTorrentToTorrServer(magnetUri) ?: infoHash` — when add fails, it falls back to using the bare infoHash as the "hash", then `getTorrentStatsFromTorrServer(hash)` returns null forever → 30 s timeout → `null`. DeezeNutz's fallback therefore hangs 30 s and gives up (DeezeNutzViewModel.kt:139-158).
- `startTorrentDownload`'s `waitForCompletion` is an unbounded `while(true)` that "force-completes" after 20 stall ticks (60 s) even with no file (`MagNutzRepository.android.kt:230-269`), marking a download Completed with no `localFilePath` → `playDownload` returns null (no `isPlayable`).
- The server is started with `--path <filesDir>/torrserver`; TorrServer writes a DB there. If two engine instances (MagNutz + P2P streaming) race on the same port/config dir, one kills the other (`killOrphanedProcess`).

**Bottom line:** MagNutz can't be trusted as the backend until we capture the actual TorrServer error from logcat (`P2pStreamingEngine` already logs `[server] ...` lines). Then either fix the API contract or replace the engine (see Phase 5).

### 3.2 DeezeNutz (recipe channels) — Root cause of failure

**What it does:** Recipe → `realSearch()` runs `DeezeNutzTorrentSearch.search()` (scraped indexes) **and** addon-catalog search in parallel, prefers torrent results. `playChannelItem()` → try debrid resolve on each candidate → fallback MagNutz `streamTorrent()` → fallback `onNavigateToStreams()`.

**Bugs (in order of severity):**

1. **Debrid resolution is silently bypassed (P0).** `playChannelItem` builds a synthetic `StreamItem(addonId = "deezenutz")` (DeezeNutzViewModel.kt:112). `StreamItem.isInstalledAddonStream` requires `addonId.startsWith("addon:")` (StreamModels.kt:76-77), and `DirectDebridPlaybackResolver.shouldResolveToPlayableStream` requires `isInstalledAddonStream` on **both** branches (DirectDebridResolver.kt:116, 118). So even with a working Torbox/Premiumize/RD account, the debrid loop **never resolves anything** and `playableDirectUrl` is always `""`.
2. **`Success` is treated as "has a URL" (P0).** `resolveToPlayableStream` returns `Success(originalStream)` whenever debrid isn't relevant/configured (DirectDebridResolver.kt:151-153) — it does **not** mean playable. DeezeNutz checks only `resolved is Success` (DeezeNutzViewModel.kt:118) then launches with `resolved.stream.playableDirectUrl.orEmpty()` → empty URL → player error. Same flaw in `resolvePlayableStreams` (DeezeNutzViewModel.kt:438-444) and **NuvioNutz** (CinematicTunerViewModel.kt:426).
3. **Torrent scrapers are fragile (P1).** `DeezeNutzTorrentSearch` hardcodes: 1337x (Cloudflare-protected → 403), RARBG (dead), apibay.org (flaky), YTS (only movies), ETTV (TV only), TorrentDownload. Generic HTML parser injects fake `seeds = 50` and `sizeBytes = 0`. So search results are often empty or junk; a channel can be created with no real playable content.
4. **No debrid configured + TorrServer down = nothing.** The two fallbacks both require a working backend.

### 3.3 NuvioNutz / CinematicTuner — Root cause of failure

**What it does:** TV-style EPG channels derived from **installed addons' catalogs**. Select channel → `fetchCatalogPage` → `EpgScheduler` → play current item via `StreamsRepository.load` + `DirectDebridPlaybackResolver`.

**Bugs:**
1. **Same `Success` ≠ URL bug (P0).** CinematicTunerViewModel.kt:421-441 iterates streams, accepts the first `Success`, and launches with `resolved.stream.playableDirectUrl ?: ""` (line 430). With addons but no debrid, HTTP streams are fine — but any `infoHash`-based (torrent) stream has no URL and still launches blank. Without debrid, the loop effectively always "succeeds" with nothing.
2. **No P2P/TorrServer fallback (P1).** Unlike DeezeNutz, `playCurrentItem` has no MagNutz fallback for torrent streams — it just navigates to the manual streams screen.
3. **Zero channels on fresh install (P1).** Channels = addon catalogs (CinematicTunerViewModel.kt:79-109). No addons installed → empty hub. There is **no built-in catalog source**.
4. **DeezeNutz channels are dead code.** `TunerChannelSource.DEEZENUTZ` is handled in `selectChannelById` (CinematicTunerViewModel.kt:196) and `loadDeezeChannelSchedule` strips a `deeze::` prefix — but **nothing ever creates a `TunerChannel` with that source**. `buildTunerChannelId` only produces `ADDON_CATALOG` ids. The DeezeNutz↔Tuner bridge is unwired.

### 3.4 TeleNutz (Telegram videos) — Root cause of failure

**What it does:** TDLib (`libtdjni.so` bundled, 4 ABIs) → auth via phone/QR/password → `SearchMessages` (video filter) over the account's own chats → progressive download to local file → ExoPlayer via `tdlib://` scheme (`TdlibGrowingFileDataSource`).

**Bugs / weaknesses:**
1. **Search returns nothing for most accounts (P0).** `useMessageDatabase = false` (TelegramTdEngine.kt:298) — TDLib's cross-chat `SearchMessages` is unreliable/returns nothing without the message DB. There is no `SearchPublicChats`/global search, and no curated channel list. A fresh account in zero chats → zero results.
2. **No channel configuration (P1).** `TdChannel` exists but is never referenced. `chatList = null` searches only the signed-in user's chats. There is no UI or config to add Telegram channels to search.
3. **Session churn (P1).** On every start without a `tdlib_session_ok` marker, it deletes `tdlib_db` + `tdlib_files` (TelegramTdEngine.kt:39-43); the marker is deleted on `AuthorizationStateClosed` (351). Sessions get re-authenticated frequently, and QR is rendered as plain text, not an image (TeleNutzScreen.kt:557-559).
4. **Playback gaps (P2).** `libmpv` cannot play `tdlib://` (only ExoPlayer's data source handles it — PlayerEngine.android.kt:276-287). Broken fallback URL `https://t.me/<displayTitle>/<id>` (TeleNutzRepository.kt:89). Thumbnails are never downloaded (`toTdMessage` reads local path that's always blank). `startProgressiveDownload` may return a path before any bytes exist (TelegramTdEngine.kt:162).
5. **Flavor compile risk (P1).** `tdlib-java` dependency is `full`-only (build.gradle.kts:398-405) but `TelegramTdEngine.kt` lives in `androidMain`, so the **playstore** distribution can't compile. Only the `full` build works.

---

## 4. In-App Resources Audit

### 4.1 Addons (Stremio protocol) — fully wired, but nothing shipped

- Complete, standards-compliant Stremio addon client: install by URL, `catalog`/`meta`/`stream`/`subtitles` resources, deep links (`stremio://`), `StreamParser` fully understands torrent streams (`infoHash`, `magnetUri`, `fileIdx`, `clientResolve`, `behaviorHints`).
- **But zero built-in addons.** `AddonRepository.initialize()` seeds nothing when storage is empty (AddonRepository.kt:66-69); no addon browser/directory exists; Home is empty without user-installed addons.
- **Addon catalogs automatically become:** Home rows, Discover, Search, Collections, **and NuvioNutz channels**. So installing 1–2 catalog addons immediately populates NuvioNutz and Home. This is the cheapest "make it feel alive" win.
- **Addon streams that carry `infoHash` (Torrentio/Comet/MediaFusion-type addons) are the debrid input.** Installed-addon streams use `addonId = "addon:..."` (StreamsRepository.kt:190), which is exactly the prefix the debrid gate requires. **NuvioNutz already benefits from this; DeezeNutz doesn't because it builds synthetic ids.**

### 4.2 Plugins (JS scraper runtime) — promising but unseeded

- Full JS/WASM scraper runtime (`PluginRuntime`, `JsRuntime`, `WasmBridge`) with host network/crypto bridges. Repositories are added by manifest URL; scrapers download JS code and execute `executeScraper(tmdbId, mediaType, season, episode)`. Results feed the streams screen (`toPluginProviderGroups`, `executeScraper` in StreamsRepository.kt:475-515).
- **Same seeding gap:** no built-in plugin repositories. Users must know a repo URL. There is a Supabase sync (`sync_push_plugins`) but the server table is empty.
- Plugins are TMDB-id driven; they are a viable in-app torrent/source engine **if** a repository exists (community "Comet", "MediaFusion", self-hosted Torrentio-style scrapers can be wrapped as plugins).
- Note: the playstore Android distribution uses `androidPlaystore/.../PluginRepository.android.kt` (a stub); the full runtime lives in `fullCommonMain`.

### 4.3 Debrid — Torbox + Premiumize only; RealDebrid dead

- Visible providers: **Torbox** and **Premiumize** (device-code OAuth). **RealDebrid is registered but `visibleInUi=false`** and its key can never become an active resolver (DebridProvider.kt:60-66, 94-100) — dead wiring, no UI to enter an RD key.
- **Premiumize sign-in is broken in this build:** `PremiumizeConfig.CLIENT_ID` is generated from the `PREMIUMIZE_CLIENT_ID` Gradle property (build.gradle.kts:138-149), and it's **empty** in this checkout → device auth throws "missing configuration" (DebridSettingsPage.kt:1497-1499). Fixable by setting the property.
- The gate flag `canResolvePlayableLinks` = toggle ON + at least one visible provider with a stored key (DebridSettings.kt:42-49). `DirectDebridStreamPreparer` auto-instantantiates up to 2–5 cached torrents in the background (add-only-if-cached for Torbox; RD is unconditional).
- **Because of the `Success` contract bug, debrid being OFF is indistinguishable from debrid failing.** Fixing the contract is prerequisite to any debrid-based hub path.

### 4.4 Net: content-source dependency matrix

| Hub | Needs addons | Needs plugins | Needs debrid | Needs TorrServer/TDLib |
|---|---|---|---|---|
| NuvioNutz (tuner) | Yes (catalogs) | No | For torrent streams | Optional fallback |
| DeezeNutz | Yes (fallback) | No | **Yes (broken gate)** | Yes (fallback, broken) |
| MagNutz | No | No | No | **Yes (broken)** |
| TeleNutz | No | No | No | **Yes (TDLib, degraded)** |

---

## 5. Cross-Cutting Root Causes (fix these first)

1. **`Success` ≠ playable URL.** `DirectDebridPlaybackResolver.resolveToPlayableStream` returns `Success(original)` when debrid isn't applicable (DirectDebridResolver.kt:146-161). Callers must check `result.stream.playableDirectUrl.isNotBlank()`. Fix the two hub call sites (DeezeNutzViewModel.kt:118, 438; CinematicTunerViewModel.kt:426) and, better, add a helper `DirectDebridPlaybackResolver.resolvePlayableUrlOrNull(stream, season, episode): String?` that returns null unless a non-blank URL exists.
2. **No content sources are seeded.** Fresh install → empty Home, empty tuner, empty plugin list. Seed default addon URLs (see Phase 1).
3. **DeezeNutz synthetic addon id skips the debrid gate.** Prefix with `addon:deezenutz` or relax the gate.
4. **TorrServer contract unverified.** Capture logcat first; then either fix the contract or replace the engine.

---

## 6. Execution Plan

### Phase 0 — Diagnose TorrServer on-device (P0, ~30 min)
- **Task:** Capture the actual failure.
  - `adb logcat -s P2pStreamingEngine:*` and `adb logcat | rg -i "torrserver|server|magnutz"`.
  - Reproduce: add a magnet in MagNutz; note whether `[server]` logs appear and what `addTorrent failed: <code>` / `TorrServer start failed` says.
  - Check the binary actually runs: `adb shell run-as com.nuvio.app 'sh -c "ls -l files/torrserver; files/torrserver/libtorrserver --help"'` (adjust path).
- **Deliverable:** A confirmed root cause (API contract mismatch vs. exec failure vs. port conflict). If the API contract is the issue, fix `addTorrent`/`getTorrentStats` JSON shape in **both** `TorrServerApi` (P2pStreamingEngine.android.kt:506) and `MagNutzRepository.android.kt:336` to match the actual TorrServer version's API.

### Phase 1 — Seed content sources (P1, ~1–2 hrs) — makes Home/NuvioNutz/DeezeNutz alive
- **Task 1a:** Add a small **built-in addon catalog list** in `AddonRepository.initialize()` (AddonRepository.kt:59-88): when `storedUrls.isEmpty()`, auto-install 2–4 well-known catalog addons (e.g. Cinema Unlimited/TMDB-style catalog addons, MDBList addon). Gate behind a first-launch flag so users can remove them. This instantly populates Home + NuvioNutz channels.
- **Task 1b:** Add a simple **"Browse Addons" sheet** in `AddonsScreen.kt` with a curated list of addon URLs (catalog addons + torrent addons) so install isn't manual-URL-only.
- **Task 1c (optional):** Seed the Supabase `addons` table so sync works for fresh profiles.
- **Verification:** Fresh install → Home has rows; NuvioNutz has channels; DeezeNutz addon-search returns items.

### Phase 2 — Fix the shared playback contract (P0, ~1 hr) — fixes "player opens empty"
- **Task 2a:** Add `DirectDebridPlaybackResolver.resolvePlayableUrlOrNull(stream, season, episode): String?` returning `result.stream.playableDirectUrl?.takeIf { it.isNotBlank() }` only on `Success`.
- **Task 2b:** CinematicTunerViewModel.kt:398-452 — use the helper; only launch when URL non-blank; else try P2P/MagNutz for torrent streams; else `onNavigateToStreams`.
- **Task 2c:** DeezeNutzViewModel.kt:99-161 and :409-462 — use the helper; treat no-URL as "try next candidate".
- **Task 2d:** Add a blank-URL guard in the player: if `sourceUrl.isBlank()` and no torrent/telenutz sentinel, show a "No playable source" error instead of the raw player error (PlayerScreenRuntimeState.kt:103 / PlayerScreenRuntimeUi.kt).
- **Verification:** Launching a non-resolvable stream shows a clean message, never a player error overlay.

### Phase 3 — Unblock debrid for the hubs (P1, ~1–2 hrs)
- **Task 3a:** DeezeNutz synthetic `StreamItem` → `addonId = "addon:deezenutz"` (DeezeNutzViewModel.kt:112) so `isInstalledAddonStream` passes and the local-torrent debrid branch works.
- **Task 3b:** Set `PREMIUMIZE_CLIENT_ID` (build prop) so Premiumize OAuth works; verify Torbox OAuth end-to-end via `DebridSettingsPage` device-auth dialog.
- **Task 3c (optional):** Re-enable RealDebrid (`visibleInUi=true`, key entry UI) if desired — the API client + `RealDebridProviderApi` already exist (DebridProviderApis.kt:194).
- **Verification:** Configure Torbox → DeezeNutz channel item with cached hash resolves to a direct URL instantly; NuvioNutz torrent stream plays.

### Phase 4 — Make DeezeNutz torrent search actually work (P1, ~1–2 hrs)
- **Task 4a:** Replace the fragile scraper stack in `DeezeNutzTorrentSearch.kt` with a TorrentSearch-style library (`com.github.prajwalch:TorrentSearch`) or a robust set of engines (apibay primary + Jackett/Prowlarr-compatible endpoints), returning real `seeds`/`sizeBytes`.
- **Task 4b:** Wire `searchTorrents` results straight into `realSearch`; surface seeds/size in DeezeNutz UI queue.
- **Task 4c:** Add the "Use Torrents" toggle to the recipe editor (planned in execution_plan_torrent_integration.md Phase 5).
- **Verification:** Creating a channel returns real torrents with seeds/sizes that resolve.

### Phase 5 — Stabilize MagNutz backend (P1, ~2–3 hrs, depends on Phase 0)
- **Option A (cheapest):** Fix TorrServer contract per Phase 0 findings; make `streamTorrent` fail fast (no 30 s hang) and harden `waitForCompletion` (bounded loop, real completion detection).
- **Option B (from execution_plan_torrent_integration.md Phase 3):** Replace TorrServer with a native engine (libretorrent) in `MagNutzEngine.kt`; sequential-download streaming. Larger effort, removes the binary dependency.
- **Task:** DeezeNutz fallback uses `streamTorrent` only when no debrid is configured (`canResolvePlayableLinks == false`), and surfaces progress to the player instead of a silent 30 s hang.
- **Verification:** Add a public magnet → downloads to `filesDir/downloads/magnutz` → plays from `file://`.

### Phase 6 — TeleNutz fixes (P2, ~2–3 hrs)
- **Task 6a:** Set `useMessageDatabase = true` (TelegramTdEngine.kt:298) and stop wiping the DB each session (keep the marker until logout). Re-test cross-chat search.
- **Task 6b:** Add channel search/discovery: `SearchPublicChats` + a configurable channel list (wired into the unused `TdChannel` model), so search targets curated channels.
- **Task 6c:** Render the QR as an image (encode `tg://login?token=` as a QR bitmap) instead of text.
- **Task 6d:** Fix thumbnail download (`DownloadFile` for `video.thumbnail`), fix the broken `t.me` fallback URL, and make the `libmpv` engine degrade gracefully for `tdlib://` (force ExoPlayer or show a message).
- **Verification:** Log in with a phone → search returns videos from the account's channels → progressive download plays in ExoPlayer.

### Phase 7 — NuvioNutz DeezeNutz bridge (P2, ~1 hr)
- **Task:** In `CinematicTunerViewModel.loadChannels()`, append one `TunerChannel` per DeezeNutz channel with `id = "deeze::<deezeId>"`, `source = TunerChannelSource.DEEZENUTZ` (making the existing `loadDeezeChannelSchedule` path live). Tuner then includes DeezeNutz channels in the EPG grid.
- **Verification:** Creating a DeezeNutz channel adds a tuner channel that plays its queue.

### Phase 8 — Regression & hardening (P2)
- Fix `while(true)`/unbounded loops with `isActive` (MagNutzRepository.android.kt:230, waitForCompletion) and completion detection.
- Add a unit test for the `Success`-vs-URL contract and for `DeezeNutzTorrentSearch` parsing.
- Rebuild `full` APK, fresh install, verify all four hubs.

---

## 7. Priority & Dependencies

| Phase | Description | Effort | Priority | Depends on |
|---|---|---|---|---|
| 0 | Diagnose TorrServer on-device | S | P0 | — |
| 1 | Seed content sources (addons + browse) | M | P1 | — |
| 2 | Fix Success≠URL playback contract | S | P0 | — |
| 3 | Unblock debrid for hubs | M | P1 | 2 |
| 4 | DeezeNutz real torrent search | M | P1 | 1 |
| 5 | Stabilize MagNutz backend | L | P1 | 0 |
| 6 | TeleNutz fixes | M | P2 | — |
| 7 | NuvioNutz ↔ DeezeNutz bridge | S | P2 | 2 |
| 8 | Regression & hardening | S | P2 | all |

**Recommended immediate sequence:** Phase 0 → Phase 2 → Phase 1 → Phase 3 → Phase 5 → Phase 4 → (6, 7, 8 at your pace).

## 8. Open Questions / Decisions Needed

1. **Built-in addons:** Which catalog addons do you want seeded by default? (I can suggest safe, public Stremio catalog addons.) Do you want them removable/reversible?
2. **TorrServer vs libretorrent:** Replace the binary engine (Option B) or fix the current one (Option A)? Affects Phase 5 scope.
3. **RealDebrid:** Do you want RD re-enabled (it's fully implemented but hidden), or keep Torbox+Premiumize only?
4. **Torrent search engine:** Use the `TorrentSearch` library (fast) or self-hosted Jackett/Prowlarr endpoints (reliable, needs a server)? Affects Phase 4.
5. **Telegram channels:** Do you have a list of channels you want TeleNutz to target, or should I add public-channel search?
6. **Premiumize client id:** Do you have a `PREMIUMIZE_CLIENT_ID` to set, or should Premiumize be dropped and Torbox be the primary debrid?
