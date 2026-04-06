package com.example.ecosystems.DataClasses

data class DeviceInfo(
    val id: Int,
    val name: String,
    val description: String = "",
    val serialNumber: String,
    val locationDescription: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val deviceTypeId: Int,
    val moduleTypeId: Int? = null,
    val tz: Int = 0,
    val timeNotOnline: Int = 0,
    val isPublic: Boolean = false,
    val isAllowDownload: Boolean = false,
    val isVerified: Boolean = false,
    val fileFormat: String = "undefined"
)