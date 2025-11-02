package com.example.ecosystems.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ecosystems.CryptoConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class SecureDevicesParametersManager(private val context: Context) {

    companion object {
        private val KEY_DATA = stringPreferencesKey("devices_parameters_data")
    }
    private val aead by lazy { CryptoConfig.getAead(context) }

    // Сохраняем  в DataStore (зашифрованным JSON)
    fun saveData(data: MutableMap<String, MutableMap<*, *>>) {
        // ключи Int в String
        val tempDevices = mutableMapOf<String, Any?>()
        (data.get("mapOfDevices") as MutableMap<Int, Map<String, Any?>>).forEach { (key, value) ->
            tempDevices[key.toString()] = JSONObject(value)
        }

        val newData = mutableMapOf<String, MutableMap<String, *>>()
        newData.put("mapOfDevices", tempDevices)
        newData.put("mapOfDeviceParameters",
            data.get("mapOfDeviceParameters") as MutableMap<String, *>
        )
        val json = JSONObject(newData as Map<*, *>).toString()
        val encrypted = aead.encrypt(json.toByteArray(), null)
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.DEFAULT)

        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[KEY_DATA] = encryptedBase64
            }
        }
    }

    // Загружаем данные обратно в Map<String, Any?>
    fun loadData(): MutableMap<String, MutableMap<*, *>> {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            val encryptedBase64 = prefs[KEY_DATA] ?: return@runBlocking mutableMapOf()

            val encrypted = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val decrypted = aead.decrypt(encrypted, null).decodeToString()

            val json = JSONObject(decrypted)
            val map = mutableMapOf<String, MutableMap<*, *>>()
            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            json.keys().forEach { key ->
                val innerJson = json.getJSONObject(key)
                if( key == "mapOfDevices"){
                    val innerMap = mutableMapOf<Int, Any?>()
                    innerJson.keys().forEach { innerKey ->
                        val result: Map<String, Any?> = mapAdapter.fromJson(innerJson.get(innerKey).toString())
                        //innerMap[innerKey.toInt()] = innerJson.get(innerKey)
                        innerMap[innerKey.toInt()] = result
                    }

                    map[key] = innerMap
                }else{
                    val innerMap = mutableMapOf<String, Any?>()
                    innerJson.keys().forEach { innerKey ->
                        val result: Map<String, Any?> = mapAdapter.fromJson(innerJson.get(innerKey).toString())
                        //innerMap[innerKey] = innerJson.get(innerKey)
                        innerMap[innerKey] = result
                    }
                    map[key] = innerMap
                }
            }
            map
        }
    }

    // Очистка всех данных
    fun clear() {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }

}