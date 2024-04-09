@file:Suppress("SpellCheckingInspection")

package com.edoctor.dlvn_sdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.ws.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.model.SBAccountResponse
import com.edoctor.dlvn_sdk.model.SendBirdAccount
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.edoctor.dlvn_sdk.store.AppStore
import com.edoctor.dlvn_sdk.Constants.Env
import com.edoctor.dlvn_sdk.Constants.edrGraphQlUrlDev
import com.edoctor.dlvn_sdk.Constants.edrGraphQlUrlProd
import com.edoctor.dlvn_sdk.Constants.edrGraphQlWsUrlDev
import com.edoctor.dlvn_sdk.Constants.edrGraphQlWsUrlProd
import com.edoctor.dlvn_sdk.Constants.webViewTag
import com.edoctor.dlvn_sdk.api.ApiService
import com.edoctor.dlvn_sdk.api.RetrofitClient
import com.edoctor.dlvn_sdk.graphql.GraphAction
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.model.AccountInitResponse
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.edoctor.dlvn_sdk.type.AppointmentScheduleSort
import com.edoctor.dlvn_sdk.type.PageLimitInput
import com.edoctor.dlvn_sdk.type.Sort
import com.edoctor.dlvn_sdk.webview.SdkWebView
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.sendbird.calls.SendBirdCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create
import java.lang.ClassCastException

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
    private var subscriptionCreated: Boolean = false
    var isShortLinkAuthen: Boolean = false
    private var apolloClient: ApolloClient? = null
    private var requestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    companion object {
        const val LOG_TAG = "EDOCTOR_SDK"
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
        internal var accountExist: Boolean? = null
        internal var environment: Env = Env.SANDBOX
        internal var needClearCache: Boolean = false
        internal var edrAccessToken: String? = null
        internal var dlvnAccessToken: String? = null
        internal var debounceWVShortLink: Boolean = false
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
//            SendbirdCallImpl.registerPushToken(pContext, token)
        }

        fun setAppState(isForeground: Boolean) {
            AppStore.isAppInForeground = isForeground
        }
    }

    init {
        EdoctorDlvnSdk.context = context
        AppStore.sdkInstance = this
//        if (context is AppCompatActivity) {
////            requestPermissionLauncher = context.registerForActivityResult(
////                ActivityResultContracts.RequestMultiplePermissions()
////            ) { permissions -> onRequestPermissionsResult(permissions)}
//        }
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
            fetchInitialAppointments()
            SendbirdChatImpl.initSendbirdChat(context, edrAppId, null, null)
            SendbirdCallImpl.initSendbirdCall(context, edrAppId)
            checkAndRemoveShortLinkCredentials {
                getSendbirdAccount()
            }
            
            if (intent.action?.equals("CallAction") == true) {
                if (intent.getStringExtra("Key") == "END_CALL") {
                    NotificationHelper.action = "_decline"
                } else {
                    NotificationHelper.action = "_accept"
                }
            } else if (intent.action.equals("ACCEPT_CALL_FROM_QUIT_STATE")) {
                CallManager.getInstance()?.acceptCallFromQuitState = true
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
        if (isNetworkConnected() && !debounceWVShortLink) {
            webView.domain = url
            debounceWVShortLink = true
            webView.hideLoading = true
            webView.show(fragmentManager, webViewTag)
        } else {
            debounceWVShortLink = false
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
                if (edrAccessToken == null) {
                    showError("Call `DLVNSendData` before calling this function!")
                }
            }
        } catch (_: Error) {

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getSendbirdAccount(saveCredentials: Boolean = true) {
        try {
            if (sendBirdAccount == null || sendBirdAccount?.token == null) {
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
                                    if (data?.thirdParty != null) {
                                        accountExist = true
                                        sendBirdAccount = SendBirdAccount(
                                            data.accountId,
                                            data.thirdParty.sendbird?.token,
                                        )
                                        // token != null: have appointmentSchedules
//                                        SendbirdCallImpl.authenticate(
//                                            context,
//                                            sendBirdAccount?.accountId.toString(),
//                                            sendBirdAccount?.token,
//                                            saveCredentials
//                                        )
                                        webView .lifecycleScope.launch {
                                            initializeSchedulesSubscription()
                                        }
//                                        requestNotificationPermission()
                                    }
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
        if (accountExist == null) {
            val params = JsonObject()
            val variables = JSONObject()
            variables.put("phone", dcid)
            params.addProperty("variables", variables.toString())
            params.addProperty("query", GraphAction.Query.checkAccountExist)

            apiService?.checkAccountExist(params)?.enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        try {
                            if (responseBody != null && responseBody is String && responseBody != JSONObject.NULL) {
                                val responseData = responseBody.toString()
                                try {
                                    if (responseData != JSONObject.NULL) {
                                        val data = JSONObject(responseData)
                                        val exist = data.optBoolean("checkAccountExist")
                                        accountExist = exist
                                        mCallback(exist)
                                    }
                                } catch (e: JSONException) {
                                    Log.e("zzz", "Error parsing JSON", e)
                                }
                            } else {
                                // Handle empty response body
                            }
                        } catch (_: ClassCastException) {

                        }
                    }
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(LOG_TAG, "An error happened!")
                    showError(t.message.toString())
                    t.printStackTrace()
                }
            })
        } else {
            mCallback(accountExist!!)
        }
    }

    private fun updateAccountAgreement(mCallback: (result: Boolean) -> Unit) {
        val params = JsonObject()
        val variables = JSONObject()
        variables.put("isAcceptAgreement", true)
        variables.put("isAcceptShareInfo", true)
        params.addProperty("variables", variables.toString())
        params.addProperty("query", GraphAction.Mutation.updateAccountAgreement)

        edrAccessToken?.let {
            apiService?.updateAccountAgreement(it, params)?.enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.body() != null) {
                        val data = JSONObject(response.body().toString())
                        val success = data.get("accountUpdateAggrement") as Boolean
                        mCallback(success)
                    }
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(LOG_TAG, "An error happened!")
                    showError(t.message.toString())
                    t.printStackTrace()
                    mCallback(false)
                }
            })
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun initializeSchedulesSubscription() {
        edrAccessToken?.let {
            if (!subscriptionCreated) {
                try {
                    Log.d("zzz", "initializeSchedulesSubscription: ${sendBirdAccount?.accountId}")
                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val builder = chain.request().newBuilder()
                            builder.header("Authorization", it)
                            chain.proceed(builder.build())
                        }
                        .build()

                    val subscriptionWsProtocol =
                        SubscriptionWsProtocol.Factory(
                            connectionPayload = { mapOf("authorization" to it) }
                        )

                    val webSocket = WebSocketNetworkTransport.Builder()
                        .protocol(subscriptionWsProtocol)
                        .serverUrl(if (environment == Env.SANDBOX) edrGraphQlWsUrlDev else edrGraphQlWsUrlProd)
                        .build()

                    apolloClient = ApolloClient.Builder()
                        .serverUrl(if (environment == Env.SANDBOX) edrGraphQlUrlDev else edrGraphQlUrlProd)
                        .subscriptionNetworkTransport(webSocket)
                        .httpEngine(DefaultHttpEngine(okHttpClient))
                        .build()

                    subscriptionCreated = true

                    val response = apolloClient!!.query(AppointmentSchedulesQuery(accountId = Optional.present(
                        sendBirdAccount?.accountId), sort = Optional.present(
                        AppointmentScheduleSort(createdAt = Optional.present(Sort.DESC))
                    ), limit = Optional.present(
                        PageLimitInput(Optional.present(0), Optional.present(12))
                    ) )).execute()
                    AppStore.widgetList?.updateDataList(response.data?.appointmentSchedules as List<AppointmentSchedulesQuery.AppointmentSchedule>)
//                    AppStore.widgetList?.dataSet = response.data?.appointmentSchedules as MutableList<SubscribeToScheduleSubscription.AppointmentSchedule>


                    apolloClient!!.subscription(
                        SubscribeToScheduleSubscription(accountId = Optional.present(sendBirdAccount?.accountId))
                    )
                        .toFlow()
                        .collect {
                            Log.d("zzz", "onMessage: ${it.data}")
                            it.data?.appointmentSchedule?.get(0)?.let { it1 ->
                                Log.d("zzz", "handleScheduleSubscriptionMessage: $it1")
                                handleScheduleSubscriptionMessage(
                                    it1
                                )
                            }
                        }
                } catch (e: ApolloException) {
                    Log.d("zzz", e.cause?.message.toString())
                    Log.d("zzz", "Error: " + e.message.toString())
                }
            }
        }
    }

    private fun handleScheduleSubscriptionMessage(data: SubscribeToScheduleSubscription.AppointmentSchedule) {
        AppStore.widgetList?.updateData(data)
    }

    private fun fetchInitialAppointments() {

    }

    fun handleAgreeConsentOnWeb() {
        initDLVNAccount {
            accountExist = true
            updateAccountAgreement {
                webView.reload()
            }
        }
    }

    private fun requestNotificationPermission() {
        requestPermissionLauncher?.launch(
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    fun deauthenticateEDR() {
        edrAccessToken = null
        dlvnAccessToken = null
        authParams = null
        isFetching = false
        needClearCache = true
        sendBirdAccount = null
        accountExist = null

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
        if (edrAccessToken == null) { // Not authenticated
            isShortLinkAuthen = true
            edrAccessToken = edrToken
            dlvnAccessToken = dlvnToken
            getSendbirdAccount(false)
        } else { // Authenticated
            if (userId == sendBirdAccount?.accountId) return
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
    }

    fun handleDeauthenticateShortLink() {
        edrAccessToken = null
        dlvnAccessToken = null
        checkSavedAuthCredentials()

        PrefUtils.removeShortLinkAuthData(context)
        SendbirdCallImpl.logOutCurrentUser(context) {
            sendBirdAccount = null
            isShortLinkAuthen = false
            SendbirdChatImpl.disconnect()
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

    private fun checkAndRemoveShortLinkCredentials(mCallback: () -> Unit) {
        val shortlinkToken: String? = PrefUtils.getShortlinkToken(context)

        if (!shortlinkToken.isNullOrEmpty()) { // Open short link then quit app
            PrefUtils.removeShortLinkAuthData(context)
            SendbirdCallImpl.logOutCurrentUser(context) {
                SendbirdChatImpl.disconnect()
                mCallback()
            }
        } else {
            mCallback()
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

    private fun onRequestPermissionsResult(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        try {
            // 0 - Cam, 1 - Mic, 2 - Notification
            val results = permissions.entries.map { it.value }

            if (results[0]) {
                NotificationHelper.initialize(context)
            }

            return
        } catch (_: Error) {

        }
    }

    fun CoroutineScope.go() = launch {
        initializeSchedulesSubscription()
    }
}