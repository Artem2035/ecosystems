package com.example.ecosystems.DeviceDataTable

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.example.ecosystems.R
import com.google.android.material.chip.ChipGroup

@SuppressLint("SetTextI18n")
fun showDataWindow(index: Int, listOfDevices: MutableList<Map<String, Any?>>,
                   mapOfDeviceParameters: MutableMap<String, Map<String, Any?>>,listOfDeviceParametertsNames: Map<String, String>, view: Context)
{
    val layoutInflater = LayoutInflater.from(view)
    val dialog = layoutInflater.inflate(R.layout.device_data, null)
    val container = Dialog(view)
    container.setContentView(dialog)
    container.setCancelable(true)
    val width = ViewGroup.LayoutParams.MATCH_PARENT
    val height = ViewGroup.LayoutParams.MATCH_PARENT
    container.window?.setLayout(width, height)
    container.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
    container.show()

    val table = dialog.findViewById<TableLayout>(R.id.dataTable)

    val device = listOfDevices.get(index)
    addRowHeader(table,"Описание устройства", view)
    addRow(table, "name",device,listOfDeviceParametertsNames, view)
    addRow(table, "location_description",device,listOfDeviceParametertsNames, view)
    addRow(table, "last_update_datetime",device,listOfDeviceParametertsNames, view)
    addRow(table, "latitude",device,listOfDeviceParametertsNames, view)
    addRow(table, "longitude",device,listOfDeviceParametertsNames, view)
    addRowHeader(table,"Показатели с устройства", view)

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
        val tableRow = TableRow(view)
        tableRow.layoutParams = ViewGroup.LayoutParams(ChipGroup.LayoutParams.MATCH_PARENT, ChipGroup.LayoutParams.WRAP_CONTENT)
        val text = TextView(view)
        Log.d("param","${parameter}")
        text.text = "${parameter.get("label")}:"
        tableRow.addView(text)
        val text2 = TextView(view)
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

fun addRow(table: TableLayout, parametr: String, device: Map<String, Any?>,
           listOfDeviceParametertsNames:  Map<String, String>, view: Context)
{
    val tableRow = TableRow(view)
    tableRow.layoutParams = ViewGroup.LayoutParams(ChipGroup.LayoutParams.MATCH_PARENT, ChipGroup.LayoutParams.WRAP_CONTENT)
    val text = TextView(view)
    Log.d("param", parametr)
    text.text = "${listOfDeviceParametertsNames.get(parametr)} :"
    //text.layoutParams = TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f)
    tableRow.addView(text)
    val text2 = TextView(view)
    text2.text = "${device.get(parametr)}"
    text2.layoutParams = TableRow.LayoutParams(ChipGroup.LayoutParams.MATCH_PARENT, ChipGroup.LayoutParams.WRAP_CONTENT, 1.0f)
    tableRow.addView(text2)
    table.addView(tableRow);
}

fun addRowHeader(table: TableLayout, header: String, view: Context)
{
    val tableRow = TableRow(view)
    tableRow.layoutParams = ViewGroup.LayoutParams(ChipGroup.LayoutParams.MATCH_PARENT, ChipGroup.LayoutParams.WRAP_CONTENT)
    val text = TextView(view)
    text.textAlignment = ChipGroup.TEXT_ALIGNMENT_CENTER
    text.text = header
    tableRow.addView(text)
    table.addView(tableRow);
}