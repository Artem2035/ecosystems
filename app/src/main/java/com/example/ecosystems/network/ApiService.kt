package com.example.ecosystems.network

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ecosystems.DataClasses.CreatePointRequest
import com.example.ecosystems.DataClasses.DeviceInfo
import com.example.ecosystems.DataClasses.UpdatePointRequest
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException

val BASE_URL: String = "https://smartecosystems.petrsu.ru/"

class ApiService(private val client: OkHttpClient = OkHttpClient.Builder().build()) {

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

    /*обновление данных устройства*/
    fun saveDeviceInfoChanges(token: String, deviceInfo: DeviceInfo){
        Log.d("saveDeviceInfoChanges", "$deviceInfo")
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id",                  deviceInfo.id.toString())
            .addFormDataPart("name",                deviceInfo.name)
            .addFormDataPart("description",         deviceInfo.description)
            .addFormDataPart("serial_number",       deviceInfo.serialNumber)
            .addFormDataPart("location_description",deviceInfo.locationDescription)
            .addFormDataPart("latitude",            deviceInfo.latitude.toString())
            .addFormDataPart("longitude",           deviceInfo.longitude.toString())
            .addFormDataPart("device_type_id",      deviceInfo.deviceTypeId.toString())
            .addFormDataPart("module_type_id",      deviceInfo.moduleTypeId.toString())
            .addFormDataPart("file_format", deviceInfo.fileFormat)
            .addFormDataPart("tz",                  deviceInfo.tz.toString())
            .addFormDataPart("time_not_online",     deviceInfo.timeNotOnline.toString())
            .addFormDataPart("is_public",           if (deviceInfo.isPublic) "1" else "0")
            .addFormDataPart("is_allow_download",   if (deviceInfo.isAllowDownload) "1" else "0")
            .addFormDataPart("is_verified",         if (deviceInfo.isVerified) "1" else "0")
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}api/v1/update_device_info")
            .post(requestBody)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        }
    }

    /*получения планов для карт для таксации леса*/
    fun loadPlans(token: String): String{
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/objects?timeoffset=-3&is_admin=false")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("Ошибка: ${response.code}")

            return response.body!!.string()
        }
    }

    /*получить все слои гис объекта*/
    fun loadPlanLayers(token: String, uuid: String): String{
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/layers/${uuid}")
            .header("Accept", "*/*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Ошибка: ${response.code}")
            }
            response.body?.string() ?: throw Exception("Empty body")
        }
    }

    fun loadPlanLayersRaw(token: String, uuid: String): okhttp3.ResponseBody {
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/layers/${uuid}")
            .header("Authorization", "Bearer $token")
            .header("Accept", "*/*")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            response.close()
            throw IOException("Ошибка: ${response.code}")
        }

        // Возвращаем body — НЕ закрываем response здесь
        return response.body ?: run {
            response.close()
            throw IOException("Пустое тело ответа")
        }
    }

    /*регистрация нового аккаунта*/
    fun profileRegistration(profileInfo: MutableMap<String, Any?>):String{
        val MEDIA_TYPE = "application/json".toMediaType()

        val requestBody = "{\"user\": ${Gson().toJson(profileInfo)}}"
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/profile/registration")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            Log.d("result", "${response}")
            if (!response.isSuccessful){
                throw IOException("Unexpected code: ${response.message}")
            }
            val requestResult = response.body!!.string()
            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.getOrDefault("result", "") != "ok"){
                throw Exception("Ошибка: ${result.getOrDefault("description", "неизвестная ошибка!")}")
            }

            return result.getOrDefault("description", "Запрос выполнен!").toString()
        }
    }
    /*загрузить данные всех гис объектов, всех слоев (в том числе точки, и фото) в базу данных*/
    fun loadTables(token: String): ResponseBody {
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/tables?timeoffset=-3")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Ошибка: ${response.code}, ${response.message}")
        return response.body ?: throw IOException("Пустое тело ответа")
    }

    /* Создать новую точку на сервере*/
    fun saveNewPoint(token: String, request: CreatePointRequest): Int {
        val gson = Gson()

        val jsonObject = gson.toJsonTree(request).asJsonObject
        // Добавляем все пары из values в корень JSON
        request.values.forEach { (key, value) ->
            jsonObject.add(key, gson.toJsonTree(value))
        }

        val json = gson.toJson(jsonObject)

        val body = json.toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/tree_points/${request.layer_uuid}")
            .header("Authorization", "Bearer ${token}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.use { it.body?.string() } ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        // Сервер возвращает объект точки, возвращает новый объект
        val result = gson.fromJson(responseBody, Map::class.java)
        val point = result["point"] as Map<String, *>
        return (point["id"] as Number).toInt()
    }

    /*сохрнаить изменения для точки слоя типа points на сервере*/
    fun savePointChanges(token: String, request: UpdatePointRequest, oldValues: String) {
        val gson = Gson()
        val jsonObject = gson.toJsonTree(request).asJsonObject
        // Добавляем все пары из values в корень JSON
        request.values.forEach { (key, value) ->
            jsonObject.add(key, gson.toJsonTree(value))
        }

        // Парсим valuesJson
        val valuesTemplate = gson.fromJson(oldValues, JsonObject::class.java)

        // Добавляем отсутствующие поля как ""
        valuesTemplate.entrySet().forEach { (key, _) ->
            if (!request.values.containsKey(key)) {
                jsonObject.addProperty(key, "")
            }
        }

        val json = gson.toJson(jsonObject)

        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/tree_points/${request.layer_uuid}")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${token}")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            val responseBody = response.body?.string()
            throw Exception("HTTP ${response.code}: $responseBody")
        }
    }

    /* Создать новые точки на сервере*/
    fun savePointsBatch(token: String, layerUUID: String, requests: List<CreatePointRequest>) {
        val gson = Gson()

        // Для каждой точки — объединяем стандартные поля и values в плоский объект
        val pointsArray = requests.map { request ->
            // Сериализуем стандартные поля через JsonObject
            val pointObject = JsonObject().apply {
                addProperty("id", request.id)
                addProperty("num", request.num)
                addProperty("lat", request.lat)
                addProperty("lng", request.lng)
                addProperty("layer_uuid", request.layer_uuid)
                addProperty("values", "{}")
            }

            // Разворачиваем values в корень объекта точки
            request.values.forEach { (key, value) ->
                pointObject.add(key, gson.toJsonTree(value))
            }

            pointObject
        }

        val root = JsonObject().apply {
            add("points", gson.toJsonTree(pointsArray))
        }

        val json = gson.toJson(root)
        Log.d("Sync root", "${root} ")
        Log.d("Sync json", "${json} ")
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/tree_points/${layerUUID}/batch")
            .header("Authorization", "Bearer ${token}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.use { it.body?.string() } ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }
    }

    /*удаление точки по id и layerUUID*/
    fun deletePoint(token: String, layerUUID: String, pointId: Int){
        Log.d("Sync d", "${pointId} ")
        val request = Request.Builder()
            .url("${BASE_URL}api/v1/orthophotoplans/tree_points/${layerUUID}/${pointId}")
            .delete("".toRequestBody())
            .header("Authorization", "Bearer ${token}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val responseBody = response.body?.string() // .string() закрывает body
                throw Exception("HTTP ${response.code}: $responseBody")
            }
        }
    }
}
