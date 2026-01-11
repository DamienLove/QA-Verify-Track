package com.qa.verifyandtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.FirebaseService
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.GlobalSettings
import com.qa.verifyandtrack.app.data.model.Repo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val user: FirebaseUser? = null,
    val repos: List<Repo> = emptyList(),
    val globalSettings: GlobalSettings = GlobalSettings(),
    val loading: Boolean = false,
    val message: String? = null
)

class MainViewModel(
    private val service: FirebaseService = FirebaseService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var reposJob: Job? = null
    private var settingsJob: Job? = null

    init {
        viewModelScope.launch {
            service.authState().collect { user ->
                _uiState.update { it.copy(user = user, message = null) }
                subscribeToRepos(user)
                subscribeToSettings(user)
            }
        }
    }

    private fun subscribeToRepos(user: FirebaseUser?) {
        reposJob?.cancel()
        if (user == null) {
            _uiState.update { it.copy(repos = emptyList()) }
            return
        }
        reposJob = viewModelScope.launch {
            service.observeRepos(user.uid).collect { repos ->
                _uiState.update { it.copy(repos = repos) }
            }
        }
    }

    private fun subscribeToSettings(user: FirebaseUser?) {
        settingsJob?.cancel()
        if (user == null) {
            _uiState.update { it.copy(globalSettings = GlobalSettings()) }
            return
        }
        settingsJob = viewModelScope.launch {
            service.observeGlobalSettings(user.uid).collect { settings ->
                _uiState.update { it.copy(globalSettings = settings) }
            }
        }
    }

    fun signIn(email: String, password: String) = launchWithStatus {
        service.signIn(email, password)
    }

    fun register(email: String, password: String) = launchWithStatus {
        service.register(email, password)
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = launchWithStatus {
        service.signInWithGoogle(account)
    }

    fun saveGlobalSettings(settings: GlobalSettings) {
        val userId = _uiState.value.user?.uid ?: return
        viewModelScope.launch {
            runCatching {
                service.saveGlobalSettings(userId, settings)
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message) }
            }
        }
    }

    fun signOut() = viewModelScope.launch {
        runCatching { service.signOut() }
    }

    private fun launchWithStatus(block: suspend () -> Unit) = viewModelScope.launch {
        _uiState.update { it.copy(loading = true, message = null) }
        runCatching { block() }
            .onFailure { error -> _uiState.update { it.copy(message = error.message) } }
        _uiState.update { it.copy(loading = false) }
    }
}
