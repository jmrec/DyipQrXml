package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun observeRoutesByTerminal(terminalId: Long): Flow<List<Route>>
    suspend fun getById(id: Long): Route?
}
