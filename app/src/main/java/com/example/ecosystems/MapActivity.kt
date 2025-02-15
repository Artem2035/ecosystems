package com.example.ecosystems

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.chip.ChipGroup.LayoutParams
import com.google.android.material.chip.ChipGroup.TEXT_ALIGNMENT_CENTER
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale

import com.example.ecosystems.DeviceDataTable.showDataWindow

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    inner class MyMarker(pos: LatLng, title: String) : ClusterItem
    {

        private val position: LatLng
        private val title: String
        private val snippet: String = ""

        override fun getPosition(): LatLng {
            return position
        }

        override fun getTitle(): String {
            return title
        }

        override fun getSnippet(): String {
            return snippet
        }

        fun getZIndex(): Float {
            return 0f
        }

        init {
            position = pos
            this.title = title
        }
    }

    private lateinit var clusterManager: ClusterManager<MyMarker>
    lateinit var mMap: GoogleMap

    private var mapFragment: SupportMapFragment? = null
    private var currentCameraPosition: CameraPosition = CameraPosition(
        LatLng(57.907,36.58),
        4.67F,0.0F,0.0F)

   //  recycle view
    private lateinit var devicesRecyclerView: RecyclerView
    private var devicesList: MutableList<Device> = mutableListOf()
    private var tempDevicesList: MutableList<Device> = mutableListOf()
    private lateinit var token:String

    var listOfDeviceParametertsNames: Map<String, String> = mapOf("name" to "Название",
        "location_description" to "Описание местности", "last_update_datetime" to "Последнее обновление",
        "latitude" to "Широта","longitude" to "Долгота")
    var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    var mapOfDeviceParameters: MutableMap<String, Map<String, Any?>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Thread {
            try {
                Log.d("Get devices","devices")

                token = intent.getStringExtra("token").toString()
                GetDevices(token)
                var index: Int = 0
                for (device in listOfDevices){
                    val name = device.get("name").toString()
                    val location = device.get("location_description").toString()
                    val lastUpdate = device.get("last_update_datetime").toString()
                    val deviceItem = Device(index, name, location, lastUpdate)
                    devicesList.add(deviceItem)
                    index += 1
                }
                Log.d("get devices", "Success")
                while(!this::clusterManager.isInitialized)
                    Log.d("clusterManager", "Getting Initialized")
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


        // Инициализируем MapFragment только если он еще не был добавлен
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().apply {
                supportFragmentManager.beginTransaction().replace(R.id.map, this).commit()
            }

        mapFragment?.getMapAsync(this)
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isBuildingsEnabled = true
        clusterManager = ClusterManager(this, mMap)
        clusterManager.setOnClusterItemClickListener { marker ->
            val index = marker.title.toInt()
            showDataWindow(index, listOfDevices, mapOfDeviceParameters,listOfDeviceParametertsNames, this)
            true
        }
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition))
    }

    fun closeButton(view: View) {
        startMainActivity()
    }

    fun showListOfDevices(view: View){
        currentCameraPosition = mMap.cameraPosition

        // Удаляем фрагмент перед переключением макета
        supportFragmentManager.findFragmentById(R.id.map)?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }

        setContentView(R.layout.activity_map_list_of_devices)

        devicesRecyclerView = findViewById(R.id.devices_recycler_view)
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)
        tempDevicesList.addAll(devicesList)
        val deviceAdapter: DeviceAdapter = DeviceAdapter(tempDevicesList, listOfDevices, mapOfDeviceParameters, listOfDeviceParametertsNames)
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
            setContentView(R.layout.activity_map)
            mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                ?: SupportMapFragment.newInstance().apply {
                    supportFragmentManager.beginTransaction().replace(R.id.map, this).commit()
                }
            mapFragment?.getMapAsync(this)
            Log.d("test",listOfDevices.toString())

            Log.d("get devices", "Success")
            Log.d("map23",currentCameraPosition.toString())

            DrawDevicesOnMap()
            //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition))
        }
    }

    fun startMainActivity()
    {
        val intent =  Intent(this,MainActivity::class.java)
        startActivity(intent)
    }

    fun startProfileActivity(view: View)
    {
        val intent =  Intent(this,ProfileActivity::class.java)
        intent.putExtra("token", token)
        startActivity(intent)
    }

  /*  @WorkerThread
    fun GetToken():String
    {
        val client = OkHttpClient()

        var token:String = ""

        val MEDIA_TYPE = "application/json".toMediaType()
        val requestBody = "{\"login\":\"admin\",\"password\":\"PetrSU2022*\"}"

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/token")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain")
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

            token = result.get("access_token").toString()
            Log.d("Token","token = ${token}")
        }
        return token
    }*/

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
        if(!this::clusterManager.isInitialized)
        {
            Log.d("Error","Problem with google Maps: clusterManager is not isInitialized!")
            throw Exception("Problem with google Maps: clusterManager is not isInitialized!")
        }
        Handler(Looper.getMainLooper()).post{
            var index: Int = 0
            for (device in listOfDevices)
            {
                val point = LatLng(device.get("latitude") as Double, device.get("longitude") as Double)
                val offsetItem = MyMarker(point, index.toString())
                clusterManager.addItem(offsetItem)
                //mMap.addMarker(MarkerOptions().position(point).title(index.toString()))
                index += 1
            }
        }
    }


}