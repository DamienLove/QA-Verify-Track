package com.qa.verifyandtrack.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AppContainer.authRepository
    private val userProfileRepository = AppContainer.userProfileRepository

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode

    init {
        // Observe current user
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _currentUser.value = user
            }
        }

        // Observe user profile
        viewModelScope.launch {
            authRepository.observeAuthState()
                .flatMapLatest { user ->
                    if (user == null) flowOf(null)
                    else userProfileRepository.observeUserProfile(user.uid)
                }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }

        // Load theme preference (would use SharedPreferences in real app)
        // For now, default to system
        _themeMode.value = "system"
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        // TODO: Save to SharedPreferences
        // val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // prefs.edit().putString("theme_mode", mode).apply()
    }

    fun signOut() {
        authRepository.signOut()
    }
}
