package com.edoctor.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.NotificationHelper
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
    private var edrAppId = ""
    private var didTokenSave = false
    private var isInitialized = false
    private var isAuthenticated = false
    private const val TAG = "RNSendBirdCalls"

    @JvmStatic
    fun initSendbirdCall(context: Context, appId: String) {
        if (!isInitialized) {
            if (SendBirdCall.init(context, appId)) {
                edrAppId = appId
                isInitialized = true
//                checkLoggedInUser(context)
            }
        }
    }

    private fun removeAllListeners() {
        SendBirdCall.removeAllListeners()
    }

    fun addListener(context: Context) {
        SendBirdCall.removeAllListeners()

        val UNIQUE_HANDLER_ID = UUID.randomUUID().toString()
        SendBirdCall.addListener(UNIQUE_HANDLER_ID, object : SendBirdCallListener() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            override fun onRinging(call: DirectCall) {
                val ongoingCallCount: Int = SendBirdCall.ongoingCallCount
                if (ongoingCallCount >= 2) {
                    call.end()
                    return
                }

                val myProcess = RunningAppProcessInfo()
                ActivityManager.getMyMemoryState(myProcess)

                if (myProcess.importance != RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    CallService.onRinging(context, call)
                }

                CallManager.getInstance()?.directCall = call
                CallManager.getInstance()?.callState = "RINGING"
                CallManager.getInstance()!!.handleSendbirdEvent(context)
                CallManager.getInstance()!!.getAppointmentDetail {}

                if (NotificationHelper.action != null) {
                    if (NotificationHelper.action == "_decline") {
                        CallManager.getInstance()?.pushToken = null
                        call.end()
                    }
                } else {
                    val intent = Intent(context, IncomingCallActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)

                    val filter = IntentFilter()
                    filter.addAction("CallAction")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(CallActionReceiver(), filter,
                            Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        context.registerReceiver(CallActionReceiver(), filter)
                    }
                }
            }

            override fun onInvitationReceived(invitation: RoomInvitation) {}
        })
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RINGING, R.raw.ringing)
        SendBirdCall.Options.addDirectCallSound(SendBirdCall.SoundType.RECONNECTING, R.raw.reconnecting)
    }

    @JvmStatic
    fun deAuthenticate(context: Context) {
        removeAllListeners()
        PrefUtils.getPushToken(context)?.let {
            if (it.isNotEmpty()) {
                SendBirdCall.unregisterPushToken(it, PushTokenType.FCM_VOIP) {
                    SendBirdCall.deauthenticate() {
                        didTokenSave = false
                        isAuthenticated = false
                        PrefUtils.removeSendbirdAuthData(context)
                        // Toast.makeText(context, "Logged out SB", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun authenticate(context: Context, userId: String, accessToken: String?, saveCredentials: Boolean = true) {
        if (accessToken != null) {
            val params: AuthenticateParams = AuthenticateParams(userId).setAccessToken(accessToken)
            if (!isAuthenticated) {
                SendBirdCall.authenticate(params, object : AuthenticateHandler {
                    override fun onResult(user: User?, e: SendBirdException?) {
                        if (e == null) {
                            addListener(context)
//                    Toast.makeText(context, "Login $userId Success", Toast.LENGTH_SHORT).show()
                            Log.d("zzz", "UserID: $userId")
                            FirebaseMessaging.getInstance().token
                                .addOnCompleteListener(object : OnCompleteListener<String?> {
                                    override fun onComplete(task: Task<String?>) {
                                        if (!task.isSuccessful) {
                                            Log.w(
                                                TAG,
                                                "Fetching FCM registration token failed",
                                                task.exception
                                            )
                                            return
                                        }

                                        val token: String? = task.result
                                        CallManager.getInstance()!!.pushToken = token
                                        registerPushToken(context, token)
                                        PrefUtils.setPushToken(context, token)

                                        SendbirdChatImpl.initSendbirdChat(
                                            context,
                                            edrAppId,
                                            userId,
                                            accessToken
                                        )
                                        SendbirdChatImpl.registerPushToken(token!!)

                                        if (!didTokenSave && saveCredentials) {
                                            PrefUtils.setAccessToken(context, accessToken)
                                            PrefUtils.setUserId(context, userId)
                                            didTokenSave = true
                                        } else if (!saveCredentials) {
                                            PrefUtils.setShortlinkToken(context, accessToken)
                                            PrefUtils.setShortlinkUserId(context, userId)
                                        }

                                        isAuthenticated = true
                                    }
                                })
                        }
                    }
                })
            }
        }
    }

    @JvmStatic
    fun registerPushToken(context: Context, pushToken: String?) {
        if (!pushToken.isNullOrEmpty()) {
            if (isInitialized) {
                SendBirdCall.registerPushToken(
                    pushToken, PushTokenType.FCM_VOIP, true
                ) { e ->
                    if (e == null) {
                        // The push token is registered successfully.
                    }
                }
            } else {
                PrefUtils.setPushToken(context, pushToken)
            }
        }
    }

    fun logOutCurrentUser(context: Context, mCallback: () -> Unit) {
        removeAllListeners()
        PrefUtils.getPushToken(context)?.let {
            if (it.isNotEmpty()) {
                SendBirdCall.unregisterPushToken(it, PushTokenType.FCM_VOIP) {
                    SendBirdCall.deauthenticate() {
                        isAuthenticated = false
                        mCallback()
                    }
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
//        CallManager.getInstance()?.callState = "DIALING"
        val intent = Intent(context, IncomingCallActivity::class.java)
        context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(context as Activity).toBundle())
    }

    private fun checkLoggedInUser(context: Context) {
        val accessToken: String? = PrefUtils.getAccessToken(context)
        val userId: String? = PrefUtils.getUserId(context)

        if (accessToken != null && accessToken != "" && userId != null && userId != "") {
            didTokenSave = true
            if (EdoctorDlvnSdk.sendBirdAccount?.token == null) {
                EdoctorDlvnSdk.sendBirdAccount = SendBirdAccount(userId, accessToken)
            }
            authenticate(context, userId, accessToken)
        }
    }

    fun getCurrentUser(): User? {
        if (isInitialized) {
            return SendBirdCall.currentUser
        }
        return null
    }
}