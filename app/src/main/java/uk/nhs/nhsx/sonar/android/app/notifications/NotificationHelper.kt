/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uk.nhs.nhsx.sonar.android.app.MainActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.TurnBluetoothOnReceiver
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel.ContactAndCheckin
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationChannels.Channel.PermissionsAndAccess
import uk.nhs.nhsx.sonar.android.app.status.StatusActivity
import javax.inject.Inject

const val NOTIFICATION_ID_BLUETOOTH_IS_DISABLED = 1337
const val NOTIFICATION_ID_LOCATION_IS_DISABLED = 1338
const val NOTIFICATION_CHECK_IN_REMINDER = 1340
const val NOTIFICATION_EXPOSED = 10001
const val NOTIFICATION_TEST_RESULT = 10002

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
        val intent = TurnBluetoothOnReceiver.intent(context)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            PermissionsAndAccess,
            NOTIFICATION_ID_BLUETOOTH_IS_DISABLED,
            context.getString(R.string.notification_bluetooth_disabled_title),
            context.getString(R.string.notification_bluetooth_disabled_text),
            context.getString(R.string.notification_bluetooth_disabled_action),
            pendingIntent
        )
    }

    fun showLocationIsDisabled() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            PermissionsAndAccess,
            NOTIFICATION_ID_LOCATION_IS_DISABLED,
            context.getString(R.string.notification_location_disabled_title),
            context.getString(R.string.notification_location_disabled_text),
            context.getString(R.string.notification_location_disabled_action),
            pendingIntent
        )
    }
}

class CheckInReminderNotification @Inject constructor(private val context: Context) {

    fun hide() {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_CHECK_IN_REMINDER)
    }

    fun show() {
        val actionPendingIntent =
            PendingIntent.getActivity(context, 0, StatusActivity.getIntent(context), FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            ContactAndCheckin,
            NOTIFICATION_CHECK_IN_REMINDER,
            context.getString(R.string.checkin_notification_title),
            context.getString(R.string.checkin_notification_text),
            context.getString(R.string.notification_action_open_app),
            actionPendingIntent,
            autoCancel = true,
            isOngoing = false
        )
    }
}

class ExposedNotification @Inject constructor(private val context: Context) {

    fun hide() {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_EXPOSED)
    }

    fun show() {
        val actionPendingIntent =
            PendingIntent.getActivity(context, 0, StatusActivity.getIntent(context), FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            ContactAndCheckin,
            NOTIFICATION_EXPOSED,
            context.getString(R.string.contact_alert_notification_title),
            context.getString(R.string.contact_alert_notification_text),
            context.getString(R.string.notification_action_open_app),
            actionPendingIntent,
            autoCancel = true,
            isOngoing = false
        )
    }
}

class TestResultNotification @Inject constructor(private val context: Context) {

    fun hide() {
        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_TEST_RESULT)
    }

    fun show() {
        val actionPendingIntent =
            PendingIntent.getActivity(context, 0, StatusActivity.getIntent(context), FLAG_UPDATE_CURRENT)

        showNotification(
            context,
            ContactAndCheckin,
            NOTIFICATION_TEST_RESULT,
            context.getString(R.string.test_result_notification_title),
            context.getString(R.string.test_result_notification_text),
            context.getString(R.string.notification_action_open_app),
            actionPendingIntent,
            autoCancel = true,
            isOngoing = false
        )
    }
}

fun Context.notificationBuilder(channel: Channel): NotificationCompat.Builder =
    NotificationCompat.Builder(this, NotificationChannels(this).createChannelReturningId(channel))
        .setColor(getColor(R.color.colorPrimary))
        .setSmallIcon(R.drawable.ic_status)
        .setContentIntent(mainActivityPendingContent(this))

private fun showNotification(
    context: Context,
    channel: Channel,
    notificationId: Int,
    contentTitle: String,
    contentText: String,
    actionTitle: String,
    actionPendingIntent: PendingIntent? = null,
    autoCancel: Boolean = true,
    isOngoing: Boolean = true
) {
    val builder = context
        .notificationBuilder(channel)
        .setContentTitle(contentTitle)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setAutoCancel(autoCancel)
        .setOngoing(isOngoing)
        .apply {
            if (actionPendingIntent != null) {
                addAction(0, actionTitle, actionPendingIntent)
            }
        }

    NotificationManagerCompat
        .from(context)
        .notify(notificationId, builder.build())
}

private fun mainActivityPendingContent(context: Context) =
    PendingIntent.getActivity(context, 0, MainActivity.getIntent(context), 0)
