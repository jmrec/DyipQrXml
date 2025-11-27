package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.dao.TerminalDao
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.Terminal
import com.fusion5.dyipqrxml.data.repository.TerminalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalTerminalRepository(
    private val terminalDao: TerminalDao
) : TerminalRepository {
    override fun observeAll(): Flow<List<Terminal>> =
        terminalDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAllList(): List<Terminal> =
        terminalDao.getAllList().map { it.toDomain() }

    override fun search(query: String): Flow<List<Terminal>> =
        terminalDao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Terminal? =
        terminalDao.getById(id)?.toDomain()

    override fun observeById(id: Long): Flow<Terminal?> =
        terminalDao.observeById(id).map { it?.toDomain() }
}
