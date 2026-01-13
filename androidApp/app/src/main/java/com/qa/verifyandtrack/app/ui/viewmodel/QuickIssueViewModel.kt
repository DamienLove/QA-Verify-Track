package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.model.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickIssueViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository
    private val gitHubRepository = AppContainer.gitHubRepository

    private val _repo = MutableStateFlow<Repository?>(null)
    val repo: StateFlow<Repository?> = _repo

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun setRepoId(repoId: String?) {
        if (repoId.isNullOrBlank()) {
            _error.value = "Missing repository ID."
            return
        }
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.uid
            if (userId == null) {
                _error.value = "No authenticated user."
                return@launch
            }
            val repos = repoRepository.getRepos(userId)
            _repo.value = repos.firstOrNull { it.id == repoId }
        }
    }

    fun createIssue(title: String, description: String, labels: List<String>, buildNumber: String?) {
        val repo = _repo.value
        if (repo == null) {
            _error.value = "Repository not selected."
            return
        }
        if (repo.githubToken.isNullOrBlank()) {
            _error.value = "Missing GitHub token."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _success.value = false
            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(repo.githubToken)
                runCatching {
                    val created = gitHubRepository.createIssue(repo.owner, repo.name, title, description, labels)
                    val tag = buildNumber?.trim().orEmpty()
                    if (tag.isNotBlank()) {
                        val added = gitHubRepository.addComment(repo.owner, repo.name, created.number, "open v$tag")
                        if (!added) {
                            throw IllegalStateException("Issue created, but failed to add build comment.")
                        }
                    }
                }
            }
            _isLoading.value = false
            _success.value = result.isSuccess
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to create issue."
            }
        }
    }
}
