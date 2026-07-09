package com.breakinschmidt.kappariwear.network

import okhttp3.MultipartBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PaprikaApi {
    @FormUrlEncoded
    @POST("api/v1/account/login/")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @GET("api/v2/sync/groceries/")
    suspend fun getGroceries(
        @Header("Authorization") token: String
    ): GroceryResponse

    @GET("api/v2/sync/grocerylists/")
    suspend fun getGroceryLists(
        @Header("Authorization") token: String
    ): GroceryListResponse

    @Multipart
    @POST("api/v2/sync/groceries/")
    suspend fun syncGroceries(
        @Header("Authorization") token: String,
        @Part data: MultipartBody.Part
    ): Any
}
