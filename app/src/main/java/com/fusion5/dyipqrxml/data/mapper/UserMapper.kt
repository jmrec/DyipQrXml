package com.fusion5.dyipqrxml.data.mapper

import com.fusion5.dyipqrxml.data.local.entity.UserEntity
import com.fusion5.dyipqrxml.data.model.User

fun UserEntity.toDomain() = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    createdAt = createdAt,
    updatedAt = updatedAt
)
