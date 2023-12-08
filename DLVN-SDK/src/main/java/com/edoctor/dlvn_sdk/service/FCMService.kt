package com.edoctor.dlvn_sdk.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.edoctor.dlvn_sdk.helper.CallNotificationHelper
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sendbird.calls.SendBirdCall.handleFirebaseMessageData
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
        val messageType =
            remoteMessage.data["sendbird_call"]?.let { JSONObject(it).getJSONObject("command").get("type").toString() }
//        Log.d("zzz", messageType)
//        val callId = remoteMessage.data["sendbird_call"]?.let { JSONObject(it).getJSONObject("command").getJSONObject("payload").get("custom_items") }
//        callId?.toString()?.let { Log.d("zzz", it) }
        if (isAppInForeground) {
            if (handleFirebaseMessageData(remoteMessage.data)) {

            }
        } else {
            if (messageType == "dial") {
                val pushToken: String? = CallManager.getInstance()?.pushToken
                if (pushToken != null) {
                    handleFirebaseMessageData(remoteMessage.data)
                } else {
                    CallNotificationHelper.showCallNotification(
                        this,
                        CallManager.getInstance()?.directCall?.caller?.nickname ?: "Bác sĩ"
                    )
                }
            }
        }
//        super.onMessageReceived(remoteMessage)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

//        if (currentUser != null) {
//            PushUtils.registerPushToken(
//                applicationContext,
//                token
//            ) { e: SendBirdException? ->
//                if (e != null) {
//                }
//            }
//        } else {
//            PrefUtils.setPushToken(applicationContext, token)
//        }
    }
}
