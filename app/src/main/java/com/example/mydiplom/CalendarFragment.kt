package com.example.mydiplom

import android.app.ProgressDialog
import android.graphics.Color
import android.os.Bundle
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CalendarFragment : Fragment() {

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getInt("USER_ID", -1)  // Получаем userId из аргументов
        }
        Log.d("CalendarFragment", "userId: $userId")  // Для отладки
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)

        Log.d("CalendarFragment", "userId: $userId")

        if (userId != -1) {
            loadUserStatistics(userId, calendarView)
        } else {
            Log.e("CalendarFragment", "userId не задан.")
        }

        calendarView.setOnDateChangedListener { _, date, _ ->
            Log.d("CalendarFragment", "Дата, на которую кликнули: ${date.toString()}")

            loadHabitDetailsForDate(date)
        }
    }

    private fun loadUserStatistics(userId: Int, calendarView: MaterialCalendarView) {

        val url = "http://${Config.IP_ADDRESS}:5000/api/Statistics/habit-statistics/$userId"

        val request = JsonArrayRequest(
            Request.Method.GET,
            url, null,
            { response ->
                Log.d("CalendarFragment", "Ответ с сервера: $response")
                updateCalendar(response, calendarView)
            },
            { error ->
                Log.e("CalendarFragment", "Ошибка загрузки статистики: $error")
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun updateCalendar(data: JSONArray, calendarView: MaterialCalendarView) {
        val completedDays = mutableListOf<CalendarDay>()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        for (i in 0 until data.length()) {
            try {
                val habitDate = data.getJSONObject(i).getString("date")
                val date = dateFormat.parse(habitDate)!!

                val calendar = Calendar.getInstance()
                calendar.time = date
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val calendarDay = CalendarDay.from(year, month, day)
                completedDays.add(calendarDay)
            } catch (e: Exception) {
                Log.e("CalendarFragment", "Ошибка при обработке даты: ${e.message}")
            }
        }

        Log.d("CalendarFragment", "Completed Days: $completedDays")

        calendarView.addDecorator(CompletedDayDecorator(completedDays))
    }
    private fun loadHabitDetailsForDate(date: CalendarDay) {
        val dateString = "${date.year}-${(date.month + 1).toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}T00:00:00Z"

        if (!isValidDate(date.year, date.month, date.day)) {
            Log.e("CalendarFragment", "Неверная дата: $dateString")
            displayNoHabitInfo()
            return
        }

        val url = "http://${Config.IP_ADDRESS}:5000/api/Statistics/habit-details?date=$dateString&userId=$userId"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            { response ->
                Log.d("CalendarFragment", "Ответ с сервера: $response")
                displayHabitDetails(response) // Обрабатываем JSONObject
            },
            { error ->
                Log.e("CalendarFragment", "Ошибка загрузки информации о привычке: ${error.message}")
                if (error.networkResponse != null) {
                    val statusCode = error.networkResponse.statusCode
                    val responseBody = String(error.networkResponse.data)
                    Log.e("CalendarFragment", "Ответ с ошибкой: $statusCode, $responseBody")
                    if (statusCode == 404) {
                        displayNoHabitInfo() // Показываем сообщение об отсутствии данных
                    }
                }
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
    private fun loadHabitInfoById(habitId: Int) {
        val url = "http://${Config.IP_ADDRESS}:5000/api/Habits/$habitId"  // Путь для получения информации о привычке по ID

        val request = JsonObjectRequest(
            Request.Method.GET,
            url, null,
            { response ->
                Log.d("CalendarFragment", "Ответ с сервера: $response")
                displayHabitDetails(response)  // Обрабатываем данные привычки
            },
            { error ->
                Log.e("CalendarFragment", "Ошибка при получении информации о привычке: ${error.message}")
                displayNoHabitInfo()  // Если произошла ошибка, показываем сообщение
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun displayHabitDetails(habitDetails: JSONObject) {
        try {
            val date = habitDetails.getString("date")
            val completedCount = habitDetails.getInt("completedCount")
            val habitInfoTextView = view?.findViewById<TextView>(R.id.habitInfoTextView)
            habitInfoTextView?.text = "Дата: $date\nВыполнено привычек: $completedCount"
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Ошибка обработки данных: ${e.message}")
            displayNoHabitInfo()
        }
    }
    private fun displayNoHabitInfo() {
        val habitInfoTextView = view?.findViewById<TextView>(R.id.habitInfoTextView)
        habitInfoTextView?.text = "Нет привычек на этот день."
    }

    private fun isValidDate(year: Int, month: Int, day: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar.get(Calendar.YEAR) == year &&
                calendar.get(Calendar.MONTH) == month &&
                calendar.get(Calendar.DAY_OF_MONTH) == day
    }

    class CompletedDayDecorator(private val completedDays: List<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay?): Boolean {
            return completedDays.contains(day)
        }

        override fun decorate(view: DayViewFacade?) {
            view?.addSpan(BackgroundColorSpan(Color.GREEN)) // Выделяем дни цветом
        }
    }

    companion object {
        fun newInstance(userId: Int) = CalendarFragment().apply {
            arguments = Bundle().apply {
                putInt("USER_ID", userId)
            }
        }
    }
}