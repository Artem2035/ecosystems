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

    // одиночная вставка точки LayerPointEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: LayerPointEntity): Long  // возвращает rowId (= id)

    //задать serverId у точки для слоя типа points
    @Query("UPDATE layer_points SET serverId = :serverId WHERE id = :localId")
    suspend fun setPointServerId(localId: Int, serverId: Int)

    //обновить valuesJson у точки для слоя типа points
    @Query("UPDATE layer_points SET valuesJson = :valuesJson, updatedAt = :updatedAt WHERE id = :pointId")
    suspend fun updatePointValuesJson(pointId: Int, valuesJson: String, updatedAt: Long = System.currentTimeMillis())

    //обновить Num у точки для слоя типа points
    @Query("UPDATE layer_points SET num = :num, updatedAt = :updatedAt WHERE id = :pointId")
    suspend fun updatePointNum(pointId: Int, num: Int, updatedAt: Long = System.currentTimeMillis())
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

    //обновление координат точки
    @Query("UPDATE layer_points SET lat = :lat, lng = :lng, updatedAt = :updatedAt WHERE id = :pointId")
    suspend fun updatePointCoordinates(
        pointId: Int,
        lat: Double,
        lng: Double,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Delete
    suspend fun delete(layer: LayerEntity)

    //удалить точку с pointId
    @Query("DELETE FROM layer_points WHERE id = :pointId")
    suspend fun deletePoint(pointId: Int)

    //удалить незаполненное значение точки
    @Query("DELETE FROM point_values WHERE pointId = :pointId AND propertyId = :propertyId")
    suspend fun deletePointValue(pointId: Int, propertyId: Int)

    //удалить все незаполненное значение точки
    @Query("DELETE FROM point_values WHERE pointId = :pointId AND propertyId IN (:propertyIds)")
    suspend fun deletePointValues(pointId: Int, propertyIds: List<Int>)

    //сохранить значения точки
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePointValues(values: List<PointValueEntity>)

    /*select*/
    @Query("SELECT * FROM layers ORDER BY id ASC")
    fun getAll(): Flow<List<LayerEntity>>                // реактивный — обновляется сам

    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getById(id: Int): LayerEntity?           // разовый, может вернуть null

    //получить id таблицы по id слоя
    @Query("SELECT tableId FROM layers WHERE id = :layerId")
    fun getTableIdByLayerId(layerId: Int): Int?


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

    //получить все layer images для слоя с layerId
    @Query("SELECT * FROM layer_images WHERE gisObjectLayerId = :layerId ORDER BY num ASC")
    suspend fun getImagesByLayerId(layerId: Int): List<LayerImageEntity>

    /*Points*/
    //получить все точки LayerPointEntity для слоя с layerId
    @Query("SELECT * FROM layer_points WHERE layerId = :layerId ORDER BY num ASC")
    suspend fun getPointsByLayerId(layerId: Int): List<LayerPointEntity>

    //получить
    @Query("SELECT * FROM layers WHERE uuid = :uuid ORDER BY id ASC")
    fun getLayerIdByUUID(uuid: String): LayerEntity?
    //получить uuid  слоя по id
    @Query("SELECT * FROM layers WHERE id = :layerId ORDER BY id ASC")
    fun getLayerUUIDById(layerId: Int): LayerEntity

    @Query("SELECT * FROM layer_points ORDER BY num ASC")
    fun getAllLayerPoints(): Flow<List<LayerPointEntity>>

    //получить все точки с данными для слоя с таким id
    @Transaction
    @Query("SELECT * FROM layer_points WHERE layerId = :layerId")
    fun getPointsWithValues(layerId: Int): Flow<List<LayerPointWithValues>>?

    @Transaction
    @Query("SELECT * FROM layer_points")
    fun getAllPointsWithValues(): Flow<List<LayerPointWithValues>>?

    @Query("SELECT * FROM point_values")
    fun getAllPointsRaw(): Flow<List<PointValueEntity>>?
    //получить точку по заданному id
    @Query("SELECT * FROM layer_points WHERE id = :pointId")
    fun getPointById(pointId: Int): LayerPointEntity?

    //получить значения для точки с данным id
    @Transaction
    @Query("SELECT * FROM layer_points WHERE id = :pointId")
    fun getPointWithValuesByPointId(pointId: Int): Flow<List<LayerPointWithValues>>?

    // получить значения для точки с данным id. Одиночный снимок — не Flow
    @Transaction
    @Query("SELECT * FROM layer_points WHERE id = :pointId")
    suspend fun getPointWithValuesByPointIdOnce(pointId: Int): LayerPointWithValues?

    //получить все точки со значениями для слоя с таким id
    @Transaction
    @Query("SELECT * FROM layer_points WHERE layerId = :layerId")
    fun getPointsWithValuesByLayerId(layerId: Int): Flow<List<LayerPointWithValues>>?

    //получить id слоя по известному id точки
    @Query("SELECT layerId FROM layer_points WHERE id = :pointId")
    suspend fun getLayerIdByPointId(pointId: Int): Int

    // получить слои типа 'points' для плана с нужным planId
    @Query("SELECT * FROM layers WHERE gisObjectId = :planId AND type = 'points' ORDER BY name ASC")
    suspend fun getPointLayersByPlanId(planId: Int): List<LayerEntity>
}

