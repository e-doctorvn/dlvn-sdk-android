package com.edoctor.dlvn_sdk.sendbirdCall

import android.content.Context
import android.util.Log
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sendbird.android.SendbirdChat
import com.sendbird.android.exception.SendbirdException
import com.sendbird.android.handler.InitResultHandler
import com.sendbird.android.params.InitParams
import com.sendbird.android.push.PushTokenRegistrationStatus

object SendbirdChatImpl {
    fun initSendbirdChat(context: Context, APP_ID: String, userId: String, token: String) {
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
                    Log.d("zzz", "SendbirdChat initialization is completed.")
                    SendbirdChat.connect(userId, token) { user, e ->
                        if (user != null) {
                            Log.d("zzz", "SendbirdChat connect is completed.")
                            FirebaseMessaging.getInstance().token
                                .addOnCompleteListener(object : OnCompleteListener<String?> {
                                    override fun onComplete(task: Task<String?>) {
                                        if (!task.isSuccessful) {
                                            Log.d("zzz", "Fetching FCM registration token failed")
                                            return
                                        }
                                        registerPushToken(task.result!!)
                                        Log.d("zzz", "READY TO GET CHAT NOTI")
                                    }
                                })
//                            if (e != null) {
//                                // Proceed in offline mode with the data stored in the local database.
//                                // Later, connection will be made automatically.
//                                // The connection will be notified through ConnectionHandler.onReconnectSucceeded()
//                                FirebaseMessaging.getInstance().token
//                                    .addOnCompleteListener(object : OnCompleteListener<String?> {
//                                        override fun onComplete(task: Task<String?>) {
//                                            if (!task.isSuccessful) {
//                                                Log.d("zzz", "Fetching FCM registration token failed")
//                                                return
//                                            }
//                                            registerPushToken(task.result!!)
//                                            Log.d("zzz", "READY TO GET CHAT NOTI")
//                                        }
//                                    })
//                            } else {
//                                // Proceed in online mode.
//                            }
                        } else {
                            // Handle error.
                        }
                    }
                }
            }
        )
    }

    fun registerPushToken(pushToken: String) {
//        if (SendbirdChat.isInitialized) {
            SendbirdChat.registerPushToken(pushToken) { status ,e ->
                if (e != null) {
                    // Handle error.
                }

                if (status == PushTokenRegistrationStatus.PENDING) {
                    // A token registration is pending.
                    // Try registering again after a connection has been successfully established.
                }
            }
//        }
    }

    fun disconnect() {
        if (SendbirdChat.isInitialized) {
            SendbirdChat.disconnect {
                Log.d("zzz", "SendbirdChat disconnected.")
            }
        }
    }
}