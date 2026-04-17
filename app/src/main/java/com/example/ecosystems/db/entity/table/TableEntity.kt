package com.example.ecosystems.db.entity.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val id: Int,
    val uuid: String,
    val name: String,
    val description: String?,   // "" или null из API
    val isUserTable: Boolean,
    val userId: Int?,            // null у системных таблиц
    val createdAt: Long,
    val updatedAt: Long
)