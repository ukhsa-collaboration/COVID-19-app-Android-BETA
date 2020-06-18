/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import testsupport.mockContextWithMockedAppComponent
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderBroadcastReceiver
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler

class ReminderBroadcastReceiverTest {

    private val reminderScheduler = mockk<ReminderScheduler>(relaxed = true)
    private val receiver = ReminderBroadcastReceiver()
        .also { it.reminderScheduler = reminderScheduler }

    @Test
    fun onReceive() {
        val context = mockContextWithMockedAppComponent()
        val intent = mockk<Intent>()

        receiver.onReceive(context, intent)

        verify(exactly = 1) { reminderScheduler.handleReminderBroadcast(intent) }
    }
}
