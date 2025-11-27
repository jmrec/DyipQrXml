package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class RouteWithTerminals(
    @Embedded val route: RouteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RouteTerminalCrossRef::class,
            parentColumn = "routeId",
            entityColumn = "terminalId"
        )
    )
    val terminals: List<TerminalEntity>
)
