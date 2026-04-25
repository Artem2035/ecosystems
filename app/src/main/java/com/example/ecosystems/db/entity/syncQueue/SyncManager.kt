package com.example.ecosystems.db.entity.syncQueue

import android.util.Log
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.SyncQueueDao
import com.example.ecosystems.db.entity.syncQueue.synchronizers.PointSyncer
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class SyncManager(
    private val syncQueueDao: SyncQueueDao,
    // Реестр синхронизаторов — Map для O(1) поиска по entityType
    private val syncers: Map<SyncEntityType, EntitySyncer>
) {
    sealed class SyncResult {
        object Success : SyncResult()
        data class PartialFailure(val failedCount: Int, val errors: List<String>) : SyncResult()
    }

    // Прогресс: current / total
    data class SyncProgress(val current: Int, val total: Int, val label: String = "")

    // SharedFlow — горячий, не хранит значения, подписчики получают только новые события
    private val _progress = MutableSharedFlow<SyncProgress>(extraBufferCapacity = 64)
    val progress: SharedFlow<SyncProgress> = _progress.asSharedFlow()


    suspend fun syncPendingChanges(): SyncResult = withContext(Dispatchers.IO) {
        val pending = syncQueueDao.getPending()
        if (pending.isEmpty()) return@withContext SyncResult.Success

        val total = pending.size
        var current = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        for (item in pending) {
            val syncer = syncers[item.entityType]

            if (syncer == null) {
                // Неизвестный тип — логируем и пропускаем, не блокируем очередь
                Log.w("SyncManager", "No syncer for entityType=${item.entityType}")
                syncQueueDao.markSynced(item.id)
                current++
                // Эмитируем прогресс даже при пропуске
                _progress.emit(SyncProgress(current, total, "Пропуск: ${item.entityType}"))
                continue
            }

            val success = try {
                syncer.sync(item)
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync failed for ${item.entityType} id=${item.entityId}", e)
                errors.add("${item.entityType}#${item.entityId}: ${e.message}")
                false
            }

            if (success) syncQueueDao.markSynced(item.id)
            else failedCount++

            current++
            // Эмитируем после каждого обработанного элемента
            _progress.emit(SyncProgress(
                current = current,
                total = total,
                label = "${item.entityType} #${item.entityId}"
            ))
        }

        if (failedCount == 0) {
            syncQueueDao.clearSynced()
            SyncResult.Success
        } else {
            SyncResult.PartialFailure(failedCount, errors)
        }
    }
}

fun buildSyncManager(
    layerRepository: LayerRepository,
    syncQueueDao: SyncQueueDao,
    layerDao: LayerEntityDao,
    api: ApiService,
    token: String
): SyncManager {

    val syncers = listOf(
        PointSyncer(layerDao, api, token, layerRepository)
    ).associateBy { it.entityType }

    return SyncManager(syncQueueDao, syncers)
}