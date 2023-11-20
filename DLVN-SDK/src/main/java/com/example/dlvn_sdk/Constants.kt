package com.example.dlvn_sdk

object Constants {
//    - Thêm 3 nút ở view picture in picture webview [DONE]
//    - Pop up thông báo sắp hết thời gian gọi [TO DO]
    const val healthConsultantUrlDev = "https://e-doctor.dev/tu-van-suc-khoe/ho-so-gia-dinh" //"http://192.168.120.250:3000/tu-van-suc-khoe"  //"https://khuat.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val healthConsultantUrlProd = "https://kh.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val edrApiUrlDev = "https://virtual-clinic.api.e-doctor.dev/"
    const val edrApiUrlProd = "https://virtual-clinic.api.edoctor.io/"
    const val webViewTag = "EDR-WebView"
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