package com.example.ecosystems.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ecosystems.db.entity.LayerEntity
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity

data class PlanWithData(
    @Embedded val plan: PlanEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "gisObjectId"
    )
    val files: List<PlanFileEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "gisObjectId"
    )
    val layers: List<LayerEntity>
)