package com.edoctor.dlvn_sdk.sendbirdCall

import android.content.Context
import android.util.Log
import com.edoctor.dlvn_sdk.service.PushNotificationService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.handler.PushRequestCompleteHandler
import com.sendbird.android.params.InitParams
import com.sendbird.android.push.PushTokenRegistrationStatus
import com.sendbird.android.push.SendbirdPushHelper
import com.sendbird.calls.SendBirdCall

object SendbirdChatImpl {
    private const val TAG = "RNSendBirdChat"

    fun initSendbirdChat(context: Context, APP_ID: String, userId: String, token: String) {
        SendbirdPushHelper.registerPushHandler(PushNotificationService())

        SendbirdChat.init(
            InitParams(APP_ID, context, useCaching = true),
            object : InitResultHandler {
                override fun onMigrationStarted() {
                    Log.i("Application", "Called when there's an update in Sendbird server.")
                }

                override fun onInitFailed(e: SendbirdException) {
                    Log.i("Application", "Called when initialize failed. SDK will still operate properly as if useLocalCaching is set to false.")
                }

                override fun onInitSucceed() {
                    SendbirdChat.connect(userId, token) { user, _ ->
                        if (user != null) {
                            SendbirdPushHelper.getPushToken { token, _ ->
                                if (token == null) {
                                    FirebaseMessaging.getInstance().token
                                        .addOnCompleteListener(object : OnCompleteListener<String?> {
                                            override fun onComplete(task: Task<String?>) {
                                                if (!task.isSuccessful) {
                                                    return
                                                }
                                                registerPushToken(task.result!!)
                                                Log.d("zzz", "READY TO GET CHAT NOTI")
                                            }
                                        })
                                }
                            }
                            SendbirdChat.setPushTriggerOption(SendbirdChat.PushTriggerOption.ALL) {}
                            SendbirdPushHelper.registerPushHandler(PushNotificationService())
                        } else {
                            // Handle error.
                        }
                    }
                }
            }
        )
    }

    fun registerPushToken(pushToken: String) {
        if (SendbirdChat.isInitialized) {
            SendbirdChat.registerPushToken(pushToken) { status ,e ->
                if (e != null) {
                    // Handle error.
                }

                if (status == PushTokenRegistrationStatus.PENDING) {
                    // A token registration is pending.
                    // Try registering again after a connection has been successfully established.
                }
            }
        }
    }

    fun disconnect() {
        if (SendbirdChat.isInitialized) {
            SendbirdPushHelper.unregisterPushHandler(false, object : PushRequestCompleteHandler {
                override fun onComplete(isRegistered: Boolean, token: String?) {
                    SendbirdChat.disconnect {
                        Log.d("zzz", "SendbirdChat disconnected.")
                    }
                }

                override fun onError(e: SendbirdException) {
                    Log.d(TAG, e.message.toString())
                }
            })
        }
    }
}