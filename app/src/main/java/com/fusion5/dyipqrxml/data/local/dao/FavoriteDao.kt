package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fusion5.dyipqrxml.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM Favorites WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeFavorites(userId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM Favorites WHERE user_id = :userId AND route_id = :routeId LIMIT 1")
    suspend fun getFavorite(userId: Long, routeId: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM Favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM Favorites WHERE user_id = :userId AND route_id = :routeId")
    suspend fun deleteByRoute(userId: Long, routeId: Long)

    @Query("SELECT COUNT(*) FROM Favorites WHERE user_id = :userId AND route_id = :routeId")
    suspend fun isFavorite(userId: Long, routeId: Long): Int
}
