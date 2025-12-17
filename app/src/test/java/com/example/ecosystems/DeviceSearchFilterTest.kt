package com.example.ecosystems

import com.example.ecosystems.DataClasses.Device
import com.example.ecosystems.utils.DeviceSearchFilter
import com.yandex.mapkit.geometry.Point
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DeviceSearchFilterTest {

    private lateinit var filter: DeviceSearchFilter
    private lateinit var devices: List<Device>

    @Before
    fun setUp() {
        filter = DeviceSearchFilter()

        devices = listOf(
            Device(
                deviceId = 1,
                heading = "Car Tracker",
                details = "GPS device for car",
                lastUpdate = "2024-01-01",
                visibility = true
            ),
            Device(
                deviceId = 2,
                heading = "Bike Sensor",
                details = "Speed and GPS sensor",
                lastUpdate = "2024-01-02",
                visibility = true
            ),
            Device(
                deviceId = 3,
                heading = "Home Alarm",
                details = "Security system",
                lastUpdate = "2024-01-03",
                visibility = false
            )
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(Locale.getDefault())
    }

/*
    @Test
    fun `filterDevicesByNameDescription returns all devices when searchText empty`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "",
            scope = SearchScope.NAME
        )

        assertEquals(devices, result)
    }

    @Test
    fun `filterDevicesByNameDescription filters by NAME`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "car",
            scope = SearchScope.NAME
        )

        assertEquals(1, result.size)
        assertEquals(1, result.first().deviceId)
        assertEquals("Car Tracker", result.first().heading)
    }

    @Test
    fun `filterDevicesByNameDescription filters by DESCRIPTION`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "gps",
            scope = SearchScope.DESCRIPTION
        )

        assertEquals(2, result.size)
        assert(result.any { it.deviceId == 1 })
        assert(result.any { it.deviceId == 2 })
    }

    @Test
    fun `filterDevicesByNameDescription filters by NAME_DESCRIPTION`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "gps",
            scope = SearchScope.NAME_DESCRIPTION
        )

        // Совпадение должно быть и в heading, и в details
        assertEquals(0, result.size)
    }

    @Test
    fun `filterDevicesByNameDescription is case insensitive`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "CAR",
            scope = SearchScope.NAME
        )

        assertEquals(1, result.size)
        assertEquals("Car Tracker", result.first().heading)
    }

    @Test
    fun `filterDevicesByNameDescription returns empty list when no matches`() {
        val result = filter.filterDevicesByNameDescription(
            devicesList = devices,
            searchText = "plane",
            scope = SearchScope.DESCRIPTION
        )

        assertEquals(0, result.size)
    }
*/

//    -------------------

    @Test
    fun `distanceTo returns zero for same points`() {
        val point = Point(55.0, 37.0)

        val distance = filter.distanceTo(point, point)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `distanceTo returns distance between points`() {
        val point1 = Point(59.9386, 30.3141)
        val point2 = Point(55.7558, 37.6173)
        val distance = filter.distanceTo(point1, point2)

        assertEquals(634193.862, distance, 0.001)
    }

    @Test
    fun `isWithinRadius returns true when point inside radius`() {
        val center = Point(55.0, 37.0)
        val nearby = Point(55.0001, 37.0001) // ~13–15 м

        val result = filter.isWithinRadius(center, nearby, radiusMeters = 50.0)

        assertTrue(result)
    }

    @Test
    fun `isWithinRadius returns false when point outside radius`() {
        val center = Point(55.0, 37.0)
        val farAway = Point(55.01, 37.01) // ~1.3 км

        val result = filter.isWithinRadius(center, farAway, radiusMeters = 100.0)

        assertFalse(result)
    }

    @Test
    fun `filterByRadius returns only devices inside radius`() {
        val devices = listOf(
            Device(
                deviceId = 1,
                heading = "Device 1",
                details = "",
                lastUpdate = ""
            ),
            Device(
                deviceId = 2,
                heading = "Device 2",
                details = "",
                lastUpdate = ""
            )
        )

        val mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf(
            1 to mapOf(
                "latitude" to 55.0001,
                "longitude" to 37.0001
            ),
            2 to mapOf(
                "latitude" to 56.0,
                "longitude" to 38.0
            )
        )

        val result = filter.filterByRadius(
            mapOfDevices = mapOfDevices,
            devices = devices,
            centerLat = 55.0,
            centerLon = 37.0,
            radiusMeters = 100.0
        )

        assertEquals(1, result.size)
        assertEquals(1, result.first().deviceId)
    }

    @Test
    fun `filterByRadius returns empty list when no devices in radius`() {
        val devices = listOf(
            Device(
                deviceId = 10,
                heading = "Device",
                details = "",
                lastUpdate = ""
            )
        )

        val mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf(
            10 to mapOf(
                "latitude" to 60.0,
                "longitude" to 60.0
            )
        )

        val result = filter.filterByRadius(
            mapOfDevices = mapOfDevices,
            devices = devices,
            centerLat = 55.0,
            centerLon = 37.0,
            radiusMeters = 500.0
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterByRadius works when latitude and longitude are strings`() {
        val devices = listOf(
            Device(
                deviceId = 1,
                heading = "Device",
                details = "",
                lastUpdate = ""
            )
        )

        val mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf(
            1 to mapOf(
                "latitude" to "55.0",
                "longitude" to "37.0"
            )
        )

        val result = filter.filterByRadius(
            mapOfDevices = mapOfDevices,
            devices = devices,
            centerLat = 55.0,
            centerLon = 37.0,
            radiusMeters = 1.0
        )

        assertEquals(1, result.size)
    }

    @Test
    fun `filterByRadius skips device when deviceId not found in mapOfDevices`() {
        val devices = listOf(
            Device(
                deviceId = 100,
                heading = "Device",
                details = "",
                lastUpdate = ""
            )
        )

        val mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()
        // deviceId 100 отсутствует

        val result = filter.filterByRadius(
            mapOfDevices = mapOfDevices,
            devices = devices,
            centerLat = 55.0,
            centerLon = 37.0,
            radiusMeters = 1000.0
        )

        assertTrue(result.isEmpty())
    }

}