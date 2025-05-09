package com.example.ecosystems.DeviceInfoActivityFragments

import androidx.lifecycle.ViewModel

class SharedViewModel(
    initialMap: MutableMap<String, Any?> = mutableMapOf() // Дефолтное значение (пустая мапа)
) : ViewModel() {

    // Публичная изменяемая мапа
    var currentDevice: MutableMap<String, Any?> = initialMap.toMutableMap()

    // Метод для удобного обновления
    fun updateValue(key: String, value: Any?) {
        currentDevice[key] = value
    }

    fun updateDevice(value:  MutableMap<String, Any?>) {
        currentDevice = HashMap(value)
    }
}