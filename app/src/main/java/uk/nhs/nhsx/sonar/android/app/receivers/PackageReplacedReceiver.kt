/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler
import javax.inject.Inject

class PackageReplacedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return

        context.appComponent.inject(this)

        reminderScheduler.reschedulePendingReminder()
    }
}
