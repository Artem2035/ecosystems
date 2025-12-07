package com.example.ecosystems.ApiService

import android.util.Log
import com.example.ecosystems.network.ApiService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SaveDeviceInfoChangesTest {
    private lateinit var client: OkHttpClient
    private lateinit var call: Call
    private lateinit var response: Response
    private lateinit var responseBody: ResponseBody

    // Пример базового URL (тот же, что вы используете в сервисе)
    private val baseUrl = "https://example.com/"

    @Before
    fun setUp() {
        client = mockk()
        call = mockk()
        response = mockk()
        responseBody = mockk()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { response.close() } just Runs
    }

    fun RequestBody.toText(): String {
        val buffer = Buffer()
        this.writeTo(buffer)
        return buffer.readUtf8()
    }

    @Test
    fun `saveDeviceInfoChanges - successful call with all fields`() {
        // Arrange
        val token = "tokenABC"

        val newDeviceInfo = mutableMapOf<String, Any?>(
            "id" to 123,
            "name" to "Test Device",
            "description" to "Test description",
            "latitude" to 55.7558,
            "longitude" to 37.6173,
            "device_type_id" to 1.0,
            "location_description" to "Moscow",
            "serial_number" to "SN123456",
            "is_public" to 1.0,
            "is_allow_download" to 0.0,
            "is_verified" to 1.0,
            "module_type_id" to 2,
            "tz" to 3.0,
            "time_not_online" to 3600.0
        )

        // Подготовим мок ответа
        val responseJson = """{"result":"ok"}"""
        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns responseJson
        every { response.close() } just Runs

        // Перехватим Request
        val slot = slot<Request>()
        every { client.newCall(capture(slot)) } returns call
        every { call.execute() } returns response

        val api = ApiService(client, baseUrl)

        // Act
        api.saveDeviceInfoChanges(token, newDeviceInfo)

        // Assert — проверим Request
        val req = slot.captured
        // Проверка URL, заголовков
        assertEquals("${baseUrl}api/v1/update_device_info", req.url.toString())
        assertEquals("Bearer $token", req.header("Authorization"))
        assertEquals("multipart/form-data", req.header("Content-Type")?.substringBefore(";"))

        val body = req.body as MultipartBody
        val partsMap = body.parts.associateBy { it.headers?.get("Content-Disposition")?.substringAfter("name=\"")?.substringBefore("\"") }

        val expectedValues = mapOf(
            "id" to "123",
            "name" to "Test Device",
            "description" to "Test description",
            "latitude" to "55.7558",
            "longitude" to "37.6173",
            "device_type_id" to "1",
            "location_description" to "Moscow",
            "serial_number" to "SN123456",
            "is_public" to "1",
            "is_allow_download" to "0",
            "is_verified" to "1",
            "file_format" to "undefined",
            "module_type_id" to "2",
            "tz" to "3",
            "time_not_online" to "3600"
        )


        // Проверяем, что все ключи присутствуют
        expectedValues.forEach { (key, expected) ->
            val part = partsMap[key]
            assert(part != null) { "Multipart body missing part: $key" }

            val bodyString = part!!.body.toText()
            assert(bodyString.contains(expected)) { "Multipart body for $key does not contain expected value" }
        }

        // И call.execute() был вызван
        verify(exactly = 1) { call.execute() }
    }

    @Test
    fun `saveDeviceInfoChanges - throws an exception in case of an unsuccessful response`() {
        // Arrange
        val token = "tokenABC"

        val newDeviceInfo = mutableMapOf<String, Any?>(
            "id" to 123,
            "name" to "Test Device",
            "device_type_id" to 1.0,
            "serial_number" to "SN123456"
        )

        every { response.isSuccessful } returns false

        val slot = slot<Request>()
        every { client.newCall(capture(slot)) } returns call
        every { call.execute() } returns response

        val api = ApiService(client, baseUrl)
        // Act & Assert - используем assertThrows
        val exception = Assert.assertThrows(IOException::class.java) {
            api.saveDeviceInfoChanges(token, newDeviceInfo)
        }

        assertEquals("Unexpected code $response", exception.message)

        Assert.assertTrue(exception.message?.contains("Unexpected code") == true)
        verify(exactly = 1) { call.execute() }
    }
}