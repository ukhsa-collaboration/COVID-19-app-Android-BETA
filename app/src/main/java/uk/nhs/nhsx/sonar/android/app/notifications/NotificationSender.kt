/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

class NotificationSender @Inject constructor(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun send(
        channel: NotificationChannels.Channel,
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
            context.notificationBuilder(channel)
                .setContentTitle(context.getString(notificationTitle))
                .setStyle(notificationStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        manager.notify(serviceId, notification)
    }
}
