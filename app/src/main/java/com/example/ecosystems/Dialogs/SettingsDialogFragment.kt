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
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.entity.LayerEntity
import com.example.ecosystems.db.entity.LayerImageEntity
import com.example.ecosystems.db.entity.LayerPointEntity
import com.example.ecosystems.db.entity.PlanEntity
import com.example.ecosystems.db.entity.PlanFileEntity
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsDialogFragment(private val token:String,
                             private var layerDao: LayerEntityDao,
                             private var planDao: PlanEntityDao,
                             private var layerRepository: LayerRepository,
                             private var planRepository: PlanRepository) : DialogFragment() {
    private val api: ApiService = ApiService()
    private var progressDialog: ProgressDialogFragment? = null

    val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        java.util.Locale.ENGLISH)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_settings, null)

        val button = view.findViewById<Button>(R.id.downloadButton)
        val backButton = view.findViewById<Button>(R.id.backButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        button.setOnClickListener {
            Log.d("SettingsDialogFragment", "SettingsDialogFragment")
            if(requireContext().isInternetAvailable()){
                val thread =Thread{
                    val gson = Gson()
                    val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
                    try {
                        progressDialog = ProgressDialogFragment()
                        progressDialog?.show(parentFragmentManager, "progress")

                        var result: Map<String, Any?> = mapAdapter.fromJson(api.loadPlans(token))
                        val listOfPlans= result.get("plans") as MutableList<Map<String, Any?>>

                        val planFileEntities = mutableListOf<PlanFileEntity>()

                        val planEntities = listOfPlans.map { plan ->
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
                        }
                        lifecycleScope.launch(Dispatchers.IO) {
                            planRepository.insertAll(planEntities)
                            planRepository.insertAllFiles(planFileEntities)
                        }

                        Log.d("TAG_DG1","1")

                        val max = planEntities.size
                        Log.d("status down", "размер = ${max}")
                        var count = 0
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {

                                planEntities.forEach {plan->
                                    ensureActive()

                                    api.loadPlanLayersRaw(token, plan.uuid).use { body ->
                                        val pointsEntities = mutableListOf<LayerPointEntity>()
                                        val imagesEntities = mutableListOf<LayerImageEntity>()
                                        val layerEntities = mutableListOf<LayerEntity>()

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

                                                        when (layer["type"]?.toString()) {
                                                            "points" -> {
                                                                val points = (json["points"] as? List<Map<String, Any?>>)
                                                                    ?: emptyList()
                                                                points.mapTo(pointsEntities) { point ->
                                                                    LayerPointEntity(
                                                                        id = (point["id"] as Number).toInt(),
                                                                        layerId = (point["layer_id"] as Number).toInt(),
                                                                        lat = (point["lat"] as Number).toDouble(),
                                                                        lng = (point["lng"] as Number).toDouble(),
                                                                        num = (point["num"] as Number).toInt(),
                                                                        valuesJson = gson.toJson(point["values"]),
                                                                        createdAt = formatter.parse(point["created_at"] as String)?.time ?: 0L,
                                                                        updatedAt = formatter.parse(point["updated_at"] as String)?.time ?: 0L
                                                                    )
                                                                }
                                                            }
                                                            "library_images" -> {
                                                                val images = (json["images"] as? List<Map<String, Any?>>)
                                                                    ?: emptyList()
                                                                images.mapNotNullTo(imagesEntities) { image ->
                                                                    LayerImageEntity(
                                                                        id = (image["id"] as? Number)?.toInt() ?: return@mapNotNullTo null,
                                                                        uuid = image["uuid"] as? String ?: return@mapNotNullTo null,
                                                                        filename = image["filename"] as? String ?: "",
                                                                        originalFilename = image["original_filename"] as? String ?: "",
                                                                        gisObjectLayerId = (image["gis_object_layer_id"] as? Number)?.toInt() ?: 0,
                                                                        lat = (image["lat"] as? Number)?.toDouble() ?: 0.0,
                                                                        lng = (image["lng"] as? Number)?.toDouble() ?: 0.0,
                                                                        num = (image["num"] as? Number)?.toInt() ?: 0,
                                                                        description = image["description"] as? String,
                                                                        createdAt = formatter.parse(image["created_at"] as String)?.time ?: 0L,
                                                                        updatedAt = formatter.parse(image["updated_at"] as String)?.time ?: 0L,
                                                                        localPath = null,
                                                                        lastAccessedAt = null
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        layerEntities.add(
                                                            LayerEntity(
                                                                id = layer["id"].toString().toDouble().toInt(),
                                                                uuid = layer["uuid"] as String,
                                                                gisObjectId = layer["gis_object_id"].toString().toDouble().toInt(),
                                                                gisObjectFileId = layer["gis_object_file_id"].toString().toDoubleOrNull()?.toInt(),
                                                                name = layer["name"] as String,
                                                                color = layer["color"] as? String,
                                                                type = layer["type"] as String,
                                                                order = layer["order"].toString().toIntOrNull(),
                                                                parentId = layer["parent_id"].toString().toIntOrNull(),
                                                                tableId = layer["table_id"].toString().toIntOrNull(),
                                                                createdAt = formatter.parse(layer["created_at"] as String)?.time ?: 0L,
                                                                updatedAt = formatter.parse(layer["updated_at"] as String)?.time ?: 0L,
                                                                dataJson = gson.toJson(dataJson),
                                                                cropEnabled = layer["crop_enabled"] as Boolean,
                                                                cropPercent = (layer["crop_percent"] as Number).toDouble(),
                                                                syncedAt = System.currentTimeMillis()
                                                            )
                                                        )
                                                    }
                                                    reader.endArray()
                                                }
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()

                                        layerRepository.insertAllData(layerEntities, pointsEntities, imagesEntities)
                                    }
                                    count += 1
                                    withContext(Dispatchers.Main) {
                                        progressDialog?.updateProgress(count, max)
                                    }
                                    Log.d("status down", "скачано ${count} из ${planEntities.size}")
                                }
                            }
                            progressDialog?.dismiss()
                            Log.d("TAG_DG2","1")
                            Handler(Looper.getMainLooper()).post{
                                val message = Toast.makeText(requireContext(),"Данные скачаны!",Toast.LENGTH_SHORT)
                                message.show()
                            }
                        }
                    }
                    catch (exception: Exception)
                    {
                        Log.d("Error","Unexpected code ${exception.message}")
                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(requireContext(),"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                            message.show()
                        }
                    }
                }
                thread.start()
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

        return dialog
    }
}