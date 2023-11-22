package com.example.dlvn_sdk.store

import android.annotation.SuppressLint
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.webview.SdkWebView

object AppStore {
    @SuppressLint("StaticFieldLeak")
    var webViewInstance: SdkWebView? = null
    @SuppressLint("StaticFieldLeak")
    var sdkInstance: EdoctorDlvnSdk? = null
}