package com.edoctor.dlvn_sdk.webview

import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat.startActivity
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.store.AppStore
import org.json.JSONObject

class JsInterface(
    private val webView: SdkWebView,
    private val sdkInstance: EdoctorDlvnSdk
) {
    private var suspendReceiving = false

    private fun isHomePageUrl(baseUrl: String, fullUrl: String): Boolean {
        // Ensure the base URL is a prefix of the full URL
        if (!fullUrl.startsWith(baseUrl)) {
            return false
        }

        // Check if there are characters following the base URL
        val suffix = fullUrl.substring(baseUrl.length)

        // Return true if there is a '/' in the suffix
        return !suffix.contains("/")
    }

    @JavascriptInterface
    fun receiveMessage(data: String): Boolean {
        val json = JSONObject(data)
        when (json.getString("type")) {
            Constants.WebviewParams.closeWebview -> handleCloseWebview(json)
            Constants.WebviewParams.requestLoginNative -> handleRequestLogin(json)
            Constants.WebviewParams.goBackFromDlvn -> 
                webView.requireActivity().runOnUiThread { webView.myWebView.goBack() }
            Constants.WebviewParams.shareDlvnArticle -> shareArticle(json.getString("url"))
            Constants.WebviewParams.onChangeChatChannel -> 
                json.optString("channelUrl").takeIf { it.isNotEmpty() }?.let { AppStore.activeChannelUrl = it }
            Constants.WebviewParams.onRequestUpdateApp -> webView.openAppInStore()
            Constants.WebviewParams.onAuthenShortLink -> 
                sdkInstance.handleAuthenticateShortLink(
                    json.getString("userId"),
                    json.getString("edrToken"),
                    json.getString("dlvnToken")
                )
            Constants.WebviewParams.onAgreeConsent -> { /* Reserved for future use */ }
            Constants.WebviewParams.onLoginSendBird -> {
                sdkInstance.getSendbirdAccount()
                if (json.optString("package") == "VIDEO") {
                    webView.requestCameraAndMicrophonePermissionForVideoCall()
                }
            }
        }
        return true
    }

    private fun handleCloseWebview(json: JSONObject) {
        if (AppStore.activeChannelUrl != Constants.entryChannel) {
            AppStore.activeChannelUrl = Constants.entryChannel
        }
        CallManager.getInstance()?.directCall?.let {
            CallManager.getInstance()?.closeWebViewActivity?.invoke()
            return
        }
        webView.requireActivity().runOnUiThread {
            val dataUrl = JSONObject(json.optString("data")).optString("url")
            when {
                isHomePageUrl(webView.defaultDomain, dataUrl) -> webView.selfClose()
                webView.myWebView.canGoBack() -> webView.myWebView.goBack()
                else -> webView.selfClose()
            }
            if (sdkInstance.isShortLinkAuthen) {
                sdkInstance.handleDeauthenticateShortLink()
            }
        }
    }

    private fun handleRequestLogin(json: JSONObject) {
        val callbackData = JSONObject(json.getString("data"))
        val currentUrl = callbackData.optString("currentUrl")
        if (currentUrl.isNotEmpty() && !suspendReceiving) {
            suspendReceiving = true
            sdkInstance.onSdkRequestLogin?.invoke(currentUrl)
            webView.requireActivity().runOnUiThread { webView.selfClose() }
            suspendReceiving = false
        }
    }

    private fun shareArticle(url: String) {
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        webView.context?.let { startActivity(it, Intent.createChooser(sharingIntent, "Share via"), null) }
    }
}