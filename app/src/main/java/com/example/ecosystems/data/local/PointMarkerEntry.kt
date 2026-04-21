package com.example.ecosystems.data.local

import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.PlacemarkMapObject

data class PointMarkerEntry(
    val placemark: PlacemarkMapObject,
    val collection: ClusterizedPlacemarkCollection
)