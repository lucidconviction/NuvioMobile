# Nuvio Mobile — Release Notes

Release notes are bundled into every build and shown in the in-app update dialog.
Update `RELEASE_NOTES` in `composeApp/src/commonMain/kotlin/com/nuvio/app/features/updater/AppUpdater.kt` for each new release.

---

## v0.7.0 — Text-Only MatchCards, VidNutz NewPipe Trending, YouTube Audio Fix, MultiWindow Audio Fix

**Built:** July 26, 2026

### SportNutz — Text-Only MatchCards, Event Titles, Fighting Sports
- Text-only MatchCards — removed all team logos/image circles; card body now shows event title prominently then home vs away team names
- Event titles shown on every card (e.g. "UFC 306: O'Malley vs Dvalishvili" for fighting events)
- Date labels on upcoming event cards
- Dead code removed: SportsAsyncImage, isFighting, displayHomeLogo, displayAwayLogo

### VidNutz — NewPipe Trending, Search Fix, Live Streams Removed
- NewPipe primary for trending with "popular"/"trending"/"viral" queries, fallback to Piped/Invidious
- Search uses NewPipe as primary source instead of unreliable Piped/Invidious
- platformYouTubeSearch enlarged to 28 results with max 30min duration
- Per-request 8s timeouts for Piped/Invidious calls
- LIVE_STREAMS category removed

### YouTube Audio — Progressive Format Priority
- InnerTube picks progressive formats over video-only adaptive + separate audio
- Priority: HLS → Progressive → Adaptive + separate
- Piped audio URL fix for HLS streams

### MultiNutz — Audio Fix
- New streams start at full volume (was 0f, causing silence)
- Audio focus at creation; subsequent streams start muted
- Channel changing forces player recreation
- Play/Pause actually calls engine methods
- Audio focus save/restore across focus changes

### Other
- Live Games overlay now filters to only live events
- Force close fixes: safe channel callback, index bounds check

### Build
```bash
./gradlew :androidApp:assembleFullDebug -Pnuvio.android.distribution=full
```
APK: `androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk`
