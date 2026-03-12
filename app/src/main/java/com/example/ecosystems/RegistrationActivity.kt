package com.example.ecosystems

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable


class RegistrationActivity : AppCompatActivity() {
    private val api: ApiService = ApiService()
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val textPolicy: TextView = findViewById(R.id.textPolicy)
        textPolicy.movementMethod = LinkMovementMethod.getInstance()
        val policySwith: SwitchCompat = findViewById(R.id.policySwitch)

        val newLoginEditText:EditText  = findViewById(R.id.newLoginEditText)
        val newNameEditText:EditText  = findViewById(R.id.newNameEditText)
        val newOrgEditText:EditText  = findViewById(R.id.newOrgEditText)
        val newPassEditText:EditText  = findViewById(R.id.newPassEditText)
        val newPassRepeatEditText:EditText  = findViewById(R.id.newPassRepeatEditText)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent =  Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

        val registerButton: Button = findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            if(!policySwith.isChecked){
                val msg = Toast.makeText(this,"Нужно принять условия пользовательского соглашения!",
                    Toast.LENGTH_SHORT)
                msg.show()
                return@setOnClickListener
            }
            val login = newLoginEditText.text.toString().trim()
            val name = newNameEditText.text.toString().trim()
            val org = newOrgEditText.text.toString().trim()
            val newPass = newPassEditText.text.toString().trim()
            val passRepeat = newPassRepeatEditText.text.toString().trim()
            if (login.isEmpty()) {
                newLoginEditText.error = "Введите логин!"
                newLoginEditText.requestFocus()
                return@setOnClickListener
            }
            if (newPass.isEmpty()) {
                newPassEditText.error = "Введите пароль!"
                newPassEditText.requestFocus()
                return@setOnClickListener
            }
            if (passRepeat.isEmpty()) {
                newPassRepeatEditText.error = "Введите повтор пароля!"
                newPassRepeatEditText.requestFocus()
                return@setOnClickListener
            }
            personalAccountData["id_user"] = -1
            personalAccountData["email"] = login
            personalAccountData["name"] = name
            personalAccountData["second_name"] = ""
            personalAccountData["organization"] = org
            personalAccountData["user_name"] = ""
            personalAccountData["phone"] = ""
            personalAccountData["is_send_emails_not_devices_link"] = 0
            personalAccountData["user_password_hash"] = newPass

            if(isInternetAvailable()){
                Thread {
                    try {
                        val result = api.profileRegistration(personalAccountData)
                        val intent =  Intent(this,MainActivity::class.java)
                        startActivity(intent)
                    }
                    catch (exception: Exception)
                    {
                        Log.d("Error","${exception.message}")
                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(this,"${exception.message}",
                                Toast.LENGTH_SHORT)
                            message.show()
                        }
                    }
                }.start()
            }else{
                val message = Toast.makeText(this,"Нет интернета!",
                    Toast.LENGTH_SHORT)
                message.show()
            }
        }
    }
}