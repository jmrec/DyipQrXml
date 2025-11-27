package com.fusion5.dyipqrxml.data.model

data class Route(
    val id: Long,
    val startTerminalId: Long,
    val endTerminalId: Long,
    val routeCode: String,
    val fare: Double,
    val estimatedTravelTimeInSeconds: Long?,
    val frequency: Int,
    val routeGeoJson: String,
    val createdAt: String,
    val updatedAt: String,
    val startTerminalName: String = "",
    val endTerminalName: String = ""
)
