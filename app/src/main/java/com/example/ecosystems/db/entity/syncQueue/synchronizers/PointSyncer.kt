package com.example.ecosystems.db.entity.syncQueue.synchronizers

import android.util.Log
import com.example.ecosystems.DataClasses.CreatePointRequest
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.entity.layer.toCreateRequest
import com.example.ecosystems.db.entity.layer.toUpdateRequest
import com.example.ecosystems.db.entity.syncQueue.EntitySyncer
import com.example.ecosystems.db.entity.syncQueue.SyncEntityType
import com.example.ecosystems.db.entity.syncQueue.SyncOperation
import com.example.ecosystems.db.entity.syncQueue.SyncQueueEntity
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.network.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.flow.firstOrNull

class PointSyncer(
    private val layerDao: LayerEntityDao,
    private val planDao: PlanEntityDao,
    private val api: ApiService,
    private val token: String,
    private val layerRepository: LayerRepository
) : EntitySyncer {

    // Накапливаем layerId, которые изменились в этой сессии синхронизации
    private val _affectedLayerIds = mutableSetOf<Int>()
    override fun getAffectedLayerIds(): Set<Int> = _affectedLayerIds.toSet()

    override val entityType = SyncEntityType.POINT
    // Разрешаем batch только для CREATE
    override fun canBatch(operation: SyncOperation) = operation == SyncOperation.CREATE

    override suspend fun syncBatch(items: List<SyncQueueEntity>): Map<Int, Boolean> {
        _affectedLayerIds.clear()
        // items — все CREATE-записи точек
        // Собираем данные по каждой точке
        val results = mutableMapOf<Int, Boolean>()
        //val gson = Gson()

        data class PointBundle(
            val queueItem: SyncQueueEntity,
            val request: CreatePointRequest,
            val layerId: Int,
            val layerUUID: String
        )
        Log.d("PointSyncer 2","${items}")
        val bundles = mutableListOf<PointBundle>()

        for (item in items) {
            val point = layerDao.getPointById(item.entityId)
            if (point == null) {
                results[item.entityId] = true // точки нет — пропускаем
                continue
            }
            val layer = layerDao.getLayerUUIDById(point.layerId)

            // Получаем uuid плана через gisObjectId слоя
            val planUUID = planDao.getPlanUuidById(layer.gisObjectId)
            if (planUUID == null) {
                Log.e("PointSyncer", "planUUID не найден для layerId=${point.layerId}")
                results[item.entityId] = false
                continue
            }

            // Собираем значения точки для отправки
            val pointValues = layerDao.getPointWithValuesByPointId(point.id)?.firstOrNull()
            val valuesMap: Map<String, Any?> = pointValues
                ?.firstOrNull()
                ?.values
                ?.associate { pv -> pv.property.name to pv.value.value }
                ?: emptyMap()

            bundles.add(
                PointBundle(
                    queueItem = item,
                    request = point.toCreateRequest(layer.uuid, valuesMap),
                    layerId = point.layerId,
                    layerUUID = layer.uuid
                )
            )
        }

        val byLayer = bundles.groupBy { it.layerId }

        for ((layerId, layerBundles) in byLayer) {
            val layerUUID = layerBundles.first().layerUUID
            val requests = layerBundles.map { it.request }

            try {
                Log.d("PointSyncer 2","${requests}")
                // POST — отправляем все новые точки слоя одним запросом
                api.savePointsBatch(token, layerUUID, requests)
                _affectedLayerIds.add(layerId)

                layerBundles.forEach { results[it.queueItem.entityId] = true }
            } catch (e: Exception) {
                Log.e("PointSyncer", "Batch failed для layerId=$layerId", e)
                layerBundles.forEach { results[it.queueItem.entityId] = false }
            }
        }

        return results
    }

    override suspend fun sync(item: SyncQueueEntity): Boolean {
        Log.d("Sync","${item}")
        return when (item.operation) {
            SyncOperation.CREATE -> {
                val point = layerDao.getPointById(item.entityId) ?: return false
                val layer = layerDao.getLayerUUIDById(point.layerId)
                val pointValues = layerDao.getPointWithValuesByPointId(point.id)?.firstOrNull()
                val pointValuesMap: Map<String, Any?> = pointValues?.firstOrNull()
                        ?.values
                        ?.associate { pv ->
                            pv.property.name to pv.value.value
                        }
                        ?: emptyMap()
                val serverId = api.saveNewPoint(token, point.toCreateRequest(layer.uuid, pointValuesMap)) // POST
                layerDao.setPointServerId(item.entityId, serverId)
                layerRepository.refreshValuesJson(point.id)
                return true
            }
            SyncOperation.UPDATE -> {
                val point = layerDao.getPointById(item.entityId) ?: return false
                val layer = layerDao.getLayerUUIDById(point.layerId)
                val pointValues = layerDao.getPointWithValuesByPointId(point.id)?.firstOrNull()
                val pointValuesMap: Map<String, Any?> = pointValues?.firstOrNull()
                    ?.values
                    ?.associate { pv ->
                        pv.property.name to pv.value.value
                    }
                    ?: emptyMap()
                val updateRequest = point.toUpdateRequest(layer.uuid, pointValuesMap)
                if(updateRequest == null) {
                    Log.e("Sync", "UPDATE: serverId == null для локального id=${item.entityId}")
                    return false
                }
                api.savePointChanges(token, updateRequest,  point.valuesJson)
                layerRepository.refreshValuesJson(point.id)
                return true
            }
            SyncOperation.DELETE -> {
                val extra = item.extraData
                    ?.let { Gson().fromJson(it, Map::class.java) }

                val layerUUID = extra?.get("layer_uuid") as? String ?: return false
                val serverId = (extra["server_id"] as? Double)?.toInt() ?: return false

                api.deletePoint(token, layerUUID, serverId)
                return true
            }
        }
    }
}

