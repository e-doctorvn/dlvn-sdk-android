package com.edoctor.dlvn_sdk.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.CallNotificationHelper
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdCallImpl
import com.edoctor.dlvn_sdk.sendbirdCall.SendbirdChatImpl
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
            // CHAT
            if (remoteMessage.data.containsKey("sendbird")) {
//                val sendbird = remoteMessage.data["sendbird"]?.let { JSONObject(it) }
//                val channel = sendbird?.get("channel") as JSONObject
//                val channelUrl = channel["channel_url"] as String
//                val messageTitle = sendbird.get("push_title") as String
//                val messageBody = sendbird.get("message") as String
                // If you want to customize a notification with the received FCM message,
                // write your method like sendNotification() below.
                Log.d("zzz", remoteMessage.data.toString())
                CallNotificationHelper.showCallNotification(
                    this,
                    "HEHEHEHEHHEEH"
                )
                sendNotification(this, "HEHEH", "HEHWHDW dnjsd", "channel_url")
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

    private fun sendNotification(
        context: Context,
        messageTitle: String,
        messageBody: String,
        channelUrl: String
    ) {
        val mainActivityClass = Class.forName(Constants.sdkMainClassname) // context.packageName + ".MainActivity"
        val intent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // Implement your own way to create and show a notification containing the received FCM message.
        val notificationBuilder = NotificationCompat.Builder(context, channelUrl)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.parseColor("#7469C4")) // small icon background color
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(fullScreenIntent)

        notificationBuilder.build()
    }
}
