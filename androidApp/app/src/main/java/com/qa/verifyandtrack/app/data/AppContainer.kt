package com.qa.verifyandtrack.app.data

import com.qa.verifyandtrack.app.data.repository.AIRepository
import com.qa.verifyandtrack.app.data.repository.AuthRepository
import com.qa.verifyandtrack.app.data.repository.GitHubRepository
import com.qa.verifyandtrack.app.data.repository.RepoRepository
import com.qa.verifyandtrack.app.data.repository.UserProfileRepository
import com.qa.verifyandtrack.app.data.service.AIService
import com.qa.verifyandtrack.app.data.service.FirebaseService
import com.qa.verifyandtrack.app.data.service.GitHubService

object AppContainer {
    private val firebaseService = FirebaseService()
    private val gitHubService = GitHubService()
    private val aiService = AIService()

    val userProfileRepository = UserProfileRepository(firebaseService)
    val authRepository = AuthRepository(firebaseService, userProfileRepository)
    val repoRepository = RepoRepository(firebaseService)
    val gitHubRepository = GitHubRepository(gitHubService)
    val aiRepository = AIRepository(aiService)

    fun googleSignInClient(context: android.content.Context, webClientId: String) =
        firebaseService.buildGoogleSignInClient(context, webClientId)
}
