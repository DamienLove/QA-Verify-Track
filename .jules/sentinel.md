## 2024-05-22 - Hardcoded Firebase Config
**Vulnerability:** Hardcoded Firebase API keys and configuration in source code.
**Learning:** While Firebase keys are not secret, hardcoding them makes it difficult to manage different environments (dev/prod) and can lead to accidental usage of production resources in development.
**Prevention:** Always use environment variables for configuration, even for non-secret values.

## 2024-05-22 - Insecure Local Storage of User Notes
**Vulnerability:** User notes were stored in `localStorage` using a global key (`global-notes`), allowing any user on the same device to access another user's notes.
**Learning:** Client-side storage is shared across all users of the browser/device unless explicitly partitioned or cleared.
**Prevention:** Scope user-specific data in client-side storage with the user's unique identifier (e.g., `notes_${userId}`).
