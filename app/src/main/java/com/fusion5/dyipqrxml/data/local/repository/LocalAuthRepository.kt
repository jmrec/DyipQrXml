package com.fusion5.dyipqrxml.data.local.repository

import com.fusion5.dyipqrxml.data.local.DyipQrDatabase
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import com.fusion5.dyipqrxml.data.mapper.toDomain
import com.fusion5.dyipqrxml.data.model.User
import com.fusion5.dyipqrxml.data.repository.AuthRepository
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import com.fusion5.dyipqrxml.util.PasswordHasher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalAuthRepository(
    private val database: DyipQrDatabase,
    private val sessionRepository: SessionRepository,
    private val passwordHasher: PasswordHasher
) : AuthRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
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
        
        // Simple splitting logic
        val parts = fullName.trim().split("\\s+".toRegex())
        val firstName = if (parts.isNotEmpty()) parts.first() else ""
        val lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
        
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val entity = UserEntity(
            firstName = firstName,
            lastName = lastName,
            email = email,
            passwordHash = hash,
            createdAt = currentTime,
            updatedAt = currentTime
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
