package com.fusion5.dyipqrxml.data.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessionUserId: Flow<Long?>
    suspend fun saveUserId(userId: Long)
    suspend fun clear()
}

