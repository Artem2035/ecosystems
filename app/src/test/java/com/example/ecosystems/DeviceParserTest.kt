package com.example.ecosystems

import com.example.ecosystems.DataClasses.Device
import com.example.ecosystems.utils.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceParserTest {

    private lateinit var parser: Parser
    private lateinit var mapOfDevices: MutableMap<Int, Map<String, Any?>>

    @Before
    fun setUp() {
        parser = Parser()
        mapOfDevices = mutableMapOf()
    }

    @Test
    fun `deviceParse correctly parses valid devices`() {
        val listOfDevices = mutableListOf(
            mapOf(
                "device_id" to 1442,
                "name" to "Метео_007",
                "location_description" to "Room 1",
                "last_update_datetime" to "2025-12-16T12:00:00Z"
            )
        )

        val result: MutableList<Device> = parser.deviceParse(listOfDevices.toMutableList(), mapOfDevices)

        assertEquals(1, result.size)
        val device = result.first()
        assertEquals(1442, device.deviceId)
        assertEquals("Метео_007", device.heading)
        assertEquals("Room 1", device.details)
        assertEquals("2025-12-16T12:00:00Z", device.lastUpdate)

        // Проверка mapOfDevices
        assertEquals(1, mapOfDevices.size)
        assertEquals(listOfDevices.first(), mapOfDevices[1442])
    }

    @Test
    fun `deviceParse skips devices without device_id`() {
        val listOfDevices = mutableListOf(
            mapOf(
                "name" to "NoIdDevice",
                "location_description" to "Room 2",
                "last_update_datetime" to "2025-12-16T13:00:00Z"
            )
        )

        val result: MutableList<Device> = parser.deviceParse(listOfDevices.toMutableList(), mapOfDevices)

        // Результат должен быть пустым
        assertTrue(result.isEmpty())
        assertTrue(mapOfDevices.isEmpty())
    }
}