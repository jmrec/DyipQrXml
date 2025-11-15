package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = TerminalEntity::class,
            parentColumns = ["id"],
            childColumns = ["terminalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["terminalId"])]
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val terminalId: Long,
    val name: String,
    val destination: String,
    val fare: Double,
    val estimatedTime: String,
    val frequency: String,
    val createdAt: Long = System.currentTimeMillis()
)
