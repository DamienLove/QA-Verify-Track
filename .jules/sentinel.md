# Sentinel's Journal

## 2024-05-22 - Content Security Policy (CSP) Implementation
**Vulnerability:** The application lacked a Content Security Policy (CSP), which increases the risk of Cross-Site Scripting (XSS) and data exfiltration.
**Learning:** React applications using Vite and Firebase require specific CSP directives to function correctly. Specifically, `style-src 'unsafe-inline'` is often needed for dynamic styling, and `connect-src` must include all external API endpoints (GitHub, Firebase, Google Generative AI).
**Prevention:** Implemented a strict CSP in `firebase.json` that whitelists only necessary domains and blocks all others. This provides a robust defense-in-depth layer.

## 2024-05-24 - Input Length Limits (DoS Prevention)
**Vulnerability:** User inputs lacked `maxLength` attributes, allowing potentially unlimited character input. This poses a Denial of Service (DoS) risk (client-side freezing, large payload transmission) and API errors when exceeding backend limits.
**Learning:** React does not automatically enforce input limits. Explicit `maxLength` attributes are a simple, high-impact defense-in-depth measure.
**Prevention:** Added `maxLength` to all `input` and `textarea` elements in `App.tsx` matching downstream API constraints.

## 2024-05-27 - Context-Unaware Caching
**Vulnerability:** API responses (Pull Requests) were cached using only the repository identifier as the key. This meant that if a privileged user fetched data, it could be served to a subsequent unprivileged user accessing the same repository, leading to information disclosure.
**Learning:** Caching strategies must include the security context (e.g., authentication token or user ID) in the cache key, or disable caching when the context varies.
**Prevention:** Modified `githubService` to bypass the cache whenever a custom token is provided, ensuring that cached data is only used for the default context.
