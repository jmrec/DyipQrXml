package com.fusion5.dyipqrxml.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val navigateToLogin: Boolean = false
)

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user == null) {
                    _state.value = ProfileUiState(navigateToLogin = true)
                } else {
                    _state.value = ProfileUiState(
                        fullName = user.fullName,
                        email = user.email,
                        navigateToLogin = false
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _state.value = _state.value.copy(navigateToLogin = true)
        }
    }

    fun onNavigated() {
        _state.value = _state.value.copy(navigateToLogin = false)
    }
}

