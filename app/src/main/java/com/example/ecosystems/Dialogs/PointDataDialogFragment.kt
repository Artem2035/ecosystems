package com.example.ecosystems.Dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.R
import com.example.ecosystems.db.relation.LayerPointWithValues
import com.example.ecosystems.db.repository.LayerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class PointDataDialogFragment(private val pointId:Int,
                              private var layerRepository: LayerRepository) : DialogFragment() {

    private lateinit var pointNumberText: TextView
    private lateinit var treeNumberText: TextView
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_point_data, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create()

        pointNumberText = view.findViewById(R.id.pointNumberText)
        treeNumberText  = view.findViewById(R.id.treeNumberText)
        latitudeText    = view.findViewById(R.id.latitudeText)
        longitudeText   = view.findViewById(R.id.longitudeText)


        view.findViewById<AppCompatButton>(R.id.closeButton).setOnClickListener {
            dismiss()
        }

        lifecycleScope.launch {
            layerRepository.getPointWithValuesByPointId(pointId)
                ?.flowOn(Dispatchers.IO)
                ?.collect { list ->
                    val item = list.firstOrNull() ?: return@collect
                    bindData(item)
                }
        }

        return dialog
    }

    private fun bindData(item: LayerPointWithValues) {
        val point = item.point

        pointNumberText.text = point.num.toString()
        latitudeText.text    = "%.6f".format(point.lat)
        longitudeText.text   = "%.6f".format(point.lng)

        // ищем значение по имени свойства — подставьте реальное имя поля
        val treeNumber = item.values
            .firstOrNull { it.property.name == "Номер учетного дерева" }
            ?.value?.value
            ?: "—"

        treeNumberText.text = treeNumber
    }
}