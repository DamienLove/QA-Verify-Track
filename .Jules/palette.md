## 2026-01-29 - Custom Radio Group Accessibility
**Learning:** Custom interactive controls (like color pickers) must explicitly implement the Radio Group pattern (role='radiogroup' + role='radio') to be accessible to screen readers, as they don't use native `<input type='radio'>` elements.
**Action:** Always verify `role="radio"` and `aria-checked` are present on custom selection components.
