package com.example.ecosystems

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val textPolicy: TextView = findViewById(com.example.ecosystems.R.id.textPolicy)
        textPolicy.movementMethod = LinkMovementMethod.getInstance()

        val backButton: Button = findViewById(com.example.ecosystems.R.id.backButton)
        backButton.setOnClickListener {
            val intent =  Intent(this,MainActivity::class.java)
            startActivity(intent)
        }
    }
}