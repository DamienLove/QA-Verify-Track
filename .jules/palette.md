## 2024-05-22 - Quick Issue Form Enhancement
**Learning:** Adding semantic form elements (`<form>`, `<label>`, `autoFocus`) significantly improves the usability of modal inputs. Users expect forms to submit on "Enter" and focus to land on the first input. Using `text-xs uppercase font-bold text-gray-500` for labels creates a consistent visual hierarchy with existing app forms.
**Action:** Always wrap input groups in a semantic `<form>` tag and ensure labels are explicitly linked via `htmlFor`/`id` even in "quick" or modal interfaces. Use `autoFocus` for the primary action.

## 2024-05-22 - Semantic Buttons for Interactions
**Learning:** Interactive elements like profile pictures acting as sign-out triggers must be implemented as `<button>` elements, not clickable `<div>`s. This ensures keyboard accessibility (Tab, Enter/Space) and proper ARIA role exposure.
**Action:** Always use `<button>` for clickable actions that are not links, ensuring they have `aria-label` if icon-only and visible focus indicators (`focus-visible:ring`).
