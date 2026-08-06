# Cross-Project Feature Analysis & Optimization Report

> **Target:** RNutz Nuvio (Nuvio_Robbdeeze)  
> **Source Projects Analyzed:** INTEL · Nuv MObile · Nuvio secrets · live-sport-plugin-main · Extreme-InfiniTV · Apex_Streamer  
> **Date:** 2026-08-03

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Source Project Snapshots](#2-source-project-snapshots)
3. [New Features to Port](#3-new-features-to-port)
4. [Feature Optimizations for Existing Nuvio Features](#4-feature-optimizations-for-existing-nuvio-features)
5. [Phase 1 — P2P Streaming Engine (from Nuv MObile/LibreTorrent)](#5-phase-1--p2p-streaming-engine-from-nuv-mobilelibretorrent)
6. [Phase 2 — SmartTube-Style Player Enhancements](#6-phase-2--smarttube-style-player-enhancements)
7. [Phase 3 — Extreme-InfiniTV Feature Parity](#7-phase-3--extreme-infinitv-feature-parity)
8. [Phase 4 — Apex Streamer Feature Parity](#8-phase-4--apex-streamer-feature-parity)
9. [Phase 5 — Live Sports Plugin Deep Integration](#9-phase-5--live-sports-plugin-deep-integration)
10. [Phase 6 — Telegram Ecosystem Deepening](#10-phase-6--telegram-ecosystem-deepening)
11. [Phase 7 — Performance & Code Optimizations](#11-phase-7--performance--code-optimizations)
12. [Appendix: Quick-Win Items](#12-appendix-quick-win-items)

---

## 1. Executive Summary

### The Opportunity

Nuvio is already the most feature-rich app in your ecosystem — it combines an IPTV player, multi-view grid, sports hub, VOD with addons, debrid, torrent streaming, Telegram content, and music. But six sibling projects contain features that could be ported, optimized, or cross-pollinated.

### The Six Source Projects at a Glance

| Project | Type | Key Strengths for Nuvio |
|---|---|---|
| **INTEL** | Research docs | Live sports plugin architecture; CORS proxy resolver strategy; AlterSend P2P design |
| **Nuv MObile** | Reference apps | LibreTorrent (P2P engine); SmartTube (player UX, SponsorBlock, AFR); TelePlay (Telegram video); DrKLO Telegram (full MTProto) |
| **Nuvio secrets** | Planning docs | Hub design philosophy; Magnutz tablet detail view specs; Cloudstream addon integration research |
| **live-sport-plugin-main** | Live sports addon | Multi-source sports aggregator; circuit breaker resilience; stream scoring; CORS proxy; YAML scrapers |
| **Extreme-InfiniTV** | IPTV player (desktop) | EPG schedule grid; multi-playlist sidebar; VOD/series library w/ poster grids; PiP; TV D-pad nav |
| **Apex_Streamer** | IPTV player (Android) | Multi-view layouts; Stremio addon integration; TMDB + sports hub; debrid orchestrator with failover; PiP + Cast |

### What Nuvio Already Has vs What's Missing

| Capability | Nuvio | Extreme-InfiniTV | Apex_Streamer | Priority |
|---|---|---|---|---|
| IPTV (M3U/Xtream/Stalker) | ✅ | ✅ | ✅ | — |
| Multi-view (MultiNutz) | ✅ (9 streams, 18 layouts) | ❌ | ✅ (4 layouts) | — |
| EPG | ✅ (now/next overlay) | ✅ (full schedule grid) | ⚠️ (basic) | Medium |
| VOD (Movies/Series) | ✅ (addons) | ✅ (Xtream VOD) | ✅ (addons + Xtream) | — |
| Sports hub | ✅ (ESPN + YouTube) | ❌ | ✅ (DaddyLive + matcher) | — |
| Debrid | ✅ (RD/TB/Pm) | ❌ | ✅ (same + orchestrator) | — |
| Torrent streaming | ⚠️ (MagNutz) | ❌ | ⚠️ (partial) | High |
| P2P streaming engine | ❌ (TorrServer manual) | ❌ | ❌ | **Highest** |
| PiP | ✅ | ✅ | ✅ | — |
| Cast/Chromecast | ❌ | ❌ | ✅ | Medium |
| SponsorBlock | ❌ | ❌ | ❌ | Medium |
| Auto Frame Rate | ❌ | ❌ | ❌ | Low |
| TV D-pad nav | ✅ | ✅ | ⚠️ | — |
| Full EPG schedule grid | ❌ | ✅ | ❌ | Medium |
| Live sports multi-source | ⚠️ (ESPN only) | ❌ | ✅ (DaddyLive) | **High** |
| Telegram audio streaming | ❌ | ❌ | ❌ | Low |

---

## 2. Source Project Snapshots

### 2.1 INTEL

Three major documents:

- **`analysis_report.md`** — Deep dive into the live sports plugin: 8 providers, cron-based match aggregation, Opossum circuit breakers, stream scoring engine (1080p>720p, direct HLS>embed), CORS-busting HLS proxy via child resolver process with WASM decryption, YAML-based zero-code scrapers
- **`iptv-sports-plugin-strategy-c-execution-plan.md`** — Execution plan for merging IPTV channels + sports scraping into a unified "IPTV Sports Ultimate" service
- **`altersend-architecture.md`** — Full AlterSend P2P architecture with Hyperswarm DHT, device pairing, relay fallback

**Key takeaway for Nuvio:** The sports plugin architecture (circuit breakers, stream scoring, multi-source aggregation, CORS proxy) should be ported into Nuvio's SportNutz hub directly.

### 2.2 Nuv MObile

Contains five reference apps:

| App | Relevance to Nuvio |
|---|---|
| **LibreTorrent** | **libtorrent4j** engine — sequential download with HTTP Range streaming, DHT, PeX, LSD, UPnP, magnet links, torrent creation. Nuvio already has `:tdlib-java` dependency, could add `libtorrent4j` for a real P2P engine. |
| **SmartTube** | **SponsorBlock**, Auto Frame Rate, 8K ExoPlayer, live chat overlay, double-tap seek, DeArrow, storyboard preview, key customization. Massive UX inspiration. |
| **TelePlay** | TV Leanback + mobile touch; Telegram video browsing; public link sharing; multi-bot parallel streaming. Good TeleNutz UX reference. |
| **DrKLO Telegram** | Full MTProto client — much more than TDLib. Secret chats, calls, stories, channels, forums, bots, passport. Reference for Telegram features Nuvio could expose. |
| **PixelMusic** | FLAC/ALAC/WAV audio, 10-band EQ, YouTube Music streaming, Telegram audio streaming, Google Drive audio, AI playlists, Last.fm smart mixes, Dynamic Material You, Chromecast Audio, Android Auto, Wear OS. **Inspiration for MusicNutz.** |

Also:
- **`features.md`** — 132-feature catalog across all apps
- **`debrify_investigation.md`** — Deep analysis of Stremio's Rust WASM core + addon protocol architecture
- **`nuvio_wiring_guide.md`** — How to wire everything together

### 2.3 Nuvio Secrets

Planning documents:

- **`ROBBDEeZENUTZ_HUBZ_SEASON1.md`** — TV-style changelog: MultiNutz layout expansion, MagNutz torrent manager, SportNutz performance upgrade, TeleNutz integration, VidNutz, MusicNutz
- **`CLOUDSTREAM_ADDON_GUIDE.md`** — How to build Cloudstream-compatible addons
- **`hub_screen_prompts.md` / `hub_screen_prompts_tv.md`** — Hub screen designs for phone + TV
- **`magnutz_tablet_detail_view/`** — Two-panel tablet layout for torrent manager
- **`HUBZ_PLAN.md`** — Hub ecosystem planning

### 2.4 live-sport-plugin-main

Node.js Express server (Stremio addon):

```
src/
├── providers/          — 8 scrapers (StreamFree, Streamed.pk, BinTV, etc.)
├── services/           — CacheService, CronService, CircuitBreakerService, StreamScoringService
├── domain/             — MatchEntity, StreamEntity
├── index.js            — Express server + Stremio manifest
public/                 — configure.html, /watch WebRTC player
resolver/               — Child process for WASM decryption + HLS relay
```

**Features to port:**
- Multi-source sports aggregation with dedup
- Circuit breaker pattern for provider reliability
- Stream scoring algorithm
- CORS-busting HLS proxy
- YAML-based scrapers (zero-code provider addition)

### 2.5 Extreme-InfiniTV

Astro/Svelte + Tauri app:

```
src/
├── components/         — Svelte components (LiveTV, EPG, Movies, Series, Settings)
├── lib/                — IPTV API client, EPG parser, format helpers
src-tauri/              — Rust Tauri backend, auto-updater, native shell
```

**Features to port:**
- Full EPG schedule grid with timezone-aware rendering
- Multi-playlist sidebar switching
- VOD/series library with poster grid + detail dialogs
- PiP with Video.js/HLS.js
- TV D-pad spatial navigation (spatial-navigation-polyfill)
- Self-updating desktop build

### 2.6 Apex_Streamer

Kotlin/Gradle Android app:

```
app/
├── src/main/java/com/apex/.../
│   ├── features/       — M3U, Xtream, Stalker players
│   ├── player/         — ExoPlayer with multi-view
│   ├── debrid/         — RD/TB/Pm orchestrator with failover
│   ├── sports/         — DaddyLive, GameToChannelMatcher
│   ├── addons/         — Stremio addon integration
│   ├── cast/           — Google Cast
│   └── settings/       — All settings
```

**Features to port:**
- Debrid orchestrator with automatic failover between RD/TB/Pm
- Google Cast integration
- DaddyLive sports source + GameToChannelMatcher
- Multi-view with 4 layouts (Nuvio already surpasses this with 18)
- Stremio addon integration (Nuvio already has this)

---

## 3. New Features to Port

### Priority Legend

| Label | Meaning |
|---|---|
| 🔴 **Critical** | Game-changing — significantly expands Nuvio's capability |
| 🟡 **High** | Major UX or feature improvement |
| 🟢 **Medium** | Nice-to-have polish |
| ⚪ **Low** | Future / niche |

### 3.1 🔴 P2P Streaming Engine (from LibreTorrent)

**Source:** Nuv MObile / `libretorrent-master`

Turn Nuvio into its own streaming source — no debrid needed.

**What to port:**
- libtorrent4j engine (already in your gradle deps as `org.libtorrent4j:libtorrent4j`)
- Sequential download with HTTP Range streaming for live playback
- Magnet link parsing and DHT peer discovery
- Streaming directly into ExoPlayer as pieces arrive
- Torrent management UI (trackers, files, peers panel)

**How it integrates:**
- MagNutz already exists but uses TorrServer (external binary). Replace with native libtorrent4j
- Add "Play from Magnet" as a source option alongside addons/debrid
- Stream torrents directly in the existing ExoPlayer surface
- Reuse the MagNutz UI for torrent management

**Files:**
```
features/p2p/
├── P2pEngine.kt              ← libtorrent4j wrapper
├── P2pTorrentSession.kt      ← Per-torrent session state
├── P2pStreamingSource.kt     ← Feeds pieces to ExoPlayer
└── P2pSettingsStorage.kt     ← Max connections, limits
```

**Optimization:** ExoPlayer already has a `CacheDataSource` (used for timeshift). Same mechanism can buffer torrent data.

### 3.2 🟡 Full EPG Schedule Grid (from Extreme-InfiniTV)

**Source:** Extreme-InfiniTV

Nuvio already has now/next EPG in the channel overlay. Extreme-InfiniTV has a **full schedule grid** with timezone-aware rendering.

**What to port:**
- Grid layout: channels as rows, time as columns, programs as cells
- Timezone-aware "all times local" rendering
- Time-slot headers scrolling horizontally
- Click any program → record or view details
- Color-coded by genre/category

**How it integrates:**
- Replace or augment the existing now/next overlay in `IptvScreen.kt`
- Add a "Schedule" tab in IPTVNutz that shows the full grid
- EPG data already exists — `EpgParser` parses XMLTV, `IptvRepository` has `epgPrograms`
- Just needs a new Composable grid layout

### 3.3 🟡 Live Sports Deep Integration (from live-sport-plugin + Apex)

**Source:** live-sport-plugin-main + Apex_Streamer

SportNutz currently uses ESPN scores + YouTube highlights. These add:

**From the live sports plugin:**
- 8+ provider aggregation with automatic failover (StreamFree, Streamed.pk, BinTV, etc.)
- Background cron cache for instant loading
- Circuit breaker pattern per provider
- Stream scoring (1080p>720p, direct HLS>embed)
- Zero-code YAML scrapers for new sources

**From Apex:**
- DaddyLive client for live sports streams
- GameToChannelMatcher — maps a sports game to an IPTV channel in your lineup

**How it integrates:**
- Replace ESPN as the primary sports data source with the multi-provider aggregator
- Add GameToChannelMatcher to the existing "Find Channel" feature
- Add circuit breakers to the existing SportsRepository
- Add background cron caching to SportsRepository

### 3.4 🟡 SponsorBlock Integration (from SmartTube)

**Source:** Nuv MObile / SmartTube-master

SponsorBlock auto-skips sponsored segments, intros, outros, and self-promotions in YouTube/VOD content.

**What to port:**
- SponsorBlock API client (open API, no key needed)
- Skip-intro button during playback
- Auto-skip configurable per category (sponsor, intro, outro, self-promo, etc.)

**How it integrates:**
- Nuvio already has `skip/SkipIntroApi.kt` and `SkipIntroButton.kt` — expand these to use SponsorBlock API
- The existing IntroDbConfig can be augmented with SponsorBlock
- Works for any YouTube content in VidNutz

**Optimization:** The existing skip-intro infrastructure (`SkipIntroRepository`, `SkipIntroButton`, `SkipInterval`) already has the UI patterns — just swap in SponsorBlock as the data source.

### 3.5 🟡 Google Cast / Chromecast (from Apex)

**Source:** Apex_Streamer

Play Nuvio content on any Chromecast device.

**What to port:**
- Media3 Cast integration (existing dependency: `androidx.media3:media3-cast`)
- Cast button in player controls
- Cast session management with disconnect

**How it integrates:**
- Nuvio already uses Media3 ExoPlayer — Cast is a Media3 extension
- Add Cast button to `PlayerControls.kt` (next to PiP button)
- Use `MediaSession` + `CastPlayer` from Media3

### 3.6 🟡 Channel Groups & Multi-Playlist Sidebar (from Extreme-InfiniTV)

**Source:** Extreme-InfiniTV

Nuvio already has `UserChannelGroup` and channel groups in settings, but Extreme-InfiniTV shows multiple playlists in a sidebar with one-click switching.

**What to port:**
- Sidebar/rail showing all playlists (M3U, Xtream, Stalker)
- Switch between playlists without re-entering credentials
- "All" view showing union of all playlists
- Playlist-specific favorites

**How it integrates:**
- Replace the current source selection chips with a sidebar/rail
- Nuvio's `IptvPlaylistSettings` already stores multiple playlists
- Add playlist-specific favorites (beyond the current global set)

### 3.7 🟢 MusicNutz Enhancements (from PixelMusic)

**Source:** Nuv MObile / PixelMusicApp

MusicNutz currently uses YouTube audio. PixelMusic shows what a full-featured music hub could be.

**What to port:**
- **Audio quality:** FLAC/ALAC/WAV support alongside YouTube
- **10-band equalizer** + bass boost + spatial virtualizer
- **Local music library** scanner (MediaStore → Room)
- **YouTube Music streaming** (InnerTube API, ad-free)
- **Telegram audio streaming** (reuse TDLib from TeleNutz)
- **Synchronized lyrics** — LRC parser with real-time highlight
- **Generative AI playlists** — natural language → LLM curates
- **Last.fm smart mixes** — artist radio, track radio, tag radio, genre, decade, mood, discovery

**How it integrates:**
- Add as MusicNutz sub-tabs: Local, YouTube Music, Telegram, Playlists
- EQ ties into the existing ExoPlayer audio session
- Telegram audio uses the same TDLib as TeleNutz

### 3.8 🟢 Multi-Device Settings Sync (from INTEL/AlterSend)

**Source:** INTEL / `altersend-architecture.md`

Already covered in `backup_sync_execution_plan.md`. AlterSend's Hyperswarm DHT architecture informs the P2P approach, but the practical recommendation was to use TDLib-based Telegram relay since TDLib already exists in Nuvio.

### 3.9 🟢 Auto Frame Rate (from SmartTube)

**Source:** Nuv MObile / SmartTube-master

Match display refresh rate to video content framerate for judder-free playback.

**What to port:**
- Detect video framerate from Media3 Format
- Trigger display mode switch on supported Android devices
- Revert to default on playback end

**How it integrates:**
- Add to `PlayerSettingsUiState` as a toggle
- Android-specific implementation via `Display.setSupportedModes()`
- Media3 `Format.frameRate` already available

---

## 4. Feature Optimizations for Existing Nuvio Features

### 4.1 🟡 MultiNutz — Add Stream Scoring & Auto-Source Selection

**Current:** User picks channels manually for each MultiNutz cell.  
**Optimization:** Add stream quality scoring (from live-sport-plugin) so the best-quality stream auto-loads per cell.

```
Current:   User long-presses channel → picks slot
Improved:  User picks slot → system picks best available stream
           (score = resolution× + bitrate× + source reliability×)
```

### 4.2 🟡 IPTVNutz — Circuit Breaker Pattern for Source Reliability

**Current:** If an M3U URL or Xtream server is down, the app shows an error or hangs.  
**Optimization:** Add Opossum-style circuit breaker (from live-sport-plugin) to each source:

```
Source state machine:
  ┌────────┐   failure threshold   ┌───────┐   timeout   ┌────────┐
  │  Closed │ ────────────────────► │ Open  │ ──────────► │ Half-  │
  │ (normal)│                      │ (dead)│              │ Open   │
  └────────┘                       └───────┘              └───┬────┘
       ▲                            │                         │
       │        success             │                         │
       └────────────────────────────┴─────────────────────────┘
                              (reset counter)
```

**Applies to:**
- M3U URL fetches
- Xtream API calls
- Stalker portal handshakes
- EPG URL fetches

### 4.3 🟡 Debrid — Failover Orchestrator (from Apex)

**Current:** Nuvio supports RealDebrid, TorBox, and Premiumize but doesn't auto-failover.  
**Optimization:** Add debrid orchestrator (from Apex) that:

- Tries providers in priority order
- Falls back on failure (RD down → try TB → try Pm)
- Shows which provider served each stream
- Health-check pings every N minutes

```kotlin
// New: DebridOrchestrator
class DebridOrchestrator(
    private val providers: List<DebridProvider>  // RD, TB, Pm in priority order
) {
    suspend fun resolveMagnet(magnet: String): DebridStream? {
        for (provider in providers) {
            if (!provider.isHealthy()) continue
            val result = provider.resolveMagnet(magnet)
            if (result != null) return result
        }
        return null  // all providers failed
    }
}
```

### 4.4 🟢 SportNutz — Background Cron Caching

**Current:** SportNutz loads ESPN data on-demand (pull-to-refresh).  
**Optimization:** Add background cron job (from live-sport-plugin's CronService) that:

- Fetches scores every 5 minutes
- Pre-warms cache so switching leagues is instant
- Serves stale data when network is down (stale-while-revalidate)

```kotlin
// Current: SportsRepository.cache is a simple Map with TTL
// Optimized: Add background Coroutine job

fun startBackgroundCache() {
    cacheJob = scope.launch {
        while (isActive) {
            val leagues = getLeagues()  // active leagues
            leagues.parMap { refreshLeagueCache(it) }
            delay(5 * 60_000L)  // every 5 minutes
        }
    }
}
```

### 4.5 🟢 VidNutz — Add SponsorBlock, DeArrow, Storyboard Previews

**Current:** VidNutz plays YouTube videos with basic ExoPlayer.  
**Optimization:** Port SmartTube features:

- **SponsorBlock** — auto-skip sponsored segments (via existing SkipIntroApi)
- **DeArrow** — replace clickbait titles/thumbnails with crowd-sourced alternatives
- **Storyboard previews** — seekbar thumbnails from YouTube sprite sheets
- **Double-tap to seek** — configurable intervals (5/10/30s)
- **Live chat overlay** — for live YouTube streams

### 4.6 🟢 TeleNutz — Add TelePlay-Style UX Features

**Current:** TeleNutz streams Telegram videos in a basic player.  
**Optimization:** Port TelePlay features:

- **TV Leanback layout** — horizontal rows with Continue Watching, Recent, Folders
- **Mobile file browser** — multi-select, batch delete/move/rename
- **Public link sharing** — time-limited signed URLs
- **Multi-bot parallel streaming** — fetch chunks from multiple bots simultaneously
- **Pinch-zoom / gesture controls** — brightness/volume swipe, zoom-to-fill

### 4.7 🟢 Player Controls — SmartTube-Style Gesture & UX

**Current:** Nuvio player controls are functional but basic.  
**Optimization:** Port SmartTube UX:

- **Double-tap sides** to seek (configurable: 5/10/30s)
- **Swipe up/down left side** → brightness
- **Swipe up/down right side** → volume
- **Long-press** for context menu (add to watch later, block channel, etc.)
- **Video zoom modes** — fill/fit/stretch/original
- **Seekbar preview thumbnails** (from YouTube storyboard or generated)
- **Remote control pairing** — phone-to-TV code pairing for remote play/pause

### 4.8 🟢 ExoPlayer Diagnostics Panel (from Apex)

**Current:** When streams fail, the user gets a generic error.  
**Optimization:** Add an ExoPlayer diagnostic overlay (inspired by Apex):

```
┌─────────────────────────────┐
│ ⚙️ Stream Diagnostics       │
│                             │
│ Stream URL                  │
│ https://cdn.provider.com/...│
│                             │
│ Resolution: 1920×1080       │
│ Codec:      H.264 (AVC)     │
│ Bitrate:    4.2 Mbps        │
│ Buffer:     12.3s / 30.0s   │
│ Dropped:    3 frames        │
│ Engine:     ExoPlayer 1.16  │
│ Audio:      AAC 2.0 128kbps │
│                             │
│ [Copy] [Test URL] [Switch   │
│  Engine: ExoPlayer/libmpv]  │
└─────────────────────────────┘
```

### 4.9 🟢 MultiNutz — Per-Cell Bookmarks to Telegram (from DVR Plan)

**Current:** MultiNutz bookmarks are local only.  
**Optimization:** Save layout bookmarks to Telegram Saved Messages using the same TeleBackupEngine from the DVR/backup plan. Restore bookmarks on any device.

### 4.10 🟢 IPTVNutz — YAML-Based Channel Group Scrapers

**Current:** Channel groups are manually curated.  
**Optimization:** Port YAML scraper system (from live-sport-plugin):

```yaml
# sources/my_playlist.yml
name: "Robbdeeze Ultimate TV"
url: "https://raw.githubusercontent.com/.../playlist.m3u"
parser: m3u
update_interval: 3600  # 1 hour
groups:
  - name: "Sports"
    filter: "ESPN|NFL|NBA|MLB"
  - name: "News"
    filter: "CNN|FOX|BBC|MSNBC"
```

Nuvio reads YAML → auto-categorizes channels into groups → user sees organized groups without manual work.

---

## 5. Phase 1 — P2P Streaming Engine (from LibreTorrent)

### Current State

MagNutz uses **TorrServer** — an external Go binary spawned via `P2pStreamingEngine.startTorrServer()`. This works but:
- Requires bundling a 10MB+ binary
- Binary must match device architecture (ARM, ARM64, x86)
- Separate process = IPC overhead
- Harder to maintain and update

### Target State

Replace TorrServer with **libtorrent4j** (native Java/Kotlin BitTorrent library) for an in-process streaming engine.

### Implementation

```
features/p2p/
├── P2pEngine.kt                  ← Singleton: init libtorrent4j session
├── P2pTorrent.kt                 ← Per-torrent: add magnet, select files, status
├── P2pStreamingDataSource.kt     ← Custom DataSource for ExoPlayer
├── P2pStreamingSource.kt         ← Bridge: torrent pieces → ExoPlayer
├── P2pTrackerService.kt          ← DHT + tracker announce loop
├── P2pSettingsRepository.kt      ← Max connections, limits
└── P2pSettingsStorage.kt         ← Persistence
```

**Key insight:** ExoPlayer already supports custom `DataSource.Factory`. Create `TorrentDataSource` that reads from libtorrent4j's in-memory pieces and feeds them to ExoPlayer as they download. Same pattern as `CacheDataSource` used for timeshift.

```kotlin
class TorrentDataSource(private val torrent: TorrentHandle) : BaseDataSource() {
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        // Wait for pieces to arrive, then return data
        // libtorrent4j blocks on piece availability
        return torrent.readPiece(buffer, offset, length)
    }
}
```

### What It Enables

- Play any magnet link instantly — no debrid needed
- Stream while downloading (torrent → ExoPlayer in real-time)
- Replace the TorrServer binary dependency entirely
- Full control over DHT, peer selection, encryption
- Sequential download mode for instant playback

### Timeline: 2 weeks

### Files to Modify

| File | Change |
|---|---|
| `commonMain/.../features/p2p/P2pStreaming.kt` | Replace TorrServer orchestration with libtorrent4j |
| `androidMain/.../features/p2p/P2pStreamingEngine.android.kt` | Remove TorrServer binary management |
| `commonMain/.../features/hub/MagNutzScreen.kt` | Keep same UI, swap engine underneath |
| `commonMain/.../features/hub/MagNutzRepository.kt` | Use libtorrent4j for magnet resolution |

---

## 6. Phase 2 — SmartTube-Style Player Enhancements

### 6.1 SponsorBlock (VidNutz)

Expand existing skip-intro infrastructure:

| File | Change |
|---|---|
| `features/player/skip/SkipIntroApi.kt` | Add SponsorBlock API client |
| `features/player/skip/SkipIntroRepository.kt` | Fetch SponsorBlock segments by video ID |
| `features/player/skip/SkipModels.kt` | Add SponsorBlock categories |
| `features/player/skip/SkipIntroButton.kt` | Add SponsorBlock-specific labels |
| `features/player/PlayerPlaybackOverlays.kt` | Hook SponsorBlock auto-skip logic |
| `features/settings/PlaybackSettingsPage.kt` | Add SponsorBlock toggle per category |

### 6.2 Double-Tap Seek (Player Controls)

| File | Change |
|---|---|
| `features/player/PlayerControls.kt` | Add double-tap gesture regions on left/right |
| `features/player/PlayerSurfaceGestures.kt` | Detect double-tap vs single-tap |
| `features/player/PlayerSettingsRepository.kt` | Add seek interval setting (5/10/30s) |

### 6.3 Seekbar Storyboard Previews (VidNutz)

| File | Change |
|---|---|
| `features/player/PlayerControls.kt` | Inject thumbnail image at seek position |
| `features/player/SubtitleCacheProvider.kt` | Extend for storyboard sprite caching |
| New: `features/player/StoryboardSpriteLoader.kt` | Parses YouTube storyboard manifest, downloads sprite tiles |

### 6.4 Voice Search (Search Hub)

Already present in Apex. Nuvio has `SearchScreen.kt` but no voice search:

| File | Change |
|---|---|
| `features/search/SearchScreen.kt` | Add microphone icon beside search field |
| `features/search/SearchRepository.kt` | Add VoiceSearchPlatform expect/actual |

---

## 7. Phase 3 — Extreme-InfiniTV Feature Parity

### 7.1 Full EPG Schedule Grid

New composable replacing the now/next overlay:

```kotlin
@Composable
fun EpgScheduleGrid(
    channels: List<IptvChannel>,
    programs: Map<String, List<EpgProgram>>,
    // ... time range, zoom level, etc.
)
```

**Layout:**
```
┌──────────┬──────────────────────────────────────────────┐
│          │  6:00       7:00       8:00       9:00       │
├──────────┼──────────────────────────────────────────────┤
│ ESPN     │ SportsCenter │   NFL Live   │ SportsCenter   │
├──────────┼──────────────┼──────────────┼────────────────┤
│ CNN      │  Morning Show  │  Newsroom  │  Situation     │
├──────────┼──────────────┼──────────────┼────────────────┤
│ FOX      │  Fox & Friends   │  The Five │  Gutfeld!      │
└──────────┴──────────────────────────────────────────────┘
```

**Features:**
- Vertical header = channels (scrollable)
- Horizontal header = time slots
- Program cells = width proportional to duration
- Click = show program details + "Record" or "Watch Catch-up"
- Scrollable in both directions
- Pinch-to-zoom time scale (1h/3h/6h/12h/24h)
- "Now" indicator line that follows current time

### 7.2 Multi-Playlist Sidebar

Replace source selection chips with a persistent sidebar:

```
┌──────────┬────────────────────────────────┐
│ 📺 Sources│   IPTVNutz Channels             │
│           │                                │
│ ● All    │   [Search...]                   │
│──────────│                                │
│ ● iptv-org│   ESPN         │ NFL Network   │
│ ○ My M3U │   SportsCenter  │ ESPN FC      │
│ ○ Xtream │   CNN          │ FOX News      │
│ ○ Stalker│   ...                          │
│──────────│                                │
│ ⭐ Favorites│                              │
│ ─────── │                                │
│ 📁 Groups │                                │
│   Sports │                                │
│   News   │                                │
│   Movies │                                │
└──────────┴────────────────────────────────┘
```

### 7.3 VOD/Series Library with Poster Grids

Nuvio already has movie/series addons, but Extreme-InfiniTV has a native Xtream VOD browser with poster grids:

| File | Change |
|---|---|
| New: `features/vod/VodScreen.kt` | Browser for Xtream VOD categories |
| New: `features/vod/VodRepository.kt` | Fetch movies/series from Xtream API |
| New: `features/vod/VodSeriesDetailScreen.kt` | Seasons + episodes view |
| `features/hub/RobbdeezeNutzHubScreen.kt` | Add "VOD" hub card |

---

## 8. Phase 4 — Apex Streamer Feature Parity

### 8.1 Debrid Orchestrator with Failover

| File | Change |
|---|---|
| New: `features/debrid/DebridOrchestrator.kt` | Priority-ordered provider chain |
| `features/debrid/DebridProvider.kt` | Add `isHealthy()` method |
| `features/debrid/DebridSettings.kt` | Add provider priority ordering |

### 8.2 Google Cast

| File | Change |
|---|---|
| New: `features/cast/CastManager.kt` | Media3 Cast integration |
| `features/player/PlayerControls.kt` | Add Cast button |
| `features/player/PlayerScreen.kt` | Add cast session lifecycle |

### 8.3 ExoPlayer Diagnostics

| File | Change |
|---|---|
| New: `features/player/PlayerDiagnostics.kt` | Overlay with stream info |
| `features/player/PlayerOverlays.kt` | Hook diagnostics toggle |
| `features/player/PlayerScreenRuntimeUi.kt` | Add diagnostics state |

---

## 9. Phase 5 — Live Sports Plugin Deep Integration

### 9.1 Multi-Source Sports Aggregator

Port the aggregation engine into SportNutz:

| File | Change |
|---|---|
| New: `features/sports/SportsAggregator.kt` | Multi-provider fetch with dedup |
| New: `features/sports/SportsCircuitBreaker.kt` | Per-provider circuit breaker |
| New: `features/sports/SportsCacheService.kt` | Background cron cache |
| New: `features/sports/SportsStreamScorer.kt` | Stream quality scoring |
| `features/sports/SportsRepository.kt` | Replace single-source with aggregator |

### 9.2 Game-to-Channel Matcher

Port from Apex:

| File | Change |
|---|---|
| `features/sports/GameToChannelMatcher.kt` | Already exists! Enhance with league-aware matching |
| `features/sports/SportsScreen.kt` | Add "Watch on Channel" button from matcher results |

### 9.3 DaddyLive Integration

| File | Change |
|---|---|
| New: `features/sports/DaddyLiveClient.kt` | Already exists in Nuvio! Expand sources |
| `features/sports/SportsRepository.kt` | Add as additional provider |

### 9.4 CORS Proxy for HLS Streams

Port from live-sport-plugin:

| File | Change |
|---|---|
| `androidMain/.../features/player/ResponseHeaderOverridingDataSource.kt` | Already exists — enhance for CORS proxy |
| New: `features/streams/HlsProxyService.kt` | Local proxy for embedded HLS streams |

---

## 10. Phase 6 — Telegram Ecosystem Deepening

### 10.1 TelePlay-Style UX for TeleNutz

| File | Change |
|---|---|
| `features/hub/TeleNutzScreen.kt` | Add TV Leanback rows layout |
| `features/hub/TeleNutzStore.kt` | Add Continue Watching + Recently Added |
| New: `features/hub/TeleNutzFileBrowser.kt` | Multi-select, batch ops, folder nav |

### 10.2 Telegram Audio Streaming for MusicNutz

| File | Change |
|---|---|
| `features/hub/MusicNutzRepository.kt` | Add TDLib search for audio files |
| `features/hub/MusicNutzScreen.kt` | Add "Telegram" source tab |
| `features/hub/MusicNutzStore.kt` | Add Telegram audio queue |

### 10.3 Telegram Backup Sync (from backup_sync_execution_plan.md)

Already covered: `TeleBackupEngine.kt` + TDLib upload/download.

---

## 11. Phase 7 — Performance & Code Optimizations

### 11.1 Lazy EPG Rendering

**Current:** EPG data is loaded into memory for all channels at once.  
**Optimization:** Virtualized EPG rendering using `LazyVerticalGrid` + time-slot windowing:

```kotlin
// Only render EPG programs visible in the current viewport + time window
LazyVerticalGrid(
    columns = GridCells.Fixed(timeSlotCount),
    // ...
) {
    items(channels) { channel ->
        val visiblePrograms = getProgramsInTimeWindow(
            channelId = channel.epgChannelId,
            windowStart = displayedTimeStart,
            windowEnd = displayedTimeEnd,
        )
        ProgramCell(programs = visiblePrograms)
    }
}
```

### 11.2 Player Startup Optimization

**Current:** Player creates a new ExoPlayer instance each time.  
**Optimization:** Pool ExoPlayer instances for faster channel switching:

```kotlin
object PlayerPool {
    private val pool = ArrayDeque<ExoPlayer>(3)

    fun acquire(): ExoPlayer {
        return if (pool.isNotEmpty()) pool.removeFirst().also { it.prepare() }
        else ExoPlayer.Builder(context).build()
    }

    fun release(player: ExoPlayer) {
        player.stop()
        pool.addLast(player)
    }
}
```

### 11.3 EPG Parse Streaming

**Current:** `EpgParser.parseXmltv()` parses the full XML in one shot.  
**Optimization:** Already has streaming support via `parseXmltvStream()` — ensure this is the default path for all EPG fetches to reduce peak memory on large EPG files.

### 11.4 Channel Cache Preloading

**Current:** Channels load on first IPTVNutz visit.  
**Optimization:** Preload in background at app startup:

```kotlin
// In App.kt
LaunchedEffect(Unit) {
    // Warm IPTV cache immediately after auth
    IptvRepository.ensureLoaded()  // already exists
    IptvRepository.refreshEpg()    // already exists, just call earlier
}
```

### 11.5 MultiNutz Memory Management

**Current:** Each MultiNutz cell holds a full ExoPlayer instance.  
**Optimization:** 
- Pause cells not visible in the current viewport
- Release player surfaces for off-screen cells
- Throttle bitmap updates for channel logos in the grid

---

## 12. Appendix: Quick-Win Items

These are small, self-contained improvements that can be done in a few hours each:

| # | Item | Source | Effort | Impact |
|---|---|---|---|---|
| 1 | **Player startup pre-warm** — create ExoPlayer earlier | Analysis | 1h | Faster channel switching |
| 2 | **Share backup via intent** — add system share button to BackupScreen | Nuv MObile | 30min | Instantly share backups |
| 3 | **EPG streaming by default** — ensure `parseXmltvStream` is the default | Analysis | 1h | Lower peak memory |
| 4 | **Double-tap seek** in VidNutz player | SmartTube | 2h | Better UX |
| 5 | **Background EPG cache refresh** on app foreground | Analysis | 1h | Always-fresh EPG |
| 6 | **Add Google Cast button** to player controls | Apex | 4h | Cast content |
| 7 | **Circuit breaker for M3U URLs** — skip dead playlists automatically | live-sport-plugin | 3h | Faster IPTV loading |
| 8 | **Channel preload on startup** — call `IptvRepository.ensureLoaded()` earlier | Analysis | 30min | Instant IPTV tab |
| 9 | **Add diagnostics overlay** — ExoPlayer stats on long-press | Apex | 3h | Debug stream issues |
| 10 | **GameToChannelMatcher** — show "Watch on ESPN" from SportNutz | Apex | 4h | Bridge sports → IPTV |

---

## Summary: Feature vs Effort Matrix

```
Effort →
        ┌────────────────────────────────────────────┐
        │  Quick (hrs)     Medium (days)   Large (wks)│
        │                                            │
 High   │ Cast Button      Debrid Orchestrator   P2P │
 Impact │ EPG Streaming    SponsorBlock         Engine│
        │ Player Pre-warm  EPG Schedule Grid         │
        │ Share Backup     Sports Aggregator         │
        │ Circuit Breakers Sports Cron Cache         │
        │ GameToChannel    Multi-Playlist Sidebar    │
        │                                            │
 Medium │ Double-Tap Seek  TelePlay UX              │
 Impact │ Diagnostics      YouTube Music (MusicNutz)│
        │ Background EPG   TeleNutz File Browser    │
        │ Channel Preload  Voice Search             │
        │                                            │
  Low   │ Auto Frame Rate  YAML Scrapers            │
 Impact │ Storyboard Prev. Telegram Audio (Music)   │
        │ AFR              AI Playlists             │
        └────────────────────────────────────────────┘
```

---

## Recommended Priority Order

### Sprint 1: Quick Wins (Week 1)
- Circuit breakers for IPTV sources
- Share backup via system intent
- Pre-warm player + preload channels on startup
- EPG streaming by default
- Double-tap seek

### Sprint 2: High Impact (Week 2-3)
- Debrid orchestration with failover
- GameToChannelMatcher enhancement
- Cast button
- EPG schedule grid
- Background sports cron cache

### Sprint 3: P2P Engine (Week 4-6)
- Replace TorrServer with libtorrent4j
- TorrentDataSource for ExoPlayer
- Magnet link streaming without debrid

### Sprint 4: Sports Deep Dive (Week 7-8)
- Multi-provider sports aggregator
- Circuit breaker per provider
- Stream scoring
- CORS proxy for HLS

### Sprint 5: UI Polish (Week 9-10)
- SponsorBlock integration
- Multi-playlist sidebar
- TelePlay TV layout for TeleNutz
- VOD browser for Xtream sources
- MusicNutz enhancements (EQ, Telegram audio)

---

*End of Feature Analysis & Optimization Report*
