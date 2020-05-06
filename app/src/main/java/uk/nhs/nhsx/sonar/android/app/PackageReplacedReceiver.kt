/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders
import uk.nhs.nhsx.sonar.android.app.status.UserStateStorage
import javax.inject.Inject

class PackageReplacedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var reminders: Reminders

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return

        context.appComponent.inject(this)

        setReminder()
    }

    private fun setReminder() {
        userStateStorage.get().scheduleCheckInReminder(reminders)
    }
}
