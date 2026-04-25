package com.example.ecosystems.db.repository

import com.example.ecosystems.db.dao.TableEntityDao
import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity

class TableRepository(private val tableEntityDao: TableEntityDao) {

    suspend fun insertTablesWithProperties(
        tables: List<TableEntity>,
        properties: List<TablePropertyEntity>
    ) {
        tableEntityDao.insertTablesWithProperties(tables, properties)
    }

    suspend fun getTableById(tableId: Int) =
        tableEntityDao.getTableById(tableId)

    suspend fun getPropertiesByTableId(tableId: Int) =
        tableEntityDao.getPropertiesByTableId(tableId)

    suspend fun getTableWithProperties(tableId: Int) =
        tableEntityDao.getTableWithProperties(tableId)

    suspend fun getAllTablesWithProperties() =
        tableEntityDao.getAllTablesWithProperties()

    suspend fun getTablePropertyIdByName(tableId: Int, propertyName: String) =
        tableEntityDao.getTablePropertyIdByName(tableId, propertyName)

    suspend fun deleteAll() = tableEntityDao.deleteAll()
}