package com.example.ecosystems.db.dto.layer

data class LayerDataDto(
    val crop_enabled: Boolean,
    val crop_percent: Double,
    val gis_file: Map<String, Any?> = emptyMap(),

    //val images: List<LayerImageDto> = emptyList(),
    val points: List<LayerPointDto> = emptyList(),

    //val shape_objects: List<Any> = emptyList(),
    //val shooting_objects: List<Any> = emptyList(),
    //val projective_coverage: List<Any> = emptyList(),

    val type: String
)