package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fusion5.dyipqrxml.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeFavorites(userId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND terminalId = :terminalId LIMIT 1")
    suspend fun getFavorite(userId: Long, terminalId: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM favorites WHERE userId = :userId AND terminalId = :terminalId")
    suspend fun deleteByTerminal(userId: Long, terminalId: Long)

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND terminalId = :terminalId")
    suspend fun isFavorite(userId: Long, terminalId: Long): Int
}

