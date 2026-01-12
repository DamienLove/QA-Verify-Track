package com.qa.verifyandtrack.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    val currentUser: StateFlow<FirebaseUser?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser())

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signIn(email, password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Unable to sign in.") }
            )
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signUp(email, password)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Unable to sign up.") }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithGoogle(idToken)
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.message ?: "Unable to sign in with Google.") }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}
