package com.example.ecosystems.db.entity.layer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ecosystems.db.entity.table.TableEntity

@Entity(tableName = "layers",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("tableId")])
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

