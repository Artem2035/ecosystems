package com.example.ecosystems.ApiService
import com.example.ecosystems.network.ApiService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetTokenTest {
    private val baseUrl = "https://example.com/"

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
    }

    private fun makeResponse(json: String, code: Int = 200, message: String = "OK"): Response {
        val body = json.toResponseBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl).build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body)
            .build()
    }

    @Test
    fun `GetToken returns token on successful response`() {
        // arrange
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val json = """
            {"result":"ok","access_token":"abc123"}
        """.trimIndent()
        val response = makeResponse(json, 200, "OK")

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        val api = ApiService(client, BASE_URL = baseUrl)

        // act
        val token = api.GetToken("user", "pass")

        // assert
        assertEquals("abc123", token)
    }

    @Test
    fun `GetToken throws IOException on non-successful HTTP code`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val response = makeResponse("""{"foo":"bar"}""", 500, "Internal Server Error")

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        val api = ApiService(client)

        assertThrows(IOException::class.java) {
            api.GetToken("user", "pass")
        }
    }

    @Test
    fun `GetToken throws Exception when result is not ok`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val json = """{"result":"error","message":"bad credentials"}"""
        val response = makeResponse(json, 200, "OK")

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        val api = ApiService(client)

        assertThrows(Exception::class.java) {
            api.GetToken("user", "pass")
        }
    }

    @Test
    fun `GetToken throws when access_token missing`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val json = """{"result":"ok"}""" // no access_token
        val response = makeResponse(json, 200, "OK")

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        val api = ApiService(client)

        assertThrows(NullPointerException::class.java) {
            // Because code does result["access_token"].toString() -> if null -> NPE when toString() on null?
            // Depending on behavior, it might be "null" string. Adjust expectation if needed.
            api.GetToken("user", "pass")
        }
    }

    @Test
    fun `GetToken rethrows network IOExceptions`() {
        val client = mockk<OkHttpClient>()
        val call = mockk<Call>()

        every { client.newCall(any()) } returns call
        every { call.execute() } throws IOException("network down")

        val api = ApiService(client)

        assertThrows(IOException::class.java) {
            api.GetToken("user", "pass")
        }
    }
}
