---
name: Obsidian Media Hub
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1b1b1b'
  surface-container: '#1f1f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353535'
  on-surface: '#e2e2e2'
  on-surface-variant: '#c4c7c8'
  inverse-surface: '#e2e2e2'
  inverse-on-surface: '#303030'
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
  on-tertiary: '#2f3131'
  tertiary-container: '#e3e2e2'
  on-tertiary-container: '#646465'
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
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474746'
  tertiary-fixed: '#e3e2e2'
  tertiary-fixed-dim: '#c7c6c6'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#464747'
  background: '#131313'
  on-background: '#e2e2e2'
  surface-variant: '#353535'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  meta-sm:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.02em
  meta-xs:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 16px
  margin-tablet: 32px
  margin-desktop: 48px
  gutter: 24px
  stack-xs: 4px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style

This design system is built for a high-end music streaming experience that prioritizes content immersion through a "lights-out" aesthetic. The brand personality is focused, sophisticated, and unapologetically technical, leaning into a blend of **Minimalism** and **Developer-centric utility**. 

By utilizing a true black foundation, the UI recedes into the hardware, allowing album art and artist photography to become the focal point. The emotional response is one of premium precision—evoking the feeling of a professional studio monitor or a high-fidelity playback engine. The aesthetic is defined by high-contrast monochrome values, sharp typography, and a rejection of unnecessary decoration.

## Colors

The palette is strictly monochromatic to ensure zero interference with media assets. 

- **Base:** The canvas is `true black (#000000)`, providing infinite depth and perfect contrast for OLED displays.
- **Surfaces:** Secondary surfaces and containers use `#1A1A1A` to create subtle separation without breaking the dark immersion.
- **Content:** Primary text and high-priority icons use pure `White (#FFFFFF)`. 
- **Hierarchy:** Supporting information scales through a range of greys: `#B0B0B0` for secondary labels, `#888888` for icons and borders, and `#666666` for disabled states or deep metadata.

## Typography

This design system employs a dual-font strategy to balance modern interface design with technical precision.

- **Geist:** Used for all primary UI elements, headings, and body copy. Its clean, geometric sans-serif terminals provide a contemporary and professional feel.
- **JetBrains Mono:** Reserved for metadata, timestamps, bitrates, and durations. The monospaced nature of the font emphasizes the "hub" and "utility" aspect of the platform, ensuring numerical data remains aligned and legible.

All headings should use tighter letter spacing for a punchy, editorial look, while monospaced metadata should use slightly increased tracking to enhance readability at small scales.

## Layout & Spacing

The layout is a responsive grid system that scales its complexity based on the available width.

- **Mobile:** A 2-column grid with 16px outer margins.
- **Tablet:** A 3-column grid with 32px outer margins.
- **Desktop:** A 4-column grid with 48px outer margins.

Spacing follows a strict vertical rhythm based on 8px increments. Content blocks should use a 24px gutter to provide breathing room for album art, while internal component spacing (like text within a list item) uses tighter 4px or 8px increments to maintain visual grouping.

## Elevation & Depth

In a true black environment, traditional drop shadows are ineffective. Instead, this design system uses **Tonal Layering** and **Stroke Definition** to create depth.

- **Level 0 (Base):** Pure `#000000` for the main background.
- **Level 1 (Platters):** `#1A1A1A` for cards, modals, and navigation bars.
- **Level 2 (Interactions):** Subtle `#888888` borders (1px) are used to define boundaries of interactive elements when they sit on the base layer.
- **Focus State:** Elements use a high-contrast 2px solid white ring to indicate focus, ensuring maximum accessibility in dark environments.

## Shapes

The shape language is a mix of geometric rigor and ergonomic softness. 

- **Cards & Media:** Album art and feature cards utilize a **12px** corner radius, creating a soft but structured look that contrasts with the sharp typography.
- **Interactive Controls:** All buttons, category chips, and mode toggles are **fully rounded (pill-shaped)** to distinguish them clearly from content containers. 
- **Indicators:** Progress bars and volume sliders use rounded caps to maintain consistency with the pill-shaped UI elements.

## Components

### Buttons & Chips
- **Primary Action:** Pill-shaped, white background with black text.
- **Secondary Action:** Pill-shaped, `#1A1A1A` background with white text and a 1px `#888888` border.
- **Category Chips:** Pill-shaped (height: 32px) with 16px horizontal and 6px vertical padding. Active state is white; inactive state is `#1A1A1A`.

### Media Cards
- **Album Art:** 1:1 Aspect ratio with 12px corner radius. No border in default state.
- **Focus/Hover:** 2px solid white focus ring with a 4px offset from the art.
- **Labels:** Title in Geist (Semi-bold), Artist in Geist (Regular, `#B0B0B0`).

### Mode Toggles
- **Structure:** Pill-shaped container (`#1A1A1A`) with a sliding pill-shaped indicator (`#888888` or white).
- **Labels:** Text inside toggles uses `meta-xs` (JetBrains Mono) for a technical appearance.

### Lists & Input
- **List Items:** 64px height for standard tracks. Metadata (duration) right-aligned in JetBrains Mono.
- **Input Fields:** Bottom-border only (2px white) or fully encapsulated in a `#1A1A1A` pill, depending on the context of the search.