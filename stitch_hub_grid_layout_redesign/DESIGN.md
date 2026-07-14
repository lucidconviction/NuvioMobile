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
  tertiary-container: '#e2e2e2'
  on-tertiary-container: '#636565'
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
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c7'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#131313'
  on-background: '#e2e2e2'
  surface-variant: '#353535'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
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
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
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

The design system is built for a high-density, media-centric experience where content is the singular focus. It adopts a **Deep Minimalism** style, utilizing a true-black environment to eliminate interface distraction and maximize the perceived contrast of media assets. 

The aesthetic is characterized by sharp precision, technical clarity, and a premium "pro-tool" feel. It targets power users who value speed and efficiency, evoking a sense of sophisticated utility. By stripping away non-essential ornamentation, the UI feels invisible, allowing the user's media library to become the primary visual driver.

## Colors

The color palette is strictly monochromatic to ensure total focus on media content. 

- **Background (ObsidianBg):** Uses absolute black (#000000) to create infinite depth and power savings on OLED displays.
- **Surface (SurfaceCard):** A dark charcoal (#1A1A1A) used to define interactive areas and content containers without breaking the dark immersion.
- **Primary Text (OnSurface):** A soft white (#E0E0E0) to reduce eye strain while maintaining high legibility.
- **Secondary Text (OnSurfaceVariant):** A muted grey (#B0B0B0) for metadata and less critical information.
- **Accent/Focus:** Pure white (#FFFFFF) is reserved exclusively for active states, focus indicators, and high-priority calls to action.

## Typography

This design system utilizes **Inter** across all levels to maintain a systematic and utilitarian feel. 

- **Headlines:** Use Bold weights with slight negative letter-spacing to create a "locked-in" editorial look.
- **Body:** Standardized on Medium and Regular weights for high readability against dark backgrounds.
- **Labels:** Small caps or bold uppercase styles are used for category tags and metadata to create a distinct visual hierarchy between content titles and descriptions.

## Layout & Spacing

The system operates on a rigorous **4dp/8dp grid**. All dimensions, padding, and margins must be multiples of 4.

- **Grid System:** A 12-column fluid grid for desktop and a 4-column grid for mobile.
- **Card Grids:** Media is displayed in square grid formats. Large icon containers within cards should maintain a 1:1 aspect ratio.
- **Rhythm:** Use `16px` (md) for standard internal padding and `24px` (lg) for section vertical spacing. Desktop layouts should utilize generous `48px` outer margins to frame the content centrally.

## Elevation & Depth

Depth is conveyed through **Tonal Layering** rather than traditional shadows. In a true black environment, shadows are invisible, so hierarchy is built by "lifting" surfaces with lighter hex values.

- **Level 0 (Base):** #000000 (The canvas).
- **Level 1 (Cards/Inputs):** #1A1A1A (Subtle lift).
- **Level 2 (Hover/Active):** #262626 (Interactive feedback).
- **Outlines:** Use a `1px` solid border of #FFFFFF at 10% opacity for card definitions, and 100% opacity for focused states.

## Shapes

The design system uses a **Soft** shape language. While the grid is rigorous and geometric, subtle corner rounding prevents the UI from feeling aggressive or dated.

- **Components:** Standard buttons and cards use a `4px` (0.25rem) radius.
- **Large Containers:** Larger sections or featured media cards may scale up to `8px` (0.5rem) to maintain visual proportion.
- **Icons:** Icons should be enclosed in square containers with the same `4px` rounding to match the component language.

## Components

- **Square Grid Cards:** The core component. Features a large 1:1 ratio icon or thumbnail container at the top (SurfaceCard background), followed by a primary text title and secondary metadata below.
- **Buttons:**
    - *Primary:* Solid White (#FFFFFF) background with Black (#000000) text. 
    - *Secondary:* #1A1A1A background with White (#FFFFFF) text or border.
- **Input Fields:** Flat #1A1A1A background with no border in default state. On focus, a 1px solid White border is applied.
- **Chips:** Small, #1A1A1A background containers with `label-sm` typography, used for categories or tags.
- **Lists:** Clean rows separated by 1px dividers (#FFFFFF at 5% opacity). No icons on the far left unless they are essential for content identification.
- **Focus States:** High-visibility White (#FFFFFF) rings or solid borders to ensure accessibility within the dark UI.