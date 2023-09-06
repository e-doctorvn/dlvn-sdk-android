@file:Suppress("SpellCheckingInspection")

package com.example.dlvn_sdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.dlvn_sdk.api.ApiService
import com.example.dlvn_sdk.api.RetrofitClient
import com.example.dlvn_sdk.model.Product
import com.example.dlvn_sdk.model.User
import com.example.dlvn_sdk.webview.SdkWebView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

class DlvnSdk(context: Context) {
    private val webView: SdkWebView = SdkWebView()
    private var apiService: ApiService? = null

    companion object {
        const val LOG_TAG = "EDOCTOR_SDK"
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
        internal lateinit var accessToken: String

        fun showError(message: String?) {
            if (message != null && message != "null" && message != "") {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    init {
        DlvnSdk.context = context
        accessToken = "hello"

        if (apiService === null) {
            apiService = RetrofitClient()
                .getInstance()
                ?.create<ApiService>()
        }
    }

    fun openWebView(fragmentManager: FragmentManager, url: String?) {
        url?.let {
            webView.domain = url
        }
        webView.show(fragmentManager, null)
    }

    fun sampleFunc(name: String): String {
        return "Hello $name from SDK!!!"
    }

    private fun getUserList(mCallback: (result: Any?) -> Unit) {
        try {
            apiService?.getData()?.enqueue(object: Callback<User> {
                /* The HTTP call failed. This method is run on the main thread */
                override fun onFailure(call: Call<User>, t: Throwable) {
                    Log.d(LOG_TAG, "An error happened!")
                    t.printStackTrace()
                }

                /* The HTTP call was successful. This method is run on the main thread */
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    Log.d(LOG_TAG, response.body().toString())
                    mCallback(response.body())
                }
            })
        } catch (e: Error) {

        }
    }

    private fun getProducts(mCallback: (result: Any?) -> Unit) {
        try {
            apiService?.getProducts()?.enqueue(object: Callback<List<Product>> {
                /* The HTTP call failed. This method is run on the main thread */
                override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                    Log.d(LOG_TAG, "An error happened!")
                    t.printStackTrace()
                }

                /* The HTTP call was successful. This method is run on the main thread */
                override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                    Log.d(LOG_TAG, response.body().toString())
                    mCallback(response.body())
                }
            })
        } catch (e: Error) {

        }
    }
}
