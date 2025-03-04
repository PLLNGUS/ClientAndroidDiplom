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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
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
            .url("http://10.21.43.221:5000/api/user/uploadProfilePicture")
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
        val url = "http://10.21.43.221:5000/api/user/getProfilePicture?userId=$userId"
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
        val url = "http://192.168.61.221:5000/api/habits?userId=$userId"
        val request = Request.Builder().url(url).get().build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Profile, "Ошибка загрузки привычек", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonResponse = JSONArray(responseBody.string())
                        runOnUiThread {
                            displayHabits(jsonResponse)
                        }
                    }
                } else {
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
        for (i in 0 until habitsArray.length()) {
            val habit = habitsArray.getJSONObject(i)
            val habitName = habit.getString("name")
            val habitDescription = habit.getString("description")
            val habitCard = layoutInflater.inflate(R.layout.habit_item, habitsContainer, false)
            habitCard.findViewById<TextView>(R.id.habitName).text = habitName
            habitCard.findViewById<TextView>(R.id.habitDescription).text = habitDescription
            habitsContainer.addView(habitCard)
        }
    }
}
