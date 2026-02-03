## 2024-05-23 - Accessibility Improvements
**Learning:** Found several interactive elements (switches, theme pickers) lacking `aria-label`, relying solely on visual context which fails for screen readers.
**Action:** Always check `role="switch"` and icon-only buttons for proper accessible labels.

## 2024-05-24 - Modal Interaction Consistency
**Learning:** Inconsistent modal behaviors (some close on backdrop click, others don't) confuse users. Found `QuickIssuePage` had it, but `Notes` and others didn't.
**Action:** Standardize all modals to support "click-outside-to-close" pattern unless explicit action is required.
