package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun observeAll(): Flow<List<Route>>
    suspend fun getById(id: Long): Route?
    fun search(query: String): Flow<List<Route>>
}
