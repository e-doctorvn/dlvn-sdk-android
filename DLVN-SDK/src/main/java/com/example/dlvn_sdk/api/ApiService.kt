package com.example.dlvn_sdk.api

import com.example.dlvn_sdk.model.Product
import com.example.dlvn_sdk.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("/api/v1/auth/profile")
    fun getData(): Call<User>

    @POST("/api/v1/auth/login")
    fun login(@Body params: String): Call<User>

    @GET("/api/v1/products?offset=0&limit=4")
    fun getProducts(): Call<List<Product>>
}