package com.edoctor.dlvn_sdk.sendbirdCall

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.CallNotificationHelper
import com.edoctor.dlvn_sdk.helper.PrefUtils
import com.edoctor.dlvn_sdk.model.SendBirdAccount
import com.edoctor.dlvn_sdk.service.CallActionReceiver
import com.edoctor.dlvn_sdk.service.CallService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.sendbird.calls.AuthenticateParams
import com.sendbird.calls.CallOptions
import com.sendbird.calls.DialParams
import com.sendbird.calls.DirectCall
import com.sendbird.calls.RoomInvitation
import com.sendbird.calls.SendBirdCall
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.User
import com.sendbird.calls.handler.AuthenticateHandler
import com.sendbird.calls.handler.SendBirdCallListener
import com.sendbird.calls.internal.PushTokenType
import java.util.UUID

object SendbirdCallImpl {
    private const val TAG = "RNSendBirdCalls"

    @JvmStatic
    fun initSendbirdCall(context: Context, APP_ID: String) {
        if (SendBirdCall.init(context, APP_ID)) {
            Toast
                .makeText(context, "initSendbirdCall success", Toast.LENGTH_SHORT)
                .show()
            checkLoggedInUser(context)
        }
    }

    private fun removeAllListeners() {
        SendBirdCall.removeAllListeners()
    }

    fun addListener(context: Context) {
        SendBirdCall.removeAllListeners()

        val UNIQUE_HANDLER_ID = UUID.randomUUID().toString()
        SendBirdCall.addListener(UNIQUE_HANDLER_ID, object : SendBirdCallListener() {
            override fun onRinging(directCall: DirectCall) {
                val ongoingCallCount: Int = SendBirdCall.ongoingCallCount
                if (ongoingCallCount >= 2) {
                    directCall.end()
                    return
                }

                CallService.onRinging(context, directCall)

                CallManager.getInstance()?.directCall = directCall
                CallManager.getInstance()?.callState = "RINGING"
                CallManager.getInstance()!!.handleSendbirdEvent(context)

                if (CallNotificationHelper.action != null) {
                    if (CallNotificationHelper.action == "_decline") {
                        CallManager.getInstance()?.pushToken = null
                        directCall.end()
                    }
                } else {
                    val intent = Intent(context, IncomingCallActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)

                    val filter = IntentFilter()
                    filter.addAction("CallAction")
                    context.registerReceiver(CallActionReceiver(), filter)
                }
            }

            override fun onInvitationReceived(roomInvitation: RoomInvitation) {}
        })
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RINGING, R.raw.ringing)
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RECONNECTING, R.raw.reconnecting)
    }

    @JvmStatic
    fun deAuthenticate(context: Context) {
        removeAllListeners()
        CallManager.getInstance()!!.pushToken?.let {
            SendBirdCall.unregisterPushToken(it, PushTokenType.FCM_VOIP) {
                SendBirdCall.deauthenticate() {
                    PrefUtils.removeSendbirdAuthData(context)
                    Toast.makeText(context, "Logged out SB", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @JvmStatic
    fun authenticate(context: Context, userId: String, accessToken: String?) {
        val params: AuthenticateParams = AuthenticateParams(userId).setAccessToken(accessToken)
        SendBirdCall.authenticate(params, object : AuthenticateHandler {
            override fun onResult(user: User?, e: SendBirdException?) {
                if (e == null) {
                    addListener(context)
                    Toast.makeText(context, "Login $userId Success", Toast.LENGTH_SHORT).show()
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener(object : OnCompleteListener<String?> {
                            override fun onComplete(task: Task<String?>) {
                                if (!task.isSuccessful) {
                                    Log.w(
                                        TAG,
                                        "Fetching FCM registration token failed",
                                        task.exception
                                    )
                                    Log.d("zzz", "Fetching FCM registration token failed")
                                    return
                                }

                                val token: String? = task.result
                                CallManager.getInstance()!!.pushToken = token
                                registerPushToken(token)
                                PrefUtils.setAccessToken(context, accessToken)
                                PrefUtils.setUserId(context, userId)
                            }
                        })
                }
            }
        })
    }

    @JvmStatic
    fun registerPushToken(pushToken: String?) {
        if (pushToken != null) {
            SendBirdCall.registerPushToken(pushToken, PushTokenType.FCM_VOIP, true
            ) { e ->
                if (e == null) {
                    // The push token is registered successfully.
                }
            }
        }
    }

    @JvmStatic
    fun startCall(context: Context, CALLEE_ID: String) {
        val params = DialParams(CALLEE_ID)
        val callOptions = CallOptions()
        params.setVideoCall(true)
        callOptions.setVideoEnabled(true).setAudioEnabled(true).setFrontCameraAsDefault(true)
        params.setCallOptions(callOptions)

        val call: DirectCall? = SendBirdCall.dial(params
        ) { _, e ->
            if (e == null) {
                // The call has been created successfully.
            }
        }

        CallManager.getInstance()?.directCall = call
        CallManager.getInstance()?.callState = "DIALING"
        val intent = Intent(context, IncomingCallActivity::class.java)
        context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(context as Activity).toBundle())
    }

    private fun checkLoggedInUser(context: Context) {
        val accessToken: String? = PrefUtils.getAccessToken(context)
        val userId: String? = PrefUtils.getUserId(context)

        if (accessToken != null && userId != null) {
            if (EdoctorDlvnSdk.sendBirdAccount == null) {
                EdoctorDlvnSdk.sendBirdAccount = SendBirdAccount(userId, accessToken)
            }
            authenticate(context, userId, accessToken)
        }
    }
}