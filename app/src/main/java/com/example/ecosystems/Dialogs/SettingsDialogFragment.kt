package com.example.ecosystems.SettingsDialogFragment

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.Dialogs.ProgressDialogFragment
import com.example.ecosystems.R
import com.example.ecosystems.db.dto.TableDto
import com.example.ecosystems.db.dto.layer.LayerImageDto
import com.example.ecosystems.db.dto.layer.LayerPointDto
import com.example.ecosystems.db.dto.layer.toEntity
import com.example.ecosystems.db.dto.toEntity
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerImageEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.syncQueue.SyncManager
import com.example.ecosystems.db.entity.table.TableEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import com.example.ecosystems.db.repository.TableRepository
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsDialogFragment(private val token:String,
                             private var layerRepository: LayerRepository,
                             private var planRepository: PlanRepository,
                             private var tableRepository: TableRepository,
                             private val syncManager: SyncManager) : DialogFragment() {
    private val api: ApiService = ApiService()
    private var progressDialog: ProgressDialogFragment? = null
    private lateinit var syncButton: Button

    val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        java.util.Locale.ENGLISH)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_settings, null)

        val button = view.findViewById<Button>(R.id.downloadButton)
        val backButton = view.findViewById<Button>(R.id.backButton)
        syncButton = view.findViewById<Button>(R.id.syncButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        syncButton.setOnClickListener {
            if (!requireContext().isInternetAvailable()) {
                Toast.makeText(requireContext(), "Нет подключения к сети", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startSync()
        }

        button.setOnClickListener {
            Log.d("SettingsDialogFragment", "SettingsDialogFragment")
            if(requireContext().isInternetAvailable()){

                val gson = Gson()
                val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
                try {
                    clearLocalData()
                    loadTables()

                    progressDialog = ProgressDialogFragment()
                    progressDialog?.show(parentFragmentManager, "progress")

                    val planFileEntities = mutableListOf<PlanFileEntity>()
                    val planEntities = mutableListOf<PlanEntity>()

                    lifecycleScope.launch(Dispatchers.IO) {
                        var result: Map<String, Any?> = mapAdapter.fromJson(api.loadPlans(token))
                        val listOfPlans= result.get("plans") as MutableList<Map<String, Any?>>

                        planEntities.addAll(listOfPlans.map { plan ->
                            val createdAt = formatter.parse(plan["created_at"] as String)?.time ?: 0L
                            val updatedAt = formatter.parse(plan["updated_at"] as String)?.time ?: 0L

                            val files = plan["files"] as? List<Map<String, Any?>> ?: emptyList()
                            planFileEntities.addAll(files.map { file ->

                                PlanFileEntity(
                                    id = (file["id"] as Number).toInt(),

                                    gisObjectId = (file["gis_object_id"] as Number).toInt(),

                                    uuid = file["uuid"] as String,
                                    name = file["name"] as String,
                                    description = file["description"] as String?,

                                    originalFilename = file["original_filename"] as String?,
                                    fileInfoUploadFilename = file["file_info_upload_filename"] as String?,
                                    fileInfoSize = (file["file_info_size"] as? Number)?.toLong(),

                                    formatType = (file["format_type"] as Number).toInt(),
                                    statusType = (file["status_type"] as Number).toInt(),

                                    gisCategoryId = (file["gis_category_id"] as Number).toInt(),
                                    gisCategoryTypeId = (file["gis_category_type_id"] as? Number)?.toInt(),

                                    droneDeviceId = (file["drone_device_id"] as? Number)?.toInt(),
                                    droneName = file["drone_name"] as String?,
                                    errorDescription = file["error_description"] as String?,

                                    hasReducedFile = file["has_reduced_file"] as? Boolean,

                                    centerLat = (file["center_lat"] as? Number)?.toDouble(),
                                    centerLng = (file["center_lng"] as? Number)?.toDouble(),

                                    bound1Lat = (file["bound_1_lat"] as? Number)?.toDouble(),
                                    bound1Lng = (file["bound_1_lng"] as? Number)?.toDouble(),
                                    bound2Lat = (file["bound_2_lat"] as? Number)?.toDouble(),
                                    bound2Lng = (file["bound_2_lng"] as? Number)?.toDouble(),

                                    year = (file["year"] as? Number)?.toInt(),

                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            })

                            PlanEntity(
                                id = (plan["id"] as Number).toInt(),
                                uuid = plan["uuid"] as String,
                                name = plan["name"] as String,
                                description = plan["description"] as String?,
                                accessType = (plan["access_type"] as Number).toInt(),
                                categoryId = (plan["category_id"] as? Number)?.toInt(),
                                userId = (plan["user_id"] as Number).toInt(),
                                isOwner = plan["is_owner"] as Boolean,
                                canEdit = plan["can_edit"] as Boolean,
                                accountIds = Gson().toJson(plan["account_ids"]),
                                accounts = Gson().toJson(plan["accounts"]),
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        })

                        planRepository.insertAll(planEntities)
                        planRepository.insertAllFiles(planFileEntities)

                        loadPlanLayers(planEntities)
                    }

                    Log.d("TAG_DG1","1")

                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(requireContext(),"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
            }else{
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(requireContext(),"Нет доступа к интернету!",Toast.LENGTH_SHORT)
                    message.show()
                }
            }
        }
        backButton.setOnClickListener {
            dismiss()
        }

/*        lifecycleScope.launch(Dispatchers.IO){
            val t1 = layerRepository.getAllPointsRaw()?.first()
            t1?.forEach { a ->
                Log.d("pv1", "PointValueEntity = ${a}")
            }

            val t = layerRepository.getAllPointsWithValues()?.first()
            t?.forEach { a ->
                Log.d("pv", "PointValueEntity = ${a.point}")
                Log.d("pv", "values = ${a.values}")
                a.values.forEach { e ->
                    Log.d("pv", "name = ${e.property.name}, v = ${e.value.value}")
                }
            }
        }*/

        return dialog
    }

    private fun startSync(){
        progressDialog = ProgressDialogFragment()
        progressDialog?.show(parentFragmentManager, "sync_progress")
        lifecycleScope.launch{
            val pending = layerRepository.getPending()
            pending.forEach { a->
                Log.d("pv", "PointValueEntity = ${a.operation}")
                Log.d("pv", "PointValueEntity = ${a}")
            }

            if (pending.isEmpty()) {
                Toast.makeText(requireContext(), "Нет изменений для синхронизации", Toast.LENGTH_SHORT).show()
                return@launch
            }

            launch {
                syncManager.progress.collect { progress ->
                    // Уже на Main потоке не нужен withContext — collect вызывается в контексте launch
                    progressDialog?.updateProgress(progress.current, progress.total)
                }
            }

            isCancelable = false
            syncButton.isEnabled = false
            Toast.makeText(requireContext(), "Синхронизация...", Toast.LENGTH_SHORT).show()

            val result = withContext(Dispatchers.IO) {
                syncManager.syncPendingChanges()
            }

            // Синхронизация завершена — закрываем диалог и показываем итог
            progressDialog?.dismiss()
            progressDialog = null

            syncButton.isEnabled = true
            isCancelable = true

            pending.forEach { a ->
                Log.d("getPending", "${a}")
            }

            when (result) {
                is SyncManager.SyncResult.Success ->
                    Toast.makeText(requireContext(), "Данные отправлены ✓", Toast.LENGTH_SHORT).show()

                is SyncManager.SyncResult.PartialFailure ->
                    Toast.makeText(
                        requireContext(),
                        "Отправлено частично. Не удалось: ${result.failedCount}",
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }

    private fun loadTables(){
        lifecycleScope.launch(Dispatchers.IO) {

            val tablesEntities = mutableListOf<TableEntity>()
            val tablePropertiesEntities = mutableListOf<TablePropertyEntity>()

            val gson = Gson()
            api.loadTables(token).use { body ->
                // JsonReader читает токен за токеном без загрузки всего JSON
                val reader = com.google.gson.stream.JsonReader(body.charStream())

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "tables" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                // Читаем один layer как Map — только один объект в RAM
                                val tableDto: TableDto = gson.fromJson(reader, TableDto::class.java)
                                tablesEntities.add(tableDto.toEntity())
                                tableDto.properties?.forEach {
                                    tablePropertiesEntities.add(it.toEntity())
                                }
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                tableRepository.insertTablesWithProperties(tablesEntities, tablePropertiesEntities)
            }
        }
    }

    private fun loadPlanLayers(planEntities: List<PlanEntity>){
        val max = planEntities.size
        val gson = Gson()
        Log.d("status down", "размер = ${max}")
        var count = 0
        lifecycleScope.launch(Dispatchers.IO) {
            planEntities.forEach {plan->
                ensureActive()

                api.loadPlanLayersRaw(token, plan.uuid).use { body ->
                    val pointsEntities = mutableListOf<LayerPointEntity>()
                    val imagesEntities = mutableListOf<LayerImageEntity>()
                    val layerEntities = mutableListOf<LayerEntity>()

                    val pointValuesEntities = mutableListOf<PointValueEntity>()
                    // JsonReader читает токен за токеном без загрузки всего JSON
                    val reader = com.google.gson.stream.JsonReader(body.charStream())

                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "layers" -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    // Читаем один layer как Map — только один объект в RAM
                                    val layer = gson.fromJson<Map<String, Any?>>(
                                        reader,
                                        object : TypeToken<Map<String, Any?>>() {}.type
                                    )

                                    val json = layer["data"] as? Map<String, Any?> ?: emptyMap()
                                    val dataJson = json
                                        .filterKeys { it !in setOf("images", "points", "shape_objects", "shooting_objects", "projective_coverage") }
                                        .mapValues { (_, v) ->
                                            if (v is Map<*, *> || v is List<*>) gson.toJson(v) else v
                                        }

                                    val tableId = when (val value = layer["table_id"]) {
                                        is Double -> {
                                            val t = value.toInt()
                                            if(t == 0)
                                                null
                                            else
                                                t
                                        }
                                        is String -> value.toIntOrNull()
                                        else -> null
                                    }

                                    val layerEnt = LayerEntity(
                                        id = layer["id"].toString().toDouble().toInt(),
                                        uuid = layer["uuid"] as String,
                                        gisObjectId = layer["gis_object_id"].toString().toDouble().toInt(),
                                        gisObjectFileId = layer["gis_object_file_id"].toString().toDoubleOrNull()?.toInt(),
                                        name = layer["name"] as String,
                                        color = layer["color"] as? String,
                                        type = layer["type"] as String,
                                        order = layer["order"].toString().toIntOrNull(),
                                        parentId = layer["parent_id"].toString().toIntOrNull(),
                                        tableId = tableId,
                                        createdAt = formatter.parse(layer["created_at"] as String)?.time ?: 0L,
                                        updatedAt = formatter.parse(layer["updated_at"] as String)?.time ?: 0L,
                                        dataJson = gson.toJson(dataJson),
                                        cropEnabled = layer["crop_enabled"] as Boolean,
                                        cropPercent = (layer["crop_percent"] as Number).toDouble(),
                                        syncedAt = System.currentTimeMillis()
                                    )

                                    when (layerEnt.type) {
                                        "points" -> {
                                            val points = gson.fromJson<List<LayerPointDto>>(
                                                gson.toJson(json["points"]),
                                                object : TypeToken<List<LayerPointDto>>() {}.type
                                            ) ?: emptyList()

                                            points.mapTo(pointsEntities) { point->
                                                val values = point.values
                                                if (!values.isNullOrEmpty() && layerEnt.tableId != null) {
                                                    values.mapTo(pointValuesEntities){ value ->
                                                        PointValueEntity(
                                                            point.id,
                                                            tableRepository.getTablePropertyIdByName(
                                                                layerEnt.tableId, value.key),
                                                            value.value.toString()
                                                        )
                                                    }
                                                }
                                                point.toEntity()
                                            }
                                        }
                                        "library_images" -> {
                                            val images = gson.fromJson<List<LayerImageDto>>(
                                                gson.toJson(json["images"]),
                                                object : TypeToken<List<LayerImageDto>>() {}.type
                                            ) ?: emptyList()

                                            images.mapTo(imagesEntities) { it.toEntity() }
                                        }
                                    }
                                    layerEntities.add(layerEnt)
                                }
                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    layerRepository.insertAllData(layerEntities, pointsEntities, imagesEntities, pointValuesEntities)
                    count += 1
                    withContext(Dispatchers.Main) {
                        progressDialog?.updateProgress(count, max)
                    }
                    Log.d("status down", "скачано ${count} из ${planEntities.size}")
                }
            }

            progressDialog?.dismiss()
            Handler(Looper.getMainLooper()).post{
                val message = Toast.makeText(requireContext(),"Данные скачаны!",Toast.LENGTH_SHORT)
                message.show()
            }
        }
    }
    //очистить планы перед загрузкой с сервера
    private fun clearLocalData() {
        lifecycleScope.launch(Dispatchers.IO){
            planRepository.deleteAll()
            tableRepository.deleteAll()
        }
    }
}