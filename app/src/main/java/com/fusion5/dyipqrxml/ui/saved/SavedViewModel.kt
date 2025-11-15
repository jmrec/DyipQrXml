package com.fusion5.dyipqrxml.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.model.Terminal
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.TerminalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val favorites: List<Terminal> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

class SavedViewModel(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val terminalRepository: TerminalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        observeUserAndFavorites()
    }

    private fun observeUserAndFavorites() {
        viewModelScope.launch {
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(SavedUiState(isUserLoggedIn = false))
                    } else {
                        favoriteRepository.observeFavorites(user.id)
                            .combine(terminalRepository.observeAll()) { favorites, terminals ->
                                val terminalMap = terminals.associateBy { it.id }
                                val favTerminals = favorites.mapNotNull { f -> f.terminalId?.let { terminalMap[it] } }
                                SavedUiState(
                                    favorites = favTerminals,
                                    isUserLoggedIn = true
                                )
                            }
                    }
                }
                .collect { state -> _uiState.update { state } }
        }
    }

    suspend fun removeFavorite(terminalId: Long) {
        authRepository.currentUser.collect { currentUser ->
            if (currentUser != null) {
                favoriteRepository.removeFavoriteByTerminal(currentUser.id, terminalId)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}