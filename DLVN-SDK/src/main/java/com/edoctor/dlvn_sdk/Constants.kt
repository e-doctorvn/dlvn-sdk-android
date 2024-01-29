package com.edoctor.dlvn_sdk

object Constants {
    const val healthConsultantUrlDev = "https://khuat.dai-ichi-life.com.vn/tu-van-suc-khoe"  // "https://e-doctor.dev/tu-van-suc-khoe"
    const val healthConsultantUrlProd = "https://kh.dai-ichi-life.com.vn/tu-van-suc-khoe"
    const val edrApiUrlDev = "https://virtual-clinic.api.e-doctor.dev/"
    const val edrApiUrlProd = "https://virtual-clinic.api.edoctor.io/"
    const val edrAttachmentUrlDev = "https://e-doctor.dev/_upload/image/"
    const val edrAttachmentUrlProd = "https://edoctor.io/_upload/image/"
    const val dlvnDomain = "dai-ichi-life.com.vn"
    const val webViewTag = "EDR-WebView"
    const val sdkMainClassname = "com.edoctor.application.MainActivity"
    const val dConnectMainClassname = "com.dlvn.mcustomerportal.activity.DashboardActivity"
    const val entryChannel = "entry_channel_url"
    const val dConnectStoreUrl = "https://play.google.com/store/apps/details?id=com.dlvn.mcustomerportal"
    object WebviewParams {
        const val closeWebview: String = "close-webview"
        const val goBackFromDlvn: String = "go-back"
        const val shareDlvnArticle: String = "shared-article"
        const val requestLoginNative: String = "request-login-native"
        const val onChangeChatChannel: String = "active-channel-url"
        const val onRequestUpdateApp: String = "request-update-app"
    }
    object IntentExtra {
        const val chatNotification: String = "isEdrChatNotification"
        const val channelUrl: String = "edrChannelUrl"
    }
    enum class CallState {
        ESTABLISHED, CONNECTED, RECONNECTING, RECONNECTED, ENDED
    }
    enum class CallAction {
        LOCAL_VIDEO, REMOTE_VIDEO
    }
    enum class Env { LIVE, SANDBOX }
}