package com.qa.verifyandtrack.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val firebaseService: FirebaseService) {
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> =
        firebaseService.signInWithEmail(email, password)

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> =
        firebaseService.signUpWithEmail(email, password)

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> =
        firebaseService.signInWithGoogle(idToken)

    fun observeAuthState(): Flow<FirebaseUser?> = firebaseService.observeAuthState()

    fun signOut() = firebaseService.signOut()

    fun currentUser(): FirebaseUser? = firebaseService.getCurrentUser()
}
