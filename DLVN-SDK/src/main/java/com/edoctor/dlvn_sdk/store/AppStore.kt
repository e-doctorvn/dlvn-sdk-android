package com.edoctor.dlvn_sdk.store

import android.annotation.SuppressLint
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.webview.SdkWebView

object AppStore {
    @SuppressLint("StaticFieldLeak")
    var webViewInstance: SdkWebView? = null
    @SuppressLint("StaticFieldLeak")
    var sdkInstance: EdoctorDlvnSdk? = null
    var activeChannelUrl: String? = null
    var isAppInForeground: Boolean = false
}