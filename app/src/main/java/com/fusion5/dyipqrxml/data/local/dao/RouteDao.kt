package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteTerminalCrossRef
import com.fusion5.dyipqrxml.data.local.entity.RouteWithTerminals
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes")
    fun observeAllRoutesWithTerminals(): Flow<List<RouteWithTerminals>>
    
    @Transaction
    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun getRouteWithTerminalsById(id: Long): RouteWithTerminals?

    @Transaction
    @Query("SELECT * FROM routes WHERE name LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY name")
    fun searchRoutesWithTerminals(query: String): Flow<List<RouteWithTerminals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteTerminalCrossRefs(refs: List<RouteTerminalCrossRef>)

    @Query("DELETE FROM routes")
    suspend fun clearRoutes()
    
    @Query("DELETE FROM route_terminal_cross_ref")
    suspend fun clearCrossRefs()

    @Transaction
    suspend fun clearAll() {
        clearCrossRefs()
        clearRoutes()
    }

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun count(): Int
}
