package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.TerminalEntity
import com.fusion5.dyipqrxml.data.model.Terminal

fun TerminalEntity.toDomain() = Terminal(
    id = id,
    name = name,
    description = description,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    updatedAt = updatedAt
)
