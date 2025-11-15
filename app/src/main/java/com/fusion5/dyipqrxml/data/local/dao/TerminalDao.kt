package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fusion5.dyipqrxml.data.local.entity.TerminalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalDao {
    @Query("SELECT * FROM terminals ORDER BY name")
    fun observeAll(): Flow<List<TerminalEntity>>

    @Query("SELECT * FROM terminals WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name")
    fun search(query: String): Flow<List<TerminalEntity>>

    @Query("SELECT * FROM terminals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TerminalEntity?

    @Query("SELECT * FROM terminals WHERE id = :id")
    fun observeById(id: Long): Flow<TerminalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terminals: List<TerminalEntity>)

    @Query("DELETE FROM terminals")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM terminals")
    suspend fun count(): Int
}
