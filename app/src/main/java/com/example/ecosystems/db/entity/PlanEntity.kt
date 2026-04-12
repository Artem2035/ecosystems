package com.example.ecosystems.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: Int,

    val uuid: String,
    val name: String,
    val description: String?,

    val accessType: Int,
    val categoryId: Int?,
    val userId: Int,

    val isOwner: Boolean,
    val canEdit: Boolean,

    val accountIds: String,
    val accounts: String,

    val createdAt: Long,
    val updatedAt: Long
)
