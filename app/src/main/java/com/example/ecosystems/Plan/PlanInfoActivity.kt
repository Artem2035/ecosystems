package com.example.ecosystems.Plan

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.ecosystems.ForestTaxationActivity
import com.example.ecosystems.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlanInfoActivity : AppCompatActivity() {
    private lateinit var listOfPlanLayers: MutableList<Map<String, Any?>>
    private lateinit var orthophotoplan: MutableMap<String, Map<String, Any?>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_info)

        val json = intent.getStringExtra("listOfPlanLayers")
        val type = object : TypeToken<MutableList<Map<String, Any?>>>() {}.type
        listOfPlanLayers = Gson().fromJson(json, type)

        listOfPlanLayers.forEach { layer ->
            if(layer.get("type") == "orthophotoplan"){
                orthophotoplan = layer as MutableMap<String, Map<String, Any?>>
            }
        }

        val gisFile = orthophotoplan.get("data")?.get("gis_file") as Map<String, Any?>

        var text = ""
        val planName: TextView = findViewById(R.id.info_plan_name)
        text = "${orthophotoplan.get("name")} ${gisFile.get("name")}"
        planName.setText(text)
        val cordCenterTextView: TextView = findViewById(R.id.cordCenterTextView)
        text = "Долгота: ${gisFile.get("center_lng")} Широта: ${gisFile.get("center_lat")}"
        cordCenterTextView.setText(text)
        val bordersTextView: TextView = findViewById(R.id.bordersTextView)
        text = "Точка1: (${gisFile.get("bound_1_lng")}, ${gisFile.get("bound_1_lat")}), \n Точка2: (${gisFile.get("bound_2_lng")}, ${gisFile.get("bound_2_lat")})"
        bordersTextView.setText(text)

        val backButton: AppCompatButton = findViewById(R.id.backButton2)
        // Кнопка для активации режима выбора
        backButton.setOnClickListener {
            val intent =  Intent(this,ForestTaxationActivity::class.java)
            startActivity(intent)
        }
    }
}