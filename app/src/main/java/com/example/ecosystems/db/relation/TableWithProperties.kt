package com.example.ecosystems.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity

data class TableWithProperties(
    @Embedded val table: TableEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tableId"
    )
    val properties: List<TablePropertyEntity>
)