package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.SyncQueueDao
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.syncQueue.SyncEntityType
import com.example.ecosystems.db.entity.syncQueue.SyncOperation
import com.example.ecosystems.db.entity.syncQueue.SyncQueueEntity
import com.google.gson.Gson

class LayerRepository(private val layerDao: LayerEntityDao,
                      private val syncQueueDao: SyncQueueDao) {

    fun getAllLayers() = layerDao.getAll()

    //обновление координат точки
    suspend fun updatePointCoordinates(pointId: Int, lat: Double, lng: Double) {
        val point = layerDao.getPointById(pointId) ?: return

        layerDao.updatePointCoordinates(pointId, lat, lng)

        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, pointId)
        if (isLocalPoint || point.serverId == null) {
            // Точка ещё не на сервере — CREATE заберёт актуальные координаты из Room
            return
        }

        syncQueueDao.removePendingUpdate(SyncEntityType.POINT, pointId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = point.serverId,
                operation = SyncOperation.UPDATE
            )
        )
    }
    //обнолвение номера точки
    suspend fun updatePointNum(pointId: Int, num: Int) {
        layerDao.updatePointNum(pointId, num)

        val point = layerDao.getPointById(pointId) ?: return
        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, point.id)
        if (isLocalPoint || point.serverId == null) {
            // CREATE точки заберёт актуальные значения из Room
            return
        }

        syncQueueDao.removePendingUpdate(SyncEntityType.POINT, pointId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = point.serverId,
                operation = SyncOperation.UPDATE
            )
        )
    }

    suspend fun insertLayers(layers: List<LayerEntity>) {
        layerDao.insertLayers(layers)
    }

    suspend fun clearSyncQueue() {
        syncQueueDao.deleteAll()
    }


    suspend fun insertAllPoints(points: List<LayerPointEntity>) {
        layerDao.insertPoints(points)
    }
    // одиночная вставка точки LayerPointEntity
    suspend fun insertPoint(point: LayerPointEntity): Long {
        //layerDao.insertPoint(point)
        val newId = layerDao.insertPoint(point)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = newId.toInt(),
                operation = SyncOperation.CREATE
            )
        )
        return newId
    }

    //массовая вставка данных
    suspend fun insertAllData(
        layers: List<LayerEntity>,
        points: List<LayerPointEntity>,
        images: List<LayerImageEntity>,
        pointsValues: List<PointValueEntity>
    ) {
        layerDao.insertAllData(layers, points, images, pointsValues)
    }

    suspend fun insertAllImages(images: List<LayerImageEntity>) {
        layerDao.insertImages(images)
    }
    //удалить точку с pointId
    suspend fun deletePoint(pointId: Int) {
        // Если точка никогда не была на сервере — просто чистим всю очередь по ней
        // Если точка есть на сервере — убираем все ожидающие операции и ставим DELETE
        val point = layerDao.getPointById(pointId) ?: return

        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, pointId)

        syncQueueDao.removeAllPending(SyncEntityType.POINT, pointId)

        if (!isLocalPoint && point.serverId != null) {
            val layer = layerDao.getLayerUUIDById(point.layerId)
            val extra = Gson().toJson(mapOf("layer_uuid" to layer.uuid))

            syncQueueDao.enqueue(
                SyncQueueEntity(
                    entityType = SyncEntityType.POINT,
                    entityId = point.serverId,
                    operation = SyncOperation.DELETE,
                    extraData = extra
                )
            )
        }
        layerDao.deletePoint(pointId)
    }
    //удалить незаполненное значение точки
    suspend fun deletePointValue(pointId: Int, propertyId: Int) {
        val point = layerDao.getPointById(pointId) ?: return
        layerDao.deletePointValue(pointId, propertyId)

        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, pointId)
        if (isLocalPoint || point.serverId == null) {
            // Точка локальная — CREATE заберёт актуальное состояние без этого значения
            refreshValuesJson(point.id)
            return
        }

        // Значения изменились — ставим UPDATE точки целиком
        // дедуплицируем: если уже есть UPDATE точки — заменяем
        syncQueueDao.removePendingUpdate(SyncEntityType.POINT, pointId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = point.serverId,
                operation = SyncOperation.UPDATE
            )
        )
    }

    suspend fun deletePointValues(pointId: Int, propertyIds: List<Int>) {

        val point = layerDao.getPointById(pointId) ?: return
        layerDao.deletePointValues(pointId, propertyIds)

        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, pointId)
        if (isLocalPoint || point.serverId == null) {
            // Точка локальная — CREATE заберёт актуальное состояние без этого значения
            refreshValuesJson(point.id)
            return
        }

        // Значения изменились — ставим UPDATE точки целиком
        // дедуплицируем: если уже есть UPDATE точки — заменяем
        syncQueueDao.removePendingUpdate(SyncEntityType.POINT, pointId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = point.serverId,
                operation = SyncOperation.UPDATE
            )
        )
    }

    //сохранить все значения точки (используется в PointDataDialogFragment)
    suspend fun savePointValues(values: List<PointValueEntity>) {
        if (values.isEmpty()) return

        val point = layerDao.getPointById(values[0].pointId) ?: return
        val isLocalPoint = syncQueueDao.hasCreate(SyncEntityType.POINT, point.id)
        if (isLocalPoint || point.serverId == null) {
            // CREATE точки заберёт актуальные значения из Room
            refreshValuesJson(point.id)
            return
        }

        layerDao.savePointValues(values)

        // Ставим один UPDATE на точку — независимо от количества изменённых значений
        syncQueueDao.removePendingUpdate(SyncEntityType.POINT, point.id)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityType = SyncEntityType.POINT,
                entityId = point.serverId,
                operation = SyncOperation.UPDATE
            )
        )
    }

    fun getAllLayersWithData() = layerDao.getAllLayersWithData()

    suspend fun getLayerWithData(layerId: Int) = layerDao.getLayerWithData(layerId)

    fun getPointsWithValues(layerId: Int) = layerDao.getPointsWithValues(layerId)

    fun getAllPointsWithValues() = layerDao.getAllPointsWithValues()

    //получить все точки LayerPointEntity для слоя с layerId
    suspend fun getPointsByLayerId(layerId: Int) = layerDao.getPointsByLayerId(layerId)

    suspend fun getImagesByLayerId(layerId: Int) = layerDao.getImagesByLayerId(layerId)

    fun getAllLayerPoints() = layerDao.getAllLayerPoints()

    fun getAllPointsRaw() = layerDao.getAllPointsRaw()

    //получить значения для точки с данным id
    fun getPointWithValuesByPointId(pointId: Int) = layerDao.getPointWithValuesByPointId(pointId)

    //получить все точки со значениями для слоя с таким id
    fun getPointsWithValuesByLayerId(layerId: Int) = layerDao.getPointsWithValuesByLayerId(layerId)

    //получить id таблицы по id слоя
    fun getTableIdByLayerId(layerId: Int) = layerDao.getTableIdByLayerId(layerId)

    //получить id слоя по известному id точки
    suspend fun getLayerIdByPointId(pointId: Int) = layerDao.getLayerIdByPointId(pointId)

    // получить слои типа 'points' для плана нужным planId
    suspend fun getPointLayersByPlanId(planId: Int): List<LayerEntity> =
        layerDao.getPointLayersByPlanId(planId)

    suspend fun getPending(): List<SyncQueueEntity> = syncQueueDao.getPending()

    suspend fun refreshValuesJson(pointId: Int) {
        val pointWithValues = layerDao.getPointWithValuesByPointIdOnce(pointId) ?: return
        val valuesMap = pointWithValues.values.associate { pvp ->
            pvp.property.name to pvp.value.value
        }
        layerDao.updatePointValuesJson(pointId, Gson().toJson(valuesMap))
    }
}
