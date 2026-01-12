package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.model.TodoItem
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
    private val gitHubRepository = AppContainer.gitHubRepository

    private val _repos = MutableStateFlow<List<Repository>>(emptyList())
    val repos: StateFlow<List<Repository>> = _repos

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
    }

    fun refreshTodos() {
        loadTodos(_repos.value)
    }

    private fun loadTodos(repos: List<Repository>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val items = withContext(Dispatchers.IO) {
                repos.map { repo ->
                    val token = repo.githubToken
                    val count = if (!token.isNullOrBlank()) {
                        gitHubRepository.initialize(token)
                        runCatching { gitHubRepository.getOpenIssueCount(repo.owner, repo.name) }.getOrDefault(0)
                    } else {
                        0
                    }
                    TodoItem(repoId = repo.id, repoName = repo.displayLabel, openIssueCount = count)
                }
            }
            _todos.value = items
            _isLoading.value = false
        }
    }
}
