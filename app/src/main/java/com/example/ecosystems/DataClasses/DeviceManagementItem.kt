package com.example.ecosystems.DataClasses

//DeviceManagementItem элемент DeviceManagementFragment для RecycleView
data class DeviceManagementItem(var deviceId: Int, var name: String, var serialNum: String,
                                var description: String, var location: String, var isPublic: Boolean)
