## 2024-05-22 - Hardcoded Firebase Config
**Vulnerability:** Hardcoded Firebase API keys and configuration in source code.
**Learning:** While Firebase keys are not secret, hardcoding them makes it difficult to manage different environments (dev/prod) and can lead to accidental usage of production resources in development.
**Prevention:** Always use environment variables for configuration, even for non-secret values.
