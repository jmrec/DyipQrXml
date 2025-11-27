package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RouteWithTerminals(
    @Embedded val route: RouteEntity,
    
    @Relation(
        parentColumn = "start_terminal_id",
        entityColumn = "id"
    )
    val startTerminal: TerminalEntity,

    @Relation(
        parentColumn = "end_terminal_id",
        entityColumn = "id"
    )
    val endTerminal: TerminalEntity
)
