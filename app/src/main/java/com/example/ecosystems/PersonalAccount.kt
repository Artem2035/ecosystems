package com.example.ecosystems

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ecosystems.DataClasses.Device
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Serializable
import java.io.IOException


class PersonalAccount : AppCompatActivity() {

    private lateinit var token:String
    //private var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_account)

        val bundle = intent.extras
        token = bundle?.getString("token").toString()
        mapOfDevices = (bundle?.getSerializable("mapOfDevices") as? MutableMap<Int, Map<String, Any?>>)!!

        val showDevicesManagmentFragment = bundle.getBoolean("showDevicesManagmentFragment", false)

        val thread =Thread {
            try {
                Log.d("Get profile data","profile")
                getPersonalAccountData(token)
            }
            catch (exception: Exception)
            {
                Log.d("Error","Unexpected code ${exception.message}")
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(this,"Unexpected code ${exception.message}",Toast.LENGTH_SHORT)
                    message.show()
                    val intent =  Intent(this,MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        thread.start()
        thread.join() // Основной поток ждет завершения фонового потока

        val accountSectionsAutoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.accountSectionsAutoCompleteTextView)
        val accountSectionNames = resources.getStringArray(R.array.account_section_names)
        val arrayAdapter = ArrayAdapter(this,R.layout.dropdown_item,accountSectionNames)
        accountSectionsAutoCompleteTextView.setAdapter(arrayAdapter)

        // Обработчик щелчка
        accountSectionsAutoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener { parent, _,
                                                                                                    position, id ->
            val selectedItem = parent.getItemAtPosition(position).toString()
            // Выводим выбранное слово
            Toast.makeText(applicationContext, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()

            when(selectedItem){
                "Аккаунт" -> changeFragment(ProfileActivityLayoutFragment())
                "Управление устройствами" -> {
                    changeFragment(DevicesManagmentFragment())
                }
            }
        }
        if(showDevicesManagmentFragment) {
            accountSectionsAutoCompleteTextView.setText(arrayAdapter.getItem(1), false)
            changeFragment(DevicesManagmentFragment())
        }
        else
            changeFragment(ProfileActivityLayoutFragment())
    }

    /*Используется в ChangePassFragment, ProfileInfoFragment,SettingsFragment при нажатии на кнопку Назад*/
    fun startMapActivity(view: View)
    {
        val intent =  Intent(this,MapActivity::class.java)
        intent.putExtra("token", token)
        startActivity(intent)
    }

    fun changeFragment(fragment: Fragment)
    {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        // Создаем Bundle и упаковываем данные
        if(fragment is DevicesManagmentFragment)
        {
            val bundle = Bundle()
            bundle.putString("token", token)
            bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
            fragment.arguments = bundle
        }
        if(fragment is ProfileActivityLayoutFragment)
        {
            val bundle = Bundle()
            bundle.putString("token", token)
            bundle.putSerializable("personalAccountData", personalAccountData as Serializable)
            fragment.arguments = bundle
        }
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    fun getPersonalAccountData(token: String)
    {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/profile")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer  ${token}")
            .header("Connection", "keep-alive")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                Log.d("Error","Unexpected code $response")
                throw IOException("Unexpected code $response")
            }

            val requestResult = response.body!!.string()

            Log.d("requestResult", requestResult)
            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Unexpected code $response")
                throw Exception("Error while making request: result.get")
            }
            personalAccountData = result.get("user") as MutableMap<String, Any?>
            Log.d("personalAccountData1", personalAccountData.toString())
        }
    }
}