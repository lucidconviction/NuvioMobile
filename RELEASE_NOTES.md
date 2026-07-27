# Nuvio Mobile — Release Notes

Release notes are bundled into every build and shown in the in-app update dialog.
Update `RELEASE_NOTES` in `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdater.kt` for each new release.

---

## v0.5.0 — Quick Channels Multi-Window, Channel Overlay Popup

**Built:** July 23, 2026

### New
- Quick Channels in Multi-Window: browse ~240 curated channels directly from the multi-window toolbar
- Quick Channel overlay: tap a quick channel to see all matching IPTV sources with search, source, and group filters
- "Quick" button in cell options (long-press → ⋮ → Quick) for slot-specific channel picking
- Channel match overlay with search bar, source filter chips, and group filter chips

### Updated
- Multi-window grid toolbar now includes ⚡ Quick pill button
- In-app updater APK served from apps.rdnutz.us

### Fixed
- Quick Channels UI lag: no longer pre-loads channel source matches for all 240 channels on open; only loads on tap

### Build
```bash
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`
