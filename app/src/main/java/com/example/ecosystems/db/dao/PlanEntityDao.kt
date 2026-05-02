package com.example.ecosystems.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.relation.PlanWithData
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity): Long          // возвращает новый id

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<PlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFiles(plans: List<PlanFileEntity>)

    @Update
    suspend fun update(plan: PlanEntity)                // обновляет по PK
    @Delete
    suspend fun delete(layer: LayerEntity)


    //очистить все планы перед загрузкой с сервера
    @Query("DELETE FROM plans")
    suspend fun deleteAll()

    @Query("SELECT * FROM plans ORDER BY id ASC")
    suspend fun getAllPlans(): List<PlanEntity>

    @Transaction
    @Query("SELECT * FROM plans")
    fun getAllPlansWithData(): Flow<List<PlanWithData>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getPlanWithData(planId: Int): PlanWithData?

    @Transaction
    @Query("SELECT * FROM plan_files WHERE gisObjectId = :planId")
    suspend fun getPlanFiles(planId: Int): List<PlanFileEntity>

    // Получить uuid плана по его id
    @Query("SELECT uuid FROM plans WHERE id = :planId")
    suspend fun getPlanUuidById(planId: Int): String?
}