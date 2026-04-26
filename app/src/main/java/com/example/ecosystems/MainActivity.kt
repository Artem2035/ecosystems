package com.example.ecosystems

import SecureTokenManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.data.local.SecureCredentialsManager
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private lateinit var editTextLogin: EditText
    private lateinit var editTextPassword: EditText
    private var token = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextLogin = findViewById(R.id.editTextLogin)
        editTextPassword = findViewById(R.id.editTextPassword)
        //editTextPassword.setText(BuildConfig.BUTTON_TEXT)
        val logInButton: Button = findViewById(com.example.ecosystems.R.id.logInButton)
        val registrationButton: Button = findViewById(com.example.ecosystems.R.id.registrationButton)

        val tokenManager = SecureTokenManager(this)

        val credentialsManager = SecureCredentialsManager(this)
        if (credentialsManager.hasSaved()) {
            editTextLogin.setText(credentialsManager.loadLogin())
            editTextPassword.setText(credentialsManager.loadPassword())
        }

        if (tokenManager.loadToken() != null) {
            token = tokenManager.loadToken()!!
        }
        val hasToken =  if (!token.isEmpty()) true else false
        if(!isInternetAvailable() && hasToken){
            val message = Toast.makeText(this,"Нет интернета! Включен оффлайн режим!",
                Toast.LENGTH_SHORT)
            message.show()
            val intent =  Intent(this,MapActivity::class.java)
            startActivity(intent)
        }

        val msg = Toast.makeText(this,"has token ${hasToken}",
            Toast.LENGTH_SHORT)
        msg.show()

        registrationButton.setOnClickListener {
            val intent =  Intent(this,RegistrationActivity::class.java)
            startActivity(intent)
        }

        logInButton.setOnClickListener {
            val login = editTextLogin.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if(login.isEmpty() or password.isEmpty())
            {
                val message = Toast.makeText(this,"Поля 'Логин' и 'Пароль' должны быть заполнены!",
                    Toast.LENGTH_SHORT)
                message.show()
                return@setOnClickListener
            }

            if (!isInternetAvailable()) {
                Toast.makeText(this, "Нет интернета!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            logInButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val token = withContext(Dispatchers.IO) { api.GetToken(login, password) }
                    withContext(Dispatchers.IO) {
                        tokenManager.saveToken(token)
                        credentialsManager.save(login, password)
                    }

                    startActivity(Intent(this@MainActivity, MapActivity::class.java))
                }
                catch (e: Exception)
                {
                    Log.d("Error","Unexpected code ${e.message}")
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                finally {
                    // Разблокируем кнопку в любом случае
                    logInButton.isEnabled = true
                }
            }

/*            if(isInternetAvailable()){
                Thread {
                    try {
                        token = api.GetToken(login,password)
                        val intent =  Intent(this,MapActivity::class.java)

                        tokenManager.saveToken(token)
                        credentialsManager.save(login, password)
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
                }.start()
            }
            else{
                val message = Toast.makeText(this,"Нет интернета! Включен оффлайн режим!",
                    Toast.LENGTH_SHORT)
                message.show()
                val intent =  Intent(this,MapActivity::class.java)
                startActivity(intent)
            }*/
        }
    }
}