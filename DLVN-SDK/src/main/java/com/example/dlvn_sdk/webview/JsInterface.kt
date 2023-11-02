package com.example.dlvn_sdk.webview

import android.util.Log
import org.json.JSONObject
import android.webkit.JavascriptInterface
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.EdoctorDlvnSdk

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
        Log.d("zzz", json.toString())
        when (json.getString("type")) {
            Constants.WebviewParams.closeWebview -> mWebview?.requireActivity()?.runOnUiThread { mWebview?.selfClose() }
            Constants.WebviewParams.requestLoginNative -> {
                val data = JSONObject(json.get("data").toString())
                if (data.has("currentUrl")) {
                    if (!suspendReceiving) {
                        suspendReceiving = true
                        sdkInstance?.onSdkRequestLogin?.invoke(data.getString("currentUrl"))
                        mWebview?.requireActivity()?.runOnUiThread { mWebview?.selfClose() }
                        suspendReceiving = false
                    }
                }
            }
        }
        return true
    }
}