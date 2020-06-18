/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications.reminders

data class Reminder(val time: Long, val type: Int) {
    companion object {
        const val REMINDER_TYPE = "REMINDER_TYPE"
        const val REMINDER_TYPE_CHECK_IN = 2
        const val REMINDER_TYPE_EXPIRED_EXPOSED = 3
    }
}
