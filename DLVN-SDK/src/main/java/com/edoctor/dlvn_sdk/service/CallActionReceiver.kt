package com.edoctor.dlvn_sdk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.edoctor.dlvn_sdk.helper.NotificationHelper
import com.edoctor.dlvn_sdk.sendbirdCall.CallManager

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val key = intent.getStringExtra("Key")

        if (action != null) {
            when (action) {
                "DECLINE_CALL" -> {
                    CallManager.getInstance()?.mContext = null
                    CallManager.getInstance()?.expireEclinicRinging()
                    CallManager.getInstance()?.directCall?.end()
                }
                "CallAction" ->
                    when (key) {
                        "END_CALL" -> {
                            NotificationHelper.cancelCallNotification()
                        }
                        "ACCEPT_CALL" -> {

                        }
                    }
            }
        }
    }
}