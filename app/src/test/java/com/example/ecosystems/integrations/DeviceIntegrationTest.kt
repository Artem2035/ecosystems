package com.example.ecosystems.integrations

import com.example.ecosystems.DataClasses.Device
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.DeviceSearchFilter
import com.example.ecosystems.utils.Parser
import com.example.ecosystems.utils.SearchScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val DEVICES_JSON = """
{
"deviceParameters": [{"test": {"test": "test"}}],
"devices": [
  {
    "device_id": 1442,
    "name": "Метео_007",
    "description": "",
    "latitude": 55.75,
    "longitude": 37.61
  },
  {
    "device_id": 1443,
    "name": "Метео_008",
    "description": "Погодная станция",
    "latitude": 55.76,
    "longitude": 37.62
  },
  {
    "device_id": 1444,
    "name": "Камера_001",
    "description": "Камера наблюдения",
    "latitude": 55.77,
    "longitude": 37.63
  }]
}
"""
private const val DEVICES_JSON2 = """
{
"deviceParameters": [],
"devices": []
}
"""
private const val DEVICES_JSON3 = """
{
"deviceParameters": [{"test": {"test": "test"}}],
"devices": [
  {
    "device_id": 1442,
    "name": "Метео_007",
    "description": "",
    "latitude": 55.75,
    "longitude": 37.61
  },
  {
    "device_id": 1443,
    "name": "Метео_008",
    "description": "Погодная станция",
    "latitude": 55.76,
    "longitude": 37.62
  },
  {
    "name": "Камера_001",
    "description": "Камера наблюдения",
    "latitude": 55.77,
    "longitude": 37.63
  }]
}
"""
@RunWith(JUnit4::class)
class DeviceIntegrationTest {

    private lateinit var apiService: ApiService
    private lateinit var parser: Parser
    private lateinit var filter: DeviceSearchFilter

    @Before
    fun setup() {
        apiService = mockk()
        parser = Parser()
        filter = DeviceSearchFilter()
    }

    @Test
    fun `integration test api parser filter with ok result`() {
        every { apiService.getDevices(any()) } returns DEVICES_JSON

        // WHEN (1) API
        val gson = Gson()
        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
        val json: Map<String, Any?> = mapAdapter.fromJson(apiService.getDevices("test_token"))

        val listOfDevices: MutableList<Map<String, Any?>>
        listOfDevices= json.get("devices") as MutableList<Map<String, Any?>>

        val i1 = mutableMapOf<String, Any?>(
            "device_id" to 1442.0,
            "name" to "Метео_007",
            "description" to "",
            "latitude" to 55.75,
            "longitude" to 37.61
        )
        val i2 = mutableMapOf<String, Any?>(
            "device_id" to 1443.0,
            "name" to "Метео_008",
            "description" to "Погодная станция",
            "latitude" to 55.76,
            "longitude" to 37.62
        )
        val i3 = mutableMapOf<String, Any?>(
            "device_id" to 1444.0,
            "name" to "Камера_001",
            "description" to "Камера наблюдения",
            "latitude" to 55.77,
            "longitude" to 37.63
        )
        val expectedListOfDevices = mutableListOf<Map<String, Any?>>(i1, i2, i3)
        assertThat(listOfDevices, hasItems(*expectedListOfDevices.toTypedArray()))

        // WHEN (2) parsing
        val mapOfDevices = mutableMapOf<Int, Map<String, Any?>>()

        val devicesList: MutableList<Device>
        devicesList = parser.deviceParse(listOfDevices, mapOfDevices)

        val expectedDevices = listOf(
            Device(deviceId=1442, heading="Метео_007", details="", lastUpdate="", visibility=false),
            Device(deviceId=1443, heading="Метео_008", details="", lastUpdate="", visibility=false),
            Device(deviceId=1444, heading="Камера_001", details="", lastUpdate="", visibility=false)
        )

        assertEquals(3, devicesList.size)
        assertThat(devicesList, hasItems(*expectedDevices.toTypedArray()))

        // WHEN (3) filtering
        val result = filter.filterDevicesByNameDescription(
            devicesList = devicesList,
            searchText = "метео",
            scope = SearchScope.NAME_DESCRIPTION
        )

        // THEN
        assertEquals(2, result.size)
        assertTrue(result.all { it.heading.contains("метео", ignoreCase = true) })
    }

    @Test
    fun `integration test api parser filter no devices from api`(){
        every { apiService.getDevices(any()) } returns DEVICES_JSON2

        // WHEN (1) API
        val gson = Gson()
        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
        val json: Map<String, Any?> = mapAdapter.fromJson(apiService.getDevices("test_token"))

        val listOfDevices: MutableList<Map<String, Any?>>
        listOfDevices= json.get("devices") as MutableList<Map<String, Any?>>

        assertEquals(0, listOfDevices.size)

        // WHEN (2) parsing
        val mapOfDevices = mutableMapOf<Int, Map<String, Any?>>()

        val devicesList: MutableList<Device>
        devicesList = parser.deviceParse(listOfDevices, mapOfDevices)

        assertEquals(0, devicesList.size)

        // WHEN (3) filtering
        val result = filter.filterDevicesByNameDescription(
            devicesList = devicesList,
            searchText = "метео",
            scope = SearchScope.NAME_DESCRIPTION
        )

        // THEN
        assertEquals(0, result.size)
    }

