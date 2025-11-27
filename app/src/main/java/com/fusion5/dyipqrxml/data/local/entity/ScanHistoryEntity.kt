package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ScanHistories",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"], name = "idx_scan_histories_user_id")
    ]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "created_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val createdAt: String,

    @ColumnInfo(name = "updated_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val updatedAt: String,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "content")
    val content: String?
)
