package com.edoctor.dlvn_sdk.service

import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.google.firebase.messaging.FirebaseMessagingService
import com.sendbird.calls.SendBirdCall

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SendbirdChatImpl.registerPushToken(token)
    }
}
