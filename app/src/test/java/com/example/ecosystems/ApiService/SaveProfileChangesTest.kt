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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SaveProfileChangesTest {

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

    @Test
    fun `saveProfileChanges - when only some fields changed sends full merged object`() {
        // Arrange
        val token = "tokenABC"

        // Изначальные (старые) данные аккаунта
        val personalAccountData = mutableMapOf<String, Any?>(
            "id" to 42,
            "name" to "OldName",
            "second_name" to "OldSurname",
            "email" to "old@example.com",
            "phone" to "+100000000",
            "organization" to "OldOrg",
            "age" to 30,
            "country" to "LT"
        )

        // Пользователь изменил только эти поля (в UI вы делаете personalAccountData["name"] = ...)
        personalAccountData["name"] = "NewName"
        personalAccountData["second_name"] = "NewSurname"
        personalAccountData["email"] = "new@example.com"
        personalAccountData["phone"] = "+199999999"
        personalAccountData["organization"] = "NewOrg"
        // остальные поля (age, country, id) остаются прежними

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
        api.saveProfileChanges(token, personalAccountData)

        // Assert — проверим Request
        val req = slot.captured
        assertEquals("${baseUrl}api/v1/profile", req.url.toString())
        assertEquals("Bearer $token", req.header("Authorization"))
        assertEquals("application/json, text/plain, */*", req.header("Accept"))

        // Получим тело запроса
        val buffer = Buffer()
        req.body!!.writeTo(buffer)
        val bodyStr = buffer.readUtf8()

        // Ожидаем JSON вида {"user":{...}} и что в нём присутствуют:
        // — обновлённые поля с новыми значениями
        assertTrue(bodyStr.contains("\"name\":\"NewName\""))
        assertTrue(bodyStr.contains("\"second_name\":\"NewSurname\""))
        assertTrue(bodyStr.contains("\"email\":\"new@example.com\""))
        assertTrue(bodyStr.contains("\"phone\":\"+199999999\""))
        assertTrue(bodyStr.contains("\"organization\":\"NewOrg\""))

        // — а также старые/неизменённые поля сохраняются
        assertTrue(bodyStr.contains("\"age\":30"))
        assertTrue(bodyStr.contains("\"country\":\"LT\""))
        assertTrue(bodyStr.contains("\"id\":42"))

        // И call.execute() был вызван
        verify(exactly = 1) { call.execute() }
    }


    @Test
    fun `saveProfileChanges - when response not successful throws IOException`() {
        // Arrange
        val token = "tokenABC"
        val personalAccountData = mutableMapOf<String, Any?>("k" to "v")

        every { response.isSuccessful } returns false
        // тело необязательно, но можно вернуть мок:
        every { response.body } returns responseBody
        every { responseBody.string() } returns "error"

        every { client.newCall(any()) } returns call
        every { call.execute() } returns response

        val api = ApiService(client, baseUrl)

        // Act — ожидаем IOException (из вашего кода: throw IOException("Unexpected code $response"))
        Assert.assertThrows(IOException::class.java) {
            api.saveProfileChanges(token, personalAccountData)
        }
    }
}