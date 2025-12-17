package com.example.api

import com.example.ecosystems.network.ApiService
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetDevicesTest {

    private lateinit var client: OkHttpClient
    private lateinit var call: Call
    private lateinit var response: Response
    private lateinit var responseBody: ResponseBody

    private val baseUrl = "https://example.com/"

    @Before
    fun setUp() {
        client = mockk()
        call = mockk()
        response = mockk()
        responseBody = mockk()
        every { response.close() } just Runs
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `getDevices returns body string when response successful`() {
        val api = ApiService(client, baseUrl)
        val slot = slot<Request>()

        every { client.newCall(capture(slot)) } returns call
        every { call.execute() } returns response

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns """{"devices":["a","b"]}"""

        val token = "token123"
        val result = api.getDevices(token)

        assertEquals("""{"devices":["a","b"]}""", result)

        // Проверяем, что Request был с корректным URL и заголовком Authorization
        val req = slot.captured
        assertEquals("${baseUrl}api/v1/devices_lite?timeoffset=-3&device_type=NaN", req.url.toString())
        assertEquals("Bearer $token", req.header("Authorization"))
        assertEquals("application/json", req.header("Accept"))

        verify { client.newCall(any()) }
        verify { call.execute() }
    }

    @Test
    fun `getDevices throws IOException when response not successful`() {
        val api = ApiService(client, baseUrl)
        val slot = slot<Request>()

        every { client.newCall(capture(slot)) } returns call
        every { call.execute() } returns response

        every { response.isSuccessful } returns false
        every { response.code } returns 500

        val token = "token123"

        val ex = assertThrows(IOException::class.java) {
            api.getDevices(token)
        }
        // Сообщение содержит код ошибки
        assert(ex.message!!.contains("Error: 500"))

        verify { client.newCall(any()) }
        verify { call.execute() }
    }
}
