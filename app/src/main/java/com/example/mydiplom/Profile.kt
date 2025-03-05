package com.example.mydiplom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class Profile : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private var imageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val ADD_HABIT_REQUEST_CODE = 1001
        private const val IMG_BB_API_KEY = "3c0ba83caff39dffe95431ebaca86d9e"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImageView = findViewById(R.id.profileImageView)
        profileImageView.setOnClickListener { openGallery() }

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
            startActivityForResult(intent, ADD_HABIT_REQUEST_CODE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PICK_IMAGE_REQUEST -> handleImagePickResult(resultCode, data)
            ADD_HABIT_REQUEST_CODE -> handleAddHabitResult(resultCode)
        }
    }

    private fun handleImagePickResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val croppedBitmap = cropToSquare(bitmap)
                        profileImageView.setImageBitmap(croppedBitmap)

                        val jpegFile = File(cacheDir, "temp_image.jpg")
                        FileOutputStream(jpegFile).use { fos ->
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        }

                        uploadImageToImgBB(GlobalData.userId, jpegFile)
                    } else {
                        showToast("Ошибка: неподдерживаемый формат изображения")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Ошибка загрузки фото")
                }
            }
        }
    }

    private fun handleAddHabitResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            fetchHabits(GlobalData.userId)
            showToast("Привычка успешно добавлена!")
        }
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val xOffset = (bitmap.width - size) / 2
        val yOffset = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
    }

    private fun uploadImageToImgBB(userId: Int, imageFile: File) {
        val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.imgBBInstance.uploadImage(imagePart, IMG_BB_API_KEY)
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.data?.url
                    if (imageUrl != null) {
                        updateProfilePicturePath(userId, imageUrl)
                    }
                } else {
                    showToast("Ошибка загрузки изображения")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Ошибка сети")
            }
        }
    }

    private fun updateProfilePicturePath(userId: Int, imageUrl: String) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/user/uploadProfilePicture"
        val jsonBody = """
            {
                "userId": $userId,
                "imagePath": "$imageUrl"
            }
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(requestBody).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка обновления пути изображения", e)
                showToast("Ошибка сети")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("Фото сохранено!")
                } else {
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code}, тело: $errorBody")
                    showToast("Ошибка сервера")
                }
            }
        })
    }

    private fun loadProfileImage(userId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/user/getProfilePicturePath?userId=$userId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("Ошибка загрузки фото")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonResponse = JSONObject(responseBody.string())
                        val imageUrl = jsonResponse.optString("profilePicturePath")
                        Log.d("ProfileActivity", "Получен URL фотографии: $imageUrl")

                        if (imageUrl.isNotEmpty()) {
                            runOnUiThread {
                                Glide.with(this@Profile)
                                    .load(imageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(profileImageView)
                                startFadeInAnimation(profileImageView)
                            }
                        } else {
                            showToast("Фото профиля отсутствует")
                        }
                    }
                } else {
                    showToast("Не удалось загрузить фото")
                }
            }
        })
    }

    private fun fetchHabits(userId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habit/user/$userId?userId=$userId"
        val request = Request.Builder().url(url).get().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
                showToast("Ошибка загрузки привычек")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        try {
                            val jsonResponse = JSONArray(responseBody.string())
                            Log.d("ProfileActivity", "Получены привычки: $jsonResponse")
                            runOnUiThread {
                                displayHabits(jsonResponse)
                            }
                        } catch (e: JSONException) {
                            Log.e("ProfileActivity", "Ошибка парсинга JSON: ${e.message}")
                            showToast("Ошибка обработки данных")
                        }
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}, тело: $errorBody")
                    showToast("Не удалось загрузить привычки")
                }
            }
        })
    }

    private fun displayHabits(habitsArray: JSONArray) {
        val habitsContainer = findViewById<LinearLayout>(R.id.habitsContainer)
        habitsContainer.removeAllViews()

        if (habitsArray.length() == 0) {
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

            val habitCard = layoutInflater.inflate(R.layout.habit_item, habitsContainer, false)

            habitCard.findViewById<TextView>(R.id.habitName).text = habitName
            habitCard.findViewById<TextView>(R.id.habitDescription).text = habitDescription

            val completeButton = habitCard.findViewById<Button>(R.id.completeButton)
            val deleteButton = habitCard.findViewById<Button>(R.id.deleteButton)

            completeButton.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down))
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up))
                    }
                }
                false
            }

            deleteButton.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down))
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up))
                    }
                }
                false
            }
            completeButton.setOnClickListener {
                if (habitId != -1) {
                    markHabitAsCompleted(habitId, habitCard)
                } else {
                    showToast("Ошибка: ID привычки не найден")
                }
            }


            deleteButton.setOnClickListener {
                if (habitId != -1) {
                    deleteHabit(habitId)
                } else {
                    showToast("Ошибка: ID привычки не найден")
                }
            }

            startFadeInAnimation(habitCard)
            habitsContainer.addView(habitCard)
        }
    }

    private fun deleteHabit(habitId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habit/delete/$habitId"
        val request = Request.Builder().url(url).delete().build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Ошибка сети: ${e.message}")
                showToast("Ошибка сети")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("Привычка удалена!")
                    fetchHabits(GlobalData.userId)
                } else {
                    val errorBody = response.body?.string() ?: "Пустое тело ответа"
                    Log.e("ProfileActivity", "Ошибка сервера: ${response.code} ${response.message}, тело: $errorBody")
                    showToast("Ошибка при удалении привычки")
                }
            }
        })
    }

    private fun markHabitAsCompleted(habitId: Int, habitCard: View) {
        // Визуальные изменения
        habitCard.setBackgroundColor(ContextCompat.getColor(this, R.color.progress_green))
        habitCard.findViewById<TextView>(R.id.habitName).paintFlags =
            habitCard.findViewById<TextView>(R.id.habitName).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        habitCard.findViewById<TextView>(R.id.habitDescription).paintFlags =
            habitCard.findViewById<TextView>(R.id.habitDescription).paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        // Показываем уведомление
        showToast("Привычка выполнена!")

        // Если хотите, можно добавить анимацию
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        habitCard.startAnimation(animation)
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
                            val completedDays = jsonResponse.length()
                            val totalDays = 30
                            val progress = (completedDays.toFloat() / totalDays) * 100

                            runOnUiThread {
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFadeInAnimation(view: android.view.View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        view.startAnimation(animation)
    }
}