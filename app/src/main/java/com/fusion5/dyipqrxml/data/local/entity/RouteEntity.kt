package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Routes",
    foreignKeys = [
        ForeignKey(
            entity = TerminalEntity::class,
            parentColumns = ["id"],
            childColumns = ["start_terminal_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TerminalEntity::class,
            parentColumns = ["id"],
            childColumns = ["end_terminal_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["start_terminal_id"], name = "idx_start_terminal_id"),
        Index(value = ["end_terminal_id"], name = "idx_end_terminal_id"),
        Index(value = ["start_terminal_id", "end_terminal_id", "route_code"], unique = true, name = "uk_terminal_routes")
    ]
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "created_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val createdAt: String,

    @ColumnInfo(name = "updated_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val updatedAt: String,

    @ColumnInfo(name = "start_terminal_id")
    val startTerminalId: Long,

    @ColumnInfo(name = "end_terminal_id")
    val endTerminalId: Long,

    @ColumnInfo(name = "route_code")
    val routeCode: String,

    @ColumnInfo(name = "fare")
    val fare: Double,

    @ColumnInfo(name = "estimated_travel_time_in_seconds")
    val estimatedTravelTimeInSeconds: Long?,

    @ColumnInfo(name = "frequency", defaultValue = "0")
    val frequency: Int,

    @ColumnInfo(name = "route_geojson")
    val routeGeoJson: String
)
