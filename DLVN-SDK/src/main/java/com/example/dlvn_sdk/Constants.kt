package com.example.dlvn_sdk

object Constants {
    const val healthConsultantUrlDev = "https://my-app-delta-sandy.vercel.app/" //"https://e-doctor.dev/tu-van-suc-khoe" //"https://khuat.dai-ichi-life.com.vn/tu-van-suc-khoe" //"https://e-doctor.dev/tu-van-suc-khoe"
    const val healthConsultantUrlProd = "https://kh.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val edrApiUrlDev = "https://virtual-clinic.api.e-doctor.dev/"
    const val edrApiUrlProd = "https://virtual-clinic.api.edoctor.io/"
    const val loginPageUrlDev = "https://khuat.dai-ichi-life.com.vn:8082/login"
    const val webViewTag = "EDR-WebView"
    object WebviewParams {
        const val closeWebview: String = "close-webview"
        const val goBackFromDlvn: String = "go-back"
        const val requestLoginNative: String = "request-login-native"
    }
    enum class Env { LIVE, SANDBOX }
}