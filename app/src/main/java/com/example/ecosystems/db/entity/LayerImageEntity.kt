package com.example.ecosystems.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "layer_images",
    foreignKeys = [ForeignKey(
        entity = LayerEntity::class,
        parentColumns = ["id"],        // LayerEntity.id
        childColumns = ["gisObjectLayerId"],  // ImageEntity.gisObjectLayerId
        onDelete = ForeignKey.CASCADE, // удалить layer → удалятся все images
        onUpdate = ForeignKey.CASCADE  // обновить id → обновится везде
    )],)
data class LayerImageEntity(
    @PrimaryKey val id: Int,
    val uuid: String,
    val filename: String,
    val originalFilename: String,
    val gisObjectLayerId: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long,
    // офлайн-поля
    val localPath: String? = null,
    //val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val lastAccessedAt: Long? = null
)