package com.example.dlvn_sdk.api

import android.util.Log
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.Constants.Env
import com.example.dlvn_sdk.graphql.DataConverterFactory
import com.google.gson.GsonBuilder
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(env: Env) {
    private val requestTimeout: Long = 180
    private var retrofit: Retrofit? = null
    private val baseUrl: String =
        if (env == Env.LIVE) Constants.edrApiUrlProd
        else Constants.edrApiUrlDev

    fun getInstance(): Retrofit? {
        if (retrofit == null) {
            val interceptor = InterceptorImp()

            val dispatcher = Dispatcher()
            dispatcher.maxRequests = 1

            val gson = GsonBuilder()
                .setLenient()
                .create()

            val okClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(requestTimeout, TimeUnit.SECONDS)
                .readTimeout(requestTimeout, TimeUnit.SECONDS)
//                .addInterceptor(interceptor)
                .dispatcher(dispatcher)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(DataConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okClient)
                .build()
        }
        return  retrofit
    }
}