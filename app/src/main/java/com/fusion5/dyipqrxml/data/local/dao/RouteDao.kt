package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes WHERE terminalId = :terminalId ORDER BY name")
    fun observeRoutesByTerminal(terminalId: Long): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("DELETE FROM routes")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun count(): Int
}
