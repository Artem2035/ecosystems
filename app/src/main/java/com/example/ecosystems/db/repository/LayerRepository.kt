package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity

class LayerRepository(private val layerDao: LayerEntityDao) {


    fun getAllLayers() = layerDao.getAll()

    suspend fun updatePointCoordinates(pointId: Int, lat: Double, lng: Double) =
        layerDao.updatePointCoordinates(pointId, lat, lng)

    //обновление координат точки
    suspend fun insertLayers(layers: List<LayerEntity>) {
        layerDao.insertLayers(layers)
    }

    suspend fun insertAllPoints(points: List<LayerPointEntity>) {
        layerDao.insertPoints(points)
    }
    // одиночная вставка точки LayerPointEntity
    suspend fun insertPoint(point: LayerPointEntity): Long =
        layerDao.insertPoint(point)

    suspend fun insertAllImages(images: List<LayerImageEntity>) {
        layerDao.insertImages(images)
    }
    //удалить точку с pointId
    suspend fun deletePoint(pointId: Int) =
        layerDao.deletePoint(pointId)

    suspend fun insertAllData(
        layers: List<LayerEntity>,
        points: List<LayerPointEntity>,
        images: List<LayerImageEntity>,
        pointsValues: List<PointValueEntity>
    ) {
        layerDao.insertAllData(layers, points, images, pointsValues)
    }
    //удалить незаполненное значение точки
    suspend fun deletePointValue(pointId: Int, propertyId: Int) =
        layerDao.deletePointValue(pointId, propertyId)
    //сохранить все значения точки
    suspend fun savePointValues(values: List<PointValueEntity>) =
        layerDao.savePointValues(values)

    fun getAllLayersWithData() = layerDao.getAllLayersWithData()

    suspend fun getLayerWithData(layerId: Int) =
        layerDao.getLayerWithData(layerId)

    fun getPointsWithValues(layerId: Int) =
        layerDao.getPointsWithValues(layerId)

    fun getAllPointsWithValues() =
        layerDao.getAllPointsWithValues()

    //получить все точки LayerPointEntity для слоя с layerId
    suspend fun getPointsByLayerId(layerId: Int) =
        layerDao.getPointsByLayerId(layerId)

    suspend fun getImagesByLayerId(layerId: Int) =
        layerDao.getImagesByLayerId(layerId)

    fun getAllLayerPoints() = layerDao.getAllLayerPoints()

    fun getAllPointsRaw() = layerDao.getAllPointsRaw()

    //получить значения для точки с данным id
    fun getPointWithValuesByPointId(pointId: Int) =
        layerDao.getPointWithValuesByPointId(pointId)

    //получить все точки со значениями для слоя с таким id
    fun getPointsWithValuesByLayerId(layerId: Int) =
        layerDao.getPointsWithValuesByLayerId(layerId)

    //получить id таблицы по id слоя
    fun getTableIdByLayerId(layerId: Int) =
        layerDao.getTableIdByLayerId(layerId)

    //получить id слоя по известному id точки
    fun getLayerIdByPointId(pointId: Int) =
        layerDao.getLayerIdByPointId(pointId)

    // получить слои типа 'points' для плана нужным planId
    suspend fun getPointLayersByPlanId(planId: Int): List<LayerEntity> =
        layerDao.getPointLayersByPlanId(planId)
}