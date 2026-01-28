# Sentinel's Journal

## 2024-05-22 - Content Security Policy (CSP) Implementation
**Vulnerability:** The application lacked a Content Security Policy (CSP), which increases the risk of Cross-Site Scripting (XSS) and data exfiltration.
**Learning:** React applications using Vite and Firebase require specific CSP directives to function correctly. Specifically, `style-src 'unsafe-inline'` is often needed for dynamic styling, and `connect-src` must include all external API endpoints (GitHub, Firebase, Google Generative AI).
**Prevention:** Implemented a strict CSP in `firebase.json` that whitelists only necessary domains and blocks all others. This provides a robust defense-in-depth layer.

## 2024-05-24 - Input Length Limits (DoS Prevention)
**Vulnerability:** User inputs lacked `maxLength` attributes, allowing potentially unlimited character input. This poses a Denial of Service (DoS) risk (client-side freezing, large payload transmission) and API errors when exceeding backend limits.
**Learning:** React does not automatically enforce input limits. Explicit `maxLength` attributes are a simple, high-impact defense-in-depth measure.
**Prevention:** Added `maxLength` to all `input` and `textarea` elements in `App.tsx` matching downstream API constraints.

## 2025-02-13 - Credential Exposure Prevention (Git)
**Vulnerability:** A local utility script (`create-user-profile.js`) required a `serviceAccountKey.json` which was not excluded from git.
**Learning:** Utility scripts often require high-privilege credentials that are not part of the standard build process. These can easily be committed if not explicitly ignored, even if they aren't in the standard `.env` path.
**Prevention:** Added `serviceAccountKey.json` and patterns for cryptographic keys (`*.jks`, `*.keystore`, `*.p12`, `*.key`, `*.pem`) to `.gitignore` to prevent accidental exposure of administrative credentials and signing keys.
