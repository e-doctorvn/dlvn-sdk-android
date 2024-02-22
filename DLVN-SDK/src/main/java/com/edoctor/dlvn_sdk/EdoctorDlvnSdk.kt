@file:Suppress("SpellCheckingInspection")

package com.edoctor.dlvn_sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.model.SBAccountResponse
import com.edoctor.dlvn_sdk.model.SendBirdAccount
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.edoctor.dlvn_sdk.store.AppStore
import com.edoctor.dlvn_sdk.Constants.Env
import com.edoctor.dlvn_sdk.Constants.webViewTag
import com.edoctor.dlvn_sdk.api.ApiService
import com.edoctor.dlvn_sdk.api.RetrofitClient
import com.edoctor.dlvn_sdk.graphql.GraphAction
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.model.AccountInitResponse
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.edoctor.dlvn_sdk.webview.SdkWebView
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.sendbird.calls.SendBirdCall
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

class EdoctorDlvnSdk(
    context: Context,
    intent: Intent?,
    env: Env = Env.SANDBOX
) {
    private val edrAppId: String = context.getString(R.string.EDR_APP_ID)
    private val webView: SdkWebView = SdkWebView(this)
    private var apiService: ApiService? = null
    private var authParams: JSONObject? = null
    private var isFetching: Boolean = false
    var isShortLinkAuthen: Boolean = false

    companion object {
        const val LOG_TAG = "EDOCTOR_SDK"
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
        internal var environment: Env = Env.SANDBOX
        internal var needClearCache: Boolean = false
        internal var edrAccessToken: String? = null
        internal var dlvnAccessToken: String? = null
        internal var sendBirdAccount: SendBirdAccount? = null

        fun showError(message: String?) {
            if (message != null && message != "null" && message != "") {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }

        fun isEdrMessage(remoteMessage: RemoteMessage): Boolean {
            return remoteMessage.data.containsKey("sendbird") || remoteMessage.data.containsKey("sendbird_call")
        }

        fun handleEdrRemoteMessage(pContext: Context, remoteMessage: RemoteMessage, icon: Int?) {
            try {
                val messageType =
                    remoteMessage.data["sendbird_call"]?.let { JSONObject(it).getJSONObject("command").get("type").toString() }

                // CHAT
                if (remoteMessage.data.containsKey("sendbird")) {
                    val sendbird = remoteMessage.data["sendbird"]?.let { JSONObject(it) }
                    val channel = sendbird?.get("channel") as JSONObject
                    val channelUrl = channel.get("channel_url") as String
                    val sender = sendbird.get("sender") as JSONObject
                    val doctorName = sender.get("name") as String

                    val messageTitle = "Quý khách có tin nhắn mới từ $doctorName"
                    val messageBody = doctorName + ": " + sendbird.get("message") as String

                    if (AppStore.activeChannelUrl != channelUrl) {
                        NotificationHelper.showChatNotification(pContext, messageTitle, messageBody, channelUrl, icon)
                    }
                } else {
                    // CALL
                    if (AppStore.isAppInForeground) {
                        if (SendBirdCall.handleFirebaseMessageData(remoteMessage.data)) {

                        }
                    } else {
                        if (messageType == "dial") {
                            val pushToken: String? = CallManager.getInstance()?.pushToken
                            if (pushToken != null) {
                                SendBirdCall.handleFirebaseMessageData(remoteMessage.data)
                            } else {
                                NotificationHelper.showCallNotification(
                                    pContext,
                                    CallManager.getInstance()?.directCall?.caller?.nickname ?: "Bác sĩ"
                                )
                            }
                        }
                    }
                }
            } catch (_: JSONException) {

            }
        }

        fun handleNewToken(pContext: Context, token: String) {
            SendbirdChatImpl.registerPushToken(token)
            SendbirdCallImpl.registerPushToken(pContext, token)
        }

        fun setAppState(isForeground: Boolean) {
            AppStore.isAppInForeground = isForeground
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
            webView.defaultDomain = Constants.healthConsultantUrlProd
        }

        if (intent != null) {
            checkSavedAuthCredentials()
            SendbirdCallImpl.initSendbirdCall(context, edrAppId)
            
            if (intent.action?.equals("CallAction") == true) {
                if (intent.getStringExtra("Key") == "END_CALL") {
                    NotificationHelper.action = "_decline"
                } else {
                    NotificationHelper.action = "_accept"
                }
            }
            if (intent.hasExtra(Constants.IntentExtra.chatNotification)
                && intent.getBooleanExtra(Constants.IntentExtra.chatNotification, false)
            ) {
                intent.getStringExtra(Constants.IntentExtra.channelUrl)?.let {
                    openChatChannelFromNotification(it)
                }
            }
        }
    }

    fun openWebView(fragmentManager: FragmentManager, url: String?) {
        webView.domain = url ?: webView.defaultDomain

        if (authParams != null && !isFetching && !webView.isVisible) {
            if (isNetworkConnected()) {
                webView.show(fragmentManager, webViewTag)
                initDLVNAccount {
                    webView.reload()
                }
            } else {
                showError(context.getString(R.string.no_internet_msg))
            }
        } else {
            if (!isFetching && !webView.isVisible) {
                if (isNetworkConnected()) {
                    webView.hideLoading = true
                    webView.show(fragmentManager, webViewTag)
                } else {
                    showError(context.getString(R.string.no_internet_msg))
                }
            }
        }
    }

    fun openWebViewWithEncodedData(fragmentManager: FragmentManager, url: String) {
        if (isNetworkConnected()) {
            webView.domain = url
            webView.hideLoading = true
            webView.show(fragmentManager, webViewTag)
        } else {
            showError(context.getString(R.string.no_internet_msg))
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
    var onOpenChatFromNotiInCalling: (() -> Unit)? = {}

    private fun authenticateSb(context: Context, userId: String, accessToken: String) {
        SendbirdCallImpl.authenticate(context, userId, accessToken)
    }

    private fun deAuthenticateSb() {
        SendbirdCallImpl.deAuthenticate(context)
    }

    private fun initDLVNAccount(mCallback: (result: Any?) -> Unit) {
        try {
            if (authParams != null) { // && dlvnAccessToken == null
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
                    if (PrefUtils.getDlvnToken(context) == "") {
                        PrefUtils.setDlvnToken(context, it.toString())
                    }
                }
                params.addProperty("query", GraphAction.Mutation.dlvnAccountInit)
                params.addProperty("variables", variables.toString())

                apiService?.initAccount(params)?.enqueue(object : Callback<AccountInitResponse> {
                    override fun onResponse(call: Call<AccountInitResponse>, response: Response<AccountInitResponse>) {
                        if (response.body()?.dlvnAccountInit?.accessToken != null) {
                            needClearCache = false
                            edrAccessToken = response.body()!!.dlvnAccountInit.accessToken
                            if (PrefUtils.getEdrToken(context) == "") {
                                PrefUtils.setEdrToken(context, edrAccessToken)
                            }
                            mCallback(response.body())
                            getSendbirdAccount()
                        } else {
                            showError(context.getString(R.string.common_error_msg))
                        }
                        isFetching = false
                    }

                    override fun onFailure(call: Call<AccountInitResponse>, t: Throwable) {
                        Log.d(LOG_TAG, "An error happened!")
                        var message = context.getString(R.string.common_error_msg)
                        if (t.message?.contains("Unable to resolve host") == true) {
                            message = context.getString(R.string.no_internet_msg)
                        }
                        showError(message)
                        t.printStackTrace()
                        isFetching = false
                    }
                })
            } else {
                showError("Call `DLVNSendData` before calling this function!")
            }
        } catch (_: Error) {

        }
    }

    private fun getSendbirdAccount(saveCredentials: Boolean = true) {
        try {
            if (sendBirdAccount?.token == null) {
                val params = JsonObject()
                params.addProperty("query", GraphAction.Query.sendBirdAccount)

                edrAccessToken?.let {
                    apiService?.getSendbirdAccount(it, params)
                        ?.enqueue(object : Callback<SBAccountResponse> {
                            override fun onResponse(
                                call: Call<SBAccountResponse>,
                                response: Response<SBAccountResponse>
                            ) {
                                if (response.body()?.account?.accountId != null) {
                                    val data = response.body()!!.account
                                    sendBirdAccount = SendBirdAccount(
                                        data.accountId,
                                        data.thirdParty.sendbird.token,
                                    )
                                    SendbirdCallImpl.authenticate(
                                        context,
                                        sendBirdAccount?.accountId.toString(),
                                        sendBirdAccount?.token.toString(),
                                        saveCredentials
                                    )
                                }
                            }

                            override fun onFailure(call: Call<SBAccountResponse>, t: Throwable) {
                                Log.d(LOG_TAG, "An error happened!")
                                showError(t.message.toString())
                                t.printStackTrace()
                            }
                        })
                }
            }
        } catch (_: Error) { }
    }

    private fun checkAccountExist(dcid: String, mCallback: (result: Boolean) -> Unit) {
        val params = JsonObject()
        val variables = JSONObject()
        variables.put("phone", dcid)
        params.addProperty("variables", variables.toString())
        params.addProperty("query", GraphAction.Query.checkAccountExist)

        apiService?.checkAccountExist(params)?.enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.body() != null) {
                    val data = JSONObject(response.body().toString())
                    val exist = data.get("checkAccountExist") as Boolean
                    Log.d("zzz", "checkAccountExist: $exist")
                    mCallback(exist)
                }
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(LOG_TAG, "An error happened!")
                showError(t.message.toString())
                t.printStackTrace()
            }
        })
    }

    fun deauthenticateEDR() {
        edrAccessToken = null
        dlvnAccessToken = null
        authParams = null
        isFetching = false
        needClearCache = true
        sendBirdAccount = null

        webView.clearCacheAndCookies(context)
        PrefUtils.removeSdkAuthData(context)
        SendbirdCallImpl.deAuthenticate(context)
        SendbirdChatImpl.disconnect()
    }

    fun authenticateEDR(params: JSONObject) { // Goi luc login thanh cong, ko goi moi lan mo app
        val dcid = params.getString("dcid")
        checkAccountExist(dcid) {
            if (it) {
                if (DLVNSendData(params)) {
                    initDLVNAccount {
                        Log.d("zzz", "initDLVNAccount success")
                    }
                }
            }
        }
    }

    fun handleAuthenticateShortLink(userId: String, edrToken: String, dlvnToken: String) {
        if (userId == sendBirdAccount?.accountId.toString()) return
        else {
            SendbirdCallImpl.logOutCurrentUser(context) {
                sendBirdAccount = null
                isShortLinkAuthen = true
                edrAccessToken = edrToken
                dlvnAccessToken = dlvnToken
                getSendbirdAccount(false)
            }
        }
    }

    fun handleDeauthenticateShortLink() {
        checkSavedAuthCredentials()
        SendbirdCallImpl.logOutCurrentUser(context) {
            getSendbirdAccount()
        }
    }

    private fun checkSavedAuthCredentials() {
        val edrToken: String? = PrefUtils.getEdrToken(context)
        val dlvnToken: String? = PrefUtils.getDlvnToken(context)

        if (!dlvnToken.isNullOrEmpty() && !edrToken.isNullOrEmpty()) {
            edrAccessToken = edrToken
            dlvnAccessToken = dlvnToken
        }
    }

    private fun openChatChannelFromNotification(channelUrl: String) {
        try {
            if (webView.webViewCallActivity == null) {
                val activity = context as AppCompatActivity
                val url = webView.defaultDomain + "/phong-tu-van?channel=${channelUrl}"

                openWebView(activity.supportFragmentManager, url)
            } else {
                onOpenChatFromNotiInCalling?.invoke()
            }
        } catch (e: Exception) {
            Log.d("zzz", e.message.toString())
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }
}