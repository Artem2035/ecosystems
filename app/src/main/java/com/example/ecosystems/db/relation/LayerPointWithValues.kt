package com.example.ecosystems.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity

data class PointValueWithProperty(
    @Embedded val value: PointValueEntity,
    @Relation(
        parentColumn = "propertyId",
        entityColumn = "id",
        entity = TablePropertyEntity::class
    )
    val property: TablePropertyEntity
)

data class LayerPointWithValues(
    @Embedded val point: LayerPointEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "pointId",
        entity = PointValueEntity::class
    )
    val values: List<PointValueWithProperty>
)