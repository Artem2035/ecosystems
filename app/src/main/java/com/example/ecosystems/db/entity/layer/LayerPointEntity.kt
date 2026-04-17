package com.example.ecosystems.db.entity.layer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(tableName = "layer_points",
    foreignKeys = [ForeignKey(
        entity = LayerEntity::class,
        parentColumns = ["id"],        // LayerEntity.id
        childColumns = ["layerId"],  // ImageEntity.gisObjectLayerId
        onDelete = ForeignKey.CASCADE, // удалятся все images
        onUpdate = ForeignKey.CASCADE  // обновить id  везде
    )],)
data class LayerPointEntity(
    @PrimaryKey val id: Int,
    val layerId: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val valuesJson: String,
    val createdAt: Long,
    val updatedAt: Long
)