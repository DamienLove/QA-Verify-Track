import { initializeApp, type FirebaseOptions } from "firebase/app";
import {
    getAuth,
    GoogleAuthProvider,
    signInWithPopup,
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signOut,
    onAuthStateChanged,
    User
} from "firebase/auth";
import { 
    getFirestore,
    doc,
    setDoc,
    getDoc,
    collection,
    query,
    where,
    onSnapshot
} from "firebase/firestore";
import { getAnalytics, isSupported as isAnalyticsSupported } from "firebase/analytics";
import { Repository } from "../types";

/**
 * Default Firebase configuration (web app) provided by user.
 * Override via Vite env vars (VITE_FIREBASE_*) to swap environments.
 */
const defaultWebConfig = {
  apiKey: "AIzaSyAgKnkhu_dcjcbXsl1GVWOXYoY-CmhMwZ4",
  authDomain: "qa-verify-and-ttack.firebaseapp.com",
  projectId: "qa-verify-and-ttack",
  storageBucket: "qa-verify-and-ttack.firebasestorage.app",
  messagingSenderId: "656737115796",
  appId: "1:656737115796:web:779252a25a44df70001e81",
  measurementId: "G-RBKTZGBYV9"
} as const;

const firebaseConfig: FirebaseOptions = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY ?? defaultWebConfig.apiKey,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ?? defaultWebConfig.authDomain,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID ?? defaultWebConfig.projectId,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? defaultWebConfig.storageBucket,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? defaultWebConfig.messagingSenderId,
  appId: import.meta.env.VITE_FIREBASE_APP_ID ?? defaultWebConfig.appId,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID ?? defaultWebConfig.measurementId,
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
const googleProvider = new GoogleAuthProvider();

// Analytics is optional; only initialize when supported (browser, not SSR)
isAnalyticsSupported().then((supported) => {
  if (supported) {
    getAnalytics(app);
  }
});

export const firebaseService = {
    // Auth
    signInWithGoogle: async () => {
        try {
            const result = await signInWithPopup(auth, googleProvider);
            return result.user;
        } catch (error) {
            console.error("Error signing in with Google", error);
            throw error;
        }
    },
    
    // Database Sync
    // Subscribe to the user's repository configuration
    subscribeToRepos: (userId: string, callback: (repos: Repository[]) => void) => {
        const docRef = doc(db, "user_settings", userId);
        return onSnapshot(docRef, (docSnap) => {
            if (docSnap.exists()) {
                const data = docSnap.data();
                callback(data.repos || []);
            } else {
                callback([]);
            }
        });
    },

    // Save repository configuration
    saveUserRepos: async (userId: string, repos: Repository[]) => {
        const docRef = doc(db, "user_settings", userId);
        await setDoc(docRef, { repos }, { merge: true });
    }
};
