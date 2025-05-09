package com.example.ecosystems.ProfileActivityFragments

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
import com.example.ecosystems.MainActivity
import com.example.ecosystems.R
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson


/**
 * A simple [Fragment] subclass.
 * Use the [ProfileInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileInfoFragment : Fragment() {
    private lateinit var token:String
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    private var saveChanges = false

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
        return inflater.inflate(R.layout.fragment_profile_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){

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
                        saveProfileChanges()
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

    fun saveProfileChanges(){
        val client = OkHttpClient()
        val MEDIA_TYPE = "application/json".toMediaType()

        //val requestBody = "{\"user\":{\"account_id\":2,\"created_at\":\"Mon, 11 Jul 2022 20:36:23 GMT\",\"email\":\"demo@example.com\",\"id\":5,\"id_user\":2,\"is_send_emails_not_devices_link\":0,\"mpanel_layouts\":\"{\\\"1\\\":[{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_1\\\",\\\"moved\\\":false,\\\"static\\\":false}],\\\"3\\\":[{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":6,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_8\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":1,\\\"i\\\":\\\"eco_widget_20\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":2,\\\"x\\\":3,\\\"y\\\":1,\\\"i\\\":\\\"eco_widget_21\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":2,\\\"i\\\":\\\"eco_widget_22\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":6,\\\"y\\\":2,\\\"i\\\":\\\"eco_widget_9\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":3,\\\"i\\\":\\\"eco_widget_10\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":6,\\\"y\\\":1,\\\"i\\\":\\\"eco_widget_28\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":3,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_29\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_7\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":3,\\\"h\\\":1,\\\"x\\\":3,\\\"y\\\":3,\\\"i\\\":\\\"eco_widget_30\\\",\\\"moved\\\":false,\\\"static\\\":false}],\\\"4\\\":[{\\\"w\\\":2,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_19\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":2,\\\"h\\\":1,\\\"x\\\":2,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_18\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":2,\\\"h\\\":1,\\\"x\\\":4,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_13\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":2,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":1,\\\"i\\\":\\\"eco_widget_15\\\",\\\"moved\\\":false,\\\"static\\\":false},{\\\"w\\\":2,\\\"h\\\":1,\\\"x\\\":2,\\\"y\\\":1,\\\"i\\\":\\\"eco_widget_14\\\",\\\"moved\\\":false,\\\"static\\\":false}],\\\"5\\\":[{\\\"w\\\":4,\\\"h\\\":1,\\\"x\\\":0,\\\"y\\\":0,\\\"i\\\":\\\"eco_widget_16\\\",\\\"moved\\\":false,\\\"static\\\":false}]}\",\"name\":\"Admin\",\"organization\":\"ПетрГУ\",\"phone\":\"+7 (921) 111-11-11\",\"second_name\":\"Admin\",\"service\":0,\"updated_at\":\"Sun, 23 Mar 2025 20:45:56 GMT\",\"user_name\":\"admin\",\"user_password_hash\":\"\$2b\$12\$BnEH3XxjaffK0yQiKoL2d.7zsYNmISbLl/HgD9p2hSLvSfb8Yf3T.    \"}}"
        //val jsonPersonalAccountData = Gson().toJson(personalAccountData)
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
            Log.d("saveProfileChanges", response.body!!.string())
        }
    }
}