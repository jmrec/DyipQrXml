package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "route_terminal_cross_ref",
    primaryKeys = ["routeId", "terminalId"],
    indices = [Index(value = ["terminalId"]), Index(value = ["routeId"])]
)
data class RouteTerminalCrossRef(
    val routeId: Long,
    val terminalId: Long
)
