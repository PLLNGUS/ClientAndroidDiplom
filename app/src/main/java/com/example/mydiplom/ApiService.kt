package com.example.mydiplom
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class User(
    val nickname: String,
    val email: String,
    val password: String,
    val profilePicture: String? = null
)

data class Habit(
    
    val name: String,
    val description: String,
    val difficulty: Int?,
    val startDate: String?,
    val endDate: String? = null,
    val repeatInterval: String?,
    val daysOfWeek: String?,
    val userId: Int
)
interface ApiService {
    @POST("api/auth/register")
    fun registerUser(@Body user: User): Call<LoginResponse>

    @POST("api/auth/login")
    fun loginUser(@Body user: User): Call<LoginResponse>
    @POST("api/habit/add")
    fun addHabit(@Body habit: Habit): Call<Void>

    @GET("api/habit/user/{userId}")
    fun getUserHabits(@Path("userId") userId: Int): Call<List<Habit>>
}
