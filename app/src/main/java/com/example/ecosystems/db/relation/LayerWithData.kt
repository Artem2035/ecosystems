package com.example.ecosystems.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ecosystems.db.entity.LayerEntity
import com.example.ecosystems.db.entity.LayerImageEntity
import com.example.ecosystems.db.entity.LayerPointEntity

data class LayerWithData(
    @Embedded val layer: LayerEntity,
    @Relation(parentColumn = "id", entityColumn = "gisObjectLayerId")
    val images: List<LayerImageEntity>,
    @Relation(parentColumn = "id", entityColumn = "layerId")
    val points: List<LayerPointEntity>
)
