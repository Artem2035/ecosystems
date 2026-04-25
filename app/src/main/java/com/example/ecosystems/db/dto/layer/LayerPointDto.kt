package com.example.ecosystems.db.dto.layer

import com.example.ecosystems.db.dto.formatter
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.google.gson.Gson

data class LayerPointDto(
    val id: Int,
    val layer_id: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val values: Map<String, Any?>?,
    val created_at: String,
    val updated_at: String
)

fun LayerPointDto.toEntity(): LayerPointEntity {
    val gson = Gson()
    return LayerPointEntity(
        id = id,
        layerId = layer_id,
        lat = lat,
        lng = lng,
        num = num,
        valuesJson = gson.toJson(values),
        createdAt = formatter.parse(created_at)?.time ?: 0L,
        updatedAt = formatter.parse(updated_at)?.time ?: 0L,
        serverId = id //при загрузке с сервера считаем что id и serverId равны
    )
}
