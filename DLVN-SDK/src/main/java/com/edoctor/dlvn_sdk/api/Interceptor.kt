package com.edoctor.dlvn_sdk.api

import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection

class InterceptorImp(): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var accessToken: String? = EdoctorDlvnSdk.edrAccessToken // get accessToken

        val response = chain.proceed(newRequestWithAccessToken(accessToken, request))

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val newAccessToken = EdoctorDlvnSdk.edrAccessToken
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
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "$accessToken")
            .build()

    private fun refreshToken(): String? {
        synchronized(this) {
            val refreshToken = EdoctorDlvnSdk.edrAccessToken
            refreshToken.let {
                return ""
            }
            return null
        }
    }
}