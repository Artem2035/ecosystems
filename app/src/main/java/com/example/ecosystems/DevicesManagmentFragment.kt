package com.example.ecosystems

import SecurePersonalAccountManager
import SecureTokenManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecosystems.DataClasses.DeviceManagementItem
import com.example.ecosystems.data.local.SecureDevicesParametersManager
import com.example.ecosystems.utils.isInternetAvailable
import java.util.Locale


/**
 * A simple [Fragment] subclass.
 * Use the [DevicesManagmentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DevicesManagmentFragment : Fragment() {
    private lateinit var token:String

    private lateinit var devicesManagmentRecyclerView: RecyclerView
    private var devicesList: MutableList<DeviceManagementItem> = mutableListOf()
    private var tempDevicesList: MutableList<DeviceManagementItem> = mutableListOf()
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Прочитать токен
        val tokenManager = SecureTokenManager(requireContext())
        token = tokenManager.loadToken()!!

        val devicesManager = SecureDevicesParametersManager(requireContext())
        val devicesManagerData = devicesManager.loadData()
        mapOfDevices = devicesManagerData.get("mapOfDevices") as MutableMap<Int, Map<String, Any?>>
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
            if(!requireContext().isInternetAvailable()){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Недоступно в офлайн режиме!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }
            val intent =  Intent(it.context,DeviceAddActivity::class.java)
            startActivity(intent)
        }
    }
}