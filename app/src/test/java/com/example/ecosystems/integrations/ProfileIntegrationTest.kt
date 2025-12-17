package com.example.ecosystems.integrations

import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.Parser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test


class ProfileIntegrationTest {

    private lateinit var apiService: ApiService
    private lateinit var parser: Parser

    @Before
    fun setup() {
        apiService = mockk()
        parser = Parser()
    }

    @Test
    fun `integration test api parser with ok result`() {
        val personalAccountData: MutableMap<String, Any?> = mutableMapOf(
            "id_user" to 1,
            "name" to "Bob",
            "second_name" to "Bob2",
            "email" to "bob@mail.com",
            "phone" to "1234",
            "organization" to "org",
            "is_send_emails_not_devices_link" to false
        )
        every { apiService.getPersonalAccountData(any()) } returns personalAccountData

        // WHEN (1) API
        val token = "abc"
        val profile = apiService.getPersonalAccountData(token)

        // WHEN (2) parsing
        val profile2 = parser.profileParse(profile)

        // THEN
        Assert.assertEquals(1, profile2.id_user)
        Assert.assertEquals("Bob", profile2.name)
        Assert.assertEquals("Bob2", profile2.second_name)
        Assert.assertEquals("bob@mail.com", profile2.email)
        Assert.assertEquals("1234", profile2.phone)
        Assert.assertEquals("org", profile2.organization)
        Assert.assertEquals(false, profile2.is_send_emails_not_devices_link)
    }

    // 2. Проверка выброса исключения при отсутствии ключа id_user
    @Test(expected = NoSuchFieldException::class)
    fun `should throw exception when id_user is missing`() {
        val data = mutableMapOf<String, Any?>(
            "name" to "Alice"
        )
        parser.profileParse(data)
    }

    // 3. Проверка парсинга с пустыми полями (должны быть дефолтные)
    @Test
    fun `should parse profile with default values when fields missing`() {
        val data = mutableMapOf<String, Any?>(
            "id_user" to 2
        )
        val profile = parser.profileParse(data)

        assertEquals(2, profile.id_user)
        assertEquals("", profile.name)
        assertEquals("", profile.second_name)
        assertEquals("", profile.email)
        assertEquals("", profile.phone)
        assertEquals("", profile.organization)
        assertFalse(profile.is_send_emails_not_devices_link)
    }

    // 4. Проверка работы с разными типами данных
    @Test
    fun `should parse profile correctly with numeric id_user as string`() {
        val data = mutableMapOf<String, Any?>(
            "id_user" to "10",
            "name" to "John"
        )
        val profile = parser.profileParse(data)

        assertEquals(10, profile.id_user)
        assertEquals("John", profile.name)
    }

    // 5. Парсинг с минимальным набором данных (дефолтные значения)
    @Test
    fun `should use default values when optional fields are missing`() {
        val data = mutableMapOf<String, Any?>(
            "id_user" to 2,
            "name" to "Bob",
            "second_name" to "Bob2",
            "email" to "bob@mail.com"
        )

        val profile = parser.profileParse(data)

        assertEquals(2, profile.id_user)
        assertEquals("Bob", profile.name)
        assertEquals("Bob2", profile.second_name)
        assertEquals("bob@mail.com", profile.email)
        assertEquals("", profile.phone)
        assertEquals("", profile.organization)
        assertFalse(profile.is_send_emails_not_devices_link)
    }
}