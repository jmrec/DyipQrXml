package com.fusion5.dyipqrxml.ui.terminals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fusion5.dyipqrxml.ServiceLocator
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import com.fusion5.dyipqrxml.data.repository.RouteRepository

class TerminalsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TerminalsViewModel::class.java)) {
            val routeRepo: RouteRepository = ServiceLocator.provideRouteRepository(context)
            val favoriteRepo: FavoriteRepository = ServiceLocator.provideFavoriteRepository(context)
            val authRepo: AuthRepository = ServiceLocator.provideAuthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return TerminalsViewModel(routeRepo, favoriteRepo, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}