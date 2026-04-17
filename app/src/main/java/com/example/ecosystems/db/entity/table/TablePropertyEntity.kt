package com.example.ecosystems.db.entity.table

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "table_properties",
    foreignKeys = [ForeignKey(
        entity = TableEntity::class,
        parentColumns = ["id"],
        childColumns = ["tableId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("tableId")]
)
data class TablePropertyEntity(
    @PrimaryKey val id: Int,
    val tableId: Int,
    val name: String,            // внутреннее имя "Вид дерева"
    val displayName: String?,     // отображаемое "Вид"
    val propertyType: String,
    val units: String,
    val sortOrder: Int,
    val enumValues: String?,     // JSON-массив если propertyType == ENUM
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)