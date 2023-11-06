package com.example.dlvn_sdk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.dlvn_sdk.EdoctorDlvnSdk
import com.example.dlvn_sdk.helper.CallNotificationHelper
import com.example.dlvn_sdk.sendbirdCall.CallManager

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val key = intent.getStringExtra("Key")

        if (action != null) {
            when (action) {
                "DECLINE_CALL" -> {
//                    Log.d("zzz", "Decline_Call" + "-" + CallManager.getInstance()?.directCall?.callId)
                    CallManager.getInstance()?.mContext = null
                    CallManager.getInstance()?.directCall?.end()
                }
                "CallAction" ->
                    when (key) {
                        "END_CALL" -> {
                            Log.d("zzz", "End call from notification")
                            val sdk = EdoctorDlvnSdk(context)
                            CallNotificationHelper.cancelCallNotification()
                        }
                        "ACCEPT_CALL" -> {

                        }
                    }
            }
        }
    }
}