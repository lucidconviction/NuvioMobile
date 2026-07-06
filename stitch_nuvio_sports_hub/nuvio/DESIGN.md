---
name: Nuvio
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#ffffff'
  on-secondary: '#283500'
  secondary-container: '#c3f400'
  on-secondary-container: '#556d00'
  tertiary: '#fff4f1'
  on-tertiary: '#5e1700'
  tertiary-container: '#ffcfc1'
  on-tertiary-container: '#af3200'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#c3f400'
  secondary-fixed-dim: '#abd600'
  on-secondary-fixed: '#161e00'
  on-secondary-fixed-variant: '#3c4d00'
  tertiary-fixed: '#ffdbd0'
  tertiary-fixed-dim: '#ffb59e'
  on-tertiary-fixed: '#3a0b00'
  on-tertiary-fixed-variant: '#852400'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Anybody
    fontSize: 48px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Anybody
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-lg-mobile:
    fontFamily: Anybody
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Anybody
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '700'
    lineHeight: '1'
    letterSpacing: 0.1em
  stats-num:
    fontFamily: Anybody
    fontSize: 20px
    fontWeight: '800'
    lineHeight: '1'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  container-margin-desktop: 40px
  container-margin-mobile: 16px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style
The design system is engineered for a high-performance sports ecosystem. The brand personality is **Dynamic, Authoritative, and Energetic**. It aims to evoke the adrenaline of a live stadium atmosphere while maintaining the precision of professional sports analytics. 

The aesthetic follows a **Modern Corporate-Athletic** style: a deep dark-mode foundation paired with high-chroma accents. It utilizes subtle "glass" overlays to maintain depth without cluttering the data-heavy interface. The goal is to provide a "command center" feel for athletes and fans alike, where live data feels urgent and static content feels premium.

## Colors
The palette is built on a **Deep Navy foundation (#020617)** to ensure maximum contrast for vibrant data visualizations. 

- **Primary (Electric Blue):** Used for primary actions, active states, and brand highlights.
- **Secondary (Neon Green):** Reserved for "win" states, positive momentum, and growth metrics.
- **Tertiary (Sunset Orange):** Used for secondary call-outs, energy-focused UI elements, and warnings.
- **Live Indicator (Stadium Red):** A high-visibility red specifically for real-time game clocks and "Live" badges.
- **Surface Tiers:** Use varying shades of charcoal and navy to create depth, moving from the base background to elevated cards.

## Typography
Typography is optimized for speed of reading and impact. 

- **Display & Headlines:** Uses **Anybody**. Its variable width and aggressive weights reflect the movement and power of sports. It should be used for player names, scores, and major section headings.
- **Body:** Uses **Hanken Grotesk**. This provides a clean, modern, and highly legible experience for long-form news and athlete bios.
- **Data & Labels:** Uses **JetBrains Mono**. Monospaced numerals are essential for scoreboards and live clocks to prevent layout jitter as numbers change.
- **Stylistic Note:** Use a slight italic lean for score displays to imply forward motion and speed.

## Layout & Spacing
The layout follows a **Fluid Grid System** with a strict 4px baseline rhythm.

- **Desktop:** 12-column grid with 24px gutters. Content is typically centered in a 1280px max-width container.
- **Mobile:** 4-column grid with 16px margins.
- **Information Density:** For live stats and play-by-play feeds, use "Compact" spacing (8px between items). For editorial and marketing content, use "Spacious" spacing (24px-32px).
- **Safe Areas:** Ensure bottom navigation and floating action buttons respect mobile device safe areas, especially during high-interaction "live" moments.

## Elevation & Depth
Depth is achieved through **Tonal Layering** and **Subtle Glows** rather than traditional drop shadows.

- **Level 0 (Base):** Deep Navy (#020617).
- **Level 1 (Cards):** Slate Blue (#1E293B) with a 1px inner border of 10% white to define edges.
- **Level 2 (Modals/Popovers):** Darker Slate with a subtle outer glow using the Primary color at 5% opacity to simulate light emission.
- **Glassmorphism:** Use backdrop-blur (12px-16px) for sticky headers and navigation bars to maintain the sense of the content "scrolling under" the UI.

## Shapes
The shape language is **Athletic and Modern**. 

Elements use a consistent **8px (0.5rem)** radius for standard cards and containers. This provides a professional balance between "aggressive" sharp corners and "too-friendly" pill shapes. 

- **Buttons:** Use a higher roundedness (12px or full pill) to make them feel touch-friendly and distinct from content cards.
- **Progress Bars:** Should have fully rounded end-caps.
- **Live Badges:** Use 4px radius for a sharper, more "urgent" appearance.

## Components

### Buttons
- **Primary:** Gradient background (Electric Blue to a slightly darker cyan), bold white text, subtle outer glow on hover.
- **Ghost:** Primary color border (1.5px) with transparent fill.

### Live Indicators
- **The "Pulse":** Use the Stadium Red color. For active games, the dot icon should have a 1.5s ease-in-out opacity animation (0.4 to 1.0).

### Cards
- **Score Cards:** Use a subtle horizontal gradient background. The "Winner" side of the card should have a 4px left-border highlight in Secondary Neon Green.
- **Stat Chips:** Small, dark grey fills with high-contrast JetBrains Mono text for player metrics (e.g., "AVG 24.5").

### Input Fields
- Dark backgrounds with a 1px border. On focus, the border transitions to the Primary Electric Blue with a faint blue outer glow.

### Scoreboard / Timers
- High-contrast display using monospaced fonts. Large font sizes (Display-LG) for primary score, with smaller labels for "1st Quarter" or "Final."

### Data Visualization
- Graphs should use the primary accent colors against the dark background. Use stroke weights of at least 2px for clarity on mobile screens.