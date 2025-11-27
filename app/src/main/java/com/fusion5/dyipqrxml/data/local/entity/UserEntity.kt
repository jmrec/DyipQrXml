package com.fusion5.dyipqrxml.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "created_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val createdAt: String,

    @ColumnInfo(name = "updated_at", defaultValue = "(CURRENT_TIMESTAMP)")
    val updatedAt: String,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "email")
    val email: String,

    // Not in the schema provided but required for the app login logic
    @ColumnInfo(name = "password_hash")
    val passwordHash: String
)
