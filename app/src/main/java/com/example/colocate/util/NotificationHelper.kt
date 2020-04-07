package com.example.colocate.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.colocate.MainActivity
import com.example.colocate.R
import com.example.colocate.TurnBluetoothOnReceiver

const val NOTIFICATION_ID_BLUETOOTH_IS_DISABLED = 1337
const val NOTIFICATION_ID_LOCATION_IS_DISABLED = 1338

fun hideBluetoothIsDisabledNotification(context: Context) {
    NotificationManagerCompat
        .from(context)
        .cancel(NOTIFICATION_ID_BLUETOOTH_IS_DISABLED)
}

fun hideLocationIsDisabledNotification(context: Context) {
    NotificationManagerCompat
        .from(context)
        .cancel(NOTIFICATION_ID_LOCATION_IS_DISABLED)
}

fun showBluetoothIsDisabledNotification(context: Context) {
    val notificationId = NOTIFICATION_ID_BLUETOOTH_IS_DISABLED
    val turnBluetoothOnIntent = Intent(context, TurnBluetoothOnReceiver::class.java).apply {
        action = TurnBluetoothOnReceiver.ACTION_TURN_BLUETOOTH_ON
    }
    val actionPendingIntent: PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            turnBluetoothOnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    showNotification(
        context,
        notificationId,
        context.getString(R.string.notification_bluetooth_disabled_title),
        context.getString(R.string.notification_bluetooth_disabled_text),
        context.getString(R.string.notification_bluetooth_disabled_action),
        actionPendingIntent
    )
}

fun showLocationIsDisabledNotification(context: Context) {
    val notificationId = NOTIFICATION_ID_LOCATION_IS_DISABLED
    val turnLocationOnIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    val actionPendingIntent: PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            turnLocationOnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

    showNotification(
        context,
        notificationId,
        context.getString(R.string.notification_location_disabled_title),
        context.getString(R.string.notification_location_disabled_text),
        context.getString(R.string.notification_location_disabled_action),
        actionPendingIntent
    )
}

private fun showNotification(
    context: Context,
    notificationId: Int,
    contentTitle: String,
    contentText: String,
    actionTitle: String,
    actionPendingIntent: PendingIntent
) {
    val intent = MainActivity.getIntent(context)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

    val builder = context
        .notificationBuilder()
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setOngoing(true)
        .setColor(context.getColor(R.color.colorAccent))
        .addAction(0, actionTitle, actionPendingIntent)

    NotificationManagerCompat
        .from(context)
        .notify(notificationId, builder.build())
}

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
