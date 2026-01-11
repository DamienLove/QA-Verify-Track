package com.qa.verifyandtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.FirebaseService
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.Repo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val user: FirebaseUser? = null,
    val repos: List<Repo> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null
)

class MainViewModel(
    private val service: FirebaseService = FirebaseService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var reposJob: Job? = null

    init {
        viewModelScope.launch {
            service.authState().collect { user ->
                _uiState.update { it.copy(user = user, message = null) }
                subscribeToRepos(user)
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

    fun signIn(email: String, password: String) = launchWithStatus {
        service.signIn(email, password)
    }

    fun register(email: String, password: String) = launchWithStatus {
        service.register(email, password)
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = launchWithStatus {
        service.signInWithGoogle(account)
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
