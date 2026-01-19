# QA Verify & Track — Project Wiki

## Overview

**QA Verify & Track** is a specialized dashboard for managing Quality Assurance workflows, tightly integrated with GitHub and utilizing specific "Build Tags" to streamline verification. It solves the common problem: *"Is this bug fixed in the build I currently have installed?"*

## 🌟 Core Concepts

### 1. Build-Aware Filtering
The system parses comments on GitHub issues to track their status relative to a build number.
*   **Syntax:** Bots or humans comment `fixed v123`, `open v124`, or `blocked v125`.
*   **Logic:**
    *   If you select **Target Build: 130** in the dashboard:
    *   An issue marked `fixed v129` will be **hidden** (it's already verified/fixed).
    *   An issue marked `fixed v131` will be **visible** (fix is in a future build).
    *   Issues marked `open` or `blocked` are always visible.
    *   Issues marked `verify fix v130` are highlighted for verification.

### 2. Configuration
*   **Global vs. Repo Tokens:**
    *   You can set a **Global GitHub PAT** in the "Config" > "Global Settings" menu. This is used as a fallback.
    *   For specific repos, you can override this with a **Repo-Specific Token** in the repository settings.
*   **Sync:** All configurations (repos, tokens, app definitions) are stored in your private Firestore document (`user_settings/{uid}`), syncing across your Web and Android instances.

## 🤖 AI Capabilities (Gemini)

The app integrates with Google's Gemini API (`gemini-3-flash-preview` model) to provide intelligent assistance.

### Issue Analysis
*   **How to use:** Click the purple "Analyze" button on an Issue Card.
*   **Output:** The AI reads the issue title and body to provide:
    1.  **Root Cause:** A potential technical explanation.
    2.  **Verification:** Key steps to reproduce or verify the fix.
    3.  **Severity:** An assessment of the bug's impact.

### Test Case Generation
*   **How to use:** Go to the "Tests" tab -> "Generate Tests".
*   **Input:** Enter a feature description (e.g., "User Login Flow with 2FA").
*   **Output:** A checklist of 5-10 actionable test cases added to your manual test plan.

## 🔧 Troubleshooting

### "AI Analysis Disabled"
*   **Cause:** The `VITE_GEMINI_API_KEY` environment variable is missing.
*   **Fix:** Add your API key to `.env.local` and restart the development server.

### "Sync Failed" / "Insufficient Privileges"
*   **Cause:**
    *   Network issues.
    *   Firebase Security Rules denying access (ensure you are logged in).
    *   GitHub Token is expired or invalid.
*   **Fix:** Check the browser console. Re-login. Verify your GitHub PAT has `repo` scope.

### PR Conflict Resolution Fails
*   **Cause:** The conflict might be too complex for the web-based resolver (e.g., binary files, massive diffs).
*   **Fix:** Resolve the conflict locally via command line or GitHub's native interface.

## 📦 Deployment Guide

### Firebase Hosting (Web)
1.  Ensure you have the Firebase CLI: `npm install -g firebase-tools`
2.  Login: `firebase login`
3.  Initialize (if needed): `firebase init hosting`
4.  Build: `npm run build`
5.  Deploy: `npm run deploy:hosting`

### Android App
The Android app is a standard Jetpack Compose project.
*   **Debug:** `./gradlew assembleDebug`
*   **Release:** Configure signing in `build.gradle.kts` and run `./gradlew assembleRelease`.

## 🎨 Theme & UI
*   The app supports **Light** and **Dark** modes.
*   **Themes:** Users can select a primary accent color in the Config page.
*   **Mobile-First:** The UI is designed to work seamlessly on mobile browsers and as a native Android app.