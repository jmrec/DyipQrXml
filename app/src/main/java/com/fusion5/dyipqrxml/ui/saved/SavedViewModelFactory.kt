package com.fusion5.dyipqrxml.ui.saved

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fusion5.dyipqrxml.ServiceLocator

class SavedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedViewModel::class.java)) {
            val authRepository = ServiceLocator.provideAuthRepository(context)
            val favoriteRepository = ServiceLocator.provideFavoriteRepository(context)
            val routeRepository = ServiceLocator.provideRouteRepository(context)
            @Suppress("UNCHECKED_CAST")
            return SavedViewModel(authRepository, favoriteRepository, routeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}