## 2024-05-22 - Quick Issue Form Enhancement
**Learning:** Adding semantic form elements (`<form>`, `<label>`, `autoFocus`) significantly improves the usability of modal inputs. Users expect forms to submit on "Enter" and focus to land on the first input. Using `text-xs uppercase font-bold text-gray-500` for labels creates a consistent visual hierarchy with existing app forms.
**Action:** Always wrap input groups in a semantic `<form>` tag and ensure labels are explicitly linked via `htmlFor`/`id` even in "quick" or modal interfaces. Use `autoFocus` for the primary action.

## 2024-05-22 - Semantic Buttons for Interactions
**Learning:** Interactive elements like profile pictures acting as sign-out triggers must be implemented as `<button>` elements, not clickable `<div>`s. This ensures keyboard accessibility (Tab, Enter/Space) and proper ARIA role exposure.
**Action:** Always use `<button>` for clickable actions that are not links, ensuring they have `aria-label` if icon-only and visible focus indicators (`focus-visible:ring`).

## 2024-05-23 - Modal Accessibility Standards
**Learning:** Modals require explicit focus management (autoFocus on primary input), keyboard support (Escape to close), and proper ARIA roles (`role="dialog"`, `aria-modal="true"`) to be fully accessible. Character counters should be linked via `aria-describedby`.
**Action:** When implementing modals, always add `autoFocus`, `aria-labelledby`, `aria-describedby` (if applicable), and `useEffect` listener for Escape key.

## 2024-05-24 - Toggle Switch Accessibility
**Learning:** Visual toggle switches implemented as buttons need `role="switch"` and `aria-checked` to be correctly interpreted by screen readers. Without these attributes, they are announced merely as "buttons" without state information.
**Action:** For all toggle-style buttons, explicitly add `role="switch"`, `aria-checked={state}`, and ensure they are labelled via `aria-label` or `aria-labelledby` (or `label` with `htmlFor`).

## 2024-05-25 - Persistent Bottom Navigation State
**Learning:** For apps with a persistent bottom bar, relying on hardcoded active states confuses users when navigating to "secondary" top-level pages (like Config). Users expect the navigation bar to accurately reflect their location.
**Action:** Use `useLocation` to dynamically determine the active tab in global navigation components. Ensure persistent navigation elements remain visible on all top-level destinations to prevent "dead ends".
