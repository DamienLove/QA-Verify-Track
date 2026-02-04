## 2024-05-23 - Accessibility Improvements
**Learning:** Found several interactive elements (switches, theme pickers) lacking `aria-label`, relying solely on visual context which fails for screen readers.
**Action:** Always check `role="switch"` and icon-only buttons for proper accessible labels.

## 2026-02-04 - Modal Interaction Pattern
**Learning:** Standardized "click-outside-to-close" using backdrop onClick + content stopPropagation.
**Action:** Apply this pattern to all future modals (BlockIssue, ConflictPr) for consistency.
