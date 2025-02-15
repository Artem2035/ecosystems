package com.example.ecosystems

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

class ProfileActivity : AppCompatActivity() {

    private lateinit var token:String
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentAdapter: FragmentPageAdapter
    private lateinit var saveChangesButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        token = intent.getStringExtra("token").toString()
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
                "Управление устройствами" -> changeFragment(DevicesManagmentFragment())
            }
        }

        changeFragment(ProfileActivityLayoutFragment())
    }

    fun startMapActivity(view: View)
    {
        val intent =  Intent(this,MapActivity::class.java)
        intent.putExtra("token", token)
        startActivity(intent)
    }

    fun changeFragment(fragment: Fragment)
    {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}