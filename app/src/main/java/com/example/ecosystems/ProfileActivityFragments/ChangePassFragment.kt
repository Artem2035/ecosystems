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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 * Use the [ChangePassFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChangePassFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_change_pass, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        val editCurrentPass = view.findViewById<EditText>(R.id.editCurrentPass)
        val editNewPass = view.findViewById<EditText>(R.id.editNewPass)
        val editNewPassRepeat = view.findViewById<EditText>(R.id.editNewPassRepeat)

        val changePasswordButton: AppCompatButton = view.findViewById(R.id.editPasswordButton)
        changePasswordButton.setOnClickListener {
            if(editCurrentPass.text.isNullOrEmpty() || editNewPass.text.isNullOrEmpty() ||
                editNewPassRepeat.text.isNullOrEmpty() ){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Нужно заполнить все поля!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }

            val thread =Thread {
                try {
                    if(editNewPass.text.toString() != editNewPassRepeat.text.toString()){
                        Handler(Looper.getMainLooper()).post{
                            val message = Toast.makeText(view.context,"Пароли не совпадают!",
                                Toast.LENGTH_SHORT)
                            message.show()
                        }
                        return@Thread
                    }
                    savePasswordChanges(editCurrentPass.text.toString(), editNewPass.text.toString())
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(view.context,"Пароль изменён!",
                            Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
                catch (exception: Exception)
                {
                    Log.d("Error","Unexpected code ${exception.message}")
                    Handler(Looper.getMainLooper()).post{
                        val message = Toast.makeText(view.context,"Unexpected code ${exception.message}",
                            Toast.LENGTH_SHORT)
                        message.show()
                    }
                }
            }
            thread.start()
            thread.join() // Основной поток ждет завершения фонового потока
        }
    }

    fun savePasswordChanges(oldPassword: String, newPassword: String){
        val client = OkHttpClient()
        val MEDIA_TYPE = "application/json".toMediaType()


        val requestBody = "{\"schema\":{\"old_password\":\"${oldPassword}\",\"new_password\":\"${newPassword}\",\"id_user\":${personalAccountData.getValue("id_user")}}}"

        val request = Request.Builder()
            .url("https://smartecosystems.petrsu.ru/api/v1/profile/change_password")
            .post(requestBody.toRequestBody(MEDIA_TYPE))
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Authorization", "Bearer ${token}")
            .header("Connection", "keep-alive")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val requestResult = response.body!!.string()

            val gson = Gson()
            val mapAdapter = gson.getAdapter(object: TypeToken<Map<String, Any?>>() {})
            val result: Map<String, Any?> = mapAdapter.fromJson(requestResult)

            if(result.get("result") != "ok")
            {
                Log.d("Error","Error while making request: result.get")
                throw Exception("Введенный текущий пароль не совпадает с паролем аккаунта!")
            }
        }
    }
}