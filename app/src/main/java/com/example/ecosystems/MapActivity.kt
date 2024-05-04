package com.example.ecosystems

import android.annotation.SuppressLint
import android.app.Dialog
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
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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


class MapActivity : AppCompatActivity(), OnMapReadyCallback {


    inner class MyMarker(pos: LatLng, title: String, ) : ClusterItem
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
                val token = GetToken()
                Log.d("Get devices","devices")
                GetDevices(token)
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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isBuildingsEnabled = true
        clusterManager = ClusterManager(this, mMap)
        clusterManager.setOnClusterItemClickListener { marker ->
            val index = marker.title.toInt()
            showDataWindow(index)
            true
        }
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        val addMarkerButton:CardView = findViewById(R.id.addMarker)
        addMarkerButton.setOnClickListener {
            val xCoordinate = findViewById(R.id.editTextNumber) as EditText
            val yCoordinate = findViewById(R.id.editTextNumber2) as EditText
            if(!xCoordinate.text.isNullOrEmpty() && !yCoordinate.text.isNullOrEmpty())
            {
                val x: Double = xCoordinate.text.toString().toDouble()
                val y: Double = yCoordinate.text.toString().toDouble()
                val newPoint = LatLng(x, y)
                mMap.addMarker(MarkerOptions().position(newPoint).title("Some new point on the map"))
            }
        }
    }

    fun closeButton(view: View) {
        startMainActivity()
    }

    @SuppressLint("SetTextI18n")
    fun showDataWindow(index: Int)
    {
        val dialog = layoutInflater.inflate(R.layout.device_data, null)
        val container = Dialog(this)
        container.setContentView(dialog)
        container.setCancelable(true)
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        container.window?.setLayout(width, height)
        container.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        container.show()

        val table = dialog.findViewById<TableLayout>(R.id.dataTable)

        val device = listOfDevices.get(index)
        addRowHeader(table,"Описание устройства")
        addRow(table, "name",device)
        addRow(table, "location_description",device)
        addRow(table, "last_update_datetime",device)
        addRow(table, "latitude",device)
        addRow(table, "longitude",device)
        addRowHeader(table,"Показатели с устройства")

        val deviceTypeId = if(device.get("device_type_id").toString() == "") ""
        else (device.get("device_type_id").toString().toDouble().toInt()).toString()

        var moduleTypeId = device.get("module_type_id")
        if(moduleTypeId == null)
            moduleTypeId = ""
        val lastParameters = device.get("last_parameter_values") as Map<String, Any?>
        for(parametr in lastParameters.keys)
        {
            val paramId = "${deviceTypeId}_${moduleTypeId}_${parametr}"
            val parameter = mapOfDeviceParameters.get(paramId)
            if(parameter == null)
                continue
            val tableRow = TableRow(this)
            tableRow.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val text = TextView(this)
            Log.d("param","${parameter}")
            text.text = "${parameter.get("label")}:"
            tableRow.addView(text)
            val text2 = TextView(this)
            val param = if(lastParameters[parametr] != null) lastParameters[parametr] else "Нет данных"
            text2.text = "${param}"
            tableRow.addView(text2)
            table.addView(tableRow);
        }

        val button = dialog.findViewById<Button>(R.id.button)
        button.setOnClickListener {
            container.dismiss()
        }
    }

    fun addRow(table: TableLayout, parametr: String, device: Map<String, Any?>)
    {
        val tableRow = TableRow(this)
        tableRow.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val text = TextView(this)
        Log.d("param", parametr)
        text.text = "${listOfDeviceParametertsNames.get(parametr)} :"
        //text.layoutParams = TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f)
        tableRow.addView(text)
        val text2 = TextView(this)
        text2.text = "${device.get(parametr)}"
        text2.layoutParams = TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f)
        tableRow.addView(text2)
        table.addView(tableRow);
    }

    fun addRowHeader(table: TableLayout, header: String)
    {
        val tableRow = TableRow(this)
        tableRow.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val text = TextView(this)
        text.textAlignment = TEXT_ALIGNMENT_CENTER
        text.text = header
        tableRow.addView(text)
        table.addView(tableRow);
    }

    fun startMainActivity()
    {
        val intent =  Intent(this,MainActivity::class.java)
        startActivity(intent)
    }

    @WorkerThread
    fun GetToken():String
    {
        val client = OkHttpClient()

        var token:String = ""

        val MEDIA_TYPE = "application/json".toMediaType()
        val requestBody = "{\"login\":\"admin\",\"password\":\"PetrSU2022*\"}"

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/token")
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

            token = result.get("access_token").toString()
            Log.d("Token","token = ${token}")
        }
        return token
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

        Log.d("get devices", "Success")
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