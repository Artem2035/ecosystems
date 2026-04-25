package com.example.ecosystems.db.entity.syncQueue

interface EntitySyncer {
    val entityType: SyncEntityType
    suspend fun sync(item: SyncQueueEntity): Boolean
}