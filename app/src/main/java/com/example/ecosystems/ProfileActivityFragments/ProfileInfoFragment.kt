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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ecosystems.MainActivity
import com.example.ecosystems.R
import com.example.ecosystems.network.ApiService
import com.example.ecosystems.utils.isInternetAvailable
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileInfoFragment : Fragment() {
    private val api: ApiService = ApiService()
    private lateinit var token:String
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    private var saveChanges = false

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
        return inflater.inflate(R.layout.fragment_profile_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        val personalAccountManager = SecurePersonalAccountManager(view.context)

        val name = view.findViewById<EditText>(R.id.editNameText)
        val surname = view.findViewById<EditText>(R.id.editSurnameText)
        val email = view.findViewById<EditText>(R.id.editTextEmailAddress)
        val phone = view.findViewById<EditText>(R.id.editTextPhone)
        val organization = view.findViewById<EditText>(R.id.editOrganisationText)

        name.setText(personalAccountData.getValue("name").toString())
        surname.setText(personalAccountData.getValue("second_name").toString())
        email.setText(personalAccountData.getValue("email").toString())
        phone.setText(personalAccountData.getValue("phone").toString())
        organization.setText(personalAccountData.getValue("organization").toString())

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

            if(saveChanges) {
                saveChangesButton.setText("Изменить")
                saveChanges= false
                val thread =Thread {
                    try {
                        Log.d("Save profile data","profile")
                        personalAccountData["name"] = name.text.toString()
                        personalAccountData["second_name"] = surname.text.toString()
                        personalAccountData["email"] = email.text.toString()
                        personalAccountData["phone"] = phone.text.toString()
                        personalAccountData["organization"] = organization.text.toString()
                        api.saveProfileChanges(token, personalAccountData)
                        personalAccountManager.saveData(personalAccountData)
                        //saveProfileChanges()
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
                name.setText(personalAccountData.getValue("name").toString())
                surname.setText(personalAccountData.getValue("second_name").toString())
                email.setText(personalAccountData.getValue("email").toString())
                phone.setText(personalAccountData.getValue("phone").toString())
                organization.setText(personalAccountData.getValue("organization").toString())
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Данные изменены!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
            }
            else{
                saveChanges= true
                saveChangesButton.setText("Сохранить")
            }
            name.isEnabled = !name.isEnabled
            surname.isEnabled = !surname.isEnabled
            email.isEnabled = !email.isEnabled
            phone.isEnabled = !phone.isEnabled
            organization.isEnabled = !organization.isEnabled
        }
    }
}