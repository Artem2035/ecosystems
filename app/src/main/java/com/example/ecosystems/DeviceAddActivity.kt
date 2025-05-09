package com.example.ecosystems

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import java.io.Serializable

class DeviceAddActivity : AppCompatActivity() {

    private lateinit var token:String
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_add)

        val bundle = intent.extras
        token = bundle?.getString("token").toString()
        mapOfDevices = (bundle?.getSerializable ("mapOfDevices") as? MutableMap<Int, Map<String, Any?>>)!!

        val accountSectionsAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.accountSectionsAutoCompleteTextView)
        val accountSectionNames = resources.getStringArray(R.array.device_type_names)
        val arrayAdapter = ArrayAdapter(this,R.layout.dropdown_item,accountSectionNames)
        accountSectionsAutoCompleteTextView.setAdapter(arrayAdapter)
    }

    fun startPersonalAccountActivity(view: View) {
        val intent =  Intent(this,PersonalAccount::class.java)
        val bundle = Bundle()
        bundle.putString("token", token)
        bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
        bundle.putBoolean("showDevicesManagmentFragment", true)
        intent.putExtras(bundle)
        startActivity(intent)
    }
}