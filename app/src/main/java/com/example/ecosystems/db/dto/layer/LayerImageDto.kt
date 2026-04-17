package com.example.ecosystems.db.dto.layer

import com.example.ecosystems.db.dto.formatter
import com.example.ecosystems.db.entity.layer.LayerImageEntity

data class LayerImageDto(
    val id: Int,
    val uuid: String,
    val filename: String,
    val original_filename: String,
    val gis_object_layer_id: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val description: String?,
    val created_at: String,
    val updated_at: String
)

fun LayerImageDto.toEntity(): LayerImageEntity {
    return LayerImageEntity(
        id = id,
        uuid = uuid,
        filename = filename,
        originalFilename = original_filename,
        gisObjectLayerId = gis_object_layer_id,
        lat = lat,
        lng = lng,
        num = num,
        description = description,
        createdAt = formatter.parse(created_at)?.time ?: 0L,
        updatedAt = formatter.parse(updated_at)?.time ?: 0L,
        localPath = null,
        lastAccessedAt = null
    )
}