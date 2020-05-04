/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import testsupport.mockContextWithMockedAppComponent

class ReminderBroadcastReceiverTest {

    private val reminders = mockk<Reminders>(relaxed = true)
    private val receiver = ReminderBroadcastReceiver().also { it.reminders = reminders }

    @Test
    fun onReceive() {
        val context = mockContextWithMockedAppComponent()
        val intent = mockk<Intent>()

        receiver.onReceive(context, intent)

        verify(exactly = 1) { reminders.handleReminderBroadcast(intent) }
    }
}
