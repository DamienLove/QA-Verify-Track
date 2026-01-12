package com.qa.verifyandtrack.app.data.repository

import com.qa.verifyandtrack.app.data.model.SubscriptionTier
import com.qa.verifyandtrack.app.data.model.UserProfile
import com.qa.verifyandtrack.app.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val firebaseService: FirebaseService) {

    /**
     * Get user profile by user ID
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            firebaseService.getUserProfile(userId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        return try {
            firebaseService.updateUserProfile(profile)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Observe user profile changes in real-time
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return firebaseService.observeUserProfile(userId)
    }

    /**
     * Create initial free profile for new user
     */
    suspend fun createFreeProfile(
        userId: String,
        email: String,
        displayName: String? = null
    ): Boolean {
        return try {
            val profile = UserProfile.createFreeProfile(userId, email, displayName)
            firebaseService.updateUserProfile(profile)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Upgrade user to PRO tier
     */
    suspend fun upgradeToPro(
        userId: String,
        expiresAt: Long? = null // null means lifetime
    ): Boolean {
        return try {
            val currentProfile = getUserProfile(userId) ?: return false
            val upgradedProfile = currentProfile.copy(
                tier = SubscriptionTier.PRO,
                repoLimit = -1, // unlimited
                aiAnalysisRemaining = -1, // unlimited
                showAds = false,
                subscriptionExpiresAt = expiresAt,
                updatedAt = System.currentTimeMillis()
            )
            firebaseService.updateUserProfile(upgradedProfile)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Downgrade user to FREE tier (subscription expired or canceled)
     */
    suspend fun downgradeToFree(userId: String): Boolean {
        return try {
            val currentProfile = getUserProfile(userId) ?: return false
            val downgradedProfile = currentProfile.copy(
                tier = SubscriptionTier.FREE,
                repoLimit = 2,
                aiAnalysisRemaining = 0,
                showAds = true,
                subscriptionExpiresAt = null,
                updatedAt = System.currentTimeMillis()
            )
            firebaseService.updateUserProfile(downgradedProfile)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if subscription is expired and downgrade if necessary
     */
    suspend fun checkAndUpdateSubscriptionStatus(userId: String): Boolean {
        return try {
            val profile = getUserProfile(userId) ?: return false

            if (profile.tier == SubscriptionTier.PRO && !profile.isSubscriptionActive) {
                downgradeToFree(userId)
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update display name and avatar URL
     */
    suspend fun updateUserInfo(
        userId: String,
        displayName: String?,
        avatarUrl: String?
    ): Boolean {
        return try {
            val currentProfile = getUserProfile(userId) ?: return false
            val updatedProfile = currentProfile.copy(
                displayName = displayName,
                avatarUrl = avatarUrl,
                updatedAt = System.currentTimeMillis()
            )
            firebaseService.updateUserProfile(updatedProfile)
            true
        } catch (e: Exception) {
            false
        }
    }
}
