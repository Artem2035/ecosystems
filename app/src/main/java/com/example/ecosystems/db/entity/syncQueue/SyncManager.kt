package com.example.ecosystems.db.entity.syncQueue

import android.util.Log
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.dao.SyncQueueDao
import com.example.ecosystems.db.dto.layer.LayerPointDto
import com.example.ecosystems.db.dto.layer.toEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.syncQueue.synchronizers.PointSyncer
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.TableRepository
import com.example.ecosystems.network.ApiService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class SyncManager(
    private val tableRepository: TableRepository,
    private val layerDao: LayerEntityDao,
    private val planDao: PlanEntityDao,
    private val api: ApiService,
    private val token: String,
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

        // Группируем: сначала список (CREATE), потом одиночные (UPDATE/DELETE)
        // Разбиваем на группы [entityType + operation] для batch
        // и отдельный список для одиночных
        val (batchable, singles) = pending.partition { item ->
            val syncer = syncers[item.entityType]
            syncer?.canBatch(item.operation) == true
        }

        //группируем по [entityType + operation] ---
        val batchGroups = batchable.groupBy { it.entityType to it.operation }
        for ((typeOp, items) in batchGroups) {
            val syncer = syncers[typeOp.first] ?: continue

            val results = try {
                Log.d("PointSyncer 1","${items}")
                syncer.syncBatch(items)
            } catch (e: Exception) {
                Log.e("SyncManager", "Batch sync failed for ${typeOp}", e)
                errors.add("${typeOp}: ${e.message}")
                // все считаем провалившимися
                items.associate { it.entityId to false }
            }

            for (item in items) {
                val success = results[item.entityId] ?: false
                if (success) syncQueueDao.markSynced(item.id)
                else failedCount++
            }
            current += items.size
            _progress.emit(SyncProgress(
                current = current,
                total = total,
                label = "${items.size}"
            ))
        }

        for (item in singles) {
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

        // Собираем все затронутые слои со всех синхронизаторов
        val allAffectedLayerIds = syncers.values
            .flatMap { it.getAffectedLayerIds() }
            .toSet()
        Log.d("PointSyncer", "${allAffectedLayerIds}")
        if (allAffectedLayerIds.isNotEmpty() && failedCount == 0) {
            try {
                refetchLayers(allAffectedLayerIds)
            } catch (e: Exception) {
                Log.e("SyncManager", "Re-fetch failed", e)
                // Не считаем это ошибкой синхронизации — данные на сервере актуальны
            }
        }

        if (failedCount == 0) {
            syncQueueDao.clearSynced()
            SyncResult.Success
        } else {
            SyncResult.PartialFailure(failedCount, errors)
        }
    }

    // Перезагрузка конкретных слоёв с сервера
    private suspend fun refetchLayers(layerIds: Set<Int>) {
        val gson = Gson()
        for (layerId in layerIds) {
            val layer = layerDao.getLayerUUIDById(layerId)
            val planUUID = planDao.getPlanUuidById(layer.gisObjectId) ?: continue

            val responseJson = api.loadPlanLayers(token, planUUID)
            val root = gson.fromJson(responseJson, Map::class.java) as Map<String, Any?>
            val layers = root["layers"] as? List<Map<String, Any?>> ?: continue

            val serverLayer = layers.firstOrNull { it["uuid"] == layer.uuid } ?: continue
            val data = serverLayer["data"] as? Map<String, Any?> ?: continue

            val points = gson.fromJson<List<LayerPointDto>>(
                gson.toJson(data["points"]),
                object : TypeToken<List<LayerPointDto>>() {}.type
            ) ?: emptyList()

            val pointValuesEntities = mutableListOf<PointValueEntity>()
            val pointsEntities = mutableListOf<LayerPointEntity>()

            points.forEach { point ->
                val values = point.values
                if (!values.isNullOrEmpty() && layer.tableId != null) {
                    values.forEach { (key, value) ->
                        pointValuesEntities.add(
                            PointValueEntity(
                                pointId = point.id,
                                propertyId = tableRepository.getTablePropertyIdByName(layer.tableId, key),
                                value = value.toString()
                            )
                        )
                    }
                }
                pointsEntities.add(point.toEntity())
            }

            // Транзакционная перезапись
            layerDao.deletePointsByLayerId(layerId)
            layerDao.insertPoints(pointsEntities)
            layerDao.savePointValues(pointValuesEntities)

            Log.d("SyncManager", "Re-fetched layerId=$layerId, points=${pointsEntities.size}")
        }
    }
}

fun buildSyncManager(
    layerRepository: LayerRepository,
    tableRepository: TableRepository,
    syncQueueDao: SyncQueueDao,
    layerDao: LayerEntityDao,
    planDao: PlanEntityDao,
    api: ApiService,
    token: String
): SyncManager {

    val syncers = listOf(
        PointSyncer(layerDao, planDao, api, token, layerRepository)
    ).associateBy { it.entityType }

    return SyncManager(tableRepository,layerDao, planDao, api, token, syncQueueDao, syncers)
}