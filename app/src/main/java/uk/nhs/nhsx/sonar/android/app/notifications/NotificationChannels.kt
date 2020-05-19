package uk.nhs.nhsx.sonar.android.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.annotation.StringRes
import uk.nhs.nhsx.sonar.android.app.R
import javax.inject.Inject

class NotificationChannels @Inject constructor(private val context: Context) {

    private val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    enum class Channel(
        @StringRes val id: Int,
        @StringRes val title: Int,
        val importance: Int = IMPORTANCE_DEFAULT
    ) {
        Default(R.string.default_notification_channel_id, R.string.default_notification_channel_name),
        ForegroundService(R.string.foreground_service_channel_id, R.string.foreground_service_channel_name),
        PermissionsAndAccess(R.string.permissions_and_access_channel_id, R.string.permissions_and_access_channel_name),
        ContactAndCheckin(R.string.contact_and_checkin_channel_id, R.string.contact_and_checkin_channel_name)
    }

    fun createChannels() =
        Channel.values().forEach { createChannelReturningId(it) }

    fun createChannelReturningId(channel: Channel): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                context.getString(channel.id),
                context.getString(channel.title),
                channel.importance
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        return context.getString(R.string.default_notification_channel_id)
    }
}
