package com.example.colocate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

private fun Context.createNotificationChannelReturningId(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.main_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    return getString(R.string.default_notification_channel_id)
}

fun Context.notificationBuilder(): NotificationCompat.Builder =
    NotificationCompat.Builder(this, createNotificationChannelReturningId())
