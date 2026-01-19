# QA Verify & Track

[![Android CI](https://github.com/DamienLove/QA-Verify-Track/actions/workflows/android_release.yml/badge.svg?branch=main)](https://github.com/DamienLove/QA-Verify-Track/actions/workflows/android_release.yml)

**QA Verify & Track** is a cross-platform QA assistant designed to streamline the testing workflow for developers and QA engineers. It bridges the gap between GitHub issues/PRs and specific application build numbers, ensuring you are verifying the right fixes on the right build.

## 🚀 Key Features

*   **Build-Aware Issue Tracking:** Automatically filters out issues that were fixed in previous builds. If an issue is marked `fixed v100` and you are testing `v101`, it won't clutter your view.
*   **GitHub Integration:**
    *   **Dashboard:** View Issues and Pull Requests for your configured repositories.
    *   **Actions:** Close, Reopen, and Block issues directly from the UI.
    *   **PR Management:** View PRs, Approve, Merge, and even **resolve file conflicts** directly in the web interface.
*   **AI-Powered Assistance (Gemini):**
    *   **Issue Analysis:** Get AI summaries of bugs, including potential root causes, verification steps, and severity assessments.
    *   **Test Generation:** Automatically generate checklist-style test cases for your app features.
*   **Cross-Platform:**
    *   **Web App:** Full-featured React dashboard.
    *   **Android App:** Manage QA from your device (Jetpack Compose).
    *   **Android Studio Plugin:** Integration for developers (Preview).
*   **Secure & Sync:** Uses Firebase Auth and Firestore to sync your settings and repository configurations across devices.

## 🛠️ Prerequisites

*   **Node.js 18+** (for the Web App)
*   **Java 17** & **Android SDK 34** (for Android App)
*   **Firebase Project:** You need a Firebase project with **Authentication** (Google & Email/Password) and **Firestore** enabled.
*   **GitHub Personal Access Token (PAT):** Required to interact with your repositories (needs `repo` scope).
*   **(Optional) Google Gemini API Key:** For AI features.

## 💻 Web App Setup (Vite + React)

1.  **Install Dependencies:**
    ```bash
    npm install
    ```

2.  **Environment Configuration:**
    Copy `.env.example` to `.env.local` and fill in your keys:
    ```ini
    # Firebase Web Configuration
    VITE_FIREBASE_API_KEY=...
    VITE_FIREBASE_AUTH_DOMAIN=...
    VITE_FIREBASE_PROJECT_ID=...
    VITE_FIREBASE_STORAGE_BUCKET=...
    VITE_FIREBASE_MESSAGING_SENDER_ID=...
    VITE_FIREBASE_APP_ID=...
    VITE_FIREBASE_MEASUREMENT_ID=...

    # AI Features (Optional)
    VITE_GEMINI_API_KEY=your_gemini_api_key
    ```

3.  **Run Locally:**
    ```bash
    npm run dev
    ```
    Access the app at `http://localhost:5173` (or the port shown in terminal).

4.  **Build & Deploy:**
    ```bash
    npm run build
    npm run deploy:hosting
    ```

## 📱 Android App Setup

1.  Open the `androidApp` directory in Android Studio.
2.  Place your `google-services.json` (from Firebase Console) into `androidApp/app/`.
3.  Build and Run:
    ```bash
    cd androidApp
    ./gradlew assembleDebug
    ```

## 🧠 AI Features

QA Verify & Track uses Google's Gemini models to enhance your workflow:
*   **Analyze Bug:** Click the "Analyze" chip on any issue card to get a smart summary.
*   **Generate Tests:** In the "Tests" tab, provide a description of your feature to generate a verification checklist.

## 🤝 Contribution

1.  Fork the repository.
2.  Create a feature branch.
3.  Submit a Pull Request.

## 📄 License

[MIT](LICENSE)