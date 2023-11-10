package com.example.dlvn_sdk.store

import android.annotation.SuppressLint
import com.example.dlvn_sdk.sendbirdCall.CallManager
import com.example.dlvn_sdk.webview.SdkWebView

object AppStore {
    @SuppressLint("StaticFieldLeak")
    var webViewInstance: SdkWebView? = null
    @SuppressLint("StaticFieldLeak")
    var callManager: CallManager? = null
}