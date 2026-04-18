package com.example.ecosystems


import SecureTokenManager
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.Dialogs.PointDataDialogFragment
import com.example.ecosystems.PhotoViewDialog.ImageAnnotationDialog
import com.example.ecosystems.Plan.PlanInfoActivity
import com.example.ecosystems.Plan.PlanSearchDialogFragment
import com.example.ecosystems.SettingsDialogFragment.SettingsDialogFragment
import com.example.ecosystems.db.AppDatabase
import com.example.ecosystems.db.dao.LayerEntityDao
import com.example.ecosystems.db.dao.PlanEntityDao
import com.example.ecosystems.db.dao.TableEntityDao
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import com.example.ecosystems.db.repository.TableRepository
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.OsmTileProvider
import com.example.ecosystems.utils.getBitmapFromVectorDrawable
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
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private lateinit var mapView: MapView
private val BASE_URL: String = "https://smartecosystems.petrsu.ru/"

class ForestTaxationActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private lateinit var token:String

    private var currentCameraPosition: CameraPosition = CameraPosition(Point(61.78932, 34.35919),
        17f, 0.0f, 0.0f)

    /*список кластеризуемых коллекции точек, слои типа libraryImages*/
    private var libraryImagesCollectionList: MutableList<ClusterizedPlacemarkCollection> = mutableListOf()
    /*список ClusterListener для кластеризуемых коллекции точек*/
    private var libraryImagesClusterListenerList: MutableList<ClusterListener> = mutableListOf()
    /*список кластеризуемых коллекции слоя типа points*/
    private var pointsCollectionList: MutableList<ClusterizedPlacemarkCollection> = mutableListOf()
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
    private lateinit var layerRepository: LayerRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var tableRepository: TableRepository

    val formatter = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        java.util.Locale.ENGLISH)

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest_taxation)

        layerDao = AppDatabase.getInstance(this).layerDao()
        planDao = AppDatabase.getInstance(this).planDao()
        tableDao = AppDatabase.getInstance(this).tableDao()
        layerRepository = LayerRepository(layerDao)
        planRepository = PlanRepository(planDao)
        tableRepository = TableRepository(tableDao)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!
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
        mapView.mapWindow.map.addInputListener(object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                if (isSelecting) {
                    onMapTapped(point)
                }
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {

            }
        })

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
            val dialog = SettingsDialogFragment(token, layerRepository, planRepository, tableRepository)
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
        }else{
            lifecycleScope.launch {
                val plans = planDao.getAllPlans().first()
                Log.d("TAG11", "Количество: ${plans.size}")
                plans.forEach { plan ->
                    planList.add(Plan(plan.id, plan.uuid,plan.name, plan.description.toString()))
                }
            }
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
        val intent =  Intent(this,TreesManagementActivity::class.java)
        val bundle = Bundle()
        //bundle.putSerializable("planPointsMap", planPointsMap as java.io.Serializable)
        bundle.putSerializable("planId", selectedPlan?.plainId)
        intent.putExtras(bundle)
        startActivity(intent)
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
    private fun addTileLayer(LayerId: String, TileFileName: String): Layer {
        val map = mapView.mapWindow.map

        val osmTileProvider = OsmTileProvider(TileFileName)
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
                        pointsCollectionList.clear()
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
                    /*                val entities = listOfPlanLayers.map { layer ->
                                        val createdAt = formatter.parse(layer["created_at"] as String)?.time ?: 0L
                                        val updatedAt = formatter.parse(layer["updated_at"] as String)?.time ?: 0L
                                        LayerEntity(
                                            id = layer["id"].toString().toDouble().toInt(),
                                            uuid = layer["uuid"] as String,
                                            gisObjectId = layer["gis_object_id"].toString().toDouble().toInt(),
                                            gisObjectFileId = layer["gis_object_file_id"].toString().toDoubleOrNull()
                                                ?.toInt(),
                                            name = layer["name"] as String,
                                            color = layer["color"] as String?,
                                            type = layer["type"] as String,
                                            order = layer["order"].toString().toIntOrNull(),
                                            parentId = layer["parent_id"].toString().toIntOrNull(),
                                            tableId = layer["table_id"].toString().toIntOrNull(),
                                            createdAt = createdAt,
                                            updatedAt = updatedAt,
                                            dataJson = Gson().toJson(layer["data"]),
                                            cropEnabled = layer["crop_enabled"] as Boolean,
                                            cropPercent = (layer["crop_percent"] as Number).toDouble(),
                                            syncedAt = System.currentTimeMillis()
                                        )
                                    }
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        layerRepository.insertAll(entities)
                                    }

                                    entities.forEach {
                                        val gson = Gson()
                                        val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
                                        val data: Map<String, Any?> = mapAdapter.fromJson(it.dataJson)

                                        if(it.type =="points"){
                                            val points = data.get("points") as List<Map<String, Any?>>
                                            Log.d("JSON_DEBUG", "${points}")
                                            val pointsEntities = points.map { point ->
                                                val createdAt = formatter.parse(point["created_at"] as String)?.time ?: 0L
                                                val updatedAt = formatter.parse(point["updated_at"] as String)?.time ?: 0L
                                                LayerPointEntity(
                                                    id = (point["id"] as Number).toInt(),
                                                    layerId = (point["layer_id"] as Number).toInt(),
                                                    lat = (point["lat"] as Number).toDouble(),
                                                    lng = (point["lng"] as Number).toDouble(),
                                                    num = (point["num"] as Number).toInt(),
                                                    valuesJson = Gson().toJson(point["values"]), // сериализация Map → JSON строка
                                                    createdAt = createdAt,
                                                    updatedAt = updatedAt
                                                )
                                            }
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                layerRepository.insertAllPoints(pointsEntities)
                                            }
                                        }
                                        if(it.type == "library_images"){
                                            val images = data.get("images") as List<Map<String, Any?>>
                                            val imagesEntities = images.mapNotNull { image ->
                                                val createdAt = formatter.parse(image["created_at"] as String)?.time ?: 0L
                                                val updatedAt = formatter.parse(image["updated_at"] as String)?.time ?: 0L
                                                LayerImageEntity(
                                                    id = (image["id"] as? Number)?.toInt() ?: return@mapNotNull null,
                                                    uuid = image["uuid"] as? String ?: return@mapNotNull null,
                                                    filename = image["filename"] as? String ?: "",
                                                    originalFilename = image["original_filename"] as? String ?: "",
                                                    gisObjectLayerId = (image["gis_object_layer_id"] as? Number)?.toInt() ?: 0,
                                                    lat = (image["lat"] as? Number)?.toDouble() ?: 0.0,
                                                    lng = (image["lng"] as? Number)?.toDouble() ?: 0.0,
                                                    num = (image["num"] as? Number)?.toInt() ?: 0,
                                                    description = image["description"] as? String,
                                                    createdAt = createdAt,
                                                    updatedAt = updatedAt,
                                                    localPath = null,
                                                    lastAccessedAt = null
                                                )
                                            }

                                            lifecycleScope.launch(Dispatchers.IO) {
                                                layerRepository.insertAllImages(imagesEntities)
                                            }
                                        }
                                    }*/
                }
                thread.start()
            }
            else{
                Log.d("TAG12", "cord: ${lat} ${lng}")
                lifecycleScope.launch {
                    selectedPlan = plan
                    Log.d("TAG12", "cord: ${selectedPlan}")

                    val list = planRepository.getPlanFiles(plan.plainId).first()
                    list.forEach { file ->
                        if(file.centerLat != null && file.centerLng != null){
                            lat = file.centerLat
                            lng = file.centerLng
                        }
                        //Log.d("TAG", "Layer: id=${layer.id}, name=${layer.name} ${layer}")
                    }
                    Log.d("TAG12", "cord2: ${lat} ${lng}")
                    val newCameraPosition = CameraPosition(Point(lat, lng),
                        cameraPosition.zoom, cameraPosition.azimuth, cameraPosition.tilt)
                    mapView.mapWindow.map.move(newCameraPosition)
                    planPointsMap.clear()
                    pointsCollectionList.clear()
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
            val dialog = PointDataDialogFragment(id.toString().toDouble().toInt(), layerRepository, tableRepository)
            dialog.show(supportFragmentManager, "point_data_dialog")
            /*val message = Toast.makeText(this,"Это дерево ${id}",Toast.LENGTH_SHORT)
            message.show()*/
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
                //imageUri = Uri.parse("android.resource://${packageName}/drawable/brand_image_dark")
            ) { annotatedBitmap ->
                // annotatedBitmap — изображение с нарисованными прямоугольниками от OpenCV
                // или сохранить на диск
            }
            dialog.show()
        }else{
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

    private lateinit var imageProvider: ImageProvider
    fun DrawObjectsOnMap()
    {
        runOnUiThread{
            val bitmap = getBitmapFromVectorDrawable(this, R.drawable.map_marker)
            imageProvider = ImageProvider.fromBitmap(bitmap)
            val mapObjects = mapView.mapWindow.map.mapObjects
            if(isInternetAvailable()){
                if(!planPointsMap.isEmpty()){
                    planPointsMap.forEach { uuid, planPointsMap  ->
                        var colorInt =  Color.parseColor("#FFA500")
                        if(planPointsMap.get("color") != null)
                            colorInt = Color.parseColor(planPointsMap.get("color").toString())
                        val tempLibraryImagesClusterListener = createLibraryImagesClusterListener(colorInt)
                        val tempLibraryImagesCollection = mapObjects.addClusterizedPlacemarkCollection(tempLibraryImagesClusterListener)

                        val data  = planPointsMap.get("data") as Map<String, Any?>
                        val points = data.get("points") as List<Map<String, Any?>>
                        points.forEach {point ->
                            tempLibraryImagesCollection.addPlacemark().apply {
                                geometry = Point(point.get("lat") as Double, point.get("lng") as Double)
                                setUserData(point.get("id"))
                                setText((point.get("num") as Number).toInt().toString())
                                setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                addTapListener(globalListener)
                            }
                        }
                        pointsClusterListenerList.add(tempLibraryImagesClusterListener)
                        pointsCollectionList.add(tempLibraryImagesCollection)
                    }
                    pointsCollectionList.forEach {
                        it.clusterPlacemarks(50.0, 17)
                    }
                }

                if(!planLibraryImagesMap.isEmpty()){
                    Log.d("planLibraryImagesMap", "${planLibraryImagesMap}")
                    planLibraryImagesMap.forEach { uuid, planLibraryImagesMap  ->
                        var colorInt =  Color.parseColor("#FFA500")
                        if(planLibraryImagesMap.get("color") != null)
                            colorInt = Color.parseColor(planLibraryImagesMap.get("color").toString())

                        //currentLibraryImagesClusterListener = createLibraryImagesClusterListener(colorInt)
                        //libraryImagesCollection = mapObjects.addClusterizedPlacemarkCollection(currentLibraryImagesClusterListener)
                        val tempLibraryImagesClusterListener = createLibraryImagesClusterListener(colorInt)
                        val tempLibraryImagesCollection = mapObjects.addClusterizedPlacemarkCollection(tempLibraryImagesClusterListener)

                        val data  = planLibraryImagesMap.get("data") as Map<String, Any?>
                        val images = data.get("images") as List<Map<String, Any?>>
                        Log.d("LibraryImages", "${uuid}")
                        images.forEach {image ->
                            tempLibraryImagesCollection.addPlacemark().apply {
                                geometry = Point(image.get("lat") as Double, image.get("lng") as Double)
                                val imageData = mutableMapOf<String, Any?>("filename" to image.get("filename"),
                                    "num" to image.get("num"), "uuid" to uuid)
                                setUserData(imageData)

                                setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                addTapListener(libraryImagesListener)
                            }
                        }
                        libraryImagesClusterListenerList.add(tempLibraryImagesClusterListener)
                        libraryImagesCollectionList.add(tempLibraryImagesCollection)
                    }
                    libraryImagesCollectionList.forEach {
                        it.clusterPlacemarks(50.0, 17)
                    }
                }
            }else{
                Log.d("TAG12", "plan1: ${selectedPlan}")
                lifecycleScope.launch {
                    if (selectedPlan != null){
                        //val layer = layerDao.getLayerIdByUUID(selectedPlan!!.planUUID)

                        val plan = planRepository.getPlanData(selectedPlan!!.plainId)
                        plan.layers.forEach {layer->
                            var colorInt =  Color.parseColor("#FFA500")
                            if(layer.color != null)
                                colorInt = Color.parseColor(layer.color)
                            val tempPointsClusterListener = createLibraryImagesClusterListener(colorInt)
                            val tempPointsCollection = mapObjects.addClusterizedPlacemarkCollection(tempPointsClusterListener)

                            when(layer.type){
                                "points" -> {
                                    val pointList = layerDao.getPointsByLayerId(layer.id).first()
                                    pointList.forEach { point ->
                                        tempPointsCollection.addPlacemark().apply {
                                            geometry = Point(point.lat, point.lng)
                                            setUserData(point.id)
                                            setText(point.num.toString())
                                            setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                            addTapListener(globalListener)
                                        }
                                    }
                                    pointsClusterListenerList.add(tempPointsClusterListener)
                                    pointsCollectionList.add(tempPointsCollection)
                                }
                                "library_images" -> {
                                    val imageList = layerDao.getImagesByLayerId(layer.id).first()
                                    imageList.forEach { image ->
                                        tempPointsCollection.addPlacemark().apply {
                                            geometry = Point(image.lat, image.lng)
                                            val imageData = mutableMapOf<String, Any?>("filename" to image.filename,
                                                "num" to image.num, "uuid" to image.uuid)
                                            setUserData(imageData)
                                            setIcon(clusterIcon(bgColor = colorInt, textColor = 0xFFFFFFFF.toInt()))
                                            addTapListener(libraryImagesListener)
                                        }
                                    }
                                    pointsClusterListenerList.add(tempPointsClusterListener)
                                    pointsCollectionList.add(tempPointsCollection)
                                }
                            }
                        }

                        pointsCollectionList.forEach {
                            it.clusterPlacemarks(50.0, 17)
                        }
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
    val bmp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    p.color = bgColor
    c.drawCircle(30f, 30f, 15f, p)

    p.color = textColor
    p.textSize = 26f
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.DEFAULT_BOLD

    return ImageProvider.fromBitmap(bmp)
}