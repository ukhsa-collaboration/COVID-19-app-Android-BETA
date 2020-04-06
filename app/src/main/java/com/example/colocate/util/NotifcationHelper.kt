package com.example.colocate.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.ColorInt
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.colocate.MainActivity
import com.example.colocate.R
import com.example.colocate.TurnBluetoothOnReceiver
import com.example.colocate.getChannel

const val NOTIFICATION_ID_BLUETOOTH_IS_DISABLED = 1337
const val NOTIFICATION_ID_LOCATION_IS_DISABLED = 1338

public fun hideBluetoothIsDisabledNotification(context: Context) {
    with(NotificationManagerCompat.from(context)) {
        cancel(NOTIFICATION_ID_BLUETOOTH_IS_DISABLED)
    }
}

public fun hideLocationIsDisabledNotification(context: Context) {
    val notificationId = NOTIFICATION_ID_LOCATION_IS_DISABLED
    with(NotificationManagerCompat.from(context)) {
        cancel(notificationId)
    }
}

public fun showBluetoothIsDisabledNotification(context: Context) {
    val notificationId = NOTIFICATION_ID_BLUETOOTH_IS_DISABLED
    val turnBluetoothOnIntent = Intent(context, TurnBluetoothOnReceiver::class.java).apply {
        action = TurnBluetoothOnReceiver.ACTION_TURN_BLUETOOTH_ON
    }
    showNotification(
        context,
        notificationId,
        context.getString(R.string.notification_bluetooth_disabled_title),
        context.getString(R.string.notification_bluetooth_disabled_text),
        context.getString(R.string.notification_bluetooth_disabled_action),
        turnBluetoothOnIntent
    )
}

public fun showLocationIsDisabledNotification(context: Context) {
    val notificationId = NOTIFICATION_ID_LOCATION_IS_DISABLED
    val turnLocationOnIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    showNotification(
        context,
        notificationId,
        context.getString(R.string.notification_location_disabled_title),
        context.getString(R.string.notification_location_disabled_text),
        context.getString(R.string.notification_location_disabled_action),
        turnLocationOnIntent
    )
}

private fun showNotification(
    context: Context,
    notificationId: Int,
    contentTitle: String,
    contentText: String,
    actionTitle: String,
    actionIntent: Intent
) {
    val intent = MainActivity.getIntent(context)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

    val actionPendingIntent: PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    val builder = NotificationCompat.Builder(context, getChannel(context))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setOngoing(true)
        .setColor(context.getColor(R.color.colorAccent))
        .addAction(0, actionTitle, actionPendingIntent)
    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
    }
}

public fun notificationWithColor(context: Context, @ColorInt color: Int): Notification {

    return NotificationCompat.Builder(context, getChannel(context))
        .setColorized(true)
        .setColor(color)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()
}
