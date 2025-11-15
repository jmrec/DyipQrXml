package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.FavoriteEntity
import com.fusion5.dyipqrxml.data.model.Favorite

fun FavoriteEntity.toDomain() = Favorite(
    id = id,
    userId = userId,
    terminalId = terminalId,
    routeId = routeId,
    createdAt = createdAt
)

