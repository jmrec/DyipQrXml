package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.DyipQrDatabase
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.User
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import com.fusion5.dyipqrxml.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LocalAuthRepository(
    private val database: DyipQrDatabase,
    private val sessionRepository: SessionRepository,
    private val passwordHasher: PasswordHasher
) : AuthRepository {

    override val currentUser: Flow<User?> = sessionRepository.sessionUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                database.userDao().observeById(userId).map { it?.toDomain() }
            }
        }

    override suspend fun signup(fullName: String, email: String, password: String) {
        val existing = database.userDao().getByEmail(email)
        require(existing == null) { "Email already registered" }
        val hash = passwordHasher.hash(password)
        val entity = UserEntity(
            fullName = fullName,
            email = email,
            passwordHash = hash
        )
        val userId = database.userDao().insert(entity)
        sessionRepository.saveUserId(userId)
    }

    override suspend fun login(email: String, password: String) {
        val entity = database.userDao().getByEmail(email)
            ?: throw IllegalArgumentException("Invalid credentials")
        if (!passwordHasher.verify(password, entity.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }
        sessionRepository.saveUserId(entity.id)
    }

    override suspend fun logout() {
        sessionRepository.clear()
    }

    override suspend fun isLoggedIn(): Boolean = sessionRepository.sessionUserId.first() != null
}
