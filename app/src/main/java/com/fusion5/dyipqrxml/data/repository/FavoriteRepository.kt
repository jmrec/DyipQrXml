package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(userId: Long): Flow<List<Favorite>>
    suspend fun addFavorite(favorite: Favorite)
    suspend fun removeFavorite(favoriteId: Long)
    suspend fun removeFavoriteByRoute(userId: Long, routeId: Long)
    suspend fun isFavorite(userId: Long, routeId: Long): Boolean
}
