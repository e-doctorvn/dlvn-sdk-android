package com.example.dlvn_sdk.api

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {
    private val requestTimeout: Long = 180
    private var retrofit: Retrofit? = null
    private val baseUrl: String = "https://api.escuelajs.co"

    fun getInstance(): Retrofit? {
        if (retrofit == null) {
            val interceptor = InterceptorImp()

            val dispatcher = Dispatcher()
            dispatcher.maxRequests = 1

            val okClient: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(requestTimeout, TimeUnit.SECONDS)
                .readTimeout(requestTimeout, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .dispatcher(dispatcher)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okClient)
                .build()
        }
        return  retrofit
    }
}