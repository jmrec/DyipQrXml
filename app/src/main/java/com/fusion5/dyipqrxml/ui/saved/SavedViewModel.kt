package com.fusion5.dyipqrxml.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.model.Route
import com.fusion5.dyipqrxml.data.model.Terminal
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import com.fusion5.dyipqrxml.data.repository.TerminalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val favorites: List<Route> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

class SavedViewModel(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    init {
        observeUserAndFavorites()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUserAndFavorites() {
        viewModelScope.launch {
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(SavedUiState(isUserLoggedIn = false))
                    } else {
                        favoriteRepository.observeFavorites(user.id)
                            .combine(routeRepository.observeAllRoutesWithTerminals()) { favorites, routes ->
                                val favoriteRoutes = favorites.mapNotNull { favorite ->
                                    routes.find { it.id == favorite.routeId }
                                }
                                SavedUiState(
                                    favorites = favoriteRoutes,
                                    isUserLoggedIn = true
                                )
                            }
                    }
                }
                .collect { state -> _uiState.update { state } }
        }
    }

    suspend fun removeFavorite(routeId: Long) {
        authRepository.currentUser.collect { currentUser ->
            if (currentUser != null) {
                favoriteRepository.removeFavoriteByRoute(currentUser.id, routeId)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}