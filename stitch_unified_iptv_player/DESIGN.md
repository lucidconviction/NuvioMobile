---
name: Obsidian Stream
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
  on-surface-variant: '#ccc3d8'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#958da1'
  outline-variant: '#4a4455'
  surface-tint: '#d2bbff'
  primary: '#d2bbff'
  on-primary: '#3f008e'
  primary-container: '#7c3aed'
  on-primary-container: '#ede0ff'
  inverse-primary: '#732ee4'
  secondary: '#89ceff'
  on-secondary: '#00344d'
  secondary-container: '#00a2e6'
  on-secondary-container: '#00344e'
  tertiary: '#ffb784'
  on-tertiary: '#4f2500'
  tertiary-container: '#a15100'
  on-tertiary-container: '#ffe0cd'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#eaddff'
  primary-fixed-dim: '#d2bbff'
  on-primary-fixed: '#25005a'
  on-primary-fixed-variant: '#5a00c6'
  secondary-fixed: '#c9e6ff'
  secondary-fixed-dim: '#89ceff'
  on-secondary-fixed: '#001e2f'
  on-secondary-fixed-variant: '#004c6e'
  tertiary-fixed: '#ffdcc6'
  tertiary-fixed-dim: '#ffb784'
  on-tertiary-fixed: '#301400'
  on-tertiary-fixed-variant: '#713700'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
  epg-time:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
  epg-slot-width: 180px
---

## Brand & Style

The design system is engineered for a premium, high-end entertainment experience. It targets a tech-savvy audience that values cinematic immersion and seamless navigation. The personality is sleek, sophisticated, and high-performance.

The aesthetic follows a **Modern Dark** direction with **Glassmorphism** accents. It utilizes deep, layered obsidian surfaces to provide a sense of infinite depth, allowing content posters and vibrant accent colors to pop. The UI should evoke a "home cinema" emotional response—quiet, unobtrusive, and powerful. Interaction states use subtle translucent blurs and glowing borders to guide the user without breaking the dark-room immersion.

## Colors

The palette is optimized for OLED displays and low-light environments. 

- **Primary (Neon Purple):** Used for active focus states, primary actions, and progress indicators.
- **Secondary (Electric Blue):** Used for category tags, live indicators, and data visualizations within the EPG.
- **Backgrounds:** A strict hierarchy of "Obsidian" (#020617) for the main canvas and "Charcoal" (#1E293B) for nested containers.
- **Glassmorphism:** Overlays use a 60% opacity of the surface color combined with a 20px backdrop blur and a thin, 1px white stroke at 12% opacity to define edges.

## Typography

This design system utilizes **Inter** for its clinical legibility and modern, systematic feel. 

- **Hierarchy:** Strong contrast between bold headlines and utilitarian body text ensures readability at a distance (TV/Lean-back mode).
- **EPG Data:** Time slots and program titles use a medium weight (`500`) to maintain clarity against dark backgrounds without the "blooming" effect of heavy weights.
- **Labels:** Meta-information (Resolution, HDR, Audio) uses `label-caps` for a technical, data-driven appearance.

## Layout & Spacing

The layout follows a **Fluid Grid** model with high-density spacing for EPG data and generous breathing room for media discovery.

- **Grid:** 12-column system for desktop, 4-column for mobile.
- **EPG Timeline:** A horizontal scrolling axis where each 30-minute block defaults to 180px width.
- **Margins:** Large 48px margins on desktop to create a cinematic "frame" around the content. On mobile, this reduces to 16px to maximize screen real estate for list views.
- **Rhythm:** All spacing (padding, gaps) is derived from a 4px base unit to ensure mathematical alignment of grid-heavy elements like the channel guide.

## Elevation & Depth

Depth is communicated through **Tonal Layering** and **Glassmorphism** rather than traditional drop shadows.

1.  **Level 0 (Base):** Deep Obsidian background.
2.  **Level 1 (Cards/Sidebar):** Charcoal surface with a subtle 1px border.
3.  **Level 2 (Overlays/Modals):** Translucent glass layer with `backdrop-filter: blur(20px)`.
4.  **Focus State:** Elements do not "lift" via shadows; instead, they receive a vibrant Neon Purple outer glow (4px spread, 0.4 opacity) and a slight scale increase (1.02x).

## Shapes

The shape language is refined and geometric. 

- **Base Radius:** 0.5rem (8px) for standard buttons and input fields.
- **Large Radius:** 1rem (16px) for channel preview cards and modal sheets.
- **Interactive Elements:** Use the `rounded-lg` scale to feel approachable yet structured.
- **EPG Blocks:** Maintain a slightly tighter 4px radius to ensure a dense "mosaic" look when tiled together in the timeline.

## Components

- **Channel Cards:** 16:9 aspect ratio. Featuring a bottom-aligned glass gradient overlay for the channel name and current program.
- **Buttons:** 
  - *Primary:* Solid Neon Purple with white text.
  - *Secondary:* Glass background with a 1px Electric Blue border.
- **EPG Timeline:** Current time indicator is a vertical Electric Blue line with a subtle glow that spans the height of the grid.
- **Bottom Sheets:** Use for playlist selection; must feature a top "grabber" and a high backdrop blur to maintain context of the background video.
- **Inputs:** Darker than the surface color (#0F172A), using a 1px border that turns Neon Purple on focus. Labels sit just above the field in `label-caps`.
- **Category Tags:** Pill-shaped, using a low-opacity Electric Blue background with high-contrast text for active filtering.