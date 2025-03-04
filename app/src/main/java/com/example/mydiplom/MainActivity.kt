package com.example.mydiplom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nicknameText = findViewById<EditText>(R.id.nicktext)
        val emailText = findViewById<EditText>(R.id.emailtext)
        val passwordText = findViewById<EditText>(R.id.passwordtext)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginButton = findViewById<Button>(R.id.loginPageButton)

        setupRetrofit()

        registerButton.setOnClickListener {
            val nickname = nicknameText.text.toString().trim()
            val email = emailText.text.toString().trim()
            val password = passwordText.text.toString().trim()

            if (nickname.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                val user = User(nickname, email, password)
                registerUser(user)
            } else {
                Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show()
            }
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, EnterPage::class.java)
            startActivity(intent)
        }
    }

    private fun setupRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.21.43.221:5000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun registerUser(user: User) {
        apiService.registerUser(user).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    Log.d("DEBUG", "Ответ от сервера: $loginResponse") // Логируем ответ

                    if (loginResponse != null) {
                        Toast.makeText(this@MainActivity, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()

                        GlobalData.userId = loginResponse.userId
                        val intent = Intent(this@MainActivity, Profile::class.java)
                        intent.putExtra("userId", loginResponse.userId)
                        intent.putExtra("nickname", loginResponse.nickname)
                        intent.putExtra("level", loginResponse.level)
                        intent.putExtra("profilePicture", loginResponse.profilePicture)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("DEBUG", "loginResponse оказался null")
                    }
                } else {
                    Log.e("HTTP", "Ошибка регистрации: ${response.code()}")
                }


        }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("HTTP", "Ошибка запроса: ${t.message}")
                Toast.makeText(this@MainActivity, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
