package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.resolveGithubToken
import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.PullRequestDetail
import com.qa.verifyandtrack.app.data.model.PullRequestFile
import com.qa.verifyandtrack.app.data.model.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PullRequestDetailViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository
    private val gitHubRepository = AppContainer.gitHubRepository

    private val _pullRequest = MutableStateFlow<PullRequestDetail?>(null)
    val pullRequest: StateFlow<PullRequestDetail?> = _pullRequest

    private val _files = MutableStateFlow<List<PullRequestFile>>(emptyList())
    val files: StateFlow<List<PullRequestFile>> = _files

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _isCommenting = MutableStateFlow(false)
    val isCommenting: StateFlow<Boolean> = _isCommenting

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError

    private val _commentSuccess = MutableStateFlow(false)
    val commentSuccess: StateFlow<Boolean> = _commentSuccess

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _globalSettings = MutableStateFlow<GlobalSettings?>(null)
    val globalSettings: StateFlow<GlobalSettings?> = _globalSettings

    private var currentRepo: Repository? = null
    private var currentPullNumber: Int? = null

    fun loadPullRequest(repoId: String?, pullNumber: Int?) {
        if (repoId.isNullOrBlank() || pullNumber == null) {
            _error.value = "Missing pull request details."
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
            val settings = repoRepository.getGlobalSettings(userId)
            _globalSettings.value = settings
            currentRepo = repo
            currentPullNumber = pullNumber
            val token = resolveGithubToken(repo, settings)
            if (token.isNullOrBlank()) {
                _error.value = "Missing GitHub token. Configure a global or repo token."
                _isLoading.value = false
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(token)
                runCatching {
                    val detail = gitHubRepository.getPullRequest(repo.owner, repo.name, pullNumber)
                    val files = gitHubRepository.getPullRequestFiles(repo.owner, repo.name, pullNumber)
                    val comments = gitHubRepository.getIssueComments(repo.owner, repo.name, pullNumber)
                    Triple(detail, files, comments)
                }
            }

            result.onSuccess { (detail, files, comments) ->
                _pullRequest.value = detail
                _files.value = files
                _comments.value = comments
            }.onFailure { error ->
                _error.value = error.message ?: "Failed to load pull request."
            }

            _isLoading.value = false
        }
    }

    fun addComment(text: String) {
        val repo = currentRepo
        val pullNumber = currentPullNumber
        if (repo == null || pullNumber == null) {
            _commentError.value = "Pull request not loaded yet."
            return
        }
        if (text.isBlank()) {
            _commentError.value = "Comment cannot be empty."
            return
        }
        val token = resolveGithubToken(repo, _globalSettings.value)
        if (token.isNullOrBlank()) {
            _commentError.value = "Missing GitHub token. Configure a global or repo token."
            return
        }

        viewModelScope.launch {
            _isCommenting.value = true
            _commentError.value = null
            _commentSuccess.value = false
            val result = withContext(Dispatchers.IO) {
                gitHubRepository.initialize(token)
                runCatching {
                    val added = gitHubRepository.addComment(repo.owner, repo.name, pullNumber, text.trim())
                    if (!added) {
                        throw IllegalStateException("Failed to add comment.")
                    }
                    gitHubRepository.getIssueComments(repo.owner, repo.name, pullNumber)
                }
            }
            result.onSuccess { updatedComments ->
                _comments.value = updatedComments
                _commentSuccess.value = true
            }.onFailure { error ->
                _commentError.value = error.message ?: "Failed to add comment."
            }
            _isCommenting.value = false
        }
    }

    fun clearCommentSuccess() {
        _commentSuccess.value = false
    }
}
