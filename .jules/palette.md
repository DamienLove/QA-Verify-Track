## 2024-05-22 - Quick Issue Form Enhancement
**Learning:** Adding semantic form elements (`<form>`, `<label>`, `autoFocus`) significantly improves the usability of modal inputs. Users expect forms to submit on "Enter" and focus to land on the first input. Using `text-xs uppercase font-bold text-gray-500` for labels creates a consistent visual hierarchy with existing app forms.
**Action:** Always wrap input groups in a semantic `<form>` tag and ensure labels are explicitly linked via `htmlFor`/`id` even in "quick" or modal interfaces. Use `autoFocus` for the primary action.

## 2024-05-22 - Semantic Buttons for Interactions
**Learning:** Interactive elements like profile pictures acting as sign-out triggers must be implemented as `<button>` elements, not clickable `<div>`s. This ensures keyboard accessibility (Tab, Enter/Space) and proper ARIA role exposure.
**Action:** Always use `<button>` for clickable actions that are not links, ensuring they have `aria-label` if icon-only and visible focus indicators (`focus-visible:ring`).

## 2024-05-23 - Modal Accessibility Standards
**Learning:** Modals require explicit focus management (autoFocus on primary input), keyboard support (Escape to close), and proper ARIA roles (`role="dialog"`, `aria-modal="true"`) to be fully accessible. Character counters should be linked via `aria-describedby`.
**Action:** When implementing modals, always add `autoFocus`, `aria-labelledby`, `aria-describedby` (if applicable), and `useEffect` listener for Escape key.
