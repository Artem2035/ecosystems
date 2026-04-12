package com.example.ecosystems.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.entity.LayerEntity
import com.example.ecosystems.db.entity.LayerImageEntity
import com.example.ecosystems.db.entity.LayerPointEntity
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity

@Database(entities = [LayerEntity::class,
    LayerImageEntity::class,
    LayerPointEntity::class,
    PlanEntity::class,
    PlanFileEntity::class], exportSchema = false, version = 6) // список всех Entity

abstract class AppDatabase : RoomDatabase() {
    abstract fun layerDao(): LayerEntityDao
    abstract fun planDao(): PlanEntityDao
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