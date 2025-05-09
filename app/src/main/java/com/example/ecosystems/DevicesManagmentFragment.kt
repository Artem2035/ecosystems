package com.example.ecosystems

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.DeviceManagementItem
import java.io.Serializable
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DevicesManagmentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DevicesManagmentFragment : Fragment() {
    //  recycle view
    //private var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    private lateinit var token:String

    private lateinit var devicesManagmentRecyclerView: RecyclerView
    private var devicesList: MutableList<DeviceManagementItem> = mutableListOf()
    private var tempDevicesList: MutableList<DeviceManagementItem> = mutableListOf()
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mapOfDevices = (it.getSerializable ("mapOfDevices") as? MutableMap<Int, Map<String, Any?>>)!!
            token = it.getString("token") as String
            mapOfDevices.forEach { deviceId, map ->
                val name = map.get("name").toString()
                val serialNumber = map.get("serial_number").toString()
                val isPublic = if (map.get("is_public") == 0.0) false else true
                val description = map.get("description").toString()
                val location = map.get("location_description").toString()
                val deviceItem = DeviceManagementItem(deviceId, name,serialNumber,description,location,isPublic)
                devicesList.add(deviceItem)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_devices_managment, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем элемент по ID
        devicesManagmentRecyclerView = view.findViewById(R.id.devices_managment_recycler_view)
        devicesManagmentRecyclerView.layoutManager = LinearLayoutManager(view.context)

        tempDevicesList.addAll(devicesList)
        val deviceAdapter = DeviceManagementItemAdapter(tempDevicesList, mapOfDevices, token)
        devicesManagmentRecyclerView.adapter = deviceAdapter

        //поиск
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                tempDevicesList.clear()
                val searchText = newText!!.lowercase(Locale.getDefault())
                if(!searchText.isEmpty()){
                    devicesList.forEach {
                        if(it.name.lowercase(Locale.getDefault()).contains(searchText)){
                            tempDevicesList.add(it)
                        }
                    }
                    deviceAdapter.notifyDataSetChanged()
                }else{
                    tempDevicesList.clear()
                    tempDevicesList.addAll(devicesList)
                    deviceAdapter.notifyDataSetChanged()
                }
                return false
            }
        })

        val addDeviceButton = view.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.editPasswordButton)
        addDeviceButton.setOnClickListener {
            val intent =  Intent(it.context,DeviceAddActivity::class.java)
            //intent.putExtra("token", token)
            val bundle = Bundle()
            //bundle.putSerializable("listOfDevices", listOfDevices as Serializable)
            bundle.putString("token", token)
            bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
            intent.putExtras(bundle)
            startActivity(intent)
        }
    }
}