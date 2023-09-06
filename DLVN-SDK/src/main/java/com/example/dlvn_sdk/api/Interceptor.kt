package com.example.dlvn_sdk.api

import com.example.dlvn_sdk.EdoctorDlvnSdk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection

class InterceptorImp(): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var accessToken: String? = EdoctorDlvnSdk.accessToken // get accessToken

        val response = chain.proceed(newRequestWithAccessToken(accessToken, request))

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val newAccessToken = EdoctorDlvnSdk.accessToken
            if (newAccessToken != accessToken) {
                return chain.proceed(newRequestWithAccessToken(accessToken, request))
            } else {
                accessToken = refreshToken()
                if (accessToken.isNullOrBlank()) {
                    // log out
                    return response
                }
                return chain.proceed(newRequestWithAccessToken(accessToken, request))
            }
        }

        return response
    }

    private fun newRequestWithAccessToken(accessToken: String?, request: Request): Request =
        request.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

    private fun refreshToken(): String? {
        synchronized(this) {
            val refreshToken = EdoctorDlvnSdk.accessToken
            refreshToken.let {
                return ""
            }
            return null
        }
    }
}