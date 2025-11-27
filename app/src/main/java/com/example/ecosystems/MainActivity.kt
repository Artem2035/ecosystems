package com.example.ecosystems

import SecureTokenManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException



class MainActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var token = ""
        val tokenManager = SecureTokenManager(this)

        if (tokenManager.loadToken() != null) {
            token = tokenManager.loadToken()!!
        }
        val hasToken =  if (!token.isEmpty()) true else false
        val msg = Toast.makeText(this,"has token ${hasToken}",
            Toast.LENGTH_SHORT)
        msg.show()
        val registrationButton: Button = findViewById(com.example.ecosystems.R.id.registrationButton)
        registrationButton.setOnClickListener {
            val intent =  Intent(this,RegistrationActivity::class.java)
            startActivity(intent)
        }

        val logInButton: Button = findViewById(com.example.ecosystems.R.id.logInButton)
        logInButton.setOnClickListener {

            val login = findViewById<EditText?>(R.id.editTextLogin).text.toString()
            val password = findViewById<EditText?>(R.id.editTextPassword).text.toString()

            if(login.isEmpty() or password.isEmpty())
            {
                val message = Toast.makeText(this,"Поля 'Логин' и 'Пароль' должны быть заполнены!",
                    Toast.LENGTH_SHORT)
                message.show()
                return@setOnClickListener
            }
            if(isInternetAvailable()){
                Thread {
                    try {
                        token = api.GetToken(login,password)
                        Log.d("Get devices","devices")
                        val intent =  Intent(this,MapActivity::class.java)

                        tokenManager.saveToken(token)
                        Log.d("Текущий токен", token)
                        startActivity(intent)
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
                    //Do some Network Request
                }.start()
            }else{
                val message = Toast.makeText(this,"Нет интернета! Включен оффлайн режим ${token}",
                    Toast.LENGTH_SHORT)
                message.show()
                val intent =  Intent(this,MapActivity::class.java)
                startActivity(intent)
            }
        }
    }
}