package com.fusion5.dyipqrxml.data.repository.local

import com.fusion5.dyipqrxml.data.local.dao.RouteDao
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.Route
import com.fusion5.dyipqrxml.data.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalRouteRepository(
    private val routeDao: RouteDao
) : RouteRepository {
    override fun observeRoutesByTerminal(terminalId: Long): Flow<List<Route>> =
        routeDao.observeRoutesByTerminal(terminalId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Route? =
        routeDao.getById(id)?.toDomain()
}
