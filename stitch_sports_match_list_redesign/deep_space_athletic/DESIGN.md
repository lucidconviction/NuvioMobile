---
name: Deep Space Athletic
colors:
  surface: '#101419'
  surface-dim: '#101419'
  surface-bright: '#36393f'
  surface-container-lowest: '#0b0e14'
  surface-container-low: '#181c21'
  surface-container: '#1c2025'
  surface-container-high: '#262a30'
  surface-container-highest: '#31353b'
  on-surface: '#e0e2ea'
  on-surface-variant: '#c0c7d4'
  inverse-surface: '#e0e2ea'
  inverse-on-surface: '#2d3136'
  outline: '#8a919d'
  outline-variant: '#404752'
  surface-tint: '#a0caff'
  primary: '#a0caff'
  on-primary: '#003259'
  primary-container: '#4da6ff'
  on-primary-container: '#003a67'
  inverse-primary: '#0061a5'
  secondary: '#c0c6dc'
  on-secondary: '#2a3041'
  secondary-container: '#404658'
  on-secondary-container: '#afb4ca'
  tertiary: '#4ae176'
  on-tertiary: '#003915'
  tertiary-container: '#06bb55'
  on-tertiary-container: '#00431a'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d2e4ff'
  primary-fixed-dim: '#a0caff'
  on-primary-fixed: '#001c37'
  on-primary-fixed-variant: '#00497e'
  secondary-fixed: '#dce2f8'
  secondary-fixed-dim: '#c0c6dc'
  on-secondary-fixed: '#151b2b'
  on-secondary-fixed-variant: '#404658'
  tertiary-fixed: '#6bff8f'
  tertiary-fixed-dim: '#4ae176'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005321'
  background: '#101419'
  on-background: '#e0e2ea'
  surface-variant: '#31353b'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  score-display:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 32px
    letterSpacing: -0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style
The design system is engineered for the high-stakes, fast-paced world of live sports. It utilizes a **Modern Dark** aesthetic with a "Deep-space" influence, prioritizing data density and immediate scannability. The atmosphere is professional, energetic, and high-performance.

The style leverages **Minimalism** to ensure team crests and scores remain the focal point, while incorporating subtle **Glassmorphism** for overlays to maintain depth without distracting from the content. The emotional response should be one of precision and urgency, suitable for fans tracking multiple live events simultaneously.

## Colors
The palette is built on a foundation of deep obsidians and cool grays to provide maximum contrast for live data.

- **Primary (#4DA6FF):** A high-visibility "Action Blue" used for interactive elements, icons, and primary CTAs.
- **Surface (#181E2E):** The secondary color, used for cards and containers to create a subtle lift from the background.
- **Highlight (#22C55E):** A vibrant green dedicated exclusively to active scores, game clocks, and positive "final" results.
- **Status Live (#FF4D4D):** A critical alert red reserved for "LIVE" indicators and pulsing animations.

## Typography
The system uses **Plus Jakarta Sans** for headlines and scores to inject a modern, slightly rounded character that feels approachable yet high-tech. **Inter** is used for all functional metadata and body text due to its exceptional legibility at small sizes and neutral, systematic tone.

- Team names use `headline-md` or `headline-sm` depending on layout density.
- Match scores use the specialized `score-display` role for maximum impact.
- Metadata (dates, leagues, venues) should always use `label-md` or `body-md` in the secondary text color.

## Layout & Spacing
This design system utilizes a **Fluid Grid** with a 4px baseline rhythm. 

- **Desktop:** 12-column grid with 24px gutters. Cards typically span 4 columns (3-up) or 6 columns (2-up).
- **Mobile:** Single column layout with 16px side margins. 
- **Vertical Rhythm:** Match cards use 16px internal padding (`md`) to allow team crests and scores enough breathing room. Groups of matches (e.g., by league) are separated by 32px (`xl`) of vertical space.

## Elevation & Depth
Depth is achieved through **Tonal Layers** rather than heavy shadows to maintain a clean, modern look.

- **Level 0 (Background):** #10141D.
- **Level 1 (Cards/Surface):** #181E2E. These elements feature a 1px inner border (stroke) of #FFFFFF at 5% opacity to define edges against the dark background.
- **Level 2 (Dropdowns/Modals):** #252D3F with a soft, 20px blur shadow at 30% opacity.
- **Interaction:** Hovering over a match card should increase the surface brightness slightly or apply a subtle 1px stroke of the Primary Accent color.

## Shapes
The design system follows a consistent **Rounded** language. 

- **Cards:** 12px (`rounded-lg`) corner radius for all match and league containers.
- **Buttons/Inputs:** 8px corner radius for standard interactive elements.
- **Badges/Live Indicators:** Fully pill-shaped (999px) for distinct visual categorization.
- **Crests:** While club crests vary, they should be housed in 48x48px or 64x64px containers with a subtle 4px radius to soften harsh edges.

## Components
- **Match Cards:** The core component. Team names in Primary Text, metadata in Secondary Text. High-resolution crests (min 40px) positioned horizontally for lists or vertically for featured matches.
- **LIVE Indicator:** A pill-shaped badge with a background of #FF4D4D at 10% opacity. It contains the text "LIVE" in #FF4D4D and a 6px circular dot that pulses (scale 1.0 to 1.5) every 1.5 seconds.
- **Action Buttons:** Solid #4DA6FF background with white text for primary actions (e.g., "Tickets"). Outlined style with 1px #4DA6FF border for secondary actions (e.g., "Stats").
- **Scoreboard:** Uses `score-display` typography. In-progress scores use the Highlight Green; final scores use Primary Text.
- **Chips:** Small, #181E2E background pills used for league names or filter tags, featuring 8px horizontal padding and `label-sm` text.
- **Lists:** Match lists should have no visible dividers; the 8px gap between cards and the tonal difference between card and background provide the necessary separation.