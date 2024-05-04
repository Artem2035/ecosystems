package com.example.ecosystems

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.ecosystems.databinding.ActivityMainBinding

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
            val intent =  Intent(this,MapActivity::class.java)
            startActivity(intent)
        }
    }
}