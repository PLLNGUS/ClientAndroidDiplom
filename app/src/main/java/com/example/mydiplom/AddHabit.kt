package com.example.mydiplom

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.util.*

class AddHabit : AppCompatActivity() {

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

        val userId = GlobalData.userId
        Log.d("AddHabit", "Received User ID from GlobalData: $userId")

        if (userId == -1) {
            Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
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
        val startDate = startDateTextView.text.toString().replace("Дата начала: ", "")
        val endDate = endDateTextView.text.toString().replace("Дата окончания: ", "")
        val repeatInterval = when (repeatRadioGroup.checkedRadioButtonId) {
            R.id.dailyRadioButton -> "Каждый день"
            R.id.weeklyRadioButton -> "Каждую неделю"
            R.id.monthlyRadioButton -> "Каждый месяц"
            else -> ""
        }
        val daysOfWeek = if (repeatInterval == "Каждую неделю") buildDaysOfWeekString() else ""


        val habit = Habit(
            name = name,
            description = description,
            difficulty = difficulty,
            startDate = startDate,
            endDate = if (endDate == "не выбрана") null else endDate,
            repeatInterval = repeatInterval,
            daysOfWeek = daysOfWeek,
            userId = GlobalData.userId
        )
        // Логируем объект habit перед отправкой
        Log.d("AddHabit", "Sending Habit: $habit")
        Log.d("AddHabit", "Habit data: ${Gson().toJson(habit)}")

        RetrofitClient.instance.addHabit(habit).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // Логируем код ответа и тело
                Log.d("AddHabit", "Response Code: ${response.code()}")
                Log.d("AddHabit", "Response Body: ${response.body()}")

                if (response.isSuccessful) {
                    Toast.makeText(this@AddHabit, "Привычка добавлена!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e("AddHabit", "Error response: ${response.errorBody()?.string()}")
                    Toast.makeText(this@AddHabit, "Ошибка при добавлении привычки", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("AddHabit", "Network Error: ${t.message}", t)
                Toast.makeText(this@AddHabit, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
}
