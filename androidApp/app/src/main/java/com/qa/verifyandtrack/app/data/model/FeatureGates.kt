package com.qa.verifyandtrack.app.data.model

object FeatureGates {
    /**
     * Check if user can add more repositories
     * FREE: Up to 2 repositories
     * PRO: Unlimited repositories
     */
    fun canAddRepo(userProfile: UserProfile?, currentRepoCount: Int): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> currentRepoCount < userProfile.repoLimit
            SubscriptionTier.PRO -> true // unlimited
        }
    }

    /**
     * Check if user can use AI analysis feature
     * FREE: Not available
     * PRO: Unlimited access
     */
    fun canUseAIAnalysis(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user can merge pull requests
     * FREE: Not available
     * PRO: Available
     */
    fun canMergePR(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user can deny/close pull requests
     * FREE: Not available
     * PRO: Available
     */
    fun canDenyPR(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user can resolve PR conflicts
     * FREE: Not available
     * PRO: Available
     */
    fun canResolveConflicts(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user can export data to CSV
     * FREE: Not available
     * PRO: Available
     */
    fun canExportData(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user can access advanced filters
     * FREE: Not available
     * PRO: Available
     */
    fun canAccessAdvancedFilters(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false
        if (!userProfile.isSubscriptionActive) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> false
            SubscriptionTier.PRO -> true
        }
    }

    /**
     * Check if user should see ads
     * FREE: Show ads
     * PRO: No ads
     */
    fun shouldShowAds(userProfile: UserProfile?): Boolean {
        if (userProfile == null) return false

        return when (userProfile.tier) {
            SubscriptionTier.FREE -> userProfile.showAds
            SubscriptionTier.PRO -> false
        }
    }

    /**
     * Get feature name for paywall display
     */
    fun getFeatureName(feature: Feature): String {
        return when (feature) {
            Feature.UNLIMITED_REPOS -> "Unlimited Repositories"
            Feature.AI_ANALYSIS -> "AI-Powered Analysis"
            Feature.MERGE_PR -> "Merge Pull Requests"
            Feature.DENY_PR -> "Close Pull Requests"
            Feature.RESOLVE_CONFLICTS -> "Resolve PR Conflicts"
            Feature.EXPORT_DATA -> "Export & Reporting"
            Feature.ADVANCED_FILTERS -> "Advanced Filters"
            Feature.AD_FREE -> "Ad-Free Experience"
        }
    }
}

enum class Feature {
    UNLIMITED_REPOS,
    AI_ANALYSIS,
    MERGE_PR,
    DENY_PR,
    RESOLVE_CONFLICTS,
    EXPORT_DATA,
    ADVANCED_FILTERS,
    AD_FREE
}
