package com.example.ecosystems.Plan

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.R
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import kotlinx.coroutines.launch

class PlanInfoDialogFragment(private val selectedPlan: Plan,
                             private var planRepository: PlanRepository,
                             private var layerRepository: LayerRepository): DialogFragment() {

/*    override fun onCreate(savedInstanceState: Bundle?) {
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

        val backButton: AppCompatButton = findViewById(R.id.planInfoBackButton)
        // Кнопка для активации режима выбора
        backButton.setOnClickListener {
            val intent =  Intent(this, ForestTaxationActivity::class.java)
            startActivity(intent)
        }
    }*/

    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.activity_plan_info, null)

        val backButton = view.findViewById<Button>(R.id.planInfoBackButton)

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
            .setView(view)
            .create()

        backButton.setOnClickListener {
            dismiss()
        }

        lifecycleScope.launch{
            val plan = planRepository.getPlanData(selectedPlan.plainId)
            if(plan == null)
                return@launch
        }


        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}