package com.fusion5.dyipqrxml.data.model

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val createdAt: String,
    val updatedAt: String
) {
    val fullName: String
        get() = "$firstName $lastName"
}
