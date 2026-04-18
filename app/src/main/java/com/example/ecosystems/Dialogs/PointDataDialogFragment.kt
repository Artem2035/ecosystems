package com.example.ecosystems.Dialogs

import android.annotation.SuppressLint
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
                              private val tableRepository: TableRepository) : DialogFragment() {

    private lateinit var pointNumberText: EditText
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var propertiesContainer: LinearLayout

    // propertyId -> EditText, чтобы при сохранении собрать значения
    private val editTextMap = mutableMapOf<Int, EditText>()
    @SuppressLint("MissingInflatedId")
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

        /*lifecycleScope.launch {


            val tableWithProperties = withContext(Dispatchers.IO) {
                tableRepository.getTableWithProperties(tableId)
            }

            layerRepository.getPointWithValuesByPointId(pointId)
                ?.flowOn(Dispatchers.IO)
                ?.collect { list ->
                    val item = list.firstOrNull() ?: return@collect
                    bindData(item)
                }
        }*/

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

            pointNumberText.setText(pointWithValues?.point?.id?.toString() ?: "—")
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

    /*private fun bindData(item: LayerPointWithValues) {
        val point = item.point

        pointNumberText.text = point.num.toString()
        latitudeText.text    = "%.6f".format(point.lat)
        longitudeText.text   = "%.6f".format(point.lng)

        val treeNumber = item.values
            .firstOrNull { it.property.name == "Номер учетного дерева" }
            ?.value?.value
            ?: "—"

        treeNumberText.text = treeNumber

        propertiesContainer.removeAllViews()

        item.values
            .sortedBy { it.property.sortOrder }
            .forEach { addPropertyRow(it) }
    }*/

    /*private fun addPropertyRow(pv: PointValueWithProperty) {
        val label = pv.property.displayName
            ?.takeIf { it.isNotBlank() }
            ?: pv.property.name

        val value = pv.value.value
            .takeIf { it.isNotBlank() }
            ?: "—"

        val units = pv.property.units
            .takeIf { it.isNotBlank() }

        val displayValue = if (units != null) "$value $units" else value

        val context = requireContext()

        // label
        val labelView = TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.gray))
            setPadding(0, 0, 0, dpToPx(4))
        }

        // value
        val valueView = TextView(context).apply {
            text = displayValue
            textSize = 15f
            setBackgroundResource(R.drawable.input_background_registration)
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(12)
            layoutParams = lp
        }

        propertiesContainer.addView(labelView)
        propertiesContainer.addView(valueView)
    }*/

    private fun addPropertyRow(property: TablePropertyEntity, value: String) {
        val label = property.displayName
            ?.takeIf { it.isNotBlank() }
            ?: property.name

/*        val units = property.units.takeIf { it.isNotBlank() }
        val displayValue = if (units != null && value != "—") "$value $units" else value*/

        val context = requireContext()

        val labelView = TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.gray))
            setPadding(0, 0, 0, dpToPx(4))
        }

/*        val valueView = EditText(context).apply {
            setText(displayValue)
            textSize = 15f
            setBackgroundResource(R.drawable.input_background_registration)
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(12) }
        }*/

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
/*        val valuesToSave = editTextMap.map { (propertyId, editText) ->
            PointValueEntity(
                pointId = pointId,
                propertyId = propertyId,
                value = editText.text.toString().trim()
            )
        }*/

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
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}