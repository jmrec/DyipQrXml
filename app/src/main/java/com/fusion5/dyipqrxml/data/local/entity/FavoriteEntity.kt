package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val terminalId: Long?,
    val routeId: Long?,
    val createdAt: Long = System.currentTimeMillis()
)

