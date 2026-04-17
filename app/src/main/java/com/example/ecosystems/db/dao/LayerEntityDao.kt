package com.example.ecosystems.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.relation.LayerPointWithValues
import com.example.ecosystems.db.relation.LayerWithData
import kotlinx.coroutines.flow.Flow

@Dao
interface LayerEntityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layer: LayerEntity): Long          // возвращает новый id

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayers(layers: List<LayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<LayerImageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<LayerPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointsValues(pointsValues: List<PointValueEntity>)

    @Transaction
    suspend fun insertAllData(
        layers: List<LayerEntity>,
        points: List<LayerPointEntity>,
        images: List<LayerImageEntity>,
        pointsValues: List<PointValueEntity>
    ) {
        insertLayers(layers)
        insertPoints(points)
        insertImages(images)
        insertPointsValues(pointsValues)
    }

    @Update
    suspend fun update(layer: LayerEntity)                // обновляет по PK

    @Delete
    suspend fun delete(layer: LayerEntity)

    /*select*/
    @Query("SELECT * FROM layers ORDER BY id ASC")
    fun getAll(): Flow<List<LayerEntity>>                // реактивный — обновляется сам

    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getById(id: Int): LayerEntity?           // разовый, может вернуть null


    // Layer + images + points
    @Transaction
    @Query("SELECT * FROM layers WHERE id = :layerId")
    suspend fun getLayerWithData(layerId: Int): LayerWithData?

    @Transaction
    @Query("SELECT * FROM layers")
    fun getAllLayersWithData(): Flow<List<LayerWithData>>

    @Transaction
    @Query("SELECT * FROM layers WHERE gisObjectId = :planId")
    fun observePlanLayersWithData(planId: Int): Flow<List<LayerWithData>>

    /*Images*/
    @Query("SELECT * FROM layer_images ORDER BY num ASC")
    fun getAllLayerImages(): Flow<List<LayerImageEntity>>

    @Query("SELECT * FROM layer_images WHERE gisObjectLayerId = :layerId ORDER BY num ASC")
    fun getImagesByLayerId(layerId: Int): Flow<List<LayerImageEntity>>

    /*Points*/
    @Query("SELECT * FROM layer_points WHERE layerId = :layerId ORDER BY num ASC")
    fun getPointsByLayerId(layerId: Int): Flow<List<LayerPointEntity>>

    @Query("SELECT * FROM layers WHERE uuid = :uuid ORDER BY id ASC")
    fun getLayerIdByUUID(uuid: String): LayerEntity?

    @Query("SELECT * FROM layer_points ORDER BY num ASC")
    fun getAllLayerPoints(): Flow<List<LayerPointEntity>>

    @Transaction
    @Query("SELECT * FROM layer_points WHERE layerId = :layerId")
    fun getPointsWithValues(layerId: Int): Flow<List<LayerPointWithValues>>?

    @Transaction
    @Query("SELECT * FROM layer_points")
    fun getAllPointsWithValues(): Flow<List<LayerPointWithValues>>?

    @Query("SELECT * FROM point_values")
    fun getAllPointsRaw(): Flow<List<PointValueEntity>>?

    @Transaction
    @Query("SELECT * FROM layer_points WHERE id = :pointId")
    fun getPointWithValuesByPointId(pointId: Int): Flow<List<LayerPointWithValues>>?

}