package com.example.ecosystems.db.entity.syncQueue

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: SyncEntityType,  // "point" | "point_value" | "plan" | "layer" | ...
    val entityId: Int,       // id сущности в локальной БД
    val operation: SyncOperation,   // "UPSERT" | "DELETE"
    val extraData: String? = null,   // JSON с доп. данными
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)