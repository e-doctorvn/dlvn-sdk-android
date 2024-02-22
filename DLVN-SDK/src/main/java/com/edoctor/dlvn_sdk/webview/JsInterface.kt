package com.edoctor.dlvn_sdk.webview

import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat.startActivity
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.store.AppStore
import org.json.JSONObject

class JsInterface(webView: SdkWebView, edoctorDlvnSdk: EdoctorDlvnSdk) {
    private var sdkInstance: EdoctorDlvnSdk? = null
    private var mWebview: SdkWebView? = null
    private var suspendReceiving: Boolean = false

    init {
        mWebview = webView
        sdkInstance = edoctorDlvnSdk
    }

    @JavascriptInterface
    fun receiveMessage(data: String): Boolean {
        val json = JSONObject(data)
        when (json.getString("type")) {
            Constants.WebviewParams.closeWebview -> {
                if (AppStore.activeChannelUrl != Constants.entryChannel) {
                    AppStore.activeChannelUrl = Constants.entryChannel
                }
                if (CallManager.getInstance()?.directCall != null) {
                    CallManager.getInstance()?.closeWebViewActivity?.invoke()
                } else {
                    // mWebview?.requireActivity()?.runOnUiThread { mWebview?.selfClose() }
                    mWebview?.requireActivity()?.runOnUiThread {
                        if (mWebview!!.domain == JSONObject(json.getString("data")).getString("url")) {
                            mWebview!!.selfClose()
//                            if (sdkInstance?.isShortLinkAuthen == true) {
//                                sdkInstance?.handleDeauthenticateShortLink()
//                            }
                        }
                    }
                }
            }
            Constants.WebviewParams.requestLoginNative -> {
                val callbackData = JSONObject(json.get("data").toString())
                if (callbackData.has("currentUrl")) {
                    if (!suspendReceiving) {
                        suspendReceiving = true
                        sdkInstance?.onSdkRequestLogin?.invoke(callbackData.getString("currentUrl"))
                        mWebview?.requireActivity()?.runOnUiThread { mWebview?.selfClose() }
                        suspendReceiving = false
                    }
                }
            }
            Constants.WebviewParams.goBackFromDlvn -> {
                mWebview?.requireActivity()?.runOnUiThread { mWebview?.myWebView?.goBack() }
            }
            Constants.WebviewParams.shareDlvnArticle -> {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                val shareBody = json.getString("url")
                sharingIntent.type = "text/plain"
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
                mWebview?.context?.let { startActivity(it, Intent.createChooser(sharingIntent, "Share via"), null) }
            }
            Constants.WebviewParams.onChangeChatChannel -> {
                if (json.has("channelUrl")) {
                    AppStore.activeChannelUrl = json.get("channelUrl").toString()
                }
            }
            Constants.WebviewParams.onRequestUpdateApp -> {
                mWebview?.openAppInStore()
            }
            Constants.WebviewParams.onAuthenShortLink -> {
                val callbackData = JSONObject(json.get("data").toString())
                val userId = callbackData.get("userId") as String
                val edrToken = callbackData.get("edrToken") as String
                val dlvnToken = callbackData.get("dlvnToken") as String

//                Log.d("zzz", "Constants.WebviewParams.onAuthenShortLink")
//                Log.d("zzz", userId)
//                Log.d("zzz", edrToken)
//                Log.d("zzz", dlvnToken)
//                sdkInstance?.handleAuthenticateShortLink(userId, edrToken, dlvnToken)
            }
        }
        return true
    }
}