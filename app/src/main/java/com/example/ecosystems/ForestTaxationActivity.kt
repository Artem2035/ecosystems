package com.example.ecosystems

import SecureTokenManager
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
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

class ForestTaxationActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private lateinit var token:String

    private var currentCameraPosition: CameraPosition = CameraPosition(Point(61.78932, 34.35919),
        17f, 0.0f, 0.0f) //57.907, 36.58 4.67f
    private lateinit var clusterizedCollection: ClusterizedPlacemarkCollection

    private var isSelecting = false
    private val selectedPoints = mutableListOf<Point>()
    private lateinit var selectButton: AppCompatButton
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var tempObjects: MapObjectCollection
    private val planLayersMap = mutableMapOf<String, List<Layer>>()
    private val tileProviders = mutableListOf<OsmTileProvider>()

    private var listOfPlans: MutableList<Map<String, Any?>> = mutableListOf()
    private var listOfPlanLayers: MutableList<Map<String, Any?>> = mutableListOf()
    private val tileFileNames = mutableListOf<String>()
    private var planList: MutableList<Plan> = mutableListOf()
    private var tempPlanList: MutableList<Plan> = mutableListOf()
    private var plansMap: MutableMap<String, Map<String, Any?>> = mutableMapOf()
    /*слои с точками*/
    private var pointsLayerMap: MutableMap<String, Any?> = mutableMapOf()
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
        startActivity(intent)
    }

/*    private fun drawPolygon2() {
        val mapObjects: MapObjectCollection = mapView.mapWindow.map.mapObjects

        // 4 точки (можно любые)
        val point1 = Point(55.751244, 37.618423)
        val point2 = Point(57.907, 36.58)
        val point3 = Point(30.0, 30.3)
        val point4 = Point(25.0, 32.618423)

        // Создаём полигон
        val outerRing = LinearRing(listOf(point1, point2, point3, point4, point1))
        val polygon = Polygon(outerRing, emptyList())

        val polygonObj: PolygonMapObject = mapObjects.addPolygon(polygon)

        // Настраиваем цвета
        polygonObj.fillColor = Color.argb(100, 0, 255, 0) // полупрозрачная зелёная заливка
        polygonObj.strokeColor = Color.GRAY // граница
        polygonObj.strokeWidth = 1f
    }*/

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
        Log.d("tile add", "1")
        return customLayer as Layer
    }

    var selectedPlan: Plan? = null
    private fun showSearchPlanDialog(){
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
                    if(file.get("bound_1_lat") != null){
                        lat = file.get("bound_1_lat").toString().toDouble()
                    }
                    if(file.get("bound_1_lng") != null){
                        lng = file.get("bound_1_lng").toString().toDouble()
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
                    if(file.get("bound_1_lat") != null){
                        lat = file.get("bound_1_lat").toString().toDouble()
                    }
                    if(file.get("bound_1_lng") != null){
                        lng = file.get("bound_1_lng").toString().toDouble()
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
                    pointsLayerMap.clear()
                    for(layer in listOfPlanLayers){
                        if(layer.get("type") == "points"){
                            layer.forEach { (key, value) ->
                                if(value is Map<*, *> || value is List<*>)
                                    pointsLayerMap[key] = deepCopy(value as Map<String, Any?>)
                                else
                                    pointsLayerMap[key] = value
                            }
                        }
                    }
                    if(!pointsLayerMap.isEmpty())
                        DrawDevicesOnMap()
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

    private lateinit var imageProvider: ImageProvider
    fun DrawDevicesOnMap()
    {
        runOnUiThread{
            val bitmap = getBitmapFromVectorDrawable(this, R.drawable.map_marker)
            imageProvider = ImageProvider.fromBitmap(bitmap)
            // Создаём коллекцию с кластеризацией
            clusterizedCollection = mapView.mapWindow.map.mapObjects
                .addClusterizedPlacemarkCollection(clusterListener)

            val data  = pointsLayerMap.get("data") as Map<String, Any?>
            val points = data.get("points") as List<Map<String, Any?>>
            points.forEach {point ->
                clusterizedCollection.addPlacemark().apply {
                    geometry = Point(point.get("lat") as Double, point.get("lng") as Double)
                    setUserData(point.get("id"))
                    setIcon(imageProvider)
                    addTapListener(globalListener)
                }
            }
            // Запускаем кластеризацию
            clusterizedCollection.clusterPlacemarks(50.0, 17)
        }
    }
}

fun deepCopy(map: Map<String, Any?>): Map<String, Any?> {
    return map.mapValues { (_, value) ->
        when (value) {
            is Map<*, *> -> deepCopy(value as Map<String, Any?>)
            is List<*> -> value.toList()
            else -> value
        }
    }
}