package com.example.dlvn_sdk.webview

import android.util.Log
import org.json.JSONObject
import android.webkit.JavascriptInterface
import com.example.dlvn_sdk.Constants
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.model.AuthenData

class JsInterface(webView: SdkWebView, edoctorDlvnSdk: EdoctorDlvnSdk) {
    private var sdkInstance: EdoctorDlvnSdk? = null
    private var mWebview: SdkWebView? = null

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
            Constants.WebviewParams.loginDataCallback -> {
                val authenResult = JSONObject(json.get("data").toString())
                if (
                    (EdoctorDlvnSdk.edrAccessToken == null || EdoctorDlvnSdk.edrAccessToken != authenResult.getString("edrToken"))
                    && authenResult.has("dlvnToken")
                ) {
                    EdoctorDlvnSdk.edrAccessToken = authenResult.getString("edrToken")
                    sdkInstance?.onAuthenDataResult?.invoke(
                        AuthenData(
                            authenResult.getString("dlvnToken"),
                            authenResult.getString("dcid")
                        )
                    )
                }
            }
        }
        return true
    }
}