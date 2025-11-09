package com.example.ecosystems

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.example.ecosystems.DeviceDataTable.showDataWindow
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Cluster
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.ClusterTapListener
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PolygonMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

private lateinit var mapView: MapView

class ForestTaxationActivity : AppCompatActivity() {
    private var currentCameraPosition: CameraPosition = CameraPosition(Point(57.907, 36.58),
        4.67f, 0.0f, 0.0f)
    private var isSelecting = false
    private val selectedPoints = mutableListOf<Point>()
    private lateinit var selectButton: AppCompatButton
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var tempObjects: MapObjectCollection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forest_taxation)
        mapView = findViewById(R.id.mapView)
        // Мгновенное перемещение
        mapView.mapWindow.map.move(currentCameraPosition)
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
                // Можно ничего не делать
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
    }

    private fun onMapTapped(point: Point) {
        selectedPoints.add(point)

        // Добавляем маркер точки
        tempObjects.addPlacemark().apply {
            geometry = point
            setText("test")
        }

        if(selectedPoints.size < 4){
            Toast.makeText(this, "Выбрано ${selectedPoints.size}/4", Toast.LENGTH_SHORT).show()
        }
        if (selectedPoints.size == 4) {
            drawPolygon()
            isSelecting = false
            selectButton.text = "Выбрать участок"
            Toast.makeText(this, "Участок отмечен!", Toast.LENGTH_SHORT).show()
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
        val intent =  Intent(this,TreesManagementActivity::class.java)
        startActivity(intent)
    }

    private fun drawPolygon2() {
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
}