package com.example.ecosystems

import SecureTokenManager
import android.annotation.SuppressLint
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.PhotoViewDialog.ImageAnnotationDialog
import com.example.ecosystems.Plan.PlanAdapter
import com.example.ecosystems.Plan.PlanInfoActivity
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.OsmTileProvider
import com.example.ecosystems.utils.getBitmapFromVectorDrawable
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
import java.util.Locale

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
    //private var pointsLayerMap: MutableMap<String, Any?> = mutableMapOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest_taxation)

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
            if (selectedPlan != null) {
                val intent =  Intent(this,PlanInfoActivity::class.java)
                val json = Gson().toJson(listOfPlanLayers)
                intent.putExtra("listOfPlanLayers", json)
                startActivity(intent)
            } else {
                val message = Toast.makeText(this,"ГИС не выбран!",Toast.LENGTH_SHORT)
                message.show()
            }
        }

        val searchPlanButton = findViewById<ImageButton>(R.id.searchPlanImageButton)
        searchPlanButton.setOnClickListener {
            showSearchPlanDialog()
        }

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
                        planList.add(Plan(planUUID, plan.get("name").toString(), plan.get("description").toString()))
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
        bundle.putSerializable("planPointsMap", planPointsMap as java.io.Serializable)
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

        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_plan_search, null)
        val planRecyclerView: RecyclerView = dialogView.findViewById(R.id.plans_recycler_view)

        val builder = AlertDialog.Builder(this)
            .setTitle("Выбрать план")
            .setView(dialogView)
            .setNegativeButton("Отмена", null) // просто закроет диалог

        val dialog = builder.create()

        val planAdapter = PlanAdapter(tempPlanList) { plan ->
            mapView.mapWindow.map.mapObjects.clear()

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
            val cameraPosition = mapView.map.cameraPosition
            var lat = cameraPosition.target.latitude
            var lng = cameraPosition.target.longitude
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
            }
            thread.start()
            dialog.dismiss()
        }
        planRecyclerView.layoutManager = LinearLayoutManager(this)
        planRecyclerView.adapter = planAdapter


        //поиск
        val searchView = dialogView.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempPlanList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if(!searchText.isEmpty()){

                    planList.forEach {
                        if(it.name.lowercase(Locale.getDefault()).contains(searchText) ||
                            it.description.lowercase(Locale.getDefault()).contains(searchText)){
                            tempPlanList.add(it)
                        }
                    }

                    planAdapter.notifyDataSetChanged()
                }else{
                    tempPlanList.clear()
                    tempPlanList.addAll(planList)
                    planAdapter.notifyDataSetChanged()
                }
                return false
            }
        })

        dialog.show()

        val negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        negative.setTextColor(Color.BLACK)

        val button = dialog.findViewById<ImageButton>(R.id.showMap)
        button?.setOnClickListener {
            dialog.dismiss()
        }
    }
    // Обработка нажатия конкретно на объекты (маркеры)
    private val globalListener = MapObjectTapListener { mapObject, point ->
        val id = mapObject.userData

        if(id != null){
            val message = Toast.makeText(this,"Это дерево ${id}",Toast.LENGTH_SHORT)
            message.show()
        }
        true
    }

    private val libraryImagesListener = MapObjectTapListener { mapObject, point ->
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
    /*иконки кластера для слоя типа "точки"*/
    private val clusterListener = ClusterListener { cluster ->
        cluster.appearance.setIcon(imageProvider)
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