<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="RNutz Nuvio" width="300" />
  <br />
  <br />

  [![Stars][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![Release][release-shield]][release-url]

  <p>
    A community fork of NuvioMobile with the RobbdeezeNutz Hub experience.
    <br />
    IPTVNutz · SportNutz · VidNutz · MusicNutz — all in one tab.
  </p>

</div>

## About

**RNutz Nuvio** is a fork of [NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) that replaces the old IPTV and Sports tabs with a single **RobbdeezeNutz Hub** containing four sub-hubs. Installs alongside the stock Nuvio app — no conflicts.

## Screenshots

<div align="center">
  <img src="assets/screenshots/photo_2026-07-06%2015.10.32.jpeg" alt="Sports Hub" width="30%" />
  <img src="assets/screenshots/photo_2026-07-06%2015.10.36.jpeg" alt="Sports Hub" width="30%" />
  <img src="assets/screenshots/photo_2026-07-06%2015.10.40.jpeg" alt="Sports Hub" width="30%" />
</div>
<div align="center">
  <img src="assets/screenshots/photo_2026-07-06%2015.10.43.jpeg" alt="IPTV" width="30%" />
  <img src="assets/screenshots/photo_2026-07-06%2015.10.47.jpeg" alt="Player Controls" width="30%" />
  <img src="assets/screenshots/photo_2026-07-06%2015.10.51.jpeg" alt="Channel Overlay" width="30%" />
</div>
<div align="center">
  <img src="assets/screenshots/photo_2026-07-06%2015.10.54.jpeg" alt="IPTV" width="30%" />
  <img src="assets/screenshots/photo_2026-07-06%2019.54.01.jpeg" alt="Trakt Login" width="30%" />
  <img src="assets/screenshots/photo_2026-07-07%2022.46.26.jpeg" alt="RobbdeezeNutz Hub" width="30%" />
</div>
<div align="center">
  <img src="assets/screenshots/photo_2026-07-07%2022.46.39.jpeg" alt="VidNutz Hub" width="30%" />
  <img src="assets/screenshots/photo_2026-07-07%2022.46.47.jpeg" alt="MusicNutz Hub" width="30%" />
</div>

## Hubs

### RobbdeezeNutz Hub
The main screen with four glass-style cards. Tap any card to open its sub-hub. The "RobbdeezeNutz Hubz" header stays visible across all screens. Tap the Hubz tab again to return to the main view.

### IPTVNutz Hub
Live TV with M3U, Xtream Codes, and Stalker Portal support. Built-in iptv-org source (12,000+ channels). In-player channel overlay with search, favorites, history, and one-tap switching.

### SportNutz Hub
Real-time scores and standings from ESPN + TheSportsDB. YouTube highlights, date navigation, team detail pages. Pull-to-refresh and per-date caching.

### VidNutz Hub
YouTube video browser with 12 categories (Trending, Politics, News, Music, Sports, etc.). Persistent search with debounce, swipe between categories, Load More for pagination, and full ExoPlayer playback.

### MusicNutz Hub
Browse millions of tracks by genre. Search by song or album. Toggle between Tracks and Albums view. Tap an album to see its full tracklist. Full-length audio via YouTube.

## Download

[Download the latest APK](https://github.com/Robbdeeze/NuvioMobile/releases/latest)

## Build from source

```bash
./gradlew :androidApp:assembleDebug -Pnuvio.android.distribution=full
```

## Features

- **D-pad / TV remote navigation** across all hubs
- **Installs alongside stock Nuvio** — separate package name
- **Monochrome grayscale design** throughout
- **Trakt login** for watch progress sync

## License

GPL-3.0

<!-- MARKDOWN LINKS & IMAGES -->
[stars-shield]: https://img.shields.io/github/stars/Robbdeeze/NuvioMobile.svg?style=for-the-badge
[stars-url]: https://github.com/Robbdeeze/NuvioMobile/stargazers
[issues-shield]: https://img.shields.io/github/issues/Robbdeeze/NuvioMobile.svg?style=for-the-badge
[issues-url]: https://github.com/Robbdeeze/NuvioMobile/issues
[release-shield]: https://img.shields.io/github/v/release/Robbdeeze/NuvioMobile?include_prereleases&style=for-the-badge&label=Release
[release-url]: https://github.com/Robbdeeze/NuvioMobile/releases/latest
