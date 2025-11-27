package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.dao.FavoriteDao
import com.fusion5.dyipqrxml.data.local.entity.FavoriteEntity
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.Favorite
import com.fusion5.dyipqrxml.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalFavoriteRepository(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun observeFavorites(userId: Long): Flow<List<Favorite>> =
        favoriteDao.observeFavorites(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun addFavorite(favorite: Favorite) {
        favoriteDao.upsert(
            FavoriteEntity(
                id = favorite.id,
                userId = favorite.userId,
                terminalId = favorite.terminalId,
                routeId = favorite.routeId,
                createdAt = favorite.createdAt
            )
        )
    }

    override suspend fun removeFavorite(favoriteId: Long) {
        favoriteDao.deleteById(favoriteId)
    }

    override suspend fun removeFavoriteByTerminal(userId: Long, terminalId: Long) {
        favoriteDao.deleteByTerminal(userId, terminalId)
    }

    override suspend fun isFavorite(userId: Long, terminalId: Long): Boolean =
        favoriteDao.isFavorite(userId, terminalId) > 0
}
