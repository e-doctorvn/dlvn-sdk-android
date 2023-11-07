package com.example.dlvn_sdk.service

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.dlvn_sdk.helper.CallNotificationHelper
import com.example.dlvn_sdk.sendbirdCall.CallManager
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
//        val callId = JSONObject(remoteMessage.data["sendbird_call"]).getJSONObject("command").getJSONObject("payload").get("call_id").toString()
        if (isAppInForeground) {
            if (handleFirebaseMessageData(remoteMessage.data)) {

            }
        } else {
            if (messageType == "dial") {
                val temp = CallManager.getInstance()?.directCall?.callId
                val user: String? = CallManager.getInstance()?.pushToken
                if (user != null) {
                    handleFirebaseMessageData(remoteMessage.data)
                    Log.d("zzz", "app in background")
                } else {
                    CallNotificationHelper.showCallNotification(this, "zzzz")
                    Log.d("zzz", "app in quit state")
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
