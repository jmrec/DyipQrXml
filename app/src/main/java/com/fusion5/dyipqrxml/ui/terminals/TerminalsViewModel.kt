package com.fusion5.dyipqrxml.ui.terminals

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.model.Favorite
import com.fusion5.dyipqrxml.data.model.Route
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class RouteWithFavorite(
    val route: Route,
    val isFavorite: Boolean
)

data class TerminalsUiState(
    val routes: List<RouteWithFavorite> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalsViewModel(
    private val routeRepository: RouteRepository,
    private val favoriteRepository: FavoriteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalsUiState(isLoading = true))
    val uiState: StateFlow<TerminalsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            authRepository.currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        combine(routeRepository.observeAllRoutesWithTerminals(), searchQuery) { routes, query ->
                            val filtered = filterRoutes(routes, query)
                            val routesWithFavorite = filtered.map { route ->
                                RouteWithFavorite(route, isFavorite = false)
                            }
                            TerminalsUiState(
                                routes = routesWithFavorite,
                                searchQuery = query,
                                isUserLoggedIn = false,
                                isLoading = false
                            )
                        }
                    } else {
                        combine(
                            routeRepository.observeAllRoutesWithTerminals(),
                            favoriteRepository.observeFavorites(user.id),
                            searchQuery
                        ) { routes, favorites, query ->
                            val filtered = filterRoutes(routes, query)
                            val favoriteRouteIds = favorites.map { it.routeId }.toSet()
                            val routesWithFavorite = filtered.map { route ->
                                RouteWithFavorite(route, isFavorite = favoriteRouteIds.contains(route.id))
                            }
                            TerminalsUiState(
                                routes = routesWithFavorite,
                                searchQuery = query,
                                isUserLoggedIn = true,
                                isLoading = false
                            )
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun filterRoutes(routes: List<Route>, query: String): List<Route> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return routes
        return routes.filter { route ->
            route.routeCode.contains(trimmed, ignoreCase = true) ||
                route.startTerminalName.contains(trimmed, ignoreCase = true) ||
                route.endTerminalName.contains(trimmed, ignoreCase = true)
        }
    }

    fun search(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleFavorite(routeId: Long) {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser.first()
            if (currentUser != null) {
                val isCurrentlyFavorite = favoriteRepository.isFavorite(currentUser.id, routeId)
                if (isCurrentlyFavorite) {
                    favoriteRepository.removeFavoriteByRoute(currentUser.id, routeId)
                } else {
                    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val favorite = Favorite(
                        id = 0, // Will be auto-generated by database
                        userId = currentUser.id,
                        routeId = routeId,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                    favoriteRepository.addFavorite(favorite)
                }
            }
        }
    }
}