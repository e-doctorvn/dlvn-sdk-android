package com.edoctor.dlvn_sdk

object Constants {
    const val healthConsultantUrlDev = "https://e-doctor.dev/phase2/tu-van-suc-khoe"  //"https://khuat.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val healthConsultantUrlProd = "https://kh.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val edrApiUrlDev = "https://virtual-clinic.api.e-doctor.dev/"
    const val edrApiUrlProd = "https://virtual-clinic.api.edoctor.io/"
    const val dlvnDomain = "dai-ichi-life.com.vn"
    const val webViewTag = "EDR-WebView"
    const val sdkMainClassname = "com.edoctor.application.MainActivity"
    object WebviewParams {
        const val closeWebview: String = "close-webview"
        const val goBackFromDlvn: String = "go-back"
        const val shareDlvnArticle: String = "shared-article"
        const val requestLoginNative: String = "request-login-native"
    }
    enum class CallState {
        ESTABLISHED, CONNECTED, RECONNECTING, RECONNECTED, ENDED
    }
    enum class CallAction {
        LOCAL_VIDEO, REMOTE_VIDEO
    }
    enum class Env { LIVE, SANDBOX }
}