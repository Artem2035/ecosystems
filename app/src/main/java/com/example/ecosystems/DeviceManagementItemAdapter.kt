package com.example.ecosystems

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.DeviceManagementItem

import java.io.Serializable

class DeviceManagementItemAdapter(private val deviceList: MutableList<DeviceManagementItem>,
                                  private var mapOfDevices: MutableMap<Int, Map<String, Any?>>,
                                  private  val token: String):RecyclerView.Adapter<DeviceManagementItemAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceDescription: TextView = itemView.findViewById(R.id.deviceDescription)
        val deviceLocation: TextView = itemView.findViewById(R.id.deviceLocation)
        val isPublicTextView: TextView = itemView.findViewById(R.id.isPublic)
        val deviceLayout: ConstraintLayout = itemView.findViewById(R.id.device_list_item_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.devices_managment_list_item, parent,false)
        return DeviceViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val currentItem = deviceList[position]

        val serialNum = if(currentItem.serialNum.isEmpty()) "Нет" else currentItem.serialNum
        val name = "${currentItem.name} s/n: ${serialNum}"
        holder.deviceName.text = name
        holder.deviceDescription.text = if(currentItem.description.isEmpty()) "Нет описания" else currentItem.description
        holder.deviceLocation.text = if(currentItem.location.isEmpty()) "Нет данных" else currentItem.location


        val isPublic: Boolean = currentItem.isPublic
        holder.isPublicTextView.text = if(isPublic) "Публично"  else "Не публично"
        if(isPublic){
            holder.isPublicTextView.background = ContextCompat.getDrawable(holder.isPublicTextView.context, R.drawable.textview_back_public)
        }else{
            holder.isPublicTextView.background = ContextCompat.getDrawable(holder.isPublicTextView.context, R.drawable.textview_back_not_public)
        }

        holder.deviceLayout.setOnClickListener{
            val intent =  Intent(it.context, DeviceInfoActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
            bundle.putString("token", token)
            bundle.putString("deviceId", currentItem.deviceId.toString())
            intent.putExtras(bundle)
            it.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}