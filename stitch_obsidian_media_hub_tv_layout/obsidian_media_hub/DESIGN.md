---
name: Obsidian Media Hub
colors:
  surface: '#141313'
  surface-dim: '#141313'
  surface-bright: '#3a3939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353434'
  on-surface: '#e5e2e1'
  on-surface-variant: '#c4c7c8'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#8e9192'
  outline-variant: '#444748'
  surface-tint: '#c6c6c7'
  primary: '#ffffff'
  on-primary: '#2f3131'
  primary-container: '#e2e2e2'
  on-primary-container: '#636565'
  inverse-primary: '#5d5f5f'
  secondary: '#c8c6c5'
  on-secondary: '#313030'
  secondary-container: '#474746'
  on-secondary-container: '#b7b5b4'
  tertiary: '#ffffff'
  on-tertiary: '#2e3132'
  tertiary-container: '#e1e3e4'
  on-tertiary-container: '#626566'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e2e2e2'
  primary-fixed-dim: '#c6c6c7'
  on-primary-fixed: '#1a1c1c'
  on-primary-fixed-variant: '#454747'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1b1b1b'
  on-secondary-fixed-variant: '#474746'
  tertiary-fixed: '#e1e3e4'
  tertiary-fixed-dim: '#c5c7c8'
  on-tertiary-fixed: '#191c1d'
  on-tertiary-fixed-variant: '#444748'
  background: '#141313'
  on-background: '#e5e2e1'
  surface-variant: '#353434'
typography:
  headline-lg:
    fontFamily: Geist
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Geist
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  metadata-sm:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.02em
  metadata-xs:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 20px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style

The design system is engineered for high-performance media consumption, specifically catering to IPTV and sports enthusiast communities. The brand personality is "Technical Premium"—it combines the sleek, cinematic quality of high-end home theater interfaces with the precision of developer-centric tools.

The visual style is **Modern Minimalist with a Technical Edge**. It utilizes a "Void" aesthetic: a true black foundation that allows media content and high-contrast typography to emerge with maximum clarity. The atmosphere is immersive, focused, and authoritative, ensuring that the interface recedes to prioritize live streams, cover art, and real-time data.

## Colors

The palette is strictly monochrome to maintain a cinematic focus. 

- **The Void (#000000):** Used for the primary background to achieve infinite contrast on OLED displays and eliminate bezel distraction.
- **Surface Tiers:** Use `#1B1B1B` for large containers, `#1F1F1F` for secondary elements, and `#2A2A2A` for elevated items like active states or tooltips.
- **Accents:** White (#FFFFFF) is reserved exclusively for high-priority text, icons, and primary action buttons. 
- **Structural Lines:** Borders use `#444748` at low opacity to define boundaries without breaking the dark immersion.

## Typography

This design system uses a dual-font strategy to balance modern aesthetics with technical utility.

- **Geist:** Employed for all primary headings and body copy. Its geometric precision and wide apertures ensure legibility at a distance, making it ideal for 10-foot UI (TV) and mobile layouts.
- **JetBrains Mono:** Used for technical metadata, timestamps, channel numbers, and sports statistics. The monospaced nature ensures that changing numerical data (like scores or clocks) remains visually stable and easy to scan.
- **Contrast Hierarchy:** Use `Pure White` for headings and `70% Opacity White` for secondary body text to establish a clear information architecture.

## Layout & Spacing

The layout follows a **Fluid Grid** model based on a 4px base unit. 

- **Media-Centric Grid:** For content discovery, use a 12-column grid on desktop and a 4-column grid on mobile. 
- **Aspect Ratios:** All primary media cards (Channels, Movies, Event Highlights) must strictly adhere to a **16:9 aspect ratio**. Secondary thumbnails for sports personalities or channel icons may use a 1:1 ratio.
- **Responsive Behavior:** 
  - **Mobile:** Single column or side-scrolling horizontal lists.
  - **Desktop/TV:** Large-scale hero carousels with multi-column grids for categorization. 
- **Gutters:** Maintain a standard 20px (1.25rem) gap between cards to allow the dark borders to breathe.

## Elevation & Depth

This design system eschews traditional shadows in favor of **Tonal Layering** and **Structural Outlines**.

- **Tiers:** Depth is communicated by increasing the lightness of the background hex. The "deeper" an element is, the closer it is to #000000. Modal overlays and popovers use the highest tier (#2A2A2A).
- **Subtle Outlines:** Every container should feature a 1px solid border using `#444748`. For interactive elements, these borders should increase in brightness on hover.
- **Focus States:** Focused elements (essential for D-pad navigation) should utilize a 2px white outline with a 4px offset to ensure the active element is unmistakable against the black void.

## Shapes

The shape language is generous and modern, softening the intensity of the monochrome palette.

- **Media Cards:** Use `rounded-2xl` (16px) for all primary content containers to create a premium, "app-like" feel.
- **Inputs & Smaller Components:** Use `rounded-xl` (12px) for buttons, search fields, and chips.
- **Selection Rings:** Follow the radius of the parent element, maintaining a consistent concentric gap.

## Components

### Media Cards
The core of the experience. 16:9 ratio. Features a subtle `#444748` border. On hover, the card scales down to `0.97`—a "press-in" effect that suggests tactile depth rather than the standard expansion.

### Buttons
- **Primary:** Pure White background with True Black (#000000) text. Bold Geist.
- **Secondary:** Ghost style. Transparent background with a 1px `#444748` border and White text.
- **Technical:** Small buttons for "Stats" or "Source" use JetBrains Mono and a `#1F1F1F` background.

### Input Fields
Fields are `#1B1B1B` with a `#444748` border. When focused, the border turns Pure White. Text is Geist 16px.

### Chips & Tags
Used for "LIVE," "HD," or "4K" labels. These use JetBrains Mono, uppercase, with a `#2A2A2A` background. "LIVE" indicators should include a small solid red circle for urgency.

### Navigation Bars
Bottom-aligned for mobile, side-aligned (rail) for TV/Desktop. Icons are stroke-based (2px) and Pure White when active, 40% opacity when inactive.

### Lists
EPG (Electronic Program Guide) lists use horizontal rows with `#1B1B1B` separators. Time markers use JetBrains Mono for consistent character widths.