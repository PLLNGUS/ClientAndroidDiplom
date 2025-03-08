package com.example.mydiplom

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import org.json.JSONArray
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class GraphFragment : Fragment() {
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getInt("USER_ID", -1)
            Log.d("GraphFragment", "Получен USER_ID: $userId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_graph, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("GraphFragment", "onViewCreated вызван, userId = $userId")


        super.onViewCreated(view, savedInstanceState)
        if (userId != -1) {
            loadUserStatistics(userId)
        }
    }


    private fun loadUserStatistics(userId: Int) {
        Log.d("GraphFragment", "Вызван loadUserStatistics для userId: $userId")

        val url = "http://${Config.IP_ADDRESS}:5000/api/Statistics/habit-statistics/$userId"
        Log.d("GraphFragment", "Отправка запроса на $url")

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("GraphFragment", "Получен ответ: $response")
                try {
                    // Теперь работаем с массивом
                    updateGraph(response)
                } catch (e: Exception) {
                    Log.e("GraphFragment", "Ошибка обработки ответа: $e")
                }
            },
            { error ->
                Log.e("GraphFragment", "Ошибка загрузки статистики: $error")
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun updateGraph(data: JSONArray) {
        val entries = mutableListOf<BarEntry>()

        val habitsByDay = mutableMapOf<Int, Int>()

        for (i in 0 until data.length()) {
            try {
                val item = data.getJSONObject(i)
                val dateString = item.getString("date")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = dateFormat.parse(dateString)!!

                val calendar = Calendar.getInstance()
                calendar.time = date
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                if (item.getBoolean("isCompleted")) {
                    habitsByDay[day] = habitsByDay.getOrDefault(day, 0) + 1
                }
            } catch (e: Exception) {
                Log.e("GraphFragment", "Ошибка при обработке данных: ${e.message}")
            }
        }

        for ((day, count) in habitsByDay) {
            entries.add(BarEntry(day.toFloat(), count.toFloat()))
        }

        if (entries.isEmpty()) {
            Log.e("GraphFragment", "Нет данных для отображения графика")
            return
        }

        val dataSet = BarDataSet(entries, "Выполненные привычки")
        dataSet.color = Color.GREEN

        val barChart = view?.findViewById<BarChart>(R.id.barChart)
        val barData = BarData(dataSet)
        barChart?.data = barData

        barChart?.xAxis?.apply {
            isEnabled = true
            granularity = 1f
            labelCount = entries.size
        }

        barChart?.axisLeft?.apply {
            axisMinimum = 0f
            granularity = 1f
        }

        barChart?.axisRight?.isEnabled = false

        barChart?.description?.isEnabled = false
        barChart?.invalidate()
    }

    companion object {
        fun newInstance(userId: Int) = GraphFragment().apply {
            arguments = Bundle().apply {
                putInt("USER_ID", userId)
            }
        }
    }
}
