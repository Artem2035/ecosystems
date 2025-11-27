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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
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
    private val api: ApiService = ApiService()
    private var personalAccountData: MutableMap<String, Any?> = mutableMapOf()
    private lateinit var token:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val personalAccountManager = SecurePersonalAccountManager(requireContext())
        // Прочитать токен
        val tokenManager = SecureTokenManager(requireContext())
        token = tokenManager.loadToken()!!
        Log.d("personalAccountManager","${personalAccountManager.loadData()}")
        personalAccountData = personalAccountManager.loadData()
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

            if(!requireContext().isInternetAvailable()){
                Handler(Looper.getMainLooper()).post{
                    val message = Toast.makeText(view.context,"Недоступно в офлайн режиме!",
                        Toast.LENGTH_SHORT)
                    message.show()
                }
                return@setOnClickListener
            }

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
                    api.savePasswordChanges(editCurrentPass.text.toString(), editNewPass.text.toString(),
                    token, personalAccountData.getValue("id_user").toString())
                    //savePasswordChanges(editCurrentPass.text.toString(), editNewPass.text.toString())
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
}