package com.example.ecosystems.db.entity.syncQueue

interface EntitySyncer {
    val entityType: SyncEntityType
    suspend fun sync(item: SyncQueueEntity): Boolean

    // batch-синк для группы записей одной операции
    // возвращает Map<localEntityId, успех>
    suspend fun syncBatch(items: List<SyncQueueEntity>): Map<Int, Boolean> {
        // дефолтная реализация — просто поштучно, для синхронизаторов без batch
        return items.associate { it.entityId to sync(it) }
    }

    // могут ли записи этого типа быть сгруппированы
    fun canBatch(operation: SyncOperation): Boolean = false
    fun getAffectedLayerIds(): Set<Int> = emptySet()
}