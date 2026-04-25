package com.example.ecosystems.db.entity.layer

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.ecosystems.DataClasses.CreatePointRequest
import com.example.ecosystems.DataClasses.UpdatePointRequest
import java.util.Date


@Entity(tableName = "layer_points",
    foreignKeys = [ForeignKey(
        entity = LayerEntity::class,
        parentColumns = ["id"],        // LayerEntity.id
        childColumns = ["layerId"],  // ImageEntity.gisObjectLayerId
        onDelete = ForeignKey.CASCADE, // удалятся все images
        onUpdate = ForeignKey.CASCADE  // обновить id  везде
    )],)
data class LayerPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val layerId: Int,
    val lat: Double,
    val lng: Double,
    val num: Int,
    val valuesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val serverId: Int? = null //id точки на сервере
)

fun LayerPointEntity.toCreateRequest(layerUuid: String, valuesJson:Map<String, Any?>): CreatePointRequest {
    return CreatePointRequest(
        id = this.serverId?.toString() ?: "",   // если null → новая точка
        num = this.num,
        lat = this.lat,
        lng = this.lng,
        values = valuesJson,
        layer_uuid = layerUuid
    )
}

fun LayerPointEntity.toUpdateRequest(
    layerUuid: String, valuesJson:Map<String, Any?>
): UpdatePointRequest? {

    val serverId = this.serverId ?: return null
    val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        java.util.Locale.ENGLISH)

    return UpdatePointRequest(
        id = serverId,
        num = num,
        lat = lat,
        lng = lng,
        values = valuesJson,
        layer_uuid = layerUuid,
        layer_id = layerId,
        created_at = formatter.format(Date(createdAt)),
        updated_at = formatter.format(Date(updatedAt))
    )
}