    @Test
    fun `integration test api parser filter incorrect device from api`(){
        every { apiService.getDevices(any()) } returns DEVICES_JSON3

        // WHEN (1) API
        val gson = Gson()
        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
        val json: Map<String, Any?> = mapAdapter.fromJson(apiService.getDevices("test_token"))

        val listOfDevices: MutableList<Map<String, Any?>>
        listOfDevices= json.get("devices") as MutableList<Map<String, Any?>>

        assertEquals(3, listOfDevices.size)
        val i1 = mutableMapOf<String, Any?>(
            "device_id" to 1442.0,
            "name" to "Метео_007",
            "description" to "",
            "latitude" to 55.75,
            "longitude" to 37.61
        )
        val i2 = mutableMapOf<String, Any?>(
            "device_id" to 1443.0,
            "name" to "Метео_008",
            "description" to "Погодная станция",
            "latitude" to 55.76,
            "longitude" to 37.62
        )
        val i3 = mutableMapOf<String, Any?>(
            "name" to "Камера_001",
            "description" to "Камера наблюдения",
            "latitude" to 55.77,
            "longitude" to 37.63
        )
        val expectedListOfDevices = mutableListOf<Map<String, Any?>>(i1, i2, i3)
        assertThat(listOfDevices, hasItems(*expectedListOfDevices.toTypedArray()))

        // WHEN (2) parsing
        val mapOfDevices = mutableMapOf<Int, Map<String, Any?>>()

        val devicesList: MutableList<Device>
        devicesList = parser.deviceParse(listOfDevices, mapOfDevices)

        assertEquals(2, devicesList.size)
        val expectedDevices = listOf(
            Device(deviceId=1442, heading="Метео_007", details="", lastUpdate="", visibility=false),
            Device(deviceId=1443, heading="Метео_008", details="", lastUpdate="", visibility=false)
        )
        assertThat(devicesList, hasItems(*expectedDevices.toTypedArray()))

        // WHEN (3) filtering
        val result = filter.filterDevicesByNameDescription(
            devicesList = devicesList,
            searchText = "метео",
            scope = SearchScope.NAME_DESCRIPTION
        )

        // THEN
        assertEquals(2, result.size)
        assertTrue(result.all { it.heading.contains("метео", ignoreCase = true) })
    }

    @Test
    fun `integration test api parser filter no devices from search filter`(){
        every { apiService.getDevices(any()) } returns DEVICES_JSON

        // WHEN (1) API
        val gson = Gson()
        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
        val json: Map<String, Any?> = mapAdapter.fromJson(apiService.getDevices("test_token"))

        val listOfDevices: MutableList<Map<String, Any?>>
        listOfDevices= json.get("devices") as MutableList<Map<String, Any?>>

        assertEquals(3, listOfDevices.size)
        val i1 = mutableMapOf<String, Any?>(
            "device_id" to 1442.0,
            "name" to "Метео_007",
            "description" to "",
            "latitude" to 55.75,
            "longitude" to 37.61
        )
        val i2 = mutableMapOf<String, Any?>(
            "device_id" to 1443.0,
            "name" to "Метео_008",
            "description" to "Погодная станция",
            "latitude" to 55.76,
            "longitude" to 37.62
        )
        val i3 = mutableMapOf<String, Any?>(
            "device_id" to 1444.0,
            "name" to "Камера_001",
            "description" to "Камера наблюдения",
            "latitude" to 55.77,
            "longitude" to 37.63
        )
        val expectedListOfDevices = mutableListOf<Map<String, Any?>>(i1, i2, i3)
        assertThat(listOfDevices, hasItems(*expectedListOfDevices.toTypedArray()))

        // WHEN (2) parsing
        val mapOfDevices = mutableMapOf<Int, Map<String, Any?>>()

        val devicesList: MutableList<Device>
        devicesList = parser.deviceParse(listOfDevices, mapOfDevices)

        assertEquals(3, devicesList.size)
        val expectedDevices = listOf(
            Device(deviceId=1442, heading="Метео_007", details="", lastUpdate="", visibility=false),
            Device(deviceId=1443, heading="Метео_008", details="", lastUpdate="", visibility=false),
            Device(deviceId=1444, heading="Камера_001", details="", lastUpdate="", visibility=false)
        )
        assertThat(devicesList, hasItems(*expectedDevices.toTypedArray()))

        // WHEN (3) filtering
        val result = filter.filterDevicesByNameDescription(
            devicesList = devicesList,
            searchText = "abracadbra",
            scope = SearchScope.NAME_DESCRIPTION
        )

        // THEN
        assertEquals(0, result.size)
    }
}
