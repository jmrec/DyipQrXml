package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.dao.RouteDao
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.Route
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalRouteRepository(
    private val routeDao: RouteDao
) : RouteRepository {
    override fun observeAll(): Flow<List<Route>> =
        routeDao.observeAllRoutesWithTerminals().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Route? =
        routeDao.getRouteWithTerminalsById(id)?.toDomain()

    override fun search(query: String): Flow<List<Route>> =
        routeDao.searchRoutesWithTerminals(query).map { list -> list.map { it.toDomain() } }
    
    // Kept for backward compatibility if needed, but filtering now happens in memory or via specific query if needed
    fun observeRoutesByTerminal(terminalId: Long): Flow<List<Route>> =
        observeAll().map { routes -> 
            routes.filter { it.terminalIds.contains(terminalId) } 
        }
}
