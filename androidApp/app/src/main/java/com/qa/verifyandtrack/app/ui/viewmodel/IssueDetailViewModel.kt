package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.data.model.Issue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IssueDetailViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository
    private val gitHubRepository = AppContainer.gitHubRepository

    private val _issue = MutableStateFlow<Issue?>(null)
    val issue: StateFlow<Issue?> = _issue

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadIssue(repoId: String?, issueNumber: Int?) {
        if (repoId.isNullOrBlank() || issueNumber == null) {
            _error.value = "Missing issue details."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val userId = authRepository.currentUser()?.uid
            if (userId == null) {
                _error.value = "No authenticated user."
                _isLoading.value = false
                return@launch
            }

            val repo = repoRepository.getRepos(userId).firstOrNull { it.id == repoId }
            if (repo == null) {
                _error.value = "Repository not found."
                _isLoading.value = false
                return@launch
            }
            val token = repo.githubToken
            if (token.isNullOrBlank()) {
                _error.value = "Missing GitHub token for ${repo.displayLabel}."
                _isLoading.value = false
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(token)
                runCatching {
                    val issue = gitHubRepository.getIssues(repo.owner, repo.name, "all")
                        .firstOrNull { it.number == issueNumber }
                    if (issue == null) {
                        return@runCatching Pair<Issue?, List<Comment>>(null, emptyList())
                    }
                    val comments = runCatching {
                        gitHubRepository.getIssueComments(repo.owner, repo.name, issue.number)
                    }.getOrDefault(emptyList())
                    Pair(issue, comments)
                }
            }

            result.onSuccess { (issue, comments) ->
                if (issue == null) {
                    _error.value = "Issue not found."
                } else {
                    _issue.value = issue.copy(comments = comments)
                    _comments.value = comments
                }
            }.onFailure { error ->
                _error.value = error.message ?: "Failed to load issue."
            }

            _isLoading.value = false
        }
    }
}
