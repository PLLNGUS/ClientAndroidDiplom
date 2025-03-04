package com.example.mydiplom

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils


import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddHabit : AppCompatActivity() {
    private lateinit var scaleDownAnimation: Animation
    private lateinit var scaleUpAnimation: Animation
    private lateinit var habitNameEditText: EditText
    private lateinit var habitDescriptionEditText: EditText
    private lateinit var difficulty1: ImageView
    private lateinit var difficulty2: ImageView
    private lateinit var difficulty3: ImageView
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private lateinit var repeatRadioGroup: RadioGroup
    private lateinit var daysOfWeekLayout: LinearLayout
    private lateinit var mondayCheckBox: CheckBox
    private lateinit var tuesdayCheckBox: CheckBox
    private lateinit var wednesdayCheckBox: CheckBox
    private lateinit var thursdayCheckBox: CheckBox
    private lateinit var fridayCheckBox: CheckBox
    private lateinit var saturdayCheckBox: CheckBox
    private lateinit var sundayCheckBox: CheckBox
    private lateinit var reminderSwitch: Switch
    private lateinit var saveButton: Button
    private var selectedDifficulty = 1

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        habitNameEditText = findViewById(R.id.habitName)
        habitDescriptionEditText = findViewById(R.id.habitDescription)
        difficulty1 = findViewById(R.id.difficulty1)
        difficulty2 = findViewById(R.id.difficulty2)
        difficulty3 = findViewById(R.id.difficulty3)
        startDateTextView = findViewById(R.id.startDate)
        endDateTextView = findViewById(R.id.endDate)
        repeatRadioGroup = findViewById(R.id.repeatRadioGroup)
        daysOfWeekLayout = findViewById(R.id.daysOfWeekLayout)
        mondayCheckBox = findViewById(R.id.mondayCheckBox)
        tuesdayCheckBox = findViewById(R.id.tuesdayCheckBox)
        wednesdayCheckBox = findViewById(R.id.wednesdayCheckBox)
        thursdayCheckBox = findViewById(R.id.thursdayCheckBox)
        fridayCheckBox = findViewById(R.id.fridayCheckBox)
        saturdayCheckBox = findViewById(R.id.saturdayCheckBox)
        sundayCheckBox = findViewById(R.id.sundayCheckBox)
        reminderSwitch = findViewById(R.id.reminderSwitch)
        saveButton = findViewById(R.id.saveButton)
        scaleDownAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        scaleUpAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        // Применение анимации к кнопке "Сохранить"
        saveButton.setOnTouchListener { view, event ->
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
        val userId = GlobalData.userId
        Log.d("AddHabit", "Received User ID from GlobalData: $userId")

        if (userId == -1) {
            Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Инициализация Retrofit
        setupRetrofit()

        saveButton.setOnClickListener {
            saveHabit(userId)
        }

        difficulty1.setOnClickListener { setDifficulty(1) }
        difficulty2.setOnClickListener { setDifficulty(2) }
        difficulty3.setOnClickListener { setDifficulty(3) }

        startDateTextView.setOnClickListener {
            showDatePickerDialog(startDateTextView)
        }

        endDateTextView.setOnClickListener {
            showDatePickerDialog(endDateTextView)
        }

        repeatRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.dailyRadioButton -> {
                    daysOfWeekLayout.visibility = View.GONE
                }
                R.id.weeklyRadioButton -> {
                    daysOfWeekLayout.visibility = View.VISIBLE
                }
                R.id.monthlyRadioButton -> {
                    daysOfWeekLayout.visibility = View.GONE
                }
            }
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
            .baseUrl("http://${Config.IP_ADDRESS}:5000/")  // IP-адрес сервера
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun setDifficulty(difficulty: Int) {
        selectedDifficulty = difficulty
        difficulty1.setImageResource(if (difficulty == 1) R.drawable.diamond else R.drawable.gradient)
        difficulty2.setImageResource(if (difficulty == 2) R.drawable.diamond else R.drawable.gradient)
        difficulty3.setImageResource(if (difficulty == 3) R.drawable.diamond else R.drawable.gradient)
    }

    private fun saveHabit(userId: Int) {
        val name = habitNameEditText.text.toString()
        val description = habitDescriptionEditText.text.toString()
        val difficulty = selectedDifficulty

        // Проверка на пустые поля
        if (name.isBlank() || description.isBlank()) {
            Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка даты начала
        val startDateStr = startDateTextView.text.toString().replace("Дата начала: ", "")
        if (startDateStr.isBlank()) {
            Toast.makeText(this, "Пожалуйста, выберите дату начала", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка даты окончания (если выбрана)
        val endDateStr = endDateTextView.text.toString().replace("Дата окончания: ", "")
        if (endDateStr != "не выбрана" && endDateStr.isBlank()) {
            Toast.makeText(this, "Пожалуйста, выберите корректную дату окончания", Toast.LENGTH_SHORT).show()
            return
        }

        // Преобразование дат
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDate: String = try {
            val date = dateFormat.parse(startDateStr)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            Log.e("AddHabit", "Invalid start date format", e)
            Toast.makeText(this, "Некорректный формат даты начала", Toast.LENGTH_SHORT).show()
            return
        }

        val endDate: String? = if (endDateStr != "не выбрана") {
            try {
                val date = dateFormat.parse(endDateStr)
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                Log.e("AddHabit", "Invalid end date format", e)
                Toast.makeText(this, "Некорректный формат даты окончания", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            null
        }

        // Проверка интервала повторения
        val repeatInterval = when (repeatRadioGroup.checkedRadioButtonId) {
            R.id.dailyRadioButton -> "Каждый день"
            R.id.weeklyRadioButton -> "Каждую неделю"
            R.id.monthlyRadioButton -> "Каждый месяц"
            else -> {
                Toast.makeText(this, "Пожалуйста, выберите интервал повторения", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Проверка дней недели (если выбрано "Каждую неделю")
        val daysOfWeek = if (repeatInterval == "Каждую неделю") buildDaysOfWeekString() else ""
        if (repeatInterval == "Каждую неделю" && daysOfWeek.isBlank()) {
            Toast.makeText(this, "Пожалуйста, выберите хотя бы один день недели", Toast.LENGTH_SHORT).show()
            return
        }

        // Создание объекта Habit
        val habit = Habit(
            name = name,
            description = description,
            difficulty = difficulty,
            startDate = startDate,
            endDate = endDate,
            repeatInterval = repeatInterval,
            daysOfWeek = daysOfWeek,
            userId = userId
        )

        // Логирование данных
        Log.d("AddHabit", "Sending Habit: $habit")
        Log.d("AddHabit", "Habit data: ${Gson().toJson(habit)}")

        // Отправка запроса
        addHabit(habit)
    }
    private fun buildDaysOfWeekString(): String {
        val days = mutableListOf<String>()
        if (mondayCheckBox.isChecked) days.add("Понедельник")
        if (tuesdayCheckBox.isChecked) days.add("Вторник")
        if (wednesdayCheckBox.isChecked) days.add("Среда")
        if (thursdayCheckBox.isChecked) days.add("Четверг")
        if (fridayCheckBox.isChecked) days.add("Пятница")
        if (saturdayCheckBox.isChecked) days.add("Суббота")
        if (sundayCheckBox.isChecked) days.add("Воскресенье")
        return days.joinToString(", ")
    }

    private fun showDatePickerDialog(textView: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            textView.text = formattedDate
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun addHabit(habit: Habit) {
        apiService.addHabit(habit).enqueue(object : Callback<HabitResponse> {
            override fun onResponse(call: Call<HabitResponse>, response: Response<HabitResponse>) {
                if (response.isSuccessful) {
                    val habitResponse = response.body()
                    Log.d("DEBUG", "Ответ от сервера: $habitResponse")

                    if (habitResponse != null) {
                        Toast.makeText(this@AddHabit, "Привычка добавлена!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("DEBUG", "habitResponse оказался null")
                        Toast.makeText(this@AddHabit, "Ошибка: сервер вернул пустой ответ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Некорректные данные. Проверьте введенные значения."
                        404 -> "Ресурс не найден. Пожалуйста, попробуйте позже."
                        500 -> "Ошибка сервера. Пожалуйста, попробуйте позже."
                        else -> "Ошибка при добавлении привычки: ${response.code()}"
                    }
                    Log.e("HTTP", errorMessage)
                    Toast.makeText(this@AddHabit, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HabitResponse>, t: Throwable) {
                Log.e("AddHabit", "Ошибка сети: ${t.message}", t)
                Toast.makeText(this@AddHabit, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
