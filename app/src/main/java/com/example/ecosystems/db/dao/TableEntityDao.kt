package com.example.ecosystems.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity
import com.example.ecosystems.db.relation.TableWithProperties

@Dao
interface TableEntityDao {

    //INSERT

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<TableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableProperties(properties: List<TablePropertyEntity>)

    @Transaction
    suspend fun insertTablesWithProperties(
        tables: List<TableEntity>,
        properties: List<TablePropertyEntity>
    ) {
        insertTables(tables)
        insertTableProperties(properties)
    }

    //очистить все метаданные таблиц перед загрузкой с сервера
    @Query("DELETE FROM tables")
    suspend fun deleteAll()

    //SELECT

    @Query("SELECT * FROM tables WHERE id = :tableId")
    suspend fun getTableById(tableId: Int): TableEntity?

    @Query("SELECT * FROM table_properties WHERE tableId = :tableId ORDER BY sortOrder ASC")
    suspend fun getPropertiesByTableId(tableId: Int): List<TablePropertyEntity>

    @Transaction
    @Query("SELECT * FROM tables WHERE id = :tableId")
    suspend fun getTableWithProperties(tableId: Int): TableWithProperties?

    @Query("SELECT * FROM tables")
    suspend fun getAllTables(): List<TableEntity>

    @Transaction
    @Query("SELECT * FROM tables")
    suspend fun getAllTablesWithProperties(): List<TableWithProperties>

    @Transaction
    @Query("SELECT id FROM table_properties WHERE tableId = :tableId and  name = :propertyName")
    suspend fun getTablePropertyIdByName(tableId: Int, propertyName: String): Int

    // вставить новые таблицы, пропустить, если уже существует
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTablesIgnore(tables: List<TableEntity>)
    //обновить существующие таблицы
    @Update
    suspend fun updateTables(tables: List<TableEntity>)
    // вставить новые параметры таблицы, пропустить, если уже существуют
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTablePropertiesIgnore(properties: List<TablePropertyEntity>)
    //обновить существующие параметры таблицы
    @Update
    suspend fun updateTableProperties(properties: List<TablePropertyEntity>)

    // Upsert вручную — сначала пробуем обновить,
    // потом вставляем только те что ещё не существуют
    @Transaction
    suspend fun upsertTablesWithProperties(
        tables: List<TableEntity>,
        properties: List<TablePropertyEntity>
    ) {
        updateTables(tables)
        insertTablesIgnore(tables)      // вставит только новые (которые не обновились)
        updateTableProperties(properties)
        insertTablePropertiesIgnore(properties)
    }
}