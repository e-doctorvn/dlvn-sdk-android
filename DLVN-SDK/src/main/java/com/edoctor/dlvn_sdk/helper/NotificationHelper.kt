package com.edoctor.dlvn_sdk.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.edoctor.dlvn_sdk.Constants
import com.edoctor.dlvn_sdk.EdoctorDlvnSdk
import com.edoctor.dlvn_sdk.R
import com.edoctor.dlvn_sdk.service.CallActionReceiver
import java.util.Date


object NotificationHelper {
    var action: String? = null
    private var notificationManager: NotificationManager? = null

    private var activityClassName = ""
    private const val CHANNEL_ID = "call_channel_id"

    fun initialize(context: Context) {
        activityClassName = context.packageName + ".MainActivity" // Constants.sdkMainClassname

        if (notificationManager == null) {
            notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel =
                    NotificationChannel(
                        CHANNEL_ID,
                        "Tư vấn sức khoẻ",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                notificationManager!!.createNotificationChannel(channel)
            }
        }
    }

    fun showCallNotification(context: Context, callerName: String) {
        initialize(context)

        val mainActivityClass = Class.forName(activityClassName)
        val intent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val decline = Intent(context, CallActionReceiver::class.java)
        decline.action = "CallAction"
        decline.putExtra("Key", "END_CALL")
        val declineIntent = PendingIntent.getBroadcast(context, 0, decline, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("BS. $callerName đang gọi")
            .setSmallIcon(R.drawable.end_call_24)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(R.drawable.end_call_24, context.getString(R.string.incoming_decline_label), declineIntent)
            .addAction(R.drawable.accept_call_24, context.getString(R.string.incoming_accept_label), fullScreenIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        notificationManager!!.notify( /*notification ID*/1, builder.build())
    }

    fun cancelCallNotification() {
        notificationManager?.cancel(1)
    }

    fun showChatNotification(context: Context, messageTitle: String, messageBody: String, channelUrl: String) {
        initialize(context)

        val mainActivityClass = Class.forName(activityClassName)
        val intent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.IntentExtra.chatNotification, true)
            putExtra(Constants.IntentExtra.channelUrl, channelUrl)
        }
        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // Implement your own way to create and show a notification containing the received FCM message.
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Color.parseColor("#D25540")) // small icon background color
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(Notification.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(fullScreenIntent)

        notificationManager!!.notify(Date().time.toInt(), notificationBuilder.build())
    }
}