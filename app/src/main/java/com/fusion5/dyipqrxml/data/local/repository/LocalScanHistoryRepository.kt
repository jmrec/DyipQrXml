package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.dao.ScanHistoryDao
import com.fusion5.dyipqrxml.data.local.entity.ScanHistoryEntity
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.ScanHistory
import com.fusion5.dyipqrxml.data.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalScanHistoryRepository(
    private val scanHistoryDao: ScanHistoryDao
) : ScanHistoryRepository {
    override fun observeHistory(userId: Long?): Flow<List<ScanHistory>> =
        scanHistoryDao.observeHistory(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveScan(content: String, userId: Long?) {
        scanHistoryDao.insert(
            ScanHistoryEntity(
                userId = userId,
                content = content
            )
        )
    }
}
