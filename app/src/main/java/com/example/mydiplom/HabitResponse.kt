package com.example.mydiplom

data class HabitResponse(
    val id: Int,
    val name: String,
    val description: String,
    val difficulty: Int,
    val startDate: String,
    val endDate: String?,
    val repeatInterval: String,
    val daysOfWeek: String,
    val userId: Int
)
