# QA Verify & Track — Project Wiki

## Purpose
QA Verify & Track is a cross‑platform QA assistant that lets testers manage repos, issues, pull requests, and AI-assisted bug analysis from web and Android. It keeps verification tied to build numbers and filters out issues already tagged for a given build via status comments.

## Key Features
- **Auth & Sync**: Firebase Auth (email/password, Google) with per-user Firestore `user_settings/{uid}` storage.
- **Repo configuration**: Add/edit/delete GitHub repos, store PAT per repo, manage apps/build numbers.
- **Build-aware issue list**: Issues hidden when latest status comment matches or exceeds current build (e.g., “closed v126”).
- **PR workflow**: View PRs, approve/merge, close, resolve conflicts (update branch), undo close.
- **AI analysis**: Gemini summaries per issue (optional API key).
- **Quick Issue**: Fast overlay to file issues to the selected repo.
- **Mobile-friendly UI**: Dark-first, compact cards, bottom nav for Projects/Config.

## Running Locally
```bash
npm install
npm run dev -- --host --port 4173
# open http://localhost:4173
```

## Environment Variables (`.env.local`)
```
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_AUTH_DOMAIN=...
VITE_FIREBASE_PROJECT_ID=...
VITE_FIREBASE_STORAGE_BUCKET=...
VITE_FIREBASE_MESSAGING_SENDER_ID=...
VITE_FIREBASE_APP_ID=...
VITE_FIREBASE_MEASUREMENT_ID=...
# Optional for AI
VITE_GEMINI_API_KEY=your_gemini_key
```

## Firebase
- Project: `qa-verify-and-ttack`
- Hosting: https://qa-verify-and-ttack.web.app
- Firestore rules:
  - Allow read/write to `user_settings/{uid}` for authenticated `uid`.
  - Deny all else.

## GitHub Integration
- PAT stored in user settings per repo.
- Services:
  - `githubService.getIssues` / `getPullRequests`
  - `getIssueComments` (used for build-aware filtering)
  - `addComment`, `updateIssueStatus`, `mergePR`, `denyPR`, `updateBranch`, `approvePR`

## Build-Aware Issue Filtering
- Sync inspects latest status comment matching `open|closed|blocked v{num}`.
- If comment build ≥ target build, the issue is hidden for that run.
- Two sync buttons:
  - **Sync**: uses entered build number unchanged.
  - **Store**: (stub) auto-populates/bumps build number before sync.

## Deploy
- Web: `npm run build` then `firebase deploy --only hosting`
- Rules: `firebase deploy --only firestore:rules`

## Android
- Debug APK: `androidApp/app/build/outputs/apk/debug/app-debug.apk`
- Build: `cd androidApp && ./gradlew assembleDebug`

## UI Notes
- Dark palette with green primary (#12d622).
- Compact issue cards; high-contrast text for night use.

## Troubleshooting
- Blank page / AI crash: ensure `VITE_GEMINI_API_KEY` set or runs without AI (client lazy-inits).
- “Insufficient privileges” on Save: user must be signed in; Firestore rules scoped to `uid`.
- Sync shows old issues: confirm status comments format “open v123”, “closed v123”, “blocked v123”.
