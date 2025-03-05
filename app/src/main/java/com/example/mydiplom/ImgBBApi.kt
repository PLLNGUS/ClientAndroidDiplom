package com.example.mydiplom

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ImgBBApi {
    @Multipart
    @POST("1/upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Query("key") apiKey: String
    ): Response<ImgBBResponse>
}
