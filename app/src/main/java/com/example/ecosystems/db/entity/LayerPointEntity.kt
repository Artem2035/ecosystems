package com.example.ecosystems.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(tableName = "layer_points",
    foreignKeys = [ForeignKey(
        entity = LayerEntity::class,
        parentColumns = ["id"],        // LayerEntity.id
        childColumns = ["layerId"],  // ImageEntity.gisObjectLayerId
        onDelete = ForeignKey.CASCADE, // удалить layer → удалятся все images
        onUpdate = ForeignKey.CASCADE  // обновить id → обновится везде
    )],)
data class LayerPointEntity(
    @PrimaryKey val id: Int,
    val layerId: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val valuesJson: String,   // values: {} сериализуется в JSON-строку через TypeConverter
    val createdAt: Long,
    val updatedAt: Long
)