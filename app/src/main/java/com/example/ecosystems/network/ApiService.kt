package com.example.ecosystems.network

import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ApiService(private val client: OkHttpClient = OkHttpClient.Builder().build(),
                 private val BASE_URL: String = "https://smartecosystems.petrsu.ru/") {
//    val BASE_URL = "https://smartecosystems.petrsu.ru/"
//    val client: OkHttpClient by lazy {
//        OkHttpClient.Builder()
//            .build()
//    }

    @WorkerThread
    fun getDevices(token: String): String {
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/devices_lite?timeoffset=-3&device_type=NaN")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("Error: ${response.code}")

            return response.body!!.string()
        }
    }

    @WorkerThread
    fun GetToken(login: String, password: String):String
    {
        var token = ""

        val MEDIA_TYPE = "application/json".toMediaType()
        val requestBody = "{\"login\":\"${login}\",\"password\":\"${password}\"}"

        val request = Request.Builder()
            .url("${BASE_URL}api/v1/token")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                Log.d("Error","Unexpected code $response ${response.message} ${response.code} ${response.body}")
                throw IOException("Unexpected code ${response.message}")
            }
            val requestResult = response.body!!.string()

            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Error while making request: result.get")
                throw Exception("Error while making request: result.get")
            }

            token = result.get("access_token")!!.toString()
            Log.d("Token","token = ${token}")
        }
        return token
    }

    fun getPersonalAccountData(token: String): MutableMap<String, Any?> {
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/profile")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer  ${token}")
            .header("Connection", "keep-alive")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                Log.d("Error","Unexpected code $response")
                throw IOException("Unexpected code $response")
            }

            val requestResult = response.body!!.string()

            Log.d("requestResult", requestResult)
            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Unexpected code $response")
                throw Exception("Error while making request: result.get")
            }
            val PersonalAccountData = result.get("user")!!
            return PersonalAccountData as MutableMap<String, Any?>
        }
    }

    fun savePasswordChanges(oldPassword: String, newPassword: String, token: String, userId: String){
        val MEDIA_TYPE = "application/json".toMediaType()


        val requestBody = "{\"schema\":{\"old_password\":\"${oldPassword}\",\"new_password\":\"${newPassword}\",\"id_user\":${userId}}}"

        val request = Request.Builder()
            .url("${BASE_URL}api/v1/profile/change_password")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val requestResult = response.body!!.string()

            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Error while making request: result.get")
                throw Exception("Введенный текущий пароль не совпадает с паролем аккаунта!")
            }
        }
    }

    /*для обновления профиля и фрагмента с настройками*/
    fun saveProfileChanges(token: String, personalAccountData: MutableMap<String, Any?>){
        val MEDIA_TYPE = "application/json".toMediaType()
        val requestBody = "{\"user\": ${Gson().toJson(personalAccountData)}}"

        Log.d("saveProfileChanes", requestBody)
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/profile")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            Log.d("saveProfileChanges", response.body!!.string())
        }
    }

    fun saveDeviceInfoChanges(token: String, newDeviceInfo: MutableMap<String, Any?>){
        Log.d("saveDeviceInfoChanges","${token} ${newDeviceInfo}")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("name", newDeviceInfo.getValue("name").toString())
            .addFormDataPart("description", newDeviceInfo.getOrDefault("description", "").toString())
            .addFormDataPart("id", newDeviceInfo.getValue("id").toString().toInt().toString())
            .addFormDataPart("latitude", newDeviceInfo.getOrDefault("latitude", "0.0").toString().toDouble().toString())
            .addFormDataPart("longitude", newDeviceInfo.getOrDefault("longitude", "0.0").toString().toDouble().toString())
            .addFormDataPart("device_type_id", newDeviceInfo.getValue("device_type_id").toString().toDouble().toInt().toString())
            .addFormDataPart("location_description", newDeviceInfo.getOrDefault("location_description", "").toString())
            .addFormDataPart("serial_number", newDeviceInfo.getValue("serial_number").toString())
            .addFormDataPart("is_public", newDeviceInfo.getOrDefault("is_public", "0.0").toString().toDouble().toInt().toString())
            .addFormDataPart("is_allow_download", newDeviceInfo.getOrDefault("is_allow_download", "0.0").toString().toDouble().toInt().toString())
            .addFormDataPart("is_verified", newDeviceInfo.getOrDefault("is_verified", "0.0").toString().toDouble().toInt().toString())
            .addFormDataPart("file_format", "undefined")
            .addFormDataPart("module_type_id", newDeviceInfo.getOrDefault("module_type_id", 1).toString())
            .addFormDataPart("tz", newDeviceInfo.getOrDefault("tz", "0").toString().toDouble().toInt().toString())
            .addFormDataPart("time_not_online", newDeviceInfo.getOrDefault("time_not_online", "0").toString().toDouble().toInt().toString())
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}api/v1/update_device_info")
            .post(requestBody)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .header("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundaryLavh0kSsI9bU3EIy")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            //response.body!!.string()
        }
    }
}