Title: [DEV] RNutz Nuvio — Watch up to 9 IPTV streams at once + Sports Hub with live scores, highlights & channel matching

Subreddit: r/IPTV

---

Hey everyone,

I've been working on a fork of NuvioMobile called **RNutz Nuvio** and just dropped v0.2.20 with a bunch of new features I think this sub will appreciate.

## What is it?

It's a free, open-source IPTV player (Android) with a built-in hub system:

- **IPTVNutz** — M3U, Xtream Codes & Stalker Portal support. 12,000+ channels from iptv-org built-in. In-player channel overlay with search, favorites, history, source/group filters.
- **MultiNutz Hub (new)** — Watch **up to 9 live streams simultaneously** in a resizable grid. Each cell has its own volume (discrete steps: Mute/15/30/45/70/85/Max), scaling (Fill/Fit/16:9/4:3/Zoom), and you can swap streams, refresh individual tiles, or save/load layout bookmarks. Only one cell has audio at a time. Push any stream from the full-screen player to the grid with one tap.
- **SportNutz** — Live scores, standings, and news from ESPN + TheSportsDB. YouTube highlights. Matches events to your IPTV channels automatically (US/UK/CA broadcasters supported). Find Channel + event detail panel with Live/Highlights/Pre-Match tabs.
- **VidNutz** — YouTube video browser with 12 categories.
- **MusicNutz** — Browse/search millions of tracks by genre.

## What's new in v0.2.20 (Phase 17)

**MultiNutz Hub Vol. 2:**
- Real-time scaling fix (resize mode applies instantly, not frozen)
- Phone↔tablet transitions no longer kill player surfaces
- Discrete volume step pills instead of a slider (way better for D-pad/TV remote)
- Stream refresh, swap positions, Mute All / Close All / Pause All
- Layout bookmarks — save/load/delete your grid setups
- Push-to-multi from the IPTV player

**Channel Overlay Overhaul:**
- Source filter pills (M3U / Xtream / Stalker)
- Group filter pills
- Real-time search
- Group subtitles on each row

**Sports Hub Fixes:**
- Highlight/prematch videos now actually play (was silently dropping the intent)
- Event images for UFC/fighting sports
- Live/Upcoming dual header with counts
- Back navigation preserved through player lifecycle

## Download

APK: https://github.com/Robbdeeze/NuvioMobile/releases/latest

Build from source: `./gradlew :androidApp:assembleDebug -Pnuvio.android.distribution=full`

Installs alongside stock Nuvio — separate package name, no conflicts.

Open source (GPL-3.0): https://github.com/Robbdeeze/NuvioMobile

---

Let me know if you run into issues or have feature requests. The 9-stream grid with per-cell controls is the main thing I'd love feedback on — it works great on tablets and phones but curious how it holds up on different devices.
