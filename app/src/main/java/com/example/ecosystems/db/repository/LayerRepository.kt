package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.entity.LayerEntity
import com.example.ecosystems.db.entity.LayerImageEntity
import com.example.ecosystems.db.entity.LayerPointEntity

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
        images: List<LayerImageEntity>
    ) {
        layerDao.insertAllData(layers, points, images)
    }

    fun getAllLayersWithData() = layerDao.getAllLayersWithData()

    suspend fun getLayerWithData(layerId: Int) =
        layerDao.getLayerWithData(layerId)

    suspend fun getPointsByLayerId(layerId: Int) =
        layerDao.getPointsByLayerId(layerId)

    suspend fun getAllLayerPoints() = layerDao.getAllLayerPoints()
}