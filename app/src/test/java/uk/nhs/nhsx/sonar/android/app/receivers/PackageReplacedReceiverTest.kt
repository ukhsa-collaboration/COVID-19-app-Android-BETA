/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.receivers

import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Test
import testsupport.TestIntent
import testsupport.mockContextWithMockedAppComponent
import uk.nhs.nhsx.sonar.android.app.notifications.reminders.ReminderScheduler

class PackageReplacedReceiverTest {

    private val reminderScheduler = mockk<ReminderScheduler>()
    private val context = mockContextWithMockedAppComponent()

    private val receiver = PackageReplacedReceiver().also {
        it.reminderScheduler = reminderScheduler
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)

        verifyAll {
            reminderScheduler wasNot Called
        }
    }

    @Test
    fun `onReceive - with package-replaced intent action`() {
        every { reminderScheduler.reschedulePendingReminder() } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_MY_PACKAGE_REPLACED))

        verifyAll {
            reminderScheduler.reschedulePendingReminder()
        }
    }
}
