package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.ScanHistoryEntity
import com.fusion5.dyipqrxml.data.model.ScanHistory

fun ScanHistoryEntity.toDomain() = ScanHistory(
    id = id,
    userId = userId,
    content = content,
    scannedAt = scannedAt
)

