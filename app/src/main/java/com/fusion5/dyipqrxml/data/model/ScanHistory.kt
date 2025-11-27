package com.fusion5.dyipqrxml.data.model

data class ScanHistory(
    val id: Long,
    val userId: Long,
    val content: String?,
    val createdAt: String,
    val updatedAt: String
)
