package com.example.ecosystems.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_files",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["gisObjectId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gisObjectId")]
)
data class PlanFileEntity(
    @PrimaryKey val id: Int,

    val gisObjectId: Int, // ссылается на план

    val uuid: String,
    val name: String,
    val description: String?,

    val originalFilename: String?,
    val fileInfoUploadFilename: String?,
    val fileInfoSize: Long?,

    val formatType: Int,
    val statusType: Int,

    val gisCategoryId: Int,
    val gisCategoryTypeId: Int?,

    val droneDeviceId: Int?,
    val droneName: String?,
    val errorDescription: String?,

    val hasReducedFile: Boolean?,

    val centerLat: Double?,
    val centerLng: Double?,
    val bound1Lat: Double?,
    val bound1Lng: Double?,
    val bound2Lat: Double?,
    val bound2Lng: Double?,

    val year: Int?,

    val createdAt: Long,
    val updatedAt: Long
)