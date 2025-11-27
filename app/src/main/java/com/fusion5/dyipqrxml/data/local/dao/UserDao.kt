package com.fusion5.dyipqrxml.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM Users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM Users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM Users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT id FROM Users WHERE email = :email LIMIT 1")
    suspend fun getIdByEmail(email: String): Long?

    @Query("SELECT * FROM Users")
    fun observeAllUsers(): Flow<List<UserEntity>>
}
