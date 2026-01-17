# Sentinel's Journal

## 2024-05-22 - Content Security Policy (CSP) Implementation
**Vulnerability:** The application lacked a Content Security Policy (CSP), which increases the risk of Cross-Site Scripting (XSS) and data exfiltration.
**Learning:** React applications using Vite and Firebase require specific CSP directives to function correctly. Specifically, `style-src 'unsafe-inline'` is often needed for dynamic styling, and `connect-src` must include all external API endpoints (GitHub, Firebase, Google Generative AI).
**Prevention:** Implemented a strict CSP in `firebase.json` that whitelists only necessary domains and blocks all others. This provides a robust defense-in-depth layer.
