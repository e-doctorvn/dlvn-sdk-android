package com.edoctor.dlvn_sdk.service

import android.content.Context
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.google.firebase.messaging.RemoteMessage
import com.sendbird.android.push.SendbirdPushHandler

class PushNotificationService: SendbirdPushHandler() {
    override fun alwaysReceiveMessage(): Boolean {
        return true
    }

    override fun onMessageReceived(context: Context, remoteMessage: RemoteMessage) {

    }

    override fun onNewToken(newToken: String?) {
        SendbirdChatImpl.registerPushToken(newToken!!)
    }
}