package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.resolveGithubToken
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.model.TodoItem
import com.qa.verifyandtrack.app.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository
    private val userProfileRepository = AppContainer.userProfileRepository
    private val gitHubRepository = AppContainer.gitHubRepository

    private val _repos = MutableStateFlow<List<Repository>>(emptyList())
    val repos: StateFlow<List<Repository>> = _repos

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _globalSettings = MutableStateFlow<GlobalSettings?>(null)
    val globalSettings: StateFlow<GlobalSettings?> = _globalSettings

    init {
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) flowOf(emptyList()) else repoRepository.observeRepos(user.uid)
                }
                .collect { repos ->
                    _repos.value = repos
                    loadTodos(repos)
                }
        }

        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) flowOf(null) else repoRepository.observeGlobalSettings(user.uid)
                }
                .collect { settings ->
                    _globalSettings.value = settings
                    loadTodos(_repos.value)
                }
        }

        // Observe user profile and ensure it exists
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(null)
                    } else {
                        // Check if profile exists, create if not
                        ensureUserProfileExists(user.uid, user.email ?: "", user.displayName)
                        userProfileRepository.observeUserProfile(user.uid)
                    }
                }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }
    }

    private suspend fun ensureUserProfileExists(userId: String, email: String, displayName: String?) {
        try {
            val existingProfile = userProfileRepository.getUserProfile(userId)
            if (existingProfile == null) {
                // Profile doesn't exist, create it
                userProfileRepository.createFreeProfile(userId, email, displayName)
            }
        } catch (e: Exception) {
            _error.value = "Failed to create user profile: ${e.message}"
        }
    }

    fun refreshTodos() {
        loadTodos(_repos.value)
    }

    private fun loadTodos(repos: List<Repository>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val settings = _globalSettings.value
            val items = withContext(Dispatchers.IO) {
                repos.map { repo ->
                    val token = resolveGithubToken(repo, settings)
                    val issueCount = if (!token.isNullOrBlank()) {
                        gitHubRepository.initialize(token)
                        runCatching { gitHubRepository.getOpenIssueCount(repo.owner, repo.name) }.getOrNull()
                    } else {
                        null
                    }
                    val prCount = if (!token.isNullOrBlank()) {
                        gitHubRepository.initialize(token)
                        runCatching { gitHubRepository.getPullRequests(repo.owner, repo.name).size }.getOrNull()
                    } else {
                        null
                    }
                    TodoItem(
                        repoId = repo.id,
                        repoName = repo.displayLabel,
                        openIssueCount = issueCount,
                        openPrCount = prCount
                    )
                }
            }
            _todos.value = items
            _isLoading.value = false
        }
    }
}
