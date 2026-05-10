package com.example.ecosystems.DataClasses

import com.example.ecosystems.db.entity.layer.LayerEntity

data class LayerDisplayItem(
    val layer: LayerEntity,
    var isVisible: Boolean = true
)