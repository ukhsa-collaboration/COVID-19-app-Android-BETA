/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.TurnBluetoothOnReceiver
import javax.inject.Inject

const val NOTIFICATION_ID_BLUETOOTH_IS_DISABLED = 1337
const val NOTIFICATION_ID_LOCATION_IS_DISABLED = 1338
const val NOTIFICATION_CHECK_IN_REMINDER = 1340

class BluetoothNotificationHelper(val context: Context) {

    fun hideBluetoothIsDisabled() {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_ID_BLUETOOTH_IS_DISABLED)
    }

    fun hideLocationIsDisabled() {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_ID_LOCATION_IS_DISABLED)
    }

    fun showBluetoothIsDisabled() {
        val turnBluetoothOnIntent = Intent(context, TurnBluetoothOnReceiver::class.java).apply {
            action = TurnBluetoothOnReceiver.ACTION_TURN_BLUETOOTH_ON
        }
        val actionPendingIntent =
            PendingIntent.getBroadcast(context, 0, turnBluetoothOnIntent, FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            NOTIFICATION_ID_BLUETOOTH_IS_DISABLED,
            context.getString(R.string.notification_bluetooth_disabled_title),
            context.getString(R.string.notification_bluetooth_disabled_text),
            context.getString(R.string.notification_bluetooth_disabled_action),
            actionPendingIntent
        )
    }

    fun showLocationIsDisabled() {
        val turnLocationOnIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val actionPendingIntent =
            PendingIntent.getActivity(context, 0, turnLocationOnIntent, FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            NOTIFICATION_ID_LOCATION_IS_DISABLED,
            context.getString(R.string.notification_location_disabled_title),
            context.getString(R.string.notification_location_disabled_text),
            context.getString(R.string.notification_location_disabled_action),
            actionPendingIntent
        )
    }
}

class CheckInReminderNotification @Inject constructor(private val context: Context) {

    fun show() {
        val actionPendingIntent =
            PendingIntent.getActivity(context, 0, MainActivity.getIntent(context), FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            NOTIFICATION_CHECK_IN_REMINDER,
            context.getString(R.string.checkin_notification_title),
            context.getString(R.string.checkin_notification_text),
            context.getString(R.string.checkin_notification_action),
            actionPendingIntent,
            autoCancel = true,
            isOngoing = false
        )
    }
}

fun Context.notificationBuilder(): NotificationCompat.Builder =
    NotificationCompat.Builder(this, createNotificationChannelReturningId())
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentIntent(mainActivityPendingContent(this))

private fun showNotification(
    context: Context,
    notificationId: Int,
    contentTitle: String,
    contentText: String,
    actionTitle: String,
    actionPendingIntent: PendingIntent? = null,
    autoCancel: Boolean = true,
    isOngoing: Boolean = true
) {
    val builder = context
        .notificationBuilder()
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(contentTitle)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(contentText)
        )
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setContentIntent(mainActivityPendingContent(context))
        .setAutoCancel(autoCancel)
        .setOngoing(isOngoing)
        .setColor(context.getColor(R.color.colorAccent))
        .apply {
            if (actionPendingIntent != null) {
                addAction(0, actionTitle, actionPendingIntent)
            }
        }

    NotificationManagerCompat
        .from(context)
        .notify(notificationId, builder.build())
}

private fun Context.createNotificationChannelReturningId(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.main_notification_channel_name),
            IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    return getString(R.string.default_notification_channel_id)
}

private fun mainActivityPendingContent(context: Context) =
    PendingIntent.getActivity(context, 0, MainActivity.getIntent(context), 0)
