# RobbdeezeNutz Fork — Complete Feature Porting Guide

> **Purpose:** Step-by-step instructions for porting all RobbdeezeNutz fork features to the **TV version** of NuvioMobile.
>
> **Source Project:** `Nuvio_Robbdeeze`  
> **Target:** NuvioMobile TV variant (likely a separate module or `composeApp` with TV-optimized composables)
>
> **Key Constraint:** Package name `app.robbdeezenutz.nuvio` must coexist with the stock app on the same device. The TV version should use the same package or a variant (e.g., `app.robbdeezenutz.nuvio.tv`).

---

## Table of Contents

1. [Overview of Features](#1-overview-of-features)
2. [Package Renaming & App Identification](#2-package-renaming--app-identification)
3. [Tab Restructure — Removing Standalone IPTV/Sports Tabs](#3-tab-restructure--removing-standalone-iptvsports-tabs)
4. [RobbdeezeNutzHub Screen (Hub with 4 Glass Cards)](#4-robbdeezenutz-hub-screen-hub-with-4-glass-cards)
5. [VidNutz Hub — YouTube Video Browser](#5-vidnutz-hub--youtube-video-browser)
6. [MusicNutz Hub — Deezer + YouTube Music Player](#6-musicnutz-hub--deezer--youtube-music-player)
7. [IPTV Grayscale Theme (No Purple/Neon)](#7-iptv-grayscale-theme-no-purpleneon)
8. [EPG UI Removal from IPTV Screen](#8-epg-ui-removal-from-iptv-screen)
9. [Sports Hub — Performance Fixes & Integration](#9-sports-hub--performance-fixes--integration)
10. [Fork Branding & Visual Identity](#10-fork-branding--visual-identity)
11. [Key Design Patterns Used Across All Hubs](#11-key-design-patterns-used-across-all-hubs)
12. [API Credentials & Configuration](#12-api-credentials--configuration)
13. [Build & Release Checklist](#13-build--release-checklist)

---

## 1. Overview of Features

The fork transforms NuvioMobile from a standard media center into the **RobbdeezeNutz** ecosystem by:

| Feature | Description | Files |
|---|---|---|
| **RobbdeezeNutzHub** | Single hub tab with 4 glass-style cards replacing standalone IPTV/Sports tabs | `RobbdeezeNutzHubScreen.kt` |
| **VidNutz** | YouTube video browser with 12 categories, search, monochrome theme | `VidNutzModels.kt`, `VidNutzRepository.kt`, `VidNutzScreen.kt` |
| **MusicNutz** | Deezer metadata + YouTube full-length audio player | `MusicNutzModels.kt`, `MusicNutzRepository.kt`, `MusicNutzScreen.kt` |
| **IPTV Grayscale** | Black/grey/white color scheme (no purple/neon) | `IptvScreen.kt` (color constants) |
| **EPG Removed** | EPG UI stripped from IPTV screen; data layer preserved | `IptvScreen.kt` |
| **Fork Branding** | "Fork by RobbdeezeNutz" on auth screen; "RNutz Nuvio" app name | `AuthScreen.kt`, `strings.xml` |
| **Package Rename** | `app.robbdeezenutz.nuvio` / `app.robbdeezenutz.nuviodebug` | `androidApp/build.gradle.kts` |

---

## 2. Package Renaming & App Identification

### 2.1 Why This Matters
The stock NuvioMobile uses `com.nuvio.android` as its application ID. To install both apps side by side, the fork uses a **different application ID** while keeping the same **namespace** (which controls generated R class paths).

### 2.2 What Was Changed

#### `androidApp/build.gradle.kts`
```kotlin
android {
    namespace = "com.nuvio.android"  // ← KEPT SAME as stock (R class compat)
    
    defaultConfig {
        applicationId = "app.robbdeezenutz.nuvio"  // ← CHANGED to new ID
    }
}

// Debug builds get a separate ID
androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.applicationId.set("app.robbdeezenutz.nuviodebug")
    }
}
```

#### `composeApp/build.gradle.kts`
```kotlin
android {
    namespace = "com.nuvio.app"  // ← KEPT SAME as stock
}
```

### 2.3 App Name

Files changed (both must match):
- `androidApp/src/debug/res/values/strings.xml` → `<string name="app_name">RNutz Nuvio</string>`
- `composeApp/src/androidMain/res/values/strings.xml` → `<string name="app_name">RNutz Nuvio</string>`

### 2.4 Porting to TV Version

In the TV module's `build.gradle.kts`:
```kotlin
defaultConfig {
    applicationId = "app.robbdeezenutz.nuvio"  // or "app.robbdeezenutz.nuvio.tv"
}
```
If using a separate package on TV, ensure the `namespace` stays as `com.nuvio.android`/`com.nuvio.app` for R class compatibility.

---

## 3. Tab Restructure — Removing Standalone IPTV/Sports Tabs

### 3.1 The Change

**Before (stock):** 6 tabs → Home, Search, Library, IPTV, Sports, Settings  
**After (fork):** 5 tabs → Home, Search, Library, **RobbdeezeNutzHub**, Settings

### 3.2 Files Modified

#### A. `NativeNavigationTab` enum (`NativeTabBridge.kt`)
```kotlin
internal enum class NativeNavigationTab {
    Home,
    Search,
    Library,
    RobbdeezeNutzHub,  // ← REPLACED Iptv + Sports with single entry
    Settings,
}
```

#### B. `AppScreenTab` enum (`App.kt`)
```kotlin
enum class AppScreenTab {
    Home,
    Search,
    Library,
    RobbdeezeNutzHub,  // ← REPLACES Iptv/Sports
    Settings,
}
```

#### C. Mapping functions (`App.kt`)
Both `toNativeNavigationTab()` and `toAppScreenTab()` must handle the new enum.

#### D. `publishTabTitles()` signature (`NativeTabBridge.kt`)
```kotlin
// BEFORE (6 params):
fun publishTabTitles(home, search, library, profile, iptv, sports)

// AFTER (5 params):
fun publishTabTitles(home, search, library, profile, hub)
```

The `expect`/`actual` declarations in 3 files must match:
- `NativeTabBridge.kt` (commonMain — expect)
- `NativeTabBridge.android.kt` (androidMain — actual)
- `NativeTabBridge.ios.kt` (iosMain — actual)

#### E. Tab title publishing (`App.kt`)
```kotlin
NativeTabBridge.publishTabTitles(
    home = nativeTabHomeTitle,
    search = nativeTabSearchTitle,
    library = nativeTabLibraryTitle,
    profile = nativeTabProfileTitle,
    hub = "Hubz",  // ← NEW 5th param
)
```

#### F. `handleRootTabClick()` (`App.kt`)
```kotlin
AppScreenTab.RobbdeezeNutzHub -> {
    iptvScrollToTopRequests.tryEmit(Unit)
    sportsScrollToTopRequests.tryEmit(Unit)
    hubResetCounter++  // ← Resets sub-screens to main hub view on re-tap
}
```

#### G. Bottom navigation bar (`App.kt` → `NuvioNavigationBar`)
Replace the separate IPTV and Sports `NavItem` entries with a single Hub `NavItem`:
```kotlin
NavItem(
    selected = selectedTab == AppScreenTab.RobbdeezeNutzHub,
    onClick = { handleRootTabClick(AppScreenTab.RobbdeezeNutzHub) },
    icon = Res.drawable.sidebar_hub,  // ← NEW dashboard icon
    contentDescription = "Hubz",
)
```

#### H. Tablet top bar (`TabletFloatingTopBar`)
Must also show the Hub pill instead of IPTV + Sports pills.

### 3.3 Porting to TV

TV typically uses a different navigation (Leanback `BrowseFragment`, side navigation, etc.). The key changes:

1. Add **RobbdeezeNutzHub** as a navigation destination (row header, side nav item, etc.)
2. Remove standalone IPTV and Sports navigation entries
3. Wire the hub label as "Hubz" in the navigation UI
4. Ensure `publishTabTitles()` expect/actual signatures match if the TV variant uses native tab bridge

---

## 4. RobbdeezeNutz Hub Screen (Hub with 4 Glass Cards)

### 4.1 Architecture Overview

`RobbdeezeNutzHubScreen` is a **container composable** that manages internal sub-screen state. It shows:

- **Persistent title bar:** "RobbdeezeNutz Hubz" + back button (when on a sub-screen)
- **4 HubCard items:** IPTVNutz Hub, SportNutz Hub, VidNutz Hub, MusicNutz Hub
- **Inline sub-screens:** When a card is tapped, the content below the title bar swaps to the respective sub-screen (IPTV, Sports, VidNutz, Music)

### 4.2 Key Implementation Details

#### State Management
```kotlin
private enum class HubSubScreen : java.io.Serializable { Hub, Iptv, Sports, VidNutz, Music }

var subScreen by rememberSaveable { mutableStateOf(HubSubScreen.Hub) }
```

**Critical:** The enum must be `Serializable` for `rememberSaveable` to work across process recreation.

#### Tab Re-tap Reset
```kotlin
LaunchedEffect(resetTrigger) {
    subScreen = HubSubScreen.Hub
}
```
When the Hub tab is re-tapped in `App.kt`, `hubResetCounter` increments, this `LaunchedEffect` fires, and the view resets to the main hub card screen.

#### Tablet-aware Top Margin
```kotlin
BoxWithConstraints(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
    val isTablet = maxWidth >= 768.dp
    val topPad = if (isTablet) 96.dp else 32.dp
    // ... Column with padding(top = topPad) ...
}
```

#### HubCard Composable
```kotlin
@Composable
private fun HubCard(title: String, description: String, iconText: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(if (isFocused) Modifier.border(2.dp, Color.White, cardShape) else Modifier)
            .clickable(onClick = onClick)
            .focusable()                          // ← D-pad support
            .onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) SurfaceCard.copy(alpha = 1.2f) else SurfaceCard)
            .padding(20.dp),
    )
}
```

**D-pad key pattern:** Every interactive element uses `focusable() + onFocusChanged` with a white border ring when focused. This is essential for TV remote navigation.

### 4.3 Sub-screen Wiring

Each sub-screen is embedded as:
```kotlin
HubSubScreen.Iptv -> {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(...), verticalAlignment = Alignment.CenterVertically) {
            Text("IPTVNutz Hub", ...)  // Sub-screen header
        }
        Box(Modifier.fillMaxSize()) {
            IptvScreen(modifier = Modifier.fillMaxSize(), onPlayChannel = ..., scrollToTopRequests = ...)
        }
    }
}
```

The existing `IptvScreen` and `SportsScreen` composables are used directly — **they must not wrap themselves in Scaffold/TopAppBar** since the hub provides the header.

### 4.4 Porting to TV

- Replace `BoxWithConstraints` + `verticalScroll` with TV-friendly layout (e.g., Leanback `RowsFragment` or a vertical grid of cards)
- Keep the `focusable() + onFocusChanged` + border pattern — this is already TV-appropriate
- The `resetTrigger` mechanism works universally
- **HubCard** could become a TV card fragment or composable with a `Presenter`
- Sub-screen navigation can use Leanback `Fragment` transitions or inline composable switching
- `rememberSaveable` works the same in TV Compose

---

## 5. VidNutz Hub — YouTube Video Browser

### 5.1 Overview

VidNutz is a full YouTube browsing experience with:
- 12 video categories (Trending, Politics, News, Music, Sports, Documentary, Technology, Entertainment, Comedy, Science, True Crime, Food & Drink)
- Persistent search bar with 400ms debounce
- 1-column grid with 16:9 thumbnail cards
- Swipe left/right to change categories (80dp threshold)
- "Load More" pagination (not infinite scroll)
- D-pad focus on all interactive elements
- Monochrome color scheme (#000000 background, black/grey/white palette)
- JetBrains Mono font for metadata

### 5.2 Data Sources (Priority Order)

| Source | Page 1 | Page 2+ | Notes |
|---|---|---|---|
| `platformYouTubeSearch()` (NewPipeExtractor) | ✅ Primary | ❌ | Fastest; Android-only |
| Invidious API (5 instances) | ✅ Fallback | ✅ Primary | Fallback chain: nadeko.net → puffyan.us → yewtu.be → skyn3t.in → snopyta.org |
| Piped API (3 instances) | ❌ | ✅ Fallback | Fallback: kavin.rocks → lunar.icu → garudalinux.org |

### 5.3 Architecture

#### Models (`VidNutzModels.kt`)
```kotlin
data class VidNutzVideo(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val durationSeconds: Int,
    val viewCount: Long = 0,
    val uploadDate: String = "",
)

enum class VidNutzCategory(val displayName: String) {
    TRENDING("Trending"), POLITICS("Politics"), ...  // 12 total
}

data class VidNutzUiState(
    val selectedCategory: VidNutzCategory = VidNutzCategory.TRENDING,
    val videos: List<VidNutzVideo> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<VidNutzVideo>? = null,
    val searchCurrentPage: Int = 1,
    val searchHasMore: Boolean = true,
)
```

#### Repository (`VidNutzRepository.kt`)

A singleton `object` with these key methods:

**`fetchTrending(page)`**: Page 1 uses `platformYouTubeSearch("trending")`. Page 2+ iterates through Invidious instances calling `/api/v1/trending?type=video&page=N`.

**`search(query, page)`**: Page 1 uses `platformYouTubeSearch(query)`. Page 2+ tries Invidious first (`/api/v1/search?q=...&type=video&sort=relevance&page=N`), then Piped (`/search?q=...&filter=videos&page=N`).

**`fetchByCategory(category, page)`**: Trends maps to `fetchTrending()`. All others use a hardcoded query string passed to `search()`.

**`resolveStream(videoId)`**: Delegates to `YouTubeStreamResolver.resolveStream(videoId)` (NewPipeExtractor stream resolution).

**Filtering:** Videos with duration < 30s or > 1800s are filtered out. Results capped at 20 per page.

#### Thumbnail generation
```kotlin
thumbnail = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
```

### 5.4 Screen Implementation (`VidNutzScreen.kt`)

**Search bar:**
```kotlin
OutlinedTextField(...)
// 400ms debounce:
searchJob = scope.launch {
    delay(400)
    val results = VidNutzRepository.search(q)
    uiState = uiState.copy(searchResults = results, ...)
}
```

**Category chips row:**
Horizontal scroll of pill-shaped chips. Selected chip is white background with black text. Focused-only chip shows white outline.

**Grid:**
`LazyVerticalGrid(columns = GridCells.Fixed(1))` — single column for phone; TV could use `GridCells.Fixed(2)` or `GridCells.Adaptive(minSize)`.

**Video card:**
- 16:9 aspect ratio thumbnail with 12dp corner radius
- Play overlay (centered, circular icon, dims on focus)
- Duration badge (bottom-right, black semi-transparent background, monospace)
- Title (max 2 lines)
- Channel name (monospace, grey)
- View count + upload date (monospace, light grey)

**Load More button:**
At bottom of grid as a trailing `item()`. Shows spinner or "Load More" button. Explicit load (not infinite scroll) for reliability.

**Swipe gesture:**
```kotlin
.pointerInput(uiState.searchQuery.isBlank()) {
    if (uiState.searchQuery.isBlank()) {
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> ... },
            onDragEnd = { swipeAccumulator = 0f },
            onDragCancel = { swipeAccumulator = 0f },
        )
    }
}
```
80dp threshold; wraps around the category list; disabled while searching.

**Playback flow:**
1. User taps video
2. `VidNutzRepository.resolveStream(videoId)` called
3. If stream URL obtained, creates `PlayerLaunch` and fires `onPlayChannel`
4. `PlayerLaunch` is routed through existing player infrastructure

### 5.5 Color Scheme
```kotlin
private val ObsidianBg = Color(0xFF000000)      // Pure black
private val SurfaceLow = Color(0xFF121212)       // Dark charcoal
private val SurfaceCard = Color(0xFF1B1B1B)      // Card surface
private val OnSurface = Color(0xFFFFFFFF)         // White text
private val OnSurfaceVariant = Color(0xFFB0B0B0) // Light grey
private val TertiaryText = Color(0xFF888888)      // Muted grey
private val BorderColor = Color(0xFF2A2A2A)       // Subtle borders
private val InputBg = Color(0xFF121212)           // Search bg
private val FocusRing = Color(0xFFFFFFFF)          // White focus ring
```

### 5.6 Porting to TV

| Phone Implementation | TV Equivalent |
|---|---|
| `LazyVerticalGrid(columns = GridCells.Fixed(1))` | `LazyVerticalGrid(columns = GridCells.Fixed(4))` or Adaptive for bigger screens |
| Swipe gesture (`detectHorizontalDragGestures`) | D-pad left/right on category chips or dedicated category navigation |
| `clickable + focusable + onFocusChanged` | Same pattern; TV remote uses D-pad focus by default |
| `AsyncImage` with `coil3` | Same (Coil works on TV) |
| `PlayerLaunch → onPlayChannel` | Same routing; TV may use Leanback `VideoFragment` instead of ExoPlayer composable |
| Search bar with `OutlinedTextField` | Same; TV may want a full-screen `SearchFragment` for keyboard input |
| `platformYouTubeSearch()` (NewPipeExtractor) | Works on Android TV; iOS TV would need Invidious/Piped fallback |
| 1-column grid | Increase columns for TV: `GridCells.Fixed(3)` or `GridCells.Adaptive(300.dp)` |
| Card sizes | Make thumbnails larger (aspect ratio 16:9, wider cards) |

**Specific TV adjustments:**
- Increase thumbnail size and font sizes (TV is viewed from farther away)
- Remove swipe gesture (TV doesn't have touch); use D-pad left/right on chips to change categories
- Add `Modifier.onPreviewKeyEvent()` for additional D-pad navigation if needed
- Consider adding a full-screen search dialog with Leanback's `SearchSupportFragment`
- Card focus should show a larger shadow/glow for visibility at a distance

---

## 6. MusicNutz Hub — Deezer + YouTube Music Player

### 6.1 Overview

MusicNutz provides music browsing and playback with:
- 12 genre categories (Trending, New Releases, Rock, Hip-Hop, Electronic, Pop, R&B, Jazz, Classical, Country, Metal, Indie)
- Tracks/Albums mode toggle
- **Metadata:** Deezer public API (free, no key required)
- **Audio playback:** YouTube full-length audio (not Deezer 30s previews)
- 2-column grid with square (1:1) album art
- Album detail view with full tracklist
- Swipe left/right for categories
- "Load More" pagination
- D-pad focus on all elements
- Same monochrome palette as VidNutz

### 6.2 Data Sources

| Data | Source | API Endpoint | Notes |
|---|---|---|---|
| Track search | Deezer | `https://api.deezer.com/search/track?q=...` | Free, no key |
| Album search | Deezer | `https://api.deezer.com/search/album?q=...` | Free, no key |
| Chart/trending | Deezer | `https://api.deezer.com/chart/0/tracks` | Free, no key |
| Album tracks | Deezer | `https://api.deezer.com/album/{id}/tracks` | Free, no key |
| Album detail | Deezer | `https://api.deezer.com/album/{id}` | Free, no key |
| **Audio playback** | **YouTube** | `platformYouTubeSearch + YouTubeStreamResolver` | Full-length audio (not 30s) |
| Audio fallback | Deezer | `track.preview` (30s MP3) | Only if YouTube fails |

### 6.3 Architecture

#### Models (`MusicNutzModels.kt`)
```kotlin
data class MusicTrack(
    val id: Long, val title: String, val artistName: String,
    val albumName: String, val albumCover: String,
    val durationSeconds: Int, val previewUrl: String?,
)

data class MusicAlbum(
    val id: Long, val title: String, val artistName: String,
    val coverUrl: String, val releaseDate: String, val trackCount: Int,
)

enum class MusicNutzMode { TRACKS, ALBUMS }

data class MusicNutzUiState(
    val selectedCategory: MusicNutzCategory, val tracks: List<MusicTrack>,
    val isLoading, isLoadingMore, currentPage, hasMore,
    val searchQuery, searchResults, searchCurrentPage, searchHasMore,
    val mode: MusicNutzMode, val albums: List<MusicAlbum>,
    val albumResults, albumPage, albumHasMore, isLoadingAlbums,
    val selectedAlbum: MusicAlbum?, val albumTracks: List<MusicTrack>,
    val isLoadingAlbumTracks: Boolean,
)
```

#### Repository (`MusicNutzRepository.kt`)

Key methods:

**`fetchTrending(page)`**: Deezer chart endpoint with pagination (`index = (page-1) * 20`).

**`search(query, page)`**: Deezer track search.

**`fetchByCategory(category, page)`**: Trends → trending, New Releases → chart albums + per-album first track fetch, others → Deezer search by genre query.

**`searchAlbums(query, page)`**: Deezer album search.

**`fetchAlbumsByCategory(category, page)`**: Similar to `fetchByCategory` but returns albums.

**`fetchAlbumTracks(albumId)`**: Gets full tracklist for an album.

**`resolveStream(track)`**: CRITICAL — the audio source:
```kotlin
suspend fun resolveStream(track: MusicTrack): StreamResult? {
    // Always try YouTube first for full-length audio
    val query = "${track.title} ${track.artistName} audio"
    val searchResults = platformYouTubeSearch(query)
    if (searchResults != null && searchResults.isNotEmpty()) {
        val stream = YouTubeStreamResolver.resolveStream(searchResults.first().videoId)
        if (stream != null) return stream
    }
    // Fallback: Deezer 30s preview
    if (track.previewUrl != null) {
        return StreamResult(url = track.previewUrl, headers = emptyMap())
    }
    return null
}
```

### 6.4 Screen Implementation (`MusicNutzScreen.kt`)

**Search bar:** Same pattern as VidNutz with 400ms debounce. Adapts placeholder text based on mode ("Search songs..." / "Search albums...").

**Mode toggle:** Two pill chips — "Tracks" and "Albums" — below the category chips. Switching mode clears search results and reloads data from the appropriate endpoint.

**Track grid:** `LazyVerticalGrid(columns = GridCells.Fixed(2))`. Each card shows:
- Square (1:1) album art with play overlay
- Duration badge
- Title (max 1 line)
- Artist name
- Album name

**Album grid:** Same 2-column layout. Tapping an album opens `AlbumDetailView`.

**Album detail view (`AlbumDetailView`):**
```kotlin
Column {
    // Back button + album title row
    LazyColumn {
        item { /* header: 200dp album art, title, artist, year, track count */ }
        items(tracks) { track -> AlbumTrackRow(track) { onPlayTrack(track) } }
    }
}
```

**Playback flow:**
```kotlin
fun playTrack(track: MusicTrack) {
    scope.launch {
        val result = MusicNutzRepository.resolveStream(track)
        if (result != null && onPlayChannel != null) {
            onPlayChannel(PlayerLaunch(
                profileId = 0,
                title = track.title,
                sourceUrl = result.url,
                sourceHeaders = result.headers,
                poster = track.albumCover,      // ← Album art shown in player
                streamTitle = track.title,
                streamSubtitle = "${track.artistName} · ${track.albumName}",
                providerName = "YouTube",
                parentMetaId = "music",
                parentMetaType = "music",
            ))
        }
    }
}
```

**Critical:** The `poster` parameter in `PlayerLaunch` displays the album art in the player UI.

### 6.5 Color Scheme

Identical to VidNutz:
```kotlin
private val ObsidianBg = Color(0xFF000000)
private val SurfaceLow = Color(0xFF121212)
private val SurfaceCard = Color(0xFF1B1B1B)
private val OnSurface = Color(0xFFFFFFFF)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val TertiaryText = Color(0xFF888888)
private val BorderColor = Color(0xFF2A2A2A)
private val InputBg = Color(0xFF121212)
private val FocusRing = Color(0xFFFFFFFF)
```

### 6.6 Porting to TV

| Phone Implementation | TV Equivalent |
|---|---|
| `LazyVerticalGrid(columns = GridCells.Fixed(2))` | Increase to `GridCells.Fixed(5)` or `GridCells.Adaptive(250.dp)` |
| Square 1:1 album art | Keep 1:1 but larger (e.g., 200dp+ on TV) |
| Album detail view | Same inline view or dedicated Leanback `DetailsFragment` |
| Swipe gesture | Replace with D-pad navigation on chips |
| Same monochrome palette | Keep identical — OLED-friendly for TV |
| Deezer API | Same; works on any platform with HTTP |
| `platformYouTubeSearch` | Works on Android TV; iOS TV needs Invidious/Piped fallback |

**TV-specific enhancements to consider:**
- Larger text (14-16sp minimum, 18-20sp for titles)
- Focus scale animation (`Modifier.graphicsLayer { scaleX/Y = if (focused) 1.1f else 1f }`)
- Rounded corners should be larger on TV (16-20dp)
- Album art in detail view could be 300-400dp on TV
- Track list rows should be taller (48-56dp) for easy D-pad targeting

---

## 7. IPTV Grayscale Theme (No Purple/Neon)

### 7.1 What Changed

The stock NuvioMobile IPTV screen uses purple/neon color accents. The fork replaced all colors with grayscale.

### 7.2 Color Constants

**Before (stock)** — approximate:
```kotlin
private val NeonPurple = Color(0xFFBB86FC)
private val ElectricBlue = Color(0xFF03DAC6)
private val SurfaceBg = Color(0xFF0B1326)  // Dark navy
```

**After (fork):**
```kotlin
private val ObsidianBg = Color(0xFF000000)       // Pure black
private val GlassBg = Color(0xFF1A1A1A).copy(alpha = 0.7f)
private val SurfaceLow = Color(0xFF111111)
private val SurfaceVariant = Color(0xFF252525)
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFE0E0E0)
private val OnSurfaceVariant = Color(0xFFB0B0B0)
private val OutlineVariant = Color(0xFF3A3A3A)
private val InputBg = Color(0xFF0F0F0F)
private val AccentGray = Color(0xFFCCCCCC)
private val FavoriteRed = Color(0xFFE91E63)
```

### 7.3 Files Changed

- `IptvScreen.kt` — all color constants replaced
- Any composable that referenced the old color constants

### 7.4 Porting to TV

- Replace color constants in the TV version's IPTV screen with the same grayscale palette
- Ensure all `@Composable get()` color delegates use `MaterialTheme.colorScheme` for dynamic theming if needed, or hardcode the grayscale constants for consistency

---

## 8. EPG UI Removal from IPTV Screen

### 8.1 What Was Removed

The following EPG UI elements were removed from `IptvScreen.kt`:
- EPG promotion banner (at the top of the channel list)
- Now/next program display on channel cards
- Long-press EPG detail sheet
- EPG tab in the add-source bottom sheet
- EPG add form (`EpgAddForm`)

### 8.2 What Was Preserved

The EPG **data layer** was kept intact:
- `EpgParser.kt` — XMLTV parser (including streaming `parseXmltvStream`)
- `IptvStorage.kt` — file-based EPG cache
- EPG data models
- EPG repository methods

This means EPG data can still be loaded and cached, it's just not displayed in the UI. This was done to leave the door open for a future, simpler EPG implementation (see "Planned" section in `version.md`).

### 8.3 Porting to TV

- Remove the same EPG UI elements from the TV IPTV screen
- Keep the EPG data layer if desired for future use
- OR implement the simpler EPG approach from `yesnt10/NuvioMobile-Enhanced`:
  - Parse `url-tvg` / `x-tvg-url` from M3U headers
  - Use regex-based parser for small files
  - Store only current program per channel
  - Match by `tvg-id`
  - Load in background coroutine

---

## 9. Sports Hub — Performance Fixes & Integration

### 9.1 SportsScreen Stripped of Standalone Scaffold

When integrated into the Hub, `SportsScreen` no longer wraps itself in its own `Scaffold`/`TopAppBar`. The Hub provides the header.

**Before:** SportsScreen had its own `Scaffold` + `TopAppBar` with "Sports Hub" title.  
**After:** SportsScreen receives `modifier: Modifier = Modifier` and renders content only.

The LazyColumn still has `PullToRefreshBox` for refresh.

### 9.2 Performance Changes (Reverted)

Two-phase loading (fast initial + background detail) was tried and **reverted** due to force closes. The repository is back to:
- Original sequential prioritized ESPN fetch (one sport at a time)
- 5s timeout per sport (was reduced to 3s, changed back)
- Sequential Invidious/Piped fallback for YouTube highlights (parallel was reverted)
- Sequential 6-URL ESPN news fetch (parallel was reverted)

### 9.3 Porting to TV

- If incorporating Sports into a Hub structure, strip the standalone Scaffold/TopAppBar
- Keep the sequential fetch pattern (it's stable)
- The `PullToRefreshBox` works on TV
- D-pad focus needs to be added to sports cards, chips, and navigation elements

---

## 10. Fork Branding & Visual Identity

### 10.1 App Name

Set in two `strings.xml` files:
- `androidApp/src/debug/res/values/strings.xml`: `<string name="app_name">RNutz Nuvio</string>`
- `composeApp/src/androidMain/res/values/strings.xml`: `<string name="app_name">RNutz Nuvio</string>`

The TV version needs the same strings in its resources.

### 10.2 Fork Attribution on Auth Screen

In `AuthScreen.kt`, inside the `AuthBrandLockup` composable:
```kotlin
Text(
    text = "Fork by RobbdeezeNutz",
    style = MaterialTheme.typography.bodySmall.copy(
        color = AuthTextSecondary.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
    ),
)
```
This appears under the Nuvio logo and tagline on the login/splash screen.

### 10.3 Hub Tab Icon

`sidebar_hub.xml` — a simple dashboard/canvas icon (4 squares in a grid):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M3,13h8V3H3V13zM3,21h8v-6H3V21zM13,21h8V11h-8V21zM13,3v6h8V3H13z"/>
</vector>
```
Place this in `composeApp/src/commonMain/composeResources/drawable/sidebar_hub.xml`.

---

## 11. Key Design Patterns Used Across All Hubs

### 11.1 D-pad Focus Pattern (TV Remote Support)

Every interactive element uses this pattern:
```kotlin
var isFocused by remember { mutableStateOf(false) }

Box(
    modifier = Modifier
        .focusable()
        .onFocusChanged { isFocused = it.isFocused }
        .then(if (isFocused) Modifier.border(2.dp, Color.White, shape) else Modifier)
)
```

**For TV:** Consider also adding:
```kotlin
.graphicsLayer {
    scaleX = if (isFocused) 1.05f else 1f
    scaleY = if (isFocused) 1.05f else 1f
}
.transition { ... } // Animate the scale change
```

### 11.2 State Persistence

All sub-screen state uses `rememberSaveable` (not `remember`) so it survives tab switches and process recreation. The `HubSubScreen` enum is `Serializable` to support this.

### 11.3 Pagination Pattern

```kotlin
// Load More button at bottom of grid
item(key = "__load_more__") {
    if (isLoadingMore) {
        CircularProgressIndicator(...)
    } else if (hasMore && items.isNotEmpty()) {
        Button(onClick = { loadMore() }) { Text("Load More") }
    }
}
```

The `loadMore()` function:
1. Checks if already loading or no more data
2. Determines next page number (separate counters for normal vs search)
3. Fetches more items
4. Appends to existing list or marks `hasMore = false` if empty result

### 11.4 Search with Debounce

```kotlin
var searchJob by remember { mutableStateOf<Job?>(null) }

searchJob?.cancel()
searchJob = scope.launch {
    delay(400)  // 400ms debounce
    val results = Repository.search(query)
    uiState = uiState.copy(searchResults = results, ...)
}
```

### 11.5 Playback Routing

All hubs use the same pattern to initiate playback:
```kotlin
onPlayChannel(PlayerLaunch(
    profileId = 0,
    title = ...,
    sourceUrl = result.url,
    sourceHeaders = result.headers,
    poster = ...,  // Optional: album art for Music, etc.
    streamTitle = ...,
    streamSubtitle = ...,  // Optional
    providerName = "YouTube" or "Deezer",
    parentMetaId = "youtube" or "music",
    parentMetaType = "youtube" or "music",
))
```

The `onPlayChannel` callback is wired in `App.kt` → `AppTabHost` to create a `PlayerLaunchStore` entry and navigate to `PlayerRoute`.

---

## 12. API Credentials & Configuration

### 12.1 Deezer

**No API key required.** The public API at `https://api.deezer.com` is free and rate-limited per IP. No registration needed.

### 12.2 YouTube (via NewPipeExtractor / platformYouTubeSearch)

**No API key required.** The `platformYouTubeSearch()` function uses:
- On **Android:** NewPipeExtractor's `SearchExtractor` with OkHttp `Downloader` — no API key
- On **iOS:** Falls back to Invidious/Piped instances (also no key)

### 12.3 Invidious & Piped Instances

Hardcoded fallback lists in `VidNutzRepository.kt`:
```kotlin
private val invidiousInstances = listOf(
    "https://inv.nadeko.net",
    "https://vid.puffyan.us",
    "https://yewtu.be",
    "https://inv.skyn3t.in",
    "https://invidious.snopyta.org",
)

private val pipedInstances = listOf(
    "https://pipedapi.kavin.rocks",
    "https://pipedapi.lunar.icu",
    "https://piped-api.garudalinux.org",
)
```

**Note:** These instances come and go. If they go offline, the app silently falls through to the next instance. You may need to update this list periodically.

### 12.4 Trakt Credentials (already in project)

```properties
TRAKT_CLIENT_ID=<value>
TRAKT_CLIENT_SECRET=<value>
TRAKT_REDIRECT_URI=nuvio://auth/trakt
```
Stored in `local.properties` (gitignored). Same credentials work for TV.

---

## 13. Build & Release Checklist

### 13.1 Build Command
```bash
./gradlew :androidApp:assembleDebug -Pnuvio.android.distribution=full
```

### 13.2 APK Output
```
androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk
```

### 13.3 JVM Requirement
Java 17 (JDK 17). Set `JAVA_HOME` accordingly.

### 13.4 Pre-release Checks
- [ ] Package name unique (`app.robbdeezenutz.nuvio` or variant)
- [ ] App name set in both `strings.xml` files
- [ ] Fork branding on auth screen
- [ ] Hub tab label says "Hubz"
- [ ] All 4 hub sub-screens functional
- [ ] D-pad focus works on all interactive elements
- [ ] VidNutz search with debounce works
- [ ] MusicNutz audio plays full YouTube tracks (not just Deezer previews)
- [ ] Album art shows in player (poster parameter)
- [ ] Swipe gestures disabled while searching
- [ ] Load More pagination works for both VidNutz and MusicNutz
- [ ] Tab re-tap resets to main hub view
- [ ] IPTV screen has grayscale theme (no purple)
- [ ] EPG UI not visible on IPTV
- [ ] Sports stable (sequential fetch, not parallel)
- [ ] Invidious/Piped instances are all online

### 13.5 GitHub Release
Tag format: `v{version}-rnutz` (e.g., `v1.0.0-rnutz`)

Release notes should include:
- Major features (VidNutz, MusicNutz, Hub restructure)
- Any upstream merges
- Download link to APK

---

## Appendix A: File Map

| New/Modified File | Purpose |
|---|---|
| `composeApp/src/.../hub/RobbdeezeNutzHubScreen.kt` | Main hub with 4 glass cards + inline sub-screens |
| `composeApp/src/.../hub/VidNutzModels.kt` | VidNutz video/category/state models |
| `composeApp/src/.../hub/VidNutzRepository.kt` | VidNutz data layer (Invidious + Piped + NewPipe) |
| `composeApp/src/.../hub/VidNutzScreen.kt` | VidNutz UI (search, chips, grid, load more, swipe) |
| `composeApp/src/.../hub/MusicNutzModels.kt` | MusicNutz track/album/category/state models |
| `composeApp/src/.../hub/MusicNutzRepository.kt` | MusicNutz data layer (Deezer + YouTube) |
| `composeApp/src/.../hub/MusicNutzScreen.kt` | MusicNutz UI (search, chips, toggle, grids, album detail) |
| `composeApp/src/.../core/ui/NativeTabBridge.kt` | Tab bridge — removed Iptv/Sports, added RobbdeezeNutzHub |
| `composeApp/src/.../core/ui/NativeTabBridge.android.kt` | Android expect/actual for 5-param `publishTabTitles` |
| `composeApp/src/.../core/ui/NativeTabBridge.ios.kt` | iOS expect/actual for 5-param `publishTabTitles` |
| `composeApp/src/.../App.kt` | Tab enum, hub wiring, reset counter, AppTabHost |
| `composeApp/src/.../features/iptv/IptvScreen.kt` | Grayscale theme, EPG UI removed |
| `composeApp/src/.../features/sports/SportsScreen.kt` | Stripped of standalone Scaffold for hub integration |
| `composeApp/src/.../features/auth/AuthScreen.kt` | "Fork by RobbdeezeNutz" text |
| `composeApp/src/commonMain/composeResources/drawable/sidebar_hub.xml` | Dashboard icon for hub tab |
| `androidApp/build.gradle.kts` | Package name changed |
| `androidApp/src/debug/res/values/strings.xml` | "RNutz Nuvio" app name |
| `composeApp/src/androidMain/res/values/strings.xml` | "RNutz Nuvio" app name |

---

## Appendix B: Common Issues & Solutions

### Issue: RememberSaveable crash with non-Serializable enum
**Solution:** Make the enum `Serializable`:
```kotlin
private enum class HubSubScreen : java.io.Serializable { Hub, Iptv, Sports, VidNutz, Music }
```

### Issue: VidNutz/MusicNutz shows no results
**Solution:** Check Invidious/Piped instance availability. The first instance in the list may be down; the code silently tries the next one. Add logging or temporarily test each instance URL in a browser.

### Issue: MusicNutz plays 30s Deezer preview instead of full song
**Solution:** The `resolveStream()` method tries YouTube first. If `platformYouTubeSearch` returns null or `YouTubeStreamResolver.resolveStream` fails, it falls back to Deezer preview. Check:
1. `platformYouTubeSearch` works (NewPipeExtractor initialized)
2. YouTube stream resolution succeeds

### Issue: App crashes on launch with ClassNotFoundException
**Solution:** Ensure all `expect`/`actual` functions match signatures. The `publishTabTitles` function must have exactly 5 params in all 3 files.

### Issue: Hub tab re-tap doesn't reset sub-screen
**Solution:** Check that `hubResetCounter++` is called in `handleRootTabClick()` and `resetTrigger` is passed to `RobbdeezeNutzHubScreen`.

---

> **Last updated:** July 2026  
> **Source:** Nuvio_Robbdeeze project at `/Users/robbdeeze/Documents/projects/Nuvio_Robbdeeze`
