package com.example.dlvn_sdk.helper

//import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dlvn_sdk.R
import com.example.dlvn_sdk.service.CallActionReceiver


object CallNotificationHelper {
    var action: String? = null
    private var notificationManager: NotificationManager? = null

    private const val CHANNEL_ID = "call_channel_id"

    fun showCallNotification(context: Context, callerName: String) {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, "Call Channel", NotificationManager.IMPORTANCE_HIGH)
            notificationManager!!.createNotificationChannel(channel)
        }

        val mainActivityClass = Class.forName(context.packageName + ".MainActivity")
        val intent = Intent(context, mainActivityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val decline = Intent(context, CallActionReceiver::class.java)
        decline.action = "CallAction"
        decline.putExtra("Key", "END_CALL")
        val declineIntent = PendingIntent.getBroadcast(context, 0, decline, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Incoming Call")
            .setContentText("$callerName đang gọi")
            .setSmallIcon(R.drawable.end_call_24)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(R.drawable.end_call_24, "Từ chối", declineIntent)
            .addAction(R.drawable.accept_call_24, "Chấp nhận", fullScreenIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        notificationManager!!.notify( /*notification ID*/1, builder.build())
    }

    fun cancelCallNotification() {
        notificationManager?.cancel(1)
    }
}