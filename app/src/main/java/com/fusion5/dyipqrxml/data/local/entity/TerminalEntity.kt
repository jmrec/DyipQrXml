package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminals")
data class TerminalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val latitude: Double?,
    val longitude: Double?
)

