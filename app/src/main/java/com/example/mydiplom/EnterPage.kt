package com.example.mydiplom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils

class EnterPage : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var scaleDownAnimation: Animation
    private lateinit var scaleUpAnimation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_page)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)

        // Загружаем анимации
        scaleDownAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        scaleUpAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        // Назначаем анимацию для кнопки входа
        loginButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.startAnimation(scaleDownAnimation)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.startAnimation(scaleUpAnimation)
                }
            }
            false
        }

        // Назначаем анимацию для текстового поля "Регистрация"
        registerTextView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.startAnimation(scaleDownAnimation)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.startAnimation(scaleUpAnimation)
                }
            }
            false
        }

        setupRetrofit()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (email.isNotBlank() && password.isNotBlank()) {
                val loginUser = User(email = email, password = password, nickname = "")
                loginUser(loginUser)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        registerTextView.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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
            .baseUrl("http://${Config.IP_ADDRESS}:5000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun loginUser(loginUser: User) {
        apiService.loginUser(loginUser).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        Toast.makeText(this@EnterPage, loginResponse.message, Toast.LENGTH_SHORT).show()
                        GlobalData.userId = loginResponse.userId
                        val intent = Intent(this@EnterPage, Profile::class.java)
                        intent.putExtra("userId", loginResponse.userId)
                        intent.putExtra("nickname", loginResponse.nickname)
                        intent.putExtra("level", loginResponse.level)
                        intent.putExtra("profilePicture", loginResponse.profilePicture)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val errorMessage = "Ошибка авторизации: ${response.code()}"
                    Log.e("HTTP", errorMessage)
                    Toast.makeText(this@EnterPage, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("HTTP", "Ошибка запроса: ${t.message}")
                Toast.makeText(this@EnterPage, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

