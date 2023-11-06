@file:Suppress("SpellCheckingInspection")

package com.example.dlvn_sdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.dlvn_sdk.Constants.Env
import com.example.dlvn_sdk.Constants.webViewTag
import com.example.dlvn_sdk.api.ApiService
import com.example.dlvn_sdk.api.RetrofitClient
import com.example.dlvn_sdk.model.AccountInitResponse
import com.example.dlvn_sdk.webview.SdkWebView
import com.google.gson.JsonObject
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

class EdoctorDlvnSdk(
    context: Context,
    env: Env = Env.SANDBOX
) {
    private val webView: SdkWebView = SdkWebView(this)
    private var apiService: ApiService? = null
    private var authParams: JSONObject? = null
    private var isFetching: Boolean = false

    companion object {
        const val LOG_TAG = "EDOCTOR_SDK"
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
        internal lateinit var accessToken: String
        internal var edrAccessToken: String? = null
        internal var dlvnAccessToken: String? = null

        fun showError(message: String?) {
            if (message != null && message != "null" && message != "") {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    init {
        EdoctorDlvnSdk.context = context
        accessToken = "hello"

        if (apiService === null) {
            apiService = RetrofitClient(env)
                .getInstance()
                ?.create<ApiService>()
        }
        if (env == Env.LIVE) {
            webView.domain = Constants.healthConsultantUrlProd
        }
    }

    fun openWebView(fragmentManager: FragmentManager, url: String?) {
        val isAvailable = !isFetching && !webView.isVisible
        url?.let {
            webView.domain = url
        }
        if (authParams != null && isAvailable) {
            initDLVNAccount {
                webView.show(fragmentManager, webViewTag)
            }
        } else {
            if (isAvailable) {
                webView.show(fragmentManager, webViewTag)
            }
        }
    }

    fun sampleFunc(name: String): String {
        return "Hello $name from SDK!!!"
    }

    fun DLVNSendData(params: JSONObject): Boolean {
        if (params.length() == 0 || !params.has("dcid") || !params.has("token")) {
            showError(
                if (params.length() == 0)
                    "Data is empty!"
                else "`dcid` and `token` are required!"
            )
            return false
        }
        authParams = params
        return true
    }

    var onSdkRequestLogin: ((callbackUrl: String) -> Unit)? = {}

    private fun initDLVNAccount(mCallback: (result: Any?) -> Unit) {
        try {
            if (authParams != null) {
                isFetching = true

                val params = JsonObject()
                val variables = JSONObject()
                variables.put("data", authParams.toString())
                variables.put("signature", "")
                authParams!!.getString("dcid").let {
                    variables.put("dcId", it.toString())
                }
                authParams!!.getString("token").let {
                    dlvnAccessToken = it.toString()
                }
                params.addProperty("query", "mutation DLVNAccountInit(\$data: String, \$signature: String, \$dcId: String){\n" +
                        "  dlvnAccountInit(data: \$data, signature: \$signature, dcId: \$dcId) {\n" +
                        "    accessToken\n" +
                        "}\n" +
                        "}")
                params.addProperty("variables", variables.toString())

                apiService?.initAccount(params)?.enqueue(object : Callback<AccountInitResponse> {
                    override fun onResponse(call: Call<AccountInitResponse>, response: Response<AccountInitResponse>) {
                        Log.d("zzz", response.body().toString())
                        if (response.body()?.dlvnAccountInit?.accessToken != null) {
                            edrAccessToken = response.body()!!.dlvnAccountInit.accessToken
                            mCallback(response.body())
                        } else {
                            showError("Đã có lỗi xảy ra!")
                        }
                        isFetching = false
                    }

                    override fun onFailure(call: Call<AccountInitResponse>, t: Throwable) {
                        Log.d(LOG_TAG, "An error happened!")
                        showError(t.message.toString())
                        t.printStackTrace()
                        isFetching = false
                    }
                })
            } else {
                showError("Call `DLVNSendData` before calling this function!")
            }
        } catch (e: Error) {

        }
    }

    fun clearWebViewCache() {
        if (authParams != null) {
            edrAccessToken = null
            dlvnAccessToken = null
            authParams = null
            isFetching = false

            webView.clearCacheAndCookies(context)
        }
    }
}
