	package com.fusion5.dyipqrxml.data.model

data class Favorite(
    val id: Long,
    val userId: Long,
    val terminalId: Long?,
    val routeId: Long?,
    val createdAt: Long
)

