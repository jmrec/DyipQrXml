package com.fusion5.dyipqrxml.data.repository

import com.fusion5.dyipqrxml.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signup(fullName: String, email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
}

