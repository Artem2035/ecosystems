package com.example.ecosystems.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ecosystems.db.entity.syncQueue.SyncEntityType
import com.example.ecosystems.db.entity.syncQueue.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    // Берём только несинхронизированные, по порядку создания
    @Query("SELECT * FROM sync_queue WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)

    // Очищаем уже отправленные — можно вызывать периодически
    @Query("DELETE FROM sync_queue WHERE isSynced = 1")
    suspend fun clearSynced()

    // Дедупликация:
    // Это важно: если пользователь 5 раз отредактировал точку офлайн,
    // отправляем только один финальный запрос
    // Дедупликация для простых ключей (POINT, PLAN, LAYER)
    // Для POINT и других простых ключей — удаляем только UPDATE, CREATE не трогаем
    @Query("""
        DELETE FROM sync_queue 
        WHERE entityType = :entityType 
        AND entityId = :entityId 
        AND operation = 'UPDATE' 
        AND isSynced = 0
    """)
    suspend fun removePendingUpdate(entityType: SyncEntityType, entityId: Int)

    // Проверяем есть ли незакрытый CREATE для этой сущности
    @Query("""
        SELECT COUNT(*) > 0 FROM sync_queue 
        WHERE entityType = :entityType 
        AND entityId = :entityId 
        AND operation = 'CREATE' 
        AND isSynced = 0
    """)
    suspend fun hasCreate(entityType: SyncEntityType, entityId: Int): Boolean
    //очистка очереди, если объект был удален
    @Query("""
        DELETE FROM sync_queue 
        WHERE entityType = :entityType 
        AND entityId = :entityId 
        AND isSynced = 0
    """)
    suspend fun removeAllPending(entityType: SyncEntityType, entityId: Int)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}