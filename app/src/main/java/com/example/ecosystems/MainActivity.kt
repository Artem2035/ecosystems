package com.example.ecosystems

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.ecosystems.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: ConstraintLayout = binding.root

        setContentView(view)

        binding.registrationButton.setOnClickListener {
            val intent =  Intent(this,RegistrationActivity::class.java)
            startActivity(intent)
        }

        binding.logInButton.setOnClickListener {

            val login:String = binding.editTextLogin.text.toString()
            val password:String = binding.editTextPassword.text.toString()


            if(login.isEmpty() or password.isEmpty())
            {
                val message = Toast.makeText(this,"Поля 'Логин' и 'Пароль' должны быть заполнены!",
                    Toast.LENGTH_SHORT)
                message.show()
                return@setOnClickListener
            }

            Thread {
                try {
                    val token = GetToken(login,password)
                    Log.d("Get devices","devices")
                    val intent =  Intent(this,MapActivity::class.java)
                    intent.putExtra("token", token)
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
        }
    }

    @WorkerThread
    fun GetToken(login: String, password: String):String
    {
        val client = OkHttpClient()

        var token = ""

        val MEDIA_TYPE = "application/json".toMediaType()
        val requestBody = "{\"login\":\"${login}\",\"password\":\"${password}\"}"

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/token")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                Log.d("Error","Unexpected code $response ${response.message} ${response.code} ${response.body}")
                throw IOException("Unexpected code ${response.message}")
            }
            val requestResult = response.body!!.string()

            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Error while making request: result.get")
                throw Exception("Error while making request: result.get")
            }

            token = result.get("access_token").toString()
            Log.d("Token","token = ${token}")
        }
        return token
    }
}