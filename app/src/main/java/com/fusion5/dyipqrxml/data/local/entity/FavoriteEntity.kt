package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Favorites",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"], name = "idx_favorites_user_id"),
        Index(value = ["route_id"], name = "idx_favorites_route_id"),
        Index(value = ["user_id", "route_id"], unique = true, name = "uk_favorites_unique_route")
    ]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "created_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val createdAt: String,

    @ColumnInfo(name = "updated_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val updatedAt: String,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "route_id")
    val routeId: Long
)
