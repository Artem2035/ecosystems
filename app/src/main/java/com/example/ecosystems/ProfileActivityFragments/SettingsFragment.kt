package com.example.ecosystems.ProfileActivityFragments

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
import com.example.ecosystems.MainActivity
import com.example.ecosystems.R
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SettingsFragment : Fragment() {
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    private lateinit var token:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token").toString()
            personalAccountData = (it.getSerializable ("personalAccountData") as? MutableMap<String, Any?>)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        val sendEmail: SwitchCompat = view.findViewById(R.id.sendEmailSwitch)
        if(personalAccountData.getValue("is_send_emails_not_devices_link") == 1.0)
            sendEmail.isChecked = true

        val saveChangesButton: AppCompatButton = view.findViewById(R.id.editPasswordButton)
        saveChangesButton.setOnClickListener {
            val thread =Thread {
                try {
                    Log.d("Save profile data","profile")
                    personalAccountData["is_send_emails_not_devices_link"] = if (sendEmail.isChecked) 1 else 0
                    saveSettingsChanges()
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

    fun saveSettingsChanges(){
        val client = OkHttpClient()
        val MEDIA_TYPE = "application/json".toMediaType()

        val requestBody = "{\"user\": ${Gson().toJson(personalAccountData)}}"

        Log.d("saveProfileChanes", token)
        Log.d("saveProfileChanes", requestBody)
        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/profile")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        }
    }
}