package com.qa.verifyandtrack.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val firebaseService: FirebaseService,
    private val userProfileRepository: UserProfileRepository
) {
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> =
        firebaseService.signInWithEmail(email, password)

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        val result = firebaseService.signUpWithEmail(email, password)
        if (result.isSuccess) {
            val user = result.getOrNull()
            if (user != null) {
                // Auto-create free user profile
                userProfileRepository.createFreeProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName
                )
            }
        }
        return result
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        val result = firebaseService.signInWithGoogle(idToken)
        if (result.isSuccess) {
            val user = result.getOrNull()
            if (user != null) {
                // Check if profile exists, create if not
                val profile = userProfileRepository.getUserProfile(user.uid)
                if (profile == null) {
                    userProfileRepository.createFreeProfile(
                        userId = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName
                    )
                }
            }
        }
        return result
    }

    fun observeAuthState(): Flow<FirebaseUser?> = firebaseService.observeAuthState()

    fun signOut() = firebaseService.signOut()

    fun currentUser(): FirebaseUser? = firebaseService.getCurrentUser()
}
