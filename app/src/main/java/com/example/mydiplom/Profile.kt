package com.example.mydiplom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class Profile : AppCompatActivity() {
    private lateinit var profileImageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        profileImageView = findViewById(R.id.profileImageView)
        profileImageView.setOnClickListener {
            openGallery()
        }
        val userId = GlobalData.userId
        val nickname = intent.getStringExtra("nickname")
        val level = intent.getIntExtra("level", 1)

        if (userId == -1 || nickname == null) {
            Toast.makeText(this, "Ошибка: данные пользователя не найдены", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        findViewById<TextView>(R.id.nicknameTextView).text = nickname
        findViewById<TextView>(R.id.levelTextView).text = "Уровень: $level"
        loadProfileImage(userId)
        fetchHabits(userId)

        findViewById<Button>(R.id.centerPlusButton).setOnClickListener {
            val intent = Intent(this, AddHabit::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val croppedBitmap = cropToSquare(bitmap)
                        profileImageView.setImageBitmap(croppedBitmap)
                        uploadImageToServer(GlobalData.userId, croppedBitmap)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Ошибка: неподдерживаемый формат изображения", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
    }

    private fun uploadImageToServer(userId: Int, bitmap: Bitmap) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("userId", userId.toString())
            .addFormDataPart(
                "image", "profile.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()
        val request = Request.Builder()
            .url("http://${Config.IP_ADDRESS}:5000/api/user/uploadProfilePicture")
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Image upload failed", e)
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("ProfileActivity", "Image uploaded successfully")
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Фото загружено!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ProfileActivity", "Server error: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loadProfileImage(userId: Int) {
        val url = "http://${Config.IP_ADDRESS}/api/user/getProfilePicture?userId=$userId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonResponse = JSONObject(responseBody.string())
                        val base64Image = jsonResponse.optString("profilePicture")
                        if (base64Image.isNotEmpty()) {
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            runOnUiThread {
                                profileImageView.setImageBitmap(bitmap)
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@Profile, "Фото профиля отсутствует", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchHabits(userId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habit/user/$userId?userId=$userId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Обработка ошибки сети
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка загрузки привычек", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        try {
                            // Парсим JSON-ответ
                            val jsonResponse = JSONArray(responseBody.string())
                            Log.d("ProfileActivity", "Получены привычки: $jsonResponse")

                            // Отображаем привычки на UI
                            runOnUiThread {
                                displayHabits(jsonResponse)
                            }
                        } catch (e: JSONException) {
                            // Обработка ошибки парсинга JSON
                            Log.e("ProfileActivity", "Ошибка парсинга JSON: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@Profile, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // Обработка ошибки сервера
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}, тело: $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Не удалось загрузить привычки", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun displayHabits(habitsArray: JSONArray) {
        val habitsContainer = findViewById<LinearLayout>(R.id.habitsContainer)
        habitsContainer.removeAllViews()

        if (habitsArray.length() == 0) {
            // Если привычек нет, покажем сообщение
            val noHabitsText = TextView(this).apply {
                text = "Привычек пока нет. Добавьте первую!"
                textSize = 16f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setPadding(0, 32, 0, 32)
            }
            habitsContainer.addView(noHabitsText)
            return
        }

        for (i in 0 until habitsArray.length()) {
            val habit = habitsArray.getJSONObject(i)
            val habitId = habit.optInt("id", -1)
            val habitName = habit.optString("name", "Без названия")
            val habitDescription = habit.optString("description", "Нет описания")

            // Создаем карточку привычки
            val habitCard = layoutInflater.inflate(R.layout.habit_item, habitsContainer, false)

            // Устанавливаем данные
            habitCard.findViewById<TextView>(R.id.habitName).text = habitName
            habitCard.findViewById<TextView>(R.id.habitDescription).text = habitDescription

            // Обработка нажатия на кнопку "Выполнено"
            val completeButton = habitCard.findViewById<Button>(R.id.completeButton)
            completeButton.setOnClickListener {
                if (habitId != -1) {
                    markHabitAsCompleted(habitId)
                } else {
                    Toast.makeText(this, "Ошибка: ID привычки не найден", Toast.LENGTH_SHORT).show()
                }
            }
            val deleteButton = habitCard.findViewById<Button>(R.id.deleteButton)
            deleteButton.setOnClickListener {
                if (habitId != -1) {
                    deleteHabit(habitId)
                } else {
                    Toast.makeText(this, "Ошибка: ID привычки не найден", Toast.LENGTH_SHORT).show()
                }
            }

            // Добавляем анимацию
            val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            habitCard.startAnimation(animation)

            // Добавляем карточку в контейнер
            habitsContainer.addView(habitCard)
        }
    }
    private fun deleteHabit(habitId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habit/delete/$habitId"
        val request = Request.Builder().url(url).delete().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Привычка удалена!", Toast.LENGTH_SHORT).show()
                        // Обновляем список привычек
                        fetchHabits(GlobalData.userId)
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}, тело: $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Ошибка при удалении привычки", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun markHabitAsCompleted(habitId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habit/complete/$habitId"
        val requestBody = JSONObject().apply {
            put("userId", GlobalData.userId)
            put("date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            put("isCompleted", true)
            put("notes", "Привычка выполнена")
        }.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder().url(url).post(requestBody).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Привычка выполнена!", Toast.LENGTH_SHORT).show()
                        // Обновляем данные пользователя (опыт и уровень)
                        fetchUserData(GlobalData.userId)
                        // Обновляем прогресс привычки
                        fetchHabitProgress(habitId)
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}, тело: $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@Profile, "Ошибка при отметке выполнения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun fetchHabitProgress(habitId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/HabitDiary/habit/$habitId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        try {
                            val jsonResponse = JSONArray(responseBody.string())
                            val completedDays = jsonResponse.length() // Количество выполненных дней
                            val totalDays = 30 // Например, за последние 30 дней
                            val progress = (completedDays.toFloat() / totalDays) * 100

                            runOnUiThread {
                                // Обновляем прогресс на UI
                                updateHabitProgress(habitId, progress)
                            }
                        } catch (e: JSONException) {
                            Log.e("ProfileActivity", "Ошибка парсинга JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}")
                }
            }
        })
    }
    private fun updateHabitProgress(habitId: Int, progress: Float) {
        val habitsContainer = findViewById<LinearLayout>(R.id.habitsContainer)
        for (i in 0 until habitsContainer.childCount) {
            val habitCard = habitsContainer.getChildAt(i)
            val habitIdInCard = habitCard.tag as? Int ?: continue

            if (habitIdInCard == habitId) {
                val progressBar = habitCard.findViewById<ProgressBar>(R.id.progressBar)
                progressBar?.progress = progress.toInt()
                break
            }
        }
    }
    private fun fetchUserData(userId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/user/$userId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        try {
                            val jsonResponse = JSONObject(responseBody.string())
                            val level = jsonResponse.optInt("level", 1)
                            val experience = jsonResponse.optInt("experience", 0)

                            runOnUiThread {
                                findViewById<TextView>(R.id.levelTextView).text = "Уровень: $level"
                                // Можно добавить отображение опыта, если нужно
                            }
                        } catch (e: JSONException) {
                            Log.e("ProfileActivity", "Ошибка парсинга JSON: ${e.message}")
                        }
                    }
                } else {
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}")
                }
            }
        })
    }
}
