package com.example.ecosystems.Dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.R
import com.example.ecosystems.db.entity.layer.PointValueEntity
import com.example.ecosystems.db.entity.table.TablePropertyEntity
import com.example.ecosystems.db.repository.LayerRepository
import com.example.ecosystems.db.repository.TableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PointDataDialogFragment(private val pointId:Int,
                              private var layerRepository: LayerRepository,
                              private val tableRepository: TableRepository,
                              private val onPointDeleted: ((pointId: Int) -> Unit)? = null) : DialogFragment() {

    private lateinit var pointNumberText: EditText
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var propertiesContainer: LinearLayout

    // propertyId -> EditText, чтобы при сохранении собрать значения
    private val editTextMap = mutableMapOf<Int, EditText>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater
            .inflate(R.layout.dialog_point_data, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create()

        pointNumberText = view.findViewById(R.id.pointNumberEditText)
        latitudeText    = view.findViewById(R.id.latitudeText)
        longitudeText   = view.findViewById(R.id.longitudeText)
        propertiesContainer = view.findViewById(R.id.propertiesContainer)



        view.findViewById<AppCompatButton>(R.id.closeButton).setOnClickListener {
            dismiss()
        }

        view.findViewById<AppCompatButton>(R.id.savePointValuesButton).setOnClickListener {
            savePointValues()
        }
        view.findViewById<AppCompatButton>(R.id.deletePointButton).setOnClickListener {
            showDeleteConfirmation()
        }

        lifecycleScope.launch {
            val layerId = withContext(Dispatchers.IO){ layerRepository.getLayerIdByPointId(pointId)}
            val tableId = withContext(Dispatchers.IO){layerRepository.getTableIdByLayerId(layerId)}

            if(tableId == null)
                return@launch

            val tableWithProperties = withContext(Dispatchers.IO) {
                tableRepository.getTableWithProperties(tableId)
            }

            val pointWithValues = withContext(Dispatchers.IO) {
                layerRepository.getPointWithValuesByPointId(pointId)
                    ?.first() // берём один снимок, подписка не нужна
                    ?.firstOrNull()
            }

            val valuesMap = pointWithValues?.values
                ?.associate { it.value.propertyId to it.value.value }
                ?: emptyMap()

            pointNumberText.setText(pointWithValues?.point?.num?.toString() ?: "—")
            latitudeText.text    = pointWithValues?.point?.lat?.let { "%.6f".format(it) } ?: "—"
            longitudeText.text   = pointWithValues?.point?.lng?.let { "%.6f".format(it) } ?: "—"

            tableWithProperties?.properties
                ?.sortedBy { it.sortOrder }
                ?.forEach { property ->
                    val value = valuesMap[property.id]
                        ?.takeIf { it.isNotBlank() }
                        ?: ""
                    addPropertyRow(property, value)
                }
        }

        return dialog
    }

    private fun addPropertyRow(property: TablePropertyEntity, value: String) {
        val label = property.displayName
            ?.takeIf { it.isNotBlank() }
            ?: property.name

        val context = requireContext()

        val labelView = TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.gray))
            setPadding(0, 0, 0, dpToPx(4))
        }

        val editText = EditText(context).apply {
            setText(value)
            textSize = 15f
            setBackgroundResource(R.drawable.input_background_registration)
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }

            // Тип ввода в зависимости от типа свойства
            inputType = when (property.propertyType) {
                "INT", "FLOAT" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else -> InputType.TYPE_CLASS_TEXT
            }

            hint = if (property.units.isNotBlank()) property.units else ""
        }

        // Сохраняем EditText по propertyId для сбора значений при сохранении
        editTextMap[property.id] = editText

        propertiesContainer.addView(labelView)
        propertiesContainer.addView(editText)
    }

    private fun savePointValues(){

        val valuesToSave = editTextMap
            .filter { (_, editText) -> editText.text.toString().isNotBlank() }
            .map { (propertyId, editText) ->
                PointValueEntity(
                    pointId = pointId,
                    propertyId = propertyId,
                    value = editText.text.toString().trim()
                )
            }

        lifecycleScope.launch {
            editTextMap.forEach { (propertyId, editText) ->
                if (editText.text.toString().isBlank()) {
                    layerRepository.deletePointValue(pointId, propertyId)
                }
            }
            layerRepository.savePointValues(valuesToSave)
            Handler(Looper.getMainLooper()).post{
                val message = Toast.makeText(requireContext(),"Сохранено!",
                    Toast.LENGTH_SHORT)
                message.show()
            }
            dismiss()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление точки")
            .setMessage("Удалить точку и все её значения?")
            .setPositiveButton("Удалить") { _, _ ->
                deletePoint()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deletePoint() {
        lifecycleScope.launch {
            onPointDeleted?.invoke(pointId)
            withContext(Dispatchers.IO) {
                layerRepository.deletePoint(pointId)
            }
            dismiss()
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}