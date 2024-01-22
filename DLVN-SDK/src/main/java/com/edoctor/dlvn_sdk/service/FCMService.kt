package com.edoctor.dlvn_sdk.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.edoctor.dlvn_sdk.store.AppStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sendbird.android.push.SendbirdPushHandler
import com.sendbird.calls.SendBirdCall
import com.sendbird.calls.SendBirdCall.handleFirebaseMessageData
import org.json.JSONException
import org.json.JSONObject

class FCMService : FirebaseMessagingService(), LifecycleObserver {
    private var isAppInForeground = false

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForegroundStart() {
        isAppInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onForegroundStop() {
        isAppInForeground = false
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            // CALL
            val messageType =
                remoteMessage.data["sendbird_call"]?.let { JSONObject(it).getJSONObject("command").get("type").toString() }
            val callId = remoteMessage.data["sendbird_call"]?.let { JSONObject(it).getJSONObject("command").getJSONObject("payload").get("custom_items") }
            callId?.toString()?.let { Log.d("zzz", it) }
            if (isAppInForeground) {
                if (handleFirebaseMessageData(remoteMessage.data)) {

                }
            } else {
                if (messageType == "dial") {
                    val pushToken: String? = CallManager.getInstance()?.pushToken
                    if (pushToken != null) {
                        handleFirebaseMessageData(remoteMessage.data)
                    } else {
                        NotificationHelper.showCallNotification(
                            this,
                            CallManager.getInstance()?.directCall?.caller?.nickname ?: "Bác sĩ"
                        )
                    }
                }
            }
            // CHAT
            if (remoteMessage.data.containsKey("sendbird")) {
                val sendbird = remoteMessage.data["sendbird"]?.let { JSONObject(it) }
                val messageTitle = "Tin nhắn mới từ bác sĩ"
                val messageBody = sendbird?.get("message") as String
                val channel = sendbird.get("channel") as JSONObject
                val channelUrl = channel.get("channel_url") as String

                Log.d("zzz", "message: $messageBody")
                if (isAppInForeground) {
                    if (AppStore.activeChannelUrl != channelUrl) {
                        NotificationHelper.showChatNotification(
                            this,
                            messageTitle,
                            messageBody,
                            channelUrl
                        )
                    }
                } else {
                    NotificationHelper.showChatNotification(
                        this,
                        messageTitle,
                        messageBody,
                        channelUrl
                    )
                }
            }
        } catch (e: JSONException) {

        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        SendbirdChatImpl.registerPushToken(token)
        if (SendBirdCall.currentUser != null) {
            SendbirdCallImpl.registerPushToken(token)
        } else {
            PrefUtils.setPushToken(this, token)
        }
    }
}
