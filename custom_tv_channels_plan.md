# Custom TV Channels & Navigation System — Execution Plan

## Overview
Build a custom TV channel system with catalog-based content sourcing, dynamic EPG generation, and in-player channel surfing for Nuvio_Robbdeeze.

## Codebase Foundation
- KMP + Compose Multiplatform app (Android/iOS)
- Existing DeezeNutz torrent-based channels, IPTV module with XMLTV EPG parsing, ExoPlayer with overlay system
- TMDB, MDBList, Trakt integrations already wired

## Suggestions
1. Use distinct naming (`CustomChannel`/`TvChannel`) to avoid confusion with existing DeezeNutz
2. Leverage existing EPG parser, player overlays, and DeezeNutz state management patterns
3. Core technical challenge: EPG playhead sync (curate → randomize → schedule → seek to clock-based position)

## Phases

### Phase 1: Data Layer — Custom Channel Models & Catalog Sourcing
- Create `CustomChannel`, `CustomChannelItem`, `EpgScheduleEntry` data models
- Create `CustomChannelStore` for local persistence
- Wire TMDB and MDBList search as catalog sources
- `CatalogSourceRepository` aggregating TMDB lists, MDBList, addon catalogs

### Phase 2: Channel Creation & Editing UI
- Channel creation screen: name + catalog source browser/search
- Catalog assignment: browse TMDB/MDBList, select titles or lists
- Channel config: randomized toggle, playback prefs, save to guide

### Phase 3: Dynamic EPG Engine
- `EpgScheduler` generates randomized schedule with inline start/end times
- Playhead calculator: given schedule + device clock → "now playing" + seek position
- Deterministic seeding for schedule consistency
- Channel detail view with active/upcoming timeline

### Phase 4: Channel Tile UI (Nuvionnutz)
- Tile component: name label + poster from first video
- Channel grid/list in hub
- Channel detail page with EPG timeline
- Play button → calculates playhead → launches player at timestamp

### Phase 5: In-Player Channel Surfing (Nuvionnutz)
- NCH (Next Channel) button in ExoPlayer overlay
- Channel selection drawer over active player
- Seamless channel switching preserving EPG schedule + deterministic order

### Phase 6: Polish & Edge Cases
- Loading/error/empty states, EPG schedule persistence, catalog change handling, performance

## Execution Order
**Phase 1 + Phase 3 first** (models + EPG engine — foundations)
→ **Phase 2 + Phase 4** (creation UI + tile presentation)
→ **Phase 5** (in-player surfing)
→ **Phase 6** (polish)
