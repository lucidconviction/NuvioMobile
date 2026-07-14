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
  surface-tint: '#c6c6c6'
  primary: '#fdfdfc'
  on-primary: '#2f3131'
  primary-container: '#e0e0e0'
  on-primary-container: '#626363'
  inverse-primary: '#5d5f5f'
  secondary: '#c6c6c6'
  on-secondary: '#2f3131'
  secondary-container: '#484949'
  on-secondary-container: '#b8b8b8'
  tertiary: '#fffcfb'
  on-tertiary: '#313030'
  tertiary-container: '#e2dfdf'
  on-tertiary-container: '#636262'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e2e2e2'
  primary-fixed-dim: '#c6c6c6'
  on-primary-fixed: '#1a1c1c'
  on-primary-fixed-variant: '#454747'
  secondary-fixed: '#e3e2e2'
  secondary-fixed-dim: '#c6c6c6'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#464747'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1c1b1b'
  on-tertiary-fixed-variant: '#474746'
  background: '#131313'
  on-background: '#e2e2e2'
  surface-variant: '#353535'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-md:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  sidebar_width: 280px
  bottom_nav_height: 64px
  gutter: 16px
  margin_desktop: 32px
  margin_mobile: 16px
  stack_gap: 24px
---

## Brand & Style
The design system is engineered for a high-end, immersive media experience. It adopts a **Technological Minimalism** aesthetic, prioritizing content through an "Obsidian" framework. The personality is disciplined, professional, and elite, catering to users who value a cinematic environment without visual noise. 

The style utilizes deep layering and subtle textural shifts rather than vibrant colors to communicate hierarchy. It draws inspiration from premium hardware interfaces, emphasizing precision, high-quality finishes, and technical sophistication.

## Colors
The palette is monochromatic and high-contrast, designed to make media artwork pop.
- **Base:** The absolute black (#000000) provides infinite depth and preserves battery life on OLED displays.
- **Surface:** Deep gray (#1A1A1A) defines cards and containers, creating a clear distinction from the background without breaking the dark immersion.
- **Content:** Light gray (#E0E0E0) is the standard for high-legibility text, while muted gray (#CCCCCC) handles secondary information.
- **Interaction:** Pure white (#FFFFFF) is reserved exclusively for active states and critical calls to action to ensure maximum focus.

## Typography
The typography system uses **Hanken Grotesk** for its sharp, contemporary geometry and exceptional legibility in dark environments. For technical labels and UI metadata, **Geist** provides a monospaced-adjacent precision that reinforces the "technical hub" feel.

Type is treated with a strict hierarchy: Titles use tighter tracking and heavier weights, while body text is given generous line height to prevent "bleeding" on high-brightness screens.

## Layout & Spacing
The system employs a **Hybrid Responsive** model:
- **Desktop/Tablet Landscape:** A persistent 280px left side-panel anchors the global navigation (Hub categories). The main content area uses a fluid grid with a 32px outer margin.
- **Mobile/Handheld:** Navigation shifts to a high-density bottom bar. Sub-navigation and category filters utilize full-screen overlays to maintain focus.

The spacing rhythm is based on an 8px scale. Media grids should use a 16px gutter to allow content to breathe while maintaining a tight, professional density.

## Elevation & Depth
Depth is achieved through **Tonal Layering** and **Subtle Outlines** rather than traditional shadows.
- **Level 0 (Base):** #000000.
- **Level 1 (Cards/Sidebar):** #1A1A1A with a 1px solid border of #262626.
- **Level 2 (Hover/Active):** #262626 with a #CCCCCC border.

This "ghost border" technique ensures that even in pitch-black environments, the structure of the UI remains visible and tactile.

## Shapes
A consistent **12px (0.75rem)** corner radius is applied to all primary containers, including media thumbnails, cards, and input fields. This radius strikes a balance between the precision of a sharp-edged professional tool and the approachability of a consumer media app. Smaller components like chips and tags use a 4px radius for a more technical, "buttoned-up" appearance.

## Components
- **Media Cards:** Feature a 12px corner radius, no visible shadow, and a 1px #262626 border. On hover, the border brightens to #CCCCCC.
- **Buttons:** Primary buttons are #E0E0E0 with black text. Ghost buttons use a white outline and no fill.
- **Navigation (Sidebar):** Icons are outlined (2px stroke). Active states use a solid white icon and a subtle 2px vertical indicator on the left edge.
- **Bottom Navigation (Mobile):** High-contrast active states using white text/icons against a slightly translucent #1A1A1A background (backdrop-filter: blur(10px)).
- **Multi-Window Toggle:** A specialized component using a grid-based icon; active windows are highlighted with a 2px #FFFFFF stroke.
- **Input Fields:** Dark gray backgrounds (#1A1A1A) with light gray placeholder text. Focus states use a 1px white border.