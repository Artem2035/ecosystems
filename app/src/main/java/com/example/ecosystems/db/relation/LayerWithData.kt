package com.example.ecosystems.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity

data class LayerWithData(
    @Embedded val layer: LayerEntity,
    @Relation(parentColumn = "id", entityColumn = "gisObjectLayerId")
    val images: List<LayerImageEntity>,
    @Relation(parentColumn = "id", entityColumn = "layerId")
    val points: List<LayerPointEntity>
)
