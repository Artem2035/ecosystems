package com.example.ecosystems

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark для проверки рендера маркеров на карте.
 * Работает без WRITE_EXTERNAL_STORAGE и GrantPermissionRule.
 */
@RunWith(AndroidJUnit4::class)
class MapRenderBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun render_0_devices() = measureRender(0)

    @Test
    fun render_100_devices() = measureRender(100)

    @Test
    fun render_300_devices() = measureRender(300)

    @Test
    fun render_500_devices() = measureRender(500)

    @Test
    fun render_1000_devices() = measureRender(1000)

    @Test
    fun render_2000_devices() = measureRender(2000)

    private fun measureRender(count: Int) {
        benchmarkRule.measureRepeated {
            // Лаунчим Activity с ActivityScenario
            ActivityScenario.launch(MapActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    // Генерируем устройства
                    activity.mapOfDevices = generateDevices(count)
                    // Отрисовываем маркеры
                    activity.DrawDevicesOnMap()
                }
                // Даем системе отрисовать несколько кадров
                Thread.sleep(1000)
            }
        }
    }
}

/**
 * Генерация фиктивных устройств для карты
 */
fun generateDevices(count: Int): MutableMap<Int, Map<String, Any?>> {
    val devices = mutableMapOf<Int, Map<String, Any?>>()
    for (i in 0 until count) {
        devices[i] = createDeviceJson(
            deviceId = i,
            lat = 55.0 + i * 0.0001,
            lon = 37.0 + i * 0.0001
        )
    }
    return devices
}

/**
 * Создание структуры устройства (моделируем JSON)
 */
fun createDeviceJson(deviceId: Int, lat: Double, lon: Double): Map<String, Any?> {
    return mapOf(
        "description" to "",
        "device_id" to deviceId,
        "device_type_id" to 1,
        "id_device" to deviceId,
        "is_allow_download" to 1,
        "is_public" to 0,
        "is_verified" to 0,
        "latitude" to lat,
        "longitude" to lon,
        "location_description" to "",
        "module_type_id" to null,
        "name" to "Метео_$deviceId",
    )
}
