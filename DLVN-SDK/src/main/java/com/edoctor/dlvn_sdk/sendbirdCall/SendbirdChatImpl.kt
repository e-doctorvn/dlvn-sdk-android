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

object SendbirdChatImpl {
    private const val TAG = "RNSendBirdChat"

    fun initSendbirdChat(context: Context, appId: String, userId: String?, token: String?) {
        if (!SendbirdChat.isInitialized) {
            SendbirdPushHelper.registerHandler(PushNotificationService())

            SendbirdChat.init(
                InitParams(appId, context, useCaching = true),
                object : InitResultHandler {
                    override fun onMigrationStarted() {
                        Log.i("Application", "Called when there's an update in Sendbird server.")
                    }

                    override fun onInitFailed(e: SendbirdException) {
                        Log.i(
                            "Application",
                            "Called when initialize failed. SDK will still operate properly as if useLocalCaching is set to false."
                        )
                    }

                    override fun onInitSucceed() {
                        authenticateChat(userId, token)
                    }
                }
            )
        } else {
            authenticateChat(userId, token)
        }
    }

    private fun authenticateChat(userId: String?, token: String?) {
        if (userId != null && token != null) {
            SendbirdChat.connect(userId, token) { user, e ->
                if (e != null) {
                    Log.e(TAG, "Failed to connect Sendbird Chat", e)
                    return@connect
                }

                if (user != null) {
                    syncPushTokenAfterConnect()
                    SendbirdChat.setPushTriggerOption(SendbirdChat.PushTriggerOption.ALL) {}
                    SendbirdPushHelper.registerHandler(PushNotificationService())
                } else {
                    Log.w(TAG, "Sendbird Chat connection completed with null user.")
                }
            }
        }
    }

    private fun syncPushTokenAfterConnect() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(object : OnCompleteListener<String?> {
                override fun onComplete(task: Task<String?>) {
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Failed to fetch FCM token for Sendbird Chat.")
                        return
                    }

                    task.result
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::registerPushToken)
                }
            })
    }

    fun registerPushToken(pushToken: String) {
        if (pushToken.isBlank()) return
        if (SendbirdChat.isInitialized) {
            SendbirdChat.registerPushToken(pushToken) { status, e ->
                if (e != null) {
                    Log.e(TAG, "Failed to register Sendbird push token", e)
                    return@registerPushToken
                }

                if (status == PushTokenRegistrationStatus.PENDING) {
                    Log.d(TAG, "Sendbird push token registration is pending.")
                }
            }
        }
    }

    fun disconnect() {
        if (SendbirdChat.isInitialized) {
            SendbirdPushHelper.unregisterHandler(false, object : PushRequestCompleteHandler {
                override fun onComplete(isRegistered: Boolean, token: String?) {
                    unregisterPushTokenAndDisconnect(token)
                }

                override fun onError(e: SendbirdException) {
                    Log.d(TAG, e.message.toString())
                    disconnectChat()
                }
            })
        }
    }

    private fun unregisterPushTokenAndDisconnect(pushToken: String?) {
        pushToken?.takeIf { it.isNotBlank() }?.let {
            unregisterPushToken(it)
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener(object : OnCompleteListener<String?> {
                override fun onComplete(task: Task<String?>) {
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Failed to fetch FCM token while disconnecting Sendbird Chat.")
                        disconnectChat()
                        return
                    }

                    task.result
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::unregisterPushToken)
                        ?: disconnectChat()
                }
            })
    }

    private fun unregisterPushToken(pushToken: String) {
        SendbirdChat.unregisterPushToken(pushToken) { e ->
            if (e != null) {
                Log.w(TAG, "Failed to unregister Sendbird push token", e)
            }
            disconnectChat()
        }
    }

    private fun disconnectChat() {
        SendbirdChat.disconnect {
            Log.d(TAG, "SendbirdChat disconnected.")
        }
    }
}
