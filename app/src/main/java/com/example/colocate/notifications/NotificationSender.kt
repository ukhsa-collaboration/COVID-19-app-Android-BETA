package com.example.colocate.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.colocate.R
import com.example.colocate.util.notificationBuilder

interface NotificationSender {
    fun send(
        serviceId: Int,
        notificationTitle: Int,
        notificationText: Int,
        launchedIntentFactory: (Context) -> Intent
    )
}

class AndroidNotificationSender(private val context: Context) : NotificationSender {

    private val manager = NotificationManagerCompat.from(context)

    override fun send(
        serviceId: Int,
        @StringRes notificationTitle: Int,
        @StringRes notificationText: Int,
        launchedIntentFactory: (Context) -> Intent
    ) {
        val launchedIntent = launchedIntentFactory(context)
        val pendingIntent = PendingIntent.getActivity(context, 0, launchedIntent, 0)
        val notificationStyle = NotificationCompat
            .BigTextStyle()
            .bigText(context.getString(notificationText))

        val notification =
            context.notificationBuilder()
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(notificationTitle))
                .setStyle(notificationStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        manager.notify(serviceId, notification)
    }
}
