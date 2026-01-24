package com.qa.verifyandtrack.app.data.service

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.model.UserProfile
import com.qa.verifyandtrack.app.data.model.UserSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseService(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) {
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user ?: throw IllegalStateException("Unable to sign in.")
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user ?: throw IllegalStateException("Unable to create account.")
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        result.user ?: throw IllegalStateException("Unable to sign in with Google.")
    }

    fun buildGoogleSignInClient(context: Context, webClientId: String): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun subscribeToRepos(userId: String): Flow<List<Repository>> = callbackFlow {
        val docRef = firestore.collection("user_settings").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val repos = snapshot?.toObject(UserSettings::class.java)?.repos ?: emptyList()
            trySend(repos)
        }
        awaitClose { registration.remove() }
    }

    fun subscribeToGlobalSettings(userId: String): Flow<GlobalSettings?> = callbackFlow {
        val docRef = firestore.collection("user_settings").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val settings = snapshot?.toObject(UserSettings::class.java)?.globalSettings
            trySend(settings)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserRepos(userId: String, repos: List<Repository>) {
        val docRef = firestore.collection("user_settings").document(userId)
        docRef.set(mapOf("repos" to repos), SetOptions.merge()).await()
    }

    suspend fun saveGlobalSettings(userId: String, settings: GlobalSettings) {
        val docRef = firestore.collection("user_settings").document(userId)
        docRef.set(mapOf("globalSettings" to settings), SetOptions.merge()).await()
    }

    suspend fun getRepos(userId: String): List<Repository> {
        val docRef = firestore.collection("user_settings").document(userId)
        val snapshot = docRef.get().await()
        return snapshot.toObject(UserSettings::class.java)?.repos ?: emptyList()
    }

    suspend fun getGlobalSettings(userId: String): GlobalSettings? {
        val docRef = firestore.collection("user_settings").document(userId)
        val snapshot = docRef.get().await()
        return snapshot.toObject(UserSettings::class.java)?.globalSettings
    }

    // User Profile Methods

    suspend fun getUserProfile(userId: String): UserProfile? {
        val docRef = firestore.collection("user_profiles").document(userId)
        val snapshot = docRef.get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        val docRef = firestore.collection("user_profiles").document(profile.userId)
        docRef.set(profile, SetOptions.merge()).await()
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = firestore.collection("user_profiles").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            val profile = snapshot?.toObject(UserProfile::class.java)
            trySend(profile)
        }
        awaitClose { registration.remove() }
    }
}
