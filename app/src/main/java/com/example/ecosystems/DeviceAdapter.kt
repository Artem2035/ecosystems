package com.example.ecosystems

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

import com.example.ecosystems.DeviceDataTable.showDataWindow

class DeviceAdapter(private val deviceList: MutableList<Device>,private val listOfDevices: MutableList<Map<String, Any?>>,
                    private val mapOfDeviceParameters: MutableMap<String, Map<String, Any?>>,
                    private val listOfDeviceParametertsNames: Map<String, String>, ):RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val deviceName: TextView = itemView.findViewById(R.id.deviceId)
        val deviceDetails: TextView = itemView.findViewById(R.id.deviceData)
        val constraintLayout: ConstraintLayout = itemView.findViewById(R.id.expandedLayout)

        val aboutDeviceButton = itemView.findViewById<Button>(R.id.deviceDataButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent,false)
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val currentItem = deviceList[position]
        holder.deviceName.text = currentItem.heading
        val deviceDetails = "${currentItem.details}\n${currentItem.lastUpdate}"
        holder.deviceDetails.text = deviceDetails

        val isVisible: Boolean = currentItem.visibility
        holder.constraintLayout.visibility = if(isVisible) View.VISIBLE else View.GONE

        holder.deviceName.setOnClickListener {
            currentItem.visibility = !currentItem.visibility
            notifyItemChanged(position)
        }
        holder.aboutDeviceButton.setOnClickListener {
            showDataWindow(currentItem.index, listOfDevices, mapOfDeviceParameters,listOfDeviceParametertsNames, holder.itemView.context)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}