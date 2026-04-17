package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity

class LayerRepository(private val layerDao: LayerEntityDao) {


    fun getAllLayers() = layerDao.getAll()

    suspend fun insertLayers(layers: List<LayerEntity>) {
        layerDao.insertLayers(layers)
    }

    suspend fun insertAllPoints(points: List<LayerPointEntity>) {
        layerDao.insertPoints(points)
    }

    suspend fun insertAllImages(images: List<LayerImageEntity>) {
        layerDao.insertImages(images)
    }

    suspend fun insertAllData(
        layers: List<LayerEntity>,
        points: List<LayerPointEntity>,
        images: List<LayerImageEntity>,
        pointsValues: List<PointValueEntity>
    ) {
        layerDao.insertAllData(layers, points, images, pointsValues)
    }

    fun getAllLayersWithData() = layerDao.getAllLayersWithData()

    suspend fun getLayerWithData(layerId: Int) =
        layerDao.getLayerWithData(layerId)

    fun getPointsWithValues(layerId: Int) =
        layerDao.getPointsWithValues(layerId)

    fun getAllPointsWithValues() =
        layerDao.getAllPointsWithValues()

    fun getPointsByLayerId(layerId: Int) =
        layerDao.getPointsByLayerId(layerId)

    fun getAllLayerPoints() = layerDao.getAllLayerPoints()

    fun getAllPointsRaw() = layerDao.getAllPointsRaw()

    fun getPointWithValuesByPointId(pointId: Int) =
        layerDao.getPointWithValuesByPointId(pointId)
}