package com.edoctor.dlvn_sdk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.helper.CallNotificationHelper
import com.edoctor.dlvn_sdk.sendbirdCall.IncomingCallActivity
import com.edoctor.dlvn_sdk.sendbirdCall.VideoCallActivity
import com.edoctor.dlvn_sdk.sendbirdCall.VideoCallActivity.STATE
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall.ongoingCallCount

private const val NOTIFICATION_ID = 1
const val EXTRA_IS_HEADS_UP_NOTIFICATION = "is_heads_up_notification"
const val EXTRA_REMOTE_NICKNAME_OR_USER_ID = "remote_nickname_or_user_id"
const val EXTRA_CALL_STATE = "call_state"
const val EXTRA_CALL_ID = "call_id"
const val EXTRA_IS_VIDEO_CALL = "is_video_call"
const val EXTRA_CALLEE_ID_TO_DIAL = "callee_id_to_dial"
const val EXTRA_DO_DIAL = "do_dial"
const val EXTRA_DO_ACCEPT = "do_accept"
const val EXTRA_IS_ACCEPT_ACTION = "is_accept_action"
const val EXTRA_IS_END_ACTION = "is_end_action"
const val EXTRA_DO_LOCAL_VIDEO_START = "do_local_video_start"
const val EXTRA_DO_END = "do_end"

class CallService : Service() {
    private var mContext: Context? = null
    private val mBinder: IBinder = CallBinder()
    private val mServiceData = ServiceData()

