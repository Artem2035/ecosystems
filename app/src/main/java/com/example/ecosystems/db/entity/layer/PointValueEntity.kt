package com.example.ecosystems.db.entity.layer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.ecosystems.db.entity.table.TablePropertyEntity

@Entity(
    tableName = "point_values",
    primaryKeys = ["pointId", "propertyId"],
    foreignKeys = [
        ForeignKey(
            entity = LayerPointEntity::class,
            parentColumns = ["id"],
            childColumns = ["pointId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TablePropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pointId"), Index("propertyId")]
)
data class PointValueEntity(
    val pointId: Int,
    val propertyId: Int,
    val value: String    // храним строкой
)