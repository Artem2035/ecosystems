package com.example.ecosystems.DeviceInfoActivityFragments

import SecureTokenManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.DeviceInfo
import com.example.ecosystems.R
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.android.material.textfield.TextInputLayout

/**
 * A simple [Fragment] subclass.
 * Use the [DeviceInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DeviceInfoFragment : Fragment() {
    private val api: ApiService = ApiService()
    private lateinit var token:String
    private var currentDevice: MutableMap<String, Any?> = mutableMapOf()
    private var newDeviceInfo: MutableMap<String, Any?> = mutableMapOf()
    private val moduleTypeAdaptersMap = mutableMapOf<String, ArrayAdapter<String>>()
    private var saveChanges = false
    private lateinit var arrayAdapter: ArrayAdapter<String>

    // Получаем ViewModel от Activity
    private val viewModel: SharedViewModel by activityViewModels()

    private lateinit var deviceTypeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var moduleTypeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var name:EditText
    private lateinit var description:EditText
    private lateinit var serialNum:EditText
    private lateinit var location:EditText
    private lateinit var latitude:EditText
    private lateinit var longitude:EditText

    private lateinit var timeZone:EditText
    private lateinit var timeNotOnline:EditText

    private lateinit var isPublic:SwitchCompat
    private lateinit var allowDownload:SwitchCompat
    private lateinit var isCertified:SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Прочитать токен
        val tokenManager = SecureTokenManager(requireContext())
        token = tokenManager.loadToken()!!

        arguments?.let {
            currentDevice = HashMap(viewModel.currentDevice)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){

        deviceTypeAutoCompleteTextView = view.findViewById(R.id.deviceTypeAutoCompleteTextView)
        val deviceTypeNames = resources.getStringArray(R.array.device_type_names)
        arrayAdapter = ArrayAdapter(view.context,R.layout.dropdown_item,deviceTypeNames)
        deviceTypeAutoCompleteTextView.setAdapter(arrayAdapter)

        moduleTypeAutoCompleteTextView = view.findViewById(R.id.moduleTypeAutoCompleteTextView)
        val moduleTypeInputLayout: TextInputLayout = view.findViewById(R.id.moduleTypeInputLayout)
        val moduleTextView: TextView = view.findViewById(R.id.textView18)
        moduleTextView.visibility = View.GONE
        moduleTypeInputLayout.visibility = View.GONE

        val moduleTypeMap = mapOf(
            "Сейсмостанция" to R.array.seismic_station_names,
            "Модуль"        to R.array.module_type_names,
            "Беспилотная система" to R.array.unmanned_system_names
        )
        moduleTypeMap.forEach { moduleName, resId ->
            moduleTypeAdaptersMap[moduleName] = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, resources.getStringArray(resId))
        }

        // показ поля - модуль устройства
        fun showModuleDropdown(moduleType: String, selectedText: String? = null, enabled: Boolean = true) {
            moduleTypeAutoCompleteTextView.setAdapter(moduleTypeAdaptersMap.getOrDefault(moduleType, null))
            moduleTypeAutoCompleteTextView.setText(selectedText, false)
            moduleTypeInputLayout.visibility = View.VISIBLE
            moduleTypeInputLayout.isEnabled = enabled
            moduleTextView.visibility = View.VISIBLE
        }
        // скрыть поле - модуль устройства
        fun hideModuleDropdown() {
            moduleTypeInputLayout.visibility = View.GONE
            moduleTextView.visibility = View.GONE
            moduleTypeAutoCompleteTextView.setText("")
        }

        hideModuleDropdown()

        deviceTypeAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position).toString()
            if(moduleTypeAdaptersMap.containsKey(selected)) showModuleDropdown(selected) else hideModuleDropdown()
        }

        val deviceTypeId  = currentDevice.getValue("device_type_id").toString().toDouble().toInt()
        // Индекс в адаптере по device_type_id
        val adapterIndex = when {
            deviceTypeId == 1       -> 0
            deviceTypeId <= 14      -> deviceTypeId - 2
            else                    -> deviceTypeId - 4
        }
        val deviceTypeName = arrayAdapter.getItem(adapterIndex).toString()
        deviceTypeAutoCompleteTextView.setText(deviceTypeName, false)

        var moduleTypeId = currentDevice.getValue("module_type_id")
        when (deviceTypeId) {
            3    -> {
                val resId = R.array.module_type_names
                showModuleDropdown(deviceTypeName, enabled = false)
                if (moduleTypeId != null){
                    moduleTypeId = moduleTypeId.toString().toDouble().toInt()
                    val text = resources.getStringArray(resId).getOrNull(moduleTypeId)
                    moduleTypeAutoCompleteTextView.setText(text, false)
                }
            }
            6    -> {
                val fileFormat = currentDevice.getOrDefault("file_format", "").toString()
                showModuleDropdown(deviceTypeName, selectedText = fileFormat, enabled = false)
            }
            17   -> {
                val resId = R.array.unmanned_system_names
                showModuleDropdown(deviceTypeName, enabled = false)
                if (moduleTypeId != null){
                    moduleTypeId = moduleTypeId.toString().toDouble().toInt()
                    val text = resources.getStringArray(resId).getOrNull(moduleTypeId)
                    moduleTypeAutoCompleteTextView.setText(text, false)
                }
            }
        }

        name = view.findViewById(R.id.editNameText)
        description = view.findViewById(R.id.editDescriptionText)
        serialNum = view.findViewById(R.id.editTextSerialNum)
        location = view.findViewById(R.id.editTextLocation)
        latitude = view.findViewById(R.id.editTextLatitude)
        longitude = view.findViewById(R.id.editTextLongitude)

        timeZone = view.findViewById(R.id.editTextTimeZone)
        timeNotOnline = view.findViewById(R.id.editTextTimeNotOnline)

        isPublic = view.findViewById(R.id.isPublicSwitch)
        allowDownload = view.findViewById(R.id.allowDownloadSwitch)
        isCertified = view.findViewById(R.id.isCertifiedSwitch)

        name.setText(currentDevice.getValue("name").toString())
        description.setText(currentDevice.getValue("description").toString())
        serialNum.setText(currentDevice.getValue("serial_number").toString())
        location.setText(currentDevice.getValue("location_description").toString())
        latitude.setText(currentDevice.getValue("latitude").toString())
        longitude.setText(currentDevice.getValue("longitude").toString())

        timeZone.setText(currentDevice.getValue("tz").toString())
        timeNotOnline.setText(currentDevice.getValue("time_not_online").toString())

        if(currentDevice.getValue("is_public")!= 0.0)
            isPublic.isChecked = true
        if(currentDevice.getValue("is_allow_download") != 0.0)
            allowDownload.isChecked = true
        if(currentDevice.getValue("is_verified")!= 0.0)
            isCertified.isChecked = true

        val saveChangesButton = view.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.addDeviceButton)
        saveChangesButton.setOnClickListener{
            if(!requireContext().isInternetAvailable()){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Недоступно в офлайн режиме!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }

            if(saveChanges) {
                saveChangesButton.setText("Изменить")
                saveChanges= false

                val newDeviceInfo = buildDeviceInfo()
                Log.d("newDeviceInfo123", "${newDeviceInfo}")
                if (newDeviceInfo == null){
                    val message = Toast.makeText(view.context,"Не удалось сохранить данные!",
                        Toast.LENGTH_SHORT)
                    message.show()
                    return@setOnClickListener
                }

                val thread =Thread {
                    try {
                        Log.d("DeviceInfoFragment","${currentDevice}")
                        api.saveDeviceInfoChanges(token, newDeviceInfo)
                        viewModel.updateDeviceInfo(newDeviceInfo,lifecycleScope)

                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(view.context,"Данные изменены!",
                                Toast.LENGTH_SHORT)
                            message.show()
                        }
                    }
                    catch (exception: Exception)
                    {
                        Log.d("Error","Unexpected code ${exception.message}")
                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(view.context,"Unexpected code ${exception.message}",
                                Toast.LENGTH_SHORT)
                            message.show()
                        }
                    }
                }
                thread.start()
                thread.join() // Основной поток ждет завершения фонового потока
            }
            else{
                saveChanges= true
                saveChangesButton.setText("Сохранить")
            }

            name.isEnabled = !name.isEnabled
            description.isEnabled = !description.isEnabled
            serialNum.isEnabled = !serialNum.isEnabled
            location.isEnabled = !location.isEnabled
            latitude.isEnabled = !latitude.isEnabled
            longitude.isEnabled = !longitude.isEnabled
            timeZone.isEnabled = !timeZone.isEnabled
            timeNotOnline.isEnabled = !timeNotOnline.isEnabled

            isPublic.isEnabled = !isPublic.isEnabled
            allowDownload.isEnabled = !allowDownload.isEnabled
            isCertified.isEnabled = !isCertified.isEnabled

            deviceTypeAutoCompleteTextView.isEnabled = !deviceTypeAutoCompleteTextView.isEnabled
            moduleTypeAutoCompleteTextView.isEnabled = !moduleTypeAutoCompleteTextView.isEnabled
        }
    }

    // Построение DeviceInfo с обработкой null
    private fun buildDeviceInfo(): DeviceInfo? {
        return try {
            val deviceTypeId = arrayAdapter.getPosition(deviceTypeAutoCompleteTextView.text.toString())
            val newDeviceTypeId = when {
                deviceTypeId == 0       -> 1
                deviceTypeId <= 12      -> deviceTypeId + 2
                else                    -> deviceTypeId + 4
            }
            var moduleTypeId: Int? = null
            var fileFormat = "undefined"
            when (newDeviceTypeId) {
                3    -> moduleTypeId = resources.getStringArray(R.array.module_type_names).indexOf(moduleTypeAutoCompleteTextView.text.toString())
                6    -> fileFormat = moduleTypeAutoCompleteTextView.text.toString()
                17   -> moduleTypeId = resources.getStringArray(R.array.unmanned_system_names).indexOf(moduleTypeAutoCompleteTextView.text.toString())
            }
            DeviceInfo(
                id = currentDevice["id_device"]?.toString()?.toDoubleOrNull()?.toInt()
                    ?: return null.also { Log.e("DeviceInfo", "id_device is null") },
                name = name.text.toString().trim(),
                description = description.text.toString().trim(),
                serialNumber = serialNum.text.toString().trim(),
                locationDescription = location.text.toString().trim(),
                latitude = latitude.text.toString().toDoubleOrNull() ?: 0.0,
                longitude = longitude.text.toString().toDoubleOrNull() ?: 0.0,
                deviceTypeId = newDeviceTypeId,
                moduleTypeId = moduleTypeId,
                tz = timeZone.text.toString().toDoubleOrNull()?.toInt() ?: 0,
                timeNotOnline = timeNotOnline.text.toString().toDoubleOrNull()?.toInt() ?: 0,
                isPublic = isPublic.isChecked,
                isAllowDownload = allowDownload.isChecked,
                isVerified = isCertified.isChecked,
                fileFormat = fileFormat
            )
        } catch (e: Exception) {
            Log.e("buildDeviceInfo", "Failed to build DeviceInfo", e)
            null
        }
    }
}