    internal inner class CallBinder : Binder() {
        val service: CallService
            get() = this@CallService
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mServiceData.isHeadsUpNotification =
            intent.getBooleanExtra(EXTRA_IS_HEADS_UP_NOTIFICATION, false)
        mServiceData.remoteNicknameOrUserId =
            intent.getStringExtra(EXTRA_REMOTE_NICKNAME_OR_USER_ID)
        mServiceData.callState = intent.getSerializableExtra(EXTRA_CALL_STATE) as STATE?
        mServiceData.callId = intent.getStringExtra(EXTRA_CALL_ID)
        mServiceData.isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, false)
        mServiceData.calleeIdToDial = intent.getStringExtra(EXTRA_CALLEE_ID_TO_DIAL)
        mServiceData.doDial = intent.getBooleanExtra(EXTRA_DO_DIAL, false)
        mServiceData.doAccept = intent.getBooleanExtra(EXTRA_DO_ACCEPT, true)
        mServiceData.doLocalVideoStart =
            intent.getBooleanExtra(EXTRA_DO_LOCAL_VIDEO_START, false)
        updateNotification(mServiceData)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        mServiceData.isHeadsUpNotification = true
        updateNotification(mServiceData)
    }

    private fun getNotification(serviceData: ServiceData): Notification {
        val content: String = if (serviceData.isVideoCall) {
            "video call"
        } else {
            "call"
        }
        val currentTime = System.currentTimeMillis().toInt()
        val channelId = mContext!!.packageName + currentTime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = mContext!!.getString(com.sendbird.calls.R.string.app_name)
            val channel = NotificationChannel(
                channelId, channelName,
                if (serviceData.isHeadsUpNotification) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = mContext!!.getSystemService(
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(channel)
        }
        val pendingIntentFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val fullScreenIntent = getCallActivityIntent(mContext, serviceData, true)
        val fullscreenPendingIntent =
            PendingIntent.getActivity(mContext, currentTime, fullScreenIntent, pendingIntentFlag)
        serviceData.isEndAction = true
        val endIntent = getCallActivityIntent(mContext, serviceData, true)
        val declinePendingIntent =
            PendingIntent.getBroadcast(mContext, currentTime + 2, endIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        serviceData.isAcceptAction = true
        serviceData.isEndAction = false
        val callIntent = getCallActivityIntent(mContext, serviceData, false)
        val acceptPendingIntent =
            PendingIntent.getActivity(mContext, currentTime + 1, callIntent, pendingIntentFlag)
        val builder = NotificationCompat.Builder(mContext!!, channelId)
        builder.setContentTitle(serviceData.remoteNicknameOrUserId)
            .setContentText(content)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(fullscreenPendingIntent)
            .setFullScreenIntent(fullscreenPendingIntent, true)
            .setAutoCancel(true)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    mContext!!.resources,
                    R.drawable.accept_call_24
                )
            ).priority = NotificationCompat.PRIORITY_HIGH
        if (ongoingCallCount > 0) {
            if (serviceData.doAccept) {
                builder.addAction(NotificationCompat.Action(R.drawable.end_call_24, "Từ chối", declinePendingIntent))
                builder.addAction(NotificationCompat.Action(R.drawable.accept_call_24, "Chấp nhận", acceptPendingIntent))
            }
        }
        return builder.build()
    }

    private fun updateNotification(serviceData: ServiceData) {
        mServiceData.set(serviceData)
        startForeground(NOTIFICATION_ID, getNotification(mServiceData))
    }

    companion object {
        private var mStarted: Boolean = false
        private fun getCallActivityIntent(
            context: Context?,
            serviceData: ServiceData,
            doEnd: Boolean
        ): Intent {
            val intent: Intent = if (serviceData.isAcceptAction) {
                Intent(context, VideoCallActivity::class.java)
            } else {
                if (serviceData.isEndAction) {
                    Intent(context, CallActionReceiver::class.java)
                } else {
                    Intent(context, IncomingCallActivity::class.java)
                }
            }
            intent.putExtra(EXTRA_CALL_STATE, serviceData.callState)
            intent.putExtra(EXTRA_CALL_ID, serviceData.callId)
            intent.putExtra(EXTRA_IS_VIDEO_CALL, serviceData.isVideoCall)
            intent.putExtra(EXTRA_CALLEE_ID_TO_DIAL, serviceData.calleeIdToDial)
            intent.putExtra(EXTRA_DO_DIAL, serviceData.doDial)
            intent.putExtra(EXTRA_DO_ACCEPT, serviceData.doAccept)
            intent.putExtra(EXTRA_DO_LOCAL_VIDEO_START, serviceData.doLocalVideoStart)
            intent.putExtra(EXTRA_DO_END, doEnd)
            intent.putExtra(EXTRA_IS_ACCEPT_ACTION, serviceData.isAcceptAction)
            intent.putExtra(EXTRA_IS_END_ACTION, serviceData.isEndAction)

            if (serviceData.isEndAction) {
                intent.action = "DECLINE_CALL"
            } else if (serviceData.isAcceptAction) {
                intent.action = "ACCEPT_CALL"
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }

            return intent
        }

        fun dial(context: Context, doDialWithCalleeId: String?, isVideoCall: Boolean) {
            if (ongoingCallCount > 0) {
                return
            }
            val serviceData = ServiceData()
            serviceData.isHeadsUpNotification = false
            serviceData.remoteNicknameOrUserId = doDialWithCalleeId
            serviceData.callState = STATE.STATE_OUTGOING
            serviceData.callId = null
            serviceData.isVideoCall = isVideoCall
            serviceData.calleeIdToDial = doDialWithCalleeId
            serviceData.doDial = true
            serviceData.doAccept = false
            serviceData.doLocalVideoStart = false
            startService(context, serviceData)
            context.startActivity(getCallActivityIntent(context, serviceData, false))
        }

        fun onRinging(context: Context?, call: DirectCall) {
            val serviceData = ServiceData()
            serviceData.isHeadsUpNotification = true
            serviceData.remoteNicknameOrUserId = "BS. " + call.caller?.nickname + " đang gọi"
            serviceData.callState = STATE.STATE_ACCEPTING
            serviceData.callId = call.callId
            serviceData.isVideoCall = call.isVideoCall
            serviceData.calleeIdToDial = null
            serviceData.doDial = false
            serviceData.doAccept = true
            serviceData.isEndAction = false
            serviceData.isAcceptAction = false
            serviceData.doLocalVideoStart = false
            startService(context, serviceData)
        }

        private fun startService(context: Context?, serviceData: ServiceData) {
            if (context != null) {
                val intent = Intent(context, CallService::class.java)
                intent.putExtra(EXTRA_IS_HEADS_UP_NOTIFICATION, serviceData.isHeadsUpNotification)
                intent.putExtra(
                    EXTRA_REMOTE_NICKNAME_OR_USER_ID,
                    serviceData.remoteNicknameOrUserId
                )
                intent.putExtra(EXTRA_CALL_STATE, serviceData.callState)
                intent.putExtra(EXTRA_CALL_ID, serviceData.callId)
                intent.putExtra(EXTRA_IS_VIDEO_CALL, serviceData.isVideoCall)
                intent.putExtra(EXTRA_CALLEE_ID_TO_DIAL, serviceData.calleeIdToDial)
                intent.putExtra(EXTRA_DO_DIAL, serviceData.doDial)
                intent.putExtra(EXTRA_DO_ACCEPT, serviceData.doAccept)
                intent.putExtra(EXTRA_DO_LOCAL_VIDEO_START, serviceData.doLocalVideoStart)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                mStarted = true
            }
        }

        fun stopService(context: Context?) {
            if (context != null && mStarted) {
                val intent = Intent(context, CallService::class.java)
                context.stopService(intent)
                mStarted = false
            }
        }
    }
}

data class ServiceData(
    var isHeadsUpNotification: Boolean = false,
    var remoteNicknameOrUserId: String? = null,
    var callState: STATE? = null,
    var callId: String? = null,
    var isAcceptAction: Boolean = false,
    var isVideoCall: Boolean = false,
    var calleeIdToDial: String? = null,
    var doDial: Boolean = false,
    var doAccept: Boolean = true,
    var isEndAction: Boolean = false,
    var doLocalVideoStart: Boolean = false,
) {
    fun set(serviceData: ServiceData) {
        isHeadsUpNotification = serviceData.isHeadsUpNotification
        remoteNicknameOrUserId = serviceData.remoteNicknameOrUserId
        callState = serviceData.callState
        callId = serviceData.callId
        isAcceptAction = serviceData.isAcceptAction
        isVideoCall = serviceData.isVideoCall
        calleeIdToDial = serviceData.calleeIdToDial
        doDial = serviceData.doDial
        doAccept = serviceData.doAccept
        isEndAction = serviceData.isEndAction
        doLocalVideoStart = serviceData.doLocalVideoStart
    }
}