package com.example.dlvn_sdk.sendbirdCall

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dlvn_sdk.Constants.CallState
import com.example.dlvn_sdk.Constants.CallAction
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.api.ApiService
import com.example.dlvn_sdk.api.RetrofitClient
import com.example.dlvn_sdk.graphql.GraphAction
import com.example.dlvn_sdk.service.CallService
import com.example.dlvn_sdk.store.AppStore
import com.google.gson.JsonObject
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall
import com.sendbird.calls.handler.DirectCallListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create

data class AcceptSetting(
    var microphone: Boolean = true,
    var camera: Boolean = true
)

class CallManager {
    var directCall: DirectCall? = null
    var pushToken: String? = null
    var callState: String = "ENDED"
    var mContext: Context? = null
    private var apiService: ApiService? = null
    var acceptCallSetting: AcceptSetting? = AcceptSetting()

    init {
        if (apiService === null) {
            apiService = RetrofitClient(EdoctorDlvnSdk.environment)
                .getInstance()
                ?.create<ApiService>()
        }
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

    fun resetCall() {
        mContext = null
        directCall = null
        callState = "ENDED"
        acceptCallSetting = AcceptSetting()
    }

    fun handleSendbirdEvent(context: Context) {
        mContext = context
        SendBirdCall.removeAllListeners()
        directCall?.setListener(object : DirectCallListener() {
            override fun onEstablished(call: DirectCall) {
                super.onEstablished(call)
            }

            override fun onConnected(directCall: DirectCall) {
                onCallStateChanged!!.invoke(CallState.CONNECTED)
                callState = "CONNECTED"
            }

            override fun onEnded(directCall: DirectCall) {
                onCallStateChanged!!.invoke(CallState.ENDED)
                CallService.stopService(context)
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

    fun approveEclinicCall() {
        val params = JsonObject()
        val variables = JSONObject()
        if (directCall?.customItems?.contains("appointmentScheduleId") == true && directCall?.customItems?.contains("eClinicId")!!) {
            variables.put("eClinicId", directCall?.customItems?.get("eClinicId"))
            variables.put("appointmentScheduleId", directCall?.customItems?.get("appointmentScheduleId"))
            params.addProperty("query", GraphAction.Mutation.eClinicApproveCall)
            params.addProperty("variables", variables.toString())

            EdoctorDlvnSdk.edrAccessToken?.let {
                apiService?.approveEClinicCall(it, params)?.enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        Log.d("zzz", response.body().toString())
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }
    }

    fun expireEclinicRinging() {
        val params = JsonObject()
        val variables = JSONObject()
        if (directCall?.customItems?.contains("appointmentScheduleId") == true && directCall?.customItems?.contains("eClinicId")!!) {
            variables.put("eClinicId", directCall?.customItems?.get("eClinicId"))
            variables.put("appointmentScheduleId", directCall?.customItems?.get("appointmentScheduleId"))
            params.addProperty("query", GraphAction.Mutation.eClinicExpireRinging)
            params.addProperty("variables", variables.toString())

            EdoctorDlvnSdk.edrAccessToken?.let {
                apiService?.expireEClinicRinging(it, params)?.enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        Log.d("zzz", response.body().toString())
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.d("zzz", t.message.toString())
                    }
                })
            }
        }
    }

    fun endEclinicCall() {
        val params = JsonObject()
        val variables = JSONObject()
        if (directCall?.customItems?.contains("appointmentScheduleId") == true && directCall?.customItems?.contains("eClinicId")!!) {
            variables.put("eClinicId", directCall?.customItems?.get("eClinicId"))
            variables.put("appointmentScheduleId", directCall?.customItems?.get("appointmentScheduleId"))
            params.addProperty("query", GraphAction.Mutation.eClinicEndCall)
            params.addProperty("variables", variables.toString())

            EdoctorDlvnSdk.edrAccessToken?.let {
                apiService?.endEClinicCall(it, params)?.enqueue(object : Callback<Any> {
                    override fun onResponse(call: Call<Any>, response: Response<Any>) {
                        Log.d("zzz", response.body().toString())
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        TODO("Not yet implemented")
                    }
                })
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