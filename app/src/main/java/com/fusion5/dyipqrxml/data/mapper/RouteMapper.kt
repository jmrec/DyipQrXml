package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.model.Route

fun RouteEntity.toDomain(): Route = Route(
    id = id,
    terminalId = terminalId,
    name = name,
    destination = destination,
    fare = fare,
    estimatedTime = estimatedTime,
    frequency = frequency,
    createdAt = createdAt
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    id = id,
    terminalId = terminalId,
    name = name,
    destination = destination,
    fare = fare,
    estimatedTime = estimatedTime,
    frequency = frequency,
    createdAt = createdAt
)
