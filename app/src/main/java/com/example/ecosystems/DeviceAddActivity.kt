package com.example.ecosystems

import SecureTokenManager
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import com.example.ecosystems.DataClasses.DeviceInfo
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.android.material.textfield.TextInputLayout

class DeviceAddActivity : AppCompatActivity() {

    private val api: ApiService = ApiService()
    private lateinit var token:String
    private var newDeviceInfo: MutableMap<String, Any?> = mutableMapOf()
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private lateinit var deviceTypeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var moduleTypeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var moduleTypeInputLayout: TextInputLayout
    private lateinit var name: EditText
    private lateinit var description: EditText
    private lateinit var serialNum: EditText
    private lateinit var location: EditText
    private lateinit var latitude: EditText
    private lateinit var longitude: EditText

    private lateinit var timeZone: EditText
    private lateinit var timeNotOnline: EditText

    private lateinit var isPublic: SwitchCompat
    private lateinit var allowDownload: SwitchCompat
    private lateinit var isCertified: SwitchCompat

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_add)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!

        deviceTypeAutoCompleteTextView = findViewById(R.id.deviceTypeAutoCompleteTextView)
        moduleTypeAutoCompleteTextView = findViewById(R.id.moduleTypeAutoCompleteTextView)
        moduleTypeInputLayout = findViewById(R.id.moduleTypeInputLayout)
        val moduleTextView: TextView = findViewById(R.id.textView18)
        moduleTextView.visibility = View.GONE
        moduleTypeInputLayout.visibility = View.GONE

        val accountSectionNames = resources.getStringArray(R.array.device_type_names)
        arrayAdapter = ArrayAdapter(this,R.layout.dropdown_item,accountSectionNames)
        deviceTypeAutoCompleteTextView.setAdapter(arrayAdapter)

        val moduleTypeMap = mapOf(
            "Сейсмостанция" to resources.getStringArray(R.array.seismic_station_names),
            "Модуль" to resources.getStringArray(R.array.module_type_names),
            "Беспилотная система" to resources.getStringArray(R.array.unmanned_system_names)
        )

        deviceTypeAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selected = parent.getItemAtPosition(position).toString()

            if (moduleTypeMap.containsKey(selected)) {
                val secondList = moduleTypeMap[selected]!!

                val secondAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    secondList
                )

                moduleTypeAutoCompleteTextView.setAdapter(secondAdapter)

                // показать второй dropdown
                moduleTypeInputLayout.visibility = View.VISIBLE
                moduleTextView.visibility = View.VISIBLE

            } else {
                // скрыть если не нужно
                moduleTypeInputLayout.visibility = View.GONE
                moduleTextView.visibility = View.GONE
                moduleTypeAutoCompleteTextView.setText("")
            }
        }

        val addDeviceButton = findViewById<AppCompatButton>(R.id.addDeviceButton)
        addDeviceButton.setOnClickListener {
            if(!isInternetAvailable()){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(this,"Недоступно в офлайн режиме!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }

            val newDeviceInfo = buildDeviceInfo()
            Log.d("newDeviceInfo123", "${newDeviceInfo}")
            if (newDeviceInfo == null){
                val message = Toast.makeText(this,"Не удалось добавить устройство!",
                    Toast.LENGTH_SHORT)
                message.show()
                return@setOnClickListener
            }

            val thread =Thread {
                try {
                    api.saveDeviceInfoChanges(token, newDeviceInfo)
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(this,"Устройство добавлено!",
                            Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(this,"Unexpected code ${exception.message}",
                            Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
            }
            thread.start()
            thread.join() // Основной поток ждет завершения фонового потока
        }

        name = findViewById(R.id.editNameText)
        description = findViewById(R.id.editDescriptionText)
        serialNum = findViewById(R.id.editTextSerialNum)
        location = findViewById(R.id.editTextLocation)
        latitude = findViewById(R.id.editTextLatitude)
        longitude = findViewById(R.id.editTextLongitude)

        timeZone = findViewById(R.id.editTextTimeZone)
        timeNotOnline = findViewById(R.id.editTextTimeNotOnline)

        isPublic = findViewById(R.id.isPublicSwitch)
        allowDownload = findViewById(R.id.allowDownloadSwitch)
        isCertified = findViewById(R.id.isCertifiedSwitch)
    }

    fun startPersonalAccountActivity(view: View) {
        val intent =  Intent(this,PersonalAccount::class.java)
        val bundle = Bundle()
        bundle.putBoolean("showDevicesManagmentFragment", true)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    // Построение DeviceInfo с обработкой null
    private fun buildDeviceInfo(): DeviceInfo? {
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
        return try {
            DeviceInfo(
                id = -1,
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