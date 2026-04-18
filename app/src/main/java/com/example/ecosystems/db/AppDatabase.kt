package com.example.ecosystems.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.dao.TableEntityDao
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity

@Database(entities = [LayerEntity::class,
    LayerImageEntity::class,
    LayerPointEntity::class,
    PlanEntity::class,
    PlanFileEntity::class,
    TableEntity::class,
    TablePropertyEntity::class,
    PointValueEntity::class], exportSchema = false, version = 10) // список всех Entity

abstract class AppDatabase : RoomDatabase() {
    abstract fun layerDao(): LayerEntityDao
    abstract fun planDao(): PlanEntityDao
    abstract fun tableDao(): TableEntityDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app.db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}