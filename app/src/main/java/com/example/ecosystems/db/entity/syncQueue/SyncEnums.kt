package com.example.ecosystems.db.entity.syncQueue

import androidx.room.TypeConverter

enum class SyncEntityType {
    POINT,
    PLAN,
    LAYER
}

class SyncConverters {
    @TypeConverter
    fun fromSyncEntityType(type: SyncEntityType): String = type.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter
    fun fromSyncOperation(op: SyncOperation): String = op.name

    @TypeConverter
    fun toSyncOperation(value: String): SyncOperation = SyncOperation.valueOf(value)
}

enum class SyncOperation { CREATE, UPDATE, DELETE }