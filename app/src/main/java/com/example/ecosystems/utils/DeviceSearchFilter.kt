package com.example.ecosystems.utils

import com.example.ecosystems.DataClasses.Device
import com.yandex.mapkit.geometry.Point
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


enum class SearchScope {
    NAME,
    DESCRIPTION,
    NAME_DESCRIPTION
}

class DeviceSearchFilter {
    fun filterDevicesByNameDescription(devicesList: List<Device>, searchText: String,
                                       scope: SearchScope): List<Device> {
        if (searchText.isEmpty()) return devicesList

        val tempDevicesList: MutableList<Device> = mutableListOf()

        when (scope) {
            SearchScope.NAME -> {
                devicesList.forEach {
                    if(it.heading.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault()))){
                        tempDevicesList.add(it)
                    }
                }
            }
            SearchScope.DESCRIPTION -> {
                devicesList.forEach {
                    if(it.details.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault()))){
                        tempDevicesList.add(it)
                    }
                }
            }
            SearchScope.NAME_DESCRIPTION -> {
                devicesList.forEach {
                    if(it.details.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault())) ||
                        it.heading.lowercase(Locale.getDefault()).contains(searchText.lowercase(Locale.getDefault()))){
                        tempDevicesList.add(it)
                    }
                }
            }
        }

        return tempDevicesList
    }

    fun isWithinRadius(point:Point, otherPoint: Point, radiusMeters: Double): Boolean {
        return distanceTo(point, otherPoint) <= radiusMeters
    }

    fun distanceTo(point1: Point, point2: Point): Double {
        val R = 6371000.0 // радиус Земли в метрах

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    fun filterByRadius(
        mapOfDevices: MutableMap<Int, Map<String, Any?>>,
        devices: List<Device>,
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double
    ): List<Device> {
        val center = Point(centerLat, centerLon)

        val tempDevicesList: MutableList<Device> = mutableListOf()

        devices.forEach {
            if(!mapOfDevices.containsKey(it.deviceId))
                return tempDevicesList
            val device = mapOfDevices.get(it.deviceId)
            val point = Point(device?.get("latitude").toString().toDouble(),
                device?.get("longitude").toString().toDouble(),)
            if ( isWithinRadius(center, point, radiusMeters))
                tempDevicesList.add(it)
        }

        return tempDevicesList
    }
}

