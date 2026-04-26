package com.example.ecosystems.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ecosystems.CryptoConfig
import credentialsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SecureCredentialsManager(private val context: Context) {

    companion object {
        private val LOGIN_KEY = stringPreferencesKey("credentials_login")
        private val PASSWORD_KEY = stringPreferencesKey("credentials_password")
    }

    // Используем тот же AEAD что и SecureTokenManager
    private val aead by lazy { CryptoConfig.getAead(context) }

    private fun encrypt(value: String): String {
        val ciphertext = aead.encrypt(value.toByteArray(), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val decoded = Base64.decode(encoded, Base64.NO_WRAP)
            val decrypted = aead.decrypt(decoded, null)
            String(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun save(login: String, password: String) {
        runBlocking {
            context.credentialsDataStore.edit { prefs ->
                prefs[LOGIN_KEY] = encrypt(login)
                prefs[PASSWORD_KEY] = encrypt(password)
            }
        }
    }

    fun loadLogin(): String? = runBlocking {
        val prefs = context.credentialsDataStore.data.first()
        val encoded = prefs[LOGIN_KEY] ?: return@runBlocking null
        decrypt(encoded)
    }

    fun loadPassword(): String? = runBlocking {
        val prefs = context.credentialsDataStore.data.first()
        val encoded = prefs[PASSWORD_KEY] ?: return@runBlocking null
        decrypt(encoded)
    }

    fun hasSaved(): Boolean = runBlocking {
        val prefs = context.credentialsDataStore.data.first()
        prefs.contains(LOGIN_KEY)
    }

    fun clear() {
        runBlocking {
            context.credentialsDataStore.edit { prefs ->
                prefs.remove(LOGIN_KEY)
                prefs.remove(PASSWORD_KEY)
            }
        }
    }
}