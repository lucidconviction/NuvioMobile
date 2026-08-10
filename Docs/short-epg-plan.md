# Plan: Live EPG from portal `get_short_epg`

## Fit / facts in this app
- Existing XMLTV EPG (`IptvRepository.refreshEpg()`) is fetched but **never rendered** — cards show only name/group, `epgProgramsByName` is unused in UI. I'll leave that as-is and add a *separate, live-per-portal* short-EPG path.
- Channels already carry `epgChannelId` (XtreamClient.kt:45), and `IptvChannel` has no portal creds — the account (`server/user/pass`) lives in `XtreamAccount`, reachable via `IptvRepository.xtreamAccounts` by `sourceId`.
- HTTP helpers available: `httpGetTextWithHeaders` / `httpGetTextWithHeadersLimited` (already used by the scraper). `TraktPlatformClock.nowEpochMs()` is how the UI gets "now".

## Data source — new `ShortEpgClient` (mirrors `XtreamClient`)
New file alongside `XtreamClient.kt`:
- `suspend fun fetchShortEpg(account: XtreamAccount, channel: IptvChannel, limit: Int): List<EpgProgram>`
  builds `.../player_api.php?username=…&password=…&action=get_short_epg&stream_id=${channel.id}&limit=$limit`
- Parse `epg_listings`: decode start/stop as **unix-seconds** (`if ts > 1e9` → ms, guard `>1e11` skip garbage) **else** ISO `"yyyy-MM-dd HH:mm:ss"` → epoch ms. Reuse `EpgParser.parseXmltvDate`-style logic where sensible.
- `title`/`description` are base64 — decode with malformed fallback (try strict, on failure strip non-base64 chars and retry, clone scraper's `decodeBase64`).
- Sort by `startTime`; map to existing `EpgProgram(channelId=fetched id, title, description, startTime, endTime)`; **empty list = portal ships no EPG**, and callers must hide silently.

## Caching (in-memory only, no disk persistence)
New `ShortEpgCache` object:
- `map<String, Deferred<List<EpgProgram>>>` keyed `"${account.server}|${channel.id}"` → memoized per-stream, so N cards on one stream make **one** HTTP call (coroutine `async` dedup, crash-safe).
- Browser cache cleared when the account/section selection changes (scoped like the reference's `_epgCache`); hit/hub cache (`portal|streamId` key) lives for the session so re-scans reuse results.
- No per-tile loading state in UI — rows render nothing until data lands or if empty.

## UI — three spots in `IptvScreen.kt`
1. **`ChannelCard` (line 1530)** — the main browser card: 7-day-old `now` is passed; add a compact now/next strip under the name/group overlay (limit 2). Red **NOW** + current title, cyan **NEXT** + upcoming, picked via `nowWithin(start,end)`. Render nothing while loading / no EPG / dead portal.
2. **EPG sheet** — long-press a card (`combinedClickable`) opens a bottom sheet with up to **8** upcoming entries `HH:mm–HH:mm` + title (re-fetch `limit=8`). Pattern already exists in the file (`QuickChannelSourcesSheet`, `ModalBottomSheet`).
3. **`TvChannelCard` (line 724, recommended row)** and **`HistoryTvCard` (line 787)** — same now/next strip for parity.
   - Only for `SourceType.Xtream` channels whose account has non-blank server/user/pass; M3U/Stalker and portal-less channels skip.

## Wiring data source→UI
`ChannelCard`/`TvChannelCard`/`HistoryTvCard` receive an optional `epgFuture: Deferred<List<EpgProgram>>?` (or a suspend lambda) computed at the call site via `ShortEpgCache` + `TraktPlatformClock.nowEpochMs()`, so the composables stay dumb and don't know accounts.

## Open question before implementing
- Long-press on cards isn't wired today (`.clickable(onClick=onPlay)`) — adding `combinedClickable` with `onLongClick` is fine, but confirm that gesture is OK vs. reserving long-press for something else.