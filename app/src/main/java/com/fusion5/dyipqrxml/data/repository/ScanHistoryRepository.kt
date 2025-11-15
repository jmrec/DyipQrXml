package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.ScanHistory
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    fun observeHistory(userId: Long?): Flow<List<ScanHistory>>
    suspend fun saveScan(content: String, userId: Long? = null)
}

