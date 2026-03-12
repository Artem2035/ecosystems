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
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.DeviceInfo
import com.example.ecosystems.R
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable

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
    private var saveChanges = false

    // Получаем ViewModel от Activity
    private val viewModel: SharedViewModel by activityViewModels()

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

        val accountSectionsAutoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.accountSectionsAutoCompleteTextView)
        val accountSectionNames = resources.getStringArray(R.array.device_type_names)
        val arrayAdapter = ArrayAdapter(view.context,R.layout.dropdown_item,accountSectionNames)
        accountSectionsAutoCompleteTextView.setAdapter(arrayAdapter)

        val device_type_id = currentDevice.getValue("device_type_id").toString().toDouble().toInt()
        if(device_type_id < 6)
            accountSectionsAutoCompleteTextView.setText(arrayAdapter.getItem(device_type_id-1), false)
        else
            accountSectionsAutoCompleteTextView.setText(arrayAdapter.getItem(device_type_id-2), false)


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

        val saveChangesButton = view.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.editPasswordButton)
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
                if (newDeviceInfo == null){
                    val message = Toast.makeText(view.context,"Не удалось сохранить данные!",
                        Toast.LENGTH_SHORT)
                    message.show()
                    return@setOnClickListener
                }

/*                newDeviceInfo["id"] = currentDevice.getValue("id_device").toString().toDouble().toInt()
                newDeviceInfo["module_type_id"] = currentDevice.getOrDefault("module_type_id", 1)?.toString()?.toDoubleOrNull()?.toInt()
                newDeviceInfo["device_type_id"] = currentDevice.getValue("device_type_id").toString().toDouble().toInt()

                newDeviceInfo["name"] = name.text.toString()
                newDeviceInfo["description"] = description.text.toString()
                newDeviceInfo["serial_number"] = serialNum.text.toString()
                newDeviceInfo["location_description"] = location.text.toString()
                newDeviceInfo["latitude"] = latitude.text.toString().toDouble()
                newDeviceInfo["longitude"] = longitude.text.toString().toDouble()
                newDeviceInfo["tz"] = timeZone.text.toString().toDouble()
                newDeviceInfo["time_not_online"] = timeNotOnline.text.toString().toDouble()
                newDeviceInfo["is_public"] = if (isPublic.isChecked) 1.0 else 0.0
                newDeviceInfo["is_allow_download"] = if (allowDownload.isChecked) 1.0 else 0.0
                newDeviceInfo["is_verified"] = if (isCertified.isChecked) 1.0 else 0.0*/

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

            accountSectionsAutoCompleteTextView.isEnabled = !accountSectionsAutoCompleteTextView.isEnabled
        }
    }

    // Построение DeviceInfo с обработкой null
    private fun buildDeviceInfo(): DeviceInfo? {
        return try {
            DeviceInfo(
                id             = currentDevice["id_device"]?.toString()?.toDoubleOrNull()?.toInt()
                    ?: return null.also { Log.e("DeviceInfo", "id_device is null") },
                name           = name.text.toString().trim(),
                description    = description.text.toString().trim(),
                serialNumber   = serialNum.text.toString().trim(),
                locationDescription = location.text.toString().trim(),
                latitude       = latitude.text.toString().toDoubleOrNull() ?: 0.0,
                longitude      = longitude.text.toString().toDoubleOrNull() ?: 0.0,
                deviceTypeId   = currentDevice["device_type_id"]?.toString()?.toDoubleOrNull()?.toInt()
                    ?: return null.also { Log.e("DeviceInfo", "device_type_id is null") },
                moduleTypeId   = currentDevice["module_type_id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 1,
                tz             = timeZone.text.toString().toDoubleOrNull()?.toInt() ?: 0,
                timeNotOnline  = timeNotOnline.text.toString().toDoubleOrNull()?.toInt() ?: 0,
                isPublic       = isPublic.isChecked,
                isAllowDownload = allowDownload.isChecked,
                isVerified     = isCertified.isChecked
            )
        } catch (e: Exception) {
            Log.e("buildDeviceInfo", "Failed to build DeviceInfo", e)
            null
        }
    }
}