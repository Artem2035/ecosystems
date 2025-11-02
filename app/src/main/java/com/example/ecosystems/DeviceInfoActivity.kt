package com.example.ecosystems

import SecureTokenManager
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.ecosystems.DeviceInfoActivityFragments.SharedViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Locale

class DeviceInfoActivity : AppCompatActivity() {

    // Инициализация ViewModel
    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var token:String
    //private var listOfDevices: MutableList<Map<String, Any?>> = mutableListOf()
    //словарь, где ключ - параметр device_id,а значение сам словарь с параметрами этого устройства (в том числе и id)
    private var mapOfDevices: MutableMap<Int, MutableMap<String, Any?>> = mutableMapOf()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private var deviceId: Int = 0
    private lateinit var currentDevice: MutableMap<String, Any?>


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        // Прочитать токен
        val tokenManager = SecureTokenManager(this)
        token = tokenManager.loadToken()!!

        val bundle = intent.extras
        mapOfDevices = (bundle?.getSerializable ("mapOfDevices") as? MutableMap<Int, MutableMap<String, Any?>>)!!
        deviceId = bundle.getString("deviceId").toString().toInt()
        currentDevice = mapOfDevices.getValue(deviceId)

        sharedViewModel.currentDevice = HashMap(currentDevice)

        val textView = findViewById<TextView>(R.id.info_device_name)
        val deviceName = "${textView.text} ${currentDevice.getValue("name")} s\\n ${currentDevice.getValue("serial_number")}"

        textView.setText(deviceName)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager2 = findViewById(R.id.viewPager2)

        val fragmentAdapter = DeviceInfoFragmentPageAdapter(supportFragmentManager,lifecycle, currentDevice)
        viewPager2.adapter = fragmentAdapter

        tabLayout.addOnTabSelectedListener(object  : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager2.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
    }

    /*Используется в DeviceInfoFragment, NotificationsFragment,NotificationTypeFragment при нажатии на кнопку Назад*/
    fun startPersonalAccountActivity(view: View) {
        mapOfDevices[deviceId] = HashMap(sharedViewModel.currentDevice)

        val intent =  Intent(this,PersonalAccount::class.java)
        val bundle = Bundle()
        bundle.putSerializable("mapOfDevices", mapOfDevices as Serializable)
        bundle.putBoolean("showDevicesManagmentFragment", true)
        intent.putExtras(bundle)
        startActivity(intent)
    }
}