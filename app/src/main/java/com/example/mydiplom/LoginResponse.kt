package com.example.mydiplom

data class LoginResponse(
    val userId: Int,
    val message: String,
    val nickname: String,
    val level: Int,
    val profilePicture: String?
)