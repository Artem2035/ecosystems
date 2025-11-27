package com.example.ecosystems.ProfileActivityFragments

import SecurePersonalAccountManager
import SecureTokenManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.MainActivity
import com.example.ecosystems.R
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SettingsFragment : Fragment() {
    private val api: ApiService = ApiService()
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    private lateinit var token:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val personalAccountManager = SecurePersonalAccountManager(requireContext())
        // Прочитать токен
        val tokenManager = SecureTokenManager(requireContext())
        token = tokenManager.loadToken()!!
        personalAccountData = personalAccountManager.loadData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        val personalAccountManager = SecurePersonalAccountManager(view.context)
        val sendEmail: SwitchCompat = view.findViewById(R.id.sendEmailSwitch)
        if(personalAccountData.getValue("is_send_emails_not_devices_link") == 1)
            sendEmail.isChecked = true

        val saveChangesButton: AppCompatButton = view.findViewById(R.id.editPasswordButton)
        saveChangesButton.setOnClickListener {
            if(!requireContext().isInternetAvailable()){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Недоступно в офлайн режиме!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }

            val thread =Thread {
                try {
                    Log.d("Save profile data","profile")
                    personalAccountData["is_send_emails_not_devices_link"] = if (sendEmail.isChecked) 1 else 0
                    api.saveProfileChanges(token, personalAccountData)
                    personalAccountManager.saveData(personalAccountData)
                    //saveSettingsChanges()
                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(view.context,"Unexpected code ${exception.message}",
                            Toast.LENGTH_SHORT)
                        message.show()
                        val intent =  Intent(view.context, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
            thread.start()
            thread.join() // Основной поток ждет завершения фонового потока
            Handler(Looper.getMainLooper()).post{
                val message = Toast.makeText(view.context,"Данные сохранены!",
                    Toast.LENGTH_SHORT)
                message.show()
            }
        }
    }
}