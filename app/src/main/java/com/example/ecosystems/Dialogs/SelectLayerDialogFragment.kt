package com.example.ecosystems.Dialogs

import android.R
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.ecosystems.db.entity.layer.LayerEntity

class SelectLayerDialogFragment(
    private val layers: List<LayerEntity>,
    private val onConfirmed: (LayerEntity, num: Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        //UI
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 16)
        }

        // Поле ввода номера точки
        val layerLabel = TextView(context).apply {
            text = "Выберете слой"
            textSize = 13f
        }
        // Спиннер выбора слоя
        val layerSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                R.layout.simple_spinner_dropdown_item,
                layers.map { it.name }
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 24 }
        }

        // Поле ввода номера точки
        val numLabel = TextView(context).apply {
            text = "Номер точки"
            textSize = 13f
        }
        val numInput = EditText(context).apply {
            hint = "Введите номер"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }

        container.addView(layerLabel)
        container.addView(layerSpinner)
        container.addView(numLabel)
        container.addView(numInput)

        val dialog = AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Создать", null) // null — не закрываем автоматически
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val numText = numInput.text.toString().trim()
                if (numText.isBlank()) {
                    numInput.error = "Обязательное поле"
                    return@setOnClickListener
                }
                val selectedLayer = layers[layerSpinner.selectedItemPosition]
                onConfirmed(selectedLayer, numText.toInt())
                dialog.dismiss()
            }
        }

        return dialog
    }
}