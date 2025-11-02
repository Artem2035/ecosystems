package com.example.ecosystems

import SecureTokenManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.Serializable

class DeviceAddActivity : AppCompatActivity() {

    private lateinit var token:String
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_add)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!

        val bundle = intent.extras
        mapOfDevices = (bundle?.getSerializable ("mapOfDevices") as? MutableMap<Int, Map<String, Any?>>)!!

        val accountSectionsAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.accountSectionsAutoCompleteTextView)
        val accountSectionNames = resources.getStringArray(R.array.device_type_names)
        val arrayAdapter = ArrayAdapter(this,R.layout.dropdown_item,accountSectionNames)
        accountSectionsAutoCompleteTextView.setAdapter(arrayAdapter)
    }

    fun startPersonalAccountActivity(view: View) {
        val intent =  Intent(this,PersonalAccount::class.java)
        val bundle = Bundle()
        bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
        bundle.putBoolean("showDevicesManagmentFragment", true)
        intent.putExtras(bundle)
        startActivity(intent)
    }
}