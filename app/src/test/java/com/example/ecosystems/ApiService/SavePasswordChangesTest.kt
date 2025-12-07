package com.example.ecosystems.ApiService

import android.util.Log
import com.example.ecosystems.network.ApiService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SavePasswordChangesTest {

    private lateinit var client: OkHttpClient
    private lateinit var call: Call
    private lateinit var response: Response
    private lateinit var responseBody: ResponseBody

    private val baseUrl = "https://example.com/"
    private lateinit var api: ApiService

    @Before
    fun setUp() {
        client = mockk()
        call = mockk()
        response = mockk(relaxed = true) // relaxed для close() и пр.
        responseBody = mockk()

        api = ApiService(client, baseUrl)

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @Test
    fun `savePasswordChanges - successful response and result ok`() {
        // Arrange
        val oldPass = "old123"
        val newPass = "new456"
        val token = "tokenABC"
        val userId = "42"

        val jsonResponse = """{"result":"ok"}"""

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns jsonResponse

        // Capture request to assert its fields (URL, headers, body)
        val slot = slot<Request>()
        every { client.newCall(capture(slot)) } returns call

        // Act
        api.savePasswordChanges(oldPass, newPass, token, userId)
        // Assert — убедимся, что newCall был вызван и захватим Request
        verify(exactly = 1) { client.newCall(any()) }
        verify(exactly = 1) { call.execute() }

        val req = slot.captured
        Assert.assertEquals("${baseUrl}api/v1/profile/change_password", req.url.toString())
        Assert.assertEquals("POST", req.method)
        Assert.assertEquals("Bearer $token", req.header("Authorization"))
        Assert.assertEquals("application/json", req.header("Content-Type"))

        // read body
        val buffer = Buffer()
        req.body?.writeTo(buffer)
        val bodyStr = buffer.readUtf8()
        Assert.assertTrue(bodyStr.contains("\"old_password\":\"$oldPass\""))
        Assert.assertTrue(bodyStr.contains("\"new_password\":\"$newPass\""))
        Assert.assertTrue(bodyStr.contains("\"id_user\":$userId"))
    }

    @Test
    fun `savePasswordChanges - constructs correct request (url, headers, body)`() {
        // Arrange
        val oldPass = "old123"
        val newPass = "new456"
        val token = "tokenABC"
        val userId = "42"

        val jsonResponse = """{"result":"ok"}"""

        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns jsonResponse

        // capture the Request passed to newCall
        val slot = slot<Request>()
        every { client.newCall(capture(slot)) } returns call

        // Act
        api.savePasswordChanges(oldPass, newPass, token, userId)

        // Assert
        val req = slot.captured
        Assert.assertEquals("${baseUrl}api/v1/profile/change_password", req.url.toString())
        Assert.assertEquals("POST", req.method)
        Assert.assertEquals("Bearer $token", req.header("Authorization"))
        Assert.assertEquals("application/json", req.header("Content-Type"))

        // read body
        val buffer = Buffer()
        req.body?.writeTo(buffer)
        val bodyStr = buffer.readUtf8()
        Assert.assertTrue(bodyStr.contains("\"old_password\":\"$oldPass\""))
        Assert.assertTrue(bodyStr.contains("\"new_password\":\"$newPass\""))
        Assert.assertTrue(bodyStr.contains("\"id_user\":$userId"))
    }

    @Test
    fun `savePasswordChanges - non successful response throws IOException`() {
        // Arrange
        every { response.isSuccessful } returns false
        every { response.code } returns 500 // optional

        // Act & Assert
        val thrown = Assert.assertThrows(IOException::class.java) {
            api.savePasswordChanges("a", "b", "t", "1")
        }
        Assert.assertTrue(thrown.message?.contains("Unexpected code") == true)
    }

    @Test
    fun `savePasswordChanges - result not ok throws Exception`() {
        // Arrange
        val jsonResponse = """{"result":"fail","error":"bad"}"""
        every { response.isSuccessful } returns true
        every { response.body } returns responseBody
        every { responseBody.string() } returns jsonResponse

        // Act & Assert
        val thrown = Assert.assertThrows(Exception::class.java) {
            api.savePasswordChanges("a", "b", "t", "1")
        }
        Assert.assertEquals(
            "Введенный текущий пароль не совпадает с паролем аккаунта!",
            thrown.message
        )
    }

    @Test
    fun `savePasswordChanges - null body leads to NPE or custom handling`() {
        // Arrange
        every { response.isSuccessful } returns true
        every { response.body } returns null

        // Act & Assert
        Assert.assertThrows(NullPointerException::class.java) {
            api.savePasswordChanges("a", "b", "t", "1")
        }
    }
}