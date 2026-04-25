package com.example.ecosystems.DataClasses

// Тело запроса для новой точки
data class CreatePointRequest(
    val id: String = "",           // пустая строка для новой точки
    val num: Int,
    val lat: Double,
    val lng: Double,
    val values: Map<String, Any?> = emptyMap(),
    val layer_uuid: String
)

// Тело запроса для существующей точки
data class UpdatePointRequest(
    val id: Int,
    val num: Int,
    val lat: Double,
    val lng: Double,
    val values: Map<String, Any?> = emptyMap(),
    val layer_uuid: String,
    val layer_id: Int,
    val created_at: String,
    val updated_at: String
)