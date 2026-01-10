<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

## QA Verify & Track — Web + Android

### Prereqs
- Node.js 18+ (for Vite web app)
- Java 17, Android SDK Platform 34 (for Android)
- Firebase project with Auth + Hosting enabled

### Web (Vite)
1. Install deps: `npm install`
2. Copy `.env.example` → `.env.local` and paste your Firebase web config keys (apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId, measurementId).
3. Dev server: `npm run dev` (default http://localhost:3000).
4. Build: `npm run build`
5. Deploy to Firebase Hosting: `npm run deploy:hosting` (after `npx firebase login`). Hosting is configured for site `qa-verify-and-ttack`.

#### Auth allowed domains
- `localhost`, `127.0.0.1`
- `qa-verify-and-ttack.firebaseapp.com`
- `qa-verify-and-ttack.web.app`
- Any custom domain you map

### Android (Jetpack Compose)
1. Open `androidApp/` in Android Studio, or run `cd androidApp && ./gradlew assembleDebug`.
2. Ensure `androidApp/app/google-services.json` matches your Firebase Android app and that its SHA-1/256 are registered in Firebase.
3. Debug APK: `androidApp/app/build/outputs/apk/debug/app-debug.apk`.
4. For release: create a release keystore, add its SHA-1/256 to Firebase, set signingConfig, then `./gradlew assembleRelease`.

### GitHub token
Users supply their own PAT with `repo` scope inside the app to access repos/issues/PRs.
