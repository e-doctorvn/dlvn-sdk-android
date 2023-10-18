package com.example.dlvn_sdk.api

import com.example.dlvn_sdk.model.AccountInitResponse
import com.example.dlvn_sdk.model.User
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("/api/v1/auth/profile")
    fun getData(): Call<User>

    @POST("graphql")
    fun initAccount(@Body params: JsonObject): Call<AccountInitResponse>
}