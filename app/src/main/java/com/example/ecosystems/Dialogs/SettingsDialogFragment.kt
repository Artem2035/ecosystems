package com.example.ecosystems.SettingsDialogFragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.DataClasses.PlanDownloadItem
import com.example.ecosystems.Dialogs.ProgressDialogFragment
import com.example.ecosystems.Plan.PlanDownloadAdapter
import com.example.ecosystems.R
import com.example.ecosystems.db.dto.TableDto
import com.example.ecosystems.db.dto.layer.LayerImageDto
import com.example.ecosystems.db.dto.layer.LayerPointDto
import com.example.ecosystems.db.dto.layer.toEntity
import com.example.ecosystems.db.dto.toEntity
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
import java.util.Locale

class SettingsDialogFragment(private val token:String,
                             private val planList: MutableList<Plan>,
                             private var layerRepository: LayerRepository,
                             private var planRepository: PlanRepository,
                             private var tableRepository: TableRepository,
                             private val syncManager: SyncManager,
                             private val onSyncComplete: (() -> Unit)? = null) : DialogFragment() {
    private val api: ApiService = ApiService()
    // Список для адаптера
    private val planItems = mutableListOf<PlanDownloadItem>()
    private var tempPlanItems = mutableListOf<PlanDownloadItem>()

    private lateinit var adapter: PlanDownloadAdapter

    private var progressDialog: ProgressDialogFragment? = null
    private lateinit var syncButton: Button

    val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        java.util.Locale.ENGLISH)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_settings, null)

        val backButton = view.findViewById<Button>(R.id.backButton)
        syncButton = view.findViewById(R.id.syncButton)

        val recyclerView = view.findViewById<RecyclerView>(R.id.plansRecyclerView)

        adapter = PlanDownloadAdapter(tempPlanItems) { item, position ->
            downloadSinglePlan(item, position)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        //поиск
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempPlanItems.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if(!searchText.isEmpty()){

                    planItems.forEach {
                        if(it.plan.name.lowercase(Locale.getDefault()).contains(searchText) ||
                            it.plan.description.lowercase(Locale.getDefault()).contains(searchText)){
                            tempPlanItems.add(it)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }else{
                    tempPlanItems.clear()
                    tempPlanItems.addAll(planItems)
                    adapter.notifyDataSetChanged()
                }
                return false
            }
        })

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
            .setView(view)
            .create()

        syncButton.setOnClickListener {
            if (!requireContext().isInternetAvailable()) {
                Toast.makeText(requireContext(), "Нет доступа к интернету!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startSync()
        }

/*        button.setOnClickListener {
            Log.d("SettingsDialogFragment", "SettingsDialogFragment")
            if (!requireContext().isInternetAvailable()) {
                Toast.makeText(requireContext(), "Нет доступа к интернету!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //showDownloadConfirmation()
        }*/

        backButton.setOnClickListener {
            dismiss()
        }

        Log.d("plan down","${planList}")
        buildPlanItems()

        return dialog
    }

    private fun buildPlanItems() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                planList.map { plan ->
                    PlanDownloadItem(
                        plan        = plan,
                        isDownloaded = planRepository.hasPlanData(plan.plainId)
                    )
                }
            }
            planItems.clear()
            planItems.addAll(items)
            tempPlanItems.addAll(items)
            adapter.notifyDataSetChanged()
        }
    }

    private fun downloadSinglePlan(item: PlanDownloadItem, position: Int){
        Log.d("plan down","${item} ${position}")
        item.isLoading = true
        adapter.notifyItemChanged(position)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                loadPlanLayers(item.plan)

                withContext(Dispatchers.Main) {
                    item.isLoading    = false
                    item.isDownloaded = true
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        "«${item.plan.name}» скачан",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("Settings", "Ошибка скачивания плана ${item.plan.name}", e)
                withContext(Dispatchers.Main) {
                    item.isLoading = false
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startSync(){
        progressDialog = ProgressDialogFragment("Прогресс синхронизации данных объектов: %d/%d")
        progressDialog?.show(parentFragmentManager, "sync_progress")

        lifecycleScope.launch{
            val pending = layerRepository.getPending()

            if (pending.isEmpty()) {
                Toast.makeText(requireContext(), "Нет изменений для синхронизации", Toast.LENGTH_SHORT).show()
                progressDialog?.dismiss()
                progressDialog = null
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
                is SyncManager.SyncResult.Success -> {
                    Toast.makeText(requireContext(), "Данные отправлены ✓", Toast.LENGTH_SHORT)
                        .show()
                    onSyncComplete?.invoke()
                }

                is SyncManager.SyncResult.PartialFailure -> {
                    Toast.makeText(
                        requireContext(),
                        "Отправлено частично. Не удалось: ${result.failedCount}",
                        Toast.LENGTH_LONG
                    ).show()
                    onSyncComplete?.invoke()
                }
                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Не удалось выполнить!",
                        Toast.LENGTH_LONG
                    ).show()
                }
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

    private fun loadPlanTable(tableId: Int){
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
                                val tableDto: TableDto = gson.fromJson(reader, TableDto::class.java)
                                if(tableDto.id != tableId)
                                    continue

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

    private fun loadPlanLayers(plan: Plan){
        val gson = Gson()
        lifecycleScope.launch(Dispatchers.IO) {
            ensureActive()

            api.loadPlanLayersRaw(token, plan.planUUID).use { body ->
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
                                if (tableId != null){
                                    loadPlanTable(tableId)
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

                Log.d("status down", "скачано для ${plan}")
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

    private fun showDownloadConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Скачивание данных")
            .setMessage("Скачивание данных приведёт к удалению несинхронизированных данных. Продолжить?")
            .setPositiveButton("Продолжить") { _, _ ->
                //startDownloading()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}