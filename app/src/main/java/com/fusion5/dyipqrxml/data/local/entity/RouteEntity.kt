package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String,
    val fare: Double,
    val estimatedTime: String,
    val frequency: String,
    val routeGeoJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
