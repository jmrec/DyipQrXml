package com.fusion5.dyipqrxml.ui.terminals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.model.Terminal
import com.fusion5.dyipqrxml.data.model.Favorite
import com.fusion5.dyipqrxml.data.model.User
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
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
import kotlinx.coroutines.flow.first

data class TerminalListItem(
    val terminal: Terminal,
    val isFavorite: Boolean
)

data class TerminalsUiState(
    val items: List<TerminalListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalsViewModel(
    private val terminalRepository: TerminalRepository,
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
                        combine(terminalRepository.observeAll(), searchQuery) { terminals, query ->
                            val filtered = filterTerminals(terminals, query)
                            TerminalsUiState(
                                items = filtered.map { TerminalListItem(it, false) },
                                searchQuery = query,
                                isUserLoggedIn = false,
                                isLoading = false
                            )
                        }
                    } else {
                        combine(
                            terminalRepository.observeAll(),
                            favoriteRepository.observeFavorites(user.id),
                            searchQuery
                        ) { terminals, favorites, query ->
                            val filtered = filterTerminals(terminals, query)
                            val favoriteIds = favorites.mapNotNull { it.terminalId }.toSet()
                            TerminalsUiState(
                                items = filtered.map { TerminalListItem(it, it.id in favoriteIds) },
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

    private fun filterTerminals(terminals: List<Terminal>, query: String): List<Terminal> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return terminals
        return terminals.filter { terminal ->
            terminal.name.contains(trimmed, ignoreCase = true) ||
                terminal.description.contains(trimmed, ignoreCase = true)
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

    fun toggleFavorite(terminalId: Long) {
        viewModelScope.launch {
            val user = authRepository.currentUser.first()
            if (user == null) {
                _uiState.update { it.copy(errorMessage = "Login required to save") }
                return@launch
            }
            val currentItems = _uiState.value.items
            val target = currentItems.firstOrNull { it.terminal.id == terminalId } ?: return@launch
            if (target.isFavorite) {
                favoriteRepository.removeFavoriteByTerminal(user.id, terminalId)
            } else {
                favoriteRepository.addFavorite(
                    Favorite(
                        id = 0,
                        userId = user.id,
                        terminalId = terminalId,
                        routeId = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}