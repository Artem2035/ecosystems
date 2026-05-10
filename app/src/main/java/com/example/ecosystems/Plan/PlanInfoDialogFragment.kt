package com.example.ecosystems.Plan

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.LayerDisplayItem
import com.example.ecosystems.DataClasses.Plan
import com.example.ecosystems.R
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.PlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlanInfoDialogFragment(private val selectedPlan: Plan,
                             private var planRepository: PlanRepository,
                             private var layerRepository: LayerRepository,
                             private val layersVisibility: MutableMap<Int, Boolean>,
                             private val onLayersVisibilityChange: (MutableMap<Int, Boolean>) -> Unit) : DialogFragment() {

    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_plan_info, null)

        val planName: TextView = view.findViewById(R.id.info_plan_name)
        val backButton = view.findViewById<Button>(R.id.planInfoBackButton)

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar)
            .setView(view)
            .create()

        val recyclerView = view.findViewById<RecyclerView>(R.id.layersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        backButton.setOnClickListener {
            onLayersVisibilityChange(layersVisibility)
            dismiss()
        }

        lifecycleScope.launch{
            val plan = planRepository.getPlanById(selectedPlan.plainId)
            if(plan == null)
                return@launch

            planName.setText(plan.name)

            val items = buildLayerItems()
            val adapter = LayerListAdapter(
                items = items,
                onLayerClick = { item ->
                    // детали слоя поверх текущего диалога
                },
                onVisibilityToggle = { item, isVisible ->
                    layersVisibility[item.layer.id] = isVisible
                }
            )
            recyclerView.adapter = adapter
        }

        return dialog
    }

    // Сборка LayerDisplayItem
    private suspend fun buildLayerItems(): List<LayerDisplayItem> {
        return withContext(Dispatchers.IO) {
            layerRepository.getLayersByPlanId(selectedPlan.plainId)
                ?.map { layer ->
                    LayerDisplayItem(
                        layer = layer,
                        isVisible = layersVisibility[layer.id] ?: true
                    )
                } ?: emptyList()
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}