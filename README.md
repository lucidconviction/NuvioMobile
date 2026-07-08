<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="RNutz Nuvio" width="300" />
  <br />
  <br />

  [![Stars][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]
  [![Release][release-shield]][release-url]

  <p>
    A community fork of NuvioMobile with RobbdeezeNutz Hub — IPTVNutz, SportNutz, VidNutz, MusicNutz.
    <br />
    Android & iOS • Kotlin Multiplatform • Compose Multiplatform
  </p>

</div>

## About

This is **RNutz Nuvio** — a community fork of [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) with a completely reimagined hub experience. The original project is a Kotlin Multiplatform media hub for Android and iOS with Stremio addon ecosystem integration.

**What this fork changes:**

### RobbdeezeNutz Hub
A single hub tab replacing the old IPTV/Sports tabs with 4 sub-hubs:

| Hub | Description |
|-----|-------------|
| **IPTVNutz Hub** | Live TV channels with M3U, Xtream Codes, Stalker Portal support. Grayscale theme, scroll-to-top overlay |
| **SportNutz Hub** | Real-time scores, ESPN + TheSportsDB integration, YouTube highlights, live scores, standings, date navigation |
| **VidNutz Hub** | YouTube video browser with 12 categories (Trending, Politics, News, Music, Sports, etc.), persistent search, 400ms debounce, swipe left/right, Load More, D-pad focus |
| **MusicNutz Hub** | Deezer-powered music browsing with album art, Tracks/Albums toggle, album detail view, full-length YouTube audio playback via NewPipeExtractor |

### Key Features
- **Persistent "RobbdeezeNutz Hubz" title** across all sub-screens
- **Smart top margin** — adapts for phone vs tablet/TV mode
- **D-pad focus** — full TV remote navigation on all cards, chips, and items
- **Tab re-tap resets** to main hub view
- **YouTube streaming** via NewPipeExtractor (Android) with Invidious/Piped fallback
- **Music playback** — Deezer metadata + YouTube full-length audio
- **Package:** `app.robbdeezenutz.nuvio`
- **App name:** RNutz Nuvio

The mobile app is built from a single shared codebase in [composeApp](./composeApp), with native platform entry points for Android and iOS.

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
</div>
<div align="center">
  <img src="assets/screenshots/photo_2026-07-07%2022.46.26.jpeg" alt="RobbdeezeNutz Hub" width="30%" />
  <img src="assets/screenshots/photo_2026-07-07%2022.46.39.jpeg" alt="VidNutz Hub" width="30%" />
  <img src="assets/screenshots/photo_2026-07-07%2022.46.47.jpeg" alt="MusicNutz Hub" width="30%" />
  <p><em>Add these screenshots to assets/screenshots/ — I couldn't view them to verify filenames</em></p>
</div>

## IPTV

- **12,000+ free channels** — one-tap add from iptv-org
- **M3U playlists, Xtream Codes, Stalker Portal** — add, toggle, refresh, delete
- **Group/category headers** — channels auto-sorted into collapsible sections
- **Favorites & history** — heart toggles, last 15 channels, persisted
- **In-player overlay** — search, browse, favorite, and switch channels without leaving playback
- **Grayscale theme** — clean black/grey/white palette

## Installation

### Android

Download the latest Android build from [GitHub Releases](https://github.com/Robbdeeze/NuvioMobile/releases/latest).

### iOS

- [TestFlight](https://testflight.apple.com/join/u4y7MHK9)

## Development

```bash
git clone https://github.com/Robbdeeze/NuvioMobile.git
cd NuvioMobile
./scripts/run-mobile.sh android
# or
./scripts/run-mobile.sh ios
```

### Project Structure

- `composeApp/` contains the shared Kotlin Multiplatform and Compose Multiplatform app code.
- `composeApp/src/commonMain/` contains shared UI, features, repositories, and platform-agnostic logic.
- `composeApp/src/androidMain/` contains Android-specific integrations.
- `composeApp/src/iosMain/` contains iOS-specific integrations.
- `iosApp/` contains the native Xcode project and iOS entry point.

Useful commands:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./scripts/build-distribution.sh
```

Versioning is driven from `iosApp/Configuration/Version.xcconfig`, which is used as the shared source of truth for both iOS and Android builds.

## Legal & DMCA

Nuvio functions solely as a client-side interface for browsing metadata and playing media provided by user-installed extensions and/or user-provided sources. It is intended for content the user owns or is otherwise authorized to access.

Nuvio is not affiliated with any third-party extensions, catalogs, sources, or content providers. It does not host, store, or distribute any media content.

For comprehensive legal information, including our full disclaimer, third-party extension policy, and DMCA/Copyright information, please visit our [Legal & Disclaimer Page](https://nuvioapp.space/legal).

## Built With

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin
- AndroidX Media3
- AVFoundation and native iOS integrations

## Star History

<a href="https://www.star-history.com/#Robbdeeze/NuvioMobile&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Robbdeeze/NuvioMobile&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Robbdeeze/NuvioMobile&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Robbdeeze/NuvioMobile&type=date&legend=top-left" />
 </picture>
</a>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[contributors-url]: https://github.com/NuvioMedia/NuvioMobile/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[forks-url]: https://github.com/NuvioMedia/NuvioMobile/network/members
[stars-shield]: https://img.shields.io/github/stars/Robbdeeze/NuvioMobile.svg?style=for-the-badge
[stars-url]: https://github.com/Robbdeeze/NuvioMobile/stargazers
[issues-shield]: https://img.shields.io/github/issues/Robbdeeze/NuvioMobile.svg?style=for-the-badge
[issues-url]: https://github.com/Robbdeeze/NuvioMobile/issues
[release-shield]: https://img.shields.io/github/v/release/Robbdeeze/NuvioMobile?include_prereleases&style=for-the-badge&label=Release
[release-url]: https://github.com/Robbdeeze/NuvioMobile/releases/latest
[license-shield]: https://img.shields.io/github/license/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[license-url]: https://github.com/NuvioMedia/NuvioMobile/blob/main/LICENSE