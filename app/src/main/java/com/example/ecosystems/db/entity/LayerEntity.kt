package com.example.ecosystems.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey val id: Int,
    val uuid: String,
    val gisObjectId: Int,
    val gisObjectFileId: Int?,
    val name: String,
    val color: String?,
    val type: String,
    val order: Int?,
    val parentId: Int?,
    val tableId: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val dataJson: String,
    val cropEnabled: Boolean,
    val cropPercent: Double,
    val syncedAt: Long //локальное поле
)