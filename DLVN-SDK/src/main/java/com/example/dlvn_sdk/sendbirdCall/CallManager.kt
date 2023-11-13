package com.example.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dlvn_sdk.Constants.CallState
import com.example.dlvn_sdk.Constants.CallAction
import com.example.dlvn_sdk.service.CallService
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall
import com.sendbird.calls.handler.DirectCallListener

data class AcceptSetting(
    var microphone: Boolean = true,
    var camera: Boolean = true
)

class CallManager {
    var directCall: DirectCall? = null
    var pushToken: String? = null
    var callState: String = "ENDED"
    var mContext: Context? = null
    var acceptCallSetting: AcceptSetting? = AcceptSetting()

    fun resetCall() {
        mContext = null
        directCall = null
        callState = "ENDED"
        acceptCallSetting = AcceptSetting()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: CallManager? = null

        fun getInstance(): CallManager? {
            if (instance == null) {
                instance = CallManager()
            }
            return instance
        }
    }

    var closeWebViewActivity: (() -> Unit)? = {}
    var onCallStateChanged: ((state: CallState) -> Unit)? = {}
    var onCallActionChanged: ((state: CallAction) -> Unit)? = {}

    fun handleSendbirdEvent(context: Context) {
        mContext = context
        SendBirdCall.removeAllListeners()
        directCall?.setListener(object : DirectCallListener() {
            override fun onEstablished(call: DirectCall) {
                super.onEstablished(call)
                Log.d("zzz", "onEstablished")
            }

            override fun onConnected(directCall: DirectCall) {
                Log.d("zzz", "onConnected")
                onCallStateChanged!!.invoke(CallState.CONNECTED)
//                if (callState != "CONNECTED") {
//                    finishCurrentActivity()
//                    val intent = Intent(context, VideoCallActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    context.startActivity(intent)
//                    callState = "CONNECTED"
//                    onCallStateChanged!!.invoke(CallState.CONNECTED)
//                }
            }

            override fun onEnded(directCall: DirectCall) {
                onCallStateChanged!!.invoke(CallState.ENDED)
                CallService.stopService(context)
//                finishCurrentActivity()
                resetCall()
            }

            override fun onAudioDeviceChanged(
                call: DirectCall,
                currentAudioDevice: AudioDevice?,
                availableAudioDevices: MutableSet<AudioDevice>
            ) {
                super.onAudioDeviceChanged(call, currentAudioDevice, availableAudioDevices)
            }

            override fun onCustomItemsDeleted(call: DirectCall, deletedKeys: List<String>) {
                super.onCustomItemsDeleted(call, deletedKeys)
            }

            override fun onCustomItemsUpdated(call: DirectCall, updatedKeys: List<String>) {
                super.onCustomItemsUpdated(call, updatedKeys)
            }

            override fun onLocalVideoSettingsChanged(call: DirectCall) {
                super.onLocalVideoSettingsChanged(call)
                onCallActionChanged!!.invoke(CallAction.LOCAL_VIDEO)
            }

            override fun onReconnected(call: DirectCall) {
                super.onReconnected(call)
                onCallStateChanged!!.invoke(CallState.RECONNECTED)
            }

            override fun onReconnecting(call: DirectCall) {
                super.onReconnecting(call)
                onCallStateChanged!!.invoke(CallState.RECONNECTING)
            }

            override fun onRemoteAudioSettingsChanged(call: DirectCall) {
                super.onRemoteAudioSettingsChanged(call)
            }

            override fun onRemoteRecordingStatusChanged(call: DirectCall) {
                super.onRemoteRecordingStatusChanged(call)
            }

            override fun onRemoteVideoSettingsChanged(call: DirectCall) {
                super.onRemoteVideoSettingsChanged(call)
                onCallActionChanged!!.invoke(CallAction.REMOTE_VIDEO)
            }

            override fun onUserHoldStatusChanged(
                call: DirectCall,
                isLocalUser: Boolean,
                isUserOnHold: Boolean
            ) {
                super.onUserHoldStatusChanged(call, isLocalUser, isUserOnHold)
            }
        })
    }

    fun finishCurrentActivity() {
        if (mContext != null) {
            if (mContext is Activity) {
                (mContext as Activity).finish()
            }
        }
    }

    fun rotateCamera(call: DirectCall) {
        call.switchCamera() {

        }
    }

    fun toggleCam(call: DirectCall) {
        if (call.isLocalVideoEnabled) {
            call.stopVideo()
        } else {
            call.startVideo()
        }
    }

    fun toggleMic(call: DirectCall) {
        if (call.isLocalAudioEnabled) {
            call.muteMicrophone()
        } else {
            call.unmuteMicrophone()
        }
    }
}