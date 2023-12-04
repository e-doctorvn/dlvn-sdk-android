@file:Suppress("SpellCheckingInspection")

package com.example.dlvn_sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.example.dlvn_sdk.Constants.Env
import com.example.dlvn_sdk.Constants.webViewTag
import com.example.dlvn_sdk.api.ApiService
import com.example.dlvn_sdk.api.RetrofitClient
import com.example.dlvn_sdk.helper.CallNotificationHelper
import com.example.dlvn_sdk.graphql.GraphAction
import com.example.dlvn_sdk.model.AccountInitResponse
import com.example.dlvn_sdk.model.SBAccountResponse
import com.example.dlvn_sdk.model.SendBirdAccount
import com.example.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.example.dlvn_sdk.store.AppStore
import com.example.dlvn_sdk.webview.SdkWebView
import com.google.gson.JsonObject
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

class EdoctorDlvnSdk(
    context: Context,
    intent: Intent,
    env: Env = Env.SANDBOX
) {
    private val edrAppId: String = context.getString(R.string.EDR_APP_ID)
    private val webView: SdkWebView = SdkWebView(this)
    private var apiService: ApiService? = null
    private var authParams: JSONObject? = null
    private var isFetching: Boolean = false
    private var sendBirdAccount: SendBirdAccount? = null

    companion object {
        const val LOG_TAG = "EDOCTOR_SDK"
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
        internal var environment: Env = Env.SANDBOX
        internal var needClearCache: Boolean = false
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
        AppStore.sdkInstance = this

        if (apiService === null) {
            apiService = RetrofitClient(env)
                .getInstance()
                ?.create<ApiService>()
        }
        if (env == Env.LIVE) {
            environment = Env.LIVE
            webView.domain = Constants.healthConsultantUrlProd
        }

        if (intent.action?.equals("CallAction") == true) {
            if (intent.getStringExtra("Key") == "END_CALL") {
                CallNotificationHelper.action = "_decline"
            } else {
                CallNotificationHelper.action = "_accept"
            }
        }
        SendbirdCallImpl.initSendbirdCall(context, edrAppId)
    }

    fun openWebView(fragmentManager: FragmentManager, url: String?) {
        url?.let {
            webView.domain = url
        }
        if (webView.isAdded && !webView.isVisible) {
            webView.selfClose()
        }
        if (authParams != null && !isFetching && !webView.isVisible) {
            initDLVNAccount {
                webView.show(fragmentManager, webViewTag)
            }
        } else {
            if (!isFetching && !webView.isVisible) {
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
        needClearCache = false
        return true
    }

    var onSdkRequestLogin: ((callbackUrl: String) -> Unit)? = {}

    fun authenticateSb(context: Context, userId: String, accessToken: String) {
        SendbirdCallImpl.authenticate(context, userId, accessToken)
    }

    fun deAuthenticateSb() {
        SendbirdCallImpl.deAuthenticate(context)
    }

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
                params.addProperty("query", GraphAction.Mutation.dlvnAccountInit)
                params.addProperty("variables", variables.toString())

                apiService?.initAccount(params)?.enqueue(object : Callback<AccountInitResponse> {
                    override fun onResponse(call: Call<AccountInitResponse>, response: Response<AccountInitResponse>) {
                        Log.d("zzz", response.body().toString())
                        if (response.body()?.dlvnAccountInit?.accessToken != null) {
                            needClearCache = false
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

    fun getSendbirdAccount() {
        try {
            val params = JsonObject()
            params.addProperty("query", GraphAction.Query.sendBirdAccount)

            edrAccessToken?.let {
                apiService?.getSendbirdAccount(it, params)?.enqueue(object : Callback<SBAccountResponse> {
                    override fun onResponse(
                        call: Call<SBAccountResponse>,
                        response: Response<SBAccountResponse>
                    ) {
                        if (response.body()?.account?.accountId != null) {
                            val data = response.body()!!.account
                            sendBirdAccount = SendBirdAccount(data.accountId, data.thirdParty.sendbird.token)
                        }
                    }

                    override fun onFailure(call: Call<SBAccountResponse>, t: Throwable) {
                        Log.d(LOG_TAG, "An error happened!")
                        showError(t.message.toString())
                        t.printStackTrace()
                    }
                })
            }
        } catch (e: Error) {

        }
    }

    fun clearWebViewCache() {
        edrAccessToken = null
        dlvnAccessToken = null
        authParams = null
        isFetching = false
        needClearCache = true

        webView.clearCacheAndCookies(context)
    }
}
