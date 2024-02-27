package com.edoctor.dlvn_sdk.service

import com.google.firebase.messaging.FirebaseMessagingService

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
//        SendbirdChatImpl.registerPushToken(token)
    }
}
