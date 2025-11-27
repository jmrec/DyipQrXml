package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.RouteEntity
import com.fusion5.dyipqrxml.data.local.entity.RouteWithTerminals
import com.fusion5.dyipqrxml.data.model.Route

fun RouteWithTerminals.toDomain(): Route = Route(
    id = route.id,
    terminalIds = terminals.map { it.id },
    name = route.name,
    destination = route.destination,
    fare = route.fare,
    estimatedTime = route.estimatedTime,
    frequency = route.frequency,
    routeGeoJson = route.routeGeoJson,
    createdAt = route.createdAt
)

fun Route.toEntity(): RouteEntity = RouteEntity(
    id = id,
    name = name,
    destination = destination,
    fare = fare,
    estimatedTime = estimatedTime,
    frequency = frequency,
    routeGeoJson = routeGeoJson,
    createdAt = createdAt
)
