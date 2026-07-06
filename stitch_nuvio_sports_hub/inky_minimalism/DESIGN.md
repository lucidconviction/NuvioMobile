---
name: Inky Minimalism
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
  secondary: '#c6c6cf'
  on-secondary: '#2f3037'
  secondary-container: '#45464e'
  on-secondary-container: '#b4b4bd'
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
  secondary-fixed: '#e2e1eb'
  secondary-fixed-dim: '#c6c6cf'
  on-secondary-fixed: '#1a1b22'
  on-secondary-fixed-variant: '#45464e'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c7'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#141313'
  on-background: '#e5e2e1'
  surface-variant: '#353434'
typography:
  headline-lg:
    fontFamily: Geist
    fontSize: 40px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: '1.4'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: '1.4'
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
  md: 24px
  lg: 48px
  xl: 80px
  gutter: 24px
  margin: 32px
---

## Brand & Style
The design system is built on a foundation of "Inky Minimalism"—a high-contrast, sophisticated aesthetic that prioritizes focus and clarity. The brand personality is precise, technical, and premium, targeting power users and professionals who value an unobtrusive but powerful interface. 

The visual style leans heavily into **Minimalism** with a touch of **Modern Tech**. By utilizing a monochromatic palette, we remove cognitive load and allow content to become the primary focal point. The interface should feel like a high-end piece of hardware: solid, intentional, and meticulously engineered.

## Colors
The color strategy is strictly monochromatic to emphasize form and hierarchy through value rather than hue. 

- **Surface:** A deep, absolute black (#000000) serves as the primary canvas, providing infinite depth and perfect contrast for OLED displays.
- **Containers:** We use charcoal shades to differentiate layers. Secondary containers use a slightly lighter grey to denote interactivity or nested content.
- **Text:** High-contrast white is reserved for primary headings and critical actions. Secondary information uses muted greys to establish a clear information hierarchy.
- **Accents:** Borders use a subtle light grey to define boundaries without breaking the minimalist flow.

## Typography
Typography in this design system is driven by **Geist**, providing a technical, precise, and highly legible experience. For metadata and small utilitarian labels, **JetBrains Mono** is employed to reinforce the "developer-centric" and engineered feel.

Headlines should use tight letter spacing and heavier weights to command attention against the black background. Body text maintains a generous line height (1.6) to ensure long-form readability. All labels are set in monospaced type to assist in data scanning and alignment.

## Layout & Spacing
The layout follows a **Fixed Grid** philosophy on desktop to maintain a controlled, architectural composition. We utilize a 12-column grid with a maximum content width of 1280px.

- **Desktop:** 12 columns, 24px gutters, and 32px side margins.
- **Tablet:** 8 columns, 16px gutters, and 24px side margins.
- **Mobile:** 4 columns, 16px gutters, and 16px side margins.

Spacing is strictly based on an 8px modular scale. Internal component padding should favor generous white space (or "black space") to prevent the UI from feeling cramped in a dark environment.

## Elevation & Depth
Depth is conveyed through **Tonal Layers** and **Low-Contrast Outlines** rather than traditional shadows. Because the background is pure black, we create elevation by stepping up the brightness of the container background.

1.  **Level 0 (Floor):** #000000 (Absolute Black)
2.  **Level 1 (Card/Container):** #121212 (Deep Charcoal)
3.  **Level 2 (Popovers/Modals):** #1C1C1C (Medium Charcoal)

Each elevated element is wrapped in a subtle 1px border (#27272A) to define its edges against the dark background. Physical shadows are not used, as the color steps provide sufficient separation.

## Shapes
This design system utilizes a **Rounded** shape language to soften the aggressive nature of the high-contrast monochromatic palette. This creates a "precision-milled" feel, reminiscent of modern hardware.

- **Default (Standard UI):** 0.5rem (8px) for buttons, inputs, and small cards.
- **Large (Containers):** 1rem (16px) for main content sections and modals.
- **Extra Large (Hero):** 1.5rem (24px) for prominent featured elements.

## Components

- **Buttons:** Primary buttons are solid white with black text for maximum visibility. Secondary buttons are outlined with a light grey border and white text.
- **Input Fields:** Surfaces should be slightly lighter than the background (#121212) with a 1px border. Focus states are indicated by a 1px solid white border.
- **Chips:** Small, pill-shaped containers with a #27272A background and white label text.
- **Cards:** Use Level 1 container color (#121212) with 0.5rem roundedness and a subtle grey border.
- **Checkboxes/Radios:** These should be stark white when active to ensure they "pop" against the dark interface.
- **Lists:** Items are separated by thin #1A1A1A horizontal dividers, utilizing the monospaced label font for secondary metadata.