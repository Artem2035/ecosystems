package com.example.ecosystems.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
}