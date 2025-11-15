package com.fusion5.dyipqrxml.data.model

data class Route(
    val id: Long,
    val terminalId: Long,
    val name: String,
    val destination: String,
    val fare: Double,
    val estimatedTime: String,
    val frequency: String,
    val createdAt: Long
)
