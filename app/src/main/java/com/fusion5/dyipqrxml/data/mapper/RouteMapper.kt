package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteWithTerminals
import com.fusion5.dyipqrxml.data.model.Route

fun RouteWithTerminals.toDomain(): Route = Route(
    id = route.id,
    startTerminalId = startTerminal.id,
    endTerminalId = endTerminal.id,
    routeCode = route.routeCode,
    fare = route.fare,
    estimatedTravelTimeInSeconds = route.estimatedTravelTimeInSeconds,
    frequency = route.frequency,
    routeGeoJson = route.routeGeoJson,
    createdAt = route.createdAt,
    updatedAt = route.updatedAt,
    startTerminalName = startTerminal.name,
    endTerminalName = endTerminal.name
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    id = id,
    startTerminalId = startTerminalId,
    endTerminalId = endTerminalId,
    routeCode = routeCode,
    fare = fare,
    estimatedTravelTimeInSeconds = estimatedTravelTimeInSeconds,
    frequency = frequency,
    routeGeoJson = routeGeoJson,
    createdAt = createdAt,
    updatedAt = updatedAt
)
