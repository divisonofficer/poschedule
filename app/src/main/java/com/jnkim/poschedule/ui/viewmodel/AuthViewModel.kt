package com.jnkim.poschedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnkim.poschedule.data.local.AuthTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class TokenAcquired(val token: String) : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: AuthTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onTokenReceived(token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            tokenManager.saveAccessToken(token)
            _uiState.value = AuthUiState.TokenAcquired(token)
            // In a real app, you would now fetch the API Key using this token
            // For MVP, we'll simulate success
            _uiState.value = AuthUiState.Success
        }
    }

    fun logout() {
        tokenManager.clearAll()
        _uiState.value = AuthUiState.Idle
    }

    fun isUserLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
}
