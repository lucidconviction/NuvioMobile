---
name: NutzTube
colors:
  surface: '#000000'
  surface-dim: '#000000'
  surface-bright: '#1A1A1A'
  surface-container-lowest: '#000000'
  surface-container-low: '#0F0F0F'
  surface-container: '#121212'
  surface-container-high: '#1A1A1A'
  surface-container-highest: '#252525'
  on-surface: '#E0E0E0'
  on-surface-variant: '#B0B0B0'
  inverse-surface: '#E0E0E0'
  inverse-on-surface: '#1A1A1A'
  outline: '#3A3A3A'
  outline-variant: '#2A2A2A'
  surface-tint: '#CCCCCC'
  primary: '#FFFFFF'
  on-primary: '#000000'
  primary-container: '#E0E0E0'
  on-primary-container: '#1A1A1A'
  secondary: '#E91E63'
  on-secondary: '#FFFFFF'
  secondary-container: '#3A0D20'
  on-secondary-container: '#FFB3C6'
  tertiary: '#7C3AED'
  on-tertiary: '#FFFFFF'
  tertiary-container: '#1A0A3A'
  on-tertiary-container: '#D2BBFF'
  error: '#FFB4AB'
  on-error: '#690005'
  error-container: '#93000A'
  on-error-container: '#FFDAD6'
  background: '#000000'
  on-background: '#E0E0E0'
  surface-variant: '#1A1A1A'
typography:
  headline-lg:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  headline-sm:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.4'
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin: 16px
---

## Brand & Style
NutzTube is a YouTube-style video browsing tab built on NewPipeExtractor. The brand personality is **Clean, Immersive, and Content-First**. The interface should feel like a premium video platform — think a dark-mode YouTube with the precision of a high-end media player.

The visual style follows **Cinematic Noir** design principles: absolute black OLED backgrounds, elevated surfaces in charcoal gray, and high-contrast white text. The interface fades into the background, putting all focus on video thumbnails and titles.

## Screens

### Main Feed / Browse
A scrollable feed of video content in a responsive grid layout. The screen is divided into:

1. **Top Bar** — Persistent "NutzTube" branding (pink accent #E91E63) with a search icon on the right.
2. **Category Chips** — Horizontally scrollable pill-shaped filters: Trending, Music, Gaming, News, Sports, Education, Entertainment. Active chip has white text on dark gray background. Inactive chips have muted gray text with subtle outline.
3. **Video Grid** — A 2-column grid of video cards. Each card shows:
   - **Thumbnail** — 16:9 aspect ratio, rounded corners (0.75rem). Duration badge in bottom-right corner (black pill with white text, JetBrains Mono).
   - **Title** — Below thumbnail, white text, max 2 lines, Geist medium 14px.
   - **Channel Name** — Muted gray, Geist regular 12px.
   - **Metadata Row** — Views count + upload time, muted gray, JetBrains Mono 11px.
4. **Infinite Scroll** — As user scrolls, more videos load via pagination. A subtle loading indicator at the bottom.

### Search View
When the search icon is tapped, the top bar transforms into a search field:
- Dark input background (#121212) with 1px outline (#3A3A3A)
- White cursor, white text input
- Placeholder text in muted gray
- X button to clear, back arrow to exit search
- Results appear in the same 2-column video grid format

### Video Detail / Player
Tapping a video card transitions to a full-screen player view:
- Video plays in ExoPlayer (existing infrastructure)
- Below the player: video title (headline-md), channel name, view count, upload date
- Description section (collapsible, body-md)
- Related videos section below (horizontal scroll row of smaller cards)

## Layout & Spacing
The layout follows a **Mobile-First** approach optimized for thumb scrolling.

- **Phone:** 2-column grid, 16px margins, 12px gap between cards.
- **Tablet:** 3-column grid, 24px margins, 16px gap.
- **Compact spacing** for the video grid to maximize content density (8px between title/metadata).
- **Spacious spacing** for top bar and category chips (16px padding).

## Elevation & Depth
Depth is created through surface color stepping against the absolute black background.

- **Level 0 (Floor):** #000000 (Absolute Black) — the main canvas.
- **Level 1 (Cards):** #121212 (Deep Charcoal) — video cards, search bar.
- **Level 2 (Active/Selected):** #1A1A1A — hovered or selected chips.
- **Level 3 (Overlays):** #252525 — modals, bottom sheets.

Cards are defined by subtle 1px borders (#2A2A2A) rather than shadows. Selected/active elements use the pink accent (#E91E63) as a 2px left border or underline indicator.

## Shapes
The shape language is designed to be approachable and modern — softer than traditional media apps but not overly rounded.

- **Video Thumbnails:** 0.75rem (12px) roundedness.
- **Category Chips:** 9999px (fully pill-shaped).
- **Buttons:** 0.5rem (8px) — slightly rounded for a professional feel.
- **Search Input:** 1rem (16px) roundedness to match the overall theme.
- **Duration Badges:** 0.25rem (4px) — sharp, informational, utilitarian.

## Components

### Video Card
```
┌──────────────────────┐
│                      │
│     THUMBNAIL        │
│     16:9             │
│               [4:32] │ ← duration badge
├──────────────────────┤
│ Video Title Here     │ ← white, 2 lines max
│ Channel Name         │ ← gray, 1 line
│ 12K views · 3d ago   │ ← muted gray, mono
└──────────────────────┘
```
- **Background:** #121212 card with 1px #2A2A2A border.
- **Duration badge:** Black pill (#000000 @ 80%) in bottom-right of thumbnail, white JetBrains Mono text.
- **Title:** White, Geist medium 14px, max 2 lines with ellipsis.
- **Channel name:** Muted gray (#B0B0B0), Geist regular 12px.
- **Metadata:** Even more muted (#888888), JetBrains Mono 11px. Bullet separator between views and time.

### Category Chips
- **Inactive:** Transparent background, 1px #3A3A3A border, #B0B0B0 text.
- **Active:** #1A1A1A background, white text, 1px #3A3A3A border.
- Minimum 48dp touch target height.

### Search Bar
- **Container:** #121212 background, 1px #3A3A3A border, 1rem rounded.
- **Text:** White, Geist body-md.
- **Placeholder:** Muted gray (#666666).
- **Icons:** White with 60% opacity.
- **Focus state:** Border transitions to white.

### Loading / Empty State
- **Loading:** Skeleton shimmer placeholders matching card dimensions. Use a linear gradient animation across #1A1A1A → #252525 → #1A1A1A.
- **Empty:** Centered icon + "No videos found" text in muted gray. "Try a different search" subtitle.

## Accent Colors
- **Pink (#E91E63):** NutzTube brand accent. Used sparingly for the "NutzTube" wordmark, active tab indicator, and play button overlays on thumbnails.
- **Purple (#7C3AED):** Secondary accent for bookmarks/favorites/later buttons.
- **White (#FFFFFF):** Primary text and active states.

## Interactions
- **Tap card** → Opens video player with crossfade transition.
- **Long press** → Context menu: "Add to Watch Later", "Share", "Open Channel".
- **Swipe back** from player returns to the scroll position.
- **Pull-to-refresh** on the feed re-fetches trending videos.
