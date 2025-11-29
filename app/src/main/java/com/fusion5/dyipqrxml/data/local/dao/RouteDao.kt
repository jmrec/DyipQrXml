package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteWithTerminals
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM Routes")
    fun observeAllRoutesWithTerminals(): Flow<List<RouteWithTerminals>>
    
    @Transaction
    @Query("SELECT * FROM Routes WHERE id = :id LIMIT 1")
    suspend fun getRouteWithTerminalsById(id: Long): RouteWithTerminals?

    @Transaction
    @Query("SELECT * FROM Routes WHERE route_code LIKE '%' || :query || '%' ORDER BY route_code")
    fun searchRoutesWithTerminals(query: String): Flow<List<RouteWithTerminals>>

    @Transaction
    @Query("""
        SELECT DISTINCT r.*
        FROM Routes r
        INNER JOIN Favorites f ON r.id = f.route_id
        WHERE f.user_id = :userId
        ORDER BY r.route_code
    """)
    fun observeFavoriteRoutesWithTerminals(userId: Long): Flow<List<RouteWithTerminals>>

    @Transaction
    @Query("""
        SELECT DISTINCT r.*
        FROM Routes r
        INNER JOIN Favorites f ON r.id = f.route_id
        WHERE f.user_id = :userId AND r.route_code LIKE '%' || :query || '%'
        ORDER BY r.route_code
    """)
    fun searchFavoriteRoutesWithTerminals(userId: Long, query: String): Flow<List<RouteWithTerminals>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    @Query("DELETE FROM Routes")
    suspend fun clearRoutes()
    
    @Transaction
    suspend fun clearAll() {
        clearRoutes()
    }

    @Transaction
    @Query("""
        SELECT * FROM Routes
        WHERE start_terminal_id = :terminalId OR end_terminal_id = :terminalId
        ORDER BY route_code
    """)
    suspend fun findRoutesByTerminalId(terminalId: Long): List<RouteWithTerminals>

    @Query("SELECT COUNT(*) FROM Routes")
    suspend fun count(): Int
}
