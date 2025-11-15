package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long?,
    val content: String,
    val scannedAt: Long = System.currentTimeMillis()
)

