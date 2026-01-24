package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ConfigViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val repoRepository = AppContainer.repoRepository

    private val _repos = MutableStateFlow<List<Repository>>(emptyList())
    val repos: StateFlow<List<Repository>> = _repos

    private val _globalSettings = MutableStateFlow<GlobalSettings?>(null)
    val globalSettings: StateFlow<GlobalSettings?> = _globalSettings

    private val _selectedRepo = MutableStateFlow<Repository?>(null)
    val selectedRepo: StateFlow<Repository?> = _selectedRepo

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
                }
        }

        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) flowOf(null) else repoRepository.observeGlobalSettings(user.uid)
                }
                .collect { settings ->
                    _globalSettings.value = settings
                }
        }
    }

    fun selectRepo(repo: Repository?) {
        _selectedRepo.value = repo
    }

    fun saveGlobalSettings(settings: GlobalSettings) {
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.uid
            if (userId == null) {
                _error.value = "No authenticated user."
                return@launch
            }
            repoRepository.saveGlobalSettings(userId, settings)
        }
    }

    fun addRepo(repo: Repository) {
        updateRepos { current -> current + repo }
    }

    fun updateRepo(repo: Repository) {
        updateRepos { current -> current.map { if (it.id == repo.id) repo else it } }
    }

    fun deleteRepo(repoId: String) {
        updateRepos { current -> current.filterNot { it.id == repoId } }
    }

    fun addApp(repoId: String, app: AppConfig) {
        updateRepos { current ->
            current.map { repo ->
                if (repo.id == repoId) repo.copy(apps = repo.apps + app) else repo
            }
        }
    }

    fun updateApp(repoId: String, app: AppConfig) {
        updateRepos { current ->
            current.map { repo ->
                if (repo.id == repoId) {
                    repo.copy(apps = repo.apps.map { if (it.id == app.id) app else it })
                } else repo
            }
        }
    }

    fun deleteApp(repoId: String, appId: String) {
        updateRepos { current ->
            current.map { repo ->
                if (repo.id == repoId) repo.copy(apps = repo.apps.filterNot { it.id == appId }) else repo
            }
        }
    }

    private fun updateRepos(transform: (List<Repository>) -> List<Repository>) {
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.uid
            if (userId == null) {
                _error.value = "No authenticated user."
                return@launch
            }
            val updated = transform(_repos.value)
            _repos.value = updated
            repoRepository.saveRepos(userId, updated)
        }
    }
}
