package com.example.ecosystems.DeviceInfoActivityFragments

import SecurePersonalAccountManager
import SecureTokenManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.MainActivity
import com.example.ecosystems.PersonalAccount
import com.example.ecosystems.R
import com.example.ecosystems.network.ApiService
import kotlinx.coroutines.launch
import java.io.IOException
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Serializable

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


        val name:EditText = view.findViewById(R.id.editNameText)
        val description:EditText = view.findViewById(R.id.editDescriptionText)
        val serialNum:EditText = view.findViewById(R.id.editTextSerialNum)
        val location:EditText = view.findViewById(R.id.editTextLocation)
        val latitude:EditText = view.findViewById(R.id.editTextLatitude)
        val longitude:EditText = view.findViewById(R.id.editTextLongitude)

        val timeZone:EditText = view.findViewById(R.id.editTextTimeZone)
        val timeNotOnline:EditText = view.findViewById(R.id.editTextTimeNotOnline)

        val isPublic:SwitchCompat = view.findViewById(R.id.isPublicSwitch)
        val allowDownload:SwitchCompat = view.findViewById(R.id.allowDownloadSwitch)
        val isCertified:SwitchCompat = view.findViewById(R.id.isCertifiedSwitch)

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

            if(saveChanges) {
                saveChangesButton.setText("Изменить")
                saveChanges= false
                val thread =Thread {
                    try {
                        Log.d("DeviceInfoFragment","${currentDevice}")
                        newDeviceInfo["id"] = currentDevice.getValue("id_device").toString().toDouble().toInt()
                        newDeviceInfo["module_type_id"] = currentDevice.getOrDefault("module_type_id", 1).toString().toDouble().toInt()
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
                        newDeviceInfo["is_verified"] = if (isCertified.isChecked) 1.0 else 0.0
                        api.saveDeviceInfoChanges(token, newDeviceInfo)
                        //saveDeviceInfoChanges()

                        currentDevice = HashMap(newDeviceInfo)
                        currentDevice.forEach { param, value ->
                            viewModel.updateValue(param, value)
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
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(view.context,"Данные изменены!",
                            Toast.LENGTH_SHORT)
                        message.show()
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
}