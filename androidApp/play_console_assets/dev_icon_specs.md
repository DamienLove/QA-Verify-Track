# Dev-Style Game Asset Icon Specifications

## Overview
This specification defines a "Developer/Prototype" style icon asset intended for use during the game development phase. The aesthetic focuses on clarity, readability, and technical utility rather than artistic polish. It mimics "programmer art" or "debug textures" (e.g., Source Engine missing textures, Blueprint styles).

## Technical Requirements
*   **Dimensions:** 512x512 pixels (Standard High-Res Icon size).
*   **Format:** PNG (32-bit with Alpha).
*   **Color Space:** sRGB.
*   **File Size:** < 1 MB (Optimized).

## Visual Style Guide ("The Debug Aesthetic")
1.  **Background:**
    *   **Grid Pattern:** A mandatory 64x64 pixel grid overlay to assist with scale reference.
    *   **Color:** High-contrast dark gray (`#202020`) or "Blueprint Blue" (`#0044AA`).
2.  **Foreground/Symbol:**
    *   **Typography:** Monospace font (e.g., Consolas, Courier New, Roboto Mono). Large, centered text reading "DEV" or "WIP".
    *   **Iconography:** Simple geometric primitives (Cube, Sphere, Cog) in wireframe or flat shading.
    *   **Contrast:** White or Neon Green (`#00FF00`) for maximum legibility against the dark background.
3.  **Border/Frame:**
    *   **Debug Border:** A 16px diagonal hazard stripe pattern (Yellow/Black) or a solid bright Magenta (`#FF00FF`) line to indicate the asset's boundaries clearly.
4.  **Meta-Data (Optional but recommended for Dev assets):**
    *   Display the resolution text (e.g., "512px") in a corner.
    *   Crosshair at the exact center (256, 256).

## Usage
*   **Placeholder:** Use this icon for inventory items, skills, or achievements that strictly do not have final art yet.
*   **builds:** Distinct from "Release" icons to prevent accidental shipping of test builds.
