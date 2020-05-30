/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.content.Intent
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Test
import testsupport.TestIntent
import testsupport.mockContextWithMockedAppComponent
import uk.nhs.nhsx.sonar.android.app.notifications.Reminders

class PackageReplacedReceiverTest {

    private val reminders = mockk<Reminders>()
    private val context = mockContextWithMockedAppComponent()

    private val receiver = PackageReplacedReceiver().also {
        it.reminders = reminders
    }

    @Test
    fun `onReceive - with unknown intent action`() {
        val intent = TestIntent("SOME_OTHER_ACTION")

        receiver.onReceive(context, intent)

        verifyAll {
            reminders wasNot Called
        }
    }

    @Test
    fun `onReceive - with package-replaced intent action`() {
        every { reminders.reschedulePendingCheckInReminder() } returns Unit

        receiver.onReceive(context, TestIntent(Intent.ACTION_MY_PACKAGE_REPLACED))

        verifyAll {
            reminders.reschedulePendingCheckInReminder()
        }
    }
}
