package com.example.ecosystems

import SecurePersonalAccountManager
import SecureTokenManager
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
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.DataClasses.Device
import com.example.ecosystems.data.local.SecureDevicesParametersManager
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Serializable
import java.io.IOException


class PersonalAccount : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private lateinit var token:String
    //private var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, Map<String, Any?>> = mutableMapOf()
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_account)

        val personalAccountManager = SecurePersonalAccountManager(this)
        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!

        val bundle = intent.extras

        val showDevicesManagmentFragment = bundle?.getBoolean("showDevicesManagmentFragment", false)

        if(isInternetAvailable()){
            val thread =Thread {
                try {
                    Log.d("Get profile data","profile")
                    personalAccountData = api.getPersonalAccountData(token)
                    Log.d("personalAccountData1", personalAccountData.toString())
                    personalAccountManager.saveData(personalAccountData)
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
        }else{
            personalAccountData = personalAccountManager.loadData()
            Log.d("personalAccountData офлайн",personalAccountData.toString())
            Handler(Looper.getMainLooper()).post{
                val message = Toast.makeText(this,"Офлайн режим!",
                    Toast.LENGTH_SHORT)
                message.show()
            }
        }

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
        if(showDevicesManagmentFragment == true) {
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
        startActivity(intent)
    }

    fun changeFragment(fragment: Fragment)
    {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        // Создаем Bundle и упаковываем данные
        if(fragment is DevicesManagmentFragment)
        {
//            val bundle = Bundle()
//            //bundle.putString("token", token)
            //bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
//            fragment.arguments = bundle
        }

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}