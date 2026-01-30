# Sentinel's Journal

## 2024-05-22 - Content Security Policy (CSP) Implementation
**Vulnerability:** The application lacked a Content Security Policy (CSP), which increases the risk of Cross-Site Scripting (XSS) and data exfiltration.
**Learning:** React applications using Vite and Firebase require specific CSP directives to function correctly. Specifically, `style-src 'unsafe-inline'` is often needed for dynamic styling, and `connect-src` must include all external API endpoints (GitHub, Firebase, Google Generative AI).
**Prevention:** Implemented a strict CSP in `firebase.json` that whitelists only necessary domains and blocks all others. This provides a robust defense-in-depth layer.

## 2024-05-24 - Input Length Limits (DoS Prevention)
**Vulnerability:** User inputs lacked `maxLength` attributes, allowing potentially unlimited character input. This poses a Denial of Service (DoS) risk (client-side freezing, large payload transmission) and API errors when exceeding backend limits.
**Learning:** React does not automatically enforce input limits. Explicit `maxLength` attributes are a simple, high-impact defense-in-depth measure.
**Prevention:** Added `maxLength` to all `input` and `textarea` elements in `App.tsx` matching downstream API constraints.

## 2024-05-24 - Permissions-Policy and Input Validation
**Vulnerability:** The application lacked a `Permissions-Policy` header, leaving potentially sensitive browser features (camera, mic) accessible if XSS occurred. Also, input validation for `displayName` and `githubToken` relied solely on client-side `maxLength` attributes, which can be bypassed.
**Learning:** React's controlled inputs respect `maxLength`, but logic-side validation is crucial for defense-in-depth, especially before persisting data to Firestore. `Permissions-Policy` provides a robust mechanism to disable unused features.
**Prevention:** Added `Permissions-Policy` header to `firebase.json` and explicit length checks in `App.tsx`'s `saveRepo` function.
