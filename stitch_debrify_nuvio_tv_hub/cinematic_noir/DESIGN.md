---
name: Cinematic Noir
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
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
  secondary: '#c8c6c6'
  on-secondary: '#303030'
  secondary-container: '#474747'
  on-secondary-container: '#b6b5b4'
  tertiary: '#ffffff'
  on-tertiary: '#303030'
  tertiary-container: '#e5e2e1'
  on-tertiary-container: '#656464'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e2e2e2'
  primary-fixed-dim: '#c6c6c7'
  on-primary-fixed: '#1a1c1c'
  on-primary-fixed-variant: '#454747'
  secondary-fixed: '#e4e2e1'
  secondary-fixed-dim: '#c8c6c6'
  on-secondary-fixed: '#1b1c1c'
  on-secondary-fixed-variant: '#474747'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1b1b1c'
  on-tertiary-fixed-variant: '#474746'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 72px
    fontWeight: '800'
    lineHeight: 80px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 8px
  gutter-desktop: 24px
  margin-desktop: 64px
  gutter-mobile: 16px
  margin-mobile: 20px
  container-max: 1440px
---

## Brand & Style
The design system is engineered for immersive, high-end media consumption. The brand personality is sophisticated, atmospheric, and invisible—stepping back to let content take center stage. 

The aesthetic blends **Minimalism** with **Glassmorphism**, utilizing deep obsidian surfaces and subtle translucency to create a sense of infinite depth. The target audience expects a premium, theater-like experience where the interface feels like an extension of the hardware. The emotional response is one of calm focus and cinematic prestige.

## Colors
The palette is rooted in absolute blacks and layered charcoals to maximize the "infinite" display effect on modern OLED screens. 

- **Background**: Pure #000000 for maximum contrast.
- **Surfaces**: #121212 (Base) and #1E1E1E (Elevated) for card structures and containers.
- **Accents**: #333333 for borders and secondary interactive elements.
- **Content**: High-contrast white for primary text and light grey for metadata to maintain clear hierarchy without eye strain in dark environments.

## Typography
The system utilizes **Inter** for its exceptional legibility and systematic feel. Headlines are intentionally bold and heavy to command attention on large TV displays and mobile screens alike. 

Large display sizes use tighter letter spacing to maintain a cohesive "poster" look. Labels and metadata use increased letter spacing and medium weights to ensure clarity against dark backgrounds.

## Layout & Spacing
This design system employs a **Fixed Grid** model for desktop and a **Fluid Grid** for mobile devices. 

- **Desktop**: A 12-column grid with 64px outer margins to provide "breathing room" reminiscent of a cinema screen.
- **TV/10ft UI**: Heavy reliance on horizontal "shelf" layouts with 24px gutters between cards.
- **Mobile**: A 4-column fluid layout that maximizes content density while maintaining 20px safe areas.

Spacing follows a strict 8px linear scale to ensure mathematical harmony across all components.

## Elevation & Depth
Hierarchy is established through **Tonal Layers** and **Subtle Outlines**. 

- **Level 0 (Background)**: #000000.
- **Level 1 (Cards/Navigation)**: #121212 with a 1px solid #1E1E1E border.
- **Level 2 (Modals/Popovers)**: #1E1E1E with a soft, diffused black shadow (40% opacity, 30px blur) to lift it from the background.
- **Interactions**: Focused elements gain a subtle 1px white border or a light-grey inner glow to indicate "active" state without using distracting colors.

## Shapes
The shape language is "Soft" (0.25rem - 0.75rem). This avoids the playfulness of fully rounded UI while softening the harshness of pure sharp corners. Card elements typically use the `rounded-lg` (0.5rem) token to feel modern and premium.

## Components
- **Buttons**: Primary buttons are high-contrast white with black text. Secondary buttons are ghost-style with #333333 borders.
- **Cards**: Media cards use a 16:9 or 2:3 aspect ratio. On hover, cards scale slightly (1.05x) and the border color shifts from #1E1E1E to #FFFFFF.
- **Input Fields**: Minimalist underlines or subtle #121212 fills. Focus state is indicated by a white bottom-border.
- **Progress Bars**: Used for watch-time; a thin 2px line using #333333 for the track and #FFFFFF for the progress fill.
- **Chips**: Small, pill-shaped #1E1E1E containers used for genre tags or "4K/HDR" badges.
- **Icons**: 24px thin-line (1.5px stroke) monochromatic icons. Use solid fills only for active/selected navigation states.