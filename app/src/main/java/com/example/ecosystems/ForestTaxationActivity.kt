package com.example.ecosystems

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.ecosystems.DeviceDataTable.showDataWindow
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Cluster
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

private lateinit var mapView: MapView

class ForestTaxationActivity : AppCompatActivity() {
    private var currentCameraPosition: CameraPosition = CameraPosition(Point(57.907, 36.58),
        4.67f, 0.0f, 0.0f)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest_taxation)
        mapView = findViewById(R.id.mapView)
        // Мгновенное перемещение
        //mapView.map.move(currentCameraPosition)
        mapView.mapWindow.map.move(currentCameraPosition)
        mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = Point(57.907, 36.58)
            setText("Мой маркер")
            setUserData("custom_data") // Можно сохранить свои данные
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

    fun closeButton(view: View) {
        startMainActivity()
    }

    fun startMainActivity()
    {
        val intent =  Intent(this,MapActivity::class.java)
        startActivity(intent)
    }
}