package com.edoctor.dlvn_sdk.api

import com.edoctor.dlvn_sdk.model.SBAccountResponse
import com.edoctor.dlvn_sdk.model.AccountInitResponse
import com.edoctor.dlvn_sdk.model.User
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @GET("/api/v1/auth/profile")
    fun getData(): Call<User>

    @POST("graphql")
    fun initAccount(@Body params: JsonObject): Call<AccountInitResponse>

    @POST("graphql")
    fun getSendbirdAccount(@Header("Authorization") token: String, @Body params: JsonObject): Call<SBAccountResponse>

    @POST("graphql")
    fun approveEClinicCall(@Header("Authorization") token: String, @Body params: JsonObject): Call<Any>

    @POST("graphql")
    fun endEClinicCall(@Header("Authorization") token: String, @Body params: JsonObject): Call<Any>

    @POST("graphql")
    fun expireEClinicRinging(@Header("Authorization") token: String, @Body params: JsonObject): Call<Any>
}