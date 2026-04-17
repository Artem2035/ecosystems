package com.example.ecosystems.db.dto.layer

import com.example.ecosystems.db.dto.formatter
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.google.gson.Gson

data class LayerDto(
    val id: Int,
    val uuid: String,
    val gis_object_id: Int,
    val gis_object_file_id: Int?,
    val name: String,
    val color: String?,
    val type: String,
    val order: Int?,
    val parent_id: Int?,
    val table_id: Int?,
    val created_at: String,
    val updated_at: String,
    val crop_enabled: Boolean,
    val crop_percent: Double,
    val data: LayerDataDto
)

fun LayerDto.toEntity(): LayerEntity {
    val gson = Gson()

    return LayerEntity(
        id = id,
        uuid = uuid,
        gisObjectId = gis_object_id,
        gisObjectFileId = gis_object_file_id,
        name = name,
        color = color,
        type = type,
        order = order,
        parentId = parent_id,
        tableId = if (table_id == 0) null else table_id,
        createdAt = formatter.parse(created_at)?.time ?: 0L,
        updatedAt = formatter.parse(updated_at)?.time ?: 0L,
        dataJson = gson.toJson(data.gis_file),
        cropEnabled = crop_enabled,
        cropPercent = crop_percent,
        syncedAt = System.currentTimeMillis()
    )
}