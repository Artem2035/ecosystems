package com.example.ecosystems.db.entity.syncQueue.synchronizers

import android.util.Log
import com.example.ecosystems.db.dao.LayerEntityDao
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
    private val api: ApiService,
    private val token: String,
    private val layerRepository: LayerRepository
) : EntitySyncer {

    override val entityType = SyncEntityType.POINT

    override suspend fun sync(item: SyncQueueEntity): Boolean {
        Log.d("Sync","${item}")
        return when (item.operation) {
            SyncOperation.CREATE -> {
                val point = layerDao.getPointById(item.entityId) ?: return true
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
                return true
            }
            SyncOperation.UPDATE -> {
                val point = layerDao.getPointById(item.entityId) ?: return true
                val layer = layerDao.getLayerUUIDById(point.layerId)
                val pointValues = layerDao.getPointWithValuesByPointId(point.id)?.firstOrNull()
                val pointValuesMap: Map<String, Any?> = pointValues?.firstOrNull()
                    ?.values
                    ?.associate { pv ->
                        pv.property.name to pv.value.value
                    }
                    ?: emptyMap()
                val updateRequest = point.toUpdateRequest(layer.uuid, pointValuesMap)
                if(updateRequest == null)
                    return false
                api.savePointChanges(token, updateRequest,  point.valuesJson)
                layerRepository.refreshValuesJson(point.id)
                return true
            }
            SyncOperation.DELETE -> {
                val extra = item.extraData
                    ?.let { Gson().fromJson(it, Map::class.java) }

                val layerUUID = extra?.get("layer_uuid") as? String

                if (layerUUID == null) {
                    Log.e("Sync", "DELETE: нет layer_uuid для точки $item, пропускаем")
                    return false
                }

                Log.d("Sync i", "${extra} ")
                Log.d("Sync i", "${layerUUID} ")
                api.deletePoint(token, layerUUID, item.entityId)
                return true
            }
        }
    }
}

