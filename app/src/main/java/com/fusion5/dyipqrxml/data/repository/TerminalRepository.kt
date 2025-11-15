package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.Terminal
import kotlinx.coroutines.flow.Flow

interface TerminalRepository {
    fun observeAll(): Flow<List<Terminal>>
    fun search(query: String): Flow<List<Terminal>>
    suspend fun getById(id: Long): Terminal?
    fun observeById(id: Long): Flow<Terminal?>
}

