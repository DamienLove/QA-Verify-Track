import { initializeApp } from "firebase/app";
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
import { Repository } from "../types";

// REPLACE WITH YOUR REAL FIREBASE CONFIGURATION
// You can get this from the Firebase Console -> Project Settings -> General -> Your Apps
const firebaseConfig = {
  apiKey: "REPLACE_WITH_YOUR_API_KEY",
  authDomain: "REPLACE_WITH_YOUR_PROJECT_ID.firebaseapp.com",
  projectId: "REPLACE_WITH_YOUR_PROJECT_ID",
  storageBucket: "REPLACE_WITH_YOUR_PROJECT_ID.appspot.com",
  messagingSenderId: "1234567890",
  appId: "1:1234567890:web:abcdef123456"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
const googleProvider = new GoogleAuthProvider();

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
