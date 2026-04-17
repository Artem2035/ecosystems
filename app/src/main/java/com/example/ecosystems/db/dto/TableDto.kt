package com.example.ecosystems.db.dto

import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity

val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
    java.util.Locale.ENGLISH)

data class TableDto(
    val id: Int,
    val uuid: String,
    val name: String,
    val description: String?,
    val is_user_table: Boolean,
    val user_id: Int?,
    val created_at: String,
    val updated_at: String,
    val properties: List<TablePropertyDto>?
)

data class TablePropertyDto(
    val id: Int,
    val table_id: Int,
    val name: String,
    val display_name: String?,
    val property_type: String,
    val units: String?,
    val sort_order: Int?,
    val enum_values: String? = null,
    val description: String?,
    val created_at: String,
    val updated_at: String
)

fun TableDto.toEntity(): TableEntity {
    return TableEntity(
        id = id,
        uuid = uuid,
        name = name,
        description = description,
        isUserTable = is_user_table,
        userId = user_id,
        createdAt = formatter.parse(created_at)?.time ?: 0L,
        updatedAt = formatter.parse(updated_at)?.time ?: 0L
    )
}

fun TablePropertyDto.toEntity(): TablePropertyEntity {
    return TablePropertyEntity(
        id = id,
        tableId = table_id,
        name = name,
        displayName = display_name,
        propertyType = property_type,
        units = units ?: "",
        sortOrder = sort_order ?: 0,
        enumValues = enum_values,
        description = description,
        createdAt = formatter.parse(created_at)?.time ?: 0L,
        updatedAt = formatter.parse(updated_at)?.time ?: 0L
    )
}