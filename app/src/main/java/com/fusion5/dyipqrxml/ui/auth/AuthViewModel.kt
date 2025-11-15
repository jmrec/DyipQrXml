package com.fusion5.dyipqrxml.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.login(email.trim(), password) }
                .onSuccess {
                    _uiState.value = AuthUiState(success = true)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(errorMessage = error.message ?: "Login failed")
                }
        }
    }

    fun signup(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.signup(fullName.trim(), email.trim(), password) }
                .onSuccess {
                    _uiState.value = AuthUiState(success = true)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState(errorMessage = error.message ?: "Signup failed")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}

