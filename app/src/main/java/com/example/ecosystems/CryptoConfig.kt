package com.example.ecosystems

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

object CryptoConfig {
    private const val MASTER_KEY_URI = "android-keystore://tink_master_key"
    private const val KEYSET_NAME = "tink_keyset"
    private const val PREF_FILE_NAME = "tink_prefs"

    /**
     * Возвращает AEAD-примитив для шифрования/дешифрования.
     * Кэшируется (lazy) и создаётся один раз на всё приложение.
     */
    fun getAead(context: Context): Aead {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(Aead::class.java)
    }
}