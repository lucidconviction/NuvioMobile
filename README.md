<div align="center">

  <img src="https://github.com/tapframe/NuvioTV/blob/main/assets/brand/app_logo_wordmark.png" alt="Nuvio" width="300" />
  <br />
  <br />

  [![Stars][stars-shield]][stars-url]
  [![Issues][issues-shield]][issues-url]

  <p>
    A modern media hub for Android and iOS built with Kotlin Multiplatform and Compose Multiplatform.
    <br />
    Stremio addon ecosystem • Cross-platform
  </p>

</div>

## About

This is a fork of [NuvioMedia/NuvioMobile](https://github.com/NuvioMedia/NuvioMobile) with a focus on adding a full-featured IPTV player. The original project is a Kotlin Multiplatform rewrite of the React Native app — a modern media hub for Android and iOS with Stremio addon ecosystem integration, playback, collection tools, watch progress, and downloads.

**What this fork adds:**
- IPTV player tab with M3U playlist and Xtream Codes API support
- Built-in iptv-org source (12,000+ free channels)
- In-player channel overlay with search, favorites, history, and one-tap switching
- EPG (XMLTV) with inline now/next programs and name-based channel matching
- Multi-source selection, collapsible groups, channel favorites, and playback history

The mobile app is built from a single shared codebase in [composeApp](./composeApp), with native platform entry points for Android and iOS.

## IPTV (New)

Nuvio now has a built-in IPTV player with M3U and Xtream Codes support:

https://github.com/Robbdeeze/NuvioMobile/raw/cmp-rewrite/assets/iptv-demo.mp4

- **12,000+ free channels** — one-tap add from iptv-org
- **Multiple M3U playlists + Xtream Codes** — add, toggle, refresh, delete
- **Group/category headers** — channels auto-sorted into collapsible sections
- **Favorites & history** — heart toggles, last 15 channels, persisted
- **In-player overlay** — search, browse, favorite, and switch channels without leaving playback
- **EPG inline** — now/next programs with progress bar on channel cards

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
[license-shield]: https://img.shields.io/github/license/NuvioMedia/NuvioMobile.svg?style=for-the-badge
[license-url]: https://github.com/NuvioMedia/NuvioMobile/blob/main/LICENSE