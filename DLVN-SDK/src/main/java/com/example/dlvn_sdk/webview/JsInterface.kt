package com.example.dlvn_sdk.webview

import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat.startActivity
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.EdoctorDlvnSdk
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
        }
        return true
    }
}