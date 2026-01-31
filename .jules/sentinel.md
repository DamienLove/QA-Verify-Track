# Sentinel's Journal

## 2024-05-22 - Content Security Policy (CSP) Implementation
**Vulnerability:** The application lacked a Content Security Policy (CSP), which increases the risk of Cross-Site Scripting (XSS) and data exfiltration.
**Learning:** React applications using Vite and Firebase require specific CSP directives to function correctly. Specifically, `style-src 'unsafe-inline'` is often needed for dynamic styling, and `connect-src` must include all external API endpoints (GitHub, Firebase, Google Generative AI).
**Prevention:** Implemented a strict CSP in `firebase.json` that whitelists only necessary domains and blocks all others. This provides a robust defense-in-depth layer.

## 2024-05-24 - Input Length Limits (DoS Prevention)
**Vulnerability:** User inputs lacked `maxLength` attributes, allowing potentially unlimited character input. This poses a Denial of Service (DoS) risk (client-side freezing, large payload transmission) and API errors when exceeding backend limits.
**Learning:** React does not automatically enforce input limits. Explicit `maxLength` attributes are a simple, high-impact defense-in-depth measure.
**Prevention:** Added `maxLength` to all `input` and `textarea` elements in `App.tsx` matching downstream API constraints.

## 2024-05-25 - Prompt Injection & Input Sanitization
**Vulnerability:** User inputs (Issue Title, Description) were passed directly to the AI service for prompt construction, and to backend storage without sanitization. This created risks of Prompt Injection (manipulating LLM behavior) and potential control character injection.
**Learning:** LLM-based features require specific input hygiene. While React prevents XSS, it doesn't prevent "semantic" injection into prompts or data pollution with control characters.
**Prevention:** Implemented `sanitizeInput` in `services/security.ts` to strip control characters (0-31, 127) while preserving whitespace structure (newlines/tabs). Applied this sanitizer to all AI prompts and critical form inputs before storage/submission.
