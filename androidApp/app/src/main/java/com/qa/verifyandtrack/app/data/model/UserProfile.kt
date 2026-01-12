package com.qa.verifyandtrack.app.data.model

enum class SubscriptionTier {
    FREE, PRO
}

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val repoLimit: Int = 2, // FREE: 2, PRO: -1 (unlimited)
    val aiAnalysisRemaining: Int = 0, // FREE: 0, PRO: -1 (unlimited)
    val subscriptionExpiresAt: Long? = null,
    val showAds: Boolean = true, // FREE: true, PRO: false
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createFreeProfile(userId: String, email: String, displayName: String? = null): UserProfile {
            return UserProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                tier = SubscriptionTier.FREE,
                repoLimit = 2,
                aiAnalysisRemaining = 0,
                showAds = true
            )
        }

        fun createProProfile(
            userId: String,
            email: String,
            displayName: String? = null,
            expiresAt: Long? = null
        ): UserProfile {
            return UserProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                tier = SubscriptionTier.PRO,
                repoLimit = -1, // unlimited
                aiAnalysisRemaining = -1, // unlimited
                showAds = false,
                subscriptionExpiresAt = expiresAt
            )
        }
    }

    val isPro: Boolean
        get() = tier == SubscriptionTier.PRO

    val isFree: Boolean
        get() = tier == SubscriptionTier.FREE

    val hasUnlimitedRepos: Boolean
        get() = repoLimit == -1

    val hasUnlimitedAI: Boolean
        get() = aiAnalysisRemaining == -1

    val isSubscriptionActive: Boolean
        get() {
            if (tier == SubscriptionTier.FREE) return true
            if (subscriptionExpiresAt == null) return true // lifetime
            return System.currentTimeMillis() < subscriptionExpiresAt
        }
}
