package com.qa.verifyandtrack.app.data.repository

import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.service.FirebaseService
import kotlinx.coroutines.flow.Flow

class RepoRepository(private val firebaseService: FirebaseService) {
    fun observeRepos(userId: String): Flow<List<Repository>> =
        firebaseService.subscribeToRepos(userId)

    suspend fun saveRepos(userId: String, repos: List<Repository>) =
        firebaseService.saveUserRepos(userId, repos)

    suspend fun getRepos(userId: String): List<Repository> =
        firebaseService.getRepos(userId)
}
