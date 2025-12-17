package com.example.ApiService

import android.util.Log
import com.example.ecosystems.network.ApiService
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetPersonalAccountDataTest {

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
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `getPersonalAccountData returns user map when result ok`() {
        val api = ApiService(client, baseUrl)
        val slot = slot<Request>()

        every { client.newCall(capture(slot)) } returns call
        every { call.execute() } returns response

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody

        // JSON with result ok and user object
        val userJson = """{"id": 42, "name": "Ivan"}"""
        val fullJson = """{"result":"ok","user":$userJson}"""
        every { responseBody.string() } returns fullJson

        val token = "abc"
        val result = api.getPersonalAccountData(token)

        // Проверяем содержимое мапы
        assertEquals(42.0, result["id"]) // при парсинге через Gson числа становятся Double
        assertEquals("Ivan", result["name"])

        // Проверка URL/хедеров запроса
        val req = slot.captured
        assertEquals("${baseUrl}api/v1/profile", req.url.toString())
        assertEquals("Bearer  $token", req.header("Authorization")) // в исходном коде есть два пробела
        assertTrue(req.header("Accept")!!.contains("application/json"))
    }

    @Test
    fun `getPersonalAccountData throws IOException when response not successful`() {
        val api = ApiService(client, baseUrl)

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        every { response.isSuccessful } returns false
        every { response.toString() } returns "Response{...}"

        val token = "abc"

        val ex = assertThrows(IOException::class.java) {
            api.getPersonalAccountData(token)
        }
        assertTrue(ex.message!!.contains("Unexpected code"))
    }

    @Test
    fun `getPersonalAccountData throws Exception when result not ok`() {
        val api = ApiService(client, baseUrl)

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody

        // result != ok
        val badJson = """{"result":"error","message":"something wrong"}"""
        every { responseBody.string() } returns badJson

        val token = "abc"

        val ex = assertThrows(Exception::class.java) {
            api.getPersonalAccountData(token)
        }
        assertTrue(ex.message!!.contains("Error while making request"))
    }
}
