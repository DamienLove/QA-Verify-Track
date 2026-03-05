## 2024-05-23 - Accessibility Improvements
**Learning:** Found several interactive elements (switches, theme pickers) lacking `aria-label`, relying solely on visual context which fails for screen readers.
**Action:** Always check `role="switch"` and icon-only buttons for proper accessible labels.

## 2024-05-24 - Interactive Div Anti-Pattern
**Learning:** Found critical "clickable div" anti-pattern in repo lists and test toggles, blocking keyboard users.
**Action:** Replace interactive `div`s with `<button>`, ensuring `type="button"`, `w-full text-left` for layout preservation, and appropriate `aria-label`/`aria-pressed` attributes.
