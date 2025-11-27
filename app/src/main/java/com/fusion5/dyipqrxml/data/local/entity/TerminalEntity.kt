package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Terminals",
    indices = [
        Index(value = ["name"], name = "idx_name"),
        Index(value = ["latitude", "longitude"], unique = true, name = "uk_coordinates")
    ]
)
data class TerminalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "created_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val createdAt: String,

    @ColumnInfo(name = "updated_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val updatedAt: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double
)
