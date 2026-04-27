package com.example.ecosystems


import SecureTokenManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.Dialogs.PointDataDialogFragment
import com.example.ecosystems.Dialogs.SelectLayerDialogFragment
import com.example.ecosystems.PhotoViewDialog.ImageAnnotationDialog
import com.example.ecosystems.Plan.PlanInfoActivity
import com.example.ecosystems.Plan.PlanSearchDialogFragment
import com.example.ecosystems.SettingsDialogFragment.SettingsDialogFragment
import com.example.ecosystems.data.local.PointMarkerEntry
import com.example.ecosystems.db.AppDatabase
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.dao.SyncQueueDao
import com.example.ecosystems.db.dao.TableEntityDao
import com.example.ecosystems.db.entity.layer.LayerEntity
import com.example.ecosystems.db.entity.layer.LayerPointEntity
import com.example.ecosystems.db.entity.syncQueue.SyncManager
import com.example.ecosystems.db.entity.syncQueue.buildSyncManager
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import com.example.ecosystems.db.repository.TableRepository
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.network.BASE_URL
import com.example.ecosystems.utils.OsmTileProvider
import com.example.ecosystems.utils.TileDiskCache
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.geometry.geo.Projections
import com.yandex.mapkit.layers.Layer
import com.yandex.mapkit.layers.LayerOptions
import com.yandex.mapkit.layers.OverzoomMode
import com.yandex.mapkit.layers.TileFormat
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.CreateTileDataSource
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectDragListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private lateinit var mapView: MapView

class ForestTaxationActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private lateinit var token:String
    private lateinit var tileDiskCache: TileDiskCache

    private var currentCameraPosition: CameraPosition = CameraPosition(Point(61.78932, 34.35919),
        17f, 0.0f, 0.0f)

    /*список кластеризуемых коллекции точек, слои типа libraryImages*/
    private var libraryImagesCollectionList: MutableList<ClusterizedPlacemarkCollection> = mutableListOf()
    /*список ClusterListener для кластеризуемых коллекции точек*/
    private var libraryImagesClusterListenerList: MutableList<ClusterListener> = mutableListOf()
    /*map кластеризуемых коллекции слоя типа points, где ключ - id слоя, к которому коллекция относится*/
    private var pointsCollectionMap: MutableMap<Int, ClusterizedPlacemarkCollection> = mutableMapOf()
    /* Реестр: pointId -> PlacemarkMapObject, для возомжности удалить маркер через кластер*/
    private val pointMarkersMap2: MutableMap<Int, PlacemarkMapObject> = mutableMapOf()
    private val pointMarkersMap: MutableMap<Int, PointMarkerEntry> = mutableMapOf()
    /*список ClusterListener для кластеризуемых коллекции точек для списка слоев типа points*/
    private var pointsClusterListenerList: MutableList<ClusterListener> = mutableListOf()

    private var isSelecting = false
    private val selectedPoints = mutableListOf<Point>()
    private lateinit var selectButton: AppCompatButton
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var tempObjects: MapObjectCollection
    private val planLayersMap = mutableMapOf<String, List<Layer>>()
    private val tileProviders = mutableListOf<OsmTileProvider>()
    /*список всех планов гис объектов*/
    private var listOfPlans: MutableList<Map<String, Any?>> = mutableListOf()
    /*слои для текущего плана гис*/
    private var listOfPlanLayers: MutableList<Map<String, Any?>> = mutableListOf()
    private val tileFileNames = mutableListOf<String>()
    private var planList: MutableList<Plan> = mutableListOf()
    private var tempPlanList: MutableList<Plan> = mutableListOf()
    /*словарь всех планов гис, где ключ - UUID*/
    private var plansMap: MutableMap<String, Map<String, Any?>> = mutableMapOf()
    /*набор полей и точек координат, где было сделано фото*/
    private var planLibraryImagesMap: MutableMap<String, MutableMap<String, Any?>> = mutableMapOf()
    /*словарь для всех слоев с точками, ключ - uuid*/
    private var planPointsMap: MutableMap<String, MutableMap<String, Any?>> = mutableMapOf()
    /*слои с точками*/
    private lateinit var layerDao: LayerEntityDao
    private lateinit var planDao: PlanEntityDao
    private lateinit var tableDao: TableEntityDao
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var layerRepository: LayerRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var tableRepository: TableRepository
    private lateinit var syncManager: SyncManager

    //перетескивание точки
    private var isDragging = false
    private var draggingPointId: Int? = null
    private var draggingPlacemark: PlacemarkMapObject? = null

    private val treesManagementLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("Launcher", "resultCode=${result.resultCode}, data=${result.data}")
        Log.d("Launcher", "deletedIds=${result.data?.getIntegerArrayListExtra(TreesManagementActivity.EXTRA_DELETED_POINT_IDS)}")

        if (result.resultCode == RESULT_OK) {
            val deletedIds = result.data
                ?.getIntegerArrayListExtra(TreesManagementActivity.EXTRA_DELETED_POINT_IDS)
                ?: return@registerForActivityResult

            deletedIds.forEach { pointId ->
                removePointFromMap(pointId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest_taxation)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!

        tileDiskCache = TileDiskCache(this)

        layerDao = AppDatabase.getInstance(this).layerDao()
        planDao = AppDatabase.getInstance(this).planDao()
        tableDao = AppDatabase.getInstance(this).tableDao()
        syncQueueDao = AppDatabase.getInstance(this).syncQueueDao()
        layerRepository = LayerRepository(layerDao, syncQueueDao)
        planRepository = PlanRepository(planDao)
        tableRepository = TableRepository(tableDao)
        syncManager = buildSyncManager(
            layerRepository = layerRepository,
            syncQueueDao = syncQueueDao,
            layerDao = layerDao,
            api = api,
            token = token
        )


        mapView = findViewById(R.id.mapView)
        // Мгновенное перемещение
        mapView.mapWindow.map.move(currentCameraPosition)
        mapView.mapWindow.map.set2DMode(true)
        mapObjects = mapView.mapWindow.map.mapObjects
        mapObjects.addPlacemark().apply {
            geometry = Point(57.907, 36.58)
            setText("Мой маркер")
            setUserData("custom_data") // Можно сохранить свои данные
        }
        tempObjects = mapObjects.addCollection() // временные объекты (точки и линии при выборе)
        // Слушатель нажатий на карту
        mapView.mapWindow.map.addInputListener(mapInputListener)

        selectButton = findViewById(R.id.addPolygonButton)
        // Кнопка для активации режима выбора
        selectButton.setOnClickListener {
            if (!isSelecting) {
                isSelecting = true
                Toast.makeText(this, "Выберете 4 точки на карте", Toast.LENGTH_SHORT).show()
                selectButton.text = "Отмена"
            } else {
                isSelecting = false
                selectedPoints.clear()
                tempObjects.clear()
                selectButton.text = "Новый участок"
            }
        }

        val planInfoImageButton = findViewById<ImageButton>(R.id.planInfoImageButton)
        // Кнопка для активации режима выбора
        planInfoImageButton.setOnClickListener {
            if(isInternetAvailable()){
                if (selectedPlan != null) {
                    val intent =  Intent(this,PlanInfoActivity::class.java)
                    val json = Gson().toJson(listOfPlanLayers)
                    intent.putExtra("listOfPlanLayers", json)
                    startActivity(intent)
                } else {
                    val message = Toast.makeText(this,"ГИС не выбран!",Toast.LENGTH_SHORT)
                    message.show()
                }
            }else {
                val message = Toast.makeText(this,"Пока не доступно в офлайн режиме!",Toast.LENGTH_SHORT)
                message.show()
            }
        }

        val settingsImageButton = findViewById<ImageButton>(R.id.settingsImageButton)
        settingsImageButton.setOnClickListener {
            val dialog = SettingsDialogFragment(token, layerRepository, planRepository, tableRepository, syncManager)
            dialog.show(supportFragmentManager, "settings_dialog")
        }

        val searchPlanButton = findViewById<ImageButton>(R.id.searchPlanImageButton)
        searchPlanButton.setOnClickListener {
            showSearchPlanDialog()
        }

        if(isInternetAvailable()){
            val thread =Thread{
                try {
                    val gson = Gson()
                    val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
                    val result: Map<String, Any?> = mapAdapter.fromJson(api.loadPlans(token))
                    listOfPlans= result.get("plans") as MutableList<Map<String, Any?>>
                    if(listOfPlans.isNotEmpty())
                    {
                        for (plan in listOfPlans)
                        {
                            val planUUID = plan.get("uuid").toString()
                            planList.add(Plan(plan.get("id").toString().toDouble().toInt(), planUUID, plan.get("name").toString(), plan.get("description").toString()))
                            plansMap.put(planUUID, plan)
                        }
                    }

                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(this,"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                        message.show()
                        val intent =  Intent(this,MapActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            thread.start()
        }
        else{
            lifecycleScope.launch {
                val plans = planRepository.getAllPlans()
                Log.d("TAG11", "Количество: ${plans.size}")
                plans.forEach { plan ->
                    planList.add(Plan(plan.id, plan.uuid,plan.name, plan.description.toString()))
                }
            }
        }
    }

    private val mapInputListener = object : InputListener {
        override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
            if (isSelecting) {
                runOnUiThread { onMapTapped(point) }
            }
        }

        override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            runOnUiThread {
                if (!isDragging) {
                    onLongTapOnMap(point.latitude, point.longitude)
                }
            }
        }
    }

    /*обработчик перемещения точки при длительном нажатии и ее перемещении*/
    private val pointDragListener = object : MapObjectDragListener {

        override fun onMapObjectDragStart(mapObject: MapObject) {
            val pointId = mapObject.userData as? Int ?: return
            isDragging = true
            draggingPointId = pointId
            draggingPlacemark = mapObject as? PlacemarkMapObject
            Log.d("drag", "Start dragging pointId=$pointId")
        }

        override fun onMapObjectDrag(mapObject: MapObject, point: Point) {
            // можно обновлять координаты в реальном времени если нужно
        }

        override fun onMapObjectDragEnd(mapObject: MapObject) {
            val pointId = draggingPointId ?: return
            val placemark = draggingPlacemark ?: return
            val newPoint = placemark.geometry

            Log.d("drag", "End drag pointId=$pointId lat=${newPoint.latitude} lng=${newPoint.longitude}")

            // Сохраняем новые координаты в БД
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    layerRepository.updatePointCoordinates(
                        pointId = pointId,
                        lat = newPoint.latitude,
                        lng = newPoint.longitude
                    )
                }
                Toast.makeText(
                    this@ForestTaxationActivity,
                    "Координаты сохранены",
                    Toast.LENGTH_SHORT
                ).show()
            }
            pointsCollectionMap.values.forEach {
                it.clusterPlacemarks(50.0, 17)
            }

            isDragging = false
            draggingPointId = null
            draggingPlacemark = null
        }
    }

    private fun onMapTapped(point: Point) {
        selectedPoints.add(point)
        // Добавляем маркер точки
        tempObjects.addPlacemark().apply {
            geometry = point
            setText("test")
        }

        if (selectedPoints.size == 4) {
            drawPolygon()
            isSelecting = false
            selectButton.text = "Выбрать участок"
            Toast.makeText(this, "Участок отмечен!", Toast.LENGTH_SHORT).show()
        }
        if (selectedPoints.size > 4){
            isSelecting = false
            selectedPoints.clear()
            tempObjects.clear()
            selectButton.text = "Новый участок"
        }
        Log.d("Map Tap", "Map tap")
    }

    /*при длительном нажатии создаем точку, если выбран гис объект (план)*/
    private fun onLongTapOnMap(lat: Double, lng: Double) {
        if(selectedPlan == null){
            Toast.makeText(this, "Гис объект (план) не выбран!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val pointLayers = withContext(Dispatchers.IO) {
                layerRepository.getPointLayersByPlanId(selectedPlan!!.plainId)
            }
            if (pointLayers.isEmpty()) {
                Toast.makeText(this@ForestTaxationActivity, "Нет доступных слоёв", Toast.LENGTH_SHORT).show()
                return@launch
            }

            SelectLayerDialogFragment(pointLayers) { selectedLayer, num ->
                lifecycleScope.launch {
                    createPointAndOpenDialog(selectedLayer, num, lat, lng)
                }
            }.show(supportFragmentManager, "select_layer")
        }
    }

    /*создание новой точки (маркера) и открытие диалага с параметрами*/
    private suspend fun createPointAndOpenDialog(
        layer: LayerEntity,
        num: Int,
        lat: Double,
        lng: Double
    ) {
        val now = System.currentTimeMillis()
        val newPoint = LayerPointEntity(
            layerId = layer.id,
            lat = lat,
            lng = lng,
            num = num,
            valuesJson = "{}",
            createdAt = now,
            updatedAt = now
        )

        val newPointId = withContext(Dispatchers.IO) {
            layerRepository.insertPoint(newPoint)
        }.toInt()

        // Цвет слоя (если есть поле color в LayerEntity, иначе дефолт)
        val colorInt = runCatching {
            Color.parseColor(layer.color)
        }.getOrDefault(Color.parseColor("#FFA500"))

        var newMarker: PlacemarkMapObject
        // Добавляем маркер в нужную коллекцию
        runOnUiThread {
            addMarkerToCollection(
                layerId = layer.id,
                pointId = newPointId,
                num = num,
                lat = lat,
                lng = lng,
                colorInt = colorInt
            )
        }

        withContext(Dispatchers.Main) {
            PointDataDialogFragment(
                pointId = newPointId,
                layerRepository = layerRepository,
                tableRepository = tableRepository,
                onPointDeleted = { deletedId ->
                    removePointFromMap(deletedId)
                }
            ).show(supportFragmentManager, "point_data_$newPointId")
        }
    }
    /*добавить маркер в коллекцию*/
    private fun addMarkerToCollection(
        layerId: Int,
        pointId: Int,
        num: Int,
        lat: Double,
        lng: Double,
        colorInt: Int
    ) {
        val collection = pointsCollectionMap[layerId]

        if (collection == null) {
            // Коллекции для слоя ещё нет — создаём новую
            val mapObjects = mapView.mapWindow.map.mapObjects
            val listener = createLibraryImagesClusterListener(colorInt)
            val newCollection = mapObjects.addClusterizedPlacemarkCollection(listener)

            val placemark =  newCollection.addPlacemark().apply {
                geometry = Point(lat, lng)
                setUserData(pointId)
                setText(num.toString())
                setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                addTapListener(globalListener)
                isDraggable = true                        // разрешить перетаскивание
                setDragListener(pointDragListener)        // добавить listener
            }
            newCollection.clusterPlacemarks(50.0, 17)

            pointsClusterListenerList.add(listener)
            pointsCollectionMap[layerId] = newCollection
            pointMarkersMap[pointId] = PointMarkerEntry(placemark, newCollection)
        }
        else {
            // Коллекция уже есть — просто добавляем маркер и перекластеризуем
            val placemark = collection.addPlacemark().apply {
                geometry = Point(lat, lng)
                setUserData(pointId)
                setText(num.toString())
                setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                addTapListener(globalListener)
                isDraggable = true                        // разрешить перетаскивание
                setDragListener(pointDragListener)        // добавить listener
            }
            collection.clusterPlacemarks(50.0, 17)
            pointMarkersMap[pointId] = PointMarkerEntry(placemark, collection)
        }
    }
    /*удалить маркер точки из кластера*/
    fun removePointFromMap(pointId: Int) {
        val entry = pointMarkersMap.remove(pointId) ?: return
        runOnUiThread {
            entry.collection.remove(entry.placemark)
            entry.collection.clusterPlacemarks(50.0, 17)
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    fun startMapActivity(view: View)
    {
        val intent =  Intent(this,MapActivity::class.java)
        startActivity(intent)
    }

    fun startTreesManagementActivity(view: View)
    {
        if (selectedPlan == null) {
            val message = Toast.makeText(this,"ГИС не выбран!",Toast.LENGTH_SHORT)
            message.show()
            return
        }

        val intent =  Intent(this,TreesManagementActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("planId", selectedPlan?.plainId)
        intent.putExtras(bundle)
        treesManagementLauncher.launch(intent)
    }

    private fun drawPolygon() {
        val ring = LinearRing(selectedPoints + selectedPoints.first()) // замкнуть
        val polygon = Polygon(ring, emptyList())
        val polygonObj = mapObjects.addPolygon(polygon)

        // Настраиваем цвета
        polygonObj.fillColor = Color.argb(100, 0, 255, 0) // полупрозрачная зелёная заливка
        polygonObj.strokeColor = Color.GRAY // граница
        polygonObj.strokeWidth = 1f
        selectedPoints.clear()
        tempObjects.clear()
        //tempObjects.clear() // очистить временные точки
    }


    private var customLayer: Layer? = null
    /*добавление слоя с кастомными тайлами карты*/
    private fun addTileLayer(LayerId: String, TileFileName: String): Layer {
        val map = mapView.mapWindow.map

        val osmTileProvider = OsmTileProvider(TileFileName, tileDiskCache, this)
        if (!tileProviders.contains(osmTileProvider)){
            tileProviders.add(osmTileProvider)
        }


        val tileDataSource = CreateTileDataSource { builder ->
            builder.setTileProvider(osmTileProvider)
            builder.setProjection(Projections.getSphericalMercator())
            builder.setTileFormat(TileFormat.PNG)
        }
        // Настройки слоя
        val layerOptions = LayerOptions()
            .setActive(true)
            .setCacheable(true)
            .setTransparent(true)               // слой поверх карты — он прозрачный
            .setNightModeAvailable(false)
            .setAnimateOnActivation(true)
            .setTileAppearingAnimationDuration(400)
            .setOverzoomMode(OverzoomMode.ENABLED)
            .setVersionSupport(false)

        customLayer = map.addTileLayer(
            LayerId,        // уникальный ID слоя
            layerOptions,     // параметры слоя
            tileDataSource      // CreateTileDataSource
        )
        return customLayer as Layer
    }

    var selectedPlan: Plan? = null
    private fun showSearchPlanDialog(){
        tempPlanList.clear()
        tempPlanList.addAll(planList)

        Log.d("TAG_DG fa","1")
        val dialog = PlanSearchDialogFragment(planList, tempPlanList) { plan ->
            mapView.mapWindow.map.mapObjects.clear()

            val cameraPosition = mapView.map.cameraPosition
            var lat = cameraPosition.target.latitude
            var lng = cameraPosition.target.longitude

            if(isInternetAvailable()){
                if(selectedPlan != plan && selectedPlan != null){
                    val planUUID = selectedPlan!!.planUUID
                    val currentLayers: List<Layer> = planLayersMap.get(planUUID)!!
                    for(layer in currentLayers){
                        layer.remove()
                        if (layer.isValid == true) {
                            layer.remove()
                        }
                    }
                    planLayersMap.remove(planUUID)
                }
                selectedPlan = plan
                val currentPlan = plansMap.get(selectedPlan!!.planUUID)
                val files = currentPlan?.get("files") as MutableList<Map<String, Any?>>

                if(!planLayersMap.containsKey(selectedPlan!!.planUUID)){
                    tileFileNames.clear()
                    for (file in files){
                        tileFileNames.add(file.get("uuid").toString())
                        if(file.get("center_lat") != null){
                            lat = file.get("center_lat").toString().toDouble()
                        }
                        if(file.get("center_lng") != null){
                            lng = file.get("center_lng").toString().toDouble()
                        }
                    }
                    val newCameraPosition = CameraPosition(Point(lat, lng),
                        cameraPosition.zoom, cameraPosition.azimuth, cameraPosition.tilt)
                    val layers = mutableListOf<Layer>()
                    for (tileFile in tileFileNames){
                        layers.add(addTileLayer(tileFile, tileFile))
                    }
                    planLayersMap.put(selectedPlan!!.planUUID, layers)
                    mapView.mapWindow.map.move(newCameraPosition)
                }
                else{
                    for (file in files){
                        tileFileNames.add(file.get("uuid").toString())
                        if(file.get("center_lat") != null){
                            lat = file.get("center_lat").toString().toDouble()
                        }
                        if(file.get("center_lng") != null){
                            lng = file.get("center_lng").toString().toDouble()
                        }
                    }
                    val newCameraPosition = CameraPosition(Point(lat, lng),
                        cameraPosition.zoom, cameraPosition.azimuth, cameraPosition.tilt)
                    mapView.mapWindow.map.move(newCameraPosition)
                }

                val thread =Thread{
                    try {
                        val gson = Gson()
                        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
                        val result: Map<String, Any?> = mapAdapter.fromJson(api.loadPlanLayers(token, selectedPlan!!.planUUID))
                        Log.d("result", "${result}")
                        listOfPlanLayers = result.get("layers") as MutableList<Map<String, Any?>>
                        planPointsMap.clear()
                        pointsCollectionMap.clear()
                        pointsClusterListenerList.clear()
                        planLibraryImagesMap.clear()
                        libraryImagesCollectionList.clear()
                        libraryImagesClusterListenerList.clear()
                        for(layer in listOfPlanLayers){
                            if(layer.get("type") == "points"){
                                val uuid = layer["uuid"].toString()
                                planPointsMap[uuid] = layer.entries.associate { (key, value) ->
                                    key to deepCopy(value)
                                }.toMutableMap()
                            }
                            if(layer.get("type") == "library_images"){
                                val uuid = layer.get("uuid").toString()
                                planLibraryImagesMap[uuid] = mutableMapOf()
                                layer.forEach { (key, value) ->
                                    if(value is Map<*, *> || value is List<*>)
                                        planLibraryImagesMap[uuid]?.set(key, deepCopy(value as Map<String, Any?>))
                                    else
                                        planLibraryImagesMap[uuid]?.set(key, value)
                                }
                            }
                        }
                        DrawObjectsOnMap()
                    }
                    catch (exception: Exception)
                    {
                        Log.d("Error","Unexpected code ${exception.message}")
                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(this,"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                            message.show()
                            val intent =  Intent(this,MapActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
                thread.start()
            }
            else{
                lifecycleScope.launch {
                    if(selectedPlan != plan && selectedPlan != null){
                        val planUUID = selectedPlan!!.planUUID
                        val currentLayers: List<Layer> = planLayersMap.get(planUUID)!!
                        for(layer in currentLayers){
                            if (layer.isValid == true) {
                                layer.remove()
                            }
                        }
                        planLayersMap.remove(planUUID)
                    }
                    selectedPlan = plan

                    val planFiles =  planRepository.getPlanFiles(plan.plainId)
                    if(!planLayersMap.containsKey(selectedPlan!!.planUUID)){
                        tileFileNames.clear()
                        planFiles.forEach{file->
                            tileFileNames.add(file.uuid)
                            if(file.centerLat != null && file.centerLng != null){
                                lat = file.centerLat
                                lng = file.centerLng
                            }
                        }
                        val layers = mutableListOf<Layer>()
                        for (tileFile in tileFileNames){
                            layers.add(addTileLayer(tileFile, tileFile))
                        }
                        planLayersMap.put(selectedPlan!!.planUUID, layers)
                    }
                    else{
                        tileFileNames.clear()
                        planFiles.forEach{file->
                            tileFileNames.add(file.uuid)
                            if(file.centerLat != null && file.centerLng != null){
                                lat = file.centerLat
                                lng = file.centerLng
                            }
                        }
                    }

                    val newCameraPosition = CameraPosition(Point(lat, lng),
                        cameraPosition.zoom, cameraPosition.azimuth, cameraPosition.tilt)
                    mapView.mapWindow.map.move(newCameraPosition)
                    planPointsMap.clear()
                    pointsCollectionMap.clear()
                    pointsClusterListenerList.clear()
                    planLibraryImagesMap.clear()
                    libraryImagesCollectionList.clear()
                    libraryImagesClusterListenerList.clear()
                    Log.d("TAG12", "DrawObjectsOnMap")
                    DrawObjectsOnMap()
                }
            }
        }
        dialog.show(supportFragmentManager, "search_plan_dialog")
    }

    // Обработка нажатия конкретно на объекты (маркеры)
    private val globalListener = MapObjectTapListener { mapObject, point ->
        val id = mapObject.userData

        if(id != null){
            val dialog = PointDataDialogFragment(id.toString().toDouble().toInt(), layerRepository, tableRepository,
                onPointDeleted = { deletedId ->
                    removePointFromMap(deletedId)
                })
            dialog.show(supportFragmentManager, "point_data_dialog")
        }
        true
    }

    private val libraryImagesListener = MapObjectTapListener { mapObject, point ->
        if(isInternetAvailable()){
            val data = mapObject.userData as Map<String, Any?>
            Log.d("imageUri", "$data")
            val fileName = data.get("filename").toString()
            val uuid = data.get("uuid").toString()
            Log.d("fileName", "${BASE_URL}api/v1/orthophotoplans/layers/images/image/${uuid}/$fileName")
            val dialog = ImageAnnotationDialog(
                context = this,
                imageUri = Uri.parse("${BASE_URL}api/v1/orthophotoplans/layers/images/image/${uuid}/$fileName"),
            ) { annotatedBitmap ->
                // annotatedBitmap — изображение с нарисованными прямоугольниками от OpenCV
                // или сохранить на диск
            }
            dialog.show()
        }
        else{
            val message = Toast.makeText(this,"Пока не доступно в офлайн режиме!",Toast.LENGTH_SHORT)
            message.show()
        }
        true
    }

    private val clusterTapListener = ClusterTapListener { cluster ->
        val currentCamera = mapView.mapWindow.map.cameraPosition
        mapView.mapWindow.map.move(
            CameraPosition(
                cluster.appearance.geometry,
                currentCamera.zoom + 1.5f,
                0f,
                0f
            )
        )
        true
    }

    /*иконки кластера для слоя типа "library_images"*/
    fun createLibraryImagesClusterListener(bgColor: Int) = ClusterListener { cluster ->
        cluster.appearance.setIcon(clusterIcon(bgColor = bgColor, textColor = 0xFFFFFFFF.toInt()))
        cluster.appearance.setText(cluster.size.toString())
        cluster.appearance.setTextStyle(
            TextStyle().apply {
                size = 12f
                color = Color.WHITE
                placement = TextStyle.Placement.CENTER
                outlineColor = Color.BLACK
                outlineWidth = 2f
            }
        )
        cluster.addClusterTapListener(clusterTapListener)
    }

    fun DrawObjectsOnMap()
    {
        runOnUiThread{
            val mapObjects = mapView.mapWindow.map.mapObjects

            Log.d("TAG12", "plan1: ${selectedPlan}")
            lifecycleScope.launch{
                if (selectedPlan != null){

                    val plan = planRepository.getPlanData(selectedPlan!!.plainId)
                    if(plan == null)
                        return@launch

                    plan.layers.forEach {layer->
                        var colorInt =  Color.parseColor("#FFA500")
                        if(layer.color != null)
                            colorInt = Color.parseColor(layer.color)
                        val tempPointsClusterListener = createLibraryImagesClusterListener(colorInt)
                        val tempPointsCollection = mapObjects.addClusterizedPlacemarkCollection(tempPointsClusterListener)

                        when(layer.type){
                            "points" -> {
                                val pointList = layerRepository.getPointsByLayerId(layer.id)
                                pointList.forEach { point ->
                                    val placemark = tempPointsCollection.addPlacemark().apply {
                                        geometry = Point(point.lat, point.lng)
                                        setUserData(point.id)
                                        setText(point.num.toString())
                                        setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                        addTapListener(globalListener)
                                        isDraggable = true                        // разрешить перетаскивание
                                        setDragListener(pointDragListener)        // добавить listener
                                    }
                                    pointMarkersMap[point.id] = PointMarkerEntry(placemark, tempPointsCollection)
                                }
                                pointsClusterListenerList.add(tempPointsClusterListener)
                                pointsCollectionMap[layer.id] = tempPointsCollection
                            }
                            "library_images" -> {
                                val imageList = layerRepository.getImagesByLayerId(layer.id)
                                imageList.forEach { image ->
                                    tempPointsCollection.addPlacemark().apply {
                                        geometry = Point(image.lat, image.lng)
                                        val imageData = mutableMapOf<String, Any?>("filename" to image.filename,
                                            "num" to image.num, "uuid" to layer.uuid)
                                        setUserData(imageData)
                                        setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                        addTapListener(libraryImagesListener)
                                    }
                                }
                                libraryImagesClusterListenerList.add(tempPointsClusterListener)
                                libraryImagesCollectionList.add(tempPointsCollection)
                            }
                        }
                    }
                    pointsCollectionMap.values.forEach {
                        it.clusterPlacemarks(50.0, 17)
                    }

                    libraryImagesCollectionList.forEach {
                        it.clusterPlacemarks(50.0, 17)
                    }
                }
            }
        }
    }
}

fun deepCopy(value: Any?): Any? {
    return when (value) {
        is Map<*, *> -> (value as Map<String, Any?>).mapValues { (_, v) -> deepCopy(v) }
        is List<*> -> value.map { deepCopy(it) }
        else -> value
    }
}

fun clusterIcon(bgColor: Int, textColor: Int): ImageProvider {
    val size = 80        // размер битмапа в пикселях
    val radius = 30f      // радиус круга — кликабельная зона
    val center = size / 2f

    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // Основной круг
    p.color = bgColor
    c.drawCircle(center, center, radius, p)

    p.color = textColor
    p.textSize = 30f
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.DEFAULT_BOLD

    return ImageProvider.fromBitmap(bmp)
}