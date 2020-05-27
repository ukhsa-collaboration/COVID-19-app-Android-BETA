package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import androidx.core.app.NotificationManagerCompat

interface NotificationManagerHelper {
    fun areNotificationsEnabled(): Boolean
}

class AndroidNotificationManagerHelper(private val appContext: Context) : NotificationManagerHelper {

    override fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }
}

class TestNotificationManagerHelper(var notificationEnabled: Boolean) : NotificationManagerHelper {

    override fun areNotificationsEnabled(): Boolean {
        return notificationEnabled
    }
}
