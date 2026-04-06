package com.example.ecosystems.DeviceInfoActivityFragments

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.ecosystems.DataClasses.DeviceInfo
import com.example.ecosystems.data.local.SecureDevicesParametersManager
import kotlinx.coroutines.CoroutineScope

class SharedViewModel(
    initialMap: MutableMap<String, Any?> = mutableMapOf() // Дефолтное значение (пустая мапа)
) : ViewModel() {

    // Публичная изменяемая мапа
    var currentDevice: MutableMap<String, Any?> = initialMap.toMutableMap()
    var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()
    private var mapOfDeviceParameters: MutableMap<String, Map<String, Any?>> = mutableMapOf()

    private var secureManager: SecureDevicesParametersManager? = null
    fun init(context: Context) {
        secureManager = SecureDevicesParametersManager(context)
        val devicesManagerData = secureManager!!.loadData()
        if(devicesManagerData.get("mapOfDevices") != null ){
            mapOfDevices = devicesManagerData.get("mapOfDevices") as MutableMap<Int, Map<String, Any?>>
            mapOfDeviceParameters = devicesManagerData.get("mapOfDeviceParameters") as MutableMap<String, Map<String, Any?>>
        }
    }

    // Метод для удобного обновления
    fun updateValue(key: String, value: Any?) {
        currentDevice[key] = value
    }

    fun updateDevice(value:  MutableMap<String, Any?>) {
        currentDevice = HashMap(value)
    }

    fun updateDeviceInfo(deviceInfo: DeviceInfo, scope: CoroutineScope) {
        //Обновляем currentDevice
        currentDevice.putAll(deviceInfo.toMap())
        // Обновляем в mapOfDevices
        val deviceId = deviceInfo.id
        val existing = mapOfDevices[deviceId]?.toMutableMap() ?: mutableMapOf()
        existing.putAll(deviceInfo.toMap())
        mapOfDevices[deviceId] = existing

        // Сохраняем кэш
        saveCache()
    }

    // Получить текущий DeviceInfo
    fun getDeviceInfo(): DeviceInfo? = currentDevice.toDeviceInfo()

    private fun saveCache() {
        val data = mutableMapOf<String, MutableMap<*, *>>(
            "mapOfDevices" to mapOfDevices,
            "mapOfDeviceParameters" to mapOfDeviceParameters
        )
        secureManager?.saveData(data)
    }
}

//  конвертации DeviceInfo -> Map
fun DeviceInfo.toMap(): MutableMap<String, Any?> = mutableMapOf(
    "id"                   to id,
    "name"                 to name,
    "device_type_id"       to deviceTypeId,
    "serial_number"        to serialNumber,
    "description"          to description,
    "location_description" to locationDescription,
    "latitude"             to latitude,
    "longitude"            to longitude,
    "module_type_id"       to moduleTypeId,
    "tz"                   to tz,
    "time_not_online"      to timeNotOnline,
    "is_public"            to isPublic,
    "is_allow_download"    to isAllowDownload,
    "is_verified"          to isVerified
)

// Map -> DeviceInfo
fun MutableMap<String, Any?>.toDeviceInfo(): DeviceInfo? {
    return try {
        DeviceInfo(
            id = this["id"]?.toString()?.toDoubleOrNull()?.toInt() ?: return null,
            name = this["name"]?.toString() ?: return null,
            deviceTypeId = this["device_type_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: return null,
            serialNumber = this["serial_number"]?.toString() ?: return null,

            description = this["description"]?.toString() ?: "",
            locationDescription = this["location_description"]?.toString() ?: "",
            latitude = this["latitude"]?.toString()?.toDoubleOrNull() ?: 0.0,
            longitude = this["longitude"]?.toString()?.toDoubleOrNull() ?: 0.0,
            moduleTypeId = this["module_type_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1,
            tz = this["tz"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
            timeNotOnline = this["time_not_online"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,

            isPublic = this["is_public"]?.toString()?.toDoubleOrNull()?.toInt() == 1,
            isAllowDownload = this["is_allow_download"]?.toString()?.toDoubleOrNull()?.toInt() == 1,
            isVerified = this["is_verified"]?.toString()?.toDoubleOrNull()?.toInt() == 1
        )
    } catch (e: Exception) {
        Log.e("toDeviceInfo", "Conversion failed", e)
        null
    }
}