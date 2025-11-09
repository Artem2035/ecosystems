package com.example.ecosystems

import SecureTokenManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.Device
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

import com.example.ecosystems.DeviceDataTable.showDataWindow
import com.example.ecosystems.data.local.SecureDevicesParametersManager
import com.example.ecosystems.utils.isInternetAvailable
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Cluster
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.io.Serializable

class MapActivity : AppCompatActivity()  {

    private var currentCameraPosition: CameraPosition = CameraPosition(
        Point(57.907,36.58),
        4.67F,0.0F,0.0F)

   //  recycle view
    private lateinit var devicesRecyclerView: RecyclerView
    private var devicesList: MutableList<Device> = mutableListOf()

    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()

    private var tempDevicesList: MutableList<Device> = mutableListOf()
    private lateinit var token:String

    var listOfDeviceParametertsNames: Map<String, String> = mapOf("name" to "Название",
        "location_description" to "Описание местности", "last_update_datetime" to "Последнее обновление",
        "latitude" to "Широта","longitude" to "Долгота")
    var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    var mapOfDeviceParameters: MutableMap<String, Map<String, Any?>> = mutableMapOf()
    var mapOfDevicesDeviceParameters: MutableMap<String, MutableMap<*, *>> = mutableMapOf()

    private lateinit var mapView: MapView
    private lateinit var clusterizedCollection: ClusterizedPlacemarkCollection
    private lateinit var devicesLayout: View
    private lateinit var backButton: AppCompatButton
    private lateinit var profileImageButton: ImageButton
    private lateinit var imageButton: ImageButton
    private lateinit var taxationImageButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.map)
        mapView.mapWindow.map.move(currentCameraPosition)
        devicesLayout = findViewById(R.id.devicesLayout)
        backButton = findViewById(R.id.backButton)
        profileImageButton = findViewById(R.id.profileImageButton)
        imageButton = findViewById(R.id.imageButton)
        taxationImageButton = findViewById(R.id.taxationImageButton)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!
        val devicesManager = SecureDevicesParametersManager(this)
        val devicesManagerData = devicesManager.loadData()

        if(isInternetAvailable()){
            Thread {
                try {
                    Log.d("Get devices","devices")
                    GetDevices(token)
                    var index: Int = 0
                    for (device in listOfDevices){
                        val name = device.get("name").toString()
                        val location = device.get("location_description").toString()
                        val lastUpdate = device.get("last_update_datetime").toString()
                        val deviceItem = Device(device.get("device_id").toString().toDouble().toInt(), name, location, lastUpdate)
                        devicesList.add(deviceItem)
                        index += 1

                        mapOfDevices.put(device.get("device_id").toString().toDouble().toInt(), device)
                    }

                    mapOfDevicesDeviceParameters.put("mapOfDevices",mapOfDevices)
                    mapOfDevicesDeviceParameters.put("mapOfDeviceParameters",mapOfDeviceParameters)
//                    Log.d("mdevices", mapOfDevicesDeviceParameters.toString())
                    devicesManager.saveData(mapOfDevicesDeviceParameters)
                    DrawDevicesOnMap()

                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(this,"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                        message.show()
                        startMainActivity()
                    }
                }
                //Do some Network Request
            }.start()
        }
        else{
            Thread {
                if(devicesManagerData.get("mapOfDevices") != null &&
                    devicesManagerData.get("mapOfDeviceParameters") != null  ){
                    mapOfDevices = devicesManagerData.get("mapOfDevices") as MutableMap<Int, Map<String, Any?>>
                    mapOfDeviceParameters = devicesManagerData.get("mapOfDeviceParameters") as MutableMap<String, Map<String, Any?>>
                    Log.d("params","${devicesManagerData.get("mapOfDevices")}")
                    DrawDevicesOnMap()
                }
            }.start()
        }
    }

    fun closeButton(view: View) {
        startMainActivity()
    }

    fun showListOfDevices(view: View){
//        currentCameraPosition = mMap.cameraPosition
        showDeviceList()
        //setContentView(R.layout.activity_map_list_of_devices)

        devicesRecyclerView = findViewById(R.id.devices_recycler_view)
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        tempDevicesList.addAll(devicesList)
        val deviceAdapter: DeviceAdapter = DeviceAdapter(tempDevicesList, mapOfDevices, mapOfDeviceParameters, listOfDeviceParametertsNames)
        devicesRecyclerView.adapter = deviceAdapter

        //поиск
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempDevicesList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if(!searchText.isEmpty()){
                    devicesList.forEach {
                        if(it.heading.lowercase(Locale.getDefault()).contains(searchText)){
                            tempDevicesList.add(it)
                        }
                    }
                    deviceAdapter.notifyDataSetChanged()
                }else{
                    tempDevicesList.clear()
                    tempDevicesList.addAll(devicesList)
                    deviceAdapter.notifyDataSetChanged()
                }
                return false
            }
        })

        val button = findViewById<ImageButton>(R.id.showMap)
        button.setOnClickListener {
            showMap()
            //setContentView(R.layout.activity_map)
            Log.d("test",listOfDevices.toString())

            Log.d("get devices", "Success")
            DrawDevicesOnMap()
        }
    }

    fun startMainActivity()
    {
        val intent =  Intent(this,MainActivity::class.java)
        startActivity(intent)
    }

    fun startProfileActivity(view: View)
    {
        val intent =  Intent(this,PersonalAccount::class.java)
        val bundle = Bundle()
        //bundle.putSerializable("listOfDevices", listOfDevices as Serializable)
        bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    fun startForestTaxationActivity(view: View){
        val intent =  Intent(this,ForestTaxationActivity::class.java)
        startActivity(intent)
    }

    @WorkerThread
    fun GetDevices(token: String)
    {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/devices_lite?timeoffset=-3&device_type=NaN")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.7")
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

            listOfDevices= result.get("devices") as MutableList<Map<String, Any?>>
            mapOfDeviceParameters = result.get("deviceParameters") as MutableMap<String, Map<String, Any?>>
        }
    }

    fun DrawDevicesOnMap()
    {
        runOnUiThread{
            val bitmap = getBitmapFromVectorDrawable(this, R.drawable.map_marker)
            val imageProvider = ImageProvider.fromBitmap(bitmap)
            // Создаём коллекцию с кластеризацией
            clusterizedCollection = mapView.mapWindow.map.mapObjects.addClusterizedPlacemarkCollection(object : ClusterListener {
                override fun onClusterAdded(cluster: Cluster) {
                    // Устанавливаем иконку кластера
                    cluster.appearance.setIcon(imageProvider)
                    // Можно добавить текст (кол-во элементов)
                    cluster.appearance.setText(cluster.size.toString())

                    // Дополнительно задаём стиль текста (иначе он не виден!)
                    cluster.appearance.setTextStyle(
                        TextStyle().apply {
                            size = 12f
                            color = Color.WHITE
                            placement = TextStyle.Placement.CENTER
                            outlineColor = Color.BLACK
                            outlineWidth = 2f
                        }
                    )

                    // Клик по кластеру — увеличиваем zoom
                    cluster.addClusterTapListener(object : ClusterTapListener {
                        override fun onClusterTap(cluster: Cluster): Boolean {
                            val currentCamera = mapView.mapWindow.map.cameraPosition
                            mapView.mapWindow.map.move(
                                CameraPosition(
                                    cluster.appearance.geometry,
                                    currentCamera.zoom + 1.5f,
                                    0f,
                                    0f
                                )
                            )
                            return true
                        }
                    })
                }
            })

            // Обработка нажатия конкретно на объекты (маркеры)
            val globalListener = MapObjectTapListener { mapObject, point ->
                val deviceId = mapObject.userData
                if (deviceId != null) {
                    //Toast.makeText(this, "Клик по объекту: $deviceId", Toast.LENGTH_SHORT).show()
                    showDataWindow(deviceId.toString().toInt(), mapOfDevices, mapOfDeviceParameters,listOfDeviceParametertsNames, this)
                }
                true
            }

            mapOfDevices.forEach { deviceId, device ->
                clusterizedCollection.addPlacemark().apply {
                    geometry = Point(device.get("latitude") as Double, device.get("longitude") as Double)
                    setUserData(deviceId.toString())
                    setIcon(imageProvider)
                    addTapListener(globalListener)
                }
            }
            // Запускаем кластеризацию
            clusterizedCollection.clusterPlacemarks(60.0, 15)
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

    private fun showDeviceList() {
        mapView.visibility = View.GONE
        backButton.visibility = View.GONE
        profileImageButton.visibility = View.GONE
        imageButton.visibility = View.GONE
        taxationImageButton.visibility = View.GONE
        devicesLayout.visibility = View.VISIBLE
    }

    private fun showMap() {
        devicesLayout.visibility = View.GONE
        mapView.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        profileImageButton.visibility = View.VISIBLE
        imageButton.visibility = View.VISIBLE
        taxationImageButton.visibility = View.VISIBLE
    }
}

fun getBitmapFromVectorDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, drawableId)!!
